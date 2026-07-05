package com.agri.motorcontrol.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Telemetry(
    val voltageR: Float = 220f,        // Red Phase Voltage
    val voltageY: Float = 220f,        // Yellow Phase Voltage
    val voltageB: Float = 220f,        // Blue Phase Voltage
    val currentR: Float = 0f,          // Red Phase Current (Amps)
    val currentY: Float = 0f,          // Yellow Phase Current (Amps)
    val currentB: Float = 0f,          // Blue Phase Current (Amps)
    val powerFactor: Float = 0.85f,    // System Power Factor (0.0 to 1.0)
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
    OVERLOAD("Overload Detected", "Current exceeded safe threshold (15A). Motor stopped automatically."),
    PHASE_FAILURE("Phase Failure (Single Phasing)", "One or more power phases dropped below safe limit (120V). Motor stopped to prevent damage."),
    PHASE_IMBALANCE("Phase Voltage Imbalance", "Voltage imbalance between phases exceeded safe limit (35V). Motor stopped automatically.")
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
