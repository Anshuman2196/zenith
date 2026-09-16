package com.zenith.launcher.ui.home

/**
 * Centralized user-facing language. Keep UI copy here so Zenith can evolve its tone, add
 * experiments, and rotate larger text libraries without changing individual widgets.
 *
 * The language is intentionally reflective rather than directive: it creates a small pause
 * around habitual actions without telling the user what they should choose.
 */
object ZenithCopy {
    val greetings = listOf(
        "A new day is already underway.",
        "Today has not decided itself yet.",
        "The next choice is still yours.",
        "You are here now.",
        "Begin where you are.",
        "Nothing needs to be solved all at once.",
        "Today is not asking for perfection.",
        "There is still room to begin.",
        "The day is moving. You can move with it.",
        "Some things deserve your attention.",
        "Not everything requires a response.",
        "Your attention is valuable.",
        "The next hour has not been spent yet.",
        "Small choices become familiar paths.",
        "This moment is enough to start.",
        "Every day teaches something.",
        "Where attention goes, habits follow.",
        "You do not need to rush into the day.",
        "The next step is usually smaller than it seems.",
        "The day is still open."
    )

    // Shown while Focus Mode is active on the toggle card.
    val focusMode = listOf(
        "Your attention is yours. Notice where it goes.",
        "Less noise. More room for what matters to you.",
        "Stay with what you chose for a while.",
        "Attention follows intention."
    )

    // A five-second awareness window before Focus Mode begins.
    val focusModeEntry = listOf(
        "What deserves your attention right now?",
        "Choose one thing.",
        "Where will your attention go?",
        "The next few moments belong to you.",
        "Begin intentionally."
    )

    val focusModeOff = listOf(
        "Your attention is yours. Notice what you choose.",
        "The world is available. You do not have to follow everything that appears.",
        "What deserves your attention now?"
    )

    val distractionPause = listOf(
        "Notice the urge before you follow it.",
        "What brought you here?",
        "You have a choice here.",
        "What are you looking for?",
        "Is this where you want your attention?",
        "Notice the impulse.",
        "The next tap is still yours.",
        "What are you hoping to find?",
        "Awareness comes before action.",
        "Pause. Then choose.",
        "Notice what pulled you here.",
        "Take one second before continuing."
    )

    val pomodoroStop = listOf(
        "You are stopping. Give the decision seven seconds to become conscious.",
        "Before you leave the work, notice why you want to leave it.",
        "Seven seconds. Notice the choice before you make it.",
        "You can stop. First, give your attention one quiet moment."
    )

    val pomodoroComplete = listOf(
        "A session has ended. What happened during this time?",
        "Notice what it felt like to follow through.",
        "What stayed with you?",
        "What did you learn from this session?",
        "What comes next?"
    )

    val pomodoroTransition = listOf(
        "Breathe. Notice the transition. Choose the next block deliberately.",
        "Let the last block end before the next one begins.",
        "Rest is part of the rhythm. Notice when you are ready to return."
    )

    val emptyTasks = listOf(
        "Nothing has been chosen yet. What deserves attention today?",
        "Start with one thing.",
        "One item is enough.",
        "The next step can be small.",
        "Attention starts with a choice."
    )

    val emptyBacklog = listOf(
        "Nothing is waiting here.",
        "The mental shelf is clear.",
        "There is room to think.",
        "No loose ends right now.",
        "Nothing is asking for attention here.",
        "This space is empty for now.",
        "The backlog is quiet.",
        "Leave the clutter here when it appears.",
        "Not everything needs carrying.",
        "Your mind can rest from remembering this."
    )

    val emptyLibrary = listOf(
        "Your library is quiet for now.",
        "Add something worth returning to.",
        "Keep the knowledge you use close.",
        "A useful library starts with one resource."
    )

    val emptyTargets = listOf(
        "Nothing is set here yet.",
        "Choose something worth moving toward.",
        "Give your effort a direction.",
        "Your next target starts as a decision."
    )

    val emptyShortcuts = listOf(
        "No shortcuts yet.",
        "Keep only what genuinely helps you move.",
        "Make the useful things easier to reach.",
        "Give the tools that matter a place here."
    )

    val emptyDeadlines = listOf(
        "Nothing is due yet.",
        "Add a deadline when something deserves a date.",
        "Keep important dates visible before they become urgent.",
        "Let future pressure stay visible, not buried."
    )
}
