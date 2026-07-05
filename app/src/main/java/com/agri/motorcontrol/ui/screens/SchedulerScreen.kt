package com.agri.motorcontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agri.motorcontrol.data.TimerSchedule
import com.agri.motorcontrol.ui.TelemetryViewModel
import com.agri.motorcontrol.ui.theme.EmeraldPrimary
import com.agri.motorcontrol.ui.theme.WarningOrange

@Composable
fun SchedulerScreen(viewModel: TelemetryViewModel, modifier: Modifier = Modifier) {
    val currentSchedule by viewModel.schedule.collectAsState()

    var startHour by remember { mutableIntStateOf(currentSchedule.startHour) }
    var startMinute by remember { mutableIntStateOf(currentSchedule.startMinute) }
    var duration by remember { mutableIntStateOf(currentSchedule.durationMinutes) }
    var isActive by remember { mutableStateOf(currentSchedule.isActive) }
    var targetMoisture by remember { mutableIntStateOf(currentSchedule.targetMoisture) }

    // Sync state if it updates externally
    LaunchedEffect(currentSchedule) {
        startHour = currentSchedule.startHour
        startMinute = currentSchedule.startMinute
        duration = currentSchedule.durationMinutes
        isActive = currentSchedule.isActive
        targetMoisture = currentSchedule.targetMoisture
    }

    fun saveSchedule() {
        viewModel.updateSchedule(
            TimerSchedule(
                startHour = startHour,
                startMinute = startMinute,
                durationMinutes = duration,
                isActive = isActive,
                targetMoisture = targetMoisture
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Main Switch Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Irrigation Schedule Timer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Trigger motor using time or moisture",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = {
                        isActive = it
                        saveSchedule()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = EmeraldPrimary
                    )
                )
            }
        }

        // --- Start Time Selector ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add, // Start time representation
                        contentDescription = "Clock",
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Start Time",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
                    NumberSelector(
                        value = startHour,
                        range = 0..23,
                        onValueChange = {
                            startHour = it
                            saveSchedule()
                        },
                        label = "Hrs"
                    )

                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )

                    // Minute picker
                    NumberSelector(
                        value = startMinute,
                        range = 0..59,
                        onValueChange = {
                            startMinute = it
                            saveSchedule()
                        },
                        label = "Mins"
                    )
                }
            }
        }

        // --- Irrigation Duration Slider ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Duration",
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Watering Duration",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "$duration min",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Slider(
                    value = duration.toFloat(),
                    onValueChange = {
                        duration = it.toInt()
                        saveSchedule()
                    },
                    valueRange = 5f..120f,
                    steps = 23, // increments of 5 mins
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary
                    )
                )
            }
        }

        // --- Moisture Threshold Control ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Moisture Trigger",
                            tint = WarningOrange
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Soil Moisture Trigger",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "< $targetMoisture%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange
                    )
                }

                Text(
                    text = "If Auto mode is active, the pump starts watering automatically when moisture drops below this value.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    lineHeight = 15.sp
                )

                Slider(
                    value = targetMoisture.toFloat(),
                    onValueChange = {
                        targetMoisture = it.toInt()
                        saveSchedule()
                    },
                    valueRange = 20f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = WarningOrange,
                        activeTrackColor = WarningOrange
                    )
                )
            }
        }
    }
}

@Composable
fun NumberSelector(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = {
                    val prev = value - 1
                    if (prev >= range.first) onValueChange(prev) else onValueChange(range.last)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            }

            Text(
                text = String.format("%02d", value),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(
                onClick = {
                    val next = value + 1
                    if (next <= range.last) onValueChange(next) else onValueChange(range.first)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}
