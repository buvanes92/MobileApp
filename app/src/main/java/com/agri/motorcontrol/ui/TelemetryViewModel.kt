package com.agri.motorcontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agri.motorcontrol.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class TelemetryViewModel : ViewModel() {

    private val _telemetry = MutableStateFlow(Telemetry())
    val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    private val _motorState = MutableStateFlow(MotorState.OFF)
    val motorState: StateFlow<MotorState> = _motorState.asStateFlow()

    private val _autoMode = MutableStateFlow(false)
    val autoMode: StateFlow<Boolean> = _autoMode.asStateFlow()

    private val _activeAlarm = MutableStateFlow(SafetyAlarm.NONE)
    val activeAlarm: StateFlow<SafetyAlarm> = _activeAlarm.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _schedule = MutableStateFlow(TimerSchedule())
    val schedule: StateFlow<TimerSchedule> = _schedule.asStateFlow()

    private val _isSimulationActive = MutableStateFlow(true)
    val isSimulationActive: StateFlow<Boolean> = _isSimulationActive.asStateFlow()

    // History lists for Analytics Graph
    private val _voltageHistory = MutableStateFlow<List<Float>>(List(15) { 220f })
    val voltageHistory: StateFlow<List<Float>> = _voltageHistory.asStateFlow()

    private val _flowHistory = MutableStateFlow<List<Float>>(List(15) { 0f })
    val flowHistory: StateFlow<List<Float>> = _flowHistory.asStateFlow()

    // Simulation injected fault states
    private var faultLowVoltage = false
    private var faultHighVoltage = false
    private var faultBlockedPipe = false
    private var faultOverload = false

    private var dryRunTicks = 0
    private var simJob: Job? = null

    init {
        addLog("Irrigation Controller Online", LogSeverity.INFO)
        addLog("GSM Signal: Strong (RSSI -65dBm)", LogSeverity.INFO)
        startSimulation()
    }

    private fun startSimulation() {
        simJob?.cancel()
        simJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isSimulationActive.value) {
                    runSimulationStep()
                }
            }
        }
    }

    private fun runSimulationStep() {
        val currentMotor = _motorState.value
        val currentTelemetry = _telemetry.value

        // 1. Calculate Voltage (normal 220V +/- 3V unless fault injected)
        val targetVoltage = when {
            faultLowVoltage -> 165f + Random.nextFloat() * 4f
            faultHighVoltage -> 258f + Random.nextFloat() * 4f
            else -> 220f + Random.nextFloat() * 6f - 3f
        }

        // 2. Calculate Current based on Motor State and Load
        val targetCurrent = if (currentMotor == MotorState.ON) {
            when {
                faultOverload -> 16.5f + Random.nextFloat() * 1.5f
                else -> 8.2f + Random.nextFloat() * 0.6f - 0.3f
            }
        } else {
            0f
        }

        // 3. Calculate Flow Rate based on Motor & Valve & Blockage
        val targetFlow = if (currentMotor == MotorState.ON && !faultBlockedPipe) {
            val baseFlow = currentTelemetry.valveOpenPercent * 0.35f // max ~35 L/min
            baseFlow + Random.nextFloat() * 2f - 1f
        } else {
            0f
        }

        // 4. Update Soil Moisture & Tank Level slowly
        var moisture = currentTelemetry.soilMoisture
        var tank = currentTelemetry.tankLevel

        if (currentMotor == MotorState.ON && targetFlow > 2f) {
            // Watering soil
            if (Random.nextInt(100) < 30) moisture = (moisture + 1).coerceAtMost(100)
            if (Random.nextInt(100) < 40) tank = (tank - 1).coerceAtLeast(0)
        } else {
            // Natural soil drying out
            if (Random.nextInt(100) < 15) moisture = (moisture - 1).coerceAtAtLeast(10)
            // Rain/Refill simulator slowly
            if (Random.nextInt(100) < 20) tank = (tank + 1).coerceAtMost(100)
        }

        // Apply new values
        _telemetry.update {
            it.copy(
                voltage = targetVoltage,
                current = targetCurrent,
                flowRate = targetFlow.coerceAtLeast(0f),
                soilMoisture = moisture,
                tankLevel = tank
            )
        }

        // Update history caches
        updateHistory(targetVoltage, targetFlow.coerceAtLeast(0f))

        // 5. Run Safety Guard Rules
        checkSafetyGuards(targetVoltage, targetCurrent, targetFlow)

        // 6. Run Automation Rules
        runAutomationRules(moisture)
    }

    private fun checkSafetyGuards(volts: Float, amps: Float, flow: Float) {
        if (_activeAlarm.value != SafetyAlarm.NONE) return

        // Undervoltage check
        if (volts < 180f) {
            triggerAlarm(SafetyAlarm.UNDER_VOLTAGE)
            return
        }

        // Overvoltage check
        if (volts > 250f) {
            triggerAlarm(SafetyAlarm.OVER_VOLTAGE)
            return
        }

        // Overload check
        if (_motorState.value == MotorState.ON && amps > 15f) {
            triggerAlarm(SafetyAlarm.OVERLOAD)
            return
        }

        // Dry run check (Flow < 2 L/min while motor is running)
        if (_motorState.value == MotorState.ON && flow < 2f) {
            dryRunTicks++
            if (dryRunTicks >= 3) { // After 3 seconds of dry running
                triggerAlarm(SafetyAlarm.DRY_RUN)
            }
        } else {
            dryRunTicks = 0
        }
    }

    private fun runAutomationRules(currentMoisture: Int) {
        if (_activeAlarm.value != SafetyAlarm.NONE) return

        // Auto Mode logic
        if (_autoMode.value) {
            val target = _schedule.value.targetMoisture
            if (_motorState.value == MotorState.OFF && currentMoisture <= target) {
                _motorState.value = MotorState.ON
                addLog("Auto irrigation triggered: moisture ($currentMoisture%) <= target ($target%)", LogSeverity.INFO)
            } else if (_motorState.value == MotorState.ON && currentMoisture >= 85) {
                _motorState.value = MotorState.OFF
                addLog("Auto irrigation stopped: soil fully watered (85%)", LogSeverity.INFO)
            }
        }
    }

    private fun triggerAlarm(alarm: SafetyAlarm) {
        _activeAlarm.value = alarm
        _motorState.value = MotorState.OFF
        addLog("CRITICAL: ${alarm.title} - ${alarm.message}", LogSeverity.CRITICAL)
    }

    private fun updateHistory(newVoltage: Float, newFlow: Float) {
        _voltageHistory.update { (it.drop(1) + newVoltage) }
        _flowHistory.update { (it.drop(1) + newFlow) }
    }

    // --- User Actions ---

    fun toggleMotor() {
        if (_activeAlarm.value != SafetyAlarm.NONE) {
            addLog("Cannot start motor: Resolve active alarms first!", LogSeverity.WARNING)
            return
        }
        val newState = if (_motorState.value == MotorState.ON) MotorState.OFF else MotorState.ON
        _motorState.value = newState
        dryRunTicks = 0
        
        // Reset current and flow immediately on shut-off
        if (newState == MotorState.OFF) {
            _telemetry.update { it.copy(current = 0f, flowRate = 0f) }
        }

        addLog("Motor switched $newState by user", LogSeverity.INFO)
    }

    fun toggleAutoMode() {
        val newState = !_autoMode.value
        _autoMode.value = newState
        addLog("Automatic Mode ${if (newState) "ENABLED" else "DISABLED"}", LogSeverity.INFO)
    }

    fun setValvePosition(percent: Int) {
        _telemetry.update { it.copy(valveOpenPercent = percent.coerceIn(0, 100)) }
    }

    fun updateSchedule(newSchedule: TimerSchedule) {
        _schedule.value = newSchedule
        addLog("Irrigation schedule updated (Moisture target: ${newSchedule.targetMoisture}%)", LogSeverity.INFO)
    }

    fun addLog(message: String, severity: LogSeverity = LogSeverity.INFO) {
        val entry = LogEntry(message = message, severity = severity)
        _logs.update { (listOf(entry) + it).take(50) } // Keep last 50 logs
    }

    fun clearAlarms() {
        if (_activeAlarm.value != SafetyAlarm.NONE) {
            val clearedType = _activeAlarm.value.title
            _activeAlarm.value = SafetyAlarm.NONE
            dryRunTicks = 0
            
            // reset faults
            faultLowVoltage = false
            faultHighVoltage = false
            faultBlockedPipe = false
            faultOverload = false
            
            addLog("Safety guards reset. Alarms cleared.", LogSeverity.INFO)
        }
    }

    // --- Simulator Settings (Fault Injections) ---

    fun toggleSimulator(enable: Boolean) {
        _isSimulationActive.value = enable
        addLog("Simulation Mode ${if (enable) "Running" else "Paused"}", LogSeverity.INFO)
    }

    fun setLowVoltageFault(active: Boolean) {
        faultLowVoltage = active
        if (active) {
            faultHighVoltage = false
            addLog("Simulator: Injected Low Voltage fault (<180V)", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored Normal Voltage", LogSeverity.INFO)
        }
    }

    fun setHighVoltageFault(active: Boolean) {
        faultHighVoltage = active
        if (active) {
            faultLowVoltage = false
            addLog("Simulator: Injected High Voltage fault (>250V)", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored Normal Voltage", LogSeverity.INFO)
        }
    }

    fun setBlockedPipeFault(active: Boolean) {
        faultBlockedPipe = active
        if (active) {
            addLog("Simulator: Simulating physical water line blockage", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Water line cleared", LogSeverity.INFO)
        }
    }

    fun setOverloadFault(active: Boolean) {
        faultOverload = active
        if (active) {
            addLog("Simulator: Simulating motor shaft mechanical load jam", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored Normal Mechanical Load", LogSeverity.INFO)
        }
    }

    fun simulateRainTrigger() {
        _telemetry.update { it.copy(soilMoisture = 90, tankLevel = 100) }
        addLog("Simulator: Injected Heavy Rainfall Event", LogSeverity.INFO)
    }
}

// Utility extension helper
private fun Int.coerceAtAtLeast(minimumValue: Int): Int {
    return if (this < minimumValue) minimumValue else this
}
