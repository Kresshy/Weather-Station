#include <OneWire.h>
#include <ArduinoJson.h>

/**
 * Weather Station Firmware v2.1
 * Optimized for Arduino Nano / HC-05 Bluetooth
 * 
 * High-precision, non-blocking weather monitor designed for 
 * custom 4-cup carbon-fiber anemometers with 5-window optical encoders.
 */

// --- Configuration ---
#define PIN_ONEWIRE 5
#define PIN_ANEMOMETER 2
#define BAUDRATE 9600
#define SERIAL_TIMEOUT 300

// --- Sensor Objects ---
OneWire oneWire(PIN_ONEWIRE);
// volatile unsigned int for safety: Prevents overflow at high wind speeds (5 windows/rev)
volatile unsigned int pulseCount = 0; 

// --- State Variables ---
unsigned long previousMillis = 0;
int currentInterval = 500;    // Target sampling interval in ms
double lastValidTemp = 20.0;
double lastValidWind = 0.0;
byte sensorAddr[8];
bool isSensorFound = false;
bool isFamilyS20 = false;     // True for DS18S20, False for DS18B20

// --- Calibration Constants (Custom 4-Cup 5-Window Carbon Anemometer) ---
// R = 28.5mm, 4 cups, 5 pulses/rev, K = 2.7
// Formula: v = (Hz * WIND_SCALE) + WIND_OFFSET
const double WIND_SCALE = 0.097;   // Calculated: 0.0967 m/s per pulse/sec
const double WIND_OFFSET = 0.05;  // Tunable: Minimal friction threshold

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

    // 1. Calculate Wind Speed (Atomic read of pulseCount)
    noInterrupts();
    unsigned int currentPulses = pulseCount;
    pulseCount = 0;
    interrupts();
    
    // Convert edges (CHANGE) to full pulses and calculate frequency
    double frequency = (currentPulses / 2.0) / timeDeltaSeconds;
    
    // Apply linear calibration if moving (> 0.01 Hz), otherwise 0
    double wind = (frequency > 0.01) ? (frequency * WIND_SCALE + WIND_OFFSET) : 0.0;
    
    // Basic noise filtering: speeds > 50m/s are likely electrical noise
    if (wind >= 0 && wind < 50.0) {
      lastValidWind = wind;
    }

    // 2. Process Temperature
    if (isSensorFound) {
      double newTemp = readTemperatureFromSensor();
      
      // Sanity Filter: 85.0 is sensor default, -127.0 is error
      if (newTemp != 85.0 && newTemp > -35.0 && newTemp < 65.0) {
        lastValidTemp = newTemp;
      }
      
      // Start the next conversion cycle (takes ~750ms)
      startTemperatureConversion();
    }

    // 3. Transmit the data frame to the Android app
    sendTelemetryPacket(lastValidWind, lastValidTemp);

    // 4. Update timing with jitter
    previousMillis = currentMillis;
    currentInterval = random(450, 550);
  }
}

/**
 * Interrupt Service Routine (ISR) triggered by the anemometer.
 */
void onWindPulse() {
  pulseCount++;
}

/**
 * Scans the OneWire bus for a DS18x20 series sensor.
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

void startTemperatureConversion() {
  oneWire.reset();
  oneWire.select(sensorAddr);
  oneWire.write(0x44, 1); 
}

double readTemperatureFromSensor() {
  byte data[12];
  oneWire.reset();
  oneWire.select(sensorAddr);
  oneWire.write(0xBE);

  for (int i = 0; i < 9; i++) {
    data[i] = oneWire.read();
  }

  int16_t raw = (data[1] << 8) | data[0];
  if (isFamilyS20) {
    raw = raw << 3; 
    if (data[7] == 0x10) {
      raw = (raw & 0xFFF0) + 12 - data[6];
    }
  }
  return (double)raw / 16.0;
}

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

  Serial.print("WS_");
  root.printTo(Serial);
  Serial.println("_end");
}
