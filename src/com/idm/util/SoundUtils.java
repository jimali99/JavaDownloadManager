package com.idm.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.awt.Toolkit;

public class SoundUtils {

    public static void playCompleteSound() {
        new Thread(() -> {
            try {
                // Generate a pleasant two-tone chime
                float sampleRate = 44100;
                byte[] buf = new byte[44100]; // 1 second buffer
                AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();

                // Tone 1: 587.33 Hz (D5) for 150ms
                playTone(sdl, 587.33, 150, sampleRate);
                // Short silence 20ms
                Thread.sleep(20);
                // Tone 2: 880.00 Hz (A5) for 250ms
                playTone(sdl, 880.00, 250, sampleRate);

                sdl.drain();
                sdl.stop();
                sdl.close();
            } catch (Exception e) {
                // Fallback to system beep
                try {
                    Toolkit.getDefaultToolkit().beep();
                } catch (Exception ignored) {}
            }
        }, "SoundPlayerThread").start();
    }

    private static void playTone(SourceDataLine sdl, double freq, int durationMs, float sampleRate) {
        int length = (int) (sampleRate * (durationMs / 1000.0));
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            double angle = 2.0 * Math.PI * i / (sampleRate / freq);
            // Apply a fast envelope fade-out at the end to avoid clicks
            double envelope = 1.0;
            if (i > length - 500) {
                envelope = (double) (length - i) / 500.0;
            } else if (i < 200) {
                envelope = (double) i / 200.0;
            }
            buffer[i] = (byte) (Math.sin(angle) * 100 * envelope);
        }
        sdl.write(buffer, 0, buffer.length);
    }
}
