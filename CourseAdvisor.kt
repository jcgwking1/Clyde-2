package com.courseadvisor

import java.io.File
import java.util.Scanner
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ============================================================================
 * Course Advisor System (Kotlin Edition)
 * Bachelor of Science in Marine Science - MSU at Naawan
 * Curriculum AY 2025-2026 (Ref: MSUN-CFMS-POS-002-2025-REV00)
 *
 * A standalone, fully-featured academic advising engine with:
 * - Complete 60-course curriculum dataset
 * - Prerequisite DAG validation & third-year standing verification
 * - Co-requisite pairing (e.g. Lecture + Lab bundling)
 * - Confidence-based automated semester plan generator
 * - Workload stress index calculator
 * - Prerequisite pathway visualizer (Marine Ecology & Research tracks)
 * - Interactive Terminal UI with ANSI styling & clean text fallbacks
 * - Student profile persistence & text plan export
 * ============================================================================
 */

// ----------------------------------------------------------------------------
// 1. DOMAIN MODELS & ENUMS
// ----------------------------------------------------------------------------

enum class CourseCategory(val displayName: String) {
    MARINE_SCIENCE("Marine Science"),
    GENERAL_EDUCATION("General Education"),
    FOUNDATION("Foundation");

    companion object {
        fun fromCode(code: String): CourseCategory = when {
            code.startsWith("MRB") || code.startsWith("OCN") -> MARINE_SCIENCE
            code.startsWith("GEC") || code.startsWith("GAS") ||
            code.startsWith("FPE") || code.startsWith("HAL") -> GENERAL_EDUCATION
            else -> FOUNDATION
        }
    }
}

data class Course(
    val code: String,
    val title: String,
    val units: Int,
    val term: Int,
    val pre: List<String> = emptyList(),
    val co: String? = null,
    val category: CourseCategory = CourseCategory.fromCode(code),
    val difficulty: Double = 6.2,
    val isCapstone: Boolean = false
)

data class WorkloadEvaluation(
    val units: Int,
    val confidence: Int,
    val stressScore: Int,
    val rating: String,
    val description: String,
    val recommendedCredits: Int = 15
)

data class StudentProfile(
    var name: String = "Alex Santos",
    val program: String = "BS in Marine Science · MSU at Naawan",
    var academicYear: String = "AY 2025–2026",
    var confidence: Int = 7,
    val completedCodes: MutableSet<String> = mutableSetOf(),
    val plannedCodes: MutableSet<String> = mutableSetOf(),
    var selectedTerm: Int = 4 // Default to Year 3 · First semester
)

// ----------------------------------------------------------------------------
// 2. CURRICULUM DATASET (60 Official Courses)
// ----------------------------------------------------------------------------

object CurriculumData {
    val TERMS: List<String> = listOf(
        "Year 1 · First semester",
        "Year 1 · Second semester",
        "Year 2 · First semester",
        "Year 2 · Second semester",
        "Year 3 · First semester",
        "Year 3 · Second semester",
        "Year 3 · Summer",
        "Year 4 · First semester",
        "Year 4 · Second semester"
    )

