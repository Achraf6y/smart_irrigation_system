#include <SoftwareSerial.h>
#include <dht.h>

dht DHT;

#define DHT11_PIN 7          // Replace with your DHT11 sensor's data pin
#define DHTTYPE DHT11     // Set DHT11 as the sensor type
#define LIGHTPIN A2       // Replace with your light sensor's analog input pin
#define SOILPIN A0        // Replace with your soil moisture sensor's analog input pin
SoftwareSerial BTSerial(0, 1); // RX, TX


void setup()
{
  Serial.begin(9600);
  BTSerial.begin(9600);

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
  
  delay(3000);
}
