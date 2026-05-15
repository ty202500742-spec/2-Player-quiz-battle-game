package quizbattle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BattleUI {

    // ── Back-references ───────────────────────────────────────────────────────
    private final JPanel       host;      // the BattlePanel itself (for add/remove)
    private final AudioManager audio;
    private final SkillCallback onSkill;  // called when player clicks a skill button

    // ── Player card panels (exposed so BattlePanel can repaint the glow) ──────
    public JPanel p1Card, p2Card;

    // ── Labels / bars ─────────────────────────────────────────────────────────
    public JProgressBar p1HealthBar, p2HealthBar;
    public JLabel       p1PtsLbl,    p2PtsLbl;
    public JLabel       p1LivesLbl,  p2LivesLbl;
    public JLabel       p1ShieldLbl, p2ShieldLbl;
    public JLabel       p1DoubleLbl, p2DoubleLbl;
    public JLabel       p1TimerLbl,  p2TimerLbl;
    public JLabel       turnIndicator;
    public JLabel       questionLabel;
    public JTextArea    logArea;

    // ── Skill buttons ─────────────────────────────────────────────────────────
    public JButton btnStrike, btnShield, btnDouble, btnDrain, btnCurse, btnLethal;
    public JPanel  skillPanel;

    // ── Answer buttons ────────────────────────────────────────────────────────
    public JLabel[] answerBtns = new JLabel[3];

    // ── Top-bar buttons (exposed so BattlePanel can wire their actions) ───────
    public JButton topMenuBtn, resetBtn, pauseBtn;

    // ══════════════════════════════════════════════════════════════════════════
    // COLOUR PALETTE  (duplicated here so BattleUI is self-contained)
    // ══════════════════════════════════════════════════════════════════════════
    public final Color BG_DARK   = new Color(15,  15,  25);
    public final Color BG_CARD   = new Color(22,  22,  38);
    public final Color BG_PANEL  = new Color(18,  18,  30);
    public final Color P1_COL    = new Color(99,  179, 237);
    public final Color P2_COL    = new Color(252, 129, 129);
    public final Color GREEN_HP  = new Color(34,  197, 94);
    public final Color YELLOW_HP = new Color(234, 179, 8);
    public final Color RED_HP    = new Color(239, 68,  68);
    public final Color ACCENT    = new Color(99,  102, 241);
    public final Color TEXT_PRI  = new Color(248, 250, 252);
    public final Color TEXT_SEC  = new Color(148, 163, 184);
    public final Color BORDER    = new Color(51,  65,  85);
    public final Color SKILL_RED = new Color(239, 68,  68);
    public final Color SKILL_BLU = new Color(59,  130, 246);
    public final Color SKILL_YEL = new Color(234, 179, 8);
    public final Color SKILL_GRN = new Color(34,  197, 94);
    public final Color SKILL_PUR = new Color(168, 85,  247);
    public final Color SKILL_DRK = new Color(220, 38,  38);

    // ── Card-blink phase (driven by BattlePanel game loop) ───────────────────
    public float cardBlinkPhase = 0f;

    // ── Shared state needed for border painting ───────────────────────────────
    private int     currentTurn     = 0;
    private boolean showGameOverlay = false;
    private boolean isPaused        = false;

    // ═════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═════════════════════════════════════════════════════════════════════════
    public BattleUI(JPanel host, AudioManager audio, SkillCallback onSkill) {
        this.host    = host;
        this.audio   = audio;
        this.onSkill = onSkill;
    }

    /** Call after every resize so components are repositioned correctly. */
    public void layoutAll(int W, int H, GameOverlayPanel gameOverPanel) {
        
        // Remove all children except the game-over overlay
        Component gameOverComponent = (gameOverPanel != null) ? gameOverPanel.asComponent() : null;
        for (Component c : host.getComponents()) {
            if (c != gameOverComponent) host.remove(c);
        }
        if (W < 100 || H < 100) return;

        int pad   = 10;
        int cardW = (W - pad * 3) / 2;
        int cardH = Math.min(150, H / 6);
        int cardY = 46;

        setupTopBar(pad, 6, W, 32);
        setupCard(true,  pad,               cardY, cardW, cardH);
        setupCard(false, pad + cardW + pad,  cardY, cardW, cardH);

        int skillY = cardY + cardH + 8;
        int skillH = Math.min(80, H / 10);
        setupSkillPanel(pad, skillY, W - pad * 2, skillH);

        int questionH = 60;
        int ansH      = 55;
        int questionY = H - pad - questionH;
        int ansY      = questionY - 8 - ansH;
        setupQuestionArea(pad, questionY, W - pad * 2, questionH);
        setupAnswerButtons(pad, ansY, W - pad * 2, ansH);

        if (gameOverPanel != null) {
            host.add(gameOverPanel.asComponent());
            gameOverPanel.reposition(W, H);
            host.setComponentZOrder(gameOverPanel.asComponent(), 0);
            
            
        }
        
        host.revalidate();
        host.repaint();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TOP BAR
    // ═════════════════════════════════════════════════════════════════════════
    private void setupTopBar(int x, int y, int W, int h) {
        turnIndicator = new JLabel("", SwingConstants.CENTER);
        turnIndicator.setFont(new Font("Monospaced", Font.BOLD, 14));
        turnIndicator.setBounds(x + 60, y, W - 60 - 110 - x, h);
        host.add(turnIndicator);

        topMenuBtn = makeIconBtn("☰ MENU", x, y + 2, 56, h - 4);
        host.add(topMenuBtn);

        resetBtn = makeIconBtn("⟳ RESET", W - 70, y + 2, 60, h - 4);
        host.add(resetBtn);

        pauseBtn = makeIconBtn("II", W - 78 - 36, y + 2, 30, h - 4);
        host.add(pauseBtn);
    }

    public JButton makeIconBtn(String text, int x, int y, int w, int h) {
        JButton b = new JButton(text);
        b.setBounds(x, y, w, h);
        b.setFocusable(false);
        b.setFont(new Font("Monospaced", Font.BOLD, 10));
        b.setBackground(BG_CARD);
        b.setForeground(TEXT_SEC);
        b.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { audio.playHover(); }
        });
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PLAYER CARD
    // ═════════════════════════════════════════════════════════════════════════
    private void setupCard(boolean isP1, int x, int y, int w, int h) {
        Color playerCol = isP1 ? P1_COL : P2_COL;

        JPanel card = new JPanel(null) {
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = (isP1 && currentTurn == 0) || (!isP1 && currentTurn == 1);
                if (isActive && !showGameOverlay && !isPaused) {
                    float pulse = (float)(0.5 + 0.5 * Math.sin(cardBlinkPhase));
                    g2.setColor(playerCol);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                    Color glow = new Color(playerCol.getRed(), playerCol.getGreen(),
                                          playerCol.getBlue(), (int)(pulse * 200));
                    g2.setColor(glow);
                    g2.setStroke(new BasicStroke(4f));
                    g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                } else {
                    g2.setColor(playerCol.darker().darker());
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                }
            }
        };
        card.setBounds(x, y, w, h);
        card.setBackground(BG_CARD);
        card.setBorder(null);

        if (isP1) p1Card = card; else p2Card = card;

        int cx = 10, cy = 6;

        JLabel nameLbl = new JLabel((isP1 ? "⚔ PLAYER 1" : "⚔ PLAYER 2")
                                    + (isP1 ? "  [A / S / D]" : "  [J / K / L]"));
        nameLbl.setFont(new Font("Monospaced", Font.BOLD, 12));
        nameLbl.setForeground(playerCol);
        nameLbl.setBounds(cx, cy, w - cx * 2, 16);
        card.add(nameLbl);

        JLabel livLbl = new JLabel("❤ ❤ ❤  Lives: 3");
        livLbl.setFont(new Font("Monospaced", Font.BOLD, 11));
        livLbl.setForeground(TEXT_PRI);
        livLbl.setBounds(cx, cy + 18, w - cx * 2, 14);
        card.add(livLbl);
        if (isP1) p1LivesLbl = livLbl; else p2LivesLbl = livLbl;

        JLabel hpLbl = new JLabel("HP");
        hpLbl.setFont(new Font("Monospaced", Font.BOLD, 10));
        hpLbl.setForeground(TEXT_SEC);
        hpLbl.setBounds(cx, cy + 36, 22, 14);
        card.add(hpLbl);

        JProgressBar hp = new JProgressBar(0, 100);
        hp.setValue(100);
        hp.setForeground(GREEN_HP);
        hp.setBackground(new Color(30, 30, 50));
        hp.setBounds(cx + 24, cy + 38, w - cx * 2 - 26, 12);
        hp.setBorderPainted(false);
        hp.setStringPainted(false);
        card.add(hp);
        if (isP1) p1HealthBar = hp; else p2HealthBar = hp;

        JLabel ptsLbl = new JLabel("POINTS: 0");
        ptsLbl.setFont(new Font("Monospaced", Font.BOLD, 11));
        ptsLbl.setForeground(SKILL_YEL);
        ptsLbl.setBounds(cx, cy + 56, w / 2 - cx, 14);
        card.add(ptsLbl);
        if (isP1) p1PtsLbl = ptsLbl; else p2PtsLbl = ptsLbl;

        JLabel shLbl = new JLabel("🛡 SHIELD");
        shLbl.setFont(new Font("Monospaced", Font.BOLD, 10));
        shLbl.setForeground(SKILL_BLU);
        shLbl.setBounds(cx + w / 2 - cx, cy + 56, 80, 14);
        shLbl.setVisible(false);
        card.add(shLbl);
        if (isP1) p1ShieldLbl = shLbl; else p2ShieldLbl = shLbl;

        JLabel dblLbl = new JLabel("✨ 2X ON");
        dblLbl.setFont(new Font("Monospaced", Font.BOLD, 10));
        dblLbl.setForeground(SKILL_YEL);
        dblLbl.setBounds(cx, cy + 72, 80, 14);
        dblLbl.setVisible(false);
        card.add(dblLbl);
        if (isP1) p1DoubleLbl = dblLbl; else p2DoubleLbl = dblLbl;

        JLabel timerLbl = new JLabel("⏱ TIME: --");
        timerLbl.setFont(new Font("Monospaced", Font.BOLD, 11));
        timerLbl.setForeground(GREEN_HP);
        timerLbl.setBounds(cx, cy + 88, w - cx * 2, 14);
        card.add(timerLbl);
        if (isP1) p1TimerLbl = timerLbl; else p2TimerLbl = timerLbl;

        host.add(card);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SKILL PANEL
    // ═════════════════════════════════════════════════════════════════════════
    private void setupSkillPanel(int x, int y, int w, int h) {
        skillPanel = new JPanel(null);
        skillPanel.setBounds(x, y, w, h);
        skillPanel.setBackground(BG_PANEL);
        skillPanel.setBorder(BorderFactory.createLineBorder(BORDER, 1));

        JLabel lbl = new JLabel("  SKILLS — spend points on your turn before answering:");
        lbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lbl.setForeground(TEXT_SEC);
        lbl.setBounds(2, 2, w - 4, 16);
        skillPanel.add(lbl);

        int bCount = 6, bGap = 6;
        int bW = (w - bGap * (bCount + 1)) / bCount;
        int bH = h - 26, bY = 20;

        btnStrike = makeSkillBtn("⚡ STRIKE\n3 pts\tQ/U",  bGap + (bW + bGap) * 0, bY, bW, bH, SKILL_RED);
        btnShield = makeSkillBtn("🛡 SHIELD\n4 pts\tW/I",  bGap + (bW + bGap) * 1, bY, bW, bH, SKILL_BLU);
        btnDouble = makeSkillBtn("✨ DOUBLE\n5 pts\tE/O",  bGap + (bW + bGap) * 2, bY, bW, bH, SKILL_YEL);
        btnDrain  = makeSkillBtn("🌀 DRAIN\n6 pts\tR/P",   bGap + (bW + bGap) * 3, bY, bW, bH, SKILL_GRN);
        btnCurse  = makeSkillBtn("💀 CURSE\n7 pts\tT/[",   bGap + (bW + bGap) * 4, bY, bW, bH, SKILL_PUR);
        btnLethal = makeSkillBtn("☠ LETHAL\n15 pts\tY/]",  bGap + (bW + bGap) * 5, bY, bW, bH, SKILL_DRK);

        skillPanel.add(btnStrike); skillPanel.add(btnShield); skillPanel.add(btnDouble);
        skillPanel.add(btnDrain);  skillPanel.add(btnCurse);  skillPanel.add(btnLethal);

        btnStrike.addActionListener(e -> { audio.playClick(); onSkill.use("strike"); });
        btnShield.addActionListener(e -> { audio.playClick(); onSkill.use("shield"); });
        btnDouble.addActionListener(e -> { audio.playClick(); onSkill.use("double"); });
        btnDrain .addActionListener(e -> { audio.playClick(); onSkill.use("drain");  });
        btnCurse .addActionListener(e -> { audio.playClick(); onSkill.use("curse");  });
        btnLethal.addActionListener(e -> { audio.playClick(); onSkill.use("lethal"); });

        host.add(skillPanel);
    }

    private JButton makeSkillBtn(String text, int x, int y, int w, int h, Color col) {
        String[] parts = text.split("\n");
        String html = "<html><center>" + parts[0] + "<br><small>" + parts[1] + "</small></center></html>";
        JButton b = new JButton(html);
        b.setBounds(x, y, w, h);
        b.setFocusable(false);
        b.setBackground(new Color(col.getRed() / 4, col.getGreen() / 4, col.getBlue() / 4));
        b.setForeground(col);
        b.setFont(new Font("Monospaced", Font.BOLD, 10));
        b.setBorder(BorderFactory.createLineBorder(col, 1));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(new Color(col.getRed() / 2, col.getGreen() / 2, col.getBlue() / 2));
                    audio.playHover();
                }
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(new Color(col.getRed() / 4, col.getGreen() / 4, col.getBlue() / 4));
            }
        });
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ANSWER BUTTONS
    // ═════════════════════════════════════════════════════════════════════════
    private void setupAnswerButtons(int x, int y, int w, int h) {
        int bW = (w - 20) / 3;
        String[] keys = {"A / J", "S / K", "D / L"};
        for (int i = 0; i < 3; i++) {
            JLabel l = new JLabel(
                "<html><center><small>" + keys[i] + "</small><br>...</center></html>",
                SwingConstants.CENTER);
            l.setBounds(x + i * (bW + 10), y, bW, h);
            l.setOpaque(true);
            l.setBackground(BG_CARD);
            l.setForeground(TEXT_PRI);
            l.setFont(new Font("Monospaced", Font.BOLD, 11));
            l.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            host.add(l);
            answerBtns[i] = l;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // QUESTION AREA
    // ═════════════════════════════════════════════════════════════════════════
    private void setupQuestionArea(int x, int y, int w, int h) {
        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setBounds(x, y, w, h);
        questionLabel.setOpaque(true);
        questionLabel.setBackground(BG_CARD);
        questionLabel.setForeground(TEXT_PRI);
        questionLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        questionLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 2),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        host.add(questionLabel);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LOG
    // ═════════════════════════════════════════════════════════════════════════
    // log/clearLog kept as no-ops so BattlePanel compiles without changes
    public void log(String msg)  { /* log area removed */ }
    public void clearLog()       { /* log area removed */ }

    // ═════════════════════════════════════════════════════════════════════════
    // REFRESH UI  — call every time game state changes
    // ═════════════════════════════════════════════════════════════════════════
    public void refreshUI(GameState s) {
        if (p1HealthBar == null) return;

        this.currentTurn = s.currentTurn;

        p1HealthBar.setValue(s.p1HP); p2HealthBar.setValue(s.p2HP);
        p1HealthBar.setForeground(hpColor(s.p1HP));
        p2HealthBar.setForeground(hpColor(s.p2HP));

        p1LivesLbl.setText("❤ ".repeat(Math.max(0, s.p1Lives)).trim() + "  Lives: " + s.p1Lives);
        p2LivesLbl.setText("❤ ".repeat(Math.max(0, s.p2Lives)).trim() + "  Lives: " + s.p2Lives);

        p1PtsLbl.setText("POINTS: " + s.p1Pts);
        p2PtsLbl.setText("POINTS: " + s.p2Pts);

        p1ShieldLbl.setVisible(s.p1Shield); p2ShieldLbl.setVisible(s.p2Shield);
        p1DoubleLbl.setVisible(s.p1Double); p2DoubleLbl.setVisible(s.p2Double);

        boolean isP1Turn = s.isP1Turn();
        if (turnIndicator != null) {
            turnIndicator.setText(isP1Turn
                ? "▶ PLAYER 1's TURN  [A / S / D]"
                : "▶ PLAYER 2's TURN  [J / K / L]");
            turnIndicator.setForeground(isP1Turn ? P1_COL : P2_COL);
        }

        int activePts = s.activePts();
        if (btnStrike != null) {
            btnStrike.setEnabled(activePts >= 3);
            btnShield.setEnabled(activePts >= 4);
            btnDouble.setEnabled(activePts >= 5);
            btnDrain .setEnabled(activePts >= 6);
            btnCurse .setEnabled(activePts >= 7);
            btnLethal.setEnabled(activePts >= 15);
        }

        refreshTimerLabels(s);
    }

    public void refreshTimerLabels(GameState s) {
        if (p1TimerLbl == null || p2TimerLbl == null) return;
        boolean isP1Turn   = s.isP1Turn();
        String  activeText = "⏱ TIME: " + s.turnSecondsLeft + "s";
        Color   col        = timerColor(s.turnSecondsLeft);

        if (isP1Turn) {
            p1TimerLbl.setText(activeText); p1TimerLbl.setForeground(col);
            p2TimerLbl.setText("⏱ TIME: --"); p2TimerLbl.setForeground(TEXT_SEC);
        } else {
            p2TimerLbl.setText(activeText); p2TimerLbl.setForeground(col);
            p1TimerLbl.setText("⏱ TIME: --"); p1TimerLbl.setForeground(TEXT_SEC);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // REFRESH QUESTION
    // ═════════════════════════════════════════════════════════════════════════
    public void refreshQuestion(GameState s) {
        if (s.p1Lives <= 0 || s.p2Lives <= 0) return;
        s.currentQ = s.qm.next();
        String[] keys = {"A / J", "S / K", "D / L"};
        for (int i = 0; i < 3; i++) {
            if (answerBtns[i] != null) {
                answerBtns[i].setBackground(BG_CARD);
                answerBtns[i].setBorder(BorderFactory.createLineBorder(BORDER, 1));
            }
        }
        if (s.currentQ != null && questionLabel != null) {
            questionLabel.setText("<html><center>["
                + s.currentQ.topic.toUpperCase() + " · "
                + s.currentQ.difficulty.toUpperCase() + "]   "
                + s.currentQ.text + "</center></html>");
            for (int i = 0; i < 3; i++) {
                if (answerBtns[i] != null)
                    answerBtns[i].setText("<html><center><small style='color:#94a3b8'>"
                        + keys[i] + "</small><br>" + s.currentQ.options[i] + "</center></html>");
            }
        }
    }

    /** Highlights the chosen answer button correct (green) or wrong (red). */
    public void markAnswer(int choiceIndex, boolean correct) {
        if (answerBtns[choiceIndex] == null) return;
        answerBtns[choiceIndex].setBackground(correct
            ? new Color(34, 97, 34) : new Color(97, 34, 34));
        answerBtns[choiceIndex].setBorder(BorderFactory.createLineBorder(
            correct ? GREEN_HP : RED_HP, 2));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // OVERLAY-AWARE STATE SETTERS  (called by OverlayManager)
    // ═════════════════════════════════════════════════════════════════════════
    public void setShowGameOverlay(boolean v) { showGameOverlay = v; }
    public void setIsPaused(boolean v)        { isPaused = v; }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════
    private Color hpColor(int hp) {
        if (hp > 50) return GREEN_HP;
        if (hp > 25) return YELLOW_HP;
        return RED_HP;
    }

    private Color timerColor(int secs) {
        if (secs > 8) return GREEN_HP;
        if (secs > 4) return YELLOW_HP;
        return RED_HP;
    }

    // ── Functional interface so BattleUI doesn't depend on BattlePanel type ──
    @FunctionalInterface
    public interface SkillCallback { void use(String skill); }

    // ── Marker interface for the game-over panel (avoids circular deps) ───────
    // GameOverlayPanel must be implemented by a class that extends Component (e.g. JPanel).
    // Call asComponent() to get the Component reference for Swing operations.
    public interface GameOverlayPanel {
        void reposition(int W, int H);
        Component asComponent();
    }
}