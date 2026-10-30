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
    var showOnboarding by remember { mutableStateOf(true) }

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
                            text = "${CurriculumData.currentProgram.code} · ${CurriculumData.currentProgram.campus}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showOnboarding = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Edit Profile", tint = EmeraldPrimary)
                    }
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

    // Student Onboarding / Profile Setup Dialog (No-Auth Intake Gate)
    if (showOnboarding) {
        var inputName by remember { mutableStateOf(studentName.ifBlank { "" }) }
        var selectedProgramIndex by remember { mutableIntStateOf(0) }
        var programExpanded by remember { mutableStateOf(false) }
        var selectedStandingIndex by remember { mutableIntStateOf(2) } // default 3rd Year
        val standings = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
        val standingTerms = listOf(0, 2, 4, 7)
        var autoFillPrior by remember { mutableStateOf(true) }
        var inputConfidence by remember { mutableFloatStateOf(confidence.toFloat()) }

        AlertDialog(
            onDismissRequest = { /* mandatory intake gate */ },
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Welcome to J.A.K.E.V. Edu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Enter your details to personalize your prerequisite path & stress ratings.", fontSize = 12.sp, color = TextMuted)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Student Full Name *") },
                        placeholder = { Text("e.g. Maria Clara Santos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val programsList = listOf(
                        "BS Marine Science · CFMS (162 Units)",
                        "BSBA Marketing Management · CBIT (169 Units)",
                        "Bachelor of Elementary Education (BEEd) · CESS (167 Units)",
                        "BS Environmental Science · CAS (168 Units)",
                        "BSA Animal Science · Agriculture (178 Units)",
                        "BSA Agronomy · Agriculture (178 Units)",
                        "BS Fisheries · CFMS (183 Units)"
                    )


                    Text("Degree Program & Prospectus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = BackgroundMint,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = programsList[selectedProgramIndex],
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextDark
                                    )
                                    Text("MSU at Naawan · AY 2025–2026", fontSize = 11.sp, color = TextMuted)
                                }
                                IconButton(onClick = { programExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Program", tint = EmeraldPrimary)
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = programExpanded,
                            onDismissRequest = { programExpanded = false }
                        ) {
                            programsList.forEachIndexed { idx, progName ->
                                DropdownMenuItem(
                                    text = { Text(progName, fontSize = 13.sp) },
                                    onClick = {
                                        selectedProgramIndex = idx
                                        programExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Current Year Standing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        standings.forEachIndexed { idx, st ->
                            FilterChip(
                                selected = selectedStandingIndex == idx,
                                onClick = { selectedStandingIndex = idx },
                                label = { Text(st, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (selectedStandingIndex > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = autoFillPrior, onCheckedChange = { autoFillPrior = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-mark prior years' subjects as passed", fontSize = 11.sp, color = TextDark)
                        }
                    }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Study Confidence Pace", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                            Text("${inputConfidence.toInt()}/10", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                        Slider(
                            value = inputConfidence,
                            onValueChange = { inputConfidence = it },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                        Text(
                            text = if (inputConfidence <= 3f) "🌿 Gentle pace (~12 units suggested limit)"
                            else if (inputConfidence <= 7f) "⚖️ Balanced pace (~15 units suggested limit)"
                            else "🚀 Intensive pace (~18 units suggested limit)",
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = inputName.trim()
                        if (name.isNotBlank()) {
                            val programIds = listOf("BSMS", "BSBA-MM", "BEED", "BSES", "BSA-ANSCI", "BSA-AGRON", "BSFI")
                            CurriculumData.setProgram(programIds[selectedProgramIndex])
                            studentName = name
                            confidence = inputConfidence.toInt()
                            val targetTerm = standingTerms[selectedStandingIndex]
                            selectedTerm = targetTerm
                            completedSet.clear()
                            if (autoFillPrior && targetTerm > 0) {
                                val prior = CurriculumData.ALL_COURSES.filter { it.term < targetTerm }.map { it.code }
                                completedSet.addAll(prior)
                            }
                            plannedSet.clear()
                            showOnboarding = false
                            Toast.makeText(context, "Welcome, $name! Loaded ${CurriculumData.currentProgram.name}.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = inputName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Start Planning")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        studentName = "Alex Santos"
                        confidence = 7
                        selectedTerm = 4
                        completedSet.clear()
                        completedSet.addAll(CurriculumData.ALL_COURSES.filter { it.term < 4 }.map { it.code })
                        plannedSet.clear()
                        plannedSet.addAll(CurriculumData.ALL_COURSES.filter { it.term == 4 }.map { it.code })
                        showOnboarding = false
                        Toast.makeText(context, "Sample student profile loaded", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Explore Demo Profile")
                }
            }
        )
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
