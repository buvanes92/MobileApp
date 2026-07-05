# IoT Integration Guide: Connecting Android App to Real Hardware

This guide explains how to connect your Android application to a real-world agricultural motor and sensor setup (using an **ESP32**, **Arduino**, or **Raspberry Pi** microcontroller) over the internet using **MQTT (Message Queuing Telemetry Transport)**, the industry standard lightweight protocol for IoT.

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
