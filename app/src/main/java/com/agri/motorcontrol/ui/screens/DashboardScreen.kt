package com.agri.motorcontrol.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agri.motorcontrol.data.MotorState
import com.agri.motorcontrol.data.SafetyAlarm
import com.agri.motorcontrol.ui.TelemetryViewModel
import com.agri.motorcontrol.ui.theme.AlertRed
import com.agri.motorcontrol.ui.theme.EmeraldPrimary
import com.agri.motorcontrol.ui.theme.WarningOrange
import com.agri.motorcontrol.ui.theme.WaterBlue
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(viewModel: TelemetryViewModel, modifier: Modifier = Modifier) {
    val telemetry by viewModel.telemetry.collectAsState()
    val motorState by viewModel.motorState.collectAsState()
    val autoMode by viewModel.autoMode.collectAsState()
    val alarm by viewModel.activeAlarm.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // --- Header Status ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "3-Phase Irrigation Pump",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (alarm == SafetyAlarm.NONE) EmeraldPrimary else AlertRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (alarm == SafetyAlarm.NONE) "GSM Connected (Normal)" else "System Fault Locked",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Phase indicators R-Y-B in header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhaseIndicatorDot(label = "R", isActive = telemetry.voltageR > 120f, color = Color(0xFFE53935))
                    PhaseIndicatorDot(label = "Y", isActive = telemetry.voltageY > 120f, color = Color(0xFFFFEB3B))
                    PhaseIndicatorDot(label = "B", isActive = telemetry.voltageB > 120f, color = Color(0xFF1E88E5))
                }
            }
        }

        // --- Alarm Banner ---
        if (alarm != SafetyAlarm.NONE) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AlertRed)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Critical Fault",
                        tint = AlertRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alarm.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertRed
                        )
                        Text(
                            text = alarm.message,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Button(
                        onClick = { viewModel.clearAlarms() },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // --- Core Motor Controls ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Motor Switch Card
            Card(
                modifier = Modifier
                    .weight(1.3f)
                    .height(160.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Motor Controller",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    val transition = rememberInfiniteTransition(label = "motorRotation")
                    val rotation by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotateAnim"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (motorState == MotorState.ON) {
                                        listOf(EmeraldPrimary.copy(alpha = 0.2f), Color.Transparent)
                                    } else {
                                        listOf(Color.Transparent, Color.Transparent)
                                    }
                                )
                            )
                    ) {
                        if (motorState == MotorState.ON) {
                            Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                                drawArc(
                                    color = EmeraldPrimary,
                                    startAngle = 0f,
                                    sweepAngle = 300f,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = { viewModel.toggleMotor() },
                            shape = CircleShape,
                            containerColor = if (motorState == MotorState.ON) EmeraldPrimary else MaterialTheme.colorScheme.background,
                            contentColor = if (motorState == MotorState.ON) Color.Black else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(60.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (motorState == MotorState.ON) EmeraldPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Toggle Motor",
                                modifier = Modifier.size(28.dp),
                                tint = if (motorState == MotorState.ON) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Text(
                        text = if (motorState == MotorState.ON) "RUNNING" else "STOPPED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (motorState == MotorState.ON) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Auto Mode Switch Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Auto Irrigation",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Icon(
                        imageVector = if (autoMode) Icons.Default.Refresh else Icons.Default.Home,
                        contentDescription = "Auto Mode Icon",
                        tint = if (autoMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(42.dp)
                    )

                    Switch(
                        checked = autoMode,
                        onCheckedChange = { viewModel.toggleAutoMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            uncheckedTrackColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Text(
                        text = if (autoMode) "ACTIVE" else "INACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // --- 3-Phase Voltage & Current Monitoring Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3-Phase Power Parameters",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Phase", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.weight(1f))
                    Text("Voltage", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    Text("Current", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Phase Red
                PhaseParamRow(
                    label = "Red (R)",
                    voltage = telemetry.voltageR,
                    current = telemetry.currentR,
                    color = Color(0xFFE53935)
                )

                Divider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 8.dp))

                // Phase Yellow
                PhaseParamRow(
                    label = "Yellow (Y)",
                    voltage = telemetry.voltageY,
                    current = telemetry.currentY,
                    color = Color(0xFFFFEB3B)
                )

                Divider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 8.dp))

                // Phase Blue
                PhaseParamRow(
                    label = "Blue (B)",
                    voltage = telemetry.voltageB,
                    current = telemetry.currentB,
                    color = Color(0xFF1E88E5)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "System Power Factor (PF):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = String.format("%.2f", telemetry.powerFactor),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }
        }

        // --- Water flow, Valve, Soil Moisture ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Water flow rate info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Water Flow", tint = WaterBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Water Flow Rate:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Text(
                        text = String.format("%.1f LPM", telemetry.flowRate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = WaterBlue
                    )
                }

                // Gate valve opening
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, contentDescription = "Valve", tint = WaterBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gate Valve Open:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Text(
                        text = "${telemetry.valveOpenPercent}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WaterBlue
                    )
                }

                Slider(
                    value = telemetry.valveOpenPercent.toFloat(),
                    onValueChange = { viewModel.setValvePosition(it.roundToInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = WaterBlue,
                        activeTrackColor = WaterBlue,
                        inactiveTrackColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp))

                // Soil Moisture & Tank Level feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(WarningOrange))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Soil Moisture", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Text(
                            text = "${telemetry.soilMoisture}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(WaterBlue))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Water Source Level", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Text(
                            text = "${telemetry.tankLevel}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhaseIndicatorDot(label: String, isActive: Boolean, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isActive) color else MaterialTheme.colorScheme.background)
                .border(
                    width = 1.5.dp,
                    color = if (isActive) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun PhaseParamRow(label: String, voltage: Float, current: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Text(
            text = "${voltage.roundToInt()} V",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (voltage < 120f) AlertRed else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End
        )

        Text(
            text = String.format("%.1f A", current),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (current > 15f) AlertRed else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End
        )
    }
}
