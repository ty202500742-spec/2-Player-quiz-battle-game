package quizbattle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import static quizbattle.GameState.Mode.BLITZ;
import static quizbattle.GameState.Mode.COMPUTER;

public class BattlePanel extends JPanel implements ActionListener {

    // ── Controller ────────────────────────────────────────────────────────────
    private final QuizBattle controller;

    // ── Subsystems ────────────────────────────────────────────────────────────
    private AudioManager audio;
    private GameState state;
    private BattleUI ui;
    private OverlayManager overlays;
    private boolean isOnMenu = true;

    // ── Bot ───────────────────────────────────────────────────────────────────
    private BotPlayer botPlayer;
    private boolean botThinking = false;   // true while the delay timer runs
    private Timer botThinkTimer;

    // ═════════════════════════════════════════════════════════════════════════
    // SPRITES
    // ═════════════════════════════════════════════════════════════════════════
    private Image p1Idle1, p1Idle2, p1Attack, p1Hit;
    private Image p2Idle1, p2Idle2, p2Attack, p2Hit;
    private BufferedImage battleBackground;

    private static final int SPRITE_W = 130;
    private static final int SPRITE_H = 130;

    private int p1X, p2X;
    private int P1_HOME_X, P2_HOME_X;

    // ═════════════════════════════════════════════════════════════════════════
    // ANIMATION STATE
    // ═════════════════════════════════════════════════════════════════════════
    private boolean isAttacking = false;
    private boolean p1IsAttacking = false, p2IsAttacking = false;
    private int animFrame = 0;

    private int breathFrame = 0, breathTick = 0;
    private static final int BREATH_INTERVAL = 22;

    private boolean p1InHit = false, p2InHit = false;
    private int p1HitTick = 0, p2HitTick = 0;
    private static final int HIT_DURATION = 18;

    private float flashAlpha = 0f;
    private Color flashColor = Color.WHITE;

    private boolean showingTurnBanner = false;
    private float bannerAlpha = 0f;
    private String bannerText = "";
    private Color   bannerColor = Color.WHITE;
    private Timer bannerTimer;

    private boolean isSlowMotion = false;
    private int slowMotionTick = 0;
    private static final int SLOW_FACTOR = 4;

    // ═════════════════════════════════════════════════════════════════════════
    // COLOURS
    // ═════════════════════════════════════════════════════════════════════════
    private final Color BG_DARK = new Color(15, 15, 25);
    private final Color P1_COL = new Color(99, 179, 237);
    private final Color P2_COL = new Color(252, 129, 129);

    private Timer gameTimer;

    // ═════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═════════════════════════════════════════════════════════════════════════
    public BattlePanel(QuizBattle controller, AudioManager audioManager) {
        this.audio = audioManager;
        this.controller = controller;
        setLayout(null);
        setBackground(BG_DARK);
        setFocusable(true);

        p1Idle1 = loadSprite("assets/sprites/CharA1.png");
        p1Idle2 = loadSprite("assets/sprites/CharA2.png");
        p1Attack = loadSprite("assets/sprites/A_at.png");
        p1Hit = loadSprite("assets/sprites/AD.png");
        p2Idle1 = loadSprite("assets/sprites/CharB1.png");
        p2Idle2 = loadSprite("assets/sprites/CharB2.png");
        p2Attack = loadSprite("assets/sprites/B_at.png");
        p2Hit = loadSprite("assets/sprites/BD.png");

        try {
            URL bgUrl = getClass().getResource("assets/images/backgroundB.png");
            if (bgUrl != null) {
                battleBackground = ImageIO.read(bgUrl);
            } else {
                File bgFile = new File("assets/images/backgroundB.png");
                if (bgFile.exists()) {
                    battleBackground = ImageIO.read(bgFile);
                }
            }
        } catch (Exception e) {
            System.out.println("[BattlePanel] Could not load background: " + e.getMessage());
        }

        state = new GameState();

        ui = new BattleUI(this, audio, skill -> useSkill(skill));
        overlays = new OverlayManager(this, audio, ui, controller);

        gameTimer = new Timer(16, this);
        gameTimer.start();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutAll();
                overlays.repositionGameOver(getWidth(), getHeight());
            }

