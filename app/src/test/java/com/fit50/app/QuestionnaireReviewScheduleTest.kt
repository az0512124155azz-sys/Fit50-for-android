package com.fit50.app

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionnaireReviewScheduleTest {
    @Test fun waitsAFullCalendarMonthAfterCompletion() {
        val completed = LocalDate.of(2026, 9, 23)
        assertFalse(QuestionnaireReviewSchedule.shouldPrompt(completed, null, LocalDate.of(2026, 10, 22)))
        assertTrue(QuestionnaireReviewSchedule.shouldPrompt(completed, null, LocalDate.of(2026, 10, 23)))
    }

    @Test fun doesNotPromptAgainWithinAMonthOfTheLastReminder() {
        val completed = LocalDate.of(2026, 9, 1)
        val prompted = LocalDate.of(2026, 10, 23)
        assertFalse(QuestionnaireReviewSchedule.shouldPrompt(completed, prompted, LocalDate.of(2026, 11, 22)))
        assertTrue(QuestionnaireReviewSchedule.shouldPrompt(completed, prompted, LocalDate.of(2026, 11, 23)))
    }

    @Test fun calendarMonthHandlesShorterMonths() {
        val completed = LocalDate.of(2026, 1, 31)
        assertFalse(QuestionnaireReviewSchedule.shouldPrompt(completed, null, LocalDate.of(2026, 2, 27)))
        assertTrue(QuestionnaireReviewSchedule.shouldPrompt(completed, null, LocalDate.of(2026, 2, 28)))
    }
}
