package com.zenith.launcher.ui.home
import androidx.compose.runtime.getValue

/**
 * Centralized user-facing language. Keep UI copy here so Zenith can evolve its tone, add
 * experiments, and rotate larger text libraries without changing individual widgets.
 *
 * The language is intentionally reflective rather than directive: it creates a small pause
 * around habitual actions without telling the user what they should choose.
 */
object ZenithCopy {
    val greetings = listOf(
        "A new day is already underway, %s.",
        "Today has not decided itself yet, %s.",
        "The next choice is still yours, %s.",
        "You are here now, %s.",
        "Begin where you are, %s.",
        "Nothing needs to be solved all at once, %s.",
        "Today is not asking for perfection, %s.",
        "There is still room to begin, %s.",
        "The day is moving. You can move with it, %s.",
        "Some things deserve your attention, %s.",
        "Not everything requires a response, %s.",
        "The next hour has not been spent yet, %s.",
        "Small choices become familiar paths, %s.",
        "This moment is enough to start, %s.",
        "Every day teaches something, %s.",
        "Where attention goes, habits follow, %s.",
        "You do not need to rush into the day, %s.",
        "The next step is usually smaller than it seems, %s.",
        "The day is still open, %s.",
        "Some choices happen quietly, %s.",
        "The morning does not need to be won, %s.",
        "You can begin again at any point, %s.",
        "A day is built one decision at a time, %s.",
        "Not every thought deserves your attention, %s.",
        "The day has already started. You are joining it, %s.",
        "You do not have to answer everything today, %s.",
        "Attention is always going somewhere, %s.",
        "What you repeat becomes familiar, %s.",
        "The next moment has not happened yet, %s.",
        "Today is another chance to notice, %s."
    )

    val focusMode = listOf(
        "Your attention is yours. Notice where it goes.",
        "Less noise. More room for what matters to you.",
        "Stay with what you chose for a while.",
        "Attention follows intention."
    )

    val focusModeEntry = listOf(
        "What deserves your attention right now?",
        "Choose one thing.",
        "Let the rest wait.",
        "Be here for a while.",
        "Where will your attention go?",
        "Stay with what matters.",
        "One thing at a time.",
        "Attention follows intention.",
        "This time belongs somewhere.",
        "What are you giving your attention to?",
        "Everything else can wait a little.",
        "Bring your attention back.",
        "Be deliberate.",
        "Settle into the task.",
        "Choose before the world chooses for you.",
        "Not everything needs your attention.",
        "Make this time count for something.",
        "Stay with the thing you chose.",
        "The next few moments belong to you.",
        "Begin intentionally.",
        "Give your attention a place to rest.",
        "Choose what matters for now.",
        "This is where your attention lives next.",
        "For a while, let one thing be enough.",
        "Attention grows where it stays."
    )

    val focusModeOff = listOf(
        "Your attention is yours. Notice what you choose.",
        "The world is available. You do not have to follow everything that appears.",
        "What deserves your attention now?"
    )

    val focusExit = listOf(
        "What happened during that time?",
        "Was your attention where you wanted it?",
        "How do you feel about that session?",
        "What deserves your attention next?",
        "Did this move you forward?",
        "What changed while you were here?",
        "How would you describe that session?",
        "What did you notice?",
        "Was this time spent the way you intended?",
        "What feels different now?",
        "Did your attention stay where you placed it?",
        "What did you learn from this session?",
        "Where did your attention go?",
        "What comes next?",
        "How did that feel?",
        "Was this the use of time you wanted?",
        "What was worth your attention?",
        "What stayed with you?",
        "What did you discover?",
        "What are you taking from this session?",
        "What became clearer?",
        "Did your intention survive the session?",
        "How would you spend the next hour?",
        "What did this time become?",
        "What matters now?"
    )

    val distractionPause = listOf(
        "Notice the urge before you follow it.",
        "What brought you here?",
        "You have a choice here.",
        "Proceed intentionally.",
        "Pause before continuing.",
        "This moment does not need to be automatic.",
        "Take a moment.",
        "What are you looking for?",
        "Is this where you want your attention?",
        "Choose consciously.",
        "The next tap is still yours.",
        "Notice the impulse.",
        "Be aware of the decision.",
        "You do not have to answer every urge immediately.",
        "What are you hoping to find?",
        "Awareness comes before action.",
        "Pause. Then choose.",
        "This can be intentional.",
        "Notice what pulled you here.",
        "Take one second before continuing.",
        "The choice has not been made yet.",
        "A pause is still a choice.",
        "What are you saying yes to?",
        "What happens if you wait a moment?",
        "This urge does not need an immediate answer.",
        "Look at the impulse before following it.",
        "You can continue. You can also reconsider.",
        "Notice the path your attention is taking.",
        "Before the next tap, notice the reason.",
        "The moment is still yours."
    )

