package tech.thorley;


import java.util.ArrayDeque; // prefered over java.util.stack 


public class Chip8 {

    // 32-bit types for clean math and logic. beacuse java doesn't have unsigned int like uint_8 of C or C++.
    private int[] registers = new int[16]; 
    private int delayTimer;   
    private int soundTimer;  
    
    // 16-bit types for addresses (keeps them semantically distinct from data)
    private short indexRegister; 
    private short pc;
    
    // 8-bit array for raw storage
    private byte[] memory = new byte[4096];
    
    // Modern stack
    private ArrayDeque<Integer> stack = new ArrayDeque<>();  

    public Chip8() {
        
        // load font into memory

        // set program counter to start of program space
        this.pc = 0x0200;

    }

    public short getPC() {return pc; }
    public void setPC(short value) { this.pc = value;}
    
    public int getV(int index){ 
        if (index < 0 || index > 15) {
            throw new IllegalArgumentException("Register index out of bounds: " + index);
        }
        return registers[index] & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
    }
    
    public void setV(int index, int value) {
        registers[index] = value & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
    }

    public void execute8XY0(int vx, int vy) {
        int value = this.getV(vy);
        this.setV(vx, value);
    }

    public void execute8XY1(int vx, int vy) {
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int returnValue = (xValue | yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, returnValue);
    }

}
