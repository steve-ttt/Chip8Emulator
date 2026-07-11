package tech.thorley;


import java.util.ArrayDeque; // prefered over java.util.stack
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


public class Chip8 {

    // 32-bit types for clean math and logic. beacuse java doesn't have unsigned int like uint_8 of C or C++.
    private int[] registers = new int[16]; 
    private int delayTimer;   
    private int soundTimer;  
    
    // 16-bit types for addresses (keeps them semantically distinct from data)
    private int indexRegister; 
    private int pc;
    
    // 8-bit array for raw storage
    private int[] memory = new int[4096];
    private boolean[][] display = new boolean[64][32]; // chip-8 has a display buffer unlike ZX Spectrum or Gameboy that just uses main memory
    
    
    // Modern stack
    private ArrayDeque<Integer> stack = new ArrayDeque<>();  
    
    private final Map<Integer, Consumer<Integer>> dispatchTable = new HashMap<>();
    private final Map<Integer, Consumer<Integer>> eightSeriesDispatchTable = new HashMap<>();
    

    public Chip8() {
        
        // load font into memory

        // set program counter to start of program space
        this.pc = 0x0200;
        setupDispatchTable(); // Initialize the "map"
        setupEightSeriesDispatchTable(); // Initialize the "map" for the 8-series
    }

     private void setupDispatchTable() {
        // The table points to methods within THIS class
        dispatchTable.put(0x0, (opcode) -> handleZeroSeries(opcode));
        dispatchTable.put(0x1, (opcode) -> execute1NNN(opcode));
        dispatchTable.put(0x2, (opcode) -> execute2NNN(opcode));
        dispatchTable.put(0x3, (opcode) -> execute3XNN(opcode));
        dispatchTable.put(0x4, (opcode) -> execute4XNN(opcode));
        dispatchTable.put(0x5, (opcode) -> execute5XY0(opcode));
        dispatchTable.put(0x6, (opcode) -> execute6XNN(opcode));
        dispatchTable.put(0x7, (opcode) -> execute7XNN(opcode));
        dispatchTable.put(0x8, (opcode) -> handleEightSeries(opcode)); 
        // ... todo
    }

    private void setupEightSeriesDispatchTable() {
        eightSeriesDispatchTable.put(0x0, (opcode) -> execute8XY0(opcode));
        eightSeriesDispatchTable.put(0x1, (opcode) -> execute8XY1(opcode));
        eightSeriesDispatchTable.put(0x2, (opcode) -> execute8XY2(opcode));
        eightSeriesDispatchTable.put(0x3, (opcode) -> execute8XY3(opcode));
        eightSeriesDispatchTable.put(0x4, (opcode) -> execute8XY4(opcode));
        eightSeriesDispatchTable.put(0x5, (opcode) -> execute8XY5(opcode));
        eightSeriesDispatchTable.put(0x6, (opcode) -> execute8XY6(opcode));
        eightSeriesDispatchTable.put(0x7, (opcode) -> execute8XY7(opcode));
        eightSeriesDispatchTable.put(0xE, (opcode) -> execute8XYE(opcode)); 
    }

    // --- 3. THE PUBLIC API (The Entry Point) ---
    public void execute(int opcode) {
        int family = (opcode & 0xF000) >> 12;
        Consumer<Integer> action = dispatchTable.get(family);
        if (action != null) {
            action.accept(opcode);
        }
    }
    
    // This is the "Middle-man" for the 8-series
    private void handleEightSeries(int opcode) {
        int subType = opcode & 0x000F;
        Consumer<Integer> action = eightSeriesDispatchTable.get(subType);
        if (action != null) {
            action.accept(opcode);
        }
    }

    private void handleZeroSeries(int opcode) {
        int subType = opcode & 0x00FF;
        switch (subType) {
            case 0xEE:
                execute00EE();
                break;
            case 0xE0:
                execute00E0();
                break;
            default:
                execute0NNN(opcode);
                break;
        }
    }

    public void fetchDecodeExecute() {
        // fetch
        int highByte = memory[pc];
        int lowByte = memory[pc + 1];
        int opcode = (highByte << 8) | lowByte;
        
        // decode
        int family = (opcode & 0xF000) >> 12;
        Consumer<Integer> action = dispatchTable.get(family);
        
        // execute
        if (action != null) {
            action.accept(opcode);
        }
        
    }
    public int[] getMemory() {
        return memory;
    }
    public void setMemory(int address,int value) {
        this.memory[address] = value;
    }
    
