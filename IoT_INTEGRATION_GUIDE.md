# IoT Integration Guide: Connecting Android App to Real Hardware

This guide explains how to connect your Android application to a real-world agricultural motor and sensor setup using an **ESP32** microcontroller over the internet using **MQTT**.

The app supports two electrical configurations:
- **Single Phase (1Φ)** — default, uses only the R-phase voltage and current sensors.
- **Three Phase (3Φ)** — enabled via Settings in the app, uses all R/Y/B voltage and current sensors.

---

## 1. System Architecture

```
┌────────────────┐             ┌─────────────┐             ┌──────────────────────┐
│  Android App   │ ◄─────────► │ MQTT Broker │ ◄─────────► │ ESP32 Controller     │
│ (Compose UI)   │    WiFi/    │  (HiveMQ /  │   WiFi /    │ (Sensors + Relays)   │
│  Dashboard     │    Cellular │  Mosquitto) │   Cellular  │ 1-Phase or 3-Phase   │
└────────────────┘             └─────────────┘             └──────────────────────┘
```

**MQTT Topics used:**
| Topic | Direction | Purpose |
|---|---|---|
| `agri/pump1/telemetry` | ESP32 → App | Sensor readings (voltage, current, flow, etc.) |
| `agri/pump1/command` | App → ESP32 | Motor ON/OFF, valve OPEN/CLOSED |
| `agri/pump1/mode` | App → ESP32 | Phase mode: `"SINGLE"` or `"THREE"` |

---

## 2. Hardware Components

### 2.1 Common Components (Both Modes)
| Component | Model | Purpose |
|---|---|---|
| Microcontroller | ESP32 (38-pin DevKit) | Main controller with WiFi |
| Flow Meter | YF-S201 | Water flow rate measurement |
| Soil Moisture Sensor | Capacitive (generic) | Soil moisture % |
| Motor Relay | 2-channel 5V relay | Motor starter control |
| Valve Relay | 1-channel 5V relay | Gate valve solenoid control |
| Power Supply | 5V 2A | Relay and sensor power |

### 2.2 Single Phase (1Φ) — Additional Components
| Component | Model | Qty | Purpose |
|---|---|---|---|
| Voltage Sensor | ZMPT101B | 1 | AC Voltage measurement (R-phase) |
| Current Transformer | SCT013-030 (30A) | 1 | AC Current measurement (R-phase) |
| Burden Resistor | 62Ω, 1/4W | 1 | For SCT013 current sensing |

### 2.3 Three Phase (3Φ) — Additional Components
| Component | Model | Qty | Purpose |
|---|---|---|---|
| Voltage Sensor | ZMPT101B | 3 | AC Voltage: one per R, Y, B phase |
| Current Transformer | SCT013-030 (30A) | 3 | AC Current: one per R, Y, B phase |
| Burden Resistor | 62Ω, 1/4W | 3 | One per SCT013 |

---

## 3. Wiring Diagrams

### 3.1 Single Phase (1Φ) Wiring

```
Single-Phase AC Supply (R + N)
    │
    ├─── R (Live) ──────────────────────── ZMPT101B VIN+ ───► ESP32 GPIO36 (ADC)
    │                                                  VIN-
    │    N (Neutral) ──────────────────── ZMPT101B GND
    │
    │    SCT013 (clip around R wire) ──── 62Ω burden ──► ESP32 GPIO39 (ADC)
    │
    ├─── Motor Relay (NC) ──────────────── Relay IN1 ──────── ESP32 GPIO25
    │         │                            Relay COM ──────── R (Live)
    │         │                            Relay NO  ──────── Motor Line 1
    │         └─── Motor Neutral ────────────────────────── Motor Line 2 (N)
    │
    └─── Valve Solenoid Relay ──────────── Relay IN2 ──────── ESP32 GPIO26
              │                            Relay COM ──────── 12V/24V supply
              └─── Solenoid Valve ──────── Relay NO  ──────── Valve +
                                           GND ──────────── Valve -

Flow Meter (YF-S201):
    VCC ── 5V
    GND ── GND
    OUT ── ESP32 GPIO27 (digital interrupt)

Soil Moisture Sensor:
    VCC ── 3.3V
    GND ── GND
    AOUT── ESP32 GPIO14 (ADC)

ESP32 5V ─── Relay Module VCC
ESP32 GND ── Relay Module GND
```

