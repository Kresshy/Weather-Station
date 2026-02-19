#include <OneWire.h>
#include <ArduinoJson.h>

/**
 * Weather Station Firmware (Single Node)
 * Optimized for Arduino Nano / Micro
 *
 * Measures Wind Speed (via Interrupt on D2) and Temperature (via OneWire on D5).
 * Sends data to the Android app via Bluetooth (Hardware Serial D0/D1).
 * 
 * Protocol: WS_{JSON}_end
 */

// --- Configuration ---
#define PIN_ONEWIRE 5
#define PIN_ANEMOMETER 2
#define SAMPLING_INTERVAL 1000 // ms
#define BAUDRATE 9600

// --- Sensor Objects ---
OneWire ds(PIN_ONEWIRE);
volatile byte pulseCount = 0;

// --- State Variables ---
unsigned long previousMillis = 0;
double currentTemp = 20.0;
double currentWind = 0.0;
byte dsAddr[8];
bool dsFound = false;

// --- Constants ---
// Calibration factor: pulses per meter/second. 
// Standard anemometers: (pulses/2) * (2.25 / (freq / 100))
const double WIND_CALIBRATION = 2.25; 

void setup() {
  Serial.begin(BAUDRATE);
  
  // Initialize Anemometer (Interrupt on D2)
  pinMode(PIN_ANEMOMETER, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER), onWindPulse, CHANGE);
  
  // Initialize Temperature Sensor (OneWire)
  if (ds.search(dsAddr)) {
    if (OneWire::crc8(dsAddr, 7) == dsAddr[7]) {
      dsFound = true;
    }
  }
  ds.reset_search();

  // Initial sensor start
  if (dsFound) {
    ds.reset();
    ds.select(dsAddr);
    ds.write(0x44, 1);
  }
}

void onWindPulse() {
  pulseCount++;
}

void loop() {
  unsigned long currentMillis = millis();
  
  if (currentMillis - previousMillis >= SAMPLING_INTERVAL) {
    double timeDelta = (currentMillis - previousMillis) / 1000.0;
    
    // Calculate Wind Speed
    detachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER));
    currentWind = (pulseCount / 2.0) * (WIND_CALIBRATION / timeDelta);
    pulseCount = 0;
    attachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER), onWindPulse, CHANGE);
    
    // Clamp wind to realistic range
    if (currentWind < 0 || currentWind > 50) currentWind = 0;

    // Calculate Temperature
    if (dsFound) {
      currentTemp = getTemperature();
      // Start next conversion immediately
      ds.reset();
      ds.select(dsAddr);
      ds.write(0x44, 1);
    }

    sendData(currentWind, currentTemp);
    previousMillis = currentMillis;
  }
}

/**
 * Reads the raw temperature from the DS18B20/DS18S20 scratchpad.
 */
double getTemperature() {
  byte data[12];
  ds.reset();
  ds.select(dsAddr);
  ds.write(0xBE); // Read Scratchpad

  for (int i = 0; i < 9; i++) {
    data[i] = ds.read();
  }

  int16_t raw = (data[1] << 8) | data[0];
  if (dsAddr[0] == 0x10) { // DS18S20 legacy adjustment
    raw = raw << 3; 
    if (data[7] == 0x10) {
      raw = (raw & 0xFFF0) + 12 - data[6];
    }
  }
  return (double)raw / 16.0;
}

/**
 * Packages sensor data into a JSON PDU and sends it over Serial.
 * Format: WS_{"version":2,"numberOfNodes":1,"measurements":[{"windSpeed":X,"temperature":Y,"nodeId":0}]}_end
 */
void sendData(double wind, double temp) {
  StaticJsonBuffer<200> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  
  root["version"] = 2;
  root["numberOfNodes"] = 1;
  
  JsonArray& measurements = root.createNestedArray("measurements");
  JsonObject& m0 = measurements.createNestedObject();
  m0["windSpeed"] = wind;
  m0["temperature"] = temp;
  m0["nodeId"] = 0;

  Serial.print("WS_");
  root.printTo(Serial);
  Serial.println("_end");
}
