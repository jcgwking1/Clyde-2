package com.courseadvisor.service

import com.courseadvisor.data.CurriculumData
import com.courseadvisor.model.Course
import com.courseadvisor.model.WorkloadEvaluation
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CourseAdvisorEngine(val courses: List<Course> = CurriculumData.ALL_COURSES) {
    private val courseMap: Map<String, Course> = courses.associateBy { it.code }

    fun findCourse(code: String): Course? = courseMap[code.uppercase().trim()]

    /**
     * Checks if a course is eligible given completed courses.
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

    fun getUnlockedCourses(courseCode: String): List<Course> {
        return courses.filter { it.pre.contains(courseCode) }
    }

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

    fun evaluateWorkload(units: Int, confidence: Int): WorkloadEvaluation {
        val score = min(95, max(15, (units * 2.1 + (10 - confidence) * 3).roundToInt()))
        val (rating, description) = when {
            score < 40 -> "Light workload" to "Gentle pace with plenty of study and rest breathing room."
            score < 65 -> "Manageable workload" to "Balanced challenge well-aligned with your subject confidence."
            score < 80 -> "Moderate workload" to "Demanding semester requiring solid time management."
            else -> "Intensive workload" to "Heavy course load; consider consulting your academic adviser."
        }
        return WorkloadEvaluation(units, confidence, score, rating, description)
    }

    fun recommendedUnitCap(confidence: Int): Int = when {
        confidence >= 7 -> 18
        confidence >= 4 -> 15
        else -> 12
    }

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