### 3.2 Three Phase (3Φ) Wiring

```
Three-Phase AC Supply (R + Y + B + N)
    │
    ├─── R Phase ─── ZMPT101B #1 VIN+ ──► ESP32 GPIO36 (ADC) [vr]
    │                            VIN-/GND
    │                SCT013 #1 (clip on R) ─ 62Ω ──► ESP32 GPIO39 (ADC) [ir]
    │
    ├─── Y Phase ─── ZMPT101B #2 VIN+ ──► ESP32 GPIO32 (ADC) [vy]
    │                            VIN-/GND
    │                SCT013 #2 (clip on Y) ─ 62Ω ──► ESP32 GPIO33 (ADC) [iy]
    │
    ├─── B Phase ─── ZMPT101B #3 VIN+ ──► ESP32 GPIO34 (ADC) [vb]
    │                            VIN-/GND
    │                SCT013 #3 (clip on B) ─ 62Ω ──► ESP32 GPIO35 (ADC) [ib]
    │
    ├─── Motor Relay (3-Phase Contactor) ── IN1 ──── ESP32 GPIO25
    │         Contactor T1/T2/T3 ─── R/Y/B Motor Terminals
    │
    └─── Valve Solenoid Relay ──────────── IN2 ──── ESP32 GPIO26

Flow Meter (YF-S201):
    OUT ── ESP32 GPIO27 (interrupt)

Soil Moisture:
    AOUT── ESP32 GPIO14 (ADC)
```

> ⚠️ **Safety Warning:** Working with mains AC voltage (230V/415V) is dangerous.
> Always isolate the supply before wiring. Use a certified electrician for live-wire connections.
> ZMPT101B and SCT013 modules are designed for safe low-voltage output to the ESP32.

---

## 4. Step 1: Add MQTT Dependency to Android Project

Add the Eclipse Paho Java MQTT library to your Version Catalog and Gradle build file.

### In [libs.versions.toml](file:///d:/Development/Mobile/gradle/libs.versions.toml):
```toml
[versions]
pahoMqtt = "1.2.5"

[libraries]
paho-mqtt = { group = "org.eclipse.paho", name = "org.eclipse.paho.client.mqttv3", version.ref = "pahoMqtt" }
```

### In [app/build.gradle.kts](file:///d:/Development/Mobile/app/build.gradle.kts):
```kotlin
dependencies {
    implementation(libs.paho.mqtt)
}
```

---

## 5. Step 2: Implement MQTT Client Helper in Android

Create a helper class to handle background connections, subscriptions, and publication.

