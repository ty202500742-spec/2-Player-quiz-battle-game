// MainMenu.java
package quizbattle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import ddf.minim.*;

public class MainMenu extends JPanel {
    private QuizBattle controller;
    private String selectedDifficulty = "easy";
    private GameState.Mode selectedMode = GameState.Mode.CLASSIC;

    // --- Background Image ---
    private Image backgroundImage;
    
    // --- Audio ---
    Minim loader;
    AudioPlayer song;
    AudioPlayer sfxHover;
    AudioPlayer sfxClick;
    private AudioManager audio;
    public void setAudioManager(AudioManager am) {
    this.audio = am;
}
    private final Color BG_DARK   = new Color(15, 15, 25);
    private final Color BG_CARD   = new Color(22, 22, 38);
    private final Color ACCENT    = new Color(99, 102, 241);
    private final Color GREEN     = new Color(34, 197, 94);
    private final Color YELLOW    = new Color(234, 179, 8);
    private final Color RED       = new Color(239, 68, 68);
    private final Color TEXT_PRI  = new Color(248, 250, 252);
    private final Color TEXT_SEC  = new Color(148, 163, 184);
    private final Color BORDER    = new Color(51, 65, 85);
    private final Color BLITZ_COL = new Color(251, 191, 36);   // amber
    private final Color CPU_COL   = new Color(167, 139, 250);  // purple

