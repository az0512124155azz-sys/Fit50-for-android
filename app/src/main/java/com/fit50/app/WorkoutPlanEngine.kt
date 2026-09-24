package com.fit50.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object WorkoutPlanEngine {
    data class Ex(
        val id:String,
        val name:String,
        val muscle:String,
        val type:String,
        val goals:Set<String>,
        val likes:Set<String>,
        val avoid:Set<String> = emptySet(),
        val difficulty:Int = 1,
        val phase:String = "main",
        val sets:Int = 2,
        val reps:Int = 10,
        val hold:Int = 0,
        val breaths:Int = 0,
        val rest:Int = 45,
        val safety:String,
        val motionId:String = id
    )

    internal val baseCatalog = listOf(
        Ex("shoulder_roll","סיבובי כתפיים","כתפיים · חימום","reps",setOf("mobility","health","posture"),setOf("mobility","yoga","garden"),difficulty=1,phase="warmup",sets=1,reps=10,rest=15,safety="תנועה איטית, ללא כאב"),
        Ex("march","הליכה במקום","רגליים · חימום","hold",setOf("health","weight","energy"),setOf("walk","garden"),difficulty=1,phase="warmup",sets=1,hold=90,rest=20,safety="קצב נוח ונשימה רציפה"),
        Ex("ankle_circle","סיבובי קרסול","קרסוליים · חימום","reps",setOf("mobility","balance"),setOf("mobility","walk"),avoid=setOf("ankles"),difficulty=1,phase="warmup",sets=1,reps=10,rest=15,safety="טווח קטן ונוח"),
        Ex("hip_circle","סיבובי אגן","ירכיים · חימום","reps",setOf("mobility","balance"),setOf("mobility","yoga"),avoid=setOf("hips","lowerBack"),difficulty=1,phase="warmup",sets=1,reps=8,rest=15,safety="ללא סיבוב חד בגב"),
        Ex("arm_swing","פתיחת ידיים","חזה · כתפיים","reps",setOf("mobility","posture","health"),setOf("mobility","walk"),avoid=setOf("shoulders"),difficulty=1,phase="warmup",sets=1,reps=12,rest=15,safety="לא לעבור טווח נוח"),
        Ex("sit_to_stand","קימה וישיבה מכיסא","ירכיים · ישבן","reps",setOf("strength","balance","independence"),setOf("strength","garden"),avoid=setOf("knees","hips"),difficulty=1,sets=2,reps=8,rest=45,safety="כיסא יציב, ברכיים בקו כפות הרגליים"),
        Ex("chair_squat","סקוואט לכיסא","ירכיים · ישבן","reps",setOf("strength","independence","weight"),setOf("strength","garden"),avoid=setOf("knees","hips"),difficulty=2,sets=3,reps=10,rest=55,safety="לרדת רק עד טווח נוח"),
        Ex("wall_push","שכיבות סמיכה בקיר","חזה · כתפיים · יד אחורית","reps",setOf("strength","posture","independence"),setOf("strength"),avoid=setOf("shoulders"),difficulty=1,sets=2,reps=10,rest=45,safety="גוף בקו ישר, מרפקים בשליטה"),
        Ex("counter_push","שכיבות סמיכה על משטח","חזה · כתפיים","reps",setOf("strength","health"),setOf("strength"),avoid=setOf("shoulders"),difficulty=2,sets=3,reps=8,rest=55,safety="משטח יציב ולא מחליק"),
        Ex("knee_push","שכיבות סמיכה על ברכיים","חזה · כתפיים","reps",setOf("strength"),setOf("strength"),avoid=setOf("shoulders","lowerBack"),difficulty=3,sets=3,reps=8,rest=60,safety="לשמור על ליבה אסופה"),
        Ex("band_row","חתירה עם גומיה","גב · יד קדמית","reps",setOf("strength","posture"),setOf("strength","garden"),avoid=setOf("shoulders","upperBack"),difficulty=2,sets=3,reps=10,rest=55,safety="גב ניטרלי ומשיכה מבוקרת"),
        Ex("towel_row","משיכת מגבת איזומטרית","גב · ידיים","hold",setOf("strength","posture"),setOf("strength"),avoid=setOf("shoulders"),difficulty=1,sets=2,hold=20,rest=40,safety="למשוך בעדינות ללא כאב"),
        Ex("wall_angels","מלאכי קיר","כתפיים · גב עליון","reps",setOf("posture","mobility"),setOf("mobility","strength"),avoid=setOf("shoulders","upperBack"),difficulty=2,sets=2,reps=8,rest=35,safety="לא לכפות מגע של הידיים בקיר"),
        Ex("heel_raise","עליות עקב","שוקיים · קרסול","reps",setOf("strength","balance","independence"),setOf("walk","strength"),avoid=setOf("ankles"),difficulty=1,sets=2,reps=12,rest=35,safety="להיעזר בקיר אם צריך"),
        Ex("toe_raise","הרמות אצבעות","שוק קדמי · יציבות","reps",setOf("balance","independence"),setOf("walk"),avoid=setOf("ankles"),difficulty=1,sets=2,reps=12,rest=30,safety="להישען קלות על קיר"),
        Ex("side_step","צעדי צד","ירכיים · ישבן","reps",setOf("strength","balance","weight"),setOf("walk","garden"),avoid=setOf("knees","hips","ankles"),difficulty=1,sets=2,reps=10,rest=35,safety="צעדים קטנים וברכיים רכות"),
        Ex("band_side_step","צעדי צד עם גומיה","ישבן · ירכיים","reps",setOf("strength","balance"),setOf("strength"),avoid=setOf("knees","hips"),difficulty=3,sets=3,reps=10,rest=50,safety="גומיה קלה, בלי קריסת ברכיים"),
        Ex("glute_bridge","גשר אגן","ישבן · ירך אחורית","reps",setOf("strength","posture"),setOf("strength","yoga"),avoid=setOf("lowerBack","hips"),difficulty=2,sets=3,reps=10,rest=45,safety="לא לקשת את הגב"),
        Ex("clamshell","פתיחת צד בשכיבה","ישבן צדדי","reps",setOf("strength","balance"),setOf("strength","mobility"),avoid=setOf("hips"),difficulty=1,sets=2,reps=12,rest=35,safety="אגן נשאר יציב"),
        Ex("dead_bug","Dead Bug מותאם","ליבה · בטן","reps",setOf("strength","posture","balance"),setOf("strength","yoga"),avoid=setOf("lowerBack"),difficulty=2,sets=2,reps=8,rest=40,safety="גב תחתון נשאר נוח"),
        Ex("bird_dog","Bird-dog","ליבה · גב · יציבות","reps",setOf("strength","balance","posture"),setOf("strength","yoga"),avoid=setOf("knees","shoulders","lowerBack"),difficulty=2,sets=2,reps=8,rest=40,safety="להאריך ולא להרים גבוה"),
        Ex("knee_plank","פלאנק על ברכיים","ליבה · בטן","hold",setOf("strength","posture"),setOf("strength","yoga"),avoid=setOf("shoulders","knees","lowerBack"),difficulty=2,sets=2,hold=20,rest=45,safety="קו ישר מהראש לברכיים"),
        Ex("wall_plank","פלאנק קיר","ליבה · כתפיים","hold",setOf("strength","posture"),setOf("strength"),avoid=setOf("shoulders"),difficulty=1,sets=2,hold=25,rest=35,safety="מרחק נוח מהקיר"),
        Ex("step_touch","Step Touch","רגליים · קצב","hold",setOf("health","weight","energy"),setOf("walk","bike"),avoid=setOf("knees","ankles"),difficulty=1,sets=2,hold=45,rest=30,safety="ללא קפיצות"),
        Ex("low_step","עלייה למדרגה נמוכה","רגליים · סיבולת","reps",setOf("strength","health","weight"),setOf("walk","garden"),avoid=setOf("knees","ankles","hips"),difficulty=2,sets=2,reps=8,rest=45,safety="מדרגה נמוכה ויציבה"),
        Ex("chair_knee_lift","הרמת ברך בישיבה","ליבה · ירכיים","reps",setOf("strength","independence"),setOf("strength"),avoid=setOf("hips"),difficulty=1,sets=2,reps=10,rest=30,safety="לשבת זקוף"),
        Ex("seated_leg_extend","יישור ברך בישיבה","ירך קדמית","reps",setOf("strength","independence"),setOf("strength"),avoid=setOf("knees"),difficulty=1,sets=2,reps=10,rest=30,safety="לא לנעול את הברך"),
        Ex("chair_row","חתירה בישיבה עם גומיה","גב · ידיים","reps",setOf("strength","posture"),setOf("strength"),avoid=setOf("shoulders","upperBack"),difficulty=1,sets=2,reps=10,rest=35,safety="חזה פתוח, גב נינוח"),
        Ex("wall_slide","החלקת ידיים על קיר","כתפיים · יציבה","reps",setOf("mobility","posture"),setOf("mobility"),avoid=setOf("shoulders"),difficulty=1,sets=2,reps=8,rest=30,safety="לעצור לפני כאב"),
        Ex("single_leg_support","עמידה על רגל עם תמיכה","שיווי משקל","hold",setOf("balance","independence"),setOf("walk","yoga"),avoid=setOf("ankles","knees","hips"),difficulty=1,sets=2,hold=20,rest=30,safety="יד ליד קיר או כיסא"),
        Ex("tandem_stance","עמידת עקב-אצבע","שיווי משקל","hold",setOf("balance","independence"),setOf("walk","yoga"),avoid=setOf("ankles"),difficulty=1,sets=2,hold=25,rest=30,safety="לעמוד ליד תמיכה"),
        Ex("line_walk","הליכה על קו","שיווי משקל · הליכה","reps",setOf("balance","independence"),setOf("walk"),avoid=setOf("ankles","knees"),difficulty=2,sets=2,reps=10,rest=35,safety="ליד קיר לתמיכה"),
        Ex("clock_reach","נגיעות שעון","יציבות · ירכיים","reps",setOf("balance","strength"),setOf("walk","strength"),avoid=setOf("ankles","knees","hips"),difficulty=2,sets=2,reps=6,rest=40,safety="טווח קטן ותמיכה זמינה"),
        Ex("weight_shift","העברת משקל","יציבות · קרסוליים","reps",setOf("balance","independence"),setOf("walk"),avoid=setOf("ankles"),difficulty=1,sets=2,reps=10,rest=25,safety="תנועה איטית ליד משענת"),
        Ex("cat_cow","Cat-Cow עדין","עמוד שדרה","reps",setOf("mobility","pain","posture"),setOf("yoga","mobility"),avoid=setOf("knees","lowerBack"),difficulty=1,sets=1,reps=8,rest=20,safety="טווח קטן ללא כאב"),
        Ex("child_pose","Child's Pose מותאם","גב · ירכיים","hold",setOf("mobility","stress","pain"),setOf("yoga","mobility"),avoid=setOf("knees","hips"),difficulty=1,phase="cooldown",sets=1,hold=25,rest=15,safety="כרית תחת הברכיים אם צריך"),
        Ex("chest_wall_stretch","מתיחת חזה בקיר","חזה · כתף","hold",setOf("mobility","posture","pain"),setOf("mobility"),avoid=setOf("shoulders"),difficulty=1,phase="cooldown",sets=1,hold=25,rest=15,safety="ללא משיכה חדה"),
        Ex("neck_side","מתיחת צוואר צדית","צוואר","hold",setOf("mobility","pain","stress"),setOf("mobility","yoga"),avoid=setOf("neck"),difficulty=1,phase="cooldown",sets=1,hold=20,rest=15,safety="ללא משיכה ביד"),
        Ex("hamstring_chair","מתיחת ירך אחורית בכיסא","ירך אחורית","hold",setOf("mobility","pain"),setOf("mobility","yoga"),avoid=setOf("lowerBack","hips"),difficulty=1,phase="cooldown",sets=1,hold=25,rest=15,safety="גב ארוך, לא להתכופף עמוק"),
        Ex("calf_wall","מתיחת שוק בקיר","שוק · קרסול","hold",setOf("mobility","walk"),setOf("mobility","walk"),avoid=setOf("ankles"),difficulty=1,phase="cooldown",sets=1,hold=25,rest=15,safety="עקב נשאר על הרצפה"),
        Ex("hip_flexor_chair","מתיחת מכופפי ירך בעמידה","ירכיים","hold",setOf("mobility","posture"),setOf("mobility","walk"),avoid=setOf("hips","lowerBack"),difficulty=1,phase="cooldown",sets=1,hold=20,rest=15,safety="אגן ניטרלי"),
        Ex("thoracic_open","פתיחת גב עליון בישיבה","גב עליון · חזה","reps",setOf("mobility","posture","pain"),setOf("mobility","yoga"),avoid=setOf("upperBack","shoulders"),difficulty=1,phase="cooldown",sets=1,reps=8,rest=15,safety="תנועה קטנה ונשימה רגועה"),
        Ex("deep_breath","נשימות עמוקות","נשימה · רגיעה","breath",setOf("stress","sleep","health"),setOf("yoga","mobility"),difficulty=1,phase="cooldown",sets=1,breaths=5,rest=10,safety="להפסיק אם יש סחרחורת"),
        Ex("box_breath","נשימת 4-4","נשימה · רגיעה","breath",setOf("stress","sleep"),setOf("yoga"),difficulty=1,phase="cooldown",sets=1,breaths=5,rest=10,safety="נשימה נוחה ללא עצירה מאומצת"),
        Ex("seated_twist","סיבוב גב עדין בישיבה","גב · מוביליטי","hold",setOf("mobility","posture"),setOf("mobility","yoga"),avoid=setOf("lowerBack","upperBack"),difficulty=1,phase="cooldown",sets=1,hold=20,rest=15,safety="ללא דחיפה עם הידיים"),
        Ex("wall_calf_raise","עליות עקב ליד קיר","שוק · שיווי משקל","reps",setOf("strength","balance"),setOf("walk","strength"),avoid=setOf("ankles"),difficulty=1,sets=3,reps=10,rest=35,safety="קיר לתמיכה"),
        Ex("mini_lunge","לאנג׳ קצר עם תמיכה","רגליים · ישבן","reps",setOf("strength","balance"),setOf("strength","garden"),avoid=setOf("knees","hips","ankles"),difficulty=3,sets=2,reps=6,rest=50,safety="טווח קצר ותמיכה קבועה"),
        Ex("hip_hinge","Hip Hinge לקיר","ירך אחורית · ישבן","reps",setOf("strength","posture"),setOf("strength","garden"),avoid=setOf("lowerBack","hips"),difficulty=2,sets=2,reps=10,rest=40,safety="גב ניטרלי, ישבן לאחור"),
        Ex("good_morning","Good Morning קל","ירך אחורית · גב","reps",setOf("strength","posture"),setOf("strength"),avoid=setOf("lowerBack","hips"),difficulty=3,sets=2,reps=10,rest=45,safety="ללא משקל ובטווח קטן"),
        Ex("biceps_band","כפיפת מרפקים עם גומיה","יד קדמית","reps",setOf("strength","independence"),setOf("strength"),difficulty=2,sets=2,reps=12,rest=35,safety="מרפקים צמודים לגוף"),
        Ex("triceps_wall","פשיטת מרפקים בקיר","יד אחורית","reps",setOf("strength"),setOf("strength"),avoid=setOf("shoulders"),difficulty=2,sets=2,reps=10,rest=35,safety="כתפיים רחוקות מהאוזניים"),
        Ex("front_raise","הרמת ידיים קדימה ללא משקל","כתפיים","reps",setOf("strength","posture"),setOf("strength"),avoid=setOf("shoulders"),difficulty=1,sets=2,reps=10,rest=30,safety="לא להרים מעל גובה כתף"),
        Ex("lateral_raise","הרמת ידיים לצדדים ללא משקל","כתפיים","reps",setOf("strength","posture"),setOf("strength"),avoid=setOf("shoulders"),difficulty=2,sets=2,reps=8,rest=35,safety="טווח נוח בלבד"),
        Ex("wall_press_iso","לחיצת קיר איזומטרית","חזה · ליבה","hold",setOf("strength"),setOf("strength"),avoid=setOf("shoulders"),difficulty=1,sets=2,hold=20,rest=35,safety="מאמץ בינוני, נשימה רציפה"),
        Ex("seated_march","צעידה בישיבה","ירכיים · ליבה","hold",setOf("health","energy","independence"),setOf("walk"),avoid=setOf("hips"),difficulty=1,sets=2,hold=45,rest=25,safety="לשבת על כיסא יציב"),
        Ex("chair_punch","אגרופים קדימה בישיבה","כתפיים · קצב","hold",setOf("health","energy","weight"),setOf("strength"),avoid=setOf("shoulders"),difficulty=1,sets=2,hold=40,rest=25,safety="קצב נוח, ללא נעילת מרפק"),
        Ex("standing_knee_drive","הרמת ברך בעמידה","ליבה · רגליים","reps",setOf("balance","health","strength"),setOf("walk","strength"),avoid=setOf("hips","knees"),difficulty=2,sets=2,reps=10,rest=35,safety="תמיכה זמינה"),
        Ex("back_step","צעד לאחור קצר","רגליים · יציבות","reps",setOf("balance","strength"),setOf("walk","garden"),avoid=setOf("knees","hips","ankles"),difficulty=2,sets=2,reps=8,rest=40,safety="צעד קטן ליד כיסא"),
        Ex("wall_sit_short","ישיבת קיר קצרה","ירכיים","hold",setOf("strength"),setOf("strength"),avoid=setOf("knees","hips"),difficulty=3,sets=2,hold=20,rest=50,safety="זווית גבוהה, לא לרדת עמוק"),
        Ex("pillow_squeeze","לחיצת כרית בין הברכיים","ירך פנימית · ליבה","hold",setOf("strength","independence"),setOf("strength"),avoid=setOf("hips"),difficulty=1,sets=2,hold=20,rest=30,safety="לחיצה בינונית בלבד"),
        Ex("scap_squeeze","כיווץ שכמות","גב עליון · יציבה","reps",setOf("posture","strength","pain"),setOf("mobility","strength"),avoid=setOf("upperBack"),difficulty=1,sets=2,reps=12,rest=25,safety="ללא הרמת כתפיים"),
        Ex("chin_tuck","הכנסת סנטר עדינה","צוואר · יציבה","reps",setOf("posture","mobility","pain"),setOf("mobility"),avoid=setOf("neck"),difficulty=1,sets=2,reps=8,rest=20,safety="תנועה קטנה וישרה לאחור"),
        Ex("pelvic_tilt","הטיית אגן עדינה","ליבה · גב תחתון","reps",setOf("pain","mobility","posture"),setOf("yoga","mobility"),avoid=setOf("lowerBack"),difficulty=1,sets=2,reps=8,rest=25,safety="ללא כאב או לחץ"),
        Ex("figure_four_chair","מתיחת Figure-4 בכיסא","ישבן · ירך","hold",setOf("mobility","pain"),setOf("yoga","mobility"),avoid=setOf("hips","knees"),difficulty=1,phase="cooldown",sets=1,hold=25,rest=15,safety="ללא לחץ על הברך"),
        Ex("wrist_mobility","תנועתיות שורש כף יד","ידיים · מוביליטי","reps",setOf("mobility","health"),setOf("mobility"),difficulty=1,phase="warmup",sets=1,reps=10,rest=10,safety="תנועה עדינה"),
        Ex("side_reach","הטיית צד בעמידה","צד הגוף · מוביליטי","hold",setOf("mobility","posture"),setOf("mobility","yoga"),avoid=setOf("lowerBack"),difficulty=1,phase="cooldown",sets=1,hold=20,rest=15,safety="לא לקרוס קדימה")
    )

    private val catalog = baseCatalog + LibraryExercises.additional(baseCatalog)

    internal fun needsPainChecks(questionnaire: Map<*, *>?): Boolean =
        (questionnaire?.get("painLevel")?.toString()?.toDoubleOrNull() ?: 0.0) > 0.0 ||
            (questionnaire?.get("painAreas") as? Collection<*>)?.any { it != "none" } == true ||
            questionnaire?.get("recentWorkoutPain") == true

    fun generate(questionnaire: Map<*, *>?, userSeed: String, date: LocalDate = LocalDate.now(),
                 recentExerciseIds: Set<String> = emptySet()): JSONObject {
        val plan = WorkoutSelectionEngine.select(catalog, questionnaire, userSeed, date, recentExerciseIds)
        val title = when(plan.mainGoal){
            "strength" -> "כוח פונקציונלי"
            "mobility" -> "מוביליטי וטווחים"
            "balance" -> "יציבות ושיווי משקל"
            "pain" -> "תנועה עדינה"
            else -> "כושר מאוזן"
        }
        val intensity = when(plan.maxDifficulty){1 -> "עדין"; 2 -> "בינוני"; else -> "מתקדם"}
        val exercises = JSONArray()
        plan.exercises.forEach { item ->
            val ex = item.exercise
            exercises.put(JSONObject()
                .put("id", ex.id)
                .put("n", ex.name)
                .put("m", ex.muscle)
                .put("type", ex.type)
                .put("phase", ex.phase)
                .put("sets", item.sets)
                .put("reps", item.reps)
                .put("hold", item.hold)
                .put("breaths", item.breaths)
                .put("inhale", 4)
                .put("exhale", 4)
                .put("repsText", when(ex.type){
                    "hold" -> item.hold.toString()+" שניות"
                    "breath" -> item.breaths.toString()+" נשימות"
                    else -> item.reps.toString()+" חזרות"
                })
                .put("pace", if(plan.maxDifficulty == 1) "לאט ובשליטה" else "קצב נוח ומבוקר")
                .put("rest", item.rest)
                .put("setRest", item.setRest)
                .put("avoidAreas", JSONArray(ex.avoid.toList()))
                .put("safety", ex.safety))
        }
        return JSONObject()
            .put("status", plan.status.code)
            .put("message", plan.message)
            .put("safetyReasons", JSONArray(plan.safetyReasons))
            .put("title", title)
            .put("sub", intensity+" · מותאם אישית")
            .put("week", 1)
            .put("dur", if (plan.status == WorkoutSelectionEngine.Status.READY) WorkoutSelectionEngine.estimateMinutes(plan.exercises) else plan.duration)
            .put("requestedDuration", plan.duration)
            .put("frequency", plan.frequency)
            .put("goal", plan.mainGoal)
            .put("conservative", plan.conservative)
            .put("painCheckEligible", needsPainChecks(questionnaire))
            .put("requiresProfessionalClearance", plan.status == WorkoutSelectionEngine.Status.CLEARANCE_REQUIRED)
            .put("exercises", exercises)
            .put("combinationSpace", estimateCombinationSpace(plan.eligibleCount, exercises.length()))
    }

    private fun estimateCombinationSpace(n:Int,k:Int):Long {
        if(n<=0 || k<=0 || n<k) return 0
        var r=1.0
        for(i in 1..k) r = r * (n-k+i) / i
        return r.coerceAtMost(9_000_000_000.0).toLong()
    }
}
