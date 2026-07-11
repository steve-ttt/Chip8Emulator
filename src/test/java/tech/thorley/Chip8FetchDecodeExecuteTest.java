package tech.thorley;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Chip8FetchDecodeExecuteTest {

     private Chip8 chip8;

    @BeforeEach
    public void setUp() {
        chip8 = new Chip8();
    }

    /**
     * Helper to load a 2-byte instruction at a given address.
     * Chip-8 instructions are 16-bit (2 bytes), big-endian.
     */
    private void loadInstructionAt(int address, int highByte, int lowByte) {
        chip8.setMemory(address, highByte);
        chip8.setMemory(address + 1, lowByte);
    }

    @Test
    public void testExecute_1NNN_Jump() {
        loadInstructionAt((short) 0x200, (byte) 0x12, (byte) 0x34);
        
        chip8.execute(0x1234);
        
        assertEquals(0x234, chip8.getPC());
        assertTrue(chip8.stackEmpty());
    }

    @Test
    public void testFetchDecode_2NNN_jumpSubroutine() {
        loadInstructionAt(0x200, 0x2A, 0xBC);
        
        chip8.fetchDecodeExecute();
        
        // Should execute 0NNN and jump to 0xABC without pushing to stack
        assertEquals(0xABC, chip8.getPC(), "PC should be set to 0xABC after 0NNN");
        assertFalse(chip8.stackEmpty(), "Stack should contain return value");
        assertEquals(0x202, chip8.stackPeek(), "Stack should contain PC value to return to");
    } 

    @Test
    public void fetchDecode_3XNN_conditionalJUmp() {
        loadInstructionAt(0x200, 0x31, 0xAA);
        chip8.setV(1, 0xAA);
        chip8.fetchDecodeExecute();

        assertEquals(0x204, chip8.getPC());

        chip8.setPC(0x200);
        loadInstructionAt(0x200, 0x31, 0xAA);
        chip8.setV(1, 0xBB);
        chip8.fetchDecodeExecute();

        assertEquals(0x202, chip8.getPC());

    }

    @Test
    public void fetchDecode_4XNN_conditionalJump() {
        // 4XNN 	Skip the following instruction if the value of register VX is not equal to NN
        loadInstructionAt(0x200, 0x41, 0xAA);
        chip8.setV(1, 0xAA);
        chip8.fetchDecodeExecute();

        assertEquals(0x202, chip8.getPC());

        chip8.setPC(0x200);
        loadInstructionAt(0x200, 0x41, 0xAA);
        chip8.setV(1, 0xBB);
        chip8.fetchDecodeExecute();

        assertEquals(0x204, chip8.getPC());

    }

    @Test
    public void fetchDecode_5XY0_conditionalJump() {
        // 5XY0 	Skip the following instruction if the value of register VX is equal to the value of register VY
        loadInstructionAt(0x200, 0x50, 0x40);
        chip8.setV(0, 0xAA);
        chip8.setV(4, 0xAA);
        chip8.fetchDecodeExecute();

        assertEquals(0x204, chip8.getPC());

        chip8.setPC(0x200);
        loadInstructionAt(0x200, 0x50, 0x40);
        chip8.setV(0, 0xAA);
        chip8.setV(4, 0xBB);
        chip8.fetchDecodeExecute();

        assertEquals(0x202, chip8.getPC());

    }

    @Test
    public void fetchDecode_6XNN_storeValue() {
        // 6XNN 	Store number NN in register VX
        loadInstructionAt(0x200, 0x61, 0xAA);
        chip8.fetchDecodeExecute();

        assertEquals(0xAA, chip8.getV(1));
    }

    @Test
    public void fetchDecode_7XNN_add() {
        // 7XNN 	Add the value NN to register VX
        loadInstructionAt(0x200, 0x70, 0x05);
        chip8.setV(0, 10);
        chip8.fetchDecodeExecute();

        assertEquals(15, chip8.getV(0));
    }

    @Test
    public void fetchDecode_00EE_returnFromSubroutine() {
        loadInstructionAt(0x200, 0x00, 0xEE);
        chip8.stackPush(0x202);
        chip8.fetchDecodeExecute();

        assertEquals(0x202, chip8.getPC());
        assertTrue(chip8.stackEmpty());
    }

    @Test
    public void clearScreen_00E0() {
        // 00E0 	Clear the display
        loadInstructionAt(0x200, 0x00, 0xE0);
        
        // fill the screen 
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                chip8.setDisplayBit(x, y, true);
            }
        }

        chip8.fetchDecodeExecute();

        // check all bits in the screen buffer are false
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                assertFalse(chip8.getDisplayBit(x, y));
            }
        }
    }

    @Test
    public void fetchDecode_8XY0_StoreValueVyinVx() {
        //8XY0 	Store the value of register VY in register VX
        loadInstructionAt(0x200, 0x80, 0x10);
        chip8.setV(1, 0xAA);
        chip8.setV(0, 0x00); // just to be sure 
        chip8.fetchDecodeExecute();

        assertEquals(0xAA, chip8.getV(0));
     
    }
}
