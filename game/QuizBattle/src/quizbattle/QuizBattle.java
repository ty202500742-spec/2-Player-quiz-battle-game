/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package quizbattle;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author End-User
 */
public class QuizBattle extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer = new JPanel(cardLayout);
    private BattlePanel battlePanel;
    private MainMenu mainMenu;  // ← keep a reference so we can control its music

    public QuizBattle() {
        setTitle("Quiz Battle - WMSU CS Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        mainMenu    = new MainMenu(this);   // ← store in field
        battlePanel = new BattlePanel(this);

        mainContainer.add(mainMenu,    "MENU");
        mainContainer.add(battlePanel, "GAME");
        add(mainContainer);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(900,  screen.width  - 40);
        int h = Math.min(780, screen.height - 60);
        setSize(w, h);
        setLocationRelativeTo(null);

        showPanel("MENU");
    }

    public void setDifficulty(String diff) {
        battlePanel.setDifficulty(diff);
    }

    public void showPanel(String name) {
        cardLayout.show(mainContainer, name);

        if (name.equals("MENU")) {
            mainMenu.resumeMenuMusic();             // ← restart MusicMenu.mp3 when returning
        }

        if (name.equals("GAME")) {
            mainContainer.getComponent(1).requestFocusInWindow();
            battlePanel.resetGame();                // ← starts MusicBattle.mp3 inside resetGame()
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
}
