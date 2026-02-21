#include <OneWire.h>
#include <ArduinoJson.h>

/**
 * Weather Station Firmware v2.0
 * Optimized for Arduino Nano / HC-05 Bluetooth
 * 
 * This firmware implements a high-precision, non-blocking weather monitor
 * designed for free-flight aeromodelling airfield deployment.
 * 
 * Hardware Connections:
 * - D2: Anemometer (Interrupt-driven, Pull-up required)
 * - D5: DS18B20 / DS18S20 Temperature Sensor (OneWire)
 * - TX/RX: HC-05 Bluetooth Module (Configured at 9600 baud)
 * 
 * Features:
 * - Non-blocking timing loop (millis based)
 * - Randomized sampling jitter to prevent signal collisions on the airfield
 * - Automatic DS18x20 family detection and high-resolution conversion
 * - Robust JSON-framed serial protocol
 */

// --- Configuration ---
#define PIN_ONEWIRE 5
#define PIN_ANEMOMETER 2
#define BAUDRATE 9600
#define SERIAL_TIMEOUT 300

// --- Sensor Objects ---
OneWire oneWire(PIN_ONEWIRE);
volatile byte pulseCount = 0; // Incremented by ISR on every anemometer click

// --- State Variables ---
unsigned long previousMillis = 0;
int currentInterval = 500;    // Target sampling interval in ms
double lastValidTemp = 20.0;
double lastValidWind = 0.0;
byte sensorAddr[8];
bool isSensorFound = false;
bool isFamilyS20 = false;     // True for DS18S20, False for DS18B20

// --- Calibration Constants ---
// Adjust this based on your specific anemometer cup size
// Standard hobby anemometer: 2.25 m/s per 1 rotation/sec
const double WIND_CALIBRATION = 2.25; 

/**
 * Entry point: Configures hardware pins, serial communication,
 * and initializes the OneWire temperature sensor.
 */
void setup() {
  Serial.begin(BAUDRATE);
  Serial.setTimeout(SERIAL_TIMEOUT);
  
  // 1. Initialize Anemometer
  // Uses CHANGE interrupt to count both rising and falling edges
  pinMode(PIN_ANEMOMETER, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER), onWindPulse, CHANGE);
  
  // 2. Initialize Temperature Sensor
  initializeTemperatureSensor();

  // Trigger the first conversion immediately
  if (isSensorFound) {
    startTemperatureConversion();
  }
}

/**
 * Main Loop: Orchestrates the non-blocking sampling cycle.
 * Calculates averages and transmits a framed JSON packet.
 */
void loop() {
  unsigned long currentMillis = millis();
  
  // Check if it's time for the next sample
  if (currentMillis - previousMillis >= (unsigned long)currentInterval) {
    double timeDeltaSeconds = (currentMillis - previousMillis) / 1000.0;
    
    // Safety check to prevent division by zero
    if (timeDeltaSeconds < 0.001) timeDeltaSeconds = 0.001;

    // 1. Calculate Wind Speed
    // Detach interrupt briefly to ensure atomicity of the pulse count read
    detachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER));
    
    // Two edges (CHANGE) per pulse, so divide by 2.0
    double wind = (pulseCount / 2.0) * (WIND_CALIBRATION / timeDeltaSeconds);
    pulseCount = 0;
    
    attachInterrupt(digitalPinToInterrupt(PIN_ANEMOMETER), onWindPulse, CHANGE);
    
    // Basic noise filtering: speeds > 50m/s are likely electrical noise
    if (wind >= 0 && wind < 50.0) {
      lastValidWind = wind;
    }

    // 2. Process Temperature
    if (isSensorFound) {
      double newTemp = readTemperatureFromSensor();
      
      // Sanity Filter:
      // 85.0 is the sensor power-on default (read before conversion is ready)
      // -127.0 is the OneWire "not found" error
      if (newTemp != 85.0 && newTemp > -35.0 && newTemp < 65.0) {
        lastValidTemp = newTemp;
      }
      
      // Start the next conversion cycle (takes ~750ms)
      startTemperatureConversion();
    }

    // 3. Transmit the data frame to the Android app
    sendTelemetryPacket(lastValidWind, lastValidTemp);

    // 4. Update timing with jitter
    // We add 10% random jitter to the 500ms base interval to prevent
    // multiple stations from transmitting at the exact same microsecond.
    previousMillis = currentMillis;
    currentInterval = random(450, 550);
  }
}

/**
 * Interrupt Service Routine (ISR) triggered by the anemometer.
 * Must be kept extremely lean.
 */
void onWindPulse() {
  pulseCount++;
}

/**
 * Scans the OneWire bus for a DS18x20 series sensor and
 * identifies its family code for proper temperature scaling.
 */
void initializeTemperatureSensor() {
  if (!oneWire.search(sensorAddr)) {
    Serial.println(F("Status: No OneWire sensors found."));
    oneWire.reset_search();
    isSensorFound = false;
  } else {
    if (OneWire::crc8(sensorAddr, 7) == sensorAddr[7]) {
      isSensorFound = true;
      if (sensorAddr[0] == 0x10) {
        isFamilyS20 = true;
        Serial.println(F("Status: DS18S20 detected."));
      } else if (sensorAddr[0] == 0x28) {
        isFamilyS20 = false;
        Serial.println(F("Status: DS18B20 detected."));
      }
    } else {
      Serial.println(F("Error: OneWire CRC failed."));
      isSensorFound = false;
    }
  }
}

/**
 * Signals the DS18x20 sensor to begin a temperature-to-digital conversion.
 */
void startTemperatureConversion() {
  oneWire.reset();
  oneWire.select(sensorAddr);
  oneWire.write(0x44, 1); // 0x44 = Start Conversion
}

/**
 * Reads the 9-byte scratchpad from the temperature sensor and
 * converts the raw data to a Celsius double.
 */
double readTemperatureFromSensor() {
  byte data[12];
  oneWire.reset();
  oneWire.select(sensorAddr);
  oneWire.write(0xBE); // 0xBE = Read Scratchpad

  for (int i = 0; i < 9; i++) {
    data[i] = oneWire.read();
  }

  int16_t raw = (data[1] << 8) | data[0];
  if (isFamilyS20) {
    // DS18S20 has a fixed 9-bit resolution
    raw = raw << 3; 
    if (data[7] == 0x10) {
      raw = (raw & 0xFFF0) + 12 - data[6];
    }
  }
  return (double)raw / 16.0;
}

/**
 * Formats sensor data into a JSON packet and transmits it via Serial.
 * Format: "WS_{JSON_DATA}_end"
 */
void sendTelemetryPacket(double wind, double temp) {
  StaticJsonBuffer<200> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  
  root["version"] = 2;
  root["numberOfNodes"] = 1;
  
  JsonArray& measurements = root.createNestedArray("measurements");
  JsonObject& m0 = measurements.createNestedObject();
  m0["windSpeed"] = wind;
  m0["temperature"] = temp;
  m0["nodeId"] = 0;

  // Frame synchronization markers for the Android app
  Serial.print("WS_");
  root.printTo(Serial);
  Serial.println("_end");
}
