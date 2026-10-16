package com.courseadvisor.model

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