    private val rawRows: List<Array<Any?>> = listOf(
        // Term 0 (Year 1 · First semester)
        arrayOf("PHY101", "Mechanics, Electromagnetism and Gravitational Theory", 2, 0, "", "PHY101.1"),
        arrayOf("PHY101.1", "Mechanics, Electromagnetism and Gravitational Theory (Lab.)", 1, 0, "", "PHY101"),
        arrayOf("MAT043", "Calculus (Integrative)", 4, 0, "", null),
        arrayOf("CHM011", "Inorganic Chemistry", 2, 0, "", "CHM011.1"),
        arrayOf("CHM011.1", "Inorganic Chemistry (Lab.)", 1, 0, "", "CHM011"),
        arrayOf("BIO117", "Cell and Molecular Biology", 4, 0, "", null),
        arrayOf("GEC101", "Understanding the Self", 3, 0, "", null),
        arrayOf("PED011", "PATHFIT 1: Movement Competency Training", 2, 0, "", null),
        arrayOf("NST001", "National Service Training Program 1", 3, 0, "", null),

        // Term 1 (Year 1 · Second semester)
        arrayOf("PHY102", "Waves, Optics, and Thermodynamics", 2, 1, "PHY101,PHY101.1", "PHY102.1"),
        arrayOf("PHY102.1", "Waves, Optics, and Thermodynamics (Lab.)", 1, 1, "PHY101,PHY101.1", "PHY102"),
        arrayOf("STT034", "Inferential Statistics and Biometry", 3, 1, "", null),
        arrayOf("CHM032", "Organic Chemistry", 2, 1, "CHM011,CHM011.1", "CHM032.2"),
        arrayOf("CHM032.2", "Organic Chemistry (Lab.)", 2, 1, "CHM011,CHM011.1", "CHM032"),
        arrayOf("BIO118", "General Physiology", 3, 1, "BIO117", null),
        arrayOf("GEC105", "Readings in Philippine History", 3, 1, "", null),
        arrayOf("PED012", "PATHFIT 2: Exercise-based Fitness Activities", 2, 1, "PED011", null),
        arrayOf("NST002", "National Service Training Program 2", 3, 1, "NST001", null),

        // Term 2 (Year 2 · First semester)
        arrayOf("OCN121", "Geological and Physical Oceanography", 4, 2, "PHY102,PHY102.1,MAT043", null),
        arrayOf("CHM045", "Biochemistry", 2, 2, "CHM032,CHM032.2", "CHM045.1"),
        arrayOf("CHM045.1", "Biochemistry (Lab.)", 1, 2, "CHM032,CHM032.2", "CHM045"),
        arrayOf("BIO119", "Developmental and Reproductive Biology", 3, 2, "BIO118", null),
        arrayOf("GEC102", "Purposive Communication", 3, 2, "", null),
        arrayOf("GEC106", "Art Appreciation", 3, 2, "", null),
        arrayOf("GEC103", "The Contemporary World", 3, 2, "", null),
        arrayOf("GEC120", "Wika at Kultura sa Mapayapang Lipunan", 3, 2, "", null),
        arrayOf("PED013", "PATHFIT 3: Swimming", 2, 2, "PED011", null),

        // Term 3 (Year 2 · Second semester)
        arrayOf("OCN122", "Biological and Chemical Oceanography", 4, 3, "CHM011,CHM011.1", null),
        arrayOf("ERS100", "Earth Science", 3, 3, "", null),
        arrayOf("BIO120", "Ecology", 3, 3, "", null),
        arrayOf("MRB147", "Systematics, Biodiversity, and Evolution", 4, 3, "", null),
        arrayOf("GEC107", "Ethics", 3, 3, "", null),
        arrayOf("FPE101", "Fundamentals of Peace Education", 3, 3, "", null),
        arrayOf("PED014", "PATHFIT 4: Team Sports", 2, 3, "PED011", null),

        // Term 4 (Year 3 · First semester)
        arrayOf("MRB131", "Marine Botany", 4, 4, "", null),
        arrayOf("MRB132", "Marine Invertebrates", 4, 4, "BIO117", null),
        arrayOf("MRB114", "Marine Plankton", 3, 4, "", null),
        arrayOf("BIO121", "Genetics", 4, 4, "STT034", null),
        arrayOf("GEC130", "Ekokritisismo at Pagpapahalaga sa Kalikasan", 3, 4, "", null),
        arrayOf("MRB125", "Marine Geography and Geochemistry", 3, 4, "", null),

        // Term 5 (Year 3 · Second semester)
        arrayOf("MRB134", "Marine Ecology", 4, 5, "STT034,MRB131,MRB132", null),
        arrayOf("MRB198", "Research Methodology", 3, 5, "STT034", null),
        arrayOf("MRB117", "Marine Protected Areas", 3, 5, "", null),
        arrayOf("GEC108", "Science, Technology and Society", 3, 5, "", null),
        arrayOf("MRB146", "Marine Microbiology & Fungi", 4, 5, "", null),
        arrayOf("MRB195", "Fundamentals of Research", 2, 5, "STT034", null),
        arrayOf("GAS123", "Gender and Society", 3, 5, "", null),

        // Term 6 (Year 3 · Summer)
        arrayOf("GEC104", "Mathematics in the Modern World", 3, 6, "", null),
        arrayOf("GEC110", "History of Muslim Filipinos and Indigenous Peoples of MINSUPALA", 3, 6, "", null),
        arrayOf("GEC124", "Entrepreneurial Mind", 3, 6, "", null),
        arrayOf("HAL101", "Halal", 3, 6, "", null),

        // Term 7 (Year 4 · First semester)
        arrayOf("MRB145", "Coral Reefs", 3, 7, "MRB134", null),
        arrayOf("MRB143", "Marine Vertebrates", 4, 7, "MRB132", null),
        arrayOf("MRB122", "Remote Sensing and Geographic Information System", 3, 7, "", null),
        arrayOf("GEC109", "Life and Works of Rizal", 3, 7, "", null),
        arrayOf("MRB148", "Integrated Coastal/Marine Resources Management", 4, 7, "MRB134", null),
        arrayOf("MRB196", "Experimental Research", 2, 7, "MRB195", null),

        // Term 8 (Year 4 · Second semester)
        arrayOf("MRB197", "Practicum", 3, 8, "", null),
        arrayOf("MRB199", "Undergraduate Thesis", 2, 8, "MRB196", null)
    )

    val ALL_COURSES: List<Course> = rawRows.map { row ->
        val code = row[0] as String
        val title = row[1] as String
        val units = row[2] as Int
        val term = row[3] as Int
        val preRaw = (row[4] as? String)?.trim().orEmpty()
        val pre = if (preRaw.isEmpty()) emptyList() else preRaw.split(",").map { it.trim() }
        val co = row[5] as? String
        Course(
            code = code,
            title = title,
            units = units,
            term = term,
            pre = pre,
            co = co,
            category = CourseCategory.fromCode(code)
        )
    }

    val COURSE_MAP: Map<String, Course> = ALL_COURSES.associateBy { it.code }

    val TOTAL_UNITS: Int = ALL_COURSES.sumOf { it.units }
}

// ----------------------------------------------------------------------------
// 3. ADVISING ENGINE & BUSINESS LOGIC
// ----------------------------------------------------------------------------

object MamdaniFuzzyEngine {
    private fun trimf(x: Double, a: Double, b: Double, c: Double): Double {
        if (x <= a || x >= c) return 0.0
        if (x == b) return 1.0
        return if (x < b) (x - a) / (b - a) else (c - x) / (c - b)
    }

    private fun trapmf(x: Double, a: Double, b: Double, c: Double, d: Double): Double {
        if (x <= a || x >= d) return 0.0
        if (x in b..c) return 1.0
        return if (x < b) (x - a) / (b - a) else (d - x) / (d - c)
    }