### Create `MqttHelper.kt` in `com.agri.motorcontrol.data`:
```kotlin
package com.agri.motorcontrol.data

import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

class MqttHelper(
    private val brokerUrl: String = "tcp://broker.hivemq.com:1883",
    private val clientId: String = "AgriAndroidClient_" + System.currentTimeMillis(),
    private val onTelemetryReceived: (Telemetry) -> Unit,
    private val onConnectionStatusChanged: (Boolean) -> Unit
) {
    private var mqttClient: MqttClient? = null

    fun connect() {
        try {
            mqttClient = MqttClient(brokerUrl, clientId, null)
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 60
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    onConnectionStatusChanged(false)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic == "agri/pump1/telemetry" && message != null) {
                        val payload = String(message.payload)
                        val telemetry = parseJsonToTelemetry(payload)
                        if (telemetry != null) {
                            onTelemetryReceived(telemetry)
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            mqttClient?.connect(options)
            mqttClient?.subscribe("agri/pump1/telemetry", 1)
            onConnectionStatusChanged(true)

        } catch (e: MqttException) {
            e.printStackTrace()
            onConnectionStatusChanged(false)
        }
    }

    /** Publish motor/valve commands to ESP32 */
    fun publishCommand(motorOn: Boolean, valveOpen: Boolean) {
        try {
            if (mqttClient?.isConnected == true) {
                val json = JSONObject().apply {
                    put("motor", if (motorOn) "ON" else "OFF")
                    put("valve", if (valveOpen) "OPEN" else "CLOSED")
                }
                val message = MqttMessage(json.toString().toByteArray()).apply { qos = 1 }
                mqttClient?.publish("agri/pump1/command", message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Publish phase mode to ESP32 so firmware knows which sensors to read */
    fun publishPhaseMode(mode: PhaseMode) {
        try {
            if (mqttClient?.isConnected == true) {
                val json = JSONObject().apply {
                    put("mode", if (mode == PhaseMode.THREE_PHASE) "THREE" else "SINGLE")
                }
                val message = MqttMessage(json.toString().toByteArray()).apply { qos = 1 }
                mqttClient?.publish("agri/pump1/mode", message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseJsonToTelemetry(jsonStr: String): Telemetry? {
        return try {
            val json = JSONObject(jsonStr)
            val modeStr = json.optString("mode", "SINGLE")
            val phaseMode = if (modeStr == "THREE") PhaseMode.THREE_PHASE else PhaseMode.SINGLE_PHASE
            Telemetry(
                voltageR = json.optDouble("vr", 220.0).toFloat(),
                voltageY = json.optDouble("vy", 220.0).toFloat(),
                voltageB = json.optDouble("vb", 220.0).toFloat(),
                currentR = json.optDouble("ir", 0.0).toFloat(),
                currentY = json.optDouble("iy", 0.0).toFloat(),
                currentB = json.optDouble("ib", 0.0).toFloat(),
                flowRate = json.optDouble("flow", 0.0).toFloat(),
                valveOpen = json.optString("valve", "OPEN") == "OPEN",
                soilMoisture = json.optInt("moist", 50),
                tankLevel = json.optInt("tank", 80),
                phaseMode = phaseMode
            )
        } catch (e: Exception) {
            null
        }
    }

    fun disconnect() {
        mqttClient?.disconnect()
    }
}
```

---

## 6. Step 3: ESP32 Firmware (Arduino C++)

Flash this firmware to your ESP32. It auto-detects the phase mode received from the Android app and reads the appropriate sensors.

### Required Arduino Libraries
Install via Arduino IDE → Sketch → Include Library → Manage Libraries:
- **PubSubClient** by Nick O'Leary
- **ArduinoJson** by Benoit Blanchon (v6.x)

