package com.recoverwell.core

import com.recoverwell.core.logic.Ask
import com.recoverwell.core.model.Profile
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * The offline Recovery coach must route the way people actually phrase
 * questions: whole-word (no "PE" inside "speed", no "car" inside "cardio"),
 * safety-first, and about the real topic ("take the boot off to sleep" is a
 * sleep question; "drive to work" is a driving one).
 */
class AskCoachTest {

    private val injury = LocalDate.of(2026, 6, 2)
    private fun at(phase: Int, weeks: Long): Pair<Profile, LocalDate> =
        Fixtures.profile().copy(injuryDate = injury, physioConfirmedPhase = phase) to injury.plusWeeks(weeks)

    private val early = at(1, 1)
    private val mid = at(3, 9)
    private val late = at(5, 30)

    private fun ask(q: String, ctx: Pair<Profile, LocalDate> = early) = Ask.answer(q, ctx.first, ctx.second)

    private fun assertTitle(q: String, expected: String, ctx: Pair<Profile, LocalDate> = early) {
        val a = ask(q, ctx)
        assertTrue("\"$q\" -> \"${a.title}\" (expected \"$expected\")", a.title.contains(expected, ignoreCase = true))
    }

    @Test
    fun movementQuestionsRouteToTheirCheck() {
        assertTitle("Can I drive yet?", "drive")
        assertTitle("Can I drive in my boot?", "drive")
        assertTitle("Am I allowed to drive?", "drive")
        assertTitle("Can I drive to work?", "drive")
        assertTitle("When can I walk without the boot?", "walk without the boot")
        assertTitle("Can I take my boot off?", "walk without the boot")
        assertTitle("Can I do heel raises?", "heel raises")
        assertTitle("When can I start calf raises?", "heel raises")
        assertTitle("Can I stretch my calf?", "stretch")
        assertTitle("When can I run?", "run")
        assertTitle("Can I play padel?", "padel")
        assertTitle("When can I get back on court?", "padel")
        assertTitle("Can I swim?", "swim")
        assertTitle("Can I ride a bike?", "bike")
    }

    @Test
    fun everydayQuestionsRouteToTheirFaq() {
        assertTitle("Can I take the boot off to sleep?", "sleep")
        assertTitle("Can I sleep without the boot?", "sleep")
        assertTitle("Can I have a shower?", "shower")
        assertTitle("How do I get upstairs?", "stairs")
        assertTitle("When can I stop using crutches?", "crutches")
        assertTitle("Can I fly to Spain on holiday?", "fly")
        assertTitle("When can I go back to work?", "work")
        assertTitle("Can I take ibuprofen?", "painkillers")
        assertTitle("How long am I on the blood thinner?", "blood thinner")
        assertTitle("My ankle is swollen, is that normal?", "swelling")
        assertTitle("Is swelling after walking normal?", "swelling", mid)
        assertTitle("There's a lump on my tendon", "lump", mid)
        assertTitle("What type of shoes should I wear?", "shoes")
        assertTitle("Do I need a heel lift in my shoes?", "shoes", mid)
        assertTitle("Can I go to the gym?", "gym")
        assertTitle("Can I do cardio?", "gym")
        assertTitle("Can I do cardio in the boot?", "gym")
        assertTitle("How much pain is OK when exercising?", "pain is ok", mid)
        assertTitle("I'm scared of re-rupturing it", "re-rupturing")
        assertTitle("I tripped on the stairs", "fallen")
    }

    @Test
    fun symptomsAlwaysWinAndOfferTheRightCall() {
        val chest = ask("I have chest pain and feel breathless")
        assertEquals("999", chest.dial)
        assertEquals(Ask.Action.OPEN_RED_FLAGS, chest.action)
        val calf = ask("My calf is hot and painful")
        assertEquals("111", calf.dial)
        val pop = ask("I heard a pop at the back of my heel")
        assertEquals(Ask.DIAL_CLINIC, pop.dial)
        assertTrue(pop.title.contains("re-rupture", ignoreCase = true))
        val head = ask("I fell and hit my head")
        assertEquals("111", head.dial)
        assertTrue(head.body.contains("A&E"))
        assertEquals(Ask.Action.OPEN_RED_FLAGS, ask("What are the red flags?").action)
        assertEquals(Ask.Action.OPEN_RED_FLAGS, ask("I'm worried about a clot").action)
    }

    @Test
    fun wordsInsideOtherWordsNeverTrigger() {
        // "pe" inside speed/type/open/slope/pedal once sent these to the red flags
        for (q in listOf("Is it safe to speed up my wedges?", "What type of shoes?", "Can I pop to the shops?",
            "Can I walk on a slope?", "Can I do chest press at the gym?")) {
            assertNotEquals("\"$q\" wrongly routed to red flags", Ask.Action.OPEN_RED_FLAGS, ask(q, mid).action)
        }
        // "car" inside cardio/care once sent these to driving
        assertFalse(ask("Can I do cardio?").title.contains("drive", ignoreCase = true))
    }

    @Test
    fun genericAndDeviceQuestions() {
        assertTitle("What can I do right now?", "Right now")
        assertTitle("What am I allowed to do?", "Right now")
        assertTitle("What's next?", "What's next")
        assertTitle("What are the next steps?", "What's next")
        assertTitle("What angle should my boot be?", "boot")
        assertTitle("When do I change my wedges?", "boot")
        assertTrue(ask("xyzzy").title.contains("help"))
    }

    @Test
    fun answersArePhaseAware() {
        val showerEarly = ask("Can I shower?", early).body
        val showerLate = ask("Can I shower?", late).body
        assertTrue(showerEarly.contains("foot pointed down"))
        assertNotEquals(showerEarly, showerLate)
        assertTrue(ask("Can I drive yet?", early).body.startsWith("Not yet"))
        assertTrue(ask("Can I drive yet?", late).body.startsWith("Yes"))
    }

    @Test
    fun movementAnswersHaveCleanPunctuation() {
        for (q in listOf("Can I drive yet?", "Can I walk without the boot?", "Can I stretch my calf?")) {
            for (ctx in listOf(early, late)) assertFalse(q, ask(q, ctx).body.contains(".."))
        }
    }

    @Test
    fun everySuggestedQuestionRoundTripsToARealAnswer() {
        for (ctx in listOf(early, at(2, 4), mid, at(4, 16), late)) {
            val topics = Ask.topics(ctx.first, ctx.second)
            assertTrue(topics.size >= 4)
            for (topic in topics) for (q in topic.questions) {
                val a = ask(q, ctx)
                assertFalse("\"$q\" fell through to the fallback", a.title.contains("I can help"))
            }
        }
    }

    @Test
    fun faqsResolveTheChosenSport() {
        val running = Fixtures.profile().copy(injuryDate = injury, physioConfirmedPhase = 5, sportId = "running")
        val q = Ask.topics(running, injury.plusWeeks(30)).flatMap { it.questions }
        assertTrue(q.any { it.contains("running", ignoreCase = true) })
        assertTrue(q.none { it.contains("{sport}", ignoreCase = true) })
    }
}
