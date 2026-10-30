package com.courseadvisor.model

enum class CourseCategory(val displayName: String) {
    MARINE_SCIENCE("Major / Specialization"),
    GENERAL_EDUCATION("General Education"),
    FOUNDATION("Foundation");

    companion object {
        fun fromCode(code: String): CourseCategory = when {
            code.startsWith("GEC") || code.startsWith("GAS") ||
            code.startsWith("FPE") || code.startsWith("HAL") ||
            code.startsWith("FIL") || code.startsWith("PED") ||
            code.startsWith("NST") -> GENERAL_EDUCATION
            code.startsWith("MRB") || code.startsWith("OCN") ||
            code.startsWith("MKT") || code.startsWith("ED") ||
            code.startsWith("ES") || code.startsWith("ANS") ||
            code.startsWith("AGR") || code.startsWith("FSH") ||
            code.startsWith("ACT") || code.startsWith("MGT") ||
            code.startsWith("BIT") || code.startsWith("ECN") ||
            code.startsWith("IBA") -> MARINE_SCIENCE
            else -> FOUNDATION
        }
    }
}
