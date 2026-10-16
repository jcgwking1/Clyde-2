package com.courseadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courseadvisor.service.CourseAdvisorEngine
import com.courseadvisor.ui.theme.*

@Composable
fun SettingsScreen(
    engine: CourseAdvisorEngine,
    studentName: String,
    confidence: Int,
    onUpdateName: (String) -> Unit,
    onUpdateConfidence: (Int) -> Unit,
    onResetProgress: () -> Unit
) {
    var nameText by remember { mutableStateOf(studentName) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMint)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Student Profile & Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text("Customize your academic pace and planning preferences.", fontSize = 12.sp, color = TextMuted)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Student Name", fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        onUpdateName(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Academic Program", fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = BackgroundMint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "BS in Marine Science · MSU at Naawan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subject Confidence", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    Surface(color = SageLight, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "$confidence / 10",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Slider(
                    value = confidence.toFloat(),
                    onValueChange = { onUpdateConfidence(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = SageLight
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("More support needed", fontSize = 11.sp, color = TextMuted)
                    Text("Ready for challenge", fontSize = 11.sp, color = TextMuted)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = SageLight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Recommended Pace: ${engine.recommendedUnitCap(confidence)} units / semester",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = if (confidence >= 7) "Maximum load for confident students." else if (confidence >= 4) "Balanced, manageable load." else "Lighter load with maximum study support.",
                            fontSize = 11.sp,
                            color = TextDark
                        )
                    }
                }
            }
        }

        // Reset Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Manage Stored Data", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Reset your sample completed courses and planned subjects.", fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset progress to clean slate")
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Progress?") },
            text = { Text("This will clear all completed subjects and planned enrollments.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetProgress()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
