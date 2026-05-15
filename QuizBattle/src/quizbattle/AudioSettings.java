/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// AudioSettings.java
package quizbattle;

/**
 * Singleton that holds the current music and SFX gain values.
 * Both MainMenu and BattlePanel read/write here so volumes are kept in sync.
 *
 * Gain range: -40.0f (almost silent) to 0.0f (full volume).
 * We expose a 0-100 integer "volume" that maps linearly to that range.
 */
public class AudioSettings {

    private static AudioSettings instance;

    // Volume 0-100 (int), default = 80
    private int musicVolume = 80;
    private int sfxVolume   = 80;

    private AudioSettings() {}

    public static AudioSettings get() {
        if (instance == null) instance = new AudioSettings();
        return instance;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int getMusicVolume() { return musicVolume; }
    public int getSfxVolume()   { return sfxVolume;   }

    public void setMusicVolume(int v) { musicVolume = clamp(v); }
    public void setSfxVolume  (int v) { sfxVolume   = clamp(v); }

    /**
     * Convert a 0-100 int volume to a Minim gain value (dB).
     * vol=100 → 0 dB (full),  vol=0 → -80 dB (silent).
     */
   public static float toGain(int vol) {
    if (vol <= 0) return -80f;

    
    return (float)(20f * Math.log10(vol / 100.0));
}

    private int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
