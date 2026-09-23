package com.fit50.app

import java.time.LocalDate

internal object QuestionnaireReviewSchedule {
    fun shouldPrompt(completedAt: LocalDate, lastPromptedAt: LocalDate?, today: LocalDate): Boolean =
        !today.isBefore(completedAt.plusMonths(1)) &&
            (lastPromptedAt == null || !today.isBefore(lastPromptedAt.plusMonths(1)))
}