            @Override
            public void componentShown(ComponentEvent e) {
                layoutAll();
                requestFocusInWindow();
            }
        });

        setupKeyBindings();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═════════════════════════════════════════════════════════════════════════
    public void setDifficulty(String diff) {
        state.qm.setDifficulty(diff);
    }

    public void setGameMode(GameState.Mode mode, String botDiff) {
        state.gameMode = mode;
        state.botDifficulty = botDiff;

        if (mode == GameState.Mode.COMPUTER) {
            BotPlayer.Difficulty bd = switch (botDiff) {
                case "easy" ->
                    BotPlayer.Difficulty.EASY;
                case "hard" ->
                    BotPlayer.Difficulty.HARD;
                default ->
                    BotPlayer.Difficulty.MEDIUM;
            };
            botPlayer = new BotPlayer(bd);
        } else {
            botPlayer = null;
        }
    }

    public void resetGame() {
        isOnMenu = false;
        if (botThinkTimer != null) {
            botThinkTimer.stop();
            botThinkTimer = null;
        }
        botThinking = false;

        overlays.dismissAll();
        state.reset();

        if (getWidth() > 0) {
            p1X = P1_HOME_X;
            p2X = P2_HOME_X;
        }
        isAttacking = false;
        p1IsAttacking = false;
        p2IsAttacking = false;
        animFrame = 0;
        breathFrame = 0;
        breathTick = 0;
        p1InHit = false;
        p1HitTick = 0;
        p2InHit = false;
        p2HitTick = 0;
        isSlowMotion = false;
        slowMotionTick = 0;
        flashAlpha = 0f;
        ui.cardBlinkPhase = 0f;

        audio.reinit();
        if (audio != null) {
            audio.pauseVictoryMusic();
        }
        audio.startBattleMusic();

        ui.clearLog();
        layoutAll();
        ui.refreshUI(state);
        ui.refreshQuestion(state);
        state.answered = false; 
        state.resetTurnTimer();
        ui.refreshTimerLabels(state);

        String modeLabel = switch (state.gameMode) {
            case BLITZ ->
                "⚡ BLITZ MODE — 3 seconds!";
            case COMPUTER ->
                "🤖 VS CPU (" + state.botDifficulty.toUpperCase() + ")";
            default ->
                "Battle started! P1: A/S/D  |  P2: J/K/L";
        };
        ui.log(modeLabel);
        showTurnBanner("PLAYER 1 GOES FIRST!", P1_COL);

        revalidate();
        repaint();
        requestFocusInWindow();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ═════════════════════════════════════════════════════════════════════════
    private void layoutAll() {
        int W = getWidth(), H = getHeight();
        if (W < 100 || H < 100) {
            return;
        }

        P1_HOME_X = W / 4 - SPRITE_W / 2;
        P2_HOME_X = W * 3 / 4 - SPRITE_W / 2;
        p1X = P1_HOME_X;
        p2X = P2_HOME_X;

        ui.layoutAll(W, H, null);

        if (ui.topMenuBtn != null) {
            for (ActionListener al : ui.topMenuBtn.getActionListeners()) {
                ui.topMenuBtn.removeActionListener(al);
            }
            ui.topMenuBtn.addActionListener(e -> {
                audio.playClick();
                showMenu();
            });
        }
        if (ui.resetBtn != null) {
            for (ActionListener al : ui.resetBtn.getActionListeners()) {
                ui.resetBtn.removeActionListener(al);
            }
            ui.resetBtn.addActionListener(e -> {
                audio.playClick();
                showRestartConfirm();
            });
        }
        if (ui.pauseBtn != null) {
            for (ActionListener al : ui.pauseBtn.getActionListeners()) {
                ui.pauseBtn.removeActionListener(al);
            }
            ui.pauseBtn.addActionListener(e -> {
                audio.playClick();
                overlays.showPauseOverlay();
            });
        }

        if (state.currentQ == null) {
            state.qm.setDifficulty("easy");
            ui.refreshQuestion(state);
            ui.refreshUI(state);
        } else {
            ui.refreshQuestion(state);
            ui.refreshUI(state);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // KEY BINDINGS  — P2 keys disabled during bot turn
    // ═════════════════════════════════════════════════════════════════════════
    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        
        // Answer Keys
        char[] p1Answ = {'a', 's', 'd'};
        char[] p2Answ = {'j', 'k', 'l'};
        
        // Dedicated Skill Clusters
        String[] skillNames = {"strike", "shield", "double", "drain", "curse", "lethal"};
        char[] p1Skills = {'q', 'w', 'e', 'r', 't', 'y'};
        char[] p2Skills = {'u', 'i', 'o', 'p', '[', ']'};

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            im.put(KeyStroke.getKeyStroke(p1Answ[i]), "p1_ans_" + i);
            am.put("p1_ans_" + i, new AbstractAction() {
                public void actionPerformed(ActionEvent e) { handleInput(idx, true); }
            });
            
            im.put(KeyStroke.getKeyStroke(p2Answ[i]), "p2_ans_" + i);
            am.put("p2_ans_" + i, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    if (!state.isBotTurn()) handleInput(idx, false);
                }
            });
        }

        // Map Player 1 Skills (Q-Y)
        for (int i = 0; i < p1Skills.length; i++) {
            final String skill = skillNames[i];
            im.put(KeyStroke.getKeyStroke(p1Skills[i]), "p1_skill_" + skill);
            am.put("p1_skill_" + skill, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    if (state.isP1Turn()) useSkill(skill);
                }
            });
        }

        // Map Player 2 Skills (U-J)
        for (int i = 0; i < p2Skills.length; i++) {
            final String skill = skillNames[i];
            im.put(KeyStroke.getKeyStroke(p2Skills[i]), "p2_skill_" + skill);
            am.put("p2_skill_" + skill, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    if (!state.isBotTurn() && !state.isP1Turn()) useSkill(skill);
                }
            });
        }

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "restart");
        am.put("restart", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (overlays.isGameOverShowing()) resetGame();
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // INPUT HANDLING
    // ═════════════════════════════════════════════════════════════════════════
    
    private void handleInput(int choice, boolean isP1) {
        if (overlays.isPaused() || isAttacking || state.currentQ == null || state.answered) {
            return;
            
        }
        if (botThinking) {
            return;
        }
        if (isP1 && state.currentTurn != 0) {
            ui.log("⚠ It's Player 2's turn! (J/K/L)");
            return;
        }
        if (!isP1 && state.currentTurn != 1) {
            ui.log("⚠ It's Player 1's turn! (A/S/D)");
            return;
        }

        boolean correct = (choice == state.currentQ.correctIndex);
        ui.markAnswer(choice, correct);

        if (correct) {
            boolean willBeBlocked = (isP1 && state.p2Shield) || (!isP1 && state.p1Shield);
            if (!willBeBlocked) {
                triggerAttackAnim(isP1);
                triggerHit(!isP1);
                flashScreen(new Color(100, 20, 20));
            }
        } else {
            triggerHit(isP1);
            flashScreen(new Color(80, 80, 200));
            audio.playWrong();
        }
        state.answered = true; 
        boolean gameOver = state.handleInput(choice, isP1, ui::log);
        ui.refreshUI(state);

        if (gameOver) {
            triggerGameOverSequence();
        } else {
            afterAnswer();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BOT TURN
    // ═════════════════════════════════════════════════════════════════════════
    private void scheduleBotTurn() {
        if (botPlayer == null || !state.isBotTurn() || botThinking || state.answered) {
            return;
        }
        botThinking = true;
        ui.log("🤖 CPU is thinking...");

        botThinkTimer = new Timer(BotPlayer.BOT_THINK_MS, e -> {
            ((Timer) e.getSource()).stop();
            botThinkTimer = null;
            if (state.isBotTurn() && !state.answered && !overlays.isGameOverShowing()) {
                executeBotTurn();
            }
            botThinking = false;
        });
        botThinkTimer.setRepeats(false);
        botThinkTimer.start();
    }

    private void executeBotTurn() {
        if (state.currentQ == null) {
            return;
        }

        // Bot may use a skill first
        String skill = botPlayer.chooseSkill(state);
        if (skill != null) {
            boolean willAttack = skill.equals("strike") || skill.equals("drain") || skill.equals("lethal");
            if (willAttack) {
                boolean blocked = state.p1Shield;
                if (!blocked || skill.equals("lethal")) {
                    triggerAttackAnim(false);
                    triggerHit(true);
                }
            }
            boolean gameOver = state.useSkill(skill, ui::log);
            ui.refreshUI(state);
            if (gameOver) {
                triggerGameOverSequence();
                return;
            }
        }

        // Bot answers
        int choice = botPlayer.chooseAnswer(state.currentQ);
        boolean correct = (choice == state.currentQ.correctIndex);
        ui.markAnswer(choice, correct);

        if (correct) {
            boolean blocked = state.p1Shield;
            if (!blocked) {
                triggerAttackAnim(false);
                triggerHit(true);
                flashScreen(new Color(100, 20, 20));
            }
        } else {
            triggerHit(false);
            flashScreen(new Color(80, 80, 200));
            audio.playWrong();
        }

        boolean gameOver = state.handleInput(choice, false, ui::log);
        ui.refreshUI(state);

        if (gameOver) {
            triggerGameOverSequence();
        } else {
            afterAnswer();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SKILLS
    // ═════════════════════════════════════════════════════════════════════════
    private void useSkill(String skill) {
        if (overlays.isPaused() || state.answered || isAttacking || botThinking) {
            return;
        }
        if (state.isBotTurn()) {
            return;
        }
        boolean isP1 = state.isP1Turn();

        switch (skill) {
            case "shield" ->
                audio.playShield();
            case "strike" ->
                audio.playStrike();
            case "double" ->
                audio.playDouble();
            case "drain" ->
                audio.playDrain();
            case "curse" ->
                audio.playCurse();
            case "lethal" ->
                audio.playLethal();
        }

        boolean willAttack = skill.equals("strike") || skill.equals("drain") || skill.equals("lethal");
        if (willAttack) {
            boolean willBeBlocked = (isP1 && state.p2Shield) || (!isP1 && state.p1Shield);
            if (!willBeBlocked || skill.equals("lethal")) {
                triggerAttackAnim(isP1);
                triggerHit(!isP1);
            }
        }

        boolean gameOver = state.useSkill(skill, ui::log);
        ui.refreshUI(state);
        if (gameOver) {
            triggerGameOverSequence();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TURN TIMEOUT
    // ═════════════════════════════════════════════════════════════════════════
    private void onTurnTimeout() {
        if (isOnMenu) {
            return;
        }
        if (overlays.isGameOverShowing()) {
            return;
        }
        boolean isP1 = state.isP1Turn();
        triggerHit(isP1);
        flashScreen(new Color(80, 80, 200));
        audio.playWrong();

        boolean gameOver = state.handleTimeout(ui::log);
        ui.refreshUI(state);

        if (gameOver) {
            triggerGameOverSequence();
        } else {
            afterAnswer();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // AFTER-ANSWER
    // ═════════════════════════════════════════════════════════════════════════
    private void afterAnswer() {
        state.advanceTurn();
        ui.cardBlinkPhase = 0f;

        boolean nextIsBotTurn = state.isBotTurn();
        String bannerMsg = nextIsBotTurn ? "CPU's TURN"
                : (state.isP1Turn() ? "PLAYER 1's TURN" : "PLAYER 2's TURN");
        Color bannerCol = state.isP1Turn() ? P1_COL : P2_COL;
        showTurnBanner(bannerMsg, bannerCol);

        Timer delay = new Timer(700, ev -> {
            ui.refreshQuestion(state);
            state.answered = false;
            state.resetTurnTimer();
            ui.refreshUI(state);

            // Kick off bot if it's now the bot's turn
            if (state.isBotTurn()) {
                scheduleBotTurn();
            }
        });
        delay.setRepeats(false);
        delay.start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GAME-OVER SEQUENCE
    // ═════════════════════════════════════════════════════════════════════════
    private void triggerGameOverSequence() {
        isSlowMotion = true;
        slowMotionTick = 0;
        audio.pauseBattleMusic();
        audio.playGameOver();

        Timer seq = new Timer(1400, e -> {
            isSlowMotion = false;
            audio.startVictoryMusic();
            overlays.showGameOver(state.winnerText, state.winnerColor, this::resetGame);
        });
        seq.setRepeats(false);
        seq.start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // OVERLAY LAUNCHERS
    // ═════════════════════════════════════════════════════════════════════════
    private void showMenu() {
        overlays.showMenuConfirm(() -> {
            audio.pauseBattleMusic();
            audio.pauseVictoryMusic();
            state.reset();
            controller.showPanel("MENU");
        });
    }

    private void showRestartConfirm() {
        overlays.showRestartConfirm(this::resetGame);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ANIMATION HELPERS
    // ═════════════════════════════════════════════════════════════════════════
    private void triggerAttackAnim(boolean p1Attacks) {
        isAttacking = true;
        p1IsAttacking = p1Attacks;
        p2IsAttacking = !p1Attacks;
        animFrame = 0;
        audio.playAttack();
    }

    private void triggerHit(boolean hitP1) {
        if (hitP1) {
            p1InHit = true;
            p1HitTick = 0;
        } else {
            p2InHit = true;
            p2HitTick = 0;
        }
    }

    private void flashScreen(Color col) {
        flashColor = col;
        flashAlpha = 0.5f;
    }

    private void showTurnBanner(String text, Color col) {
        bannerText = text;
         bannerColor = col;
        bannerAlpha = 1f;
        showingTurnBanner = true;
        if (bannerTimer != null) {
            bannerTimer.stop();
        }
        bannerTimer = new Timer(30, e -> {
            bannerAlpha -= 0.04f;
            if (bannerAlpha <= 0) {
                bannerAlpha = 0;
                showingTurnBanner = false;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        bannerTimer.start();
    }

    private void finishAttack() {
        isAttacking = false;
        p1IsAttacking = false;
        p2IsAttacking = false;
        animFrame = 0;
        p1X = P1_HOME_X;
        p2X = P2_HOME_X;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SPRITE FRAME SELECTION
    // ═════════════════════════════════════════════════════════════════════════
    private Image getFrame(boolean isP1) {
        if (isP1) {
            if (p1InHit) {
                return p1Hit != null ? p1Hit : p1Idle1;
            }
            if (isAttacking && p1IsAttacking) {
                return p1Attack != null ? p1Attack : p1Idle1;
            }
            return breathFrame == 0 ? p1Idle1 : (p1Idle2 != null ? p1Idle2 : p1Idle1);
        } else {
            if (p2InHit) {
                return p2Hit != null ? p2Hit : p2Idle1;
            }
            if (isAttacking && p2IsAttacking) {
                return p2Attack != null ? p2Attack : p2Idle1;
            }
            return breathFrame == 0 ? p2Idle1 : (p2Idle2 != null ? p2Idle2 : p2Idle1);
        }
    }

    public void pauseTimers() {
        isOnMenu = true;
        state.resetTurnTimer();
        if (botThinkTimer != null) {
            botThinkTimer.stop();
            botThinkTimer = null;
        }
        botThinking = false;
        state.answered = true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GAME LOOP
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isSlowMotion) {
            slowMotionTick++;
            if (flashAlpha > 0) {
                flashAlpha = Math.max(0f, flashAlpha - 0.01f);
            }
            repaint();
            if (slowMotionTick % SLOW_FACTOR != 0) {
                return;
            }
        }

        overlays.tickOverlayPulse();

        if (!overlays.isPaused() && !isOnMenu) {
            if (!overlays.isGameOverShowing() && !isSlowMotion) {
                if (state.tickTurnTimer()) {
                    onTurnTimeout();
                }
                ui.refreshTimerLabels(state);
            }

            if (!overlays.isGameOverShowing()) {
                ui.cardBlinkPhase += 0.08f;
                if (ui.cardBlinkPhase > Math.PI * 2) {
                    ui.cardBlinkPhase -= (float) (Math.PI * 2);
                }
                if (ui.p1Card != null) {
                    ui.p1Card.repaint();
                }
                if (ui.p2Card != null) {
                    ui.p2Card.repaint();
                }
            }

            if (isAttacking) {
                animFrame++;
                if (p1IsAttacking) {
                    int target = P2_HOME_X - SPRITE_W - 20;
                    if (animFrame < 10) {
                        p1X = P1_HOME_X + (target - P1_HOME_X) * animFrame / 10;
                    } else if (animFrame < 20) {
                        p1X = P1_HOME_X + (target - P1_HOME_X) * (20 - animFrame) / 10;
                    } else {
                        finishAttack();
                    }
                } else if (p2IsAttacking) {
                    int target = P1_HOME_X + SPRITE_W + 20;
                    if (animFrame < 10) {
                        p2X = P2_HOME_X - (P2_HOME_X - target) * animFrame / 10;
                    } else if (animFrame < 20) {
                        p2X = P2_HOME_X - (P2_HOME_X - target) * (20 - animFrame) / 10;
                    } else {
                        finishAttack();
                    }
                }
            }

            breathTick++;
            if (breathTick >= BREATH_INTERVAL) {
                breathTick = 0;
                breathFrame = 1 - breathFrame;
            }

            if (p1InHit) {
                p1HitTick++;
                if (p1HitTick >= HIT_DURATION) {
                    p1InHit = false;
                    p1HitTick = 0;
                }
            }
            if (p2InHit) {
                p2HitTick++;
                if (p2HitTick >= HIT_DURATION) {
                    p2InHit = false;
                    p2HitTick = 0;
                }
            }
        }

        if (flashAlpha > 0) {
            flashAlpha = Math.max(0f, flashAlpha - 0.05f);
        }
        if (!overlays.isGameOverShowing()) {
            repaint();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PAINTING
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int W = getWidth(), H = getHeight();

        if (battleBackground != null) {
            g2.drawImage(battleBackground, 0, 0, W, H, null);
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(15, 15, 30), W, H, new Color(10, 10, 20)));
            g2.fillRect(0, 0, W, H);
        }

        int floorY = H - 180;
        g2.setColor(new Color(30, 30, 50, 180));
        g2.fillRoundRect(W / 8, floorY, W * 6 / 8, 8, 8, 8);

        int spriteY = floorY - SPRITE_H - 5;
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(p1X, floorY - 20, SPRITE_W, 18);
        g2.fillOval(p2X, floorY - 20, SPRITE_W, 18);

        Image f1 = getFrame(true);
        if (f1 != null) {
            g2.drawImage(f1, p1X, spriteY, SPRITE_W, SPRITE_H, null);
        } else {
            g2.setColor(P1_COL);
            g2.fillRoundRect(p1X + 5, spriteY, SPRITE_W - 10, SPRITE_H, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 30));
            g2.drawString("P1", p1X + 22, spriteY + SPRITE_H / 2 + 12);
        }

        Image f2 = getFrame(false);
        if (f2 != null) {
            g2.drawImage(f2, p2X + SPRITE_W, spriteY, -SPRITE_W, SPRITE_H, null);
        } else {
            g2.setColor(P2_COL);
            g2.fillRoundRect(p2X + 5, spriteY, SPRITE_W - 10, SPRITE_H, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 30));
            // Show "CPU" label in computer mode
            String p2Label = state.isComputerMode() ? "CPU" : "P2";
            g2.drawString(p2Label, p2X + 22, spriteY + SPRITE_H / 2 + 12);
        }

        if (flashAlpha > 0.01f) {
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(),
                    flashColor.getBlue(), (int) (flashAlpha * 200)));
            g2.fillRect(0, 0, W, H);
        }

        if (showingTurnBanner && bannerAlpha > 0) {
            int bH = 60, bY = H / 2 - bH / 2;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bannerAlpha));
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRoundRect(W / 4, bY, W / 2, bH, 16, 16);
           g2.setColor(bannerColor);  
            g2.setFont(new Font("Monospaced", Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(bannerText, (W - fm.stringWidth(bannerText)) / 2, bY + bH / 2 + 8);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // Mode badge (top-center, subtle)
        if (state.gameMode != GameState.Mode.CLASSIC) {
            String badge = state.gameMode == GameState.Mode.BLITZ ? "⚡ BLITZ"
                    : "🤖 CPU — " + state.botDifficulty.toUpperCase();
            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2.setColor(new Color(255, 255, 255, 120));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(badge, (W - fm.stringWidth(badge)) / 2, 18);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MINIM HOST HELPERS
    // ═════════════════════════════════════════════════════════════════════════
    public String sketchPath(String filename) {
        return audio != null ? audio.sketchPath(filename) : System.getProperty("user.dir");
    }

    public java.io.InputStream createInput(String filename) {
        return audio != null ? audio.createInput(filename) : null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SPRITE LOADER
    // ═════════════════════════════════════════════════════════════════════════
    private Image loadSprite(String name) {
        try {
            URL url = getClass().getResource("/" + name);
            if (url != null) {
                return ImageIO.read(url);
            }
            File f = new File(name);
            if (f.exists()) {
                return ImageIO.read(f);
            }
            File f2 = new File("../", name);
            if (f2.exists()) {
                return ImageIO.read(f2);
            }
        } catch (Exception e) {
            System.err.println("[BattlePanel] Could not load sprite: " + name);
        }
        return null;
    }
}
