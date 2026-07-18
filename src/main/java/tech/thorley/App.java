package tech.thorley;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Main application to run the Chip-8 emulator.
 */
public class App {
    private static final class SwingAudioOutput implements Chip8.AudioOutput {
        private SourceDataLine line;

        SwingAudioOutput() {
            try {
                AudioFormat format = new AudioFormat(44100f, 8, 1, true, false);
                line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();
            } catch (LineUnavailableException e) {
                line = null;
            }
        }

        @Override
        public void start() {
            if (line != null) {
                byte[] buffer = new byte[1024];
                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = (byte) (Math.sin(i / 10.0) * 80);
                }
                line.write(buffer, 0, buffer.length);
            }
        }

        @Override
        public void stop() {
            if (line != null) {
                line.flush();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Chip8 chip8 = new Chip8();
            JFrame frame = new JFrame("Chip-8 Emulator");
            DisplayPanel displayPanel = new DisplayPanel(chip8);
            Chip8.AudioOutput audioOutput = new SwingAudioOutput();
            chip8.setAudioOutput(audioOutput);

            frame.setLayout(new BorderLayout());
            frame.add(displayPanel, BorderLayout.CENTER);
            frame.pack(); // Sizes the window to fit the preferred size of its subcomponents
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setResizable(false);
            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int keyIndex = KeyboardInputMapper.mapKeyCode(e.getKeyCode());
                    if (keyIndex >= 0) {
                        chip8.setKeyState(keyIndex, true);
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    int keyIndex = KeyboardInputMapper.mapKeyCode(e.getKeyCode());
                    if (keyIndex >= 0) {
                        chip8.setKeyState(keyIndex, false);
                    }
                }
            });
            frame.setFocusable(true);
            frame.requestFocusInWindow();
            frame.setVisible(true);

            // Load a ROM
            try {
                // Make sure you have this ROM file in the specified path
                chip8.loadRom(Path.of("ROMs/IBM Logo.ch8"));
            } catch (IOException e) {
                e.printStackTrace();
                // Handle error, maybe show a dialog
                return;
            }

            // Start the emulation loop in a new thread
            new Thread(() -> {
                while (true) {
                    // Execute a few cycles
                    for (int i = 0; i < 10; i++) {
                        chip8.fetchDecodeExecute();
                    }
                    // Update timers and repaint at 60Hz
                    chip8.updateTimers();
                    displayPanel.repaint();
                    try { Thread.sleep(16); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }).start();
        });
    }
}
