#include <OneWire.h>
#include <ArduinoJson.h>

/**
 * Refined Weather Station Firmware (Single Node)
 * Optimized for Arduino Nano
 * 
 * Features:
 * - Interrupt-driven wind speed measurement (D2)
 * - OneWire temperature sensing with family detection (D5)
 * - Randomized sampling jitter (450ms-550ms) to prevent airfield signal collisions
 * - Modern JSON protocol compatible with Android app (WS_ prefix)
 */

// --- Pin Definitions ---
#define PIN_ONEWIRE 5
#define PIN_ANEMOMETER 2
#define BAUDRATE 9600
#define SERIAL_TIMEOUT 300

// --- Sensor Objects ---
OneWire ds(PIN_ONEWIRE);
volatile byte pulseCount = 0;

// --- State Variables ---
unsigned long previousMillis = 0;
int currentInterval = 500;
double lastTemp = 20.0;
double lastWind = 0.0;
byte dsAddr[8];
bool dsConnected = false;
bool isDS18S20 = false;

// --- Constants ---
const double WIND_CALIBRATION = 2.25; 

void setup() {
  Serial.begin(BAUDRATE);
  Serial.setTimeout(SERIAL_TIMEOUT);
  
  // 1. Initialize Anemometer
  pinMode(PIN_ANEMOMETER, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER), onWindPulse, CHANGE);
  
  // 2. Initialize Temperature Sensor & Identify Family
  if (!ds.search(dsAddr)) {
    Serial.println(F("Status: No OneWire sensors found."));
    ds.reset_search();
  } else {
    if (OneWire::crc8(dsAddr, 7) == dsAddr[7]) {
      dsConnected = true;
      if (dsAddr[0] == 0x10) {
        isDS18S20 = true;
        Serial.println(F("Status: DS18S20 detected."));
      } else if (dsAddr[0] == 0x28) {
        isDS18S20 = false;
        Serial.println(F("Status: DS18B20 detected."));
      }
    } else {
      Serial.println(F("Error: OneWire CRC failed."));
    }
  }

  // Trigger initial conversion
  if (dsConnected) {
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
  
  if (currentMillis - previousMillis >= currentInterval) {
    double timeDelta = (currentMillis - previousMillis) / 1000.0;
    
    // 1. Calculate Wind Speed
    detachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER));
    double wind = (pulseCount / 2.0) * (WIND_CALIBRATION / timeDelta);
    pulseCount = 0;
    attachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER), onWindPulse, CHANGE);
    
    // Apply basic noise filtering
    if (wind >= 0 && wind < 50.0) {
      lastWind = wind;
    }

    // 2. Calculate Temperature
    if (dsConnected) {
      double newTemp = readDS18x20();
      
      // --- Temperature Sanity Filter ---
      // 85.0 is the DS18B20 power-on default (before first conversion)
      // -127.0 is the OneWire "not found" error value
      if (newTemp != 85.0 && newTemp > -30.0 && newTemp < 60.0) {
        lastTemp = newTemp;
      }
      
      // Start next conversion for the next loop
      ds.reset();
      ds.select(dsAddr);
      ds.write(0x44, 1);
    }

    // 3. Transmit Data
    sendPDU(lastWind, lastTemp);

    // 4. Update timing with jitter (450ms - 550ms)
    // Jitter helps prevent multiple stations from "clashing" on the same frequency
    previousMillis = currentMillis;
    currentInterval = random(450, 550);
  }
}

double readDS18x20() {
  byte data[12];
  ds.reset();
  ds.select(dsAddr);
  ds.write(0xBE); // Read Scratchpad

  for (int i = 0; i < 9; i++) {
    data[i] = ds.read();
  }

  int16_t raw = (data[1] << 8) | data[0];
  if (isDS18S20) {
    raw = raw << 3; 
    if (data[7] == 0x10) {
      raw = (raw & 0xFFF0) + 12 - data[6];
    }
  }
  return (double)raw / 16.0;
}

void sendPDU(double wind, double temp) {
  StaticJsonBuffer<200> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  
  root["version"] = 2;
  root["numberOfNodes"] = 1;
  
  JsonArray& measurements = root.createNestedArray("measurements");
  JsonObject& m0 = measurements.createNestedObject();
  m0["windSpeed"] = wind;
  m0["temperature"] = temp;
  m0["nodeId"] = 0;

  // Prefix must be "WS_" for the current Android parser
  Serial.print("WS_");
  root.printTo(Serial);
  Serial.println("_end");
}
