package quizbattle;

import javax.swing.*;
import java.awt.*;

public class QuizBattle extends JFrame {
     private AudioManager audioManager;
    
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer = new JPanel(cardLayout);
    private BattlePanel battlePanel;
    private MainMenu mainMenu;  // ← keep a reference so we can control its music

    public QuizBattle() {
    setTitle("Quiz Battle");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setResizable(false);

    mainMenu     = new MainMenu(this);
    audioManager = new AudioManager(mainMenu);
    battlePanel  = new BattlePanel(this, audioManager);

    // Give MainMenu its audio reference BEFORE showPanel is called
    mainMenu.setAudioManager(audioManager);

    mainContainer.add(mainMenu,    "MENU");
    mainContainer.add(battlePanel, "GAME");
    add(mainContainer);

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int w = Math.min(900,  screen.width  - 40);
    int h = Math.min(780, screen.height - 60);
    setSize(w, h);
    setLocationRelativeTo(null);

    showPanel("MENU"); // now safe — audioManager is wired up
}

    public void setDifficulty(String diff) {
        battlePanel.setDifficulty(diff);
    }

    public void showPanel(String name) {
    cardLayout.show(mainContainer, name);
    if (name.equals("MENU")) {
         battlePanel.pauseTimers(); 
        audioManager.startMenuMusic();   
    }
    if (name.equals("GAME")) {
        audioManager.pauseMenuMusic();   // ← stop menu music when game starts
        mainContainer.getComponent(1).requestFocusInWindow();
        battlePanel.resetGame();
    }
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            new QuizBattle().setVisible(true);
        });
    }
    
    public void setGameMode(GameState.Mode mode, String botDiff) {
    battlePanel.setGameMode(mode, botDiff);
    }
    
}