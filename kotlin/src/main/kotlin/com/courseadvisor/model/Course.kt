package com.courseadvisor.model

data class Course(
    val code: String,
    val title: String,
    val units: Int,
    val term: Int,
    val pre: List<String> = emptyList(),
    val co: String? = null,
    val category: CourseCategory = CourseCategory.fromCode(code)
)

data class WorkloadEvaluation(
    val units: Int,
    val confidence: Int,
    val stressScore: Int,
    val rating: String,
    val description: String
)
