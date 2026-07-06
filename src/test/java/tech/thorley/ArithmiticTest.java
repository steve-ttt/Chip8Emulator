package tech.thorley;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/* 
   The first CHIP-8 interpreter (on the COSMAC VIP computer) was also located in RAM, from address 000 to 1FF. 
   It would expect a CHIP-8 program to be loaded into memory after it, starting at address 200 (512 in decimal).
   Although modern interpreters are not in the same memory space, you should do the same to be able to run 
   the old programs; you can just leave the initial space empty, except for the font.
*/

public class ArithmiticTest {

    private Chip8 chipVM;

    @BeforeEach
    public void setUp() {
        this.chipVM = new Chip8();
    }

   
    @Test
    public void testPCInitializedto0x200() {
        // the chip-8 program counter starts at 0x200.
        assertEquals(0x0200, chipVM.getPC(), "Expected 0x0200 the start of program space");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 15}) // just pick a few indices
    public void testSpecificRegistersInitializedToZero(int n) {
        assertEquals(0, chipVM.getV(n), "Expected register to return data " + n);
    }

    @Test
    public void testDataRegisterOutofBoundsThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> chipVM.getV(88), "Register index out of bounds: 88");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 10",   // Test Reg 0 with value 10
        "4, 126",  // Test Reg 4 with value 126
        "1, 255",  // Test Reg 1 with value 255
        "15, 1"    // Test Reg 15 with value 0
    })
    public void addValueToRegister(int index, int value) {
        chipVM.setV(index, (byte)value);
        assertEquals(value, chipVM.getV(index));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 10, 2",   // VX=0, Value=10, VY=2
        "4, 126, 8",  // VX=4, Value=126, VY=8
        "1, 255, 3"   // VX=1, Value=255, VY=3       
    })
    public void testSetVnRegister(int vx, int value, int vy) { 
        // The instruction 8XY0: Set VX to the value of VY.

        chipVM.setV(vy, value); 
        chipVM.execute8XY0(vx, vy); 
        assertEquals(value, chipVM.getV(vx), 
            "Value " + value + " should have moved from V" + vy + " to V" + vx);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1, 1, 128, 129",   // VX=0, Value=0x01 (0000 0001), VY=1, Value=0x80 (1000 0000)
        "4, 170, 5, 85, 255",  // VX=4, Value=0xAA (1010 1010), VY=5, Value=0x55 (0101 0101)
        "1, 255, 2, 1, 255"   // VX=1, Value=0xFF (1111 1111), VY=2, Value=0x01 (0000 0001)       
    })
    public void bitwiseORvalueAtVxVy(int vx, int xValue, int vy, int yValue, int expected) { 
        // 8XY1: Bitwise OR
        // VX is set to the bitwise/binary logical disjunction (OR) of VX and VY. VY is not affected.
        chipVM.setV(vy, yValue);
        chipVM.setV(vx, xValue);
        chipVM.execute8XY1(vx, vy);
        assertEquals(expected, chipVM.getV(vx), 
            "Vx Value: " + vx + " should be bitwise OR to Vy value: " + vy + " resulting in: " + expected);
                    // Assert VY was NOT mutated
        assertEquals(yValue, chipVM.getV(vy), 
            "Vy should not be affected by the AND operation");

    }

    @ParameterizedTest
    @CsvSource({
        "0, 0,   1, 85,  0",  // 0x00 & 0x55 = 0x00
        "4, 255, 5, 85,  85", // 0xFF & 0x55 = 0x55
        "1, 240, 2, 85,  80"  // 0xF0 & 0x55 = 0x50 (80 in decimal) masks bottom nybble 
    })
    public void bitwiseANDvalueAtVxVy(int vx, int xValue, int vy, int yValue, int expected) { 
        //  8XY2: Binary AND
        //  VX is set to the bitwise/binary logical conjunction (AND) of VX and VY. VY is not affected.
        chipVM.setV(vy, yValue);
        chipVM.setV(vx, xValue);
        chipVM.execute8XY2(vx, vy);
        
        // Assert VX changed correctly
        assertEquals(expected, chipVM.getV(vx), 
            "Vx should be set to the bitwise AND of initial Vx and Vy");
            
        // Assert VY was NOT mutated
        assertEquals(yValue, chipVM.getV(vy), 
            "Vy should not be affected by the AND operation");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 255, 1, 255,  0",   // 0xFF XOR 0xFF = 0x00
        "4, 170, 5, 0,  170",   // 0xAA ^ 0x00 = 0xAA Zero masking
        "1, 170, 2, 85, 255",   // 0xAA ^ 0x55 = 0xFF every bit is different so every bit should flip to 1
        "2, 60 , 3, 24, 36"     // 0x3C ^ 0x18 = 0x24
    })
    public void bitwiseXORvalueAtVxVy(int vx, int xValue, int vy, int yValue, int expected) { 
        // 8XY3: Logical XOR
        // VX is set to the bitwise/binary exclusive OR (XOR) of VX and VY. VY is not affected.
        chipVM.setV(vy, yValue);
        chipVM.setV(vx, xValue);
        chipVM.execute8XY3(vx, vy);
        
        // Assert VX changed correctly
        assertEquals(expected, chipVM.getV(vx), 
            "Vx should be set to the bitwise XOR of initial Vx and Vy");
            
        // Assert VY was NOT mutated
        assertEquals(yValue, chipVM.getV(vy), 
            "Vy should not be affected by the XOR operation");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 255, 1, 1,  0, 1",   // add 255 and 1 resutl 0 carry 1
        "4, 128, 5, 0,  128, 0",   // add 128 and 0 result 128 no carry 
        "1, 0, 2, 0, 0, 0"   // add 0 and 0 result 0 no carry
    })
    public void addvalueAtVxVy(int vx, int xValue, int vy, int yValue, int expected, int carry) { 
        /* 8XY4: Add
        VX is set to the value of VX plus the value of VY. VY is not affected.
        Unlike 7XNN, this addition will affect the carry flag. If the result is larger than 255 (and thus overflows the 8-bit register VX), 
        the flag register VF is set to 1. If it doesn’t overflow, VF is set to 0. */
        chipVM.setV(vy, yValue);
        chipVM.setV(vx, xValue);
        chipVM.execute8XY4(vx, vy);
        
        // Assert VX changed correctly
        assertEquals(expected, chipVM.getV(vx), 
            "Vx should be set to addition of Vx and Vy");
        assertEquals(carry, chipVM.getV(0x0F), 
                    "The carry bit should be set to: " + carry + " for the addition of " + xValue + " and " + yValue);    
        // Assert VY was NOT mutated
        assertEquals(yValue, chipVM.getV(vy), 
            "Vy should not be affected by the addition operation");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, 1,  255, 0",   // 0 - 1 = 255 (VF=0)
        "4, 128, 5, 0, 128, 1",  // 128 - 0 = 128 (VF=1)
        "1, 69, 2, 42, 27, 1"    // 69 - 42 = 27 (VF=1)
    })
    public void subtractxy5(int vx, int xValue, int vy, int yValue, int expected, int borrow) {
        runSubtractionTest(vx, xValue, vy, yValue, expected, borrow, true);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, 1,  1, 1",     // 1 - 0 = 1 (VF=1)
        "4, 128, 5, 0, 128, 0",  // 0 - 128 = 128 (VF=0)
        "1, 69, 2, 42, 229, 0"    // 42 - 69 = 229 (VF=0)
    })
    public void subtractxy7(int vx, int xValue, int vy, int yValue, int expected, int borrow) {
        runSubtractionTest(vx, xValue, vy, yValue, expected, borrow, false);
    }

    // Private helper to avoid code duplication
    private void runSubtractionTest(int vx, int xValue, int vy, int yValue, int expected, int borrow, boolean isXY5) {
        /*         
        8XY5 and 8XY7: Subtract
        These both subtract the value in one register from the other, and put the result in VX. In both cases, VY is not affected.
        8XY5 sets VX to the result of VX - VY.
        8XY7 sets VX to the result of VY - VX.
        This subtraction will also affect the carry flag, but note that it’s opposite from what you might think. 
        If the minuend (the first operand) is larger than or equal to the subtrahend (second operand), VF will be set to 1. 
        If the subtrahend is larger, and we “underflow” the result, VF is set to 0. Another way of thinking of it is that VF 
        is set to 1 before the subtraction, and then the subtraction either borrows from VF (setting it to 0) or not.
         */
        chipVM.setV(vy, yValue);
        chipVM.setV(vx, xValue);
        if (isXY5) {
            chipVM.execute8XY5(vx, vy);
        } else {
            chipVM.execute8XY7(vy, vx);
        }

        assertEquals(expected, chipVM.getV(vx));
        assertEquals(borrow, chipVM.getV(0xF),
                    "The borrow bit should be set to: " + borrow + " for the subtrction of " + xValue + " - " + yValue);
        assertEquals(yValue, chipVM.getV(vy), "Vy should not be affected by the subtraction operation");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, 7,  3, 1",     // Vy=7 right shift answer to Vx (3) carry to (VF=1)
        "4, 0, 5, 170, 85, 0"  // Vy = 0xAA = 170  result = 85(0x55)(VF=0)
    })
    public void rightShift(int vx, int xValue, int vy, int yValue, int expected, int carry) {
        chipVM.setV(vy, yValue);
        chipVM.setV(vx, xValue);
        chipVM.execute8XY6(vx, vy);
        assertEquals(expected, chipVM.getV(vx));
        assertEquals(carry, chipVM.getV(0xF),
                    "The carrt bit should be set to: " + carry + " for the rightshift of " + yValue);
    }
    
    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, 240, 224, 1",     // Vy=240(0b11110000) left shift answer to Vx (0b11100000) carry to (VF=1)
        "4, 0, 5, 85, 170, 0"  // Vy = 85(0b01010101)  result =170(0b10101010(VF=0)
    })
    public void leftShift(int vx, int xValue, int vy, int yValue, int expected, int carry) {
        chipVM.setV(vy, yValue);
        chipVM.setV(vx, xValue);
        chipVM.execute8XYE(vx, vy);
        assertEquals(expected, chipVM.getV(vx));
        assertEquals(carry, chipVM.getV(0xF),
                    "The carry bit should be set to: " + carry + " for the leftshift of " + yValue);
    }

    /*
        8XY6 and 8XYE: Shift
    */
}
