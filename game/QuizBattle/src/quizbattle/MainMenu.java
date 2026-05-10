/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package quizbattle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import ddf.minim.*;

/**
 *
 * @author End-User
 */
public class MainMenu extends JPanel {
    private QuizBattle controller;
    private String selectedDifficulty = "easy";

    // --- Background Image ---
    private Image backgroundImage;

    // --- Audio ---
    Minim loader;
    AudioPlayer song;
    AudioPlayer sfxHover;
    AudioPlayer sfxClick;

    private final Color BG_DARK   = new Color(15, 15, 25);
    private final Color BG_CARD   = new Color(22, 22, 38);
    private final Color ACCENT    = new Color(99, 102, 241);
    private final Color GREEN     = new Color(34, 197, 94);
    private final Color YELLOW    = new Color(234, 179, 8);
    private final Color RED       = new Color(239, 68, 68);
    private final Color TEXT_PRI  = new Color(248, 250, 252);
    private final Color TEXT_SEC  = new Color(148, 163, 184);
    private final Color BORDER    = new Color(51, 65, 85);

    private JButton btnEasy, btnMed, btnHard;
    private float fadeAlpha = 0f;
    private Timer fadeTimer;

    public MainMenu(QuizBattle controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        // --- Load Background Image ---
        // Ensure "menu_bg.png" is in your project's root folder
        backgroundImage = new ImageIcon("backgroundM.png").getImage();

        // --- Minim Init ---
        loader   = new Minim(this);
        song     = loader.loadFile("MusicMenu.mp3");
        sfxHover = loader.loadFile("Hover.mp3");
        sfxClick = loader.loadFile("Click.mp3");

        if (song != null) {
            song.loop();
        }

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

        // Difficulty label
        JLabel diffLbl = new JLabel("SELECT DIFFICULTY", SwingConstants.CENTER);
        diffLbl.setFont(new Font("Monospaced", Font.BOLD, 14));
        diffLbl.setForeground(TEXT_SEC);
        gbc.gridy = 3; gbc.insets = new Insets(4, 40, 8, 40);
        center.add(diffLbl, gbc);

        // Difficulty buttons
        JPanel diffRow = new JPanel(new GridLayout(1, 3, 10, 0));
        diffRow.setOpaque(false);
        btnEasy = makeDiffBtn("🌿 EASY",   GREEN);
        btnMed  = makeDiffBtn("🔥 MEDIUM", YELLOW);
        btnHard = makeDiffBtn("💀 HARD",   RED);
        btnEasy.addActionListener(e -> { playClick(); selectDiff("easy"); });
        btnMed .addActionListener(e -> { playClick(); selectDiff("medium"); });
        btnHard.addActionListener(e -> { playClick(); selectDiff("hard"); });
        diffRow.add(btnEasy); diffRow.add(btnMed); diffRow.add(btnHard);
        gbc.gridy = 4; gbc.insets = new Insets(0, 40, 16, 40);
        center.add(diffRow, gbc);

        selectDiff("easy");

        // Info box
        JTextArea info = new JTextArea(
            "  ⚡ Strike (3pts)   🛡 Shield (4pts)   ✨ Double (5pts)\n" +
            "  🌀 Drain HP (6pts)   💀 Curse (7pts)   ☠ LETHAL (15pts)\n" +
            "  P1 keys: A / S / D       P2 keys: J / K / L"
        );
        info.setFont(new Font("Monospaced", Font.PLAIN, 12));
        info.setForeground(TEXT_SEC);
        info.setBackground(BG_CARD);
        info.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        info.setEditable(false);
        gbc.gridy = 5; gbc.insets = new Insets(0, 40, 16, 40);
        center.add(info, gbc);

        gbc.insets = new Insets(4, 80, 4, 80);

        // START button
        JButton btnStart = makeMainBtn("▶  START BATTLE", ACCENT);
        btnStart.addActionListener(e -> {
            playClick();
            stopMenuMusic();
            controller.setDifficulty(selectedDifficulty);
            controller.showPanel("GAME");
        });
        gbc.gridy = 6; center.add(btnStart, gbc);

        // HOW TO PLAY button
        JButton btnTut = makeMainBtn("📖  HOW TO PLAY", BG_CARD);
        btnTut.setForeground(TEXT_SEC);
        btnTut.addActionListener(e -> { playClick(); showTutorial(); });
        gbc.gridy = 7; center.add(btnTut, gbc);

        // EXIT button
        JButton btnExit = makeMainBtn("✕  EXIT", BG_CARD);
        btnExit.setForeground(RED);
        btnExit.addActionListener(e -> {
            playClick();
            stopMenuMusic();
            System.exit(0);
        });
        gbc.gridy = 8; gbc.insets = new Insets(4, 80, 20, 80);
        center.add(btnExit, gbc);
    }

    public void resumeMenuMusic() {
        if (loader == null) loader = new Minim(this);
        if (song == null) song = loader.loadFile("MusicMenu.mp3");
        if (song != null) {
            song.rewind();
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
        if (sfxHover != null) { sfxHover.rewind(); sfxHover.play(); }
    }

    private void playClick() {
        if (sfxClick != null) { sfxClick.rewind(); sfxClick.play(); }
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

        // 1. Draw Background Image
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, w, h, this);
            // Optional: Dark overlay to make UI elements pop
            g2.setColor(new Color(15, 15, 25, 100)); 
            g2.fillRect(0, 0, w, h);
        } else {
            // Fallback gradient if image isn't found
            GradientPaint gp = new GradientPaint(w / 2f, 0, new Color(20, 20, 45), w / 2f, h, BG_DARK);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        }

        // 2. Fade-in overlay logic
        if (fadeAlpha < 1.0f) {
            g2.setColor(new Color(15, 15, 25, (int) ((1f - fadeAlpha) * 255)));
            g2.fillRect(0, 0, w, h);
        }
    }

    private JButton makeDiffBtn(String text, Color col) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setFont(new Font("Monospaced", Font.BOLD, 13));
        b.setBackground(BG_CARD);
        b.setForeground(col);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(col, 2),
            BorderFactory.createEmptyBorder(8, 6, 8, 6)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { playHover(); }
        });
        return b;
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

    private void selectDiff(String diff) {
        selectedDifficulty = diff;
        resetBtn(btnEasy, GREEN);
        resetBtn(btnMed,  YELLOW);
        resetBtn(btnHard, RED);
        JButton sel = diff.equals("easy") ? btnEasy : diff.equals("medium") ? btnMed : btnHard;
        Color   col = diff.equals("easy") ? GREEN   : diff.equals("medium") ? YELLOW : RED;
        sel.setBackground(col);
        sel.setForeground(Color.WHITE);
    }

    private void resetBtn(JButton b, Color col) {
        b.setBackground(BG_CARD);
        b.setForeground(col);
    }

    private void showTutorial() {
        JOptionPane.showMessageDialog(this,
            "HOW TO PLAY\n" +
            "──────────────────────────────────────────\n" +
            "• Players take turns answering questions.\n" +
            "• Player 1 controls: A (opt 1)  S (opt 2)  D (opt 3)\n" +
            "• Player 2 controls: J (opt 1)  K (opt 2)  L (opt 3)\n\n" +
            "SCORING\n" +
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
            "Losing all HP costs 1 life. Lose all lives = DEFEAT.",
            "HOW TO PLAY", JOptionPane.PLAIN_MESSAGE);
    }
}
