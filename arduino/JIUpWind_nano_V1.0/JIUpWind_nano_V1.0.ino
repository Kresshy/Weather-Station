#include <OneWire.h>
#include <SoftwareSerial.h>
#include <ArduinoJson.h>

/* DS18S20 Temperature chip i/o */
#define DS18S20_ID 0x10
#define DS18B20_ID 0x28

SoftwareSerial mySerial(0, 1); // RX, TX
OneWire  ds(5);  // on pin 5

int interval = 500; // sampling interval (milliseconds)
unsigned long previousMillis = 0;
unsigned long currentMillis = 0;
const int timeout=300;

// Temperature
byte i;
byte addr[8];
double TemperatureSum;
double temp;
bool tempConnection;
float tempRange[2] = { -35.0, 45.0 };
double tempRangeMin;
double tempRangeMax;

// Windspeed
volatile byte pulsecount;
double windspeed;
float windRange[] = { 0, 15.0 };
int sensorVal;
const byte interruptPin = 2;

void setup(void) {
// Communication  
  Serial.begin(9600);
  Serial.setTimeout(timeout);
  mySerial.begin(9600);
  mySerial.setTimeout(timeout);

// Temperature
   if(!ds.search(addr)) {
   Serial.print("No more addresses.\n");
   tempConnection = false;
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
      tempConnection = true;
  }
  else if ( addr[0] == 0x28) {
      Serial.print("Device is a DS18B20 family device.\n");
      tempConnection = true;
  }
  else {
      Serial.print("Device family is not recognized: 0x");
      Serial.println(addr[0],HEX);
      tempConnection = false;
      return;
  }
  
  ds.reset();
  ds.select(addr);
  ds.write(0x44,1);
  tempRangeMin = -35;
  tempRangeMax = 45;

// Windspeed
  pinMode(interruptPin, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(interruptPin), wind_pulse, CHANGE);
  pulsecount = 0;
}

void wind_pulse(){
    pulsecount++;
}

void loop(void) {    
    interval = random(450, 550);  
     if (millis() > (previousMillis + interval)){
      temp = getTemp(temp);
      windspeed = getWindspeed(windspeed);
      if(tempConnection){
        //sendData(temp, windspeed);
        sendJSONData(temp, windspeed);
      }
      else{
        delay(1000);
        setup();
      }
      previousMillis = millis();
     }
}

void sendJSONData(double temp, double windspeed)
{
  int nodeCount = 1;
  DynamicJsonBuffer jsonBuffer;
  JsonObject& node0 = jsonBuffer.createObject();
  node0["windSpeed"] = windspeed;
  node0["temperature"] = temp;
  node0["nodeId"] = 0;
  JsonObject& root = jsonBuffer.createObject();
  root["version"] = 2;
  root["numberOfNodes"] = nodeCount;
  JsonArray& data = root.createNestedArray("measurements");
  data.add(node0);
  jsonBuffer.clear();
//  root.printTo(mySerial);
//  mySerial.print("\n");
  Serial.print("start_");
  root.printTo(Serial);
  Serial.print("_end\n");
}
/*
void sendData(double temp, double windspeed){
    String message = "start_";
    message += windspeed;    
    message += " ";
    message += temp;
    message += "_end\n";   
    mySerial.print(message);
    Serial.print(message);
}
*/
double getWindspeed(double oldWindspeed){
  detachInterrupt(digitalPinToInterrupt(interruptPin));
  currentMillis = millis();
  double freq = currentMillis - previousMillis;
  double wind = (pulsecount/2) * (2.25/(freq / 100)); 
  pulsecount = 0;
  previousMillis = currentMillis;
  attachInterrupt(digitalPinToInterrupt(interruptPin), wind_pulse, CHANGE);
  if((wind >= windRange[0]) && (wind < windRange[1])){
    return wind;
  }
  else {
    return oldWindspeed;
  }  
}

double getTemp(double oldTemp) {
  //returns the temperature from one DS18S20 in DEG Celsius
  byte data[12];
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

  double tempRead = ((MSB << 8) | LSB); //using two's compliment
  TemperatureSum = tempRead / 16;
  
  if((TemperatureSum > tempRange[0]) && (TemperatureSum < tempRange[1])){
    return TemperatureSum;
  }
  else{
    return oldTemp;
  }  
}
