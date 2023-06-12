import datetime
import time
import pyrebase
import jaydebeapi
import jpype

# Load the Derby JDBC driver
derby_jars = ["/home/pi/Desktop/Test/derbyshared.jar", "/home/pi/Desktop/Test/derbyclient.jar", "/home/pi/Desktop/Test/derbytools.jar"]
jpype.startJVM(jpype.getDefaultJVMPath(), "-Djava.class.path=" + ":".join(derby_jars))

# Connect to the Derby database
conn = jaydebeapi.connect(
    "org.apache.derby.jdbc.ClientDriver",
    "jdbc:derby://localhost:1527/mydatabase",
    [],
    derby_jars,
)

# Create a Firebase app and authenticate
firebase_config = {
    "apiKey": "***",
    "authDomain": "***",
    "databaseURL": "***",
    "projectId": "***",
    "storageBucket": "***",
    "messagingSenderId": "***",
    "appId": "***",
    "measurementId": "***"
}

firebase = pyrebase.initialize_app(firebase_config)

# Get a reference to the Firebase Realtime Database
db = firebase.database()
while True :
	# Fetch the latest data from the Derby database
	cursor = conn.cursor()
	cursor.execute("SELECT * FROM SENSOR_DATA ORDER BY TIMESTAMP DESC FETCH FIRST ROW ONLY")
	row = cursor.fetchone()

	if row is not None:
		timestamp, temperature, humidity, light_sensor, soil_moisture = row

		# Convert light sensor value to percentage with 2 decimal places
		light_percentage = round(light_sensor, 2)

		# Convert soil moisture value to percentage with 2 decimal places
		soil_percentage = round(soil_moisture, 2)

		# Format the timestamp as a string
		#formatted_timestamp = timestamp.strftime("%Y-%m-%d %H:%M:%S")

		# Construct the data to be inserted into Firebase
		firebase_data = {
			'timestamp': timestamp,
			'temperature': temperature,
			'humidity': humidity,
			'lightSensor': light_percentage,
			'soilMoisture': soil_percentage
		}

		# Insert the data into the Firebase Realtime Database
		db.child('data').push(firebase_data)
	
	time.sleep(1)
		
# Close the database cursor and connection
cursor.close()
conn.close()
jpype.shutdownJVM()
