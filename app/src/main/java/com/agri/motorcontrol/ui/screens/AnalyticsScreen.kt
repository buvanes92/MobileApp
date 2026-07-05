package com.agri.motorcontrol.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agri.motorcontrol.data.LogEntry
import com.agri.motorcontrol.data.LogSeverity
import com.agri.motorcontrol.ui.TelemetryViewModel
import com.agri.motorcontrol.ui.theme.AlertRed
import com.agri.motorcontrol.ui.theme.EmeraldPrimary
import com.agri.motorcontrol.ui.theme.WarningOrange
import com.agri.motorcontrol.ui.theme.WaterBlue

@Composable
fun AnalyticsScreen(viewModel: TelemetryViewModel, modifier: Modifier = Modifier) {
    val voltageRHistory by viewModel.voltageRHistory.collectAsState()
    val voltageYHistory by viewModel.voltageYHistory.collectAsState()
    val voltageBHistory by viewModel.voltageBHistory.collectAsState()
    val flowHistory by viewModel.flowHistory.collectAsState()
    val logs by viewModel.logs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 3-Phase Voltage Chart ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3-Phase Voltage Stability (V)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendLabel("R", Color(0xFFE53935))
                        LegendLabel("Y", Color(0xFFFBC02D))
                        LegendLabel("B", Color(0xFF1E88E5))
                    }
                }
                
                // Draw custom 3-Phase Line Chart for Voltage
                ThreePhaseLineChart(
                    dataR = voltageRHistory,
                    dataY = voltageYHistory,
                    dataB = voltageBHistory,
                    yMin = 100f,
                    yMax = 280f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            }
        }

        // --- Water Flow Chart ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Live Water Flow Rate (LPM)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LineChart(
                    data = flowHistory,
                    yMin = 0f,
                    yMax = 45f,
                    lineColor = WaterBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            }
        }

        // --- System Logs ---
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
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Logs List",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Controller Logs & History",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.clearLogsHistory() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Logs",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No log records available",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            LogItem(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendLabel(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun ThreePhaseLineChart(
    dataR: List<Float>,
    dataY: List<Float>,
    dataB: List<Float>,
    yMin: Float,
    yMax: Float,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingRight = 40f
        val chartWidth = width - paddingRight
        val chartHeight = height

        // Draw horizontal grid lines
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = (chartHeight / gridCount) * i
            drawLine(
                color = axisColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        fun mapValueToY(value: Float): Float {
            val ratio = (value - yMin) / (yMax - yMin)
            return chartHeight - (ratio.coerceIn(0f, 1f) * chartHeight)
        }

        val pointsCount = dataR.size
        val xStep = chartWidth / (pointsCount - 1)

        // Draw helper to plot a line path
        fun drawPhasePath(data: List<Float>, color: Color) {
            if (data.size < 2) return
            val path = Path()
            val fillPath = Path()
            
            val startY = mapValueToY(data[0])
            path.moveTo(0f, startY)
            fillPath.moveTo(0f, chartHeight)
            fillPath.lineTo(0f, startY)

            for (i in 1 until pointsCount) {
                val x = i * xStep
                val y = mapValueToY(data[i])
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            fillPath.lineTo((pointsCount - 1) * xStep, chartHeight)
            fillPath.close()

            // Area fill (light alpha)
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            // Trend line
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Last value point indicator
            val lastX = (pointsCount - 1) * xStep
            val lastY = mapValueToY(data.last())
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = color,
                radius = 2.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }

        // Draw R, Y, B lines
        drawPhasePath(dataR, Color(0xFFE53935))
        drawPhasePath(dataY, Color(0xFFFBC02D))
        drawPhasePath(dataB, Color(0xFF1E88E5))
    }
}

@Composable
fun LineChart(
    data: List<Float>,
    yMin: Float,
    yMax: Float,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingRight = 40f
        val chartWidth = width - paddingRight
        val chartHeight = height

        if (data.size < 2) return@Canvas

        // Draw horizontal grid lines
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = (chartHeight / gridCount) * i
            drawLine(
                color = axisColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val path = Path()
        val fillPath = Path()

        val pointsCount = data.size
        val xStep = chartWidth / (pointsCount - 1)

        fun mapValueToY(value: Float): Float {
            val ratio = (value - yMin) / (yMax - yMin)
            return chartHeight - (ratio.coerceIn(0f, 1f) * chartHeight)
        }

        val startY = mapValueToY(data[0])
        path.moveTo(0f, startY)
        fillPath.moveTo(0f, chartHeight)
        fillPath.lineTo(0f, startY)

        for (i in 1 until pointsCount) {
            val x = i * xStep
            val y = mapValueToY(data[i])
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        fillPath.lineTo((pointsCount - 1) * xStep, chartHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = chartHeight
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        val lastX = (pointsCount - 1) * xStep
        val lastY = mapValueToY(data.last())
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = lineColor,
            radius = 2.5.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val severityColor = when (log.severity) {
        LogSeverity.CRITICAL -> AlertRed
        LogSeverity.WARNING -> WarningOrange
        LogSeverity.INFO -> EmeraldPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(severityColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = log.timestamp,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = log.message,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
