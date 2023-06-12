import com.fazecast.jSerialComm.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class recdataserver {
    public static void main(String[] args) {
        // Bluetooth connection
        String portName = "/dev/rfcomm0"; 
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(9600);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setNumDataBits(8);
        
        //check if the port is open
        if (serialPort.openPort()) {
            System.out.println("Serial port opened successfully.");
        } else {
            System.err.println("Failed to open the serial port.");
            return;
        }

        // connection with the database
        try {
            String url = "jdbc:derby://localhost:1527/mydatabase;create=true";
            Connection conn = DriverManager.getConnection(url);

            // Check if the table exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "SENSOR_DATA", null);
            if (!tables.next()) {
                // Create the table if it doesn't exist
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE TABLE sensor_data (timestamp TIMESTAMP, temperature DECIMAL, humidity DECIMAL,light_sensor DECIMAL, soil_moisture DECIMAL)");
                stmt.close();
            }
            tables.close();

            // Create a new thread for receiving data
            Thread receivingThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                int numBytes;

                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Read data from the serial port
                    numBytes = serialPort.readBytes(buffer, buffer.length);

                    // Process the received data
                    if (numBytes > 0) {
                        String receivedData = new String(buffer, 0, numBytes);
                        System.out.println(" +++ Received data : " + receivedData);

                        // Split the received data by comma
                        String[] values = receivedData.split(",");
                        if (values.length == 4) {
                            try {
                                // Parse the values and calculate percentages
                                double temperature = Double.parseDouble(values[0]);
                                double humidity = Double.parseDouble(values[1]);
                                double lightSensor = Double.parseDouble(values[2]);
                                double soilMoisture_raw = Double.parseDouble(values[3]);

                                // Calculate soil moisture percentage
                                double soilMoisture = soilMoisture_raw - 150; // min value is 150 and max is 1050
                                double invertedSoilMoisture = 900 - soilMoisture; // 1050 - 150 = 900
                                double soil_percentage = Math.round((invertedSoilMoisture* 100) / 900);

                                // Calculate light sensor percentage
                                double light_percentage = Math.round((lightSensor / 1000) * 100 * 10) / 10.0;
                                
                                System.out.println(temperature + " -- " + humidity + " -- "+ light_percentage + " -- "+ soil_percentage);
                                // Get current time and date
                                LocalDateTime currentTime = LocalDateTime.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                String formattedDateTime = currentTime.format(formatter);

                                // Insert data into the database
                                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO SENSOR_DATA (TIMESTAMP, TEMPERATURE, HUMIDITY, LIGHT_SENSOR, SOIL_MOISTURE) VALUES (?, ?, ?, ?, ?)");
                                pstmt.setString(1, formattedDateTime);
                                pstmt.setDouble(2, temperature);
                                pstmt.setDouble(3, humidity);
                                pstmt.setDouble(4, light_percentage);
                                pstmt.setDouble(5, soil_percentage);
                                
                                pstmt.executeUpdate();
                                pstmt.close();
                            } catch (NumberFormatException | SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

            // Start the receiving thread
            receivingThread.start();

            // Wait for the receiving thread to finish (optional)
            try {
                receivingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Close the JDBC connection when done
            conn.close();
            System.out.println("JDBC connection closed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Close the serial port when done
        serialPort.closePort();
        System.out.println("Serial port closed.");
    }
}
