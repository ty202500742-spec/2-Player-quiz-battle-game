package quizbattle;

import ddf.minim.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.swing.JPanel;

/**
 * AudioManager — owns every Minim player and all audio helpers.
 * BattlePanel creates one instance and calls the public play/stop methods.
 *
 * Usage:
 *   AudioManager audio = new AudioManager(this);  // pass the panel for Minim
 *   audio.startBattleMusic();
 *   audio.playClick();
 */
public class AudioManager {

    // ── Minim ─────────────────────────────────────────────────────────────────
    private Minim       audioLoader;
    private AudioPlayer bgMusic;
    private AudioPlayer bgVictory;
    private AudioPlayer sfxHover;
    private AudioPlayer sfxClick;
    private AudioPlayer sfxAttack;
    private AudioPlayer sfxWrong;
    private AudioPlayer sfxGameOver;
    private AudioPlayer sfxShield;
    private AudioPlayer sfxStrike;
    private AudioPlayer sfxDouble;
    private AudioPlayer sfxDrain;
    private AudioPlayer sfxCurse;
    private AudioPlayer sfxLethal;
    private AudioPlayer bgMenu;

    // ── Volume constants ──────────────────────────────────────────────────────
    private static final float MUSIC_MUFFLED_OFFSET = -20.0f;
    private boolean musicMuffled = false;

    // ── Minim needs a "sketch" host — we delegate to the owner panel ──────────
    private final JPanel host;