```cpp
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// ─── WiFi Settings ────────────────────────────────────────────────
const char* ssid     = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// ─── MQTT Broker ──────────────────────────────────────────────────
const char* mqtt_server = "broker.hivemq.com";
const int   mqtt_port   = 1883;

// ─── Pin Definitions ──────────────────────────────────────────────
// Common pins
#define MOTOR_RELAY_PIN  25   // Motor contactor relay
#define VALVE_RELAY_PIN  26   // Gate valve solenoid relay
#define FLOW_SENSOR_PIN  27   // YF-S201 flow meter (interrupt)
#define MOISTURE_PIN     14   // Capacitive soil moisture sensor

// Single Phase (1Φ) — R phase only
#define V_R_PIN  36   // ZMPT101B voltage sensor – R phase
#define I_R_PIN  39   // SCT013 current clamp   – R phase

// Additional Three Phase (3Φ) pins
#define V_Y_PIN  32   // ZMPT101B – Y phase
#define I_Y_PIN  33   // SCT013   – Y phase
#define V_B_PIN  34   // ZMPT101B – B phase
#define I_B_PIN  35   // SCT013   – B phase

// ─── Calibration Constants ────────────────────────────────────────
// Adjust VCAL until readVoltageRMS() returns your supply voltage (~230V)
#define VCAL    110.0f
// Adjust ICAL until readCurrentRMS() matches a clamp meter reading
#define ICAL     30.0f
#define VREF      3.3f
#define ADC_RES  4095.0f

// ─── Mode State ──────────────────────────────────────────────────
bool threePhaseMode = false;   // Default: Single Phase (matches app default)

// ─── Flow Meter ───────────────────────────────────────────────────
volatile int flowPulses = 0;
void IRAM_ATTR pulseCounter() { flowPulses++; }

// ─── MQTT Client ──────────────────────────────────────────────────
WiFiClient   espClient;
PubSubClient client(espClient);

// ─── Read RMS Voltage from ZMPT101B ───────────────────────────────
float readVoltageRMS(int pin) {
    long sumSq = 0;
    const int samples = 200;
    for (int i = 0; i < samples; i++) {
        int raw = analogRead(pin) - 2048;  // remove DC offset
        sumSq += (long)raw * raw;
        delayMicroseconds(100);
    }
    float rms = sqrt((float)sumSq / samples);
    return rms * (VREF / ADC_RES) * VCAL;
}

// ─── Read RMS Current from SCT013 ─────────────────────────────────
float readCurrentRMS(int pin) {
    long sumSq = 0;
    const int samples = 200;
    for (int i = 0; i < samples; i++) {
        int raw = analogRead(pin) - 2048;
        sumSq += (long)raw * raw;
        delayMicroseconds(100);
    }
    float rms = sqrt((float)sumSq / samples);
    return rms * (VREF / ADC_RES) * ICAL;
}

// ─── WiFi Setup ───────────────────────────────────────────────────
void setup_wifi() {
    delay(10);
    Serial.print("Connecting to WiFi: ");
    Serial.println(ssid);
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nWiFi connected. IP: " + WiFi.localIP().toString());
}

// ─── MQTT Command Callback (from Android App) ─────────────────────
void callback(char* topic, byte* payload, unsigned int length) {
    String topicStr = String(topic);
    String msg = "";
    for (unsigned int i = 0; i < length; i++) msg += (char)payload[i];

    StaticJsonDocument<256> doc;
    if (deserializeJson(doc, msg)) return;

    // Handle motor/valve commands
    if (topicStr == "agri/pump1/command") {
        const char* motorState = doc["motor"];
        const char* valveState = doc["valve"];
        if (motorState) {
            digitalWrite(MOTOR_RELAY_PIN, String(motorState) == "ON" ? HIGH : LOW);
            Serial.println(String("Motor: ") + motorState);
        }
        if (valveState) {
            digitalWrite(VALVE_RELAY_PIN, String(valveState) == "OPEN" ? HIGH : LOW);
            Serial.println(String("Valve: ") + valveState);
        }
    }

    // Handle phase mode switch from Android Settings
    if (topicStr == "agri/pump1/mode") {
        const char* modeStr = doc["mode"];
        if (modeStr) {
            threePhaseMode = (String(modeStr) == "THREE");
            Serial.println(threePhaseMode ? "Mode: 3-Phase" : "Mode: Single Phase");
        }
    }
}

// ─── MQTT Reconnect ───────────────────────────────────────────────
void reconnect() {
    while (!client.connected()) {
        Serial.print("Connecting to MQTT...");
        if (client.connect("ESP32_AgriPumpController")) {
            Serial.println("connected.");
            client.subscribe("agri/pump1/command");
            client.subscribe("agri/pump1/mode");
        } else {
            Serial.print("failed, rc=");
            Serial.println(client.state());
            delay(5000);
        }
    }
}

// ─── Setup ────────────────────────────────────────────────────────
void setup() {
    Serial.begin(115200);

    pinMode(MOTOR_RELAY_PIN, OUTPUT);
    pinMode(VALVE_RELAY_PIN, OUTPUT);
    digitalWrite(MOTOR_RELAY_PIN, LOW);   // Motor OFF on boot
    digitalWrite(VALVE_RELAY_PIN, HIGH);  // Valve OPEN on boot

    pinMode(FLOW_SENSOR_PIN, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(FLOW_SENSOR_PIN), pulseCounter, FALLING);

    analogReadResolution(12);
    analogSetAttenuation(ADC_11db);  // 0–3.3V full range

    setup_wifi();
    client.setServer(mqtt_server, mqtt_port);
    client.setCallback(callback);
}

// ─── Main Loop ────────────────────────────────────────────────────
void loop() {
    if (!client.connected()) reconnect();
    client.loop();

    static unsigned long lastMsg = 0;
    unsigned long now = millis();

    if (now - lastMsg > 2000) {   // Publish every 2 seconds
        lastMsg = now;

        // Flow Rate
        float flowRate = (flowPulses / 7.5f);  // YF-S201: L/min
        flowPulses = 0;

        // Soil Moisture
        int rawMoist = analogRead(MOISTURE_PIN);
        int moistPct = constrain(map(rawMoist, 4095, 1500, 0, 100), 0, 100);

        // Voltage & Current — phase dependent
        float vr = readVoltageRMS(V_R_PIN);
        float ir = readCurrentRMS(I_R_PIN);
        float vy = 0.0f, iy = 0.0f;
        float vb = 0.0f, ib = 0.0f;

        if (threePhaseMode) {
            vy = readVoltageRMS(V_Y_PIN);
            vb = readVoltageRMS(V_B_PIN);
            iy = readCurrentRMS(I_Y_PIN);
            ib = readCurrentRMS(I_B_PIN);
        }

        // Build JSON Payload
        StaticJsonDocument<512> doc;
        doc["mode"]  = threePhaseMode ? "THREE" : "SINGLE";
        doc["vr"]    = vr;
        doc["ir"]    = ir;
        doc["vy"]    = vy;   // 0.0 in single-phase
        doc["iy"]    = iy;
        doc["vb"]    = vb;
        doc["ib"]    = ib;
        doc["flow"]  = flowRate;
        doc["valve"] = digitalRead(VALVE_RELAY_PIN) == HIGH ? "OPEN" : "CLOSED";
        doc["moist"] = moistPct;
        doc["tank"]  = 80;   // Replace with ultrasonic sensor if available

        char buffer[512];
        serializeJson(doc, buffer);
        client.publish("agri/pump1/telemetry", buffer);

        Serial.println(buffer);
    }
}
```

