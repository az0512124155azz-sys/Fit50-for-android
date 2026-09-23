package com.fit50.app

import java.time.LocalDate

private typealias Ex = WorkoutPlanEngine.Ex

/** Selects a complete session. This is exercise planning, not medical diagnosis. */
internal object WorkoutSelectionEngine {

    enum class Pattern { MOBILITY, CARDIO, LOWER, UPPER_PUSH, UPPER_PULL, CORE, BALANCE, BREATH }
    enum class Status(val code: String) { READY("ready"), CLEARANCE_REQUIRED("clearance_required"), NO_SAFE_EXERCISES("no_safe_exercises") }
    data class Prescription(val exercise: Ex, val sets: Int, val reps: Int, val hold: Int, val breaths: Int, val rest: Int)
    data class Plan(
        val status: Status,
        val message: String,
        val exercises: List<Prescription>,
        val duration: Int,
        val frequency: Int,
        val mainGoal: String,
        val maxDifficulty: Int,
        val conservative: Boolean,
        val eligibleCount: Int
    )

    private val equipment = mapOf(
        "band_row" to "band", "band_side_step" to "band", "chair_row" to "band", "biceps_band" to "band",
        "towel_row" to "towel", "low_step" to "step", "pillow_squeeze" to "pillow"
    )
    private val floorExercises = setOf("knee_push", "knee_plank", "bird_dog", "cat_cow", "child_pose", "glute_bridge", "clamshell", "dead_bug", "pelvic_tilt")
    private val spineFlexion = setOf("seated_twist", "child_pose", "hamstring_chair", "good_morning", "hip_hinge", "cat_cow")

    fun patternFor(id: String): Pattern = when(id) {
        "march", "step_touch", "seated_march", "chair_punch" -> Pattern.CARDIO
        "sit_to_stand", "chair_squat", "side_step", "band_side_step", "glute_bridge", "clamshell", "low_step",
        "chair_knee_lift", "seated_leg_extend", "mini_lunge", "hip_hinge", "good_morning", "wall_sit_short", "pillow_squeeze" -> Pattern.LOWER
        "wall_push", "counter_push", "knee_push", "triceps_wall", "front_raise", "lateral_raise", "wall_press_iso" -> Pattern.UPPER_PUSH
        "band_row", "towel_row", "chair_row", "biceps_band", "scap_squeeze" -> Pattern.UPPER_PULL
        "dead_bug", "bird_dog", "knee_plank", "wall_plank", "pelvic_tilt" -> Pattern.CORE
        "heel_raise", "toe_raise", "wall_calf_raise", "single_leg_support", "tandem_stance", "line_walk",
        "clock_reach", "weight_shift", "standing_knee_drive", "back_step" -> Pattern.BALANCE
        "deep_breath", "box_breath" -> Pattern.BREATH
        "shoulder_roll", "ankle_circle", "hip_circle", "arm_swing", "wall_angels", "wall_slide", "cat_cow",
        "child_pose", "chest_wall_stretch", "neck_side", "hamstring_chair", "calf_wall", "hip_flexor_chair",
        "thoracic_open", "seated_twist", "figure_four_chair", "chin_tuck", "wrist_mobility", "side_reach" -> Pattern.MOBILITY
        else -> error("Exercise has no movement pattern: $id")
    }

    private fun family(id: String): String = when(id) {
        "sit_to_stand", "chair_squat", "wall_sit_short" -> "squat"
        "wall_push", "counter_push", "knee_push", "wall_press_iso", "triceps_wall" -> "push"
        "band_row", "towel_row", "chair_row" -> "row"
        "heel_raise", "wall_calf_raise" -> "calf_raise"
        "march", "seated_march", "standing_knee_drive" -> "march"
        "side_step", "band_side_step", "step_touch" -> "side_step"
        "mini_lunge", "back_step" -> "lunge"
        else -> id
    }

