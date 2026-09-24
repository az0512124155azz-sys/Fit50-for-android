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
        assertEquals(10, first.exercises.size)
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
        for(q in listOf(mapOf("chestPain" to true), mapOf("restricted" to true), mapOf("surgery" to true), mapOf("conditions" to listOf("heart")))) {
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
        val symptomsDespiteApproval = plan(mapOf("conditions" to listOf("heart"), "clinicianApproved" to true, "chestPain" to true))
        assertEquals(WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED, symptomsDespiteApproval.status)
    }

    @Test fun listsEveryClearanceReasonAtOnce() {
        val result = plan(mapOf(
            "chestPain" to true, "restricted" to true, "surgery" to true,
            "conditions" to listOf("heart"), "painLevel" to 9, "painPattern" to "new"
        ))
        assertEquals(WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED, result.status)
        assertEquals(5, result.safetyReasons.size)
        assertTrue(result.exercises.isEmpty())
    }

    @Test fun newScreeningSymptomsAreReviewedTogether() {
        val result = plan(mapOf(
            "chestPainRestDaily" to true, "dizzyLossBalance" to true,
            "fainted" to true, "asthmaRecentSymptoms" to true
        ))
        assertEquals(WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED, result.status)
        assertEquals(4, result.safetyReasons.size)
        assertTrue(result.exercises.isEmpty())
        val reviewed = plan(mapOf(
            "dizzyLossBalance" to true, "fainted" to true,
            "symptomsCleared" to true, "duration" to "45"
        ))
        assertEquals(ready, reviewed.status)
        assertEquals(20, reviewed.duration)
        assertTrue(reviewed.exercises.filter { it.exercise.phase == "main" }.all { it.rest in 10..35 })
        assertTrue(reviewed.exercises.filter { it.exercise.phase != "main" }.all { it.rest <= 10 })
        val chestAtRest = plan(mapOf("chestPainRestDaily" to true, "symptomsCleared" to true))
        assertEquals(WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED, chestAtRest.status)
    }

    @Test fun existingQuestionnaireNeedsNewScreeningOnlyOnce() {
        val old = mapOf<String, Any?>("completedAt" to "2026-09-01T00:00:00Z", "lastTrained" to "now")
        assertEquals(WorkoutSelectionEngine.Status.SCREENING_REQUIRED, plan(old).status)
        val updated = old + mapOf(
            "chestPainRestDaily" to false, "dizzyLossBalance" to false, "fainted" to false
        )
        assertEquals(ready, plan(updated).status)
        val asthma = updated + mapOf("conditions" to listOf("asthma"))
        assertEquals(WorkoutSelectionEngine.Status.SCREENING_REQUIRED, plan(asthma).status)
        assertEquals(ready, plan(asthma + mapOf("asthmaRecentMeds" to true, "asthmaRecentSymptoms" to false)).status)
    }

    @Test fun resolvedAssessedChestPainReceivesConservativePlan() {
        val result = plan(mapOf(
            "chestPain" to true, "chestPainStatus" to "cleared", "duration" to "45", "lastTrained" to "now"
        ))
        assertEquals(ready, result.status)
        assertEquals(20, result.duration)
        assertEquals(1, result.maxDifficulty)
        assertTrue(result.exercises.all { it.sets == 1 })
        assertTrue(result.exercises.filter { it.exercise.phase == "main" }.all { it.rest in 10..35 })
        assertTrue(result.exercises.filter { it.exercise.phase != "main" }.all { it.rest <= 10 })
    }

    @Test fun painAndLongBreakReduceVolumeWithoutAdvancingDifficulty() {
        val result = plan(mapOf("painLevel" to 6, "lastTrained" to "never", "mainGoal" to "strength"))
        assertEquals(1, result.maxDifficulty)
        assertTrue(result.exercises.all { it.exercise.difficulty == 1 && it.sets == 1 })
        assertTrue(result.exercises.filter { it.exercise.phase == "main" }.all { it.reps <= 8 && it.hold <= 25 })
        val frequent = plan(mapOf("lastTrained" to "now", "freq" to "5", "duration" to "45"))
        assertTrue(frequent.exercises.all { it.sets <= 2 })
    }

    @Test fun highTypicalPainGetsAShortAdaptedSessionWhenNoRedFlagsAreReported() {
        val result = plan(mapOf("painLevel" to 9, "painPattern" to "stable", "painAreas" to listOf("knees"), "duration" to "45", "mainGoal" to "pain"))
        assertEquals(ready, result.status)
        assertEquals(15, result.duration)
        assertTrue(result.exercises.size <= 5)
        assertEquals(1, result.maxDifficulty)
        assertTrue(result.exercises.none { "knees" in it.exercise.avoid })
        assertTrue(result.exercises.all { it.sets == 1 })
        assertTrue(result.exercises.filter { it.exercise.phase == "main" }.all { it.reps <= 6 && it.hold <= 15 })
        assertTrue(result.exercises.filter { it.exercise.phase != "main" }.all { it.hold <= 30 })
        assertTrue(result.exercises.filter { it.exercise.phase == "main" }.all { it.rest in 10..35 })
        assertTrue(result.exercises.filter { it.exercise.phase != "main" }.all { it.rest <= 10 })
        val withRestriction = plan(mapOf("painLevel" to 9, "restricted" to true))
        assertEquals(WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED, withRestriction.status)
        for (pattern in listOf(null, "new", "unsure")) {
            val answers = mapOf<String, Any?>("painLevel" to 9, "painPattern" to pattern)
            assertEquals(WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED, plan(answers).status)
        }
    }

    @Test fun recoveryMatchesExerciseLoadAndDiffersFromBetweenSetRest() {
        val easy = catalog.first { it.id == "chin_tuck" }
        val demanding = catalog.first { it.id == "chair_squat" }
        val warmup = catalog.first { it.id == "shoulder_roll" }
        val cooldown = catalog.first { it.id == "deep_breath" }
        assertEquals(10, WorkoutSelectionEngine.transitionRest(warmup, easy, false))
        assertEquals(15, WorkoutSelectionEngine.transitionRest(easy, demanding, false))
        assertEquals(35, WorkoutSelectionEngine.transitionRest(demanding, easy, true))
        assertEquals(10, WorkoutSelectionEngine.transitionRest(demanding, cooldown, false))
        assertEquals(0, WorkoutSelectionEngine.transitionRest(cooldown, null, false))

        val result = plan(mapOf("mainGoal" to "strength", "lastTrained" to "now", "duration" to "30"))
        assertEquals(ready, result.status)
        assertTrue(result.exercises.all { it.rest in 0..45 })
        assertTrue(result.exercises.filter { it.sets > 1 }.all { it.setRest in 25..60 })
        assertEquals(0, result.exercises.last().rest)
    }

    @Test fun selectedDurationChangesActualWorkInsteadOfPaddingRest() {
        val base = mapOf<String, Any?>("mainGoal" to "strength", "lastTrained" to "now")
        val short = plan(base + ("duration" to "15"))
        val long = plan(base + ("duration" to "30"))
        assertEquals(ready, short.status)
        assertEquals(ready, long.status)
        assertTrue(long.exercises.size > short.exercises.size)
        assertTrue(long.exercises.sumOf { it.sets } > short.exercises.sumOf { it.sets })
        assertTrue(WorkoutSelectionEngine.estimateMinutes(long.exercises) > WorkoutSelectionEngine.estimateMinutes(short.exercises))
        assertTrue(WorkoutSelectionEngine.estimateMinutes(short.exercises) >= 10)
        assertTrue(WorkoutSelectionEngine.estimateMinutes(long.exercises) >= 20)
        assertTrue(long.exercises.all { it.rest <= 45 })

        val beginner = plan(mapOf("duration" to "20", "lastTrained" to "never"))
        assertEquals(ready, beginner.status)
        assertTrue(beginner.exercises.any { it.exercise.phase == "main" && it.sets == 2 })
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
                    assertTrue(result.exercises.size <= 13)
                    assertEquals("warmup", result.exercises.first().exercise.phase)
                    assertEquals("cooldown", result.exercises.last().exercise.phase)
                    assertTrue(result.exercises.none { area in it.exercise.avoid })
                    assertTrue(result.exercises.all { it.exercise.difficulty <= result.maxDifficulty })
                    assertEquals(result.exercises.size, result.exercises.map { it.exercise.id }.distinct().size)
                }
    }
}