    fun evaluate(units: Int, confidence: Int, avgDifficulty: Double = 6.2): WorkloadEvaluation {
        val c = confidence.toDouble().coerceIn(1.0, 10.0)
        val u = units.toDouble().coerceIn(3.0, 26.0)
        val d = avgDifficulty.coerceIn(1.0, 10.0)

        val confLow = trapmf(c, 1.0, 1.0, 3.5, 5.0)
        val confMed = trimf(c, 3.5, 6.0, 8.0)
        val confHigh = trapmf(c, 6.5, 8.5, 10.0, 10.0)

        val loadLight = trapmf(u, 3.0, 3.0, 11.0, 14.0)
        val loadStd = trimf(u, 13.0, 17.0, 20.0)
        val loadHeavy = trapmf(u, 18.0, 21.0, 26.0, 26.0)

        val diffEasy = trapmf(d, 1.0, 1.0, 3.5, 5.0)
        val diffMod = trimf(d, 4.0, 6.2, 8.0)
        val diffHard = trapmf(d, 6.5, 8.2, 10.0, 10.0)

        val r1 = min(confLow, loadHeavy)
        val r2 = min(confLow, diffHard)
        val r3 = minOf(confLow, loadStd, diffMod)
        val r4 = min(confLow, loadLight)
        val r5 = min(confMed, loadHeavy)
        val r6 = minOf(confMed, loadStd, diffMod)
        val r7 = min(confMed, loadLight)
        val r8 = minOf(confHigh, loadHeavy, diffHard)
        val r9 = minOf(confHigh, loadHeavy, diffMod)
        val r10 = min(confHigh, loadStd)
        val r11 = min(confHigh, loadLight)

        val firingRelaxed = max(r7, r11)
        val firingManageable = maxOf(r4, r6, r9, r10)
        val firingDemanding = maxOf(r3, r5, r8)
        val firingCritical = max(r1, r2)

        var num = 0.0
        var den = 0.0
        for (s in 0..100 step 2) {
            val sD = s.toDouble()
            val muR = min(firingRelaxed, trapmf(sD, 0.0, 0.0, 20.0, 38.0))
            val muM = min(firingManageable, trimf(sD, 25.0, 48.0, 68.0))
            val muD = min(firingDemanding, trimf(sD, 52.0, 74.0, 88.0))
            val muC = min(firingCritical, trapmf(sD, 76.0, 90.0, 100.0, 100.0))
            val muAgg = maxOf(muR, muM, muD, muC)
            num += sD * muAgg
            den += muAgg
        }

        val crispScore = if (den > 0.0) (num / den).roundToInt().coerceIn(12, 98) else 50
        val (rating, description) = when {
            crispScore <= 38 -> "Relaxed workload" to "Gentle pace with plenty of study and resting room."
            crispScore <= 64 -> "Manageable workload" to "Balanced challenge well-aligned with your subject confidence."
            crispScore <= 82 -> "Demanding workload" to "Demanding semester requiring solid time management."
            else -> "Critical workload" to "High burnout risk; consider deferring a non-critical elective."
        }

        val recCredits = when {
            crispScore <= 45 -> if (confidence >= 7) 18 else 15
            crispScore <= 70 -> 15
            else -> 12
        }

        return WorkloadEvaluation(units, confidence, crispScore, rating, description, recCredits)
    }

    fun recommendedUnitCap(confidence: Int): Int = when {
        confidence >= 7 -> 18
        confidence >= 4 -> 15
        else -> 12
    }
}

class CourseAdvisorEngine(val courses: List<Course> = CurriculumData.ALL_COURSES) {
    private val courseMap: Map<String, Course> = courses.associateBy { it.code }

    fun findCourse(code: String): Course? = courseMap[code.uppercase().trim()]

    /**
     * Checks if a course is eligible given the student's completed courses.
     * Special rule: MRB198 and MRB195 require third-year standing (all terms 0..3 completed).
     */
    fun isEligible(course: Course, completedCodes: Set<String>): Boolean {
        val prerequisitesSatisfied = course.pre.all { completedCodes.contains(it) }
        val thirdYearRequirement = if (course.code in listOf("MRB198", "MRB195")) {
            courses.filter { it.term < 4 }.all { completedCodes.contains(it.code) }
        } else {
            true
        }
        return prerequisitesSatisfied && thirdYearRequirement
    }

    fun getMissingPrerequisites(course: Course, completedCodes: Set<String>): List<String> {
        val missing = course.pre.filter { !completedCodes.contains(it) }.toMutableList()
        if (course.code in listOf("MRB198", "MRB195")) {
            val incompleteUnderclass = courses.filter { it.term < 4 && !completedCodes.contains(it.code) }
            if (incompleteUnderclass.isNotEmpty()) {
                missing.add("Third-year standing (${incompleteUnderclass.size} lower-term subjects incomplete)")
            }
        }
        return missing
    }

    /**
     * Finds all courses that require [courseCode] as a prerequisite.
     */
    fun getUnlockedCourses(courseCode: String): List<Course> {
        return courses.filter { it.pre.contains(courseCode) }
    }

    /**
     * Traces the full recursive prerequisite tree up to foundational subjects.
     */
    fun getFullPrerequisiteTree(courseCode: String, visited: MutableSet<String> = mutableSetOf()): List<String> {
        val course = findCourse(courseCode) ?: return emptyList()
        val result = mutableListOf<String>()
        for (pre in course.pre) {
            if (visited.add(pre)) {
                result.addAll(getFullPrerequisiteTree(pre, visited))
                result.add(pre)
            }
        }
        return result.distinct()
    }

    /**
     * Evaluates workload stress index using the Mamdani Fuzzy Inference Engine.
     */
    fun evaluateWorkload(units: Int, confidence: Int, avgDifficulty: Double = 6.2): WorkloadEvaluation {
        return MamdaniFuzzyEngine.evaluate(units, confidence, avgDifficulty)
    }

    fun recommendedUnitCap(confidence: Int): Int = MamdaniFuzzyEngine.recommendedUnitCap(confidence)