    private fun text(value: Any?) = value?.toString().orEmpty()
    private fun number(value: Any?, default: Int) = text(value).toIntOrNull() ?: default
    private fun flag(value: Any?) = when(value) {
        is Boolean -> value
        else -> text(value).lowercase() in setOf("1", "true", "yes")
    }
    private fun values(value: Any?): Set<String> = when(value) {
        is Collection<*> -> value.map(::text).filter(String::isNotBlank).toSet()
        is Array<*> -> value.map(::text).filter(String::isNotBlank).toSet()
        else -> emptySet()
    } - "none"

    private fun desired(goal: String): List<Pattern> = when(goal) {
        "strength" -> listOf(Pattern.LOWER, Pattern.UPPER_PUSH, Pattern.UPPER_PULL, Pattern.BALANCE, Pattern.CORE)
        "balance" -> listOf(Pattern.BALANCE, Pattern.LOWER, Pattern.CORE, Pattern.UPPER_PUSH, Pattern.CARDIO)
        "mobility", "pain" -> listOf(Pattern.MOBILITY, Pattern.LOWER, Pattern.BALANCE, Pattern.UPPER_PUSH, Pattern.CORE)
        else -> listOf(Pattern.CARDIO, Pattern.LOWER, Pattern.UPPER_PUSH, Pattern.BALANCE, Pattern.UPPER_PULL)
    }

