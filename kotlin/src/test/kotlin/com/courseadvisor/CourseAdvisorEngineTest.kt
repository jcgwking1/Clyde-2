package com.courseadvisor

import com.courseadvisor.data.CurriculumData
import com.courseadvisor.service.CourseAdvisorEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourseAdvisorEngineTest {
    private val engine = CourseAdvisorEngine()

    @Test
    fun testTotalCoursesAndUnits() {
        assertEquals(59, CurriculumData.ALL_COURSES.size, "Curriculum should have exactly 59 courses")
        assertTrue(CurriculumData.TOTAL_UNITS > 140, "Curriculum should have standard BS degree units")
    }

    @Test
    fun testPrerequisitesEligibility() {
        val bio118 = engine.findCourse("BIO118")!!
        // BIO118 requires BIO117
        assertFalse(engine.isEligible(bio118, emptySet()), "BIO118 should not be eligible without BIO117")
        assertTrue(engine.isEligible(bio118, setOf("BIO117")), "BIO118 should be eligible once BIO117 is completed")
    }

    @Test
    fun testThirdYearStandingRule() {
        val mrb195 = engine.findCourse("MRB195")!! // Requires STT034 + full 3rd year standing (terms 0..3)
        val allUnderclass = CurriculumData.ALL_COURSES.filter { it.term < 4 }.map { it.code }.toSet()

        assertFalse(engine.isEligible(mrb195, setOf("STT034")), "MRB195 must require third year standing")
        assertTrue(engine.isEligible(mrb195, allUnderclass), "MRB195 should be eligible when all lower terms are finished")
    }

    @Test
    fun testWorkloadStressCalculation() {
        // units = 18, confidence = 7 -> round(18 * 2.1 + (10 - 7) * 3) = round(37.8 + 9) = 47
        val workload = engine.evaluateWorkload(18, 7)
        assertEquals(47, workload.stressScore)
        assertEquals("Manageable workload", workload.rating)
    }

    @Test
    fun testPlanGenerationCoRequisiteBundling() {
        // Test with no completed courses: PHY101 should bundle PHY101.1
        val plan = engine.generatePlan(emptySet(), confidence = 7)
        val codes = plan.map { it.code }
        if (codes.contains("PHY101")) {
            assertTrue(codes.contains("PHY101.1"), "PHY101 and PHY101.1 should be bundled together as co-requisites")
        }
    }
}
