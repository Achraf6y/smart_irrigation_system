#include <SoftwareSerial.h>
#include <dht.h>

dht DHT;

#define DHT11_PIN 7          // Replace with your DHT11 sensor's data pin
#define DHTTYPE DHT11     // Set DHT11 as the sensor type
#define LIGHTPIN A2       // Replace with your light sensor's analog input pin
#define SOILPIN A0        // Replace with your soil moisture sensor's analog input pin
#define RELAY_PIN 2       // Replace with the Arduino pin connected to the relay module
SoftwareSerial BTSerial(0, 1); // RX, TX

void setup()
{
  Serial.begin(9600);
  BTSerial.begin(9600);
  pinMode(RELAY_PIN, OUTPUT); // Set the relay pin as an output
  digitalWrite(RELAY_PIN, LOW); // Initialize the relay as off
}

void loop()
{
  // READ DATA
  int chk = DHT.read11(DHT11_PIN);

  float humidity = DHT.humidity;
  float temperature = DHT.temperature;
  int light = analogRead(LIGHTPIN);
  int soil_moisture = analogRead(SOILPIN);
  
  // DISPLAY DATA
  /*Serial.print(humidity);
  Serial.print(",");
  Serial.print(temperature);
  Serial.print(",");
  Serial.print(light);
  Serial.print(",");
  Serial.println(soil_moisture);
  */

  // Convert sensor data to comma-separated string
  String data = String(temperature) + "," + String(humidity) + "," + String(light) + "," + String(soil_moisture);
  Serial.println(data);
  
  // Send data via Bluetooth
  BTSerial.println(data);

  // Activate or stop the water pump based on soil moisture level
  if (soil_moisture > 800) {
    activateWaterPump();
  } else if (soil_moisture < 550) {
    stopWaterPump();
  }

  delay(3000);
}

// Function to activate the water pump
void activateWaterPump() {
  digitalWrite(RELAY_PIN, HIGH); // Turn on the relay module
}

// Function to stop the water pump
void stopWaterPump() {
  digitalWrite(RELAY_PIN, LOW); // Turn off the relay module
}
