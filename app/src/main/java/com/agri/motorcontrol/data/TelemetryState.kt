package com.agri.motorcontrol.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Telemetry(
    val voltage: Float = 220f,         // in Volts
    val current: Float = 0f,           // in Amperes (0 if motor is off)
    val flowRate: Float = 0f,          // in Liters per minute
    val valveOpenPercent: Int = 100,    // 0 to 100%
    val soilMoisture: Int = 55,        // in %
    val tankLevel: Int = 85            // in %
)

enum class MotorState {
    ON, OFF
}

enum class SafetyAlarm(val title: String, val message: String) {
    NONE("", ""),
    UNDER_VOLTAGE("Undervoltage Error", "Voltage dropped below safe threshold (180V). Motor stopped automatically."),
    OVER_VOLTAGE("Overvoltage Error", "Voltage exceeded safe threshold (250V). Motor stopped automatically."),
    DRY_RUN("Dry Run Detected", "No water flow detected while motor running. Motor stopped to prevent burn-out."),
    OVERLOAD("Overload Detected", "Current exceeded safe threshold (15A). Motor stopped automatically.")
}

enum class LogSeverity {
    INFO, WARNING, CRITICAL
}

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
    val message: String,
    val severity: LogSeverity = LogSeverity.INFO
)

data class TimerSchedule(
    val startHour: Int = 6,
    val startMinute: Int = 0,
    val durationMinutes: Int = 30,
    val isActive: Boolean = false,
    val targetMoisture: Int = 40 // auto shut-off moisture threshold
)
