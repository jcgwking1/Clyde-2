package com.courseadvisor.data

import com.courseadvisor.model.Course
import com.courseadvisor.model.CourseCategory

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
