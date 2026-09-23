package com.fit50.app

/** Conservative workout response to symptoms reported during a session. Not a diagnosis. */
internal object WorkoutPainPolicy {
    private val areas = setOf("chest", "head", "neck", "shoulders", "upperBack", "lowerBack", "hips", "knees", "ankles", "other")
    private val warningSigns = setOf("breathing", "dizziness", "fainting", "coldSweat", "radiating")

    fun valid(report: Map<*, *>): Boolean {
        val intensity = (report["intensity"] as? Number)?.toInt() ?: return false
        val area = report["area"] as? String ?: return false
        val pattern = report["pattern"] as? String ?: return false
        val reportedAt = (report["reportedAt"] as? Number)?.toLong() ?: return false
        return intensity in 1..10 && area in areas && pattern in setOf("usual", "new", "worse") && reportedAt > 0
    }

    fun requiresStop(report: Map<*, *>): Boolean {
        if (!valid(report)) return true
        val intensity = (report["intensity"] as Number).toInt()
        val flags = (report["warningSigns"] as? Collection<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
        return report["area"] == "chest" || intensity >= 7 || report["pattern"] != "usual" ||
            flags.any { it in warningSigns }
    }

    fun applyToQuestionnaire(questionnaire: Map<String, Any?>, report: Map<*, *>?, completedAtMillis: Long): Map<String, Any?> {
        if (report == null || !valid(report)) return questionnaire
        val reportedAt = (report["reportedAt"] as Number).toLong()
        if (completedAtMillis >= reportedAt) return questionnaire
        val adjusted = questionnaire.toMutableMap()
        adjusted["recentWorkoutPain"] = true
        if (requiresStop(report)) {
            adjusted["exercisePainClearanceRequired"] = true
            return adjusted
        }
        val existingPain = when (val value = questionnaire["painLevel"]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
        adjusted["painLevel"] = maxOf(existingPain, (report["intensity"] as Number).toInt())
        adjusted["avoid"] = true
        val oldAreas = (questionnaire["painAreas"] as? Collection<*>)?.filterIsInstance<String>().orEmpty()
        adjusted["painAreas"] = (oldAreas + report["area"].toString()).distinct()
        return adjusted
    }
}
