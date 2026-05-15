package quizbattle;

import java.util.Random;

/**
 * BotPlayer — CPU logic for Computer mode.
 * Easy   = 40% correct chance
 * Medium = 65% correct chance
 * Hard   = 90% correct chance
 *
 * The bot also randomly uses skills when it has enough points.
 */
public class BotPlayer {

    public enum Difficulty { EASY, MEDIUM, HARD }

    private final Difficulty difficulty;
    private final Random     rng = new Random();

    // Delay before bot "answers" (ms) — feels more natural
    public static final int BOT_THINK_MS = 1200;

    public BotPlayer(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Difficulty getDifficulty() { return difficulty; }

    // ── Correct-answer probability ────────────────────────────────────────────
    private double correctChance() {
        return switch (difficulty) {
            case EASY   -> 0.40;
            case MEDIUM -> 0.65;
            case HARD   -> 0.90;
        };
    }

    /**
     * Returns the answer index the bot will submit (0, 1, or 2).
     * May or may not be the correct one depending on difficulty.
     */
    public int chooseAnswer(Question q) {
        if (rng.nextDouble() < correctChance()) {
            return q.correctIndex;          // answer correctly
        } else {
            // Pick a wrong answer
            int wrong = rng.nextInt(2);     // 0 or 1
            if (wrong >= q.correctIndex) wrong++;   // shift past the correct index
            return wrong;
        }
    }

    /**
     * Returns a skill name to use, or null if the bot decides not to use one.
     * The bot only acts when it's P2's turn (currentTurn == 1).
     */
    public String chooseSkill(GameState state) {
        int pts = state.p2Pts;
        if (pts < 3) return null;           // can't afford anything

        // Skill-use probability scales with difficulty
        double useChance = switch (difficulty) {
            case EASY   -> 0.20;
            case MEDIUM -> 0.45;
            case HARD   -> 0.70;
        };
        if (rng.nextDouble() > useChance) return null;

        // Prioritize based on situation
        if (pts >= 15 && difficulty == Difficulty.HARD) return "lethal";
        if (pts >= 6  && state.p2HP < 40)               return "drain";   // low HP — steal some back
        if (pts >= 4  && !state.p2Shield)                return "shield";  // protect self
        if (pts >= 3)                                     return "strike";  // default attack

        return null;
    }
}