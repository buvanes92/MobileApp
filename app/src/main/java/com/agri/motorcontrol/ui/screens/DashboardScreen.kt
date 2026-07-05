package com.agri.motorcontrol.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
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
        // --- Connection & Header status ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
                        text = "Irrigation Pump A",
                        fontSize = 20.sp,
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
                            text = if (alarm == SafetyAlarm.NONE) "Online" else "Fault Alarm Active",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Weather / Signal status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Signal Strength",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Battery Level",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- Alarm Banner ---
        if (alarm != SafetyAlarm.NONE) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertRed
                        )
                        Text(
                            text = alarm.message,
                            fontSize = 12.sp,
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

        // --- Core Motor Trigger & Auto Mode Control ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Motor Switch Card
            Card(
                modifier = Modifier
                    .weight(1.3f)
                    .height(180.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Motor Control",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    // Animated spinning ring around motor start button
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
                            .size(90.dp)
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
                        // Spinning dotted ring
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

                        // Physical button
                        FloatingActionButton(
                            onClick = { viewModel.toggleMotor() },
                            shape = CircleShape,
                            containerColor = if (motorState == MotorState.ON) EmeraldPrimary else MaterialTheme.colorScheme.background,
                            contentColor = if (motorState == MotorState.ON) Color.Black else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(68.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (motorState == MotorState.ON) EmeraldPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow, // Represents Motor toggle
                                contentDescription = "Toggle Motor",
                                modifier = Modifier.size(32.dp),
                                tint = if (motorState == MotorState.ON) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Text(
                        text = if (motorState == MotorState.ON) "RUNNING" else "STOPPED",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (motorState == MotorState.ON) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Auto-Mode Controller Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Auto Irrigation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Icon(
                        imageVector = if (autoMode) Icons.Default.Refresh else Icons.Default.Home,
                        contentDescription = "Auto Mode Icon",
                        tint = if (autoMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(50.dp)
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // --- Live Telemetry Dials ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Voltage Gauge Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(125.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                CircularMetric(
                    value = "${telemetry.voltage.roundToInt()}V",
                    label = "Voltage",
                    progress = (telemetry.voltage / 300f).coerceIn(0f, 1f),
                    color = when {
                        telemetry.voltage < 180f -> WarningOrange
                        telemetry.voltage > 250f -> AlertRed
                        else -> EmeraldPrimary
                    }
                )
            }

            // Current Gauge Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(125.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                CircularMetric(
                    value = String.format("%.1f A", telemetry.current),
                    label = "Current",
                    progress = (telemetry.current / 20f).coerceIn(0f, 1f),
                    color = if (telemetry.current > 15f) AlertRed else MaterialTheme.colorScheme.primary
                )
            }

            // Water Flow Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(125.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                CircularMetric(
                    value = String.format("%.1f LPM", telemetry.flowRate),
                    label = "Water Flow",
                    progress = (telemetry.flowRate / 45f).coerceIn(0f, 1f),
                    color = WaterBlue
                )
            }
        }

        // --- Gate Valve & Moisture Controls ---
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
                Text(
                    text = "Gate Valve Control",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Valve Icon",
                            tint = WaterBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Position:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "${telemetry.valveOpenPercent}% ${if (telemetry.valveOpenPercent == 100) "(OPEN)" else if (telemetry.valveOpenPercent == 0) "(CLOSED)" else ""}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
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

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.background)

                // Irrigation feedback stats (Moisture & Tank Level)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Moisture",
                                tint = WarningOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Soil Moisture", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Text(
                            text = "${telemetry.soilMoisture}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Tank Level",
                                tint = WaterBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Water Source Level", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Text(
                            text = "${telemetry.tankLevel}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularMetric(
    value: String,
    label: String,
    progress: Float,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawCircle(
                    color = color.copy(alpha = 0.12f),
                    style = Stroke(width = 4.dp.toPx())
                )
                // Colored progress track
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
