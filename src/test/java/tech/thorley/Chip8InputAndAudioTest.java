package tech.thorley;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;

import org.junit.jupiter.api.Test;

class Chip8InputAndAudioTest {

    @Test
    void keypadStateCanBeToggled() {
        Chip8 chip8 = new Chip8();

        chip8.setKeyState(0xA, true);
        assertTrue(chip8.isKeyPressed(0xA));

        chip8.setKeyState(0xA, false);
        assertFalse(chip8.isKeyPressed(0xA));
    }

    @Test
    void keyboardMappingUsesChip8Layout() {
        assertEquals(0x1, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_1));
        assertEquals(0x2, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_2));
        assertEquals(0x3, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_3));
        assertEquals(0xC, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_4));
        assertEquals(0x4, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_Q));
        assertEquals(0x5, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_W));
        assertEquals(0x6, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_E));
        assertEquals(0xD, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_R));
        assertEquals(0x7, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_A));
        assertEquals(0x8, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_S));
        assertEquals(0x9, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_D));
        assertEquals(0xE, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_F));
        assertEquals(0xA, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_Z));
        assertEquals(0x0, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_X));
        assertEquals(0xB, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_C));
        assertEquals(0xF, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_V));
        assertEquals(-1, KeyboardInputMapper.mapKeyCode(KeyEvent.VK_ESCAPE));
    }

    @Test
    void soundOutputIsStartedAndStoppedWithTimer() {
        RecordingAudioOutput audio = new RecordingAudioOutput();
        Chip8 chip8 = new Chip8(audio);

        chip8.setSoundTimer(2);
        chip8.updateTimers();

        assertTrue(audio.started);
        assertEquals(1, audio.startCount);
        assertEquals(1, chip8.getSoundTimer());

        chip8.updateTimers();

        assertFalse(audio.started);
        assertEquals(1, audio.stopCount);
        assertEquals(0, chip8.getSoundTimer());
    }

    private static final class RecordingAudioOutput implements Chip8.AudioOutput {
        private boolean started;
        private int startCount;
        private int stopCount;

        @Override
        public void start() {
            this.started = true;
            this.startCount++;
        }

        @Override
        public void stop() {
            this.started = false;
            this.stopCount++;
        }
    }
}
