package com.courseadvisor.cli

import com.courseadvisor.data.CurriculumData
import com.courseadvisor.model.Course
import com.courseadvisor.model.CourseCategory
import com.courseadvisor.model.StudentProfile
import com.courseadvisor.service.CourseAdvisorEngine
import java.io.File
import java.util.Scanner

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
                    println("Thank you for using CourseAdvisor. Chart your own course! *\n")
                }
                else -> println("Invalid choice. Please select an option between 0 and 9.\n")
            }
        }
    }

    private fun printBanner() {
        println("""
+------------------------------------------------------------------------+
|    * CourseAdvisor - Bachelor of Science in Marine Science             |
|    Curriculum AY 2025-2026 - MSU at Naawan (MSUN-CFMS-POS-002)         |
+------------------------------------------------------------------------+
        """.trimIndent())
    }

    private fun printMainMenu() {
        println("------------------------------------------------------------------------")
        println(" MAIN MENU ")
        println("  1. Academic Overview & Degree Progress")
        println("  2. Course Catalog (Browse, Search & Filter)")
        println("  3. Prerequisite Pathways & Dependency Tree")
        println("  4. Semester Planner & Workload Assessment")
        println("  5. Auto-Generate Recommended Plan (AI Rules)")
        println("  6. Manage Completed Courses")
        println("  7. Student Profile & Confidence Tuning")
        println("  8. Export Semester Plan to Text File")
        println("  9. Reset Progress to Clean Slate")
        println("  0. Exit")
        println("------------------------------------------------------------------------")
    }

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

        println("========================================================================")
        println(" ACADEMIC OVERVIEW - ${profile.name}")
        println("========================================================================")
        println("Program:          ${profile.program}")
        println("Standing:         ${determineStanding()}")
        println("Subject Conf.:    ${profile.confidence}/10 (Recommended capacity: ${engine.recommendedUnitCap(profile.confidence)} units)")
        println()
        println("Degree Progress:  ${TerminalStyle.progressBar(percent)} ${String.format("%.1f", percent)}%")
        println("Curriculum Units: $earnedUnits of $totalUnits units earned")
        println("Courses Done:     ${completedCourses.size} of ${CurriculumData.ALL_COURSES.size} courses completed")
        println()
        println("Current Planned Term: ${CurriculumData.TERMS[profile.selectedTerm]}")
        println("Planned Courses:      ${plannedCourses.size} courses (${plannedUnits} units)")
        println("Workload Stress:      ${workload.stressScore}/100 - [${workload.rating}]")
        println("Note:                 ${workload.description}")
        println()

        println("Progress by Academic Term:")
        for (i in CurriculumData.TERMS.indices) {
            val termCourses = CurriculumData.ALL_COURSES.filter { it.term == i }
            val doneInTerm = termCourses.filter { profile.completedCodes.contains(it.code) }.size
            val termPercent = (doneInTerm.toDouble() / termCourses.size) * 100.0
            val status = if (doneInTerm == termCourses.size) "[Complete]" else "$doneInTerm/${termCourses.size} done"
            println("  ${CurriculumData.TERMS[i].padEnd(28)} [${TerminalStyle.progressBar(termPercent, 12)}]  $status")
        }
        println()
    }

    private fun determineStanding(): String {
        val completedCount = profile.completedCodes.size
        return when {
            completedCount >= 50 -> "4th Year - Senior Standing"
            completedCount >= 34 -> "3rd Year - Junior Standing"
            completedCount >= 18 -> "2nd Year - Sophomore Standing"
            else -> "1st Year - Freshman Standing"
        }
    }

    private fun showCourseCatalog() {
        println("========================================================================")
        println(" COURSE CATALOG")
        println("========================================================================")
        println("Filters:")
        println("  1. Show all courses")
        println("  2. Filter by Category (Marine Science / General Education / Foundation)")
        println("  3. Filter by Term")
        println("  4. Search by Code or Title")
        print("\nSelect filter (1-4) or press Enter to show all: ")
        val choice = readInput()

        val filtered = when (choice) {
            "2" -> {
                println("Choose Category: (1) Marine Science (2) General Education (3) Foundation")
                when (readInput()) {
                    "1" -> CurriculumData.ALL_COURSES.filter { it.category == CourseCategory.MARINE_SCIENCE }
                    "2" -> CurriculumData.ALL_COURSES.filter { it.category == CourseCategory.GENERAL_EDUCATION }
                    "3" -> CurriculumData.ALL_COURSES.filter { it.category == CourseCategory.FOUNDATION }
                    else -> CurriculumData.ALL_COURSES
                }
            }
            "3" -> {
                println("Select Term index (0..8):")
                CurriculumData.TERMS.forEachIndexed { i, t -> println("  $i. $t") }
                val termIdx = readInput().toIntOrNull() ?: 0
                CurriculumData.ALL_COURSES.filter { it.term == termIdx }
            }
            "4" -> {
                print("Enter search keyword (e.g. 'MRB', 'Chemistry', 'Ecology'): ")
                val q = readInput().lowercase()
                CurriculumData.ALL_COURSES.filter {
                    it.code.lowercase().contains(q) || it.title.lowercase().contains(q)
                }
            }
            else -> CurriculumData.ALL_COURSES
        }

        println("\nFound ${filtered.size} courses:\n")
        printCourseTable(filtered)

        print("\nEnter course code for deep details (or press Enter to return): ")
        val detailCode = readInput()
        if (detailCode.isNotEmpty()) {
            showCourseDetail(detailCode)
        }
    }

    private fun printCourseTable(courses: List<Course>) {
        println("CODE".padEnd(10) + "UNITS".padEnd(7) + "TERM".padEnd(28) + "STATUS".padEnd(16) + "TITLE")
        println("-".repeat(80))
        for (c in courses) {
            val isDone = profile.completedCodes.contains(c.code)
            val isEligible = engine.isEligible(c, profile.completedCodes)
            val status = when {
                isDone -> "[Completed]"
                isEligible -> "[Ready]"
                else -> "[Locked]"
            }
            val termName = CurriculumData.TERMS[c.term]
            println(c.code.padEnd(10) + "${c.units}u".padEnd(7) + termName.padEnd(28) + status.padEnd(16) + c.title)
        }
    }

    private fun showCourseDetail(code: String) {
        val course = engine.findCourse(code)
        if (course == null) {
            println("Course '$code' not found.\n")
            return
        }

        val isDone = profile.completedCodes.contains(course.code)
        val isEligible = engine.isEligible(course, profile.completedCodes)
        val missing = engine.getMissingPrerequisites(course, profile.completedCodes)
        val unlocked = engine.getUnlockedCourses(course.code)

        println("\n------------------------------------------------------------------------")
        println(" COURSE DETAILS: ${course.code} - ${course.title}")
        println("------------------------------------------------------------------------")
        println("Units:         ${course.units}")
        println("Curriculum:    ${CurriculumData.TERMS[course.term]} - ${course.category.displayName}")
        println("Status:        " + if (isDone) "Completed" else if (isEligible) "Eligible for Enrollment" else "Prerequisites Incomplete")
        println("Prerequisites: " + if (course.pre.isEmpty()) "None" else course.pre.joinToString(", "))
        if (course.co != null) println("Co-requisite:  ${course.co}")
        if (missing.isNotEmpty() && !isDone) {
            println("Missing Requirements:")
            missing.forEach { println("   * $it") }
        }
        if (unlocked.isNotEmpty()) {
            println("Directly unlocks (${unlocked.size} courses):")
            unlocked.forEach { println("   -> ${it.code} (${it.title})") }
        }
        println("------------------------------------------------------------------------\n")
    }

    private fun showPrerequisiteVisualizer() {
        println("========================================================================")
        println(" PREREQUISITE PATHWAYS & DEPENDENCY GRAPH")
        println("========================================================================")
        println("1. Marine Ecology Pathway")
        println("2. Research & Undergraduate Thesis Pathway")
        println("3. Oceanography & Physical Sciences Pathway")
        println("4. Custom Course Dependency Tree (Trace full ancestry & unlocks)")
        print("\nSelect pathway (1-4): ")
        when (readInput()) {
            "1" -> printMarineEcologyPathway()
            "2" -> printResearchPathway()
            "3" -> printOceanographyPathway()
            "4" -> {
                print("Enter course code to trace: ")
                val c = readInput()
                traceDependencies(c)
            }
            else -> printMarineEcologyPathway()
        }
    }

    private fun printMarineEcologyPathway() {
        println("\n-- Marine Ecology Pathway --")
        val nodes = listOf("BIO117", "MRB132", "MRB134", "MRB145")
        for ((idx, code) in nodes.withIndex()) {
            val c = engine.findCourse(code)!!
            val done = profile.completedCodes.contains(code)
            val mark = if (done) "[DONE]" else "[UPCOMING]"
            val arrow = if (idx < nodes.size - 1) "      |" else ""
            println("   [${c.code}] ${c.title.padEnd(35)} ($mark)")
            if (arrow.isNotEmpty()) {
                println(arrow)
                println("      v")
            }
        }
        println("\nNote: MRB134 also requires STT034 and MRB131. MRB148 can be taken concurrently with or after MRB134.\n")
    }

    private fun printResearchPathway() {
        println("\n-- Research & Thesis Pathway --")
        val nodes = listOf("STT034", "MRB195", "MRB196", "MRB199")
        for ((idx, code) in nodes.withIndex()) {
            val c = engine.findCourse(code)!!
            val done = profile.completedCodes.contains(code)
            val mark = if (done) "[DONE]" else "[UPCOMING]"
            val arrow = if (idx < nodes.size - 1) "      |" else ""
            println("   [${c.code}] ${c.title.padEnd(35)} ($mark)")
            if (arrow.isNotEmpty()) {
                println(arrow)
                println("      v")
            }
        }
        println("\nNote: MRB195 & MRB198 require full Third-Year Standing (all Year 1 & 2 courses completed).\n")
    }

    private fun printOceanographyPathway() {
        println("\n-- Oceanography & Chemistry Pathway --")
        val nodes = listOf("CHM011", "CHM032", "CHM045")
        for ((idx, code) in nodes.withIndex()) {
            val c = engine.findCourse(code)!!
            val done = profile.completedCodes.contains(code)
            val mark = if (done) "[DONE]" else "[UPCOMING]"
            val arrow = if (idx < nodes.size - 1) "      |" else ""
            println("   [${c.code}] ${c.title.padEnd(35)} ($mark)")
            if (arrow.isNotEmpty()) {
                println(arrow)
                println("      v")
            }
        }
        println("\nNote: OCN121 requires PHY102, PHY102.1, and MAT043. OCN122 requires CHM011 and CHM011.1.\n")
    }

    private fun traceDependencies(code: String) {
        val course = engine.findCourse(code)
        if (course == null) {
            println("Course '$code' not found.\n")
            return
        }
        val fullAncestry = engine.getFullPrerequisiteTree(course.code)
        val unlocks = engine.getUnlockedCourses(course.code)

        println("\nDependency Report for ${course.code} (${course.title}):")
        println("Direct Prerequisites: " + (if (course.pre.isEmpty()) "None" else course.pre.joinToString(", ")))
        println("Full Prerequisite Ancestry: " + (if (fullAncestry.isEmpty()) "Foundational (No prior courses required)" else fullAncestry.joinToString(" -> ")))
        println("Directly Unlocks: " + (if (unlocks.isEmpty()) "Terminal subject (No further prerequisites depend on this)" else unlocks.joinToString(", ") { it.code }))
        println()
    }

    private fun showSemesterPlanner() {
        val plannedCourses = CurriculumData.ALL_COURSES.filter {
            profile.plannedCodes.contains(it.code) && !profile.completedCodes.contains(it.code)
        }
        val totalPlannedUnits = plannedCourses.sumOf { it.units }
        val workload = engine.evaluateWorkload(totalPlannedUnits, profile.confidence)

        println("========================================================================")
        println(" SEMESTER PLANNER - ${CurriculumData.TERMS[profile.selectedTerm]}")
        println("========================================================================")
        println("Student Confidence: ${profile.confidence}/10 (Adviser cap: ${engine.recommendedUnitCap(profile.confidence)} units)")
        println("Total Planned:      $totalPlannedUnits units (${plannedCourses.size} courses)")
        println("Workload Stress:    ${workload.stressScore}/100 - [${workload.rating}]")
        println("Adviser Feedback:   ${workload.description}")
        println()

        if (plannedCourses.isEmpty()) {
            println("No courses currently planned. Use Option 5 to auto-generate or add manually below.\n")
        } else {
            println("CODE".padEnd(10) + "UNITS".padEnd(7) + "ELIGIBILITY".padEnd(20) + "DETAILS")
            println("-".repeat(70))
            for (c in plannedCourses) {
                val eligible = engine.isEligible(c, profile.completedCodes)
                val elStatus = if (eligible) "[Eligible]" else "[Prereqs Needed]"
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
        val action = readInput().uppercase()
        when (action) {
            "A" -> {
                print("Enter course code to add: ")
                val addCode = readInput().uppercase()
                val target = engine.findCourse(addCode)
                if (target == null) {
                    println("Course not found.")
                } else if (profile.completedCodes.contains(target.code)) {
                    println("Course is already completed!")
                } else {
                    profile.plannedCodes.add(target.code)
                    if (target.co != null && !profile.completedCodes.contains(target.co)) {
                        profile.plannedCodes.add(target.co)
                        println("Also added co-requisite ${target.co}.")
                    }
                    println("Course ${target.code} added to plan.")
                }
            }
            "R" -> {
                print("Enter course code to remove: ")
                val remCode = readInput().uppercase()
                if (profile.plannedCodes.remove(remCode)) {
                    println("Removed $remCode from plan.")
                } else {
                    println("Course was not in your plan.")
                }
            }
            "T" -> {
                println("Select target term (0..8):")
                CurriculumData.TERMS.forEachIndexed { i, t -> println("  $i. $t") }
                val newTerm = readInput().toIntOrNull()
                if (newTerm != null && newTerm in 0..8) {
                    profile.selectedTerm = newTerm
                    println("Target term set to ${CurriculumData.TERMS[newTerm]}.")
                }
            }
            "C" -> {
                profile.plannedCodes.clear()
                println("Planner cleared.")
            }
        }
        println()
    }

    private fun autoGeneratePlan() {
        println("------------------------------------------------------------------------")
        println(" AUTO-GENERATING SEMESTER PLAN")
        println("------------------------------------------------------------------------")
        println("Analyzing curriculum, completed prerequisites, co-requisites, and standing...")
        val recommended = engine.generatePlan(profile.completedCodes, profile.confidence)
        val units = recommended.sumOf { it.units }

        profile.plannedCodes.clear()
        profile.plannedCodes.addAll(recommended.map { it.code })

        println("[OK] Plan generated successfully!")
        println("Total recommended courses: ${recommended.size} (${units} units)")
        println()
        for (c in recommended) {
            val coNote = if (c.co != null) " (Paired with co-requisite ${c.co})" else ""
            println("  * [${c.code}] ${c.title} - ${c.units} units$coNote")
        }
        println()
    }

    private fun manageCompletedCourses() {
        println("========================================================================")
        println(" MANAGE COMPLETED COURSES (${profile.completedCodes.size} marked complete)")
        println("========================================================================")
        println("1. Toggle completion of a specific course")
        println("2. Mark an entire academic term as completed")
        println("3. Mark an entire academic term as incomplete")
        println("4. List currently completed courses")
        print("\nSelect action (1-4): ")
        when (readInput()) {
            "1" -> {
                print("Enter course code to toggle (e.g. 'BIO117'): ")
                val code = readInput().uppercase()
                val course = engine.findCourse(code)
                if (course == null) {
                    println("Course not found.")
                } else if (profile.completedCodes.contains(code)) {
                    profile.completedCodes.remove(code)
                    println("Marked $code as INCOMPLETE.")
                } else {
                    profile.completedCodes.add(code)
                    println("Marked $code as COMPLETED!")
                }
            }
            "2" -> {
                println("Select Term to mark completed (0..8):")
                CurriculumData.TERMS.forEachIndexed { i, t -> println("  $i. $t") }
                val idx = readInput().toIntOrNull()
                if (idx != null && idx in 0..8) {
                    val count = CurriculumData.ALL_COURSES.filter { it.term == idx }.map { it.code }
                    profile.completedCodes.addAll(count)
                    println("Marked all ${count.size} courses in ${CurriculumData.TERMS[idx]} as completed.")
                }
            }
            "3" -> {
                println("Select Term to mark incomplete (0..8):")
                CurriculumData.TERMS.forEachIndexed { i, t -> println("  $i. $t") }
                val idx = readInput().toIntOrNull()
                if (idx != null && idx in 0..8) {
                    val count = CurriculumData.ALL_COURSES.filter { it.term == idx }.map { it.code }
                    profile.completedCodes.removeAll(count.toSet())
                    println("Marked all courses in ${CurriculumData.TERMS[idx]} as incomplete.")
                }
            }
            "4" -> {
                println("\nCompleted courses list:")
                profile.completedCodes.sorted().forEach { println("  * $it - ${engine.findCourse(it)?.title}") }
            }
        }
        println()
    }

    private fun manageStudentProfile() {
        println("========================================================================")
        println(" STUDENT PROFILE & SETTINGS")
        println("========================================================================")
        println("Student Name:       ${profile.name}")
        println("Degree Program:     ${profile.program}")
        println("Subject Confidence: ${profile.confidence}/10")
        println("Unit Cap Rule:      ${engine.recommendedUnitCap(profile.confidence)} units")
        println()
        println("1. Edit student name")
        println("2. Adjust subject confidence (1..10)")
        print("\nSelect option (1-2) or press Enter to return: ")
        when (readInput()) {
            "1" -> {
                print("Enter student name: ")
                val newName = readInput()
                if (newName.isNotEmpty()) {
                    profile.name = newName
                    println("Name updated to $newName.")
                }
            }
            "2" -> {
                print("Enter confidence rating (1-10): ")
                val newConf = readInput().toIntOrNull()
                if (newConf != null && newConf in 1..10) {
                    profile.confidence = newConf
                    println("Confidence updated to $newConf/10.")
                } else {
                    println("Must be a number between 1 and 10.")
                }
            }
        }
        println()
    }

    private fun exportPlanToFile() {
        val plannedCourses = CurriculumData.ALL_COURSES.filter {
            profile.plannedCodes.contains(it.code) && !profile.completedCodes.contains(it.code)
        }
        val units = plannedCourses.sumOf { it.units }
        val termName = CurriculumData.TERMS[profile.selectedTerm]

        val content = buildString {
            appendLine("========================================================================")
            appendLine("CourseAdvisor - BS Marine Science (Curriculum AY 2025-2026)")
            appendLine("Mindanao State University at Naawan")
            appendLine("Student: ${profile.name}")
            appendLine("Target:  $termName")
            appendLine("========================================================================")
            appendLine()
            appendLine("RECOMMENDED COURSE LOAD:")
            if (plannedCourses.isEmpty()) {
                appendLine("  (No courses planned)")
            } else {
                plannedCourses.forEach {
                    appendLine("  ${it.code.padEnd(10)} - ${it.title} (${it.units} units)")
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
        println("[OK] Plan exported successfully to '$filename'!\n")
    }

    private fun resetSampleProgress() {
        print("Are you sure you want to clear all completed courses and plans? (y/N): ")
        val conf = readInput().lowercase()
        if (conf == "y" || conf == "yes") {
            profile.completedCodes.clear()
            profile.plannedCodes.clear()
            println("[OK] Progress cleared to clean slate.\n")
        } else {
            println("Reset cancelled.\n")
        }
    }
}