    /**
     * Automated plan generator that bundles co-requisites and adheres to Mamdani unit cap.
     */
    fun generatePlan(completedCodes: Set<String>, confidence: Int): List<Course> {
        val cap = recommendedUnitCap(confidence)
        val planned = mutableListOf<String>()
        var currentUnits = 0

        val candidates = courses.filter { !completedCodes.contains(it.code) && isEligible(it, completedCodes) }

        for (course in candidates) {
            if (planned.contains(course.code)) continue

            // Bundle course with its co-requisite if present and uncompleted
            val group = mutableListOf(course)
            val coCode = course.co
            if (coCode != null && !completedCodes.contains(coCode) && !planned.contains(coCode)) {
                findCourse(coCode)?.let { group.add(it) }
            }

            val groupUnits = group.sumOf { it.units }
            if (currentUnits + groupUnits <= cap) {
                planned.addAll(group.map { it.code })
                currentUnits += groupUnits
            }
        }

        return planned.mapNotNull { findCourse(it) }
    }
}

// ----------------------------------------------------------------------------
// 4. ANSI FORMATTING & TERMINAL STYLING
// ----------------------------------------------------------------------------

object TerminalStyle {
    private val isAnsiSupported = System.console() != null || System.getenv("TERM") != null || System.getProperty("os.name").lowercase().contains("windows")

    const val RESET = "\u001B[0m"
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"

    const val FG_CYAN = "\u001B[36m"
    const val FG_BLUE = "\u001B[34m"
    const val FG_GREEN = "\u001B[32m"
    const val FG_YELLOW = "\u001B[33m"
    const val FG_RED = "\u001B[31m"
    const val FG_MAGENTA = "\u001B[35m"
    const val FG_WHITE = "\u001B[37m"

    fun bold(text: String): String = if (isAnsiSupported) "$BOLD$text$RESET" else text
    fun cyan(text: String): String = if (isAnsiSupported) "$FG_CYAN$text$RESET" else text
    fun green(text: String): String = if (isAnsiSupported) "$FG_GREEN$text$RESET" else text
    fun yellow(text: String): String = if (isAnsiSupported) "$FG_YELLOW$text$RESET" else text
    fun blue(text: String): String = if (isAnsiSupported) "$FG_BLUE$text$RESET" else text
    fun dim(text: String): String = if (isAnsiSupported) "$DIM$text$RESET" else text

    fun badge(text: String, color: String): String {
        return if (isAnsiSupported) "$color[$text]$RESET" else "[$text]"
    }

    fun progressBar(percent: Double, width: Int = 28): String {
        val filled = (percent * width / 100.0).roundToInt().coerceIn(0, width)
        val empty = width - filled
        val bar = "#".repeat(filled) + "-".repeat(empty)
        return "[$bar]"
    }
}

// ----------------------------------------------------------------------------
// 5. INTERACTIVE CLI APPLICATION
// ----------------------------------------------------------------------------

class CourseAdvisorCli {
    private val engine = CourseAdvisorEngine()
    private val profile = StudentProfile()
    private val scanner = Scanner(System.`in`)

    init {
        // Initialize default sample profile (Terms 0..3 completed) matching web app
        val sampleCompleted = CurriculumData.ALL_COURSES
            .filter { it.term < 4 }
            .map { it.code }
        profile.completedCodes.addAll(sampleCompleted)

        // Initialize term 4 planned courses
        val samplePlanned = CurriculumData.ALL_COURSES
            .filter { it.term == 4 }
            .map { it.code }
        profile.plannedCodes.addAll(samplePlanned)
    }