    public int getPC() {return pc; }
    public void setPC(int value) { this.pc = value;}
    public int stackPeek() {
        return stack.peek();
    }
    public void stackPush(int value) {
        stack.push(value);
    }
    public int stackPop() {
        return stack.pop();
    }
    public boolean stackEmpty() {
        return stack.isEmpty();
    }
    
    public int getV(int index){ 
        if (index < 0 || index > 15) {
            throw new IllegalArgumentException("Register index out of bounds: " + index);
        }
        return registers[index] & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
    }
    
    public void setV(int index, int value) {
        registers[index] = value & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
    }

    public void incrementPC() {
        this.pc += 2;
    }

    public void setDisplayBit(int x, int y, boolean value) {
        display[x][y] = value;
    }

    public boolean getDisplayBit(int x, int y) {
        return display[x][y];
    }

    public void clearDisplay() {
        for (boolean[] display1 : display) {
            for (int j = 0; j < display1.length; j++) {
                display1[j] = false;
            }
        }
    }
    

    /////////////////////////////////////////////
    /// OP COdes 
    /// 
    /////////////////////////////////////////////

    public void execute0NNN(int opcode) {
        int address = opcode & 0x0FFF;
        this.pc = address;
    }

    public void execute00E0() {
        this.clearDisplay();
    }

    public void execute00EE() {
        this.pc = this.stackPop();
    }

    public void execute1NNN(int opcode) { 
        int address = opcode & 0x0FFF; // You extract the part you need inside the method
        this.pc = address;
    }

    public void execute2NNN(int opcode) {
        int address = opcode & 0x0FFF;
        this.incrementPC();
        this.stackPush(this.pc);
        this.pc = address;
    }

    public void execute3XNN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;
        int xValue = this.getV(vx);
        if (xValue == nn) {
            this.incrementPC();
            this.incrementPC();
        } else {
            this.incrementPC();
        }
    }

    public void execute4XNN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;   
        int xValue = this.getV(vx);
        if (xValue != nn) {
            this.incrementPC();
            this.incrementPC();
        } else {
            this.incrementPC();            
        }        
    }

    public void execute5XY0(int opcode) {
        // 5XY0 	Skip the following instruction if the value of register VX is equal to the value of register VY
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        if (xValue == yValue) {
            this.incrementPC();
            this.incrementPC();
        } else {
            this.incrementPC();
        }
    }

    public void execute6XNN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;
        this.setV(vx, nn);
    }

    public void execute7XNN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;
        int xValue = this.getV(vx);
        int result = xValue + nn;
        result = result & 0xFF;
        this.setV(vx, result);
    }

    public void execute8XY0(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int value = this.getV(vy);
        this.setV(vx, value);
    }

    public void execute8XY1(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue | yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
    }

    public void execute8XY2(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue & yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
    }

    public void execute8XY3(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue ^ yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
    }

    public void execute8XY4(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = xValue + yValue; // add them together
        if(result > 255) {
            this.setV(0xF, 1); // set the carry bit
        } else {
            this.setV(0xF, 0); // explicityly set the carry to 0 just in case
        }
        result = result & 0xFF;
        this.setV(vx, result);
    }

    public void execute8XY5(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int borrow = 1;
        int result = xValue - yValue;
        if ( yValue > xValue ) {
            borrow = 0;
            result = 256 - yValue;
        }
        result = result & 0xFF;
        this.setV(0xF, borrow); // set borrow
        this.setV(vx, result);
    }

    public void execute8XY6(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int yValue = this.getV(vy);
        int carry = yValue & 1;
        int result = yValue >> 1;
        result = result & 0xFF;
        this.setV(0xF, carry); // set carry
        this.setV(vx, result);
    }

    public void execute8XY7(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int borrow = 1;
        int result =  yValue - xValue;
        if ( result < 0 ) {
            borrow = 0;
            result = 256 + result;
        }
        result = result & 0xFF;
        this.setV(0xF, borrow); // set borrow
        this.setV(vx, result);
    }

    public void execute8XYE(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int yValue = this.getV(vy);
        int carry = yValue  & 0x80;
        if(carry == 0x80) {
            carry = 1;
        } else {
            carry = 0;
        }        
        int result = yValue << 1;
        result = result & 0xFF;
        this.setV(0xF, carry); // set carry
        this.setV(vx, result);

    }

    public void execute9XY0(int vx, int vy) {
        // 9XY0 	Skip the following instruction if the value of register VX is NOT equal to the value of register VY
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        if (xValue != yValue) {
            this.incrementPC();
        }

    }

}