    private float fadeAlpha = 0f;
    private Timer fadeTimer;
    private static AudioPlayer menuSongInstance;
    public MainMenu(QuizBattle controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        
        backgroundImage = new ImageIcon("assets/images/backgroundM.png").getImage();

        loader   = new Minim(this);
      
        sfxHover = loader.loadFile("assets/audio/Hover.mp3");
        sfxClick = loader.loadFile("assets/audio/Click.mp3");
        if (menuSongInstance == null) {
    menuSongInstance = loader.loadFile("MusicMenu.mp3");
}
song = menuSongInstance;
        resumeMenuMusic();
        // Fade-in animation
        fadeAlpha = 0f;
        fadeTimer = new Timer(16, e -> {
            fadeAlpha = Math.min(1f, fadeAlpha + 0.04f);
            repaint();
            if (fadeAlpha >= 1f) ((Timer) e.getSource()).stop();
        });
        fadeTimer.start();

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        add(center, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill  = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 40, 6, 40);

        // Title
        JLabel title = new JLabel("⚔ QUIZ BATTLE", SwingConstants.CENTER);
        title.setFont(loadFont(52f));
        title.setForeground(TEXT_PRI);
        gbc.gridy = 0; gbc.insets = new Insets(20, 40, 4, 40);
        center.add(title, gbc);

        JLabel sub = new JLabel("WMSU CS EDITION", SwingConstants.CENTER);
        sub.setFont(new Font("Monospaced", Font.BOLD, 13));
        sub.setForeground(ACCENT);
        gbc.gridy = 1; gbc.insets = new Insets(0, 40, 20, 40);
        center.add(sub, gbc);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        gbc.gridy = 2; gbc.insets = new Insets(4, 40, 16, 40);
        center.add(sep, gbc);


        // START button — now opens the mode+difficulty dialog
        JButton btnStart = makeMainBtn("▶  START BATTLE", ACCENT);
        btnStart.addActionListener(e -> {
            playClick();
            showModeAndDifficultyDialog();
        });
        gbc.gridy = 4; center.add(btnStart, gbc);

        // HOW TO PLAY button
        JButton btnTut = makeMainBtn("📖  HOW TO PLAY", BG_CARD);
        btnTut.setForeground(TEXT_SEC);
        btnTut.addActionListener(e -> { playClick(); showTutorial(); });
        gbc.gridy = 5; center.add(btnTut, gbc);

        // SETTINGS button
        JButton btnSettings = makeMainBtn("⚙  SETTINGS", BG_CARD);
        btnSettings.setForeground(TEXT_SEC);
        btnSettings.addActionListener(e -> { playClick(); showSettingsDialog(); });
        gbc.gridy = 6; center.add(btnSettings, gbc);

        // EXIT button
        JButton btnExit = makeMainBtn("✕  EXIT", BG_CARD);
        btnExit.setForeground(RED);
        btnExit.addActionListener(e -> {
            playClick();
            stopMenuMusic();
            System.exit(0);
        });
        gbc.gridy = 7; gbc.insets = new Insets(4, 80, 20, 80);
        center.add(btnExit, gbc);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MODE + DIFFICULTY DIALOG
    // A two-step modal:
    //   Step 1 — pick a game mode (Classic / Blitz / vs CPU)
    //   Step 2 — pick difficulty (Easy / Medium / Hard)
    //            (for Blitz/Classic the difficulty affects the question pool;
    //             for CPU it also sets the bot's skill level)
    // ══════════════════════════════════════════════════════════════════════════
    private void showModeAndDifficultyDialog() {
        // ── Dialog shell ──────────────────────────────────────────────────────
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                                  "Select Mode", true);
        dlg.setUndecorated(true);
        dlg.setSize(480, 340);
        dlg.setLocationRelativeTo(this);

        // ── Card container ────────────────────────────────────────────────────
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 12, 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            }
        };
        card.setOpaque(false);
        dlg.setContentPane(card);

        // We'll swap between two "pages" by showing/hiding panels
        JPanel stepMode = buildStepMode(dlg, card);
        card.add(stepMode);

        dlg.setVisible(true);
    }

    // ── Step 1: Choose Game Mode ──────────────────────────────────────────────
    private JPanel buildStepMode(JDialog dlg, JPanel card) {
        JPanel p = new JPanel(null);
        p.setOpaque(false);
        p.setBounds(0, 0, 480, 340);

        JLabel lbl = new JLabel("SELECT GAME MODE", SwingConstants.CENTER);
        lbl.setFont(new Font("Monospaced", Font.BOLD, 17));
        lbl.setForeground(TEXT_PRI);
        lbl.setBounds(0, 18, 480, 26);
        p.add(lbl);

        JSeparator line = new JSeparator();
        line.setForeground(BORDER);
        line.setBounds(20, 52, 440, 2);
        p.add(line);

        // Mode buttons — stacked with description
        int bX = 30, bW = 420, bY = 68, bH = 60, gap = 10;

        JButton btnClassic = makeModeBtn("⚔  CLASSIC",
            "Standard turn-based quiz duel  •  10 sec per turn", ACCENT, bX, bY, bW, bH);
        JButton btnBlitz   = makeModeBtn("⚡  BLITZ",
            "Lightning-fast mode  •  only 3 seconds to answer!", BLITZ_COL,
            bX, bY + (bH + gap), bW, bH);
        JButton btnCPU     = makeModeBtn("🤖  VS CPU",
            "Challenge the computer — choose its difficulty next", CPU_COL,
            bX, bY + (bH + gap) * 2, bW, bH);

        p.add(btnClassic); p.add(btnBlitz); p.add(btnCPU);

        // Cancel
        JButton btnCancel = makeSmallBtn("✕  CANCEL", RED);
        btnCancel.setBounds(190, bY + (bH + gap) * 3 + 6, 100, 32);
        btnCancel.addActionListener(e -> { playClick(); dlg.dispose(); });
        p.add(btnCancel);

        // Wire mode buttons — each opens the difficulty step
        btnClassic.addActionListener(e -> {
            playClick();
            selectedMode = GameState.Mode.CLASSIC;
            swapToStep2(dlg, card, p, "⚔  CLASSIC MODE");
        });
        btnBlitz.addActionListener(e -> {
            playClick();
            selectedMode = GameState.Mode.BLITZ;
            swapToStep2(dlg, card, p, "⚡  BLITZ MODE");
        });
        btnCPU.addActionListener(e -> {
            playClick();
            selectedMode = GameState.Mode.COMPUTER;
            swapToStep2(dlg, card, p, "🤖  VS CPU");
        });

        return p;
    }

    // ── Step 2: Choose Difficulty ─────────────────────────────────────────────
    private void swapToStep2(JDialog dlg, JPanel card, JPanel step1, String modeLabel) {
        step1.setVisible(false);

        JPanel p = new JPanel(null);
        p.setOpaque(false);
        p.setBounds(0, 0, 480, 340);

        JLabel modeLbl = new JLabel(modeLabel, SwingConstants.CENTER);
        modeLbl.setFont(new Font("Monospaced", Font.BOLD, 15));
        modeLbl.setForeground(ACCENT);
        modeLbl.setBounds(0, 14, 480, 22);
        p.add(modeLbl);

        JLabel lbl = new JLabel("SELECT DIFFICULTY", SwingConstants.CENTER);
        lbl.setFont(new Font("Monospaced", Font.BOLD, 17));
        lbl.setForeground(TEXT_PRI);
        lbl.setBounds(0, 40, 480, 24);
        p.add(lbl);

        JSeparator line = new JSeparator();
        line.setForeground(BORDER);
        line.setBounds(20, 70, 440, 2);
        p.add(line);

        int bX = 30, bW = 420, bY = 84, bH = 58, gap = 10;

        JButton btnEasy = makeModeBtn("🌿  EASY",
            "Beginner questions  •  40% bot accuracy (CPU mode)", GREEN, bX, bY, bW, bH);
        JButton btnMed  = makeModeBtn("🔥  MEDIUM",
            "Mixed questions  •  65% bot accuracy (CPU mode)", YELLOW,
            bX, bY + (bH + gap), bW, bH);
        JButton btnHard = makeModeBtn("💀  HARD",
            "Expert questions  •  90% bot accuracy (CPU mode)", RED,
            bX, bY + (bH + gap) * 2, bW, bH);

        p.add(btnEasy); p.add(btnMed); p.add(btnHard);

        // Back button
        JButton btnBack = makeSmallBtn("← BACK", TEXT_SEC);
        btnBack.setBounds(30, bY + (bH + gap) * 3 + 6, 80, 32);
        btnBack.addActionListener(e -> {
            playClick();
            card.remove(p);
            step1.setVisible(true);
            card.revalidate();
            card.repaint();
        });
        p.add(btnBack);

        // Wire difficulty buttons
        btnEasy.addActionListener(e -> {
            playClick();
            selectedDifficulty = "easy";
            launchGame(dlg);
        });
        btnMed.addActionListener(e -> {
            playClick();
            selectedDifficulty = "medium";
            launchGame(dlg);
        });
        btnHard.addActionListener(e -> {
            playClick();
            selectedDifficulty = "hard";
            launchGame(dlg);
        });

        card.add(p);
        card.revalidate();
        card.repaint();
    }

    // ── Actually start the game once mode+difficulty are chosen ──────────────
    private void launchGame(JDialog dlg) {
        dlg.dispose();
        stopMenuMusic();
        controller.setDifficulty(selectedDifficulty);
        controller.setGameMode(selectedMode, selectedDifficulty);
        controller.showPanel("GAME");
    }

    // ── Mode button (wide, two-line) ──────────────────────────────────────────
    private JButton makeModeBtn(String title, String desc, Color col,
                                int x, int y, int w, int h) {
        String html = "<html><b>" + title + "</b><br>"
                    + "<font color='#94a3b8'><small>" + desc + "</small></font></html>";
        JButton b = new JButton(html);
        b.setBounds(x, y, w, h);
        b.setFocusable(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBackground(new Color(col.getRed() / 6, col.getGreen() / 6, col.getBlue() / 6 + 10));
        b.setForeground(col);
        b.setFont(new Font("Monospaced", Font.BOLD, 13));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(col.darker(), 1),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color dimBg  = new Color(col.getRed() / 6, col.getGreen() / 6, col.getBlue() / 6 + 10);
        Color hoverBg = new Color(
            Math.min(255, col.getRed()   / 3),
            Math.min(255, col.getGreen() / 3),
            Math.min(255, col.getBlue()  / 3 + 10));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(hoverBg);
                b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(col, 2),
                    BorderFactory.createEmptyBorder(5, 15, 5, 15)));
                playHover();
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(dimBg);
                b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(col.darker(), 1),
                    BorderFactory.createEmptyBorder(6, 16, 6, 16)));
            }
        });
        return b;
    }

    private JButton makeSmallBtn(String text, Color col) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setFont(new Font("Monospaced", Font.BOLD, 11));
        b.setBackground(BG_CARD);
        b.setForeground(col);
        b.setBorder(BorderFactory.createLineBorder(col.darker(), 1));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { playHover(); }
        });
        return b;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SETTINGS DIALOG  (unchanged from original)
    // ══════════════════════════════════════════════════════════════════════════
    private void showSettingsDialog() {
        AudioSettings as = AudioSettings.get();

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                                  "⚙ Settings", true);
        dlg.setUndecorated(true);
        dlg.setSize(420, 260);
        dlg.setLocationRelativeTo(this);

        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 12, 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            }
        };
        card.setPreferredSize(new Dimension(420, 260));

        JLabel titleLbl = new JLabel("⚙  SETTINGS", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Monospaced", Font.BOLD, 18));
        titleLbl.setForeground(TEXT_PRI);
        titleLbl.setBounds(0, 18, 420, 28);
        card.add(titleLbl);

        JSeparator line = new JSeparator();
        line.setForeground(BORDER);
        line.setBounds(20, 52, 380, 2);
        card.add(line);

        JLabel musicLbl = new JLabel("🎵  Music Volume");
        musicLbl.setFont(new Font("Monospaced", Font.BOLD, 13));
        musicLbl.setForeground(TEXT_SEC);
        musicLbl.setBounds(30, 66, 200, 20);
        card.add(musicLbl);

        JLabel musicVal = new JLabel(as.getMusicVolume() + "%", SwingConstants.RIGHT);
        musicVal.setFont(new Font("Monospaced", Font.BOLD, 13));
        musicVal.setForeground(ACCENT);
        musicVal.setBounds(340, 66, 50, 20);
        card.add(musicVal);

        JSlider musicSlider = makeSlider(as.getMusicVolume());
        musicSlider.setBounds(30, 88, 360, 28);
        musicSlider.addChangeListener(ev -> {
            int v = musicSlider.getValue();
            musicVal.setText(v + "%");
            as.setMusicVolume(v);
            if (song != null) song.setGain(AudioSettings.toGain(v));
             if (audio != null) audio.applyMusicVolume();
        });
        card.add(musicSlider);

        JLabel sfxLbl = new JLabel("🔊  SFX Volume");
        sfxLbl.setFont(new Font("Monospaced", Font.BOLD, 13));
        sfxLbl.setForeground(TEXT_SEC);
        sfxLbl.setBounds(30, 128, 200, 20);
        card.add(sfxLbl);

        JLabel sfxVal = new JLabel(as.getSfxVolume() + "%", SwingConstants.RIGHT);
        sfxVal.setFont(new Font("Monospaced", Font.BOLD, 13));
        sfxVal.setForeground(ACCENT);
        sfxVal.setBounds(340, 128, 50, 20);
        card.add(sfxVal);

        JSlider sfxSlider = makeSlider(as.getSfxVolume());
        sfxSlider.setBounds(30, 150, 360, 28);
        sfxSlider.addChangeListener(ev -> {
            int v = sfxSlider.getValue();
            sfxVal.setText(v + "%");
            as.setSfxVolume(v);
            if (sfxHover != null) sfxHover.setGain(AudioSettings.toGain(v));
            if (sfxClick != null) sfxClick.setGain(AudioSettings.toGain(v));
        });
        card.add(sfxSlider);

        JButton closeBtn = new JButton("✔  DONE");
        closeBtn.setFont(new Font("Monospaced", Font.BOLD, 13));
        closeBtn.setForeground(new Color(15, 15, 25));
        closeBtn.setBackground(ACCENT);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setBounds(150, 196, 120, 38);
        closeBtn.addActionListener(e -> { playClick(); dlg.dispose(); });
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setBackground(ACCENT.brighter()); }
            public void mouseExited (MouseEvent e) { closeBtn.setBackground(ACCENT); }
        });
        card.add(closeBtn);

        dlg.setContentPane(card);
        dlg.setVisible(true);
    }

    private JSlider makeSlider(int initialValue) {
        JSlider s = new JSlider(0, 100, initialValue);
        s.setOpaque(false);
        s.setForeground(ACCENT);
        s.setBackground(new Color(0, 0, 0, 0));
        s.setPaintTicks(true);
        s.setMajorTickSpacing(25);
        s.setMinorTickSpacing(5);
        s.setPaintLabels(false);
        s.setFocusable(false);
        s.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        return s;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AUDIO HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    public void resumeMenuMusic() {
        if (loader == null) loader = new Minim(this);
        if (song == null) song = loader.loadFile("MusicMenu.mp3");
        if (song != null) {
            song.rewind();

song.setGain(AudioSettings.toGain(
    AudioSettings.get().getMusicVolume()
));

song.loop();
        }
    }

    public void stopMenuMusic() {
        if (song != null) {
            song.pause();
            song.close();
            song = null;
        }
    }

    private void playHover() {
        if (sfxHover != null) {
            sfxHover.setGain(AudioSettings.toGain(AudioSettings.get().getSfxVolume()));
            sfxHover.rewind();
            sfxHover.play();
        }
    }

    private void playClick() {
        if (sfxClick != null) {
            sfxClick.setGain(AudioSettings.toGain(AudioSettings.get().getSfxVolume()));
            sfxClick.rewind();
            sfxClick.play();
        }
    }

    public String sketchPath(String fileName) { return fileName; }

    public InputStream createInput(String fileName) {
        try { return new FileInputStream(new File(fileName)); }
        catch (Exception e) { return null; }
    }

    private Font loadFont(float size) {
        return new Font("Monospaced", Font.BOLD, (int) size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, w, h, this);
            g2.setColor(new Color(15, 15, 25, 100));
            g2.fillRect(0, 0, w, h);
        } else {
            GradientPaint gp = new GradientPaint(w / 2f, 0, new Color(20, 20, 45), w / 2f, h, BG_DARK);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        }

        if (fadeAlpha < 1.0f) {
            g2.setColor(new Color(15, 15, 25, (int) ((1f - fadeAlpha) * 255)));
            g2.fillRect(0, 0, w, h);
        }
    }

    private JButton makeMainBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setFont(new Font("Monospaced", Font.BOLD, 15));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            final Color orig = bg;
            public void mouseEntered(MouseEvent e) {
                b.setBackground(orig.brighter());
                playHover();
            }
            public void mouseExited(MouseEvent e) { b.setBackground(orig); }
        });
        return b;
    }

    private void showTutorial() {
        JOptionPane.showMessageDialog(this,
            "HOW TO PLAY\n" +
            "──────────────────────────────────────────\n" +
            "• Players take turns answering questions.\n" +
            "• Player 1 controls: A (opt 1)  S (opt 2)  D (opt 3)\n"
               + "Q (skill 1)  W (skill 2)  E (skill 3) R (skill 4)  T (skill 5)  Y (skill 6)\n" +
            "• Player 2 controls: J (opt 1)  K (opt 2)  L (opt 3)\n"
               + "U (skill 1)  I (skill 2)  O (skill 3) P (skill 4)  [ (skill 5)  ] (skill 6)\n" +

            "\nSCORING\n" +
            "──────────────────────────────────────────\n" +
            "• Correct answer → opponent loses 20 HP, you earn +1 pt\n" +
            "• Wrong answer → you lose 10 HP\n" +
            "• If you have >3 pts and answer wrong → also lose 1 pt\n\n" +
            "SKILLS (use BEFORE answering, on your turn)\n" +
            "──────────────────────────────────────────\n" +
            "⚡ Strike   (3 pts) → Opponent -20 HP\n" +
            "🛡 Shield   (4 pts) → Block next hit\n" +
            "✨ Double   (5 pts) → Next correct answer = 2 pts\n" +
            "🌀 Drain    (6 pts) → Steal 20 HP from opponent\n" +
            "💀 Curse    (7 pts) → Opponent -2 pts\n" +
            "☠ LETHAL  (15 pts) → Wipe opponent HP to zero!\n\n" +
            "LIVES: Each player starts with 3 lives.\n" +
            "Losing all HP costs 1 life. Lose all lives = DEFEAT.\n\n" +
            "GAME MODES\n" +
            "──────────────────────────────────────────\n" +
            "⚔  Classic  — 10 seconds per turn, standard rules\n" +
            "⚡  Blitz    — Only 3 seconds to answer each question!\n" +
            "🤖  VS CPU   — Fight the AI at Easy / Medium / Hard",
            "HOW TO PLAY", JOptionPane.PLAIN_MESSAGE);
    }
}