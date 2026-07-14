package tech.thorley;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void loadROMfile() throws IOException {
        Chip8 chip8 = new Chip8();
        Path romPath = Path.of("ROMs/IBM Logo.ch8");

        assertTrue(Files.exists(romPath), "ROM file should exist at the project root");

        byte[] romBytes = Files.readAllBytes(romPath);
        int nonZeroByteIndex = findFirstNonZeroByteIndex(romBytes);

        assertTrue(nonZeroByteIndex >= 0, "ROM should contain at least one non-zero byte");

        chip8.loadRom(romPath);

        assertEquals(
                romBytes[nonZeroByteIndex] & 0xFF,
                chip8.getMemory()[0x200 + nonZeroByteIndex],
                "ROM bytes should be loaded into Chip-8 memory at the program start"
        );
    }

    private int findFirstNonZeroByteIndex(byte[] romBytes) {
        for (int i = 0; i < romBytes.length; i++) {
            if ((romBytes[i] & 0xFF) != 0) {
                return i;
            }
        }
        return -1;
    }
}
