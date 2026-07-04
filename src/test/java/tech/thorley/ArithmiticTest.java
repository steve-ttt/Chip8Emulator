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
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 15}) // You can just pick a few key indices
    public void testSpecificRegistersInitializedToZero(int n) {
        assertEquals(0, chipVM.getV(n));
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

}
