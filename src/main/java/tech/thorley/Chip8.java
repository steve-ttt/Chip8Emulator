package tech.thorley;

public class Chip8 {

    private short[] stack;
    private int stackFrame ;
    private short indexRegister; 
    private byte[] registers;
    private short pc;
    private byte[] memory;
    private byte delayTimer ;   
    private byte soundTimer  ;  

    public Chip8() {
        registers = new byte[16];
        memory = new byte[4096];
        stack = new short[32];
        
        // load font into memory

        // set program counter to start of program space
        this.pc = 0x0200;

    }

    public short getPC() {return pc; }
    public void setPC(short value) { this.pc = value;}
}
