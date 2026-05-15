package quizbattle;

/**
 * GameState — pure data + game-logic class. No Swing imports. BattlePanel reads
 * state fields and calls mutator methods.
 */
public class GameState {

    // ── Game Mode ─────────────────────────────────────────────────────────────
    public enum Mode {
        CLASSIC, BLITZ, COMPUTER
    }
    public Mode gameMode = Mode.CLASSIC;
    public String botDifficulty = "medium";   // "easy" | "medium" | "hard"

    // ── Lives / HP / Points ───────────────────────────────────────────────────
    public int p1Lives = 3, p2Lives = 3;
    public int p1HP = 100, p2HP = 100;
    public int p1Pts = 0, p2Pts = 0;

    // ── Status effects ────────────────────────────────────────────────────────
    public boolean p1Shield = false, p2Shield = false;
    public boolean p1Double = false, p2Double = false;

    // ── Turn ──────────────────────────────────────────────────────────────────
    public int currentTurn = 0;   // 0 = P1, 1 = P2 (or BOT)
    public boolean answered = false;

    // ── Turn timer ────────────────────────────────────────────────────────────
    // Classic = 10s, Blitz = 3s, Computer = 10s (bot ignores the timer)
    public static final int TICKS_PER_SECOND = 63;
    public static final int POINT_PENALTY_THRESHOLD = 3;

    public int turnSecondsLeft = 10;
    public int timerTickAcc = 0;

    // ── Question state ────────────────────────────────────────────────────────
    public QuestionManager qm = new QuestionManager();
    public Question currentQ;

    // ── Game-over ─────────────────────────────────────────────────────────────
    public boolean gameOver = false;
    public String winnerText = "";
    public java.awt.Color winnerColor;

    // ── Colour refs ───────────────────────────────────────────────────────────
    public static final java.awt.Color P1_COL = new java.awt.Color(99, 179, 237);
    public static final java.awt.Color P2_COL = new java.awt.Color(252, 129, 129);

    // ═════════════════════════════════════════════════════════════════════════
    // MODE HELPERS
    // ═════════════════════════════════════════════════════════════════════════
    public int turnSeconds() {
        return switch (gameMode) {
            case BLITZ ->
                3;
            default ->
                10;   // CLASSIC and COMPUTER both use 10s
        };
    }

    public boolean isComputerMode() {
        return gameMode == Mode.COMPUTER;
    }

