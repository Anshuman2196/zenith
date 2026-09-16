package com.zenith.launcher.ui.home

/**
 * Centralized user-facing language. Keep UI copy here so Zenith can evolve its tone, add
 * experiments, and rotate larger text libraries without changing individual widgets.
 */
object LauncherCopy {
    val greetings = listOf(
        "Welcome back, %s",
        "One step at a time, %s",
        "You’re here. Begin with what matters, %s",
        "Make this moment count, %s",
        "Small actions become patterns. Start one, %s"
    )

    val focusMode = listOf(
        "Protect your attention. Let the important things stay loud.",
        "Your attention is yours. Decide what gets it.",
        "Less noise. More of the person you’re trying to become.",
        "Stay with the work long enough for focus to become familiar."
    )

    val focusModeOff = listOf(
        "Your attention is yours. Choose what deserves it.",
        "The world is available. You don’t have to give it your attention.",
        "Keep the useful things close. Let everything else wait."
    )

    val distractionPause = listOf(
        "Notice the urge before you follow it.",
        "Is this what you meant to do right now?",
        "Let the impulse pass through before you act.",
        "You do not have to answer every urge immediately.",
        "Stay with the choice for a moment.",
        "What deserves your attention right now?"
    )

    val pomodoroStop = listOf(
        "You’re stopping. Give the urge seven seconds before you decide what comes next.",
        "Before you leave the work, notice why you want to leave it.",
        "Seven seconds. Let the decision be deliberate, not automatic.",
        "You can stop. First, give your attention one quiet moment."
    )

    val pomodoroComplete = listOf(
        "You finished the block. Let that win register before you move on.",
        "The work is done. Notice the feeling of following through.",
        "You kept the promise you made to yourself. Let it land.",
        "One completed block becomes evidence: you can return and do it again."
    )

    val pomodoroTransition = listOf(
        "Breathe. Notice the transition. Choose the next block deliberately.",
        "Let the last block end before the next one begins.",
        "Rest is part of the rhythm. Return when you’re ready."
    )

    val emptyTasks = listOf(
        "Nothing here yet. Add one small thing and give your attention somewhere to land.",
        "Start with one task. Momentum usually follows movement.",
        "This space is empty on purpose. Give it one meaningful next step."
    )

    val emptyChapters = listOf(
        "Nothing here yet. Add what you want your future self to stop carrying in your head.",
        "Your backlog starts with one honest item. Add the next thing worth returning to.",
        "Leave the mental clutter here. Add a topic and make it visible."
    )

    val emptyPdfs = listOf(
        "Your library is empty. Add something worth returning to.",
        "Keep the knowledge you use close. Add your first resource.",
        "A useful library starts with one resource. Add what you’ll want within reach."
    )

    val emptyMilestones = listOf(
        "No targets yet. Choose something worth moving toward.",
        "Give your effort a direction. Add one target you can act on.",
        "Your next target starts as a decision. Put it here."
    )

    val emptyShortcuts = listOf(
        "No shortcuts yet. Keep only what genuinely helps you move.",
        "Make the useful things easier to reach. Add a shortcut.",
        "Your space is quiet. Give the tools that matter a place here."
    )

    val emptyDeadlines = listOf(
        "Nothing due yet. Add a deadline when something deserves a date.",
        "No deadlines here yet. Make the important dates visible before they become urgent.",
        "Keep future pressure visible, not buried. Add a deadline when you have one."
    )
}