---

## 7. Sensor Calibration

### 7.1 ZMPT101B Voltage Calibration
1. Connect to a known AC supply (e.g., 230V).
2. Read `vr` from Serial Monitor.
3. Adjust `VCAL` until it reads ~230.0:
   ```
   VCAL_new = VCAL_old × (230.0 / measured_voltage)
   ```

### 7.2 SCT013 Current Calibration
1. Clip the SCT013 around a **single** live wire only.
2. With a known load (e.g., 1000W heater → ~4.35A @ 230V), read `ir`.
3. Adjust `ICAL` until it matches your clamp meter reading:
   ```
   ICAL_new = ICAL_old × (known_current / measured_current)
   ```

### 7.3 Soil Moisture Calibration
1. Read ADC in **dry air** → note as `dry` (~4095).
2. Read ADC with sensor in **water** → note as `wet` (~1500).
3. Update firmware: `map(rawMoist, dry, wet, 0, 100)`.

---

## 8. Physical Deployment Checklist

### Pre-Power Checks
- [ ] ESP32 powered via USB or 5V regulator — NOT directly from AC mains.
- [ ] Relay module has opto-isolator (verify before connecting to ESP32 GPIO).
- [ ] SCT013 burden resistors (62Ω) are soldered across the sensor output terminals.
- [ ] ZMPT101B modules connected to correct phase (phase-to-neutral only).
- [ ] Flow sensor installed in-line, aligned with arrow on body.

