#include <OneWire.h>
#include <SoftwareSerial.h>

#define DS18S20_ID 0x10
#define DS18B20_ID 0x28
#define ROBOT_NAME "Weather Station 00"
#define BLUETOOTH_SPEED 9600

SoftwareSerial mySerial(0, 1); // RX, TX
String command = "";

int sensorPin = A2;    // select the input pin for the potentiometer
int ledPin = 13;      // select the pin for the LED
int sensorValue = 0;  // variable to store the value coming from the sensor
int sensorState = LOW;
int previousSensorState = LOW;
int frequency = 0; // frequency of the anemometer
int interval = 1000; // sampling interval (milliseconds)
const int UNIT_FREQ = 100; // 1 m/s = 100 freq
unsigned long previousMillis = 0;
unsigned long currentMillis = 0;

byte i;
byte present = 0;
byte data[12];
byte addr[8];


OneWire ds(5);

void setup() {
  // declare the ledPin as an OUTPUT:
  pinMode(ledPin, OUTPUT);  
  Serial.begin(9600);
  mySerial.begin(9600);
  delay(1000);
  
  if(!ds.search(addr)) {
   Serial.print("No more addresses.\n");
   ds.reset_search();
   return;
  }
  
  Serial.print("R=");
  
  for (i = 0; i< 8; i++) {
   Serial.print(addr[i], HEX);
   Serial.print(" ");
  }
  
  if ( OneWire::crc8( addr, 7) != addr[7]) {
   Serial.print("CRC is not valid!\n");
   return;
  }
  
  if ( addr[0] == 0x10) {
      Serial.print("Device is a DS18S20 family device.\n");
  }
  else if ( addr[0] == 0x28) {
      Serial.print("Device is a DS18B20 family device.\n");
  }
  else {
      Serial.print("Device family is not recognized: 0x");
      Serial.println(addr[0],HEX);
      return;
  }
  
  ds.reset();
  ds.select(addr);
  ds.write(0x44,1);
  
}

void loop() {
  // read the value from the sensor:
  sensorValue = analogRead(sensorPin);
  
  if (sensorValue < 300)
    sensorState = LOW;
  else 
    sensorState = HIGH;
  
  if (sensorState != previousSensorState) {
    frequency++;
    previousSensorState = sensorState;
    // Serial.println(frequency/2);
  }
  
  currentMillis = millis();
  
  if (currentMillis - previousMillis >= interval) {

    previousMillis = currentMillis;
    float windSpeed = (float) frequency / UNIT_FREQ;
    float temperature = getTemp();
    Serial.print("wind: ");
    Serial.print(windSpeed);
    Serial.print(" m/s temperature: ");
    Serial.println(temperature);

    if (mySerial.available()) {
      mySerial.write("wind: ");
      mySerial.write(windSpeed);
      mySerial.write(" m/s temperature: ");
      mySerial.write(temperature); 
    }

    frequency = 0;
  }
}


float getTemp() {
  //returns the temperature from one DS18S20 in DEG Celsius

  byte data[12];
  byte addr[8];

  if ( !ds.search(addr)) {
    //no more sensors on chain, reset search
    ds.reset_search();
    return -1000;
  }

  if ( OneWire::crc8( addr, 7) != addr[7]) {
    Serial.println("CRC is not valid!");
    return -1000;
  }

  if ( addr[0] != 0x10 && addr[0] != 0x28) {
     Serial.print("Device is not recognized");
    return -1000;
  }

  ds.reset();
  ds.select(addr);
  ds.write(0x44,1); // start conversion, with parasite power on at the end

  byte present = ds.reset();
  ds.select(addr);
  ds.write(0xBE); // Read Scratchpad


  for (int i = 0; i < 9; i++) { // we need 9 bytes
    data[i] = ds.read();
  }

  ds.reset_search();

  byte MSB = data[1];
  byte LSB = data[0];

  float tempRead = ((MSB << 8) | LSB); //using two's compliment
  float TemperatureSum = tempRead / 16;

  return TemperatureSum;
}
