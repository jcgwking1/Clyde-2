package com.courseadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courseadvisor.data.CurriculumData
import com.courseadvisor.model.Course
import com.courseadvisor.service.CourseAdvisorEngine
import com.courseadvisor.ui.components.CourseRowItem
import com.courseadvisor.ui.components.StatCard
import com.courseadvisor.ui.components.subtleOceanContour
import com.courseadvisor.ui.theme.*

@Composable
fun OverviewScreen(
    engine: CourseAdvisorEngine,
    completedCodes: Set<String>,
    plannedCodes: Set<String>,
    confidence: Int,
    onNavigateToPlanner: () -> Unit,
    onSelectCourse: (Course) -> Unit,
    onGeneratePlan: () -> Unit
) {
    val completedCourses = CurriculumData.ALL_COURSES.filter { completedCodes.contains(it.code) }
    val earnedUnits = completedCourses.sumOf { it.units }
    val totalUnits = CurriculumData.TOTAL_UNITS
    val progressFraction = earnedUnits.toFloat() / totalUnits.toFloat()
    val progressPercent = (progressFraction * 100).toInt()

    val plannedCourses = CurriculumData.ALL_COURSES.filter {
        plannedCodes.contains(it.code) && !completedCodes.contains(it.code)
    }
    val plannedUnits = plannedCourses.sumOf { it.units }
    val workload = engine.evaluateWorkload(plannedUnits, confidence)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOcean)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Hero Journey Banner with Ocean Blue Gradient & Wave Accent
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = OceanPrimary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(OceanPrimary, OceanDark)
                            )
                        )
                        .subtleOceanContour()
                        .padding(20.dp)
                ) {
                    Column {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "YOUR DEGREE, IN MOTION",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Big ambitions.\nOne semester at a time.",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A thoughtful plan for your BS in Marine Science at MSU at Naawan.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToPlanner,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = OceanPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Explore my planner", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Stats Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Degree progress",
                    value = "$progressPercent%",
                    detail = "$earnedUnits of $totalUnits units",
                    icon = Icons.Default.School,
                    progress = progressFraction,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Courses done",
                    value = "${completedCourses.size}",
                    detail = "of ${CurriculumData.ALL_COURSES.size} curriculum courses",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Standing",
                    value = "3rd year",
                    detail = "First semester · AY 2025–2026",
                    icon = Icons.Default.DateRange,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Adviser load",
                    value = "${engine.recommendedUnitCap(confidence)} units",
                    detail = "Based on confidence $confidence/10",
                    icon = Icons.Default.Water,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Workload Gauge Card with Ocean Contour
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(SurfaceWhite, OceanSurface)))
                        .subtleOceanContour()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("A balanced semester", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                            Text("Room to learn. Space to breathe.", fontSize = 12.sp, color = TextMuted)
                        }
                        Icon(Icons.Default.Water, contentDescription = null, tint = OceanPrimary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${workload.stressScore}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OceanPrimary
                        )
                        Text(
                            text = "/100",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = if (workload.stressScore < 65) OceanLight else WarningAmberBg,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = workload.rating,
                                color = if (workload.stressScore < 65) OceanPrimary else WarningAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { workload.stressScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (workload.stressScore < 65) OceanPrimary else WarningAmber,
                        trackColor = OceanLight
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = workload.description,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Recommended Subjects Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Your next semester", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Text("Recommended based on prerequisites", fontSize = 12.sp, color = TextMuted)
                }
                TextButton(onClick = onNavigateToPlanner) {
                    Text("View all", color = OceanPrimary)
                }
            }
        }

        val previewList = plannedCourses.take(4)
        if (previewList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(SurfaceWhite, OceanSurface)))
                            .subtleOceanContour()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No courses planned for this semester.", color = TextMuted, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onGeneratePlan,
                            colors = ButtonDefaults.buttonColors(containerColor = OceanPrimary)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate my plan")
                        }
                    }
                }
            }
        } else {
            items(previewList) { course ->
                CourseRowItem(
                    course = course,
                    isCompleted = completedCodes.contains(course.code),
                    isEligible = engine.isEligible(course, completedCodes),
                    onClick = { onSelectCourse(course) }
                )
            }
        }
    }
}