### WiFi & MQTT Checks
- [ ] Flash firmware with correct `ssid` / `password`.
- [ ] Open Arduino Serial Monitor at 115200 baud — confirm `"WiFi connected"`.
- [ ] Confirm `"Connecting to MQTT... connected."` in Serial Monitor.
- [ ] Use **MQTT Explorer** (desktop tool) to subscribe to `agri/pump1/telemetry` — verify JSON arrives every ~2 seconds.

### Single Phase (1Φ) Test
- [ ] Serial Monitor shows `"Mode: Single Phase"` on boot.
- [ ] `vr` ≈ 220–240V in telemetry. `vy` and `vb` = 0.0. ✓
- [ ] Publish `{"motor":"ON","valve":"OPEN"}` to `agri/pump1/command` — motor relay clicks ON.
- [ ] `ir` increases when motor is running.
- [ ] Publish `{"motor":"OFF","valve":"CLOSED"}` — motor relay clicks OFF, `ir` → 0.

### Three Phase (3Φ) Test
- [ ] In Android App → Settings → select **3-Phase** — app publishes to `agri/pump1/mode`.
- [ ] Serial Monitor shows `"Mode: 3-Phase"`.
- [ ] `vr`, `vy`, `vb` all ≈ 220–240V. `ir`, `iy`, `ib` all > 0 when motor runs.
- [ ] Phase balance: all three voltages within ±5V of each other.

### Android App Verification
- [ ] Fresh install: Default mode is **Single Phase** — only R-phase cards visible.
- [ ] Settings → switch to **3-Phase** → R/Y/B phase cards appear.
- [ ] Setting persists after app restart.
- [ ] Analytics screen: 1-Phase shows 1 voltage line; 3-Phase shows 3 lines (R/Y/B).

---

## 9. Troubleshooting

| Issue | Likely Cause | Fix |
|---|---|---|
| Voltage reads 0.0V | ZMPT101B not wired or broken | Check VIN+/VIN- connections |
| Voltage reads > 400V | `VCAL` too high | Reduce `VCAL` |
| Current reads 0.0A | SCT013 not clipped / missing burden resistor | Clip on single wire; add 62Ω resistor |
| No MQTT messages | Wrong WiFi credentials or broker | Check Serial Monitor for errors |
| Mode not switching | `agri/pump1/mode` not subscribed | Re-flash; ensure subscription in `reconnect()` |
| Motor won't turn ON | Relay is active-LOW type | Change `HIGH` to `LOW` in relay GPIO writes |
| App shows simulator data | App not connected to MQTT | Integrate `MqttHelper` into `TelemetryViewModel` (see Section 5) |

---

## 1. System Architecture

```
┌────────────────┐             ┌─────────────┐             ┌─────────────┐
│  Android App   │ ◄─────────► │ MQTT Broker │ ◄─────────► │ ESP32/Node  │
│ (Compose UI)   │    WiFi/    │  (HiveMQ /  │   Cellular/ │ (Pump Relay │
│  Telemetry     │    Cellular │  Mosquitto) │     WiFi    │  & Sensors) │
└────────────────┘             └─────────────┘             └─────────────┘
```

1. **ESP32 Microcontroller** reads physical sensors (voltage transducers, current CT coils, flow meters) and publishes telemetry as a JSON payload to `agri/pump1/telemetry`.
2. **Android App** subscribes to `agri/pump1/telemetry`, parses the JSON, and updates the dashboard.
3. When you tap the Motor Switch or Gate Valve, the app publishes a command (e.g., `{"motor": "ON"}`) to `agri/pump1/command`. The ESP32 listens and toggles the physical relays.

---

