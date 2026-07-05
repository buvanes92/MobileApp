package com.agri.motorcontrol.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agri.motorcontrol.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.random.Random

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

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

    // 3-Phase Voltage History lists for Analytics Graph
    private val _voltageRHistory = MutableStateFlow<List<Float>>(List(15) { 220f })
    val voltageRHistory: StateFlow<List<Float>> = _voltageRHistory.asStateFlow()

    private val _voltageYHistory = MutableStateFlow<List<Float>>(List(15) { 220f })
    val voltageYHistory: StateFlow<List<Float>> = _voltageYHistory.asStateFlow()

    private val _voltageBHistory = MutableStateFlow<List<Float>>(List(15) { 220f })
    val voltageBHistory: StateFlow<List<Float>> = _voltageBHistory.asStateFlow()

    private val _flowHistory = MutableStateFlow<List<Float>>(List(15) { 0f })
    val flowHistory: StateFlow<List<Float>> = _flowHistory.asStateFlow()

    // Simulation injected fault states
    private var faultRPhaseCut = false
    private var faultYPhaseCut = false
    private var faultBPhaseCut = false
    private var faultImbalance = false
    private var faultOverload = false
    private var faultDryRun = false

    private var dryRunTicks = 0
    private var simJob: Job? = null

    init {
        loadLogsFromFile()
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

        // 1. Calculate individual Phase Voltages (normal 220V +/- 3V unless phase cuts / imbalance injected)
        val baseVoltR = if (faultRPhaseCut) 0f else if (faultImbalance) 165f else 220f + Random.nextFloat() * 4f - 2f
        val baseVoltY = if (faultYPhaseCut) 0f else if (faultImbalance) 232f else 221f + Random.nextFloat() * 4f - 2f
        val baseVoltB = if (faultBPhaseCut) 0f else if (faultImbalance) 241f else 219f + Random.nextFloat() * 4f - 2f

        // 2. Calculate Phase Currents based on Motor State
        val targetCurrentR = if (currentMotor == MotorState.ON && !faultRPhaseCut) {
            when {
                faultOverload -> 16.2f + Random.nextFloat() * 0.8f
                else -> 8.2f + Random.nextFloat() * 0.4f - 0.2f
            }
        } else 0f

        val targetCurrentY = if (currentMotor == MotorState.ON && !faultYPhaseCut) {
            when {
                faultOverload -> 16.5f + Random.nextFloat() * 0.8f
                else -> 8.1f + Random.nextFloat() * 0.4f - 0.2f
            }
        } else 0f

        val targetCurrentB = if (currentMotor == MotorState.ON && !faultBPhaseCut) {
            when {
                faultOverload -> 16.3f + Random.nextFloat() * 0.8f
                else -> 8.3f + Random.nextFloat() * 0.4f - 0.2f
            }
        } else 0f

        // 3. Calculate Flow Rate based on Motor & Valve & Blockage
        val targetFlow = if (currentMotor == MotorState.ON && currentTelemetry.valveOpen && !faultDryRun) {
            val baseFlow = 35f // max ~35 L/min when open
            baseFlow + Random.nextFloat() * 1.5f - 0.7f
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
            if (Random.nextInt(100) < 15) moisture = (moisture - 1).coerceAtLeast(10)
            // Rain/Refill simulator slowly
            if (Random.nextInt(100) < 20) tank = (tank + 1).coerceAtMost(100)
        }

        // Apply new values
        _telemetry.update {
            it.copy(
                voltageR = baseVoltR,
                voltageY = baseVoltY,
                voltageB = baseVoltB,
                currentR = targetCurrentR,
                currentY = targetCurrentY,
                currentB = targetCurrentB,
                flowRate = targetFlow.coerceAtLeast(0f),
                soilMoisture = moisture,
                tankLevel = tank
            )
        }

        // Update history caches
        updateHistory(baseVoltR, baseVoltY, baseVoltB, targetFlow.coerceAtLeast(0f))

        // 5. Run Safety Guard Rules
        checkSafetyGuards(baseVoltR, baseVoltY, baseVoltB, targetCurrentR, targetCurrentY, targetB = targetCurrentB, targetFlow)

        // 6. Run Automation Rules
        runAutomationRules(moisture)
    }

    private fun checkSafetyGuards(voltsR: Float, voltsY: Float, voltsB: Float, targetR: Float, targetY: Float, targetB: Float, flow: Float) {
        if (_activeAlarm.value != SafetyAlarm.NONE) return

        // 1. Phase Failure / Single Phasing Check (Any phase < 120V)
        if (voltsR < 120f || voltsY < 120f || voltsB < 120f) {
            triggerAlarm(SafetyAlarm.PHASE_FAILURE)
            return
        }

        // 2. Phase Voltage Imbalance Check (difference > 35V)
        val maxVolt = maxOf(voltsR, voltsY, voltsB)
        val minVolt = minOf(voltsR, voltsY, voltsB)
        if (maxVolt - minVolt > 35f) {
            triggerAlarm(SafetyAlarm.PHASE_IMBALANCE)
            return
        }

        // 3. Average Over/Undervoltage check
        val avgVolts = (voltsR + voltsY + voltsB) / 3f
        if (avgVolts < 180f) {
            triggerAlarm(SafetyAlarm.UNDER_VOLTAGE)
            return
        }
        if (avgVolts > 250f) {
            triggerAlarm(SafetyAlarm.OVER_VOLTAGE)
            return
        }

        // 4. Overload Check (Any phase current > 15A)
        if (_motorState.value == MotorState.ON && (targetR > 15f || targetY > 15f || targetB > 15f)) {
            triggerAlarm(SafetyAlarm.OVERLOAD)
            return
        }

        // 5. Dry Run check (Flow < 2 L/min while motor is running)
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

    private fun updateHistory(newVR: Float, newVY: Float, newVB: Float, newFlow: Float) {
        _voltageRHistory.update { (it.drop(1) + newVR) }
        _voltageYHistory.update { (it.drop(1) + newVY) }
        _voltageBHistory.update { (it.drop(1) + newVB) }
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
            _telemetry.update {
                it.copy(
                    currentR = 0f,
                    currentY = 0f,
                    currentB = 0f,
                    flowRate = 0f
                )
            }
        }

        addLog("Motor switched $newState by user", LogSeverity.INFO)
    }

    fun toggleAutoMode() {
        val newState = !_autoMode.value
        _autoMode.value = newState
        addLog("Automatic Mode ${if (newState) "ENABLED" else "DISABLED"}", LogSeverity.INFO)
    }

    fun toggleValve() {
        _telemetry.update { it.copy(valveOpen = !it.valveOpen) }
        addLog("Gate Valve switched ${if (_telemetry.value.valveOpen) "OPEN" else "CLOSED"} by user", LogSeverity.INFO)
    }

    fun updateSchedule(newSchedule: TimerSchedule) {
        _schedule.value = newSchedule
        addLog("Irrigation schedule updated (Moisture target: ${newSchedule.targetMoisture}%)", LogSeverity.INFO)
    }

    fun addLog(message: String, severity: LogSeverity = LogSeverity.INFO) {
        val entry = LogEntry(message = message, severity = severity)
        _logs.update { (listOf(entry) + it).take(50) } // Keep last 50 logs
        saveLogsToFile()
    }

    fun clearAlarms() {
        if (_activeAlarm.value != SafetyAlarm.NONE) {
            val clearedType = _activeAlarm.value.title
            _activeAlarm.value = SafetyAlarm.NONE
            dryRunTicks = 0
            
            // Reset faults
            faultRPhaseCut = false
            faultYPhaseCut = false
            faultBPhaseCut = false
            faultImbalance = false
            faultOverload = false
            faultDryRun = false
            
            addLog("Safety guards reset. Alarms cleared.", LogSeverity.INFO)
        }
    }

    // --- Simulator Settings (Fault Injections) ---

    fun toggleSimulator(enable: Boolean) {
        _isSimulationActive.value = enable
        addLog("Simulation Mode ${if (enable) "Running" else "Paused"}", LogSeverity.INFO)
    }

    fun setRPhaseCut(active: Boolean) {
        faultRPhaseCut = active
        if (active) {
            addLog("Simulator: Injected R-Phase Failure (0V)", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored R-Phase Voltage", LogSeverity.INFO)
        }
    }

    fun setYPhaseCut(active: Boolean) {
        faultYPhaseCut = active
        if (active) {
            addLog("Simulator: Injected Y-Phase Failure (0V)", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored Y-Phase Voltage", LogSeverity.INFO)
        }
    }

    fun setBPhaseCut(active: Boolean) {
        faultBPhaseCut = active
        if (active) {
            addLog("Simulator: Injected B-Phase Failure (0V)", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored B-Phase Voltage", LogSeverity.INFO)
        }
    }

    fun setPhaseImbalance(active: Boolean) {
        faultImbalance = active
        if (active) {
            addLog("Simulator: Injected Phase Imbalance (R=165V, Y=232V, B=241V)", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored Phase Balance", LogSeverity.INFO)
        }
    }

    fun setOverloadFault(active: Boolean) {
        faultOverload = active
        if (active) {
            addLog("Simulator: Simulating motor shaft overload (>15A)", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored normal mechanical load", LogSeverity.INFO)
        }
    }

    fun setBlockedPipeFault(active: Boolean) {
        faultDryRun = active
        if (active) {
            addLog("Simulator: Simulating dry running (water flow = 0)", LogSeverity.WARNING)
        } else {
            addLog("Simulator: Restored water supply flow", LogSeverity.INFO)
        }
    }

    fun simulateRainTrigger() {
        _telemetry.update { it.copy(soilMoisture = 90, tankLevel = 100) }
        addLog("Simulator: Injected heavy rainfall event", LogSeverity.INFO)
    }

    fun clearLogsHistory() {
        _logs.value = emptyList()
        try {
            val file = File(getApplication<Application>().filesDir, "system_logs.json")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        addLog("Log history cleared by user", LogSeverity.INFO)
    }

    private fun saveLogsToFile() {
        viewModelScope.launch {
            try {
                val jsonArray = JSONArray()
                _logs.value.forEach { log ->
                    val jsonObject = JSONObject().apply {
                        put("id", log.id)
                        put("timestamp", log.timestamp)
                        put("message", log.message)
                        put("severity", log.severity.name)
                    }
                    jsonArray.put(jsonObject)
                }
                getApplication<Application>().openFileOutput("system_logs.json", android.content.Context.MODE_PRIVATE).use { output ->
                    output.write(jsonArray.toString().toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadLogsFromFile() {
        try {
            val file = File(getApplication<Application>().filesDir, "system_logs.json")
            if (!file.exists()) {
                val initialLogs = listOf(
                    LogEntry(message = "3-Phase Smart Pump Controller Online", severity = LogSeverity.INFO),
                    LogEntry(message = "GSM Signal: Strong (RSSI -61dBm)", severity = LogSeverity.INFO)
                )
                _logs.value = initialLogs
                saveLogsToFile()
                return
            }
            val content = file.readText()
            val jsonArray = JSONArray(content)
            val loadedLogs = mutableListOf<LogEntry>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val log = LogEntry(
                    id = jsonObject.optString("id", UUID.randomUUID().toString()),
                    timestamp = jsonObject.optString("timestamp", ""),
                    message = jsonObject.optString("message", ""),
                    severity = LogSeverity.valueOf(jsonObject.optString("severity", LogSeverity.INFO.name))
                )
                loadedLogs.add(log)
            }
            _logs.value = loadedLogs
        } catch (e: Exception) {
            e.printStackTrace();
            _logs.value = listOf(
                LogEntry(message = "Error loading stored logs. Resetting log history.", severity = LogSeverity.WARNING),
                LogEntry(message = "3-Phase Smart Pump Controller Online", severity = LogSeverity.INFO)
            )
            saveLogsToFile()
        }
    }
}
