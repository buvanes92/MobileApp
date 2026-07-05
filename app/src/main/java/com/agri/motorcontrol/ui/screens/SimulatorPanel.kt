package com.agri.motorcontrol.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agri.motorcontrol.ui.TelemetryViewModel
import com.agri.motorcontrol.ui.theme.AlertRed
import com.agri.motorcontrol.ui.theme.EmeraldPrimary
import com.agri.motorcontrol.ui.theme.WarningOrange
import com.agri.motorcontrol.ui.theme.WaterBlue
import kotlin.math.roundToInt

@Composable
fun SimulatorPanel(
    viewModel: TelemetryViewModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSimRunning by viewModel.isSimulationActive.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    var lowVoltFault by remember { mutableStateOf(false) }
    var highVoltFault by remember { mutableStateOf(false) }
    var blockedPipeFault by remember { mutableStateOf(false) }
    var overloadFault by remember { mutableStateOf(false) }

    // Synchronize fault states when clearAlarms or normal reset is called in VM
    LaunchedEffect(telemetry) {
        if (telemetry.voltage in 180f..250f) {
            lowVoltFault = false
            highVoltFault = false
        }
        if (telemetry.current <= 15f) {
            overloadFault = false
        }
        if (telemetry.flowRate > 0f) {
            blockedPipeFault = false
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (!isExpanded) {
            // Floating pill trigger button
            ExtendedFloatingActionButton(
                onClick = onToggleExpand,
                icon = { Icon(Icons.Default.Build, "Settings", tint = Color.Black) },
                text = { Text("Hardware Simulator", fontWeight = FontWeight.Bold, color = Color.Black) },
                containerColor = WarningOrange,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
            )
        } else {
            // Backdrop dim filter
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .clickable { onToggleExpand() }
            )

            // Panel content card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .align(Alignment.BottomCenter)
                    .clickable(enabled = false) {}, // prevent click-through
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Sim Engine",
                                tint = WarningOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "IoT Hardware Simulator",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        IconButton(onClick = onToggleExpand) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Master Simulator Engine Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Run Telemetry Simulation Loop",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generates live fluctuations & monitors guards",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = isSimRunning,
                            onCheckedChange = { viewModel.toggleSimulator(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "INJECT TELEMETRY ANOMALIES (TEST CUTOFFS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Voltage Sag/Surge Checkboxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                lowVoltFault = !lowVoltFault
                                highVoltFault = false
                                viewModel.setLowVoltageFault(lowVoltFault)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lowVoltFault) WarningOrange else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Low Volt (165V)",
                                fontSize = 12.sp,
                                color = if (lowVoltFault) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Button(
                            onClick = {
                                highVoltFault = !highVoltFault
                                lowVoltFault = false
                                viewModel.setHighVoltageFault(highVoltFault)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (highVoltFault) AlertRed else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "High Volt (260V)",
                                fontSize = 12.sp,
                                color = if (highVoltFault) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Blocked Pipe & Motor Overload Checkboxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                blockedPipeFault = !blockedPipeFault
                                viewModel.setBlockedPipeFault(blockedPipeFault)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (blockedPipeFault) WarningOrange else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Block Water Flow",
                                fontSize = 12.sp,
                                color = if (blockedPipeFault) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Button(
                            onClick = {
                                overloadFault = !overloadFault
                                viewModel.setOverloadFault(overloadFault)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (overloadFault) AlertRed else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Motor Overload",
                                fontSize = 12.sp,
                                color = if (overloadFault) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Weather Trigger
                    Button(
                        onClick = { viewModel.simulateRainTrigger() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WaterBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Trigger Simulated Heavy Rainfall", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
