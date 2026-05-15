package quizbattle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class OverlayManager {

    // ── Back-references ───────────────────────────────────────────────────────
    private final JPanel       host;
    private final AudioManager audio;
    private final BattleUI     ui;
    private final QuizBattle   controller;

    // ── Pause overlay ─────────────────────────────────────────────────────────
    private JPanel pausePanel;
    private float  pausePanelAlpha = 0f;
    private Timer  pausePanelTimer;

    // ── Restart-confirm overlay ───────────────────────────────────────────────
    private JPanel restartConfirmPanel;
    private float  restartConfirmAlpha = 0f;
    private Timer  restartConfirmTimer;

    // ── Menu-confirm overlay ──────────────────────────────────────────────────
    private JPanel menuConfirmPanel;
    private float  confirmAlpha = 0f;
    private Timer  confirmTimer;

    // ── Game-over overlay ─────────────────────────────────────────────────────
    // Implements BattleUI.GameOverlayPanel so it can be passed to layoutAll()
    private BattleUI.GameOverlayPanel gameOverPanel   = null;
    private float                     overlayAlpha    = 0f;
    private float                     overlayPulse    = 0f;
    private Timer                     overlayTimer;
    private BufferedImage             frozenBackground = null;
    private String                    winnerText       = "";
    private Runnable                  onRestart;

    // ── Shared state flags ────────────────────────────────────────────────────
    private boolean isPaused        = false;
    private boolean showGameOverlay = false;

    // ══════════════════════════════════════════════════════════════════════════
    // COLOUR PALETTE
    // ══════════════════════════════════════════════════════════════════════════
    private final Color BG_DARK   = new Color(15,  15,  25);
    private final Color BG_CARD   = new Color(22,  22,  38);
    private final Color ACCENT    = new Color(99,  102, 241);
    private final Color TEXT_PRI  = new Color(248, 250, 252);
    private final Color TEXT_SEC  = new Color(148, 163, 184);
    private final Color BORDER    = new Color(51,  65,  85);
    private final Color SKILL_RED = new Color(239, 68,  68);

    // ═════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═════════════════════════════════════════════════════════════════════════
    public OverlayManager(JPanel host, AudioManager audio, BattleUI ui, QuizBattle controller) {
        this.host       = host;
        this.audio      = audio;
        this.ui         = ui;
        this.controller = controller;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GAME-OVER OVERLAY
    // ═════════════════════════════════════════════════════════════════════════

    /** Call after the slow-motion sequence ends. */
    public void showGameOver(String text, Color winnerColor, Runnable onRestart) {
        this.winnerText  = text;
        this.onRestart   = onRestart;
        showGameOverlay  = true;
        overlayAlpha     = 0f;
        overlayPulse     = 0f;

        int W = host.getWidth(), H = host.getHeight();
        frozenBackground = boxBlur(captureSnapshot(), 6);

        buildGameOverPanel(winnerColor, W, H);

        if (overlayTimer != null) overlayTimer.stop();
        overlayTimer = new Timer(16, e -> {
            overlayAlpha = Math.min(1f, overlayAlpha + 0.03f);
            overlayPulse += 0.05f;
            if (gameOverPanel != null) gameOverPanel.asComponent().setVisible(true);
            host.repaint();
            if (overlayAlpha >= 1f) ((Timer) e.getSource()).stop();
        });
        overlayTimer.start();

        ui.setShowGameOverlay(true);
    }

    /** Called every game-loop tick while the game-over overlay is active. */
    public void tickOverlayPulse() {
        if (showGameOverlay) overlayPulse += 0.05f;
    }

    public boolean isGameOverShowing() { return showGameOverlay; }

    /** Returns the current game-over panel (may be null). Used by BattlePanel for layoutAll(). */
    public BattleUI.GameOverlayPanel getGameOverPanel() { return gameOverPanel; }

    private void buildGameOverPanel(Color winnerColor, int W, int H) {
        // Remove old panel if present
        if (gameOverPanel != null) host.remove(gameOverPanel.asComponent());

        // Build the backing JPanel that also implements GameOverlayPanel
        JPanel backing = new JPanel(null) {
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
        backing.setOpaque(false);
        backing.setBounds(0, 0, W, H);

        // Wrap in GameOverlayPanel interface
        gameOverPanel = new BattleUI.GameOverlayPanel() {
            @Override public void reposition(int w, int h) { backing.setBounds(0, 0, w, h); }
            @Override public Component asComponent()       { return backing; }
        };

        int cardW = Math.min(480, W - 80), cardH = 230;
        int cardX = (W - cardW) / 2, cardY = (H - cardH) / 2;

        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 12, 22, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                float pulse = (float)(0.6 + 0.4 * Math.sin(overlayPulse));
                Color glow  = new Color(winnerColor.getRed(), winnerColor.getGreen(),
                                        winnerColor.getBlue(), (int)(pulse * 255));
                g2.setColor(glow); g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
            }
        };
        card.setOpaque(false); card.setBounds(cardX, cardY, cardW, cardH);

        JLabel trophy = new JLabel("🏆", SwingConstants.CENTER);
        trophy.setFont(new Font("Dialog", Font.PLAIN, 48));
        trophy.setBounds(0, 10, cardW, 70); card.add(trophy);

        JLabel winLbl = new JLabel(
            "<html><center>" + winnerText + "</center></html>", SwingConstants.CENTER);
        winLbl.setFont(new Font("Monospaced", Font.BOLD, 18));
        winLbl.setForeground(winnerColor);
        winLbl.setBounds(10, 84, cardW - 20, 36); card.add(winLbl);

        JLabel sub = new JLabel("The battle has ended!", SwingConstants.CENTER);
        sub.setFont(new Font("Monospaced", Font.PLAIN, 13));
        sub.setForeground(TEXT_SEC);
        sub.setBounds(10, 124, cardW - 20, 20); card.add(sub);

        JButton restartBtn = new JButton("▶  PLAY AGAIN  (or press ENTER)");
        restartBtn.setFont(new Font("Monospaced", Font.BOLD, 13));
        restartBtn.setForeground(new Color(15, 15, 25));
        restartBtn.setBackground(winnerColor);
        restartBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        restartBtn.setFocusPainted(false);
        restartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        restartBtn.setBounds((cardW - 320) / 2, 158, 320, 42);
        restartBtn.addActionListener(e -> { audio.playClick(); dismissGameOver(); onRestart.run(); });
        restartBtn.addMouseListener(new MouseAdapter() {
            final Color base = winnerColor;
            public void mouseEntered(MouseEvent e) { restartBtn.setBackground(base.brighter()); audio.playHover(); }
            public void mouseExited (MouseEvent e) { restartBtn.setBackground(base); }
        });
        card.add(restartBtn);

        backing.add(card);
        host.add(backing);
        host.setComponentZOrder(backing, 0);
        backing.setVisible(true);
        host.revalidate(); host.repaint();
    }

    public void repositionGameOver(int W, int H) {
        if (gameOverPanel != null) gameOverPanel.reposition(W, H);
    }

    public void dismissGameOver() {
        if (overlayTimer != null) overlayTimer.stop();
        if (gameOverPanel != null) {
            host.remove(gameOverPanel.asComponent());
            gameOverPanel = null;
        }
        showGameOverlay = false; overlayAlpha = 0f; frozenBackground = null;
        ui.setShowGameOverlay(false);
        host.revalidate(); host.repaint();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PAUSE OVERLAY
    // ═════════════════════════════════════════════════════════════════════════
    public void showPauseOverlay() {
        if (pausePanel != null) return;
        isPaused = true;
        audio.muffleMusic(true);
        ui.setIsPaused(true);

        int W = host.getWidth(), H = host.getHeight();
        final BufferedImage blurred = boxBlur(captureSnapshot(), 8);

        pausePanelAlpha = 0f;
        pausePanel = buildBlurPanel(blurred);
        pausePanel.setBounds(0, 0, W, H);

        int cardW = Math.min(340, W - 80), cardH = 180;
        JPanel card = buildDarkCard(cardW, cardH, ACCENT);
        card.setBounds((W - cardW) / 2, (H - cardH) / 2, cardW, cardH);

        JLabel icon = new JLabel("⏸", SwingConstants.CENTER);
        icon.setFont(new Font("Dialog", Font.PLAIN, 36)); icon.setForeground(TEXT_SEC);
        icon.setBounds(0, 14, cardW, 42); card.add(icon);

        JLabel title = new JLabel("GAME PAUSED", SwingConstants.CENTER);
        title.setFont(new Font("Monospaced", Font.BOLD, 20)); title.setForeground(TEXT_PRI);
        title.setBounds(10, 60, cardW - 20, 28); card.add(title);

        int btnW = 200, btnX = (cardW - btnW) / 2;
        JButton resumeBtn = new JButton("▶  RESUME");
        styleYesBtn(resumeBtn, ACCENT);
        resumeBtn.setBounds(btnX, 106, btnW, 44);
        resumeBtn.addActionListener(e -> { audio.playClick(); dismissPauseOverlay(); });
        card.add(resumeBtn);

        pausePanel.add(card);
        pushOverlay(pausePanel);
        fadeIn(pausePanelTimer = new Timer(16, null), () -> pausePanelAlpha,
               v -> { pausePanelAlpha = v; pausePanel.repaint(); }, 0.07f,
               t -> pausePanelTimer = null);
    }

    public void dismissPauseOverlay() {
        if (pausePanel == null) return;
        if (pausePanelTimer != null) pausePanelTimer.stop();
        host.remove(pausePanel); pausePanel = null; pausePanelAlpha = 0f;
        isPaused = false;
        audio.muffleMusic(false);
        ui.setIsPaused(false);
        host.revalidate(); host.repaint(); host.requestFocusInWindow();
    }

    public boolean isPaused() { return isPaused; }

    // ═════════════════════════════════════════════════════════════════════════
    // RESTART-CONFIRM OVERLAY
    // ═════════════════════════════════════════════════════════════════════════
    public void showRestartConfirm(Runnable onConfirm) {
        if (restartConfirmPanel != null) return;
        audio.muffleMusic(true);

        int W = host.getWidth(), H = host.getHeight();
        final BufferedImage blurred = boxBlur(captureSnapshot(), 6);
        restartConfirmAlpha = 0f;
        restartConfirmPanel = buildBlurPanel(blurred);
        restartConfirmPanel.setBounds(0, 0, W, H);

        int cardW = Math.min(380, W - 80), cardH = 200;
        JPanel card = buildDarkCard(cardW, cardH, BORDER);
        card.setBounds((W - cardW) / 2, (H - cardH) / 2, cardW, cardH);

        addLabel(card, "⟳", "Dialog", Font.PLAIN, 32, TEXT_SEC, 0, 14, cardW, 36);
        addLabel(card, "Restart the Battle?", "Monospaced", Font.BOLD, 16, TEXT_PRI, 10, 54, cardW - 20, 22);
        addLabel(card, "All progress will be lost.", "Monospaced", Font.PLAIN, 11, TEXT_SEC, 10, 80, cardW - 20, 16);

        int btnW = (cardW - 50) / 2, btnY = 116;
        JButton yesBtn = new JButton("⟳  YES, RESTART");
        styleYesBtn(yesBtn, SKILL_RED);
        yesBtn.setBounds(16, btnY, btnW, 38);
        yesBtn.addActionListener(e -> { audio.playClick(); dismissRestartConfirm(); onConfirm.run(); });
        card.add(yesBtn);

        JButton noBtn = makeCancelBtn("✖  CANCEL");
        noBtn.setBounds(16 + btnW + 18, btnY, btnW, 38);
        noBtn.addActionListener(e -> { audio.playClick(); dismissRestartConfirm(); });
        card.add(noBtn);

        restartConfirmPanel.add(card);
        pushOverlay(restartConfirmPanel);
        fadeIn(restartConfirmTimer = new Timer(16, null), () -> restartConfirmAlpha,
               v -> { restartConfirmAlpha = v; restartConfirmPanel.repaint(); }, 0.06f,
               t -> restartConfirmTimer = null);
    }

    public void dismissRestartConfirm() {
        if (restartConfirmPanel == null) return;
        if (restartConfirmTimer != null) restartConfirmTimer.stop();
        host.remove(restartConfirmPanel); restartConfirmPanel = null; restartConfirmAlpha = 0f;
        audio.muffleMusic(false);
        host.revalidate(); host.repaint(); host.requestFocusInWindow();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MENU-CONFIRM OVERLAY  (with volume sliders)
    // ═════════════════════════════════════════════════════════════════════════
    public void showMenuConfirm(Runnable onExitToMenu) {
        if (menuConfirmPanel != null) return;
        isPaused = true;  
        audio.muffleMusic(true);

        int W = host.getWidth(), H = host.getHeight();
        final BufferedImage blurred = boxBlur(captureSnapshot(), 6);

        confirmAlpha = 0f;
        menuConfirmPanel = buildBlurPanel(blurred);
        menuConfirmPanel.setBounds(0, 0, W, H);

        int cardW = Math.min(420, W - 80), cardH = 280;
        JPanel card = buildDarkCard(cardW, cardH, BORDER);
        card.setBounds((W - cardW) / 2, (H - cardH) / 2, cardW, cardH);

        // ── Header ────────────────────────────────────────────────────────────
        addLabel(card, "☰", "Dialog", Font.PLAIN, 28, TEXT_SEC, 0, 14, cardW, 30);
        addLabel(card, "Menu", "Monospaced", Font.BOLD, 16, TEXT_PRI, 10, 46, cardW - 20, 22);

        JSeparator div1 = new JSeparator();
        div1.setForeground(BORDER); div1.setBounds(16, 74, cardW - 32, 2); card.add(div1);

        // ── Volume sliders ────────────────────────────────────────────────────
        AudioSettings as = AudioSettings.get();

        addLabel(card, "🎵  Music", "Monospaced", Font.BOLD, 12, TEXT_SEC, 16, 82, 90, 18);
        JLabel musicVal = addLabel(card, as.getMusicVolume() + "%", "Monospaced", Font.BOLD, 12, ACCENT,
                                   cardW - 52, 82, 38, 18);
        musicVal.setHorizontalAlignment(SwingConstants.RIGHT);
        JSlider musicSlider = makeSlider(as.getMusicVolume());
        musicSlider.setBounds(16, 100, cardW - 32, 26); card.add(musicSlider);
        musicSlider.addChangeListener(ev -> {
            int v = musicSlider.getValue();
            musicVal.setText(v + "%");
            as.setMusicVolume(v);
            audio.applyMusicVolume();
        });

        addLabel(card, "🔊  SFX", "Monospaced", Font.BOLD, 12, TEXT_SEC, 16, 134, 90, 18);
        JLabel sfxVal = addLabel(card, as.getSfxVolume() + "%", "Monospaced", Font.BOLD, 12, ACCENT,
                                 cardW - 52, 134, 38, 18);
        sfxVal.setHorizontalAlignment(SwingConstants.RIGHT);
        JSlider sfxSlider = makeSlider(as.getSfxVolume());
        sfxSlider.setBounds(16, 152, cardW - 32, 26); card.add(sfxSlider);
        sfxSlider.addChangeListener(ev -> {
            int v = sfxSlider.getValue();
            sfxVal.setText(v + "%");
            as.setSfxVolume(v);
            audio.applySfxVolume();
        });

        JSeparator div2 = new JSeparator();
        div2.setForeground(BORDER); div2.setBounds(16, 186, cardW - 32, 2); card.add(div2);

        addLabel(card, "Exit to Main Menu?", "Monospaced", Font.PLAIN, 11, TEXT_SEC, 10, 194, cardW - 20, 16);

        // ── Buttons ───────────────────────────────────────────────────────────
        int btnW = (cardW - 50) / 2, btnY = 220;

        JButton exitBtn = new JButton("✔  YES, EXIT");
        styleYesBtn(exitBtn, SKILL_RED);
        exitBtn.setBounds(16, btnY, btnW, 38);
        exitBtn.addActionListener(e -> {
            audio.playClick();
            dismissMenuConfirmSilent();
            onExitToMenu.run();
        });
        card.add(exitBtn);

        JButton resumeBtn = new JButton("▶  RESUME GAME");
        styleYesBtn(resumeBtn, ACCENT);
        resumeBtn.setBounds(16 + btnW + 18, btnY, btnW, 38);
        resumeBtn.addActionListener(e -> { audio.playClick(); dismissMenuConfirm(); });
        card.add(resumeBtn);

        menuConfirmPanel.add(card);
        pushOverlay(menuConfirmPanel);
        fadeIn(confirmTimer = new Timer(16, null), () -> confirmAlpha,
               v -> { confirmAlpha = v; menuConfirmPanel.repaint(); }, 0.06f,
               t -> confirmTimer = null);
    }

    public void dismissMenuConfirm() {
        if (menuConfirmPanel == null) return;
        if (confirmTimer != null) confirmTimer.stop();
        host.remove(menuConfirmPanel); menuConfirmPanel = null; confirmAlpha = 0f;
        isPaused = false;
        audio.muffleMusic(false);
        host.revalidate(); host.repaint(); host.requestFocusInWindow();
    }
    
    public void dismissMenuConfirmSilent() {
    if (menuConfirmPanel == null) return;
    if (confirmTimer != null) confirmTimer.stop();
    isPaused = false;
    host.remove(menuConfirmPanel); menuConfirmPanel = null; confirmAlpha = 0f;
    // isPaused stays true — we're leaving anyway
    host.revalidate(); host.repaint();
}
    
    /** Dismisses every overlay at once — used by resetGame(). */
    public void dismissAll() {
        dismissMenuConfirm();
        dismissPauseOverlay();
        dismissRestartConfirm();
        dismissGameOver();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SHARED HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private void pushOverlay(JPanel panel) {
        host.add(panel);
        host.setComponentZOrder(panel, 0);
        panel.setVisible(true);
        host.revalidate(); host.repaint();
    }

    /** Builds the blurred-background overlay panel. Alpha is driven by caller. */
    private JPanel buildBlurPanel(BufferedImage blurred) {
        return new JPanel(null) {
            float getAlpha() {
                if (this == pausePanel)          return pausePanelAlpha;
                if (this == restartConfirmPanel) return restartConfirmAlpha;
                return confirmAlpha;
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                float a = getAlpha();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                g2.drawImage(blurred, 0, 0, getWidth(), getHeight(), null);
                g2.setColor(new Color(0, 0, 0, (int)(a * 160)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        };
    }

    /** A rounded dark card panel with an animated border color. */
    private JPanel buildDarkCard(int w, int h, Color borderCol) {
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 12, 22, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(borderCol); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JLabel addLabel(JPanel parent, String text, String fontName,
                            int fontStyle, int fontSize, Color fg,
                            int x, int y, int w, int h) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font(fontName, fontStyle, fontSize));
        l.setForeground(fg);
        l.setBounds(x, y, w, h);
        parent.add(l);
        return l;
    }

    private void styleYesBtn(JButton btn, Color col) {
        btn.setFont(new Font("Monospaced", Font.BOLD, 12));
        btn.setForeground(new Color(15, 15, 25)); btn.setBackground(col);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            final Color base = col;
            public void mouseEntered(MouseEvent e) { btn.setBackground(base.brighter()); audio.playHover(); }
            public void mouseExited (MouseEvent e) { btn.setBackground(base); }
        });
    }

    private JButton makeCancelBtn(String label) {
        JButton b = new JButton(label);
        b.setFont(new Font("Monospaced", Font.BOLD, 12));
        b.setForeground(TEXT_PRI); b.setBackground(BG_CARD);
        b.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(40, 40, 60)); audio.playHover(); }
            public void mouseExited (MouseEvent e) { b.setBackground(BG_CARD); }
        });
        return b;
    }

    private JSlider makeSlider(int value) {
        JSlider s = new JSlider(0, 100, value);
        s.setOpaque(false); s.setForeground(ACCENT);
        s.setPaintTicks(true); s.setMajorTickSpacing(25); s.setMinorTickSpacing(5);
        s.setPaintLabels(false); s.setFocusable(false);
        return s;
    }

    /** Generic alpha fade-in runner. Stops itself when alpha reaches 1. */
    private void fadeIn(Timer t, java.util.function.Supplier<Float> get,
                        java.util.function.Consumer<Float> set,
                        float step, java.util.function.Consumer<Timer> onDone) {
        t.addActionListener(e -> {
            float next = Math.min(1f, get.get() + step);
            set.accept(next);
            if (next >= 1f) { ((Timer) e.getSource()).stop(); onDone.accept((Timer) e.getSource()); }
        });
        t.start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SCREENSHOT + BOX BLUR
    // ═════════════════════════════════════════════════════════════════════════
    private BufferedImage captureSnapshot() {
        int W = host.getWidth(), H = host.getHeight();
        BufferedImage snap = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = snap.createGraphics();
        host.paint(sg);   // paint() is public; paintComponent() is protected
        sg.dispose();
        return snap;
    }

    private BufferedImage boxBlur(BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float[] data = new float[size * size];
        float val = 1.0f / (size * size);
        for (int i = 0; i < data.length; i++) data[i] = val;
        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        op.filter(src, dest);
        return dest;
    }
}