/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package quizbattle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import ddf.minim.*;

/**
 *
 * @author End-User
 */

public class BattlePanel extends JPanel implements ActionListener {
    private QuizBattle controller;
    

    // AUDIO

    private Minim       audioLoader;
    private AudioPlayer bgMusic;
    private AudioPlayer sfxHover;
    private AudioPlayer sfxClick;
    private AudioPlayer sfxAttack;


    // GAME STATE

    private int p1Lives = 3, p2Lives = 3;
    private int p1HP = 100, p2HP = 100;
    private int p1Pts = 0, p2Pts = 0;
    private static final int POINT_PENALTY_THRESHOLD = 3;
    private int currentTurn = 0;          // 0 = P1, 1 = P2
    private boolean p1Shield = false, p2Shield = false;
    private boolean p1Double = false, p2Double = false;
    private boolean isPaused = false, isAttacking = false;
    private boolean p1IsAttacking = false, p2IsAttacking = false;
    private int     animFrame  = 0;
    private float   flashAlpha = 0f;
    private Color   flashColor = Color.WHITE;


    // SPRITES  —  4 frames per player

    private Image p1Idle1, p1Idle2, p1Attack, p1Hit;
    private Image p2Idle1, p2Idle2, p2Attack, p2Hit;

    private BufferedImage battleBackground;

    private int p1X, p2X;
    private int P1_HOME_X, P2_HOME_X;

    // Sprite render size 
    private static final int SPRITE_W = 180;
    private static final int SPRITE_H = 180;

    // Breathing animation 
    private int breathFrame    = 0;
    private int breathTick     = 0;
    private static final int BREATH_INTERVAL = 22;

    // Hit-flash state 
    private boolean p1InHit = false, p2InHit = false;
    private int     p1HitTick = 0,   p2HitTick = 0;
    private static final int HIT_DURATION = 18;


    // QUESTION STATE

    private QuestionManager qm = new QuestionManager();
    private Question currentQ;
    private boolean answered = false;


    // TURN-TRANSITION BANNER

    private boolean showingTurnBanner = false;
    private float   bannerAlpha       = 0f;
    private String  bannerText        = "";
    private Timer   bannerTimer;


    // GAME-OVER OVERLAY

    private boolean         showGameOverlay  = false;
    private String          winnerText       = "";
    private float           overlayAlpha     = 0f;
    private float           overlayPulse     = 0f;
    private Timer           overlayTimer;
    private JButton         restartBtn;
    private JPanel          gameOverPanel;
    private BufferedImage   frozenBackground = null;


    // MENU-CONFIRM OVERLAY

    private JPanel menuConfirmPanel = null;
    private float  confirmAlpha     = 0f;
    private Timer  confirmTimer;


    // COLOUR PALETTE

    private final Color BG_DARK   = new Color(15,  15,  25);   // [CHANGE] Main dark background fallback
    private final Color BG_CARD   = new Color(22,  22,  38);   // [CHANGE] Card/panel background
    private final Color BG_PANEL  = new Color(18,  18,  30);   // [CHANGE] Skill panel background
    private final Color P1_COL    = new Color(99,  179, 237);  // [CHANGE] Player 1 accent color (blue)
    private final Color P2_COL    = new Color(252, 129, 129);  // [CHANGE] Player 2 accent color (red)
    private final Color GREEN_HP  = new Color(34,  197, 94);   // [CHANGE] HP bar color when full
    private final Color YELLOW_HP = new Color(234, 179, 8);    // [CHANGE] HP bar color at mid health
    private final Color RED_HP    = new Color(239, 68,  68);   // [CHANGE] HP bar color when low
    private final Color ACCENT    = new Color(99,  102, 241);  // [CHANGE] General accent/highlight color
    private final Color TEXT_PRI  = new Color(248, 250, 252);  // [CHANGE] Primary text color
    private final Color TEXT_SEC  = new Color(148, 163, 184);  // [CHANGE] Secondary/dim text color
    private final Color BORDER    = new Color(51,  65,  85);   // [CHANGE] Border/divider color
    private final Color SKILL_RED = new Color(239, 68,  68);   // [CHANGE] Strike skill color
    private final Color SKILL_BLU = new Color(59,  130, 246);  // [CHANGE] Shield skill color
    private final Color SKILL_YEL = new Color(234, 179, 8);    // [CHANGE] Double skill color
    private final Color SKILL_GRN = new Color(34,  197, 94);   // [CHANGE] Drain skill color
    private final Color SKILL_PUR = new Color(168, 85,  247);  // [CHANGE] Curse skill color
    private final Color SKILL_DRK = new Color(220, 38,  38);   // [CHANGE] Lethal skill color


    // UI COMPONENTS

    private JLabel        questionLabel;
    private JProgressBar  p1HealthBar, p2HealthBar;
    private JLabel[]      answerBtns  = new JLabel[3];
    private JLabel        p1PtsLbl, p2PtsLbl, p1LivesLbl, p2LivesLbl;
    private JLabel        p1ShieldLbl, p2ShieldLbl, p1DoubleLbl, p2DoubleLbl;
    private JLabel        turnIndicator;
    private JTextArea     logArea;
    private JPanel        skillPanel;
    private JButton btnStrike, btnShield, btnDouble, btnDrain, btnCurse, btnLethal;
    private Timer   gameTimer;
    private JButton topMenuBtn;


    // CONSTRUCTOR

    public BattlePanel(QuizBattle controller) {
        this.controller = controller;
        setLayout(null);
        setBackground(BG_DARK);
        setFocusable(true);

        // Load all 8 sprite frames
        p1Idle1  = loadSprite("BreP1.png");
        p1Idle2  = loadSprite("BreP2.png");
        p1Attack = loadSprite("AttackP1.png");
        p1Hit    = loadSprite("DamP1.png");
        p2Idle1  = loadSprite("BreP11.png");
        p2Idle2  = loadSprite("BreP22.png");
        p2Attack = loadSprite("AttackP2.png");
        p2Hit    = loadSprite("DamP2.png");

        try {
            URL bgUrl = getClass().getResource("ba.png");
            if (bgUrl != null) {
                battleBackground = ImageIO.read(bgUrl);
            } else {
                File bgFile = new File("backgroundB.png");
                if (bgFile.exists()) battleBackground = ImageIO.read(bgFile);
            }
            if (battleBackground == null)
                System.out.println("[BG] battleBG.png not found — using gradient fallback.");
        } catch (Exception e) {
            System.out.println("[BG] Could not load battleBG.png: " + e.getMessage());
        }

        initAudio();
        gameTimer = new Timer(16, this);
        gameTimer.start();

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                layoutAll();
                if (showGameOverlay) repositionOverlay();
            }
            @Override public void componentShown(ComponentEvent e) {
                layoutAll();
                requestFocusInWindow();
            }
        });

        setupKeyBindings();
    }


    // SPRITE ANIMATION — frame selector

    private Image getFrame(boolean isP1) {
        if (isP1) {
            if (p1InHit)
                return p1Hit    != null ? p1Hit    : (p1Idle1 != null ? p1Idle1 : p1Idle2);
            if (isAttacking && p1IsAttacking)
                return p1Attack != null ? p1Attack : (p1Idle1 != null ? p1Idle1 : p1Idle2);
            if (breathFrame == 0) return p1Idle1 != null ? p1Idle1 : p1Idle2;
            else                  return p1Idle2 != null ? p1Idle2 : p1Idle1;
        } else {
            if (p2InHit)
                return p2Hit    != null ? p2Hit    : (p2Idle1 != null ? p2Idle1 : p2Idle2);
            if (isAttacking && p2IsAttacking)
                return p2Attack != null ? p2Attack : (p2Idle1 != null ? p2Idle1 : p2Idle2);
            if (breathFrame == 0) return p2Idle1 != null ? p2Idle1 : p2Idle2;
            else                  return p2Idle2 != null ? p2Idle2 : p2Idle1;
        }
    }

    // MINIM HELPERS
    
    public String sketchPath(String filename) {
        URL url = getClass().getResource("/" + filename);
        if (url != null) {
            try { return new File(url.toURI()).getParent(); }
            catch (Exception ignored) {}
        }
        return System.getProperty("user.dir");
    }

    public InputStream createInput(String filename) {
        InputStream is = getClass().getResourceAsStream("/" + filename);
        if (is != null) return is;
        try {
            File f = new File(filename);
            if (!f.exists()) f = new File(System.getProperty("user.dir"), filename);
            if (f.exists()) return new java.io.FileInputStream(f);
        } catch (Exception ignored) {}
        System.err.println("[Minim] Could not locate audio file: " + filename);
        return null;
    }

    // AUDIO

    private void initAudio() {
        try {
            audioLoader = new Minim(this);
            bgMusic   = audioLoader.loadFile("MusicBattle.mp3");
            sfxHover  = audioLoader.loadFile("Hover.mp3");
            sfxClick  = audioLoader.loadFile("Click.mp3");
            sfxAttack = audioLoader.loadFile("Attack.mp3");
        } catch (Exception e) {
            System.err.println("[Audio] Failed to initialise: " + e.getMessage());
        }
    }

    private void startBattleMusic() {
        if (bgMusic != null) {
            bgMusic.rewind();
            bgMusic.loop();
        } else {
            System.err.println("[Audio] MusicBattle.mp3 not found – running without music.");
        }
    }

    private void stopAllAudio() {
        try {
            if (bgMusic   != null) { bgMusic.pause();   bgMusic.close();   bgMusic   = null; }
            if (sfxHover  != null) { sfxHover.close();  sfxHover  = null; }
            if (sfxClick  != null) { sfxClick.close();  sfxClick  = null; }
            if (sfxAttack != null) { sfxAttack.close(); sfxAttack = null; }
            if (audioLoader != null) { audioLoader.stop(); audioLoader = null; }
        } catch (Exception e) {
            System.err.println("[Audio] Error releasing audio: " + e.getMessage());
        }
    }

    private void playHover()  { if (sfxHover  != null) { sfxHover.rewind();  sfxHover.play();  } }
    private void playClick()  { if (sfxClick  != null) { sfxClick.rewind();  sfxClick.play();  } }
    private void playAttack() { if (sfxAttack != null) { sfxAttack.rewind(); sfxAttack.play(); } }


    // LAYOUT

    private void layoutAll() {
        for (Component c : getComponents()) {
            if (c != gameOverPanel) remove(c);
        }
        int W = getWidth(), H = getHeight();
        if (W < 100 || H < 100) return;

        int pad   = 10;
        int cardW = (W - pad * 3) / 2;
        int cardH = Math.min(130, H / 6);
        int cardY = 46;

        setupTopBar(pad, 6, W, 32);
        setupCard(true,  pad,              cardY, cardW, cardH);
        setupCard(false, pad + cardW + pad, cardY, cardW, cardH);

        int skillY = cardY + cardH + 8;
        int skillH = Math.min(80, H / 10);
        setupSkillPanel(pad, skillY, W - pad * 2, skillH);

        int ansY = skillY + skillH + 8;
        int ansH = 50;
        setupAnswerButtons(pad, ansY, W - pad * 2, ansH);

        int questionY = H - 120 - pad;
        int logY      = H - 52  - pad;

        P1_HOME_X = W / 4 - SPRITE_W / 2;
        P2_HOME_X = W * 3 / 4 - SPRITE_W / 2;
        p1X = P1_HOME_X;
        p2X = P2_HOME_X;

        setupQuestionArea(pad, questionY, W - pad * 2, 60);
        setupLog(pad, logY, W - pad * 2, 44);

        if (gameOverPanel != null) {
            add(gameOverPanel);
            repositionOverlay();
            setComponentZOrder(gameOverPanel, 0);
        }

        revalidate();
        repaint();

        if (qm != null && currentQ == null) {
            qm.setDifficulty("easy");
            refreshQuestion();
            refreshUI();
        } else if (currentQ != null) {
            refreshQuestion();
            refreshUI();
        }
    }

    // UI SETUP HELPERS
    
    private Image loadSprite(String name) {
        try {
            URL url = getClass().getResource("/" + name);
            if (url != null) return ImageIO.read(url);
            File f = new File(name);
            if (f.exists()) return ImageIO.read(f);
            File f2 = new File("../", name);
            if (f2.exists()) return ImageIO.read(f2);
        } catch (Exception e) {
            System.err.println("Could not load sprite: " + name);
        }
        return null;
    }

    public void setDifficulty(String diff) { qm.setDifficulty(diff); }

    private void setupTopBar(int x, int y, int W, int h) {
        turnIndicator = new JLabel("", SwingConstants.CENTER);
        turnIndicator.setFont(new Font("Monospaced", Font.BOLD, 14));
        turnIndicator.setBounds(x + 60, y, W - 60 - 110 - x, h);
        add(turnIndicator);

        topMenuBtn = makeIconBtn("☰ MENU", x, y + 2, 56, h - 4);
        topMenuBtn.addActionListener(e -> { playClick(); showMenuConfirm(); });
        add(topMenuBtn);

        JButton resetBtn = makeIconBtn("⟳ RESET", W - 70, y + 2, 60, h - 4);
        resetBtn.addActionListener(e -> { playClick(); resetGame(); });
        add(resetBtn);

        JButton pauseBtn = makeIconBtn("⏸", W - 78 - 36, y + 2, 30, h - 4);
        pauseBtn.addActionListener(e -> { playClick(); isPaused = !isPaused; });
        add(pauseBtn);
    }

    private JButton makeIconBtn(String text, int x, int y, int w, int h) {
        JButton b = new JButton(text);
        b.setBounds(x, y, w, h);
        b.setFocusable(false);
        b.setFont(new Font("Monospaced", Font.BOLD, 10));
        b.setBackground(BG_CARD);
        b.setForeground(TEXT_SEC);
        b.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { playHover(); }
        });
        return b;
    }

    private void setupCard(boolean isP1, int x, int y, int w, int h) {
        JPanel card = new JPanel(null);
        card.setBounds(x, y, w, h);
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isP1 ? P1_COL : P2_COL, 2),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        int cx = 10, cy = 6;

        JLabel nameLbl = new JLabel((isP1 ? "⚔ PLAYER 1" : "⚔ PLAYER 2")
                                    + (isP1 ? "  [A / S / D]" : "  [J / K / L]"));
        nameLbl.setFont(new Font("Monospaced", Font.BOLD, 12));
        nameLbl.setForeground(isP1 ? P1_COL : P2_COL);
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

        add(card);
    }

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

        btnStrike = makeSkillBtn("⚡ STRIKE\n3 pts",  bGap + (bW + bGap) * 0, bY, bW, bH, SKILL_RED);
        btnShield = makeSkillBtn("🛡 SHIELD\n4 pts",  bGap + (bW + bGap) * 1, bY, bW, bH, SKILL_BLU);
        btnDouble = makeSkillBtn("✨ DOUBLE\n5 pts",  bGap + (bW + bGap) * 2, bY, bW, bH, SKILL_YEL);
        btnDrain  = makeSkillBtn("🌀 DRAIN\n6 pts",   bGap + (bW + bGap) * 3, bY, bW, bH, SKILL_GRN);
        btnCurse  = makeSkillBtn("💀 CURSE\n7 pts",   bGap + (bW + bGap) * 4, bY, bW, bH, SKILL_PUR);
        btnLethal = makeSkillBtn("☠ LETHAL\n15 pts", bGap + (bW + bGap) * 5, bY, bW, bH, SKILL_DRK);

        skillPanel.add(btnStrike); skillPanel.add(btnShield); skillPanel.add(btnDouble);
        skillPanel.add(btnDrain);  skillPanel.add(btnCurse);  skillPanel.add(btnLethal);

        btnStrike.addActionListener(e -> { playClick(); useSkill("strike"); });
        btnShield.addActionListener(e -> { playClick(); useSkill("shield"); });
        btnDouble.addActionListener(e -> { playClick(); useSkill("double"); });
        btnDrain .addActionListener(e -> { playClick(); useSkill("drain");  });
        btnCurse .addActionListener(e -> { playClick(); useSkill("curse");  });
        btnLethal.addActionListener(e -> { playClick(); useSkill("lethal"); });

        add(skillPanel);
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
                    playHover();
                }
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(new Color(col.getRed() / 4, col.getGreen() / 4, col.getBlue() / 4));
            }
        });
        return b;
    }

    private void setupAnswerButtons(int x, int y, int w, int h) {
        int bW = (w - 20) / 3;
        String[] keys = {"A / J", "S / K", "D / L"};
        for (int i = 0; i < 3; i++) {
            JLabel l = new JLabel("<html><center><small>" + keys[i] + "</small><br>...</center></html>",
                                  SwingConstants.CENTER);
            l.setBounds(x + i * (bW + 10), y, bW, h);
            l.setOpaque(true);
            l.setBackground(BG_CARD);
            l.setForeground(TEXT_PRI);
            l.setFont(new Font("Monospaced", Font.BOLD, 11));
            l.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            add(l);
            answerBtns[i] = l;
        }
    }

    private void setupQuestionArea(int x, int y, int w, int h) {
        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setBounds(x, y, w, h);
        questionLabel.setOpaque(true);
        questionLabel.setBackground(BG_CARD);
        questionLabel.setForeground(TEXT_PRI);
        questionLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        questionLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 2),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        add(questionLabel);
    }

    private void setupLog(int x, int y, int w, int h) {
        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        logArea.setBackground(new Color(10, 10, 18));
        logArea.setForeground(TEXT_SEC);
        logArea.setEditable(false);
        logArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBounds(x, y, w, h);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scroll);
    }

    private void log(String msg) {
        if (logArea == null) return;
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }


    // KEY BINDINGS

    private void setupKeyBindings() {
        InputMap  im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        char[] p1Keys = {'a', 's', 'd'};
        char[] p2Keys = {'j', 'k', 'l'};

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            im.put(KeyStroke.getKeyStroke(p1Keys[i]), "p1_" + i);
            am.put("p1_" + i, new AbstractAction() {
                public void actionPerformed(ActionEvent e) { handleInput(idx, true); }
            });
            im.put(KeyStroke.getKeyStroke(p2Keys[i]), "p2_" + i);
            am.put("p2_" + i, new AbstractAction() {
                public void actionPerformed(ActionEvent e) { handleInput(idx, false); }
            });
        }

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "restart");
        am.put("restart", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (showGameOverlay) resetGame();
            }
        });
    }


    // GAME LOGIC — input handling
 
    private void handleInput(int choice, boolean isP1) {
        if (isPaused || isAttacking || currentQ == null || answered) return;
        if ( isP1 && currentTurn != 0) { log("⚠ It's Player 2's turn! (J/K/L)"); return; }
        if (!isP1 && currentTurn != 1) { log("⚠ It's Player 1's turn! (A/S/D)"); return; }

        answered = true;
        boolean correct = (choice == currentQ.correctIndex);
        String pName = isP1 ? "Player 1" : "Player 2";

        if (answerBtns[choice] != null) {
            answerBtns[choice].setBackground(correct ? new Color(34, 97, 34) : new Color(97, 34, 34));
            answerBtns[choice].setBorder(
                BorderFactory.createLineBorder(correct ? GREEN_HP : RED_HP, 2));
        }

        if (correct) {
            int pts = 1;
            if ( isP1 && p1Double) { pts = 2; p1Double = false; log("✨ P1 Double Points activated!"); }
            if (!isP1 && p2Double) { pts = 2; p2Double = false; log("✨ P2 Double Points activated!"); }
            if (isP1) p1Pts += pts; else p2Pts += pts;

            boolean blocked = false;
            if ( isP1 && p2Shield) { p2Shield = false; blocked = true; log("🛡 P2's Shield blocked the attack!"); }
            if (!isP1 && p1Shield) { p1Shield = false; blocked = true; log("🛡 P1's Shield blocked the attack!"); }

            if (!blocked) {
                if (isP1) {
                    p2HP -= 20;
                    triggerAttackAnim(true);
                    triggerHit(false);
                    flashScreen(new Color(100, 20, 20));
                } else {
                    p1HP -= 20;
                    triggerAttackAnim(false);
                    triggerHit(true);
                    flashScreen(new Color(100, 20, 20));
                }
            }
            log("✅ " + pName + " CORRECT! +" + pts + " pt(s).");
        } else {
            if (isP1) {
                p1HP -= 10;
                triggerHit(true);
                flashScreen(new Color(80, 80, 200));
            } else {
                p2HP -= 10;
                triggerHit(false);
                flashScreen(new Color(80, 80, 200));
            }
            if ( isP1 && p1Pts > POINT_PENALTY_THRESHOLD) { p1Pts = Math.max(0, p1Pts - 1); log("⚠ P1 point penalty: -1 pt!"); }
            if (!isP1 && p2Pts > POINT_PENALTY_THRESHOLD) { p2Pts = Math.max(0, p2Pts - 1); log("⚠ P2 point penalty: -1 pt!"); }
            log("❌ " + pName + " WRONG! -10 HP.");
        }

        boolean gameOver = checkKnockouts();
        refreshUI();

        if (!gameOver) {
            currentTurn = 1 - currentTurn;
            showTurnBanner(
                currentTurn == 0 ? "PLAYER 1's TURN" : "PLAYER 2's TURN",
                currentTurn == 0 ? P1_COL : P2_COL);
            Timer delay = new Timer(700, ev -> {
                refreshQuestion();
                answered = false;
                refreshUI();
            });
            delay.setRepeats(false);
            delay.start();
        }
    }

 
    // GAME LOGIC — skills
 
    private void useSkill(String skill) {
        if (isPaused || answered || isAttacking) return;
        boolean isP1  = (currentTurn == 0);
        int     pts   = isP1 ? p1Pts : p2Pts;
        String  pName = isP1 ? "Player 1" : "Player 2";

        int cost = switch (skill) {
            case "strike" -> 3;  case "shield" -> 4;  case "double" -> 5;
            case "drain"  -> 6;  case "curse"  -> 7;  case "lethal" -> 15;
            default -> 999;
        };

        if (pts < cost) {
            log("⚠ " + pName + " needs " + cost + " pts for " + skill + " (has " + pts + ").");
            return;
        }
        if (isP1) p1Pts -= cost; else p2Pts -= cost;

        switch (skill) {
            case "strike" -> {
                boolean blocked = false;
                if ( isP1 && p2Shield) { p2Shield = false; blocked = true; log("🛡 P2's Shield blocked Strike!"); }
                if (!isP1 && p1Shield) { p1Shield = false; blocked = true; log("🛡 P1's Shield blocked Strike!"); }
                if (!blocked) {
                    if (isP1) { p2HP -= 20; triggerAttackAnim(true);  triggerHit(false); }
                    else       { p1HP -= 20; triggerAttackAnim(false); triggerHit(true);  }
                    log("⚡ " + pName + " used STRIKE! Opponent -20 HP.");
                }
            }
            case "shield" -> {
                if (isP1) p1Shield = true; else p2Shield = true;
                log("🛡 " + pName + " activated SHIELD!");
            }
            case "double" -> {
                if (isP1) p1Double = true; else p2Double = true;
                log("✨ " + pName + " activated DOUBLE POINTS!");
            }
            case "drain" -> {
                if (isP1) {
                    p2HP -= 20; p1HP = Math.min(100, p1HP + 20);
                    triggerAttackAnim(true); triggerHit(false);
                } else {
                    p1HP -= 20; p2HP = Math.min(100, p2HP + 20);
                    triggerAttackAnim(false); triggerHit(true);
                }
                log("🌀 " + pName + " used DRAIN! Stole 20 HP.");
            }
            case "curse" -> {
                if (isP1) p2Pts = Math.max(0, p2Pts - 2);
                else       p1Pts = Math.max(0, p1Pts - 2);
                log("💀 " + pName + " cast CURSE! Opponent -2 pts.");
            }
            case "lethal" -> {
                if (isP1) { p2HP = 0; p2Pts = 0; triggerAttackAnim(true);  triggerHit(false); }
                else       { p1HP = 0; p1Pts = 0; triggerAttackAnim(false); triggerHit(true);  }
                log("☠ " + pName + " used LETHAL BLOW! Instant KO!");
            }
        }
        checkKnockouts();
        refreshUI();
    }


    // GAME LOGIC — knockouts & game over

    private boolean checkKnockouts() {
        if (p1HP <= 0) {
            p1Lives--;
            p1HP = 100;
            log("💥 Player 1 knocked out! Lives left: " + p1Lives);
            flashScreen(new Color(200, 50, 50, 120));
        }
        if (p2HP <= 0) {
            p2Lives--;
            p2HP = 100;
            log("💥 Player 2 knocked out! Lives left: " + p2Lives);
            flashScreen(new Color(200, 50, 50, 120));
        }
        if (p1Lives <= 0) {
            answered = true;
            log("🏆 >>> PLAYER 2 WINS THE BATTLE! <<<");
            triggerGameOverOverlay("🏆  PLAYER 2 WINS!  🏆", P2_COL);
            return true;
        } else if (p2Lives <= 0) {
            answered = true;
            log("🏆 >>> PLAYER 1 WINS THE BATTLE! <<<");
            triggerGameOverOverlay("🏆  PLAYER 1 WINS!  🏆", P1_COL);
            return true;
        }
        return false;
    }


    // GAME-OVER OVERLAY
    
    private void triggerGameOverOverlay(String text, Color winnerColor) {
        winnerText = text;
        showGameOverlay = true;
        overlayAlpha = 0f;
        overlayPulse = 0f;

        int W = getWidth(), H = getHeight();
        frozenBackground = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = frozenBackground.createGraphics();
        paintComponent(sg);
        for (Component c : getComponents()) {
            if (!c.isVisible()) continue;
            sg.translate(c.getX(), c.getY());
            c.paint(sg);
            sg.translate(-c.getX(), -c.getY());
        }
        sg.dispose();
        frozenBackground = boxBlur(frozenBackground, 6);

        buildGameOverPanel(winnerColor);

        if (overlayTimer != null) overlayTimer.stop();
        overlayTimer = new Timer(16, e -> {
            overlayAlpha = Math.min(1f, overlayAlpha + 0.03f);
            overlayPulse += 0.05f;
            if (gameOverPanel != null) gameOverPanel.setVisible(true);
            repaint();
            if (overlayAlpha >= 1f) ((Timer) e.getSource()).stop();
        });
        overlayTimer.start();
    }

    private void buildGameOverPanel(Color winnerColor) {
        if (gameOverPanel != null) remove(gameOverPanel);
        int W = getWidth(), H = getHeight();

        gameOverPanel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                int pw = getWidth(), ph = getHeight();
                if (frozenBackground != null) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
                    g2.drawImage(frozenBackground, 0, 0, pw, ph, null);
                }
                g2.setColor(new Color(0, 0, 0, (int)(overlayAlpha * 160)));
                g2.fillRect(0, 0, pw, ph);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        };
        gameOverPanel.setOpaque(false);
        repositionOverlay();

        int cardW = Math.min(480, W - 80);
        int cardH = 230;
        int cardX = (W - cardW) / 2;
        int cardY = (H - cardH) / 2;

        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 12, 22, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                float pulse = (float)(0.6 + 0.4 * Math.sin(overlayPulse));
                Color glow  = new Color(winnerColor.getRed(), winnerColor.getGreen(),
                                        winnerColor.getBlue(), (int)(pulse * 255));
                g2.setColor(glow);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
            }
        };
        card.setOpaque(false);
        card.setBounds(cardX, cardY, cardW, cardH);

        JLabel trophy = new JLabel("🏆", SwingConstants.CENTER);
        trophy.setFont(new Font("Dialog", Font.PLAIN, 48));
        trophy.setBounds(0, 10, cardW, 70);
        card.add(trophy);

        JLabel winLbl = new JLabel("<html><center>" + winnerText + "</center></html>", SwingConstants.CENTER);
        winLbl.setFont(new Font("Monospaced", Font.BOLD, 22));
        winLbl.setForeground(winnerColor);
        winLbl.setBounds(10, 84, cardW - 20, 36);
        card.add(winLbl);

        JLabel sub = new JLabel("The battle has ended!", SwingConstants.CENTER);
        sub.setFont(new Font("Monospaced", Font.PLAIN, 13));
        sub.setForeground(TEXT_SEC);
        sub.setBounds(10, 124, cardW - 20, 20);
        card.add(sub);

        restartBtn = new JButton("▶  PLAY AGAIN  (or press ENTER)");
        restartBtn.setFont(new Font("Monospaced", Font.BOLD, 13));
        restartBtn.setForeground(new Color(15, 15, 25));
        restartBtn.setBackground(winnerColor);
        restartBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        restartBtn.setFocusPainted(false);
        restartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        restartBtn.setBounds((cardW - 320) / 2, 158, 320, 42);
        restartBtn.addActionListener(e -> { playClick(); resetGame(); });
        restartBtn.addMouseListener(new MouseAdapter() {
            final Color base = winnerColor;
            public void mouseEntered(MouseEvent e) { restartBtn.setBackground(winnerColor.brighter()); playHover(); }
            public void mouseExited (MouseEvent e) { restartBtn.setBackground(base); }
        });
        card.add(restartBtn);

        gameOverPanel.add(card);
        add(gameOverPanel);
        setComponentZOrder(gameOverPanel, 0);
        gameOverPanel.setVisible(true);
        revalidate();
        repaint();
    }

    private void repositionOverlay() {
        if (gameOverPanel != null) gameOverPanel.setBounds(0, 0, getWidth(), getHeight());
    }


    // MENU-CONFIRM OVERLAY

    private void showMenuConfirm() {
        if (menuConfirmPanel != null) return;
        int W = getWidth(), H = getHeight();

        BufferedImage snap = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = snap.createGraphics();
        paintComponent(sg);
        for (Component c : getComponents()) {
            if (!c.isVisible()) continue;
            sg.translate(c.getX(), c.getY());
            c.paint(sg);
            sg.translate(-c.getX(), -c.getY());
        }
        sg.dispose();
        final BufferedImage blurred = boxBlur(snap, 6);

        confirmAlpha = 0f;
        menuConfirmPanel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                int pw = getWidth(), ph = getHeight();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, confirmAlpha));
                g2.drawImage(blurred, 0, 0, pw, ph, null);
                g2.setColor(new Color(0, 0, 0, (int)(confirmAlpha * 160)));
                g2.fillRect(0, 0, pw, ph);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        };
        menuConfirmPanel.setOpaque(false);
        menuConfirmPanel.setBounds(0, 0, W, H);

        int cardW = Math.min(380, W - 80);
        int cardH = 180;
        int cardX = (W - cardW) / 2;
        int cardY = (H - cardH) / 2;

        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 12, 22, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            }
        };
        card.setOpaque(false);
        card.setBounds(cardX, cardY, cardW, cardH);

        JLabel icon = new JLabel("☰", SwingConstants.CENTER);
        icon.setFont(new Font("Dialog", Font.PLAIN, 28));
        icon.setForeground(TEXT_SEC);
        icon.setBounds(0, 16, cardW, 30);
        card.add(icon);

        JLabel question = new JLabel("Exit to Main Menu?", SwingConstants.CENTER);
        question.setFont(new Font("Monospaced", Font.BOLD, 16));
        question.setForeground(TEXT_PRI);
        question.setBounds(10, 50, cardW - 20, 22);
        card.add(question);

        JLabel sub = new JLabel("Current game progress will be reset.", SwingConstants.CENTER);
        sub.setFont(new Font("Monospaced", Font.PLAIN, 11));
        sub.setForeground(TEXT_SEC);
        sub.setBounds(10, 76, cardW - 20, 16);
        card.add(sub);

        int btnW = (cardW - 50) / 2;
        int btnY = 110;

        JButton yesBtn = new JButton("✔  YES, EXIT");
        yesBtn.setFont(new Font("Monospaced", Font.BOLD, 12));
        yesBtn.setForeground(new Color(15, 15, 25));
        yesBtn.setBackground(SKILL_RED);
        yesBtn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        yesBtn.setFocusPainted(false);
        yesBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        yesBtn.setBounds(16, btnY, btnW, 38);
        yesBtn.addActionListener(e -> {
            playClick();
            dismissMenuConfirm();
            stopAllAudio();
            resetGameState();
            controller.showPanel("MENU");
        });
        yesBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { yesBtn.setBackground(SKILL_RED.brighter()); playHover(); }
            public void mouseExited (MouseEvent e) { yesBtn.setBackground(SKILL_RED); }
        });
        card.add(yesBtn);

        JButton noBtn = new JButton("✖  NO, STAY");
        noBtn.setFont(new Font("Monospaced", Font.BOLD, 12));
        noBtn.setForeground(TEXT_PRI);
        noBtn.setBackground(BG_CARD);
        noBtn.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        noBtn.setFocusPainted(false);
        noBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        noBtn.setBounds(16 + btnW + 18, btnY, btnW, 38);
        noBtn.addActionListener(e -> { playClick(); dismissMenuConfirm(); });
        noBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { noBtn.setBackground(new Color(40, 40, 60)); playHover(); }
            public void mouseExited (MouseEvent e) { noBtn.setBackground(BG_CARD); }
        });
        card.add(noBtn);

        menuConfirmPanel.add(card);
        add(menuConfirmPanel);
        setComponentZOrder(menuConfirmPanel, 0);
        menuConfirmPanel.setVisible(true);

        if (confirmTimer != null) confirmTimer.stop();
        confirmTimer = new Timer(16, e -> {
            confirmAlpha = Math.min(1f, confirmAlpha + 0.06f);
            menuConfirmPanel.repaint();
            if (confirmAlpha >= 1f) ((Timer) e.getSource()).stop();
        });
        confirmTimer.start();

        revalidate();
        repaint();
    }

    private void dismissMenuConfirm() {
        if (menuConfirmPanel == null) return;
        if (confirmTimer != null) confirmTimer.stop();
        remove(menuConfirmPanel);
        menuConfirmPanel = null;
        confirmAlpha = 0f;
        revalidate();
        repaint();
        requestFocusInWindow();
    }


    // GAME STATE RESET

    private void resetGameState() {
        showGameOverlay = false;
        overlayAlpha    = 0f;
        frozenBackground = null;
        if (overlayTimer != null) overlayTimer.stop();
        if (gameOverPanel != null) { remove(gameOverPanel); gameOverPanel = null; }

        p1Lives = 3; p2Lives = 3;
        p1HP    = 100; p2HP  = 100;
        p1Pts   = 0;   p2Pts = 0;
        p1Shield = false; p2Shield = false;
        p1Double = false; p2Double = false;
        currentTurn = 0;
        answered    = false;
        isAttacking = false;
        animFrame   = 0;

        breathFrame = 0; breathTick = 0;
        p1InHit = false; p1HitTick = 0;
        p2InHit = false; p2HitTick = 0;

        if (getWidth() > 0) { p1X = P1_HOME_X; p2X = P2_HOME_X; }
        qm.reset();
        currentQ = null;
        if (logArea != null) logArea.setText("");
    }

    public void resetGame() {
        dismissMenuConfirm();
        resetGameState();
        if (audioLoader == null) initAudio();
        startBattleMusic();
        refreshUI();
        refreshQuestion();
        if (logArea != null) log("Battle started! P1: A/S/D  |  P2: J/K/L");
        showTurnBanner("PLAYER 1 GOES FIRST!", P1_COL);
        revalidate();
        repaint();
        requestFocusInWindow();
    }


    // ANIMATION HELPERS

    private void triggerAttackAnim(boolean p1Attacks) {
        isAttacking   = true;
        p1IsAttacking = p1Attacks;
        p2IsAttacking = !p1Attacks;
        animFrame     = 0;
        playAttack();
    }

    private void triggerHit(boolean hitP1) {
        if (hitP1) { p1InHit = true; p1HitTick = 0; }
        else        { p2InHit = true; p2HitTick = 0; }
    }

    private void flashScreen(Color col) {
        flashColor = col;
        flashAlpha = 0.5f;
    }

    private void showTurnBanner(String text, Color col) {
        bannerText = text;
        bannerAlpha = 1f;
        showingTurnBanner = true;
        if (bannerTimer != null) bannerTimer.stop();
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


    // UI REFRESH
    
    private void refreshUI() {
        if (p1HealthBar == null) return;
        p1HealthBar.setValue(p1HP);
        p2HealthBar.setValue(p2HP);
        p1HealthBar.setForeground(p1HP > 50 ? GREEN_HP : p1HP > 25 ? YELLOW_HP : RED_HP);
        p2HealthBar.setForeground(p2HP > 50 ? GREEN_HP : p2HP > 25 ? YELLOW_HP : RED_HP);
        p1LivesLbl.setText("❤ ".repeat(Math.max(0, p1Lives)).trim() + "  Lives: " + p1Lives);
        p2LivesLbl.setText("❤ ".repeat(Math.max(0, p2Lives)).trim() + "  Lives: " + p2Lives);
        p1PtsLbl.setText("POINTS: " + p1Pts);
        p2PtsLbl.setText("POINTS: " + p2Pts);
        p1ShieldLbl.setVisible(p1Shield);
        p2ShieldLbl.setVisible(p2Shield);
        p1DoubleLbl.setVisible(p1Double);
        p2DoubleLbl.setVisible(p2Double);

        boolean isP1Turn = (currentTurn == 0);
        if (turnIndicator != null) {
            turnIndicator.setText(isP1Turn
                ? "▶ PLAYER 1's TURN  [A / S / D]"
                : "▶ PLAYER 2's TURN  [J / K / L]");
            turnIndicator.setForeground(isP1Turn ? P1_COL : P2_COL);
        }

        int activePts = isP1Turn ? p1Pts : p2Pts;
        if (btnStrike != null) {
            btnStrike.setEnabled(activePts >= 3);
            btnShield.setEnabled(activePts >= 4);
            btnDouble.setEnabled(activePts >= 5);
            btnDrain .setEnabled(activePts >= 6);
            btnCurse .setEnabled(activePts >= 7);
            btnLethal.setEnabled(activePts >= 15);
        }
    }

    private void refreshQuestion() {
        if (p1Lives <= 0 || p2Lives <= 0 || (currentQ == null && qm == null)) return;
        currentQ = qm.next();
        for (int i = 0; i < 3; i++) {
            if (answerBtns[i] != null) {
                answerBtns[i].setBackground(BG_CARD);
                answerBtns[i].setBorder(BorderFactory.createLineBorder(BORDER, 1));
            }
        }
        if (currentQ != null && questionLabel != null) {
            questionLabel.setText("<html><center>[" + currentQ.topic.toUpperCase() + " · "
                + currentQ.difficulty.toUpperCase() + "]   " + currentQ.text + "</center></html>");
            String[] keys = {"A / J", "S / K", "D / L"};
            for (int i = 0; i < 3; i++) {
                if (answerBtns[i] != null) {
                    answerBtns[i].setText("<html><center><small style='color:#94a3b8'>"
                        + keys[i] + "</small><br>" + currentQ.options[i] + "</center></html>");
                }
            }
        }
    }

    // GAME LOOP
    @Override
    public void actionPerformed(ActionEvent e) {
        if (showGameOverlay) {
            overlayPulse += 0.05f;
            repaint();
        }

        if (!isPaused) {
            if (isAttacking) {
                animFrame++;
                if (p1IsAttacking) {
                    int target = P2_HOME_X - SPRITE_W - 20;
                    if      (animFrame < 10) p1X = P1_HOME_X + (target - P1_HOME_X) * animFrame / 10;
                    else if (animFrame < 20) p1X = P1_HOME_X + (target - P1_HOME_X) * (20 - animFrame) / 10;
                    else finishAttack();
                } else if (p2IsAttacking) {
                    int target = P1_HOME_X + SPRITE_W + 20;
                    if      (animFrame < 10) p2X = P2_HOME_X - (P2_HOME_X - target) * animFrame / 10;
                    else if (animFrame < 20) p2X = P2_HOME_X - (P2_HOME_X - target) * (20 - animFrame) / 10;
                    else finishAttack();
                }
            }

            breathTick++;
            if (breathTick >= BREATH_INTERVAL) {
                breathTick  = 0;
                breathFrame = 1 - breathFrame;
            }

            if (p1InHit) { p1HitTick++; if (p1HitTick >= HIT_DURATION) { p1InHit = false; p1HitTick = 0; } }
            if (p2InHit) { p2HitTick++; if (p2HitTick >= HIT_DURATION) { p2InHit = false; p2HitTick = 0; } }
        }

        if (flashAlpha > 0) flashAlpha = Math.max(0f, flashAlpha - 0.05f);
        if (!showGameOverlay) repaint();
    }

    private void finishAttack() {
        isAttacking = false; p1IsAttacking = false; p2IsAttacking = false;
        animFrame   = 0;
        p1X = P1_HOME_X;
        p2X = P2_HOME_X;
    }

    // PAINTING
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);

        int W = getWidth(), H = getHeight();

        if (battleBackground != null) {

            g2.drawImage(battleBackground, 0, -70, W, H, null);

        } else {

            g2.setPaint(new GradientPaint(0, 0, new Color(15, 15, 30), W, H, new Color(10, 10, 20)));
            g2.fillRect(0, 0, W, H);
        }

        // Arena floor 
        int floorY = H - 150;
        g2.setColor(new Color(30, 30, 50, 180));
        g2.fillRoundRect(W / 8, floorY, W * 6 / 8, 8, 8, 8);

        int spriteY = floorY - SPRITE_H - 5;

        // Drop shadows
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(p1X, floorY - 50, SPRITE_W, 18);
        g2.fillOval(p2X, floorY - 50, SPRITE_W, 18);

        // Player 1 sprite 
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

        // Player 2 sprite (mirrored)
        Image f2 = getFrame(false);
        if (f2 != null) {
            g2.drawImage(f2, p2X + SPRITE_W, spriteY, -SPRITE_W, SPRITE_H, null);
        } else {
            g2.setColor(P2_COL);
            g2.fillRoundRect(p2X + 5, spriteY, SPRITE_W - 10, SPRITE_H, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 30));
            g2.drawString("P2", p2X + 22, spriteY + SPRITE_H / 2 + 12);
        }

        // Screen flash overlay
        if (flashAlpha > 0.01f) {
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(),
                                  flashColor.getBlue(), (int)(flashAlpha * 200)));
            g2.fillRect(0, 0, W, H);
        }

        // Turn banner 
        if (showingTurnBanner && bannerAlpha > 0) {
            int bH = 60;
            int bY = H / 2 - bH / 2;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bannerAlpha));
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRoundRect(W / 4, bY, W / 2, bH, 16, 16);
            Color bannerCol = (currentTurn == 0) ? P1_COL : P2_COL;
            g2.setColor(bannerCol);
            g2.setFont(new Font("Monospaced", Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (W - fm.stringWidth(bannerText)) / 2;
            g2.drawString(bannerText, tx, bY + bH / 2 + 8);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    // UTILITY
    private BufferedImage boxBlur(BufferedImage src, int radius) {
        int     size = radius * 2 + 1;
        float[] data = new float[size * size];
        float   val  = 1.0f / (size * size);
        for (int i = 0; i < data.length; i++) data[i] = val;
        Kernel     kernel = new Kernel(size, size, data);
        ConvolveOp op     = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        op.filter(src, dest);
        return dest;
    }
}