    // ═════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═════════════════════════════════════════════════════════════════════════
    public AudioManager(JPanel host) {
        this.host = host;
        initAudio();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MINIM HOST HELPERS  (Minim calls these reflectively)
    // ═════════════════════════════════════════════════════════════════════════
    public String sketchPath(String filename) {
        URL url = host.getClass().getResource("/" + filename);
        if (url != null) {
            try { return new File(url.toURI()).getParent(); }
            catch (Exception ignored) {}
        }
        return System.getProperty("user.dir");
    }

    public InputStream createInput(String filename) {
        InputStream is = host.getClass().getResourceAsStream("/" + filename);
        if (is != null) return is;
        try {
            File f = new File(filename);
            if (!f.exists()) f = new File(System.getProperty("user.dir"), filename);
            if (f.exists()) return new java.io.FileInputStream(f);
        } catch (Exception ignored) {}
        System.err.println("[AudioManager] Could not locate: " + filename);
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // INIT
    // ═════════════════════════════════════════════════════════════════════════
    private void initAudio() {
        try {
            audioLoader = new Minim(this);
            bgMusic     = audioLoader.loadFile("assets/audio/MusicBattle.mp3");
            sfxHover    = audioLoader.loadFile("assets/audio/Hover.mp3");
            sfxClick    = audioLoader.loadFile("assets/audio/Click.mp3");
            sfxAttack   = audioLoader.loadFile("assets/audio/Attack.mp3");
            tryLoad("assets/audio/Error.mp3",    p -> sfxWrong    = p);
            tryLoad("assets/audio/GameOver1.mp3", p -> sfxGameOver = p);
            tryLoad("assets/audio/End.mp3",       p -> bgVictory   = p);
            tryLoad("assets/audio/SfxShield.mp3", p -> sfxShield = p);
            tryLoad("assets/audio/SfxStrike.mp3", p -> sfxStrike = p);
            tryLoad("assets/audio/SfxDouble.mp3", p -> sfxDouble = p);
            tryLoad("assets/audio/SfxDrain.mp3",  p -> sfxDrain  = p);
            tryLoad("assets/audio/SfxCurse.mp3",  p -> sfxCurse  = p);
            tryLoad("assets/audio/SfxLethal.mp3", p -> sfxLethal = p);
            tryLoad("assets/audio/MusicMenu.mp3", p -> bgMenu = p);

            applyMusicVolume();
            applySfxVolume();
        } catch (Exception e) {
            System.err.println("[AudioManager] Init failed: " + e.getMessage());
        }
    }

    /** Loads a file and passes the player to a consumer; logs on failure. */
    private void tryLoad(String file, java.util.function.Consumer<AudioPlayer> setter) {
        try { setter.accept(audioLoader.loadFile(file)); }
        catch (Exception e) { System.err.println("[AudioManager] " + file + " not found."); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // VOLUME CONTROL
    // ═════════════════════════════════════════════════════════════════════════
    public void applyMusicVolume() {
        float base = AudioSettings.toGain(AudioSettings.get().getMusicVolume());
        if (bgMusic   != null) bgMusic.setGain(musicMuffled ? base + MUSIC_MUFFLED_OFFSET : base);
        if (bgVictory != null) bgVictory.setGain(base);
        if (bgMenu != null) bgMenu.setGain(base);
    }

    public void applySfxVolume() {
        float g = AudioSettings.toGain(AudioSettings.get().getSfxVolume());
        if (sfxHover    != null) sfxHover.setGain(g);
        if (sfxClick    != null) sfxClick.setGain(g);
        if (sfxAttack   != null) sfxAttack.setGain(g);
        if (sfxWrong    != null) sfxWrong.setGain(g);
        if (sfxGameOver != null) sfxGameOver.setGain(g);
        if (sfxShield  != null) sfxShield.setGain(g);
        if (sfxStrike  != null) sfxStrike.setGain(g);
        if (sfxDouble  != null) sfxDouble.setGain(g);
        if (sfxDrain   != null) sfxDrain.setGain(g);
        if (sfxCurse   != null) sfxCurse.setGain(g);
        if (sfxLethal  != null) sfxLethal.setGain(g);
    }

    public void muffleMusic(boolean muffle) {
        musicMuffled = muffle;
        if (bgMusic != null && bgMusic.isPlaying()) {
            float base = AudioSettings.toGain(AudioSettings.get().getMusicVolume());
            bgMusic.setGain(muffle ? base + MUSIC_MUFFLED_OFFSET : base);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PLAYBACK
    // ═════════════════════════════════════════════════════════════════════════
    public void startBattleMusic() {
        if (bgMusic != null) {
            bgMusic.rewind();
            bgMusic.loop();
            musicMuffled = false;
            applyMusicVolume();
        }
    }
    public void startMenuMusic() {
    if (bgMenu != null) {
        bgMenu.rewind();
        bgMenu.loop();
        applyMusicVolume();
    }
}

public void pauseMenuMusic() {
    if (bgMenu != null) bgMenu.pause();
}
    public void startVictoryMusic() {
        if (bgVictory != null) {
            bgVictory.rewind();
            bgVictory.loop();
            applyMusicVolume();
        }
    }

    public void pauseBattleMusic() {
        if (bgMusic != null) bgMusic.pause();
    }

    public void pauseVictoryMusic() {
        if (bgVictory != null) bgVictory.pause();
    }

    public void playHover() {
        playSfx(sfxHover);
    }

    public void playClick() {
        playSfx(sfxClick);
    }

    public void playAttack() {
        playSfx(sfxAttack);
    }

    public void playWrong() {
        playSfx(sfxWrong);
    }

    public void playGameOver() {
        playSfx(sfxGameOver);
    }
    
    public void playShield() { 
        playSfx(sfxShield); }
    public void playStrike() { 
        playSfx(sfxStrike); }
    public void playDouble() { 
        playSfx(sfxDouble); }
    public void playDrain() { 
        playSfx(sfxDrain); }
    public void playCurse() {
        playSfx(sfxCurse); }
    public void playLethal() { 
        playSfx(sfxLethal); }

    /** Rewinds and plays a sound-effect player at the current SFX gain. */
    private void playSfx(AudioPlayer p) {
        if (p == null) return;
        p.setGain(AudioSettings.toGain(AudioSettings.get().getSfxVolume()));
        p.rewind();
        p.play();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═════════════════════════════════════════════════════════════════════════
    public void stopAll() {
        try {
            closePlayer(bgMusic);     bgMusic     = null;
            closePlayer(bgVictory);   bgVictory   = null;
            closePlayer(sfxHover);    sfxHover    = null;
            closePlayer(sfxClick);    sfxClick    = null;
            closePlayer(sfxAttack);   sfxAttack   = null;
            closePlayer(sfxWrong);    sfxWrong    = null;
            closePlayer(sfxGameOver); sfxGameOver = null;
            closePlayer(sfxShield);  sfxShield  = null;
            closePlayer(sfxStrike);  sfxStrike  = null;
            closePlayer(sfxDouble);  sfxDouble  = null;
            closePlayer(sfxDrain);   sfxDrain   = null;
            closePlayer(sfxCurse);   sfxCurse   = null;
            closePlayer(sfxLethal);  sfxLethal  = null;
            closePlayer(bgMenu); bgMenu = null;
            if (audioLoader != null) { audioLoader.stop(); audioLoader = null; }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error releasing audio: " + e.getMessage());
        }
    }

    private void closePlayer(AudioPlayer p) {
        if (p != null) { p.pause(); p.close(); }
    }

    
    
    /** Call after stopAll() if the panel is re-shown (e.g. after returning from menu). */
    public void reinit() {
        if (audioLoader == null) initAudio();
    }
}