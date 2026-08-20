package com.example.sleeptracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleeptracker.analytics.Period
import com.example.sleeptracker.analytics.PeriodSummary
import com.example.sleeptracker.analytics.formatHours
import com.example.sleeptracker.ui.SleepViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis as CoreHorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis as CoreVerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

private val LabelsKey = ExtraStore.Key<List<String>>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(vm: SleepViewModel) {
    val period by vm.period.collectAsState()
    val summary by vm.summary.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Аналитика") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { index, p ->
                    SegmentedButton(
                        selected = period == p,
                        onClick = { vm.setPeriod(p) },
                        shape = SegmentedButtonDefaults.itemShape(index, Period.entries.size),
                    ) {
                        Text(p.title)
                    }
                }
            }

            StatsRow(summary, period)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (period == Period.YEAR) "Средний сон за ночь, ч" else "Сон по дням, ч",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (summary.hasData) {
                        SleepChart(summary)
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Нет данных за этот период",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(summary: PeriodSummary, period: Period) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(
            title = "Средний сон",
            value = if (summary.hasData) formatHours(summary.avgSleepHours) else "—",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "Оценка",
            value = if (summary.hasData) String.format("%.1f", summary.avgQuality) else "—",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "Засыпание",
            value = if (summary.hasData) "${Math.round(summary.avgFallAsleepMinutes)} мин" else "—",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SleepChart(summary: PeriodSummary) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(summary) {
        modelProducer.runTransaction {
            columnSeries { series(summary.points.map { it.hours }) }
            extras { it[LabelsKey] = summary.points.map { p -> p.label } }
        }
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary

    val bottomFormatter = CartesianValueFormatter { context, x, _ ->
        val labels = context.model.extraStore.getOrNull(LabelsKey).orEmpty()
        labels.getOrNull(x.toInt()) ?: ""
    }
    val startFormatter = CartesianValueFormatter { _, y, _ ->
        if (y == 0.0) "0" else String.format("%.0f", y)
    }

    // при большом количестве столбиков подписи по оси X прореживаем
    val labelSpacing = when {
        summary.points.size > 20 -> 5
        summary.points.size > 12 -> 2
        else -> 1
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = fill(barColor),
                        thickness = if (summary.points.size > 12) 8.dp else 16.dp,
                        shape = CorneredShape.rounded(allDp = 4f),
                    )
                ),
            ),
            startAxis = CoreVerticalAxis.rememberStart(
                label = rememberAxisLabelComponent(color = labelColor, textSize = 11.sp),
                line = rememberAxisLineComponent(fill = fill(lineColor)),
                guideline = rememberAxisGuidelineComponent(fill = fill(lineColor)),
                valueFormatter = startFormatter,
                itemPlacer = remember { CoreVerticalAxis.ItemPlacer.count({ 5 }) },
            ),
            bottomAxis = CoreHorizontalAxis.rememberBottom(
                label = rememberAxisLabelComponent(color = labelColor, textSize = 10.sp),
                line = rememberAxisLineComponent(fill = fill(lineColor)),
                guideline = null,
                valueFormatter = bottomFormatter,
                itemPlacer = remember(labelSpacing) {
                    CoreHorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing })
                },
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(240.dp),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
    )
}