## 2. Step 1: Add MQTT Dependency to Android Project

Add the Eclipse Paho Java MQTT library to your Version Catalog and Gradle build file.

### In [libs.versions.toml](file:///d:/Development/Mobile/gradle/libs.versions.toml):
```toml
[versions]
# ...
pahoMqtt = "1.2.5"

[libraries]
# ...
paho-mqtt = { group = "org.eclipse.paho", name = "org.eclipse.paho.client.mqttv3", version.ref = "pahoMqtt" }
```

### In [app/build.gradle.kts](file:///d:/Development/Mobile/app/build.gradle.kts):
```kotlin
dependencies {
    // ...
    implementation(libs.paho.mqtt)
}
```

---

## 3. Step 2: Implement MQTT Client Helper in Android

Create a helper class to handle background connections, subscriptions, and publication.

### Create `MqttHelper.kt` in `com.agri.motorcontrol.data`:
```kotlin
package com.agri.motorcontrol.data

import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

class MqttHelper(
    private val brokerUrl: String = "tcp://broker.hivemq.com:1883", // Public test broker
    private val clientId: String = "AgriAndroidClient_" + System.currentTimeMillis(),
    private val onTelemetryReceived: (Telemetry) -> Unit,
    private val onConnectionStatusChanged: (Boolean) -> Unit
) {
    private var mqttClient: MqttClient? = null

    fun connect() {
        try {
            mqttClient = MqttClient(brokerUrl, clientId, null)
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 60
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    onConnectionStatusChanged(false)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic == "agri/pump1/telemetry" && message != null) {
                        val payload = String(message.payload)
                        val telemetry = parseJsonToTelemetry(payload)
                        if (telemetry != null) {
                            onTelemetryReceived(telemetry)
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            mqttClient?.connect(options)
            mqttClient?.subscribe("agri/pump1/telemetry", 1)
            onConnectionStatusChanged(true)

        } catch (e: MqttException) {
            e.printStackTrace()
            onConnectionStatusChanged(false)
        }
    }

    fun publishCommand(motorOn: Boolean, valveOpen: Boolean) {
        try {
            if (mqttClient?.isConnected == true) {
                val json = JSONObject().apply {
                    put("motor", if (motorOn) "ON" else "OFF")
                    put("valve", if (valveOpen) "OPEN" else "CLOSED")
                }
                val message = MqttMessage(json.toString().toByteArray()).apply { qos = 1 }
                mqttClient?.publish("agri/pump1/command", message)
            }
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }

    private fun parseJsonToTelemetry(jsonStr: String): Telemetry? {
        return try {
            val json = JSONObject(jsonStr)
            Telemetry(
                voltageR = json.optDouble("vr", 220.0).toFloat(),
                voltageY = json.optDouble("vy", 220.0).toFloat(),
                voltageB = json.optDouble("vb", 220.0).toFloat(),
                currentR = json.optDouble("ir", 0.0).toFloat(),
                currentY = json.optDouble("iy", 0.0).toFloat(),
                currentB = json.optDouble("ib", 0.0).toFloat(),
                flowRate = json.optDouble("flow", 0.0).toFloat(),
                valveOpen = json.optString("valve", "OPEN") == "OPEN",
                soilMoisture = json.optInt("moist", 50),
                tankLevel = json.optInt("tank", 80)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun disconnect() {
        mqttClient?.disconnect()
    }
}
```

---

## 4. Step 3: ESP32 Hardware Firmware (Arduino C++)

Flash this firmware to your ESP32 controller. It reads physical sensors and connects to the same broker.

