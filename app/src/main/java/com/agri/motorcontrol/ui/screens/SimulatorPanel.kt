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
import androidx.compose.ui.graphics.Color
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

    var rPhaseCut by remember { mutableStateOf(false) }
    var yPhaseCut by remember { mutableStateOf(false) }
    var bPhaseCut by remember { mutableStateOf(false) }
    var imbalanceFault by remember { mutableStateOf(false) }
    var overloadFault by remember { mutableStateOf(false) }
    var blockedPipeFault by remember { mutableStateOf(false) }

    // Synchronize fault states when clearAlarms or normal reset is called in VM
    LaunchedEffect(telemetry) {
        if (telemetry.voltageR > 120f) rPhaseCut = false
        if (telemetry.voltageY > 120f) yPhaseCut = false
        if (telemetry.voltageB > 120f) bPhaseCut = false
        
        val maxV = maxOf(telemetry.voltageR, telemetry.voltageY, telemetry.voltageB)
        val minV = minOf(telemetry.voltageR, telemetry.voltageY, telemetry.voltageB)
        if (maxV - minV <= 35f) {
            imbalanceFault = false
        }
        
        if (telemetry.currentR <= 15f && telemetry.currentY <= 15f && telemetry.currentB <= 15f) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .clickable { onToggleExpand() }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .align(Alignment.BottomCenter)
                    .clickable(enabled = false) {},
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
                                text = "IoT 3-Phase Simulator",
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
                                text = "Run Telemetry Simulation",
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "INJECT FAULTS (TEST SAFETY TRIPS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Phase Cuts (R, Y, B failure cuts)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                rPhaseCut = !rPhaseCut
                                viewModel.setRPhaseCut(rPhaseCut)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (rPhaseCut) AlertRed else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Cut R-Phase",
                                fontSize = 11.sp,
                                color = if (rPhaseCut) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Button(
                            onClick = {
                                yPhaseCut = !yPhaseCut
                                viewModel.setYPhaseCut(yPhaseCut)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (yPhaseCut) AlertRed else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Cut Y-Phase",
                                fontSize = 11.sp,
                                color = if (yPhaseCut) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Button(
                            onClick = {
                                bPhaseCut = !bPhaseCut
                                viewModel.setBPhaseCut(bPhaseCut)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bPhaseCut) AlertRed else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Cut B-Phase",
                                fontSize = 11.sp,
                                color = if (bPhaseCut) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Phase Imbalance & Overload
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                imbalanceFault = !imbalanceFault
                                viewModel.setPhaseImbalance(imbalanceFault)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (imbalanceFault) WarningOrange else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Voltage Imbalance",
                                fontSize = 11.sp,
                                color = if (imbalanceFault) Color.White else MaterialTheme.colorScheme.onBackground
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
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Current Overload",
                                fontSize = 11.sp,
                                color = if (overloadFault) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Button(
                            onClick = {
                                blockedPipeFault = !blockedPipeFault
                                viewModel.setBlockedPipeFault(blockedPipeFault)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (blockedPipeFault) WarningOrange else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Dry Run (0 LPM)",
                                fontSize = 11.sp,
                                color = if (blockedPipeFault) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.simulateRainTrigger() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WaterBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Trigger Simulated Heavy Rainfall", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