    val pomodoroStop = listOf(
        "Before you leave the work, notice why you want to leave it.",
        "What is pulling your attention away?",
        "You are stopping. Give the decision a moment to become conscious.",
        "What changed between starting and now?",
        "Do you want to stop, or do you want relief?",
        "Notice the feeling before the action.",
        "What is asking for your attention right now?",
        "This choice deserves a moment.",
        "Take a breath of awareness before continuing.",
        "What are you moving toward?",
        "What are you moving away from?",
        "Pause before ending the session.",
        "The session can end. First, notice why.",
        "What is making this choice feel right?",
        "Observe the urge before acting on it.",
        "This is a transition. Notice it.",
        "The timer is still running. What changed?",
        "What does stopping solve right now?",
        "What are you hoping happens next?",
        "Give the decision your attention.",
        "A conscious stop is different from an automatic one.",
        "The urge arrived. Notice it first.",
        "What would happen if you stayed one more minute?",
        "This moment deserves awareness.",
        "Whatever you choose, choose consciously."
    )

    val pomodoroComplete = listOf(
        "A session has ended.",
        "What happened during this time?",
        "Did this move things forward?",
        "What deserves your attention next?",
        "How do you feel about that session?",
        "What did you accomplish here?",
        "What stayed with you?",
        "Take a moment before the next thing.",
        "What was valuable about this session?",
        "Was your attention where you wanted it?",
        "The session is over. Reflect briefly.",
        "What did you learn?",
        "How would you describe this session?",
        "Something changed during that time.",
        "What comes next?",
        "What became clearer?",
        "Was this time spent intentionally?",
        "What are you carrying forward?",
        "The timer ended. The reflection begins.",
        "How did this session feel?",
        "What was worth your attention?",
        "What did this block become?",
        "What are you taking from this work?",
        "A session ends. Awareness remains.",
        "Notice what this time gave you."
    )

    val pomodoroBreak = listOf(
        "The work can rest for a moment.",
        "Step away intentionally.",
        "A pause has its own purpose.",
        "Rest is part of the rhythm.",
        "Let your attention breathe.",
        "You do not need to fill every moment.",
        "The session ended. The pause begins.",
        "Take the break you earned.",
        "Be present here too.",
        "A few quiet minutes matter.",
        "Recovery is part of progress.",
        "Let the mind settle.",
        "This moment belongs to rest.",
        "The pause is not empty.",
        "Allow yourself to step back.",
        "The next session can wait a moment.",
        "Not every minute must be productive.",
        "Rest consciously.",
        "A break is still part of the process.",
        "Be where you are now."
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
        "Your mind can rest from remembering this.",
        "There is nothing to keep track of here.",
        "The burden of remembering is light today.",
        "A clear backlog creates space.",
        "Nothing is competing for attention here.",
        "This space is ready when you need it."
    )

    val emptyTodo = listOf(
        "Nothing has been chosen yet.",
        "What deserves attention today?",
        "Start with one thing.",
        "One item is enough.",
        "The next step can be small.",
        "Choose something worth doing.",
        "A list begins with a single item.",
        "Nothing is written here yet.",
        "Begin anywhere.",
        "Attention starts with a choice.",
        "The first task is often the hardest to choose.",
        "One clear step can change the day.",
        "What would make today easier?",
        "What deserves a place on this list?",
        "A blank list is an open invitation."
    )

    val targetsCopy = listOf(
        "Direction matters more than speed.",
        "An aim gives effort somewhere to go.",
        "Targets are reminders, not judgments.",
        "Progress rarely moves in a straight line.",
        "A target is a direction, not a verdict.",
        "What matters is movement, not perfection.",
        "Keep your eyes on the direction.",
        "Growth often arrives quietly.",
        "The target remains. Today is just one day.",
        "A result is information, not identity.",
        "Attention today shapes outcomes tomorrow.",
        "Stay connected to the direction you chose.",
        "Small improvements still count.",
        "The path matters as much as the destination.",
        "A target simply points the way."
    )

    // Existing non-psychological empty-state copy remains centralized here.
    val emptyLibrary = listOf(
        "Your library is quiet for now.",
        "Add something worth returning to.",
        "Keep the knowledge you use close.",
        "A useful library starts with one resource."
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

    // Compatibility aliases used by existing widget code.
    val emptyTasks get() = emptyTodo
    val emptyTargets get() = targetsCopy
    val pomodoroTransition get() = pomodoroBreak
}
