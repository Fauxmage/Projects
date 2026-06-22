package com.example.pebtip.ui.screens.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pebtip.AccelRecord

@Composable
fun DataScreen(viewModel: DataViewModel) {
    val storedBatchCount by viewModel.storedBatchCount.collectAsState()
    val storedSampleCount by viewModel.storedSampleCount.collectAsState()
    val storedCount by viewModel.storedCount.collectAsState()
    val recentSamples by viewModel.recentSamples.collectAsState()
    val latestBattery by viewModel.latestBatteryLevel.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Data Overview",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = "Batches stored",
                    value = storedBatchCount,
                    unit = "",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Watch battery",
                    value = latestBattery ?: 0,
                    unit = if (latestBattery != null) "%" else "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = "Samples stored",
                    value = storedSampleCount,
                    unit = "",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Button(
                onClick = { viewModel.uploadAllStoredData() },
                enabled = !isUploading && storedCount > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isUploading) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.outline,
                        disabledContainerColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.outline
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.outline,
                        disabledContainerColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.outline
                    )

                }
            ) {
                Text(text = if (isUploading) "Uploading..." else "Upload Stored Data")
            }
        }

        if (uploadStatus != null) {
            item {
                Text(
                    text = uploadStatus!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (recentSamples.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "20 Most Recent Samples",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SampleTableHeader()
                    recentSamples.forEach { record ->
                        SampleRow(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleTableHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("X", "Y", "Z").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    )
}

@Composable
private fun SampleRow(record: AccelRecord) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(record.x, record.y, record.z).forEach { value ->
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}
