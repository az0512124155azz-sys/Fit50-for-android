package com.fit50.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSelectionEngineTest {
    private val ready = WorkoutSelectionEngine.Status.READY
    private val catalog = WorkoutPlanEngine.baseCatalog
    private val date = LocalDate.of(2026, 9, 23)
    private fun plan(q: Map<String, Any?> = emptyMap(), recent: Set<String> = emptySet(), day: LocalDate = date) =
        WorkoutSelectionEngine.select(catalog, q, "user-1", day, recent)

    @Test fun everyBaseExerciseHasAMovementPattern() {
        assertEquals(66, catalog.size)
        catalog.forEach { WorkoutSelectionEngine.patternFor(it.motionId) }
    }

    @Test fun sameInputsProduceTheSameBalancedSession() {
        val q = mapOf("mainGoal" to "strength", "duration" to "20", "lastTrained" to "now", "likes" to listOf("strength"))
        val first = plan(q)
        val second = plan(q)
        assertEquals(ready, first.status)
        assertEquals(first.exercises.map { it.exercise.id }, second.exercises.map { it.exercise.id })
        assertEquals(6, first.exercises.size)
        assertEquals("warmup", first.exercises.first().exercise.phase)
        assertEquals("cooldown", first.exercises.last().exercise.phase)
        assertEquals(first.exercises.size, first.exercises.map { it.exercise.id }.distinct().size)
        val patterns = first.exercises.filter { it.exercise.phase == "main" }.map { WorkoutSelectionEngine.patternFor(it.exercise.motionId) }.toSet()
        assertTrue(WorkoutSelectionEngine.Pattern.LOWER in patterns)
        assertTrue(WorkoutSelectionEngine.Pattern.UPPER_PUSH in patterns)
        assertTrue(WorkoutSelectionEngine.Pattern.UPPER_PULL in patterns)
        assertTrue(patterns.size >= 3)
    }

    @Test fun avoidsReportedPainAndUnconfirmedEquipment() {
        val result = plan(mapOf("mainGoal" to "balance", "painAreas" to listOf("knees"), "lastTrained" to "now"))
        assertEquals(ready, result.status)
        assertTrue(result.exercises.none { "knees" in it.exercise.avoid })
        assertTrue(result.exercises.none { it.exercise.motionId in setOf("band_row", "band_side_step", "chair_row", "biceps_band", "towel_row", "low_step", "pillow_squeeze") })
        val withEquipment = plan(mapOf("availableEquipment" to listOf("band", "towel", "step", "pillow"), "lastTrained" to "now"))
        assertTrue(withEquipment.eligibleCount > plan(mapOf("lastTrained" to "now")).eligibleCount)
    }

    @Test fun medicalFlagsDoNotReturnAFallbackWorkout() {
        for(q in listOf(mapOf("chestPain" to true), mapOf("restricted" to true), mapOf("surgery" to true), mapOf("conditions" to listOf("heart")), mapOf("painLevel" to 9))) {
            val result = plan(q)
            assertEquals(WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED, result.status)
            assertTrue(result.exercises.isEmpty())
        }
        val noOptions = WorkoutSelectionEngine.select(catalog.filter { it.id == "chair_squat" }, mapOf("painAreas" to listOf("knees")), "user-1", date)
        assertEquals(WorkoutSelectionEngine.Status.NO_SAFE_EXERCISES, noOptions.status)
        assertTrue(noOptions.exercises.isEmpty())
        val approved = plan(mapOf("conditions" to listOf("heart"), "clinicianApproved" to true, "lastTrained" to "now"))
        assertEquals(ready, approved.status)
        assertEquals(1, approved.maxDifficulty)
    }

    @Test fun painAndLongBreakReduceVolumeWithoutAdvancingDifficulty() {
        val result = plan(mapOf("painLevel" to 6, "lastTrained" to "never", "mainGoal" to "strength"))
        assertEquals(1, result.maxDifficulty)
        assertTrue(result.exercises.all { it.exercise.difficulty == 1 && it.sets == 1 && it.reps <= 8 && it.hold <= 25 })
        val frequent = plan(mapOf("lastTrained" to "now", "freq" to "5", "duration" to "45"))
        assertTrue(frequent.exercises.all { it.sets <= 2 })
    }

    @Test fun recentCompletionAndDateCanRotateTheSession() {
        val q = mapOf("mainGoal" to "health", "duration" to "30", "lastTrained" to "now")
        val original = plan(q).exercises.map { it.exercise.id }
        val withRecent = plan(q, original.toSet()).exercises.map { it.exercise.id }
        val dates = (1L..7L).map { plan(q, day = date.plusDays(it)).exercises.map { item -> item.exercise.id } }
        assertFalse(original == withRecent && dates.all { it == original })
    }

    @Test fun variedProfilesNeverBypassEligibilityOrPhaseStructure() {
        for(goal in listOf("health", "pain", "strength", "balance", "mobility"))
            for(area in listOf("none", "knees", "shoulders", "lowerBack"))
                for(training in listOf("never", "6m", "now")) {
                    val q = mapOf("mainGoal" to goal, "painAreas" to listOf(area), "lastTrained" to training, "duration" to "30")
                    val result = plan(q)
                    if(result.status != ready)continue
                    assertTrue(result.exercises.size <= 8)
                    assertEquals("warmup", result.exercises.first().exercise.phase)
                    assertEquals("cooldown", result.exercises.last().exercise.phase)
                    assertTrue(result.exercises.none { area in it.exercise.avoid })
                    assertTrue(result.exercises.all { it.exercise.difficulty <= result.maxDifficulty })
                    assertEquals(result.exercises.size, result.exercises.map { it.exercise.id }.distinct().size)
                }
    }
}
