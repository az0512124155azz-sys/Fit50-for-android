package com.fit50.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPainPolicyTest {
    private fun report(area: String = "knees", intensity: Int = 3, pattern: String = "usual", flags: List<String> = emptyList()) =
        mapOf("area" to area, "intensity" to intensity, "pattern" to pattern,
            "warningSigns" to flags, "reportedAt" to 2_000L)

    @Test fun familiarMildPainMakesTheNextPlanGentlerAndAvoidsTheArea() {
        val baseline = mapOf<String, Any?>("painLevel" to 1, "painAreas" to listOf("shoulders"), "duration" to "45")
        val adjusted = WorkoutPainPolicy.applyToQuestionnaire(baseline, report(), 1_000L)
        assertFalse(WorkoutPainPolicy.requiresStop(report()))
        assertEquals(true, adjusted["recentWorkoutPain"])
        assertEquals(true, adjusted["avoid"])
        assertEquals(listOf("shoulders", "knees"), adjusted["painAreas"])
        val plan = WorkoutSelectionEngine.select(WorkoutPlanEngine.baseCatalog, adjusted, "user", java.time.LocalDate.of(2026, 9, 23))
        assertEquals(WorkoutSelectionEngine.Status.READY, plan.status)
        assertEquals(20, plan.duration)
        assertTrue(plan.exercises.none { "knees" in it.exercise.avoid })
        assertTrue(plan.exercises.all { it.sets == 1 })
    }

    @Test fun warningSignsStopAndRequireReviewUntilQuestionnaireIsUpdated() {
        for (unsafe in listOf(report(area = "chest"), report(intensity = 8), report(pattern = "new"), report(flags = listOf("dizziness")))) {
            assertTrue(WorkoutPainPolicy.requiresStop(unsafe))
            val adjusted = WorkoutPainPolicy.applyToQuestionnaire(emptyMap(), unsafe, 1_000L)
            val plan = WorkoutSelectionEngine.select(WorkoutPlanEngine.baseCatalog, adjusted, "user", java.time.LocalDate.of(2026, 9, 23))
            assertEquals(WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED, plan.status)
            assertTrue(plan.safetyReasons.any { "אימון קודם" in it })
            assertEquals(emptyMap<String, Any?>(), WorkoutPainPolicy.applyToQuestionnaire(emptyMap(), unsafe, 3_000L))
        }
    }

    @Test fun painChecksAppearOnlyForPlansWithReportedPain() {
        assertFalse(WorkoutPlanEngine.needsPainChecks(emptyMap<String, Any?>()))
        assertFalse(WorkoutPlanEngine.needsPainChecks(mapOf("painLevel" to 0, "painAreas" to listOf("none"))))
        assertTrue(WorkoutPlanEngine.needsPainChecks(mapOf("painLevel" to 3)))
        assertTrue(WorkoutPlanEngine.needsPainChecks(mapOf("recentWorkoutPain" to true)))
    }
}
