package tech.thorley;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ControlFLowTest {

    private Chip8 chipVM;

    @BeforeEach
    public void setUp() {
        this.chipVM = new Chip8();
    }

    /*  1NNN 	Jump to address NNN
        2NNN 	Execute subroutine starting at address NNN        
        3XNN 	Skip the following instruction if the value of register VX equals NN
        4XNN 	Skip the following instruction if the value of register VX is not equal to NN
        5XY0 	Skip the following instruction if the value of register VX is equal to the value of register VY
        6XNN 	Store number NN in register VX
        9XY0 	Skip the following instruction if the value of register VX is not equal to the value of register VY
    */

    @Test
    public void incrementPC() {
        int currentPC = chipVM.getPC();
        chipVM.incrementPC();
        assertTrue(chipVM.getPC() == currentPC + 2); // every instruction is 2 bytes long
    }

    @Test
    public void systemCall() {
        // 0NNN 	Jump to address NNN
        short address = 0x0ABC;
        chipVM.execute0NNN(address);
        assertTrue(chipVM.getPC() == address);
        assertTrue(chipVM.stackEmpty());
    }

    @Test 
    public void returnFromSubroutine() {
        // 00EE 	Return from a subroutine
        int currentPC = chipVM.getPC();
        chipVM.stackPush(currentPC);
        chipVM.setPC((short)0x1234);
        chipVM.execute00EE();
        assertTrue(chipVM.getPC() == currentPC);
    }

    @Test
    public void executeSubroutine() {
        // 2NNN 	Execute subroutine starting at address NNN
        int opcode = 0x2123;
        int address = 0x123;
        int currentPC = chipVM.getPC();
        chipVM.execute2NNN(opcode);
        assertTrue(chipVM.getPC() == address);
        assertEquals(currentPC + 2, chipVM.stackPeek());
    }

    @Test
    public void jumpEquals() {
        // 3XNN
        int opcode = 0x3088;
        int nnValue = 0x88;
        
        chipVM.setV(0, nnValue);
        chipVM.execute3XNN(opcode);
        assertEquals(0x204, chipVM.getPC(), "Expect next instruction to be skipped"); 
        
        chipVM.setPC(0x200);
        chipVM.setV(0, 99);
        chipVM.execute3XNN(opcode);
        assertEquals(0x202, chipVM.getPC(), "Expect PC to point to the next instruction to be executed"); 
        // value not equal so no jump
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, 128, 512, 516",   // Not equal: PC increments by 2 (for instruction) + 2 (for skip) = 4. 512 -> 516
        "4, 255, 5, 255, 512, 514"  // Equal: PC increments by 2 (for instruction). 512 -> 514
    })
    public void jumpNotEqualsRegister(int vx, int xValue, int vy, int yValue, short pc, int expectedPC) {
        int opcode = 0x9000 | (vx << 8) | (vy << 4);
        // 9XY0 	Skip the following instruction if the value of register VX is NOT equal to the value of register VY
        chipVM.setPC(pc);
        chipVM.setV(vx, xValue);
        chipVM.setV(vy, yValue);
        chipVM.execute9XY0(opcode);
        assertTrue(chipVM.getPC() == expectedPC);

    }

    @Test
    public void storeNNinRegisterx() {
        // 6XNN 	Store number NN in register VX
        int vx = 0;
        int nnValue = 0xAA;
        int expected = 0xAA;
        int opcode = 0x60AA;
        chipVM.setV(vx, nnValue);
        chipVM.execute6XNN(opcode);
        assertTrue(chipVM.getV(vx) == expected);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 10, 4, 10, 512, 516",   // Equal: PC increments by 2 (for instruction) + 2 (for skip) = 4. 512 -> 516
        "0, 126, 4, 85, 512, 514"  // Not equal: PC increments by 2 (for instruction). 512 -> 514
    })
    public void jumpEqualsRegister(int vx, int xValue, int vy, int yValue, short pc, int expectedPC) {
        // 5XY0 	Skip the following instruction if the value of register VX is  equal to the value of register VY
        int opcode = 0x5040;
        chipVM.setPC(pc);
        chipVM.setV(vx, xValue);
        chipVM.setV(vy, yValue);
        chipVM.execute5XY0(opcode);
        assertEquals(expectedPC, chipVM.getPC(), "Expect next instruction to be skipped");

    }
   
}
