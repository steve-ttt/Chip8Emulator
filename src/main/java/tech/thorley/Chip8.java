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
    
    // Modern stack
    private ArrayDeque<Integer> stack = new ArrayDeque<>();  
    
    private final Map<Integer, Consumer<Integer>> dispatchTable = new HashMap<>();

    public Chip8() {
        
        // load font into memory

        // set program counter to start of program space
        this.pc = 0x0200;
        setupDispatchTable(); // Initialize the "map"
        
    }

     private void setupDispatchTable() {
        // The table points to methods within THIS class
        dispatchTable.put(0x0, (opcode) -> handleZeroSeries(opcode));
        dispatchTable.put(0x1, (opcode) -> execute1NNN(opcode));
        dispatchTable.put(0x2, (opcode) -> execute2NNN(opcode));
        dispatchTable.put(0x3, (opcode) -> execute3XNN(opcode));
        dispatchTable.put(0x8, (opcode) -> handleEightSeries(opcode)); 
        // ...
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
        int subType = (opcode & 0x00F0) >> 4;
        // Switch logic here...
    }

    private void handleZeroSeries(int opcode) {
        int subType = (opcode & 0x00FF) >> 4;
        // switch statemnt
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



    /////////////////////////////////////////////
    /// OP COdes 
    /// 
    /////////////////////////////////////////////

    public void execute0NNN(int opcode) {
        int address = opcode & 0x0FFF;
        this.pc = address;
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

    public void execute4XNN(int vx, int nn) {
        int xValue = this.getV(vx);
        if (xValue != nn) {
            this.incrementPC();
        }
    }

    public void execute5XY0(int vx, int vy) {
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        if (xValue == yValue) {
            this.incrementPC();
        }
    }

    public void execute6XNN(int vx, int nn) {
        this.setV(vx, nn);
    }

    public void execute7XNN(int vx, int nn) {
        int xValue = this.getV(vx);
        int result = xValue + nn;
        result = result & 0xFF;
        this.setV(vx, result);
    }

    public void execute8XY0(int vx, int vy) {
        int value = this.getV(vy);
        this.setV(vx, value);
    }

    public void execute8XY1(int vx, int vy) {
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue | yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
    }

    public void execute8XY2(int vx, int vy) {
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue & yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
    }

    public void execute8XY3(int vx, int vy) {
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue ^ yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
    }

    public void execute8XY4(int vx, int vy) {
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

    public void execute8XY5(int vx, int vy) {
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

    public void execute8XY7(int vy, int vx) {
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

    public void execute8XY6(int vx, int vy) {
        int yValue = this.getV(vy);
        int carry = yValue & 1;
        int result = yValue >> 1;
        result = result & 0xFF;
        this.setV(0xF, carry); // set carry
        this.setV(vx, result);
    }

    public void execute8XYE(int vx, int vy) {
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