    fun select(catalog: List<Ex>, questionnaire: Map<*, *>?, userSeed: String, date: LocalDate,
               recentExerciseIds: Set<String> = emptySet()): Plan {
        val q = questionnaire ?: emptyMap<String, Any?>()
        val pain = number(q["painLevel"], 0).coerceIn(0, 10)
        val highPain = pain >= 8
        val requestedDuration = number(q["duration"], 20).coerceIn(15, 45)
        val duration = if (highPain) 15 else requestedDuration
        val frequency = number(q["freq"], 3).coerceIn(2, 5)
        val goal = text(q["mainGoal"]).takeIf { it in setOf("health", "pain", "strength", "balance", "mobility") } ?: "health"
        val conditions = values(q["conditions"])
        val meds = values(q["meds"])
        val painAreas = values(q["painAreas"]) + if("back" in conditions) setOf("lowerBack") else emptySet()
        val likes = values(q["likes"])
        val goals = values(q["extraGoals"]) + goal
        val availableEquipment = values(q["availableEquipment"])
        val recentMotions = recentExerciseIds.map { recent -> catalog.firstOrNull { it.id == recent }?.motionId ?: recent }.toSet()
        val trained = text(q["lastTrained"])
        val approved = flag(q["clinicianApproved"])
        val clearanceMessage = when {
            flag(q["chestPain"]) -> "בשאלון צוין כאב בחזה בזמן מאמץ. לפני אימון עצמאי יש לברר את הסיבה עם איש מקצוע רפואי."
            flag(q["restricted"]) -> "בשאלון צוין שרופא הגביל פעילות גופנית. יש לפעול לפי ההנחיות שקיבלת לפני אימון עצמאי."
            !approved && flag(q["surgery"]) -> "בשאלון צוין ניתוח בשנתיים האחרונות ללא אישור להתחיל פעילות. יש לברר את ההתאמה לפני אימון עצמאי."
            !approved && "heart" in conditions -> "בשאלון צוין מצב לבבי ללא אישור להתחיל פעילות. יש לברר את ההתאמה לפני אימון עצמאי."
            else -> null
        }
        var maxDifficulty = when(trained) { "now" -> 3; "6m" -> 2; else -> 1 }
        val conservative = pain >= 5 || flag(q["avoid"]) || flag(q["surgery"]) || conditions.any { it in setOf("heart", "bp", "joints", "back", "osteo") } || meds.any { it in setOf("heart", "bp", "thinners") }
        if(conservative) maxDifficulty = 1
        if(clearanceMessage != null) return Plan(Status.CLEARANCE_REQUIRED, clearanceMessage, emptyList(), duration, frequency, goal, 1, true, 0)

        val eligible = catalog.filter { ex ->
            val id = ex.motionId
            ex.difficulty <= maxDifficulty && ex.avoid.intersect(painAreas).isEmpty() &&
                (equipment[id] == null || equipment[id] in availableEquipment) &&
                !("osteo" in conditions && id in spineFlexion) &&
                !(conservative && id in floorExercises)
        }
        val empty = Plan(Status.NO_SAFE_EXERCISES, "לא נמצאו מספיק תרגילים מתאימים לתשובות בשאלון. כדאי לעדכן את השאלון או להתייעץ עם איש מקצוע.", emptyList(), duration, frequency, goal, maxDifficulty, conservative, eligible.size)
        val targetCount = when(duration) { 15 -> 5; in 16..20 -> 6; in 21..30 -> 8; else -> 10 }
        val warmCount = if(duration >= 30) 2 else 1
        val coolCount = if(duration >= 30) 2 else 1
        val mainCount = targetCount - warmCount - coolCount
        val chosen = mutableListOf<Ex>()
        val chosenIds = mutableSetOf<String>()
        val families = mutableSetOf<String>()
        val patternCounts = mutableMapOf<Pattern, Int>()
        fun stableNoise(id: String): Int = Math.floorMod("$userSeed|$date|$goal|$id".hashCode(), 11)
        fun score(ex: Ex): Int {
            val pattern = patternFor(ex.motionId)
            return (if(goal in ex.goals) 40 else 0) + ex.goals.intersect(goals).size * 11 +
                ex.likes.intersect(likes).size * 8 + (if(ex.motionId in recentMotions) -24 else 0) +
                (if(ex.phase == "main" && patternCounts.getOrDefault(pattern, 0) == 0) 24 else 0) -
                patternCounts.getOrDefault(pattern, 0) * 22 + stableNoise(ex.id)
        }
        fun pick(phase: String, pattern: Pattern? = null): Boolean {
            val best = eligible.asSequence().filter { it.phase == phase && it.id !in chosenIds && family(it.motionId) !in families }
                .filter { pattern == null || patternFor(it.motionId) == pattern }
                .maxWithOrNull(compareBy<Ex> { score(it) }.thenBy { it.id }) ?: return false
            chosen += best
            chosenIds += best.id
            families += family(best.motionId)
            val group = patternFor(best.motionId)
            patternCounts[group] = patternCounts.getOrDefault(group, 0) + 1
            return true
        }
        repeat(warmCount) { pick("warmup") }
        val patternOrder = desired(goal).toMutableList()
        val rotation = Math.floorMod(date.toEpochDay() + userSeed.hashCode().toLong(), 2L).toInt()
        if(goal == "health" && rotation == 1) {
            patternOrder.remove(Pattern.UPPER_PULL)
            patternOrder.add(2, Pattern.UPPER_PULL)
        }
        if(goal == "strength" && rotation == 1) {
            patternOrder.remove(Pattern.CORE)
            patternOrder.add(3, Pattern.CORE)
        }
        for(pattern in patternOrder) if(chosen.count { it.phase == "main" } < mainCount) pick("main", pattern)
        while(chosen.count { it.phase == "main" } < mainCount && pick("main")) { /* fill remaining movement slots */ }
        repeat(coolCount) { pick("cooldown") }
        if(chosen.none { it.phase == "warmup" } || chosen.count { it.phase == "main" } < 2 || chosen.none { it.phase == "cooldown" }) return empty

        val novice = trained !in setOf("now", "6m")
        val prescriptions = chosen.map { ex ->
            val gentle = conservative || novice
            val sets = if(ex.phase != "main" || gentle) 1 else ex.sets.coerceAtMost(if(duration <= 20 || frequency >= 4) 2 else 3)
            Prescription(ex, sets, if(highPain) ex.reps.coerceAtMost(6) else if(gentle) ex.reps.coerceAtMost(8) else ex.reps,
                if(highPain) ex.hold.coerceAtMost(15) else if(gentle) ex.hold.coerceAtMost(25) else ex.hold, ex.breaths,
                if(highPain) ex.rest.coerceAtLeast(60) else if(gentle) ex.rest.coerceAtLeast(40) else ex.rest)
        }
        return Plan(Status.READY, "", prescriptions, duration, frequency, goal, maxDifficulty, conservative, eligible.size)
    }
}
