package tech.thorley;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

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
        assertEquals(0x0200, chipVM.getPC());
    }

}
