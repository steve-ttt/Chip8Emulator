package tech.thorley;

import java.lang.reflect.Field;

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

    private void setIndexRegister(int value) {
        try {
            Field field = Chip8.class.getDeclaredField("indexRegister");
            field.setAccessible(true);
            field.setInt(chip8, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getIndexRegister() {
        try {
            Field field = Chip8.class.getDeclaredField("indexRegister");
            field.setAccessible(true);
            return field.getInt(chip8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setKeyState(int key, boolean pressed) {
        try {
            Field field = Chip8.class.getDeclaredField("keypad");
            field.setAccessible(true);
            boolean[] keypad = (boolean[]) field.get(chip8);
            keypad[key] = pressed;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setDelayTimer(int value) {
        try {
            Field field = Chip8.class.getDeclaredField("delayTimer");
            field.setAccessible(true);
            field.setInt(chip8, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setSoundTimer(int value) {
        try {
            Field field = Chip8.class.getDeclaredField("soundTimer");
            field.setAccessible(true);
            field.setInt(chip8, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    private int getDelayTimer() {
        try {
            Field field = Chip8.class.getDeclaredField("delayTimer");
            field.setAccessible(true);
            return field.getInt(chip8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getSoundTimer() {
        try {
            Field field = Chip8.class.getDeclaredField("soundTimer");
            field.setAccessible(true);
            return field.getInt(chip8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    public void clearScreen_00E0_advancesProgramCounter() {
        loadInstructionAt(0x200, 0x00, 0xE0);

        chip8.fetchDecodeExecute();

        assertEquals(0x202, chip8.getPC());
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

    @Test
    public void fetchDecode_8XY1_BitwiseOR() {
        // 8XY1: Bitwise OR
        // VX is set to the bitwise/binary logical disjunction (OR) of VX and VY. VY is not affected.
        loadInstructionAt(0x200, 0x80, 0x11);
        chip8.setV(0, 0x01);
        chip8.setV(1, 0x80);
        chip8.fetchDecodeExecute();

        assertEquals(0x81, chip8.getV(0));
        assertEquals(0x80, chip8.getV(1));

    }

    @Test
    public void fetchDecode_8XY2_BitwiseAND() {
        // 8XY2: Binary AND
        // VX is set to the bitwise/binary logical conjunction (AND) of VX and VY. VY is not affected.
        loadInstructionAt(0x200, 0x80, 0x12);
        chip8.setV(0, 0xF0);
        chip8.setV(1, 0x88);

        chip8.fetchDecodeExecute();

        assertEquals(0x80, chip8.getV(0));
        assertEquals(0x88, chip8.getV(1));

    }

    @Test
    public void fetchDecode_8XY3_BitwiseXOR() {
        // 8XY3: Logical XOR
        // VX is set to the bitwise/binary exclusive OR (XOR) of VX and VY. VY is not affected.
        loadInstructionAt(0x200, 0x80, 0x13);
        chip8.setV(0, 0xF0);
        chip8.setV(1, 0x88);

        chip8.fetchDecodeExecute();

        assertEquals(0x78, chip8.getV(0));
        assertEquals(0x88, chip8.getV(1));

    }

    @Test
    public void fetchDecode_8XY4_Add() {    
        // 8XY4: Add
        // VX is set to the value of VX plus the value of VY. VY is not affected.
        // Unlike 7XNN, this addition will affect the carry flag. If the result is larger than 255
        // (and thus overflows the 8-bit register VX), the flag register VF is set to 1
        loadInstructionAt(0x200, 0x80, 0x14);
        chip8.setV(0, 0xFF);
        chip8.setV(1, 0x01);

        chip8.fetchDecodeExecute();

        assertEquals(0x00, chip8.getV(0));
        assertEquals(0x01, chip8.getV(1));
        assertEquals(1, chip8.getV(0xF));

    }

    @Test 
    public void fetchDecode_8XY5_Subtract() {
        // 8XY5: Subtract
        // VX is set to VX minus VY. VY is not affected 
        // Set VF to 01 if a borrow does not occur
        loadInstructionAt(0x200, 0x80, 0x15);
        chip8.setV(0, 0xFF);
        chip8.setV(1, 0x01);

        chip8.fetchDecodeExecute();

        assertEquals(0xFE, chip8.getV(0));
        assertEquals(0x01, chip8.getV(1));
        assertEquals(1, chip8.getV(0xF));

    }

    @Test
    public void fetchDecode_8XY6_ShiftRight() {
        // 8XY6: Shift Right    
        loadInstructionAt(0x200, 0x80, 0x16);
        chip8.setV(1, 0x80);

        chip8.fetchDecodeExecute();

        assertEquals(0x40, chip8.getV(0));
        assertEquals(0, chip8.getV(0xF));

    }

    @Test
    public void fetchDecode_8XY7_Subtract() {
        // 8XY7: Subtract
        // VX is set to VY minus VX. VY is not affected 
        // Set VF to 01 if a borrow does not occur
        loadInstructionAt(0x200, 0x80, 0x17);
        chip8.setV(1, 0xFF);
        chip8.setV(0, 0x01);

        chip8.fetchDecodeExecute(); 

        assertEquals(0xFE, chip8.getV(0));
        assertEquals(0xFF, chip8.getV(1));
        assertEquals(1, chip8.getV(0xF));

    }


    @Test
    public void fetchDecode_8XYE_ShiftLeft() {
        // 8XYE: Shift Left
        loadInstructionAt(0x200, 0x80, 0x1E);
        chip8.setV(1, 0xF0);

        chip8.fetchDecodeExecute();

        assertEquals(0xE0, chip8.getV(0));
        assertEquals(1, chip8.getV(0xF));

    }

    
    @Test
    public void fetchDecode_ANNN() {
        // ANNN 	Store memory address NNN in register I
        loadInstructionAt(0x200, 0xA1, 0x23);

        chip8.fetchDecodeExecute();

        assertEquals(0x123, getIndexRegister());
    }

    @Test
    public void fetchDecode_BNNN() {
        //BNNN 	Jump to address NNN + V0
        loadInstructionAt(0x200, 0xB1, 0x23);
        chip8.setV(0, 0x05);

        chip8.fetchDecodeExecute();

        assertEquals(0x128, chip8.getPC());
    }

    @Test
    public void fetchDecode_CXNN() {
        //CXNN 	Set VX to a random number with a mask of NN
        loadInstructionAt(0x200, 0xC1, 0x0F);
        chip8.setV(1, 0x55);

        chip8.fetchDecodeExecute();

        assertTrue(chip8.getV(1) >= 0x00);
        assertTrue(chip8.getV(1) <= 0x0F);
    }
    
    @Test
    public void fetchDecode_DXYN_drawSprite() {
        loadInstructionAt(0x200, 0xD0, 0x11);
        chip8.setV(0, 0x00);
        chip8.setV(1, 0x00);
        setIndexRegister(0x300);
        chip8.setMemory(0x300, 0x80);

        chip8.fetchDecodeExecute();

        assertTrue(chip8.getDisplayBit(0, 0));
        assertFalse(chip8.getDisplayBit(1, 0));
    }

    @Test
    public void fetchDecode_EX9E_skipIfKeyPressed() {
        // EX9E 	Skip the following instruction if the key corresponding to the hex value currently stored in register VX is pressed
        loadInstructionAt(0x200, 0xE0, 0x9E);
        chip8.setV(0, 0x0A);
        setKeyState(0x0A, true);

        chip8.fetchDecodeExecute();

        assertEquals(0x204, chip8.getPC());
    }

    @Test
    public void fetchDecode_EXA1_skipIfKeyNotPressed() {
        // EXA1 	Skip the following instruction if the key corresponding to the hex value currently stored in register VX is not pressed
        loadInstructionAt(0x200, 0xE0, 0xA1);
        chip8.setV(0, 0x0A);
        setKeyState(0x0A, false);

        chip8.fetchDecodeExecute();

        assertEquals(0x204, chip8.getPC());
    }

    @Test
    public void fetchDecode_FX07_storeDelayTimer() {
        // FX07 	Store the current value of the delay timer in register VX
        loadInstructionAt(0x200, 0xF0, 0x07);
        try {
            Field field = Chip8.class.getDeclaredField("delayTimer");
            field.setAccessible(true);
            field.setInt(chip8, 0x3C);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        chip8.fetchDecodeExecute();

        assertEquals(0x3C, chip8.getV(0));
    }

    @Test
    public void fetchDecode_FX0A_waitForKeyPress() {
        // FX0A 	Wait for a keypress and store the result in register VX
        loadInstructionAt(0x200, 0xF0, 0x0A);
        setKeyState(0x0A, true);

        chip8.fetchDecodeExecute();

        assertEquals(0x0A, chip8.getV(0));
    }

    @Test
    public void fetchDecode_FX15_setDelayTimer() {
        // FX15 	Set the delay timer to the value of register VX
        loadInstructionAt(0x200, 0xF0, 0x15);
        chip8.setV(0, 0x2A);

        chip8.fetchDecodeExecute();

        assertEquals(0x2A, getDelayTimer());
    }

    @Test
    public void fetchDecode_FX18_setSoundTimer() {
        // FX18 	Set the sound timer to the value of register VX
        loadInstructionAt(0x200, 0xF0, 0x18);
        chip8.setV(0, 0x1E);

        chip8.fetchDecodeExecute();

        assertEquals(0x1E, getSoundTimer());
    }

    @Test
    public void fetchDecode_FX1E_addToIndexRegister() {
        // FX1E 	Add the value stored in register VX to register I
        loadInstructionAt(0x200, 0xF0, 0x1E);
        chip8.setV(0, 0x05);
        setIndexRegister(0x100);

        chip8.fetchDecodeExecute();

        assertEquals(0x105, getIndexRegister());
    }

    @Test
    public void fetchDecode_FX29_setIndexRegisterToFontAddress() {
        // FX29 	Set I to the memory address of the sprite data corresponding to the hexadecimal digit stored in register VX
        loadInstructionAt(0x200, 0xF0, 0x29);
        chip8.setV(0, 0x0A);

        chip8.fetchDecodeExecute();

        assertEquals(0x82, getIndexRegister());
    }

    @Test
    public void fetchDecode_FX33_storeBCD() {
        // FX33 	Store the BCD equivalent of the value stored in register VX at addresses I, I + 1, and I + 2
        loadInstructionAt(0x200, 0xF0, 0x33);
        chip8.setV(0, 0x7B);
        setIndexRegister(0x300);

        chip8.fetchDecodeExecute();

        assertEquals(0x01, chip8.getMemory()[0x300]);
        assertEquals(0x02, chip8.getMemory()[0x301]);
        assertEquals(0x03, chip8.getMemory()[0x302]);
    }

    @Test
    public void fetchDecode_FX55_storeRegistersToMemory() {
        // FX55 	Store the values of registers V0 to VX inclusive in memory starting at address I
        loadInstructionAt(0x200, 0xF2, 0x55);
        chip8.setV(0, 0x01);
        chip8.setV(1, 0x02);
        chip8.setV(2, 0x03);
        setIndexRegister(0x300);

        chip8.fetchDecodeExecute();

        assertEquals(0x01, chip8.getMemory()[0x300]);
        assertEquals(0x02, chip8.getMemory()[0x301]);
        assertEquals(0x03, chip8.getMemory()[0x302]);
        assertEquals(0x303, getIndexRegister());
    }

    @Test
    public void fetchDecode_FX65_loadRegistersFromMemory() {
        // FX65 	Fill registers V0 to VX inclusive with the values stored in memory starting at address I
        loadInstructionAt(0x200, 0xF2, 0x65);
        chip8.setMemory(0x300, 0x01);
        chip8.setMemory(0x301, 0x02);
        chip8.setMemory(0x302, 0x03);
        setIndexRegister(0x300);

        chip8.fetchDecodeExecute();

        assertEquals(0x01, chip8.getV(0));
        assertEquals(0x02, chip8.getV(1));
        assertEquals(0x03, chip8.getV(2));
        assertEquals(0x303, getIndexRegister());
    }

    @Test
    public void testUpdateTimers_decrementsWhenPositive() {
        setDelayTimer(10);
        setSoundTimer(5);

        chip8.updateTimers();

        assertEquals(9, getDelayTimer(), "Delay timer should decrement by 1");
        assertEquals(4, getSoundTimer(), "Sound timer should decrement by 1");
    }

    @Test
    public void testUpdateTimers_doesNotGoBelowZero() {
        setDelayTimer(0);
        setSoundTimer(0);

        chip8.updateTimers();

        assertEquals(0, getDelayTimer(), "Delay timer should not go below 0");
        assertEquals(0, getSoundTimer(), "Sound timer should not go below 0");
    }

    @Test
    public void testUpdateTimers_stopsAtZero() {
        setDelayTimer(1);
        setSoundTimer(1);

        chip8.updateTimers();

        assertEquals(0, getDelayTimer(), "Delay timer should stop at 0");
        assertEquals(0, getSoundTimer(), "Sound timer should stop at 0");
    }
}
