import serial
import datetime
import time
import pyrebase
import jaydebeapi
import jpype

# Bluetooth serial port settings
bluetooth_port = '/dev/rfcomm0'
baud_rate = 9600

# Connect to the Bluetooth module
bluetooth = serial.Serial(bluetooth_port, baud_rate)

# Load the Derby JDBC driver
derby_jars = ["/home/pi/derby/lib/derbyshared.jar","/home/pi/derby/lib/derbyclient.jar","/home/pi/derby/lib/derbytools.jar"]  # Replace with the actual path to your Derby JDBC driver
jpype.startJVM(jpype.getDefaultJVMPath(), "-Djava.class.path=" + ":".join(derby_jars))

# Connect to the Derby database
conn = jaydebeapi.connect(
    "org.apache.derby.jdbc.ClientDriver",
    "jdbc:derby://localhost:1527/mydatabase;create=true",
    [],
    derby_jars,
)

# Create a table for sensor data if it doesn't exist
cursor = conn.cursor()
cursor.execute("SELECT * FROM SYS.SYSTABLES WHERE tablename='SENSOR_DATA'")
if cursor.fetchone() is None:
    cursor.execute("""
        CREATE TABLE sensor_data (
            timestamp TIMESTAMP,
            temperature DECIMAL,
            humidity DECIMAL,
            light_sensor DECIMAL,
            soil_moisture DECIMAL
        )
    """)


# Firebase configuration
firebase_config = {
    "apiKey": "AIzaSyBHxe48Z2xCDF3UZOR5DOWcXfdxoMvdeaE",
    "authDomain": "irrigation-intelligente-b8c1b.firebaseapp.com",
    "databaseURL": "https://irrigation-intelligente-b8c1b-default-rtdb.europe-west1.firebasedatabase.app",
    "projectId": "irrigation-intelligente-b8c1b",
    "storageBucket": "irrigation-intelligente-b8c1b.appspot.com",
    "messagingSenderId": "1092335549244",
    "appId": "1:1092335549244:web:2eed7b0e30fc98f6b23c39",
    "measurementId": "G-9GD7H2V917"

}

# Initialize Firebase
firebase = pyrebase.initialize_app(firebase_config)
firebase_db = firebase.database()

while True:
    # Read data from Bluetooth module
    data = bluetooth.readline().decode().strip()

    # Parse the comma-separated values
    temperature, humidity, light_sensor, soil_moisture = map(float, data.split(','))

    # Convert light sensor value to percentage
    light_percentage = (light_sensor / 980) * 100

    # Convert soil moisture value to percentage
    soil_percentage = 100 - ((soil_moisture - 200) / (1100 - 200)) * 100

    # Get the current timestamp
    timestamp = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    # Insert the data into the Derby database
    cursor.execute(
        "INSERT INTO sensor_data VALUES (?, ?, ?, ?, ?)",
        (timestamp, temperature, humidity, light_percentage, soil_percentage),
    )
    conn.commit()

    # Insert the data into the Firebase Realtime Database
    firebase_data = {
        'timestamp': timestamp,
        'temperature': temperature,
        'humidity': humidity,
        'lightSensor': light_percentage,
        'soilMoisture': soil_percentage
    }
    firebase_db.child('data').push(firebase_data)

    # Wait for some time before reading the next data
    time.sleep(1)

# Close the Bluetooth connection, Derby database connection, and JVM
bluetooth.close()
cursor.close()
conn.close()
jpype.shutdownJVM()

