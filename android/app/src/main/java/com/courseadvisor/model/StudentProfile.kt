package com.courseadvisor.model

data class StudentProfile(
    var name: String = "Alex Santos",
    val program: String = "BS in Marine Science · MSU at Naawan",
    var academicYear: String = "AY 2025–2026",
    var confidence: Int = 7,
    val completedCodes: MutableSet<String> = mutableSetOf(),
    val plannedCodes: MutableSet<String> = mutableSetOf(),
    var selectedTerm: Int = 4
)
