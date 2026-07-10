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
    public void jumpToAddress() {
        // 1NNN 	Jump to address NNN
        short address = 0x1234;
        chipVM.execute1NNN(address);
        assertTrue(chipVM.getPC() == address);

    }

    @Test
    public void executeSubroutine() {
        // 2NNN 	Execute subroutine starting at address NNN
        short address = 0x1234;
        int currentPC = chipVM.getPC();
        chipVM.execute2NNN(address);
        assertTrue(chipVM.getPC() == address);
        assertEquals(currentPC, chipVM.stackPeek());
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

    @Test
    public void jumpNotEquals() {
        // 4XNN
        int nnValue = 88;
        int currentPC = chipVM.getPC();
        chipVM.setV(0, 99);
        chipVM.execute4XNN(0, nnValue);
        assertTrue(chipVM.getPC() == currentPC + 2); // the pc will be incermented in the main loop so we only need one jump here

        currentPC = chipVM.getPC();
        chipVM.setV(0, nnValue);
        chipVM.execute4XNN(0, nnValue);
        assertTrue(chipVM.getPC() == currentPC); // value equal so no jump
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, 128, 512, 514",   // Test Reg 0 = 0 reg 1 = 128 pc = 512(0x200) 
        "4, 255, 5, 255, 512, 512"  // Test Reg 4 = 255 reg 5 = 255 pc = 512(0x200)
    })
    public void jumpNotEqualsRegister(int vx, int xValue, int vy, int yValue, short pc, int expectedPC) {
        // 9XY0 	Skip the following instruction if the value of register VX is NOT equal to the value of register VY
        chipVM.setPC(pc);
        chipVM.setV(vx, xValue);
        chipVM.setV(vy, yValue);
        chipVM.execute9XY0(vx, vy);
        assertTrue(chipVM.getPC() == expectedPC);

    }

    @ParameterizedTest
    @CsvSource({
        "0, 10, 10",   // Test Reg 0 = 10 expect the vaule in V0 to be 10 
        "4, 255, 255"  // Test Reg 4 = 255 expect the vaule in V4 to be 255
    })
    public void storeNNinRegisterx(int vx, int nnValue, int expected) {
        // 6XNN 	Store number NN in register VX
        chipVM.execute6XNN(vx, nnValue);
        assertTrue(chipVM.getV(vx) == expected);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 10, 1, 10, 512, 514",   // Test Reg 0 = 10 reg 1 = 10 pc = 512(0x200) 
        "4, 126, 5, 85, 512, 512"  // Test Reg 4 = 126 reg 5 = 85 pc = 512(0x200)
    })
    public void jumpEqualsRegister(int vx, int xValue, int vy, int yValue, short pc, int expectedPC) {
        // 9XY0 	Skip the following instruction if the value of register VX is NOT equal to the value of register VY
        chipVM.setPC(pc);
        chipVM.setV(vx, xValue);
        chipVM.setV(vy, yValue);
        chipVM.execute5XY0(vx, vy);
        assertTrue(chipVM.getPC() == expectedPC);

    }
   
}