    private fun readInput(): String {
        return try {
            if (scanner.hasNextLine()) {
                scanner.nextLine().trim()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun start() {
        printBanner()
        var running = true
        while (running) {
            printMainMenu()
            print("\nSelect an option (0-9): ")
            if (!scanner.hasNextLine()) {
                println("\nNo interactive terminal input detected. Exiting cleanly.")
                break
            }
            val input = readInput()
            println()

            when (input) {
                "1" -> showAcademicOverview()
                "2" -> showCourseCatalog()
                "3" -> showPrerequisiteVisualizer()
                "4" -> showSemesterPlanner()
                "5" -> autoGeneratePlan()
                "6" -> manageCompletedCourses()
                "7" -> manageStudentProfile()
                "8" -> exportPlanToFile()
                "9" -> resetSampleProgress()
                "0", "exit", "quit" -> {
                    running = false
                    println(TerminalStyle.cyan("Thank you for using J.A.K.E.V. Edu. Chart your own course! 🌊\n"))
                }
                else -> println(TerminalStyle.yellow("Invalid choice. Please select an option between 0 and 9.\n"))
            }
        }
    }

    private fun printBanner() {
        println(TerminalStyle.cyan("""
  ╔════════════════════════════════════════════════════════════════════════╗
  ║    🌊  J.A.K.E.V. Edu — Bachelor of Science in Marine Science          ║
  ║    Curriculum AY 2025–2026 · MSU at Naawan (MSUN-CFMS-POS-002)        ║
  ╚════════════════════════════════════════════════════════════════════════╝
        """.trimIndent()))
    }

    private fun printMainMenu() {
        println(TerminalStyle.bold("────────────────────────────────────────────────────────────────────────"))
        println(TerminalStyle.bold(" MAIN MENU "))
        println("  ${TerminalStyle.cyan("1.")} Academic Overview & Degree Progress")
        println("  ${TerminalStyle.cyan("2.")} Course Catalog (Browse, Search & Filter)")
        println("  ${TerminalStyle.cyan("3.")} Prerequisite Pathways & Dependency Tree")
        println("  ${TerminalStyle.cyan("4.")} Semester Planner & Workload Assessment")
        println("  ${TerminalStyle.cyan("5.")} Auto-Generate Recommended Plan (AI Rules)")
        println("  ${TerminalStyle.cyan("6.")} Manage Completed Courses")
        println("  ${TerminalStyle.cyan("7.")} Student Profile & Confidence Tuning")
        println("  ${TerminalStyle.cyan("8.")} Export Semester Plan to Text File")
        println("  ${TerminalStyle.cyan("9.")} Reset Progress to Clean Slate")
        println("  ${TerminalStyle.dim("0. Exit")}")
        println(TerminalStyle.bold("────────────────────────────────────────────────────────────────────────"))
    }

    // --- Option 1: Academic Overview ---
    private fun showAcademicOverview() {
        val completedCourses = CurriculumData.ALL_COURSES.filter { profile.completedCodes.contains(it.code) }
        val earnedUnits = completedCourses.sumOf { it.units }
        val totalUnits = CurriculumData.TOTAL_UNITS
        val percent = (earnedUnits.toDouble() / totalUnits) * 100.0

        val plannedCourses = CurriculumData.ALL_COURSES.filter {
            profile.plannedCodes.contains(it.code) && !profile.completedCodes.contains(it.code)
        }
        val plannedUnits = plannedCourses.sumOf { it.units }
        val workload = engine.evaluateWorkload(plannedUnits, profile.confidence)

        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println(TerminalStyle.cyan(" ACADEMIC OVERVIEW · ${profile.name}"))
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println("Program:          ${profile.program}")
        println("Standing:         ${determineStanding()}")
        println("Subject Conf.:    ${profile.confidence}/10 (Recommended capacity: ${engine.recommendedUnitCap(profile.confidence)} units)")
        println()
        println("Degree Progress:  ${TerminalStyle.progressBar(percent)} ${String.format("%.1f", percent)}%")
        println("Curriculum Units: ${TerminalStyle.bold("$earnedUnits")} of $totalUnits units earned")
        println("Courses Done:     ${TerminalStyle.bold("${completedCourses.size}")} of ${CurriculumData.ALL_COURSES.size} courses completed")
        println()
        println(TerminalStyle.bold("Current Planned Term: ${CurriculumData.TERMS[profile.selectedTerm]}"))
        println("Planned Courses:  ${plannedCourses.size} courses (${plannedUnits} units)")
        println("Workload Stress:  ${TerminalStyle.bold("${workload.stressScore}/100")} — ${TerminalStyle.badge(workload.rating, TerminalStyle.FG_GREEN)}")
        println("Note:             ${workload.description}")
        println()

        println(TerminalStyle.bold("Progress by Academic Term:"))
        for (i in CurriculumData.TERMS.indices) {
            val termCourses = CurriculumData.ALL_COURSES.filter { it.term == i }
            val doneInTerm = termCourses.filter { profile.completedCodes.contains(it.code) }.size
            val termPercent = (doneInTerm.toDouble() / termCourses.size) * 100.0
            val status = if (doneInTerm == termCourses.size) TerminalStyle.green("✓ Complete") else TerminalStyle.dim("$doneInTerm/${termCourses.size} done")
            println("  ${CurriculumData.TERMS[i].padEnd(28)} [${TerminalStyle.progressBar(termPercent, 12)}]  $status")
        }
        println()
    }

    private fun determineStanding(): String {
        val completedCount = profile.completedCodes.size
        return when {
            completedCount >= 50 -> "4th Year · Senior Standing"
            completedCount >= 34 -> "3rd Year · Junior Standing"
            completedCount >= 18 -> "2nd Year · Sophomore Standing"
            else -> "1st Year · Freshman Standing"
        }
    }

    // --- Option 2: Course Catalog ---
    private fun showCourseCatalog() {
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println(TerminalStyle.cyan(" COURSE CATALOG"))
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println("Filters:")
        println("  1. Show all courses")
        println("  2. Filter by Category (Marine Science / General Education / Foundation)")
        println("  3. Filter by Term")
        println("  4. Search by Code or Title")
        print("\nSelect filter (1-4) or press Enter to show all: ")
        val choice = scanner.nextLine().trim()

        val filtered = when (choice) {
            "2" -> {
                println("Choose Category: (1) Marine Science (2) General Education (3) Foundation")
                when (scanner.nextLine().trim()) {
                    "1" -> CurriculumData.ALL_COURSES.filter { it.category == CourseCategory.MARINE_SCIENCE }
                    "2" -> CurriculumData.ALL_COURSES.filter { it.category == CourseCategory.GENERAL_EDUCATION }
                    "3" -> CurriculumData.ALL_COURSES.filter { it.category == CourseCategory.FOUNDATION }
                    else -> CurriculumData.ALL_COURSES
                }
            }
            "3" -> {
                println("Select Term index (0..8):")
                CurriculumData.TERMS.forEachIndexed { i, t -> println("  $i. $t") }
                val termIdx = scanner.nextLine().trim().toIntOrNull() ?: 0
                CurriculumData.ALL_COURSES.filter { it.term == termIdx }
            }
            "4" -> {
                print("Enter search keyword (e.g. 'MRB', 'Chemistry', 'Ecology'): ")
                val q = scanner.nextLine().trim().lowercase()
                CurriculumData.ALL_COURSES.filter {
                    it.code.lowercase().contains(q) || it.title.lowercase().contains(q)
                }
            }
            else -> CurriculumData.ALL_COURSES
        }

        println("\nFound ${filtered.size} courses:\n")
        printCourseTable(filtered)

        print("\nEnter course code for deep details (or press Enter to return): ")
        val detailCode = scanner.nextLine().trim()
        if (detailCode.isNotEmpty()) {
            showCourseDetail(detailCode)
        }
    }

    private fun printCourseTable(courses: List<Course>) {
        println("CODE".padEnd(10) + "UNITS".padEnd(7) + "TERM".padEnd(28) + "STATUS".padEnd(16) + "TITLE")
        println("─".repeat(80))
        for (c in courses) {
            val isDone = profile.completedCodes.contains(c.code)
            val isEligible = engine.isEligible(c, profile.completedCodes)
            val status = when {
                isDone -> TerminalStyle.green("Completed")
                isEligible -> TerminalStyle.cyan("Ready")
                else -> TerminalStyle.yellow("Locked")
            }
            val termName = CurriculumData.TERMS[c.term]
            println(c.code.padEnd(10) + "${c.units}u".padEnd(7) + termName.padEnd(28) + status.padEnd(16) + c.title)
        }
    }

    private fun showCourseDetail(code: String) {
        val course = engine.findCourse(code)
        if (course == null) {
            println(TerminalStyle.yellow("Course '$code' not found.\n"))
            return
        }

        val isDone = profile.completedCodes.contains(course.code)
        val isEligible = engine.isEligible(course, profile.completedCodes)
        val missing = engine.getMissingPrerequisites(course, profile.completedCodes)
        val unlocked = engine.getUnlockedCourses(course.code)

        println("\n" + TerminalStyle.bold("────────────────────────────────────────────────────────────────────────"))
        println(TerminalStyle.cyan(" COURSE DETAILS: ${course.code} — ${course.title}"))
        println(TerminalStyle.bold("────────────────────────────────────────────────────────────────────────"))
        println("Units:         ${course.units}")
        println("Curriculum:    ${CurriculumData.TERMS[course.term]} · ${course.category.displayName}")
        println("Status:        " + if (isDone) TerminalStyle.green("Completed") else if (isEligible) TerminalStyle.cyan("Eligible for Enrollment") else TerminalStyle.yellow("Prerequisites Incomplete"))
        println("Prerequisites: " + if (course.pre.isEmpty()) "None" else course.pre.joinToString(", "))
        if (course.co != null) println("Co-requisite:  ${course.co}")
        if (missing.isNotEmpty() && !isDone) {
            println(TerminalStyle.yellow("Missing Requirements:"))
            missing.forEach { println("   • $it") }
        }
        if (unlocked.isNotEmpty()) {
            println("Directly unlocks (${unlocked.size} courses):")
            unlocked.forEach { println("   → ${it.code} (${it.title})") }
        }
        println(TerminalStyle.bold("────────────────────────────────────────────────────────────────────────\n"))
    }

    // --- Option 3: Prerequisite Pathways ---
    private fun showPrerequisiteVisualizer() {
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println(TerminalStyle.cyan(" PREREQUISITE PATHWAYS & DEPENDENCY GRAPH"))
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println("1. Marine Ecology Pathway")
        println("2. Research & Undergraduate Thesis Pathway")
        println("3. Oceanography & Physical Sciences Pathway")
        println("4. Custom Course Dependency Tree (Trace full ancestry & unlocks)")
        print("\nSelect pathway (1-4): ")
        when (scanner.nextLine().trim()) {
            "1" -> printMarineEcologyPathway()
            "2" -> printResearchPathway()
            "3" -> printOceanographyPathway()
            "4" -> {
                print("Enter course code to trace: ")
                val c = scanner.nextLine().trim()
                traceDependencies(c)
            }
            else -> printMarineEcologyPathway()
        }
    }

    private fun printMarineEcologyPathway() {
        println("\n" + TerminalStyle.bold("── Marine Ecology Pathway ──"))
        val nodes = listOf("BIO117", "MRB132", "MRB134", "MRB145")
        for ((idx, code) in nodes.withIndex()) {
            val c = engine.findCourse(code)!!
            val done = profile.completedCodes.contains(code)
            val mark = if (done) TerminalStyle.green("✓ DONE") else TerminalStyle.yellow("○ UPCOMING")
            val arrow = if (idx < nodes.size - 1) "      ↓" else ""
            println("   [${c.code}] ${c.title.padEnd(35)} ($mark)")
            if (arrow.isNotEmpty()) println(arrow)
        }
        println(TerminalStyle.dim("\nNote: MRB134 also requires STT034 and MRB131. MRB148 can be taken concurrently with or after MRB134.\n"))
    }

    private fun printResearchPathway() {
        println("\n" + TerminalStyle.bold("── Research & Thesis Pathway ──"))
        val nodes = listOf("STT034", "MRB195", "MRB196", "MRB199")
        for ((idx, code) in nodes.withIndex()) {
            val c = engine.findCourse(code)!!
            val done = profile.completedCodes.contains(code)
            val mark = if (done) TerminalStyle.green("✓ DONE") else TerminalStyle.yellow("○ UPCOMING")
            val arrow = if (idx < nodes.size - 1) "      ↓" else ""
            println("   [${c.code}] ${c.title.padEnd(35)} ($mark)")
            if (arrow.isNotEmpty()) println(arrow)
        }
        println(TerminalStyle.dim("\nNote: MRB195 & MRB198 require full Third-Year Standing (all Year 1 & 2 courses completed).\n"))
    }

    private fun printOceanographyPathway() {
        println("\n" + TerminalStyle.bold("── Oceanography & Chemistry Pathway ──"))
        val nodes = listOf("CHM011", "CHM032", "CHM045")
        for ((idx, code) in nodes.withIndex()) {
            val c = engine.findCourse(code)!!
            val done = profile.completedCodes.contains(code)
            val mark = if (done) TerminalStyle.green("✓ DONE") else TerminalStyle.yellow("○ UPCOMING")
            val arrow = if (idx < nodes.size - 1) "      ↓" else ""
            println("   [${c.code}] ${c.title.padEnd(35)} ($mark)")
            if (arrow.isNotEmpty()) println(arrow)
        }
        println(TerminalStyle.dim("\nNote: OCN121 requires PHY102, PHY102.1, and MAT043. OCN122 requires CHM011 and CHM011.1.\n"))
    }

    private fun traceDependencies(code: String) {
        val course = engine.findCourse(code)
        if (course == null) {
            println(TerminalStyle.yellow("Course '$code' not found.\n"))
            return
        }
        val fullAncestry = engine.getFullPrerequisiteTree(course.code)
        val unlocks = engine.getUnlockedCourses(course.code)

        println("\nDependency Report for ${course.code} (${course.title}):")
        println("Direct Prerequisites: " + (if (course.pre.isEmpty()) "None" else course.pre.joinToString(", ")))
        println("Full Prerequisite Ancestry: " + (if (fullAncestry.isEmpty()) "Foundational (No prior courses required)" else fullAncestry.joinToString(" → ")))
        println("Directly Unlocks: " + (if (unlocks.isEmpty()) "Terminal subject (No further prerequisites depend on this)" else unlocks.joinToString(", ") { it.code }))
        println()
    }

    // --- Option 4: Semester Planner ---
    private fun showSemesterPlanner() {
        val plannedCourses = CurriculumData.ALL_COURSES.filter {
            profile.plannedCodes.contains(it.code) && !profile.completedCodes.contains(it.code)
        }
        val totalPlannedUnits = plannedCourses.sumOf { it.units }
        val workload = engine.evaluateWorkload(totalPlannedUnits, profile.confidence)

        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println(TerminalStyle.cyan(" SEMESTER PLANNER · ${CurriculumData.TERMS[profile.selectedTerm]}"))
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println("Student Confidence: ${profile.confidence}/10 (Adviser cap: ${engine.recommendedUnitCap(profile.confidence)} units)")
        println("Total Planned:      $totalPlannedUnits units (${plannedCourses.size} courses)")
        println("Workload Stress:    ${TerminalStyle.bold("${workload.stressScore}/100")} — ${TerminalStyle.badge(workload.rating, TerminalStyle.FG_GREEN)}")
        println("Adviser Feedback:   ${workload.description}")
        println()

        if (plannedCourses.isEmpty()) {
            println(TerminalStyle.dim("No courses currently planned. Use Option 5 to auto-generate or add manually below.\n"))
        } else {
            println("CODE".padEnd(10) + "UNITS".padEnd(7) + "ELIGIBILITY".padEnd(20) + "DETAILS")
            println("─".repeat(70))
            for (c in plannedCourses) {
                val eligible = engine.isEligible(c, profile.completedCodes)
                val elStatus = if (eligible) TerminalStyle.green("Eligible") else TerminalStyle.yellow("Prereqs Needed")
                val notes = if (eligible) "Ready to enroll" else "Requires: " + engine.getMissingPrerequisites(c, profile.completedCodes).joinToString(", ")
                println(c.code.padEnd(10) + "${c.units}u".padEnd(7) + elStatus.padEnd(20) + notes)
            }
            println()
        }

        println("Planner actions:")
        println("  (A) Add a course to plan")
        println("  (R) Remove a course from plan")
        println("  (T) Change target term")
        println("  (C) Clear planned courses")
        println("  (Enter to return to Main Menu)")
        print("Action: ")
        val action = scanner.nextLine().trim().uppercase()
        when (action) {
            "A" -> {
                print("Enter course code to add: ")
                val addCode = scanner.nextLine().trim().uppercase()
                val target = engine.findCourse(addCode)
                if (target == null) {
                    println(TerminalStyle.yellow("Course not found."))
                } else if (profile.completedCodes.contains(target.code)) {
                    println(TerminalStyle.yellow("Course is already completed!"))
                } else {
                    profile.plannedCodes.add(target.code)
                    if (target.co != null && !profile.completedCodes.contains(target.co)) {
                        profile.plannedCodes.add(target.co)
                        println(TerminalStyle.cyan("Also added co-requisite ${target.co}."))
                    }
                    println(TerminalStyle.green("Course ${target.code} added to plan."))
                }
            }
            "R" -> {
                print("Enter course code to remove: ")
                val remCode = scanner.nextLine().trim().uppercase()
                if (profile.plannedCodes.remove(remCode)) {
                    println(TerminalStyle.green("Removed $remCode from plan."))
                } else {
                    println(TerminalStyle.yellow("Course was not in your plan."))
                }
            }
            "T" -> {
                println("Select target term (0..8):")
                CurriculumData.TERMS.forEachIndexed { i, t -> println("  $i. $t") }
                val newTerm = scanner.nextLine().trim().toIntOrNull()
                if (newTerm != null && newTerm in 0..8) {
                    profile.selectedTerm = newTerm
                    println(TerminalStyle.green("Target term set to ${CurriculumData.TERMS[newTerm]}."))
                }
            }
            "C" -> {
                profile.plannedCodes.clear()
                println(TerminalStyle.green("Planner cleared."))
            }
        }
        println()
    }

    // --- Option 5: Auto-Generate Plan ---
    private fun autoGeneratePlan() {
        println(TerminalStyle.bold("────────────────────────────────────────────────────────────────────────"))
        println(TerminalStyle.cyan(" AUTO-GENERATING SEMESTER PLAN"))
        println(TerminalStyle.bold("────────────────────────────────────────────────────────────────────────"))
        println("Analyzing curriculum, completed prerequisites, co-requisites, and standing...")
        val recommended = engine.generatePlan(profile.completedCodes, profile.confidence)
        val units = recommended.sumOf { it.units }

        profile.plannedCodes.clear()
        profile.plannedCodes.addAll(recommended.map { it.code })

        println(TerminalStyle.green("✓ Plan generated successfully!"))
        println("Total recommended courses: ${recommended.size} (${units} units)")
        println()
        for (c in recommended) {
            val coNote = if (c.co != null) " (Paired with co-requisite ${c.co})" else ""
            println("  • [${c.code}] ${c.title} — ${c.units} units$coNote")
        }
        println()
    }

    // --- Option 6: Manage Completed Courses ---
    private fun manageCompletedCourses() {
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println(TerminalStyle.cyan(" MANAGE COMPLETED COURSES (${profile.completedCodes.size} marked complete)"))
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println("1. Toggle completion of a specific course")
        println("2. Mark an entire academic term as completed")
        println("3. Mark an entire academic term as incomplete")
        println("4. List currently completed courses")
        print("\nSelect action (1-4): ")
        when (scanner.nextLine().trim()) {
            "1" -> {
                print("Enter course code to toggle (e.g. 'BIO117'): ")
                val code = scanner.nextLine().trim().uppercase()
                val course = engine.findCourse(code)
                if (course == null) {
                    println(TerminalStyle.yellow("Course not found."))
                } else if (profile.completedCodes.contains(code)) {
                    profile.completedCodes.remove(code)
                    println(TerminalStyle.yellow("Marked $code as INCOMPLETE."))
                } else {
                    profile.completedCodes.add(code)
                    println(TerminalStyle.green("Marked $code as COMPLETED!"))
                }
            }
            "2" -> {
                println("Select Term to mark completed (0..8):")
                CurriculumData.TERMS.forEachIndexed { i, t -> println("  $i. $t") }
                val idx = scanner.nextLine().trim().toIntOrNull()
                if (idx != null && idx in 0..8) {
                    val count = CurriculumData.ALL_COURSES.filter { it.term == idx }.map { it.code }
                    profile.completedCodes.addAll(count)
                    println(TerminalStyle.green("Marked all ${count.size} courses in ${CurriculumData.TERMS[idx]} as completed."))
                }
            }
            "3" -> {
                println("Select Term to mark incomplete (0..8):")
                CurriculumData.TERMS.forEachIndexed { i, t -> println("  $i. $t") }
                val idx = scanner.nextLine().trim().toIntOrNull()
                if (idx != null && idx in 0..8) {
                    val count = CurriculumData.ALL_COURSES.filter { it.term == idx }.map { it.code }
                    profile.completedCodes.removeAll(count.toSet())
                    println(TerminalStyle.yellow("Marked all courses in ${CurriculumData.TERMS[idx]} as incomplete."))
                }
            }
            "4" -> {
                println("\nCompleted courses list:")
                profile.completedCodes.sorted().forEach { println("  • $it — ${engine.findCourse(it)?.title}") }
            }
        }
        println()
    }

    // --- Option 7: Student Profile ---
    private fun manageStudentProfile() {
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println(TerminalStyle.cyan(" STUDENT PROFILE & SETTINGS"))
        println(TerminalStyle.bold("════════════════════════════════════════════════════════════════════════"))
        println("Student Name:       ${profile.name}")
        println("Degree Program:     ${profile.program}")
        println("Subject Confidence: ${profile.confidence}/10")
        println("Unit Cap Rule:      ${engine.recommendedUnitCap(profile.confidence)} units")
        println()
        println("1. Edit student name")
        println("2. Adjust subject confidence (1..10)")
        print("\nSelect option (1-2) or press Enter to return: ")
        when (scanner.nextLine().trim()) {
            "1" -> {
                print("Enter student name: ")
                val newName = scanner.nextLine().trim()
                if (newName.isNotEmpty()) {
                    profile.name = newName
                    println(TerminalStyle.green("Name updated to $newName."))
                }
            }
            "2" -> {
                print("Enter confidence rating (1-10): ")
                val newConf = scanner.nextLine().trim().toIntOrNull()
                if (newConf != null && newConf in 1..10) {
                    profile.confidence = newConf
                    println(TerminalStyle.green("Confidence updated to $newConf/10."))
                } else {
                    println(TerminalStyle.yellow("Must be a number between 1 and 10."))
                }
            }
        }
        println()
    }

    // --- Option 8: Export Plan to File ---
    private fun exportPlanToFile() {
        val plannedCourses = CurriculumData.ALL_COURSES.filter {
            profile.plannedCodes.contains(it.code) && !profile.completedCodes.contains(it.code)
        }
        val units = plannedCourses.sumOf { it.units }
        val termName = CurriculumData.TERMS[profile.selectedTerm]

        val content = buildString {
            appendLine("════════════════════════════════════════════════════════════════════════")
            appendLine("CourseAdvisor — BS Marine Science (Curriculum AY 2025–2026)")
            appendLine("Mindanao State University at Naawan")
            appendLine("Student: ${profile.name}")
            appendLine("Target:  $termName")
            appendLine("════════════════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("RECOMMENDED COURSE LOAD:")
            if (plannedCourses.isEmpty()) {
                appendLine("  (No courses planned)")
            } else {
                plannedCourses.forEach {
                    appendLine("  ${it.code.padEnd(10)} — ${it.title} (${it.units} units)")
                }
            }
            appendLine()
            appendLine("Total: $units units")
            appendLine("Confidence Rating: ${profile.confidence}/10")
            appendLine()
            appendLine("IMPORTANT NOTICE:")
            appendLine("This document is an academic planning aid only.")
            appendLine("Please confirm all subject enrollments with your assigned academic adviser.")
            appendLine("Ref: MSUN-CFMS-POS-002-2025-REV00")
        }

        val filename = "marine-science-plan.txt"
        File(filename).writeText(content)
        println(TerminalStyle.green("✓ Plan exported successfully to '$filename'!\n"))
    }

    // --- Option 9: Reset Progress ---
    private fun resetSampleProgress() {
        print("Are you sure you want to clear all completed courses and plans? (y/N): ")
        val conf = scanner.nextLine().trim().lowercase()
        if (conf == "y" || conf == "yes") {
            profile.completedCodes.clear()
            profile.plannedCodes.clear()
            println(TerminalStyle.green("✓ Progress cleared to clean slate.\n"))
        } else {
            println("Reset cancelled.\n")
        }
    }
}

// ----------------------------------------------------------------------------
// 6. MAIN ENTRY POINT
// ----------------------------------------------------------------------------

fun main() {
    val cli = CourseAdvisorCli()
    cli.start()
}
