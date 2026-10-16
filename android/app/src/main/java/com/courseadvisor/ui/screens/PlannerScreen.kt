package com.courseadvisor.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courseadvisor.data.CurriculumData
import com.courseadvisor.model.Course
import com.courseadvisor.service.CourseAdvisorEngine
import com.courseadvisor.ui.components.CourseRowItem
import com.courseadvisor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    engine: CourseAdvisorEngine,
    completedCodes: Set<String>,
    plannedCodes: Set<String>,
    selectedTerm: Int,
    confidence: Int,
    onSelectTerm: (Int) -> Unit,
    onRemoveFromPlan: (String) -> Unit,
    onSelectCourse: (Course) -> Unit,
    onNavigateToCatalog: () -> Unit,
    onGeneratePlan: () -> Unit
) {
    val context = LocalContext.current
    val plannedCourses = CurriculumData.ALL_COURSES.filter {
        plannedCodes.contains(it.code) && !completedCodes.contains(it.code)
    }
    val plannedUnits = plannedCourses.sumOf { it.units }
    val workload = engine.evaluateWorkload(plannedUnits, confidence)
    var expandedTermDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMint)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Term Picker & Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Academic Term", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedTermDropdown = true },
                            color = BackgroundMint,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = CurriculumData.TERMS[selectedTerm],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedTermDropdown,
                            onDismissRequest = { expandedTermDropdown = false }
                        ) {
                            CurriculumData.TERMS.forEachIndexed { index, termName ->
                                DropdownMenuItem(
                                    text = { Text(termName, fontSize = 14.sp) },
                                    onClick = {
                                        onSelectTerm(index)
                                        expandedTermDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${plannedCourses.size} courses · $plannedUnits units",
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Surface(
                            color = if (workload.stressScore < 65) SuccessGreenBg else WarningAmberBg,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Workload: ${workload.stressScore}/100",
                                color = if (workload.stressScore < 65) SuccessGreen else WarningAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGeneratePlan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-plan", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToCatalog,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add subject", fontSize = 13.sp, color = EmeraldPrimary)
                }

                FilledTonalButton(
                    onClick = {
                        Toast.makeText(context, "Plan exported as plain text (${plannedUnits} units)", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Course List Header
        item {
            Text(
                text = "Planned Subjects for Enrollment",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        if (plannedCourses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No subjects planned yet.", fontWeight = FontWeight.SemiBold, color = TextDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Auto-generate a plan based on prerequisites or add from catalog.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(plannedCourses) { course ->
                CourseRowItem(
                    course = course,
                    isCompleted = completedCodes.contains(course.code),
                    isEligible = engine.isEligible(course, completedCodes),
                    onClick = { onSelectCourse(course) },
                    trailingAction = {
                        IconButton(
                            onClick = { onRemoveFromPlan(course.code) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = WarningAmber, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
}
