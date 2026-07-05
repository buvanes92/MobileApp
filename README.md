# Smart Agriculture Motor Control & Monitoring Android App

A native Android mobile application built with **Kotlin**, **Jetpack Compose**, and **Coroutines (Flow/StateFlow)**. The application is structured using **MVVM (Model-View-ViewModel)** architecture and adheres to Google's official modern Android development guidelines.

## Key Features

1. **Live Dashboard & Controls**:
   - Touch-sensitive neon **Motor Switch** with spinning activity ring.
   - Smooth **Gate Valve Slider** to regulate water flow.
   - Custom **Circular Gauges** monitoring Voltage (V), Current (A), and Flow Rate (LPM) in real time.
   - Live status badges for Soil Moisture and Water Source levels.

2. **Automated Scheduler**:
   - Time-based irrigation scheduling with custom duration controls.
   - Moisture-level trigger threshold (e.g., auto-start pump when soil drops below 40%).

3. **Analytics & Diagnostic Logs**:
   - Custom **Canvas-drawn area line graphs** showing voltage fluctuations and flow rate history.
   - Live color-coded diagnostic log history (Info, Warnings, Critical Alerts).

4. **Safety Protection Guards**:
   - **Dry Run Protection**: Automatically stops the motor if water flow drops below 2 LPM for 3 seconds to prevent pump damage.
   - **Voltage Cut-offs**: Shuts off the motor if line voltage sags below 180V or surges above 250V.
   - **Overload Cut-off**: Shuts off the motor if current exceeds 15A.

---

## Interactive IoT Hardware Simulator

To allow complete visual testing without physical microcontrollers (ESP32/Arduino) or sensors connected, the app has a built-in **IoT Hardware Simulator**. 

To use it:
1. Tap the orange **Hardware Simulator** floating button at the bottom-right.
2. Toggle "Run Telemetry Simulation Loop" to start generating live data.
3. Click any fault buttons to inject anomalies:
   - **Low Volt / High Volt**: Triggers voltage cut-off protections.
   - **Block Water Flow**: Cuts flow rate, triggering **Dry Run Protection** after 3 seconds.
   - **Motor Overload**: Spikes current, triggering **Overload Protection**.
   - **Simulate Heavy Rainfall**: Spikes soil moisture and tank levels to demonstrate automated shutoff.

---

## How to Build and Run in Android Studio

1. **Launch Android Studio** (version Hedgehog 2023.3.1 or newer recommended).
2. Choose **File -> Open** and navigate to the project directory: `d:\Development\Mobile`.
3. Let Gradle sync and download required libraries (Android Studio will automatically download the correct JDK and Gradle wrapper version).
4. Connect an Android Device via USB (with Developer Mode & USB Debugging enabled) or start an Android Virtual Device (Emulator).
5. Click the green **Run (Play)** button in Android Studio to build the APK and deploy it.
