import com.fazecast.jSerialComm.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class recdata {
    public static void main(String[] args) {
        // Find the HC-05 Bluetooth module by port name
        String portName = "/dev/rfcomm0"; // Replace with the appropriate port name
        SerialPort serialPort = SerialPort.getCommPort(portName);

        // Set the desired parameters for serial communication
        serialPort.setBaudRate(9600);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setNumDataBits(8);

        // Open the serial port
        if (serialPort.openPort()) {
            System.out.println("Serial port opened successfully.");
        } else {
            System.err.println("Failed to open the serial port.");
            return;
        }

        // Establish the JDBC connection to Derby
        try {
            String url = "jdbc:derby:testdb;create=true";
            Connection conn = DriverManager.getConnection(url);

            // Check if the table exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "SENSOR_DATA", null);
            if (!tables.next()) {
                // Create the table if it doesn't exist
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE TABLE SENSOR_DATA (TEMPERATURE DOUBLE, HUMIDITY DOUBLE, LIGHT_SENSOR DOUBLE, LAND_MOISTURE DOUBLE, INSERT_TIME TIMESTAMP)");
                stmt.close();
            }
            tables.close();

            // Create a new thread for receiving data
            Thread receivingThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                int numBytes;

                while (true) {
                    try {
                        Thread.sleep(1000); // Delay for 1 second
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Read data from the serial port
                    numBytes = serialPort.readBytes(buffer, buffer.length);

                    // Process the received data
                    if (numBytes > 0) {
                        String receivedData = new String(buffer, 0, numBytes);
                        System.out.println("Received data: " + receivedData);

                        // Split the received data by comma
                        String[] values = receivedData.split(",");
                        if (values.length == 4) {
                            try {
                                // Parse the values and calculate percentages
                                double temperature = Double.parseDouble(values[0]);
                                double humidity = Double.parseDouble(values[1]);
                                double lightSensor = Double.parseDouble(values[2]);
                                double landMoisture = Double.parseDouble(values[3]);

                                // Calculate soil moisture percentage
                                double invertedLandMoisture = 1100 - landMoisture;
                                double soilMoisturePercentage = (invertedLandMoisture / 1100) * 100;

                                // Calculate light sensor percentage
                                double lightSensorPercentage = (lightSensor / 950) * 100;

                                // Get current time and date
                                LocalDateTime currentTime = LocalDateTime.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                String formattedDateTime = currentTime.format(formatter);

                                // Insert data into the database
                                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO SENSOR_DATA (TEMPERATURE, HUMIDITY, LIGHT_SENSOR, LAND_MOISTURE, INSERT_TIME) VALUES (?, ?, ?, ?, ?)");
                                pstmt.setDouble(1, temperature);
                                pstmt.setDouble(2, humidity);
                                pstmt.setDouble(3, lightSensorPercentage);
                                pstmt.setDouble(4, soilMoisturePercentage);
                                pstmt.setString(5, formattedDateTime);
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