```cpp
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// WiFi Settings
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// MQTT Broker Settings
const char* mqtt_server = "broker.hivemq.com";
const int mqtt_port = 1883;

// Pin Definitions
#define MOTOR_RELAY_PIN 25
#define VALVE_RELAY_PIN 26
#define FLOW_SENSOR_PIN 34
#define MOISTURE_PIN    35

WiFiClient espClient;
PubSubClient client(espClient);

// Flow meter variables
volatile int flowPulses = 0;
void IRAM_ATTR pulseCounter() {
    flowPulses++;
}

void setup() {
    Serial.begin(115200);
    pinMode(MOTOR_RELAY_PIN, OUTPUT);
    pinMode(VALVE_RELAY_PIN, OUTPUT);
    digitalWrite(MOTOR_RELAY_PIN, LOW); // Start with motor OFF
    digitalWrite(VALVE_RELAY_PIN, HIGH); // Start with valve OPEN (Active Low/High depending on relay)

    pinMode(FLOW_SENSOR_PIN, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(FLOW_SENSOR_PIN), pulseCounter, FALLING);

    setup_wifi();
    client.setServer(mqtt_server, mqtt_port);
    client.setCallback(callback);
}

void setup_wifi() {
    delay(10);
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
    }
}

// Receive Motor and Valve Commands from Android App
void callback(char* topic, byte* payload, unsigned int length) {
    String msg = "";
    for (int i = 0; i < length; i++) {
        msg += (char)payload[i];
    }
    
    StaticJsonDocument<200> doc;
    DeserializationError error = deserializeJson(doc, msg);
    if (!error) {
        const char* motorState = doc["motor"];
        const char* valveState = doc["valve"];

        if (String(motorState) == "ON") {
            digitalWrite(MOTOR_RELAY_PIN, HIGH);
        } else {
            digitalWrite(MOTOR_RELAY_PIN, LOW);
        }

        if (String(valveState) == "OPEN") {
            digitalWrite(VALVE_RELAY_PIN, HIGH);
        } else {
            digitalWrite(VALVE_RELAY_PIN, LOW);
        }
    }
}

void reconnect() {
    while (!client.connected()) {
        if (client.connect("ESP32_AgriPumpController")) {
            client.subscribe("agri/pump1/command");
        } else {
            delay(5000);
        }
    }
}

void loop() {
    if (!client.connected()) {
        reconnect();
    }
    client.loop();

    static unsigned long lastMsg = 0;
    unsigned long now = millis();
    
    // Read and Publish Telemetry every 2 seconds
    if (now - lastMsg > 2000) {
        lastMsg = now;

        // Calculate flow rate from pulses
        float flowRate = (flowPulses / 7.5); // Liters per min (standard YF-S201 formula)
        flowPulses = 0;

        // Read analog sensors (mock conversions for reference)
        int rawMoist = analogRead(MOISTURE_PIN);
        int moistPercent = map(rawMoist, 4095, 1500, 0, 100); // Calibrate dry/wet limits

        // Simulated Power grid values (Read from ZMPT101B / Current Transformers in production)
        float vr = 220.0 + random(-4, 4);
        float vy = 219.0 + random(-4, 4);
        float vb = 221.0 + random(-4, 4);
        
        bool motorIsOn = digitalRead(MOTOR_RELAY_PIN) == HIGH;
        float ir = motorIsOn ? (8.2 + random(-10, 10)/10.0) : 0.0;
        float iy = motorIsOn ? (8.1 + random(-10, 10)/10.0) : 0.0;
        float ib = motorIsOn ? (8.3 + random(-10, 10)/10.0) : 0.0;

        StaticJsonDocument<256> doc;
        doc["vr"] = vr;
        doc["vy"] = vy;
        doc["vb"] = vb;
        doc["ir"] = ir;
        doc["iy"] = iy;
        doc["ib"] = ib;
        doc["flow"] = flowRate;
        doc["valve"] = digitalRead(VALVE_RELAY_PIN) == HIGH ? "OPEN" : "CLOSED";
        doc["moist"] = moistPercent;
        doc["tank"] = random(70, 95); // Simulating tank levels

        char buffer[256];
        serializeJson(doc, buffer);
        client.publish("agri/pump1/telemetry", buffer);
    }
}
```
