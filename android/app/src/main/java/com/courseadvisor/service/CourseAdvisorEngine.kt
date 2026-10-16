package com.courseadvisor.service

import com.courseadvisor.data.CurriculumData
import com.courseadvisor.model.Course
import com.courseadvisor.model.WorkloadEvaluation
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Mamdani Fuzzy Inference System for Academic Stress Calculation
 * Reference: Section 3.2, CourseAdvisor-DevBP.docx
 */
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

        // Fuzzification
        val confLow = trapmf(c, 1.0, 1.0, 3.5, 5.0)
        val confMed = trimf(c, 3.5, 6.0, 8.0)
        val confHigh = trapmf(c, 6.5, 8.5, 10.0, 10.0)

        val loadLight = trapmf(u, 3.0, 3.0, 11.0, 14.0)
        val loadStd = trimf(u, 13.0, 17.0, 20.0)
        val loadHeavy = trapmf(u, 18.0, 21.0, 26.0, 26.0)

        val diffEasy = trapmf(d, 1.0, 1.0, 3.5, 5.0)
        val diffMod = trimf(d, 4.0, 6.2, 8.0)
        val diffHard = trapmf(d, 6.5, 8.2, 10.0, 10.0)

        // Rule Evaluation (Min operator)
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

        // Defuzzification: Centroid Method
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

data class BackwardChainingGoal(
    val goalCode: String,
    val goalTitle: String,
    val unfulfilledChain: List<String>,
    val depthTiers: Map<Int, List<Course>>,
    val terminalSatisfied: List<String>
)

data class CourseRationale(
    val code: String,
    val urgency: String,
    val headline: String,
    val detail: String
)

class CourseAdvisorEngine(val courses: List<Course> = CurriculumData.ALL_COURSES) {
    private val courseMap: Map<String, Course> = courses.associateBy { it.code }
    private val adjacency: Map<String, List<String>>
    private val depthTiers: Map<String, Int>

    init {
        val adj = mutableMapOf<String, MutableList<String>>()
        courses.forEach { adj[it.code] = mutableListOf() }
        for (c in courses) {
            for (pre in c.pre) {
                adj.getOrPut(pre) { mutableListOf() }.add(c.code)
            }
        }
        adjacency = adj

        // Kahn's Algorithm for Longest-Path Levelization
        val inDegree = courses.associate { it.code to it.pre.size }.toMutableMap()
        val tiers = courses.associate { it.code to 0 }.toMutableMap()
        val queue = ArrayDeque<String>()

        inDegree.filter { it.value == 0 }.keys.forEach { queue.add(it) }

        while (queue.isNotEmpty()) {
            val u = queue.removeFirst()
            val curTier = tiers[u] ?: 0

            for (v in adjacency[u].orEmpty()) {
                val nextDeg = (inDegree[v] ?: 1) - 1
                inDegree[v] = nextDeg
                val exTier = tiers[v] ?: 0
                tiers[v] = max(exTier, curTier + 1)
                if (nextDeg == 0) {
                    queue.add(v)
                }
            }
        }
        depthTiers = tiers
    }

    fun findCourse(code: String): Course? = courseMap[code.uppercase().trim()]

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

    fun getUnlockedCourses(courseCode: String): List<Course> {
        return courses.filter { it.pre.contains(courseCode) }
    }

    /**
     * Backward Chaining Prerequisite Solver from Milestone Goal
     */
    fun backwardChainFromGoal(goalCode: String, completedCodes: Set<String>): BackwardChainingGoal {
        val goalCourse = findCourse(goalCode)
        val goalTitle = goalCourse?.title ?: goalCode

        val unfulfilled = mutableSetOf<String>()
        val terminal = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun traverse(code: String) {
            if (!visited.add(code)) return
            val c = findCourse(code) ?: return
            for (p in c.pre) {
                if (completedCodes.contains(p)) {
                    terminal.add(p)
                } else {
                    unfulfilled.add(p)
                    traverse(p)
                }
            }
        }

        traverse(goalCode)

        val tiersMap = unfulfilled
            .mapNotNull { findCourse(it) }
            .groupBy { depthTiers[it.code] ?: 0 }

        return BackwardChainingGoal(
            goalCode = goalCode,
            goalTitle = goalTitle,
            unfulfilledChain = unfulfilled.toList(),
            depthTiers = tiersMap,
            terminalSatisfied = terminal.toList()
        )
    }

    fun generateRationale(course: Course, completedCodes: Set<String>): CourseRationale {
        val unlocks = adjacency[course.code].orEmpty()
        val capstone = unlocks.firstOrNull { it in listOf("MRB199", "MRB197", "MRB148", "MRB196") }

        return when {
            capstone != null -> {
                val capTitle = findCourse(capstone)?.title ?: capstone
                CourseRationale(course.code, "high", "Direct Gateway for $capstone", "Prerequisite milestone blocker for $capTitle.")
            }
            unlocks.size >= 2 -> {
                CourseRationale(course.code, "high", "Unlocks ${unlocks.size} Downstream Courses", "Foundational subject for: ${unlocks.take(3).joinToString(", ")}.")
            }
            course.co != null -> {
                CourseRationale(course.code, "medium", "Co-requisite Companion", "Best scheduled alongside lecture/lab partner ${course.co}.")
            }
            else -> {
                val tier = (depthTiers[course.code] ?: 0) + 1
                CourseRationale(course.code, "normal", "Tier $tier Progression Step", "Regular milestone progress in ${course.category.displayName}.")
            }
        }
    }

    fun evaluateWorkload(units: Int, confidence: Int, avgDifficulty: Double = 6.2): WorkloadEvaluation {
        return MamdaniFuzzyEngine.evaluate(units, confidence, avgDifficulty)
    }

    fun recommendedUnitCap(confidence: Int): Int = MamdaniFuzzyEngine.recommendedUnitCap(confidence)

    fun generatePlan(completedCodes: Set<String>, confidence: Int): List<Course> {
        val cap = recommendedUnitCap(confidence)
        val planned = mutableListOf<String>()
        var currentUnits = 0

        val candidates = courses.filter { !completedCodes.contains(it.code) && isEligible(it, completedCodes) }

        for (course in candidates) {
            if (planned.contains(course.code)) continue

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
