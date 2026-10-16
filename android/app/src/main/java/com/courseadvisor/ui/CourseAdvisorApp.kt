package com.courseadvisor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courseadvisor.data.CurriculumData
import com.courseadvisor.model.Course
import com.courseadvisor.model.StudentProfile
import com.courseadvisor.service.CourseAdvisorEngine
import com.courseadvisor.ui.screens.*
import com.courseadvisor.ui.theme.*

enum class NavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OVERVIEW("Overview", Icons.Default.Dashboard),
    PLANNER("Planner", Icons.Default.CalendarMonth),
    CATALOG("Catalog", Icons.AutoMirrored.Filled.MenuBook),
    PATHWAYS("Pathways", Icons.Default.AccountTree),
    SETTINGS("Settings", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseAdvisorApp() {
    val context = LocalContext.current
    val engine = remember { CourseAdvisorEngine() }
    val profile = remember {
        StudentProfile().apply {
            // Default sample profile matching web app (Terms 0..3 completed)
            completedCodes.addAll(CurriculumData.ALL_COURSES.filter { it.term < 4 }.map { it.code })
            plannedCodes.addAll(CurriculumData.ALL_COURSES.filter { it.term == 4 }.map { it.code })
        }
    }

    var currentTab by remember { mutableStateOf(NavTab.OVERVIEW) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    // State holders for triggering recomposition
    val completedSet = remember { mutableStateListOf<String>().apply { addAll(profile.completedCodes) } }
    val plannedSet = remember { mutableStateListOf<String>().apply { addAll(profile.plannedCodes) } }
    var selectedTerm by remember { mutableIntStateOf(profile.selectedTerm) }
    var confidence by remember { mutableIntStateOf(profile.confidence) }
    var studentName by remember { mutableStateOf(profile.name) }

    fun triggerAutoPlan() {
        val recommended = engine.generatePlan(completedSet.toSet(), confidence)
        plannedSet.clear()
        plannedSet.addAll(recommended.map { it.code })
        currentTab = NavTab.PLANNER
        Toast.makeText(context, "Plan generated · ${recommended.sumOf { it.units }} units planned", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "J.A.K.E.V. Edu",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = TextDark
                            )
                        }
                        Text(
                            text = "BS Marine Science · MSU at Naawan",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    Surface(
                        color = SageLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "AY 2025–2026",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                tonalElevation = 8.dp
            ) {
                NavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = SageLight,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                NavTab.OVERVIEW -> OverviewScreen(
                    engine = engine,
                    completedCodes = completedSet.toSet(),
                    plannedCodes = plannedSet.toSet(),
                    confidence = confidence,
                    onNavigateToPlanner = { currentTab = NavTab.PLANNER },
                    onSelectCourse = { selectedCourse = it },
                    onGeneratePlan = { triggerAutoPlan() }
                )
                NavTab.PLANNER -> PlannerScreen(
                    engine = engine,
                    completedCodes = completedSet.toSet(),
                    plannedCodes = plannedSet.toSet(),
                    selectedTerm = selectedTerm,
                    confidence = confidence,
                    onSelectTerm = {
                        selectedTerm = it
                        val termCourses = CurriculumData.ALL_COURSES.filter { c -> c.term == it && !completedSet.contains(c.code) }
                        plannedSet.clear()
                        plannedSet.addAll(termCourses.map { c -> c.code })
                    },
                    onRemoveFromPlan = { plannedSet.remove(it) },
                    onSelectCourse = { selectedCourse = it },
                    onNavigateToCatalog = { currentTab = NavTab.CATALOG },
                    onGeneratePlan = { triggerAutoPlan() }
                )
                NavTab.CATALOG -> CatalogScreen(
                    engine = engine,
                    completedCodes = completedSet.toSet(),
                    onSelectCourse = { selectedCourse = it }
                )
                NavTab.PATHWAYS -> PrerequisitesScreen(
                    engine = engine,
                    completedCodes = completedSet.toSet(),
                    onSelectCourse = { selectedCourse = it }
                )
                NavTab.SETTINGS -> SettingsScreen(
                    engine = engine,
                    studentName = studentName,
                    confidence = confidence,
                    onUpdateName = { studentName = it },
                    onUpdateConfidence = { confidence = it },
                    onResetProgress = {
                        completedSet.clear()
                        plannedSet.clear()
                        Toast.makeText(context, "Progress reset to clean slate", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Course Detail Modal Dialog
    if (selectedCourse != null) {
        val course = selectedCourse!!
        val isDone = completedSet.contains(course.code)
        val isEligible = engine.isEligible(course, completedSet.toSet())
        val missing = engine.getMissingPrerequisites(course, completedSet.toSet())
        val isPlanned = plannedSet.contains(course.code)

        AlertDialog(
            onDismissRequest = { selectedCourse = null },
            title = {
                Column {
                    Surface(color = SageLight, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "${course.code} · ${course.units} UNITS",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(course.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(
                        "${CurriculumData.TERMS[course.term]} · ${course.category.displayName}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = BackgroundMint,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Prerequisites", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                            Text(
                                text = if (course.pre.isEmpty()) "No course prerequisites" else course.pre.joinToString(", "),
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            if (course.code in listOf("MRB195", "MRB198")) {
                                Text("Requires complete Third-Year Standing.", fontSize = 11.sp, color = WarningAmber, fontWeight = FontWeight.Medium)
                            }
                            if (course.co != null) {
                                Text("Co-requisite: ${course.co}", fontSize = 12.sp, color = InfoBlue, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    if (!isDone && missing.isNotEmpty()) {
                        Text(
                            text = "Missing: ${missing.joinToString(", ")}",
                            fontSize = 12.sp,
                            color = WarningAmber
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isDone) {
                            completedSet.remove(course.code)
                        } else {
                            completedSet.add(course.code)
                            plannedSet.remove(course.code)
                        }
                        selectedCourse = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDone) WarningAmber else EmeraldPrimary
                    )
                ) {
                    Text(if (isDone) "Mark Incomplete" else "Mark as Completed")
                }
            },
            dismissButton = {
                if (!isDone) {
                    OutlinedButton(
                        onClick = {
                            if (!isEligible) {
                                Toast.makeText(context, "Prerequisites not met yet", Toast.LENGTH_SHORT).show()
                            } else {
                                if (isPlanned) {
                                    plannedSet.remove(course.code)
                                    Toast.makeText(context, "Removed from plan", Toast.LENGTH_SHORT).show()
                                } else {
                                    plannedSet.add(course.code)
                                    if (course.co != null && !completedSet.contains(course.co)) {
                                        plannedSet.add(course.co)
                                    }
                                    Toast.makeText(context, "Added to semester plan", Toast.LENGTH_SHORT).show()
                                }
                                selectedCourse = null
                            }
                        }
                    ) {
                        Text(if (isPlanned) "Remove from Plan" else "Add to Plan")
                    }
                }
            }
        )
    }
}