    public boolean isBotTurn() {
        return isComputerMode() && currentTurn == 1;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RESET
    // ═════════════════════════════════════════════════════════════════════════
    public void reset() {
        p1Lives = 3;
        p2Lives = 3;
        p1HP = 100;
        p2HP = 100;
        p1Pts = 0;
        p2Pts = 0;
        p1Shield = false;
        p2Shield = false;
        p1Double = false;
        p2Double = false;
        currentTurn = 0;
        answered = false;
        gameOver = false;
        winnerText = "";
        winnerColor = null;
        turnSecondsLeft = turnSeconds();
        timerTickAcc = 0;
        qm.reset();
        currentQ = null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TURN TIMER
    // ═════════════════════════════════════════════════════════════════════════
    public void resetTurnTimer() {
        turnSecondsLeft = turnSeconds();
        timerTickAcc = 0;
    }

    /**
     * Called once per game-loop tick. Returns true if the timer just hit zero
     * (caller should handle timeout).
     */
    public boolean tickTurnTimer() {
        if (answered || gameOver) {
            return false;
        }
        // Bot turn — never time out; BattlePanel drives bot separately
        if (isBotTurn()) {
            return false;
        }
        timerTickAcc++;
        if (timerTickAcc >= TICKS_PER_SECOND) {
            timerTickAcc = 0;
            turnSecondsLeft--;
            if (turnSecondsLeft <= 0) {
                return true;
            }
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HANDLE TIMEOUT
    // ═════════════════════════════════════════════════════════════════════════
    public boolean handleTimeout(LogCallback log) {
        answered = true;
        boolean isP1 = (currentTurn == 0);
        log.log("⏰ " + (isP1 ? "Player 1" : "Player 2") + " ran out of time! -10 HP.");
        if (isP1) {
            p1HP -= 10;
        } else {
            p2HP -= 10;
        }
        return checkKnockouts(log);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HANDLE ANSWER INPUT
    // ═════════════════════════════════════════════════════════════════════════
    public boolean handleInput(int choice, boolean isP1, LogCallback log) {
        answered = true;
        boolean correct = (choice == currentQ.correctIndex);
        String pName = isP1 ? "Player 1" : (isComputerMode() ? "CPU" : "Player 2");

        if (correct) {
            int pts = 1;
            if (isP1 && p1Double) {
                pts = 2;
                p1Double = false;
                log.log("✨ P1 Double Points activated!");
            } else if (!isP1 && p2Double) {
                pts = 2;
                p2Double = false;
                log.log("✨ " + (isComputerMode() ? "CPU" : "P2") + " Double Points activated!");
            }
            if (isP1) {
                p1Pts += pts;
            } else {
                p2Pts += pts;
            }

            boolean blocked = false;
            if (isP1 && p2Shield) {
                p2Shield = false;
                blocked = true;
                log.log("🛡 " + (isComputerMode() ? "CPU" : "P2") + "'s Shield blocked the attack!");
            }
            if (!isP1 && p1Shield) {
                p1Shield = false;
                blocked = true;
                log.log("🛡 P1's Shield blocked the attack!");
            }
            if (!blocked) {
                if (isP1) {
                    p2HP -= 20;
                } else {
                    p1HP -= 20;
                }
            }
            log.log("✅ " + pName + " CORRECT! +" + pts + " pt(s).");
        } else {
            if (isP1) {
                p1HP -= 10;
            } else {
                p2HP -= 10;
            }
            if (isP1 && p1Pts > POINT_PENALTY_THRESHOLD) {
                p1Pts = Math.max(0, p1Pts - 1);
                log.log("⚠ P1 point penalty: -1 pt!");
            }
            if (!isP1 && p2Pts > POINT_PENALTY_THRESHOLD) {
                p2Pts = Math.max(0, p2Pts - 1);
                log.log("⚠ " + (isComputerMode() ? "CPU" : "P2") + " point penalty: -1 pt!");
            }
            log.log("❌ " + pName + " WRONG! -10 HP.");
        }

        return checkKnockouts(log);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SKILLS
    // ═════════════════════════════════════════════════════════════════════════
    public boolean useSkill(String skill, LogCallback log) {
        boolean isP1 = (currentTurn == 0);
        int pts = isP1 ? p1Pts : p2Pts;
        String pName = isP1 ? "Player 1" : (isComputerMode() ? "CPU" : "Player 2");

        int cost = skillCost(skill);
        if (pts < cost) {
            log.log("⚠ " + pName + " needs " + cost + " pts for " + skill + " (has " + pts + ").");
            return false;
        }
        if (isP1) {
            p1Pts -= cost;
        } else {
            p2Pts -= cost;
        }

        switch (skill) {
            case "strike" -> {
                boolean blocked = false;
                if (isP1 && p2Shield) {
                    p2Shield = false;
                    blocked = true;
                    log.log("🛡 " + (isComputerMode() ? "CPU" : "P2") + "'s Shield blocked Strike!");
                }
                if (!isP1 && p1Shield) {
                    p1Shield = false;
                    blocked = true;
                    log.log("🛡 P1's Shield blocked Strike!");
                }
                if (!blocked) {
                    if (isP1) {
                        p2HP -= 20;
                    } else {
                        p1HP -= 20;
                    }
                    log.log("⚡ " + pName + " used STRIKE! Opponent -20 HP.");
                }
            }
            case "shield" -> {
                if (isP1) {
                    p1Shield = true;
                } else {
                    p2Shield = true;
                }
                log.log("🛡 " + pName + " activated SHIELD!");
            }
            case "double" -> {
                if (isP1) {
                    p1Double = true;
                } else {
                    p2Double = true;
                }
                log.log("✨ " + pName + " activated DOUBLE POINTS!");
            }
            case "drain" -> {
                boolean blocked = false;
                if (isP1 && p2Shield) {
                    p2Shield = false;
                    blocked = true;
                    log.log("🛡 " + (isComputerMode() ? "CPU" : "P2") + "'s Shield blocked Drain!");
                }
                if (!isP1 && p1Shield) {
                    p1Shield = false;
                    blocked = true;
                    log.log("🛡 P1's Shield blocked Drain!");
                }
                if (!blocked) {
                    if (isP1) {
                        p2HP -= 20;
                        p1HP = Math.min(100, p1HP + 20);
                    } else {
                        p1HP -= 20;
                        p2HP = Math.min(100, p2HP + 20);
                    }
                    log.log("🌀 " + pName + " used DRAIN! Stole 20 HP.");
                }
            }
            case "curse" -> {
                if (isP1) {
                    p2Pts = Math.max(0, p2Pts - 2);
                } else {
                    p1Pts = Math.max(0, p1Pts - 2);
                }
                log.log("💀 " + pName + " cast CURSE! Opponent -2 pts.");
            }
            case "lethal" -> {
                if (isP1) {
                    p2HP = 0;
                    p2Pts = 0;
                } else {
                    p1HP = 0;
                    p1Pts = 0;
                }
                log.log("☠ " + pName + " used LETHAL BLOW! Instant KO!");
            }
        }
        return checkKnockouts(log);
    }

    public static int skillCost(String skill) {
        return switch (skill) {
            case "strike" ->
                3;
            case "shield" ->
                4;
            case "double" ->
                5;
            case "drain" ->
                6;
            case "curse" ->
                7;
            case "lethal" ->
                15;
            default ->
                999;
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    // KNOCKOUTS
    // ═════════════════════════════════════════════════════════════════════════
    public boolean checkKnockouts(LogCallback log) {
        if (p1HP <= 0) {
            p1Lives--;
            p1HP = 100;
            log.log("💥 Player 1 knocked out! Lives left: " + p1Lives);
        }
        if (p2HP <= 0) {
            p2Lives--;
            p2HP = 100;
            String p2Name = isComputerMode() ? "CPU" : "Player 2";
            log.log("💥 " + p2Name + " knocked out! Lives left: " + p2Lives);
        }
         if (p1Lives <= 0 && p2Lives <= 0) {
        answered    = true;
        gameOver    = true;
        winnerText  = "🏆  DRAW — PLAYER 1 WINS TIEBREAK!  🏆";
        winnerColor = P1_COL;
        log.log("🏆 >>> DRAW! Player 1 wins the tiebreak! <<<");
        return true;
    }
         
        if (p1Lives <= 0) {
            answered = true;
            gameOver = true;
            winnerText = isComputerMode() ? "🏆  CPU WINS!  🏆" : "🏆  PLAYER 2 WINS!  🏆";
            winnerColor = P2_COL;
            log.log("🏆 >>> " + (isComputerMode() ? "CPU" : "PLAYER 2") + " WINS THE BATTLE! <<<");
            return true;
        }
        if (p2Lives <= 0) {
            answered = true;
            gameOver = true;
            winnerText = "🏆  PLAYER 1 WINS!  🏆";
            winnerColor = P1_COL;
            log.log("🏆 >>> PLAYER 1 WINS THE BATTLE! <<<");
            return true;
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CONVENIENCE
    // ═════════════════════════════════════════════════════════════════════════
    public boolean isP1Turn() {
        return currentTurn == 0;
    }

    public int activePts() {
        return isP1Turn() ? p1Pts : p2Pts;
    }

    public void advanceTurn() {
        currentTurn = 1 - currentTurn;
    }

    @FunctionalInterface
    public interface LogCallback {

        void log(String msg);
    }
}
