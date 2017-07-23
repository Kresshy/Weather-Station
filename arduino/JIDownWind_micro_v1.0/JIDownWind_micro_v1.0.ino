
#include <OneWire.h>
#include <ArduinoJson.h>

/* DS18S20 Temperature chip i/o */
#define DS18S20_ID 0x10
#define DS18B20_ID 0x28

OneWire  ds(5);  // on pin 5

String inputString = "";         // a string to hold incoming data
boolean stringComplete = false;  // whether the string is complete
int interval = 999; // sampling interval (milliseconds)
unsigned long previousMillis = 0;
unsigned long currentMillis = 0;
const int timeout = 800;
float TempTable[2] = { 0.0, 0.0 };
float WindTable[2] = { 0.0, 0.0 };
boolean Node1 = false;



// Temperature
byte i;
byte addr[8];
double TemperatureSum;
double temp;
double tempRange[2] = { -35, 45 };


// Windspeed
volatile byte pulsecount;
double windspeed;
int sensorVal;
const byte interruptPin = 2;
double windRange[2] = { 0, 15};

 
void setup() {
 Serial.begin(9600);
 Serial.setTimeout(timeout);
 Serial1.begin(9600);
 Serial1.setTimeout(timeout);
 inputString.reserve(200);

 // Temperature
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
  
// Windspeed
  pinMode(interruptPin, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(interruptPin), wind_pulse, CHANGE);
  pulsecount = 0;  
}

void wind_pulse(){
    pulsecount++;
}

void loop() {  
 if (stringComplete) {
    parseSerialData(inputString);
    inputString = "";
    stringComplete = false;
  }  
  currentMillis = millis();  
  if (currentMillis > (previousMillis + interval)){
    int timeStamp = currentMillis - previousMillis;
    if(timeStamp > interval) { interval - 1; }
    if(timeStamp < interval) { interval + 1; }
    getTemp();
    getWindSpeed();
    parseSerialData(inputString);
    sendJSONData();
    previousMillis = currentMillis;  
  }
}
void parseSerialData(String data){  
  int ind1 = data.indexOf('_') +1 ;  //finds location of first ,
  int ind2 = data.indexOf('_', ind1+1 );   //finds location of second
  String json = data.substring(ind1,ind2);
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.parseObject(json);
  if (root.success()) {
    JsonObject& root2 = root["measurements"][0];
    double upWind = root2["windSpeed"];
    double upTemp = root2["temperature"];
    jsonBuffer.clear();
    if((upWind >= windRange[0]) && (upWind < windRange[1])){
      WindTable[1] = upWind;
    }
    if((upTemp > tempRange[0]) && (upTemp < tempRange[1])){
     TempTable[1] = upTemp;
    }
  }
}
void serialEvent1() {
  while (Serial1.available()) {
    // get the new byte:
    char inChar = (char)Serial1.read();
    // add it to the inputString:
    inputString += inChar;
    // if the incoming character is a newline, set a flag
    // so the main loop can do something about it:
    if (inChar == '\n') {
      Node1 = true;
      stringComplete = true;
    }
  }
}
void sendJSONData()
{
  int nodeCount = 1;
  DynamicJsonBuffer jsonBuffer;
  JsonObject& node0 = jsonBuffer.createObject();
  node0["windSpeed"] = WindTable[0];
  node0["temperature"] = TempTable[0];
  node0["nodeId"] = 0;
  JsonObject& node1 = jsonBuffer.createObject();
  if(Node1){
   nodeCount++; 
  }
  node1["windSpeed"] = WindTable[1];
  node1["temperature"] = TempTable[1];
  node1["nodeId"] = 1;        
  
  JsonObject& root = jsonBuffer.createObject();
  root["version"] = 2;
  root["numberOfNodes"] = nodeCount;
  JsonArray& data = root.createNestedArray("measurements");
  data.add(node0);
  if(Node1){
    data.add(node1);
  }
  jsonBuffer.clear();
  //root.printTo(Serial);
  //Serial.print("\n");
  Serial1.print("start_");
  root.printTo(Serial1);
  Serial1.print("_end\n");
  
  Node1 = false;
}
void getWindSpeed(){
  detachInterrupt(digitalPinToInterrupt(interruptPin));
  currentMillis = millis();
  double freq = currentMillis - previousMillis;
  double wind = (pulsecount/2) * (2.25/(freq / 100)); 
  pulsecount = 0;
  previousMillis = currentMillis;
  attachInterrupt(digitalPinToInterrupt(interruptPin), wind_pulse, CHANGE);
  if((wind >= windRange[0]) && (wind < windRange[1])){
    WindTable[0] = wind;
  }
  else {
    WindTable[0] = WindTable[0];
  }  
}
void getTemp() {
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
       TempTable[0] = TemperatureSum;
  }
  else{
    TempTable[0] = TempTable[0];
  }
}
