package tech.thorley;


import java.lang.IllegalArgumentException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;

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

   
    /**
     * This chip 8 program counter starts at 0x200. 
     */
    @Test
    public void testPCInitializedto0x200() {
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

    /*
    done    8XY0: Set
        VX is set to the value of VY.
        
    done    8XY1: Bitwise OR
        VX is set to the bitwise/binary logical disjunction (OR) of VX and VY. VY is not affected.
        
    done    8XY2: Binary AND
        VX is set to the bitwise/binary logical conjunction (AND) of VX and VY. VY is not affected.
        
    done    8XY3: Logical XOR
        VX is set to the bitwise/binary exclusive OR (XOR) of VX and VY. VY is not affected.
        
        8XY4: Add
        VX is set to the value of VX plus the value of VY. VY is not affected.
        Unlike 7XNN, this addition will affect the carry flag. If the result is larger than 255 (and thus overflows the 8-bit register VX), the flag register VF is set to 1. If it doesn’t overflow, VF is set to 0.
        
        8XY5 and 8XY7: Subtract
        These both subtract the value in one register from the other, and put the result in VX. In both cases, VY is not affected.
        8XY5 sets VX to the result of VX - VY.
        8XY7 sets VX to the result of VY - VX.
        This subtraction will also affect the carry flag, but note that it’s opposite from what you might think. If the minuend (the first operand) is larger than or equal to the subtrahend (second operand), VF will be set to 1. If the subtrahend is larger, and we “underflow” the result, VF is set to 0. Another way of thinking of it is that VF is set to 1 before the subtraction, and then the subtraction either borrows from VF (setting it to 0) or not.
        
        8XY6 and 8XYE: Shift
    */
}
