package tech.thorley;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque; // prefered over java.util.stack
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;


public class Chip8 {

    public interface AudioOutput {
        void start();
        void stop();
    }

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
    private boolean[] keypad = new boolean[16];
    private AudioOutput audioOutput;
    
    // Modern stack
    private ArrayDeque<Integer> stack = new ArrayDeque<>();  
    
    private final Map<Integer, Consumer<Integer>> dispatchTable = new HashMap<>();
    private final Map<Integer, Consumer<Integer>> eightSeriesDispatchTable = new HashMap<>();
    private final Map<Integer, Consumer<Integer>> fseriesDispatchTable = new HashMap<>();

    private final Random random = new Random();
    

    public Chip8() {
        this(new NoopAudioOutput());
    }

    public Chip8(AudioOutput audioOutput) {
        this.audioOutput = audioOutput;
        loadFontSet();

        // set program counter to start of program space
        this.pc = 0x0200;
        setupDispatchTable(); // Initialize the "map"
        setupEightSeriesDispatchTable(); // Initialize the "map" for the 8-series
        setupFseriesDispatchTable(); // Initialize the "map" for the F-series
    }

    private void loadFontSet() {
        int[] fontset = {
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
        };

        for (int i = 0; i < fontset.length; i++) {
            this.memory[0x050 + i] = fontset[i] & 0xFF;
        }
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
        dispatchTable.put(0xA, (opcode) -> executeANNN(opcode));
        dispatchTable.put(0x9, (opcode) -> execute9XY0(opcode));
        dispatchTable.put(0xB, (opcode) -> executeBNNN(opcode));
        dispatchTable.put(0xC, (opcode) -> executeCXNN(opcode));
        dispatchTable.put(0xD, (opcode) -> executeDXYN(opcode));
        dispatchTable.put(0xE, (opcode) -> handleESeries(opcode));
        dispatchTable.put(0xF, (opcode) -> handleFSeries(opcode));
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

    private void setupFseriesDispatchTable() {
        fseriesDispatchTable.put(0x07, (opcode) -> executeFX07(opcode));
        fseriesDispatchTable.put(0x0A, (opcode) -> executeFX0A(opcode));
        fseriesDispatchTable.put(0x15, (opcode) -> executeFX15(opcode));
        fseriesDispatchTable.put(0x18, (opcode) -> executeFX18(opcode));
        fseriesDispatchTable.put(0x1E, (opcode) -> executeFX1E(opcode));
        fseriesDispatchTable.put(0x29, (opcode) -> executeFX29(opcode));
        fseriesDispatchTable.put(0x33, (opcode) -> executeFX33(opcode));
        fseriesDispatchTable.put(0x55, (opcode) -> executeFX55(opcode));
        fseriesDispatchTable.put(0x65, (opcode) -> executeFX65(opcode));
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

    private void handleESeries(int opcode) {
        int subType = opcode & 0x00FF;
        switch (subType) {
            case 0x9E:
                executeEX9E(opcode);
                break;
            case 0xA1:
                executeEXA1(opcode);
                break;
            default:
                break;
        }
    }

    private void handleFSeries(int opcode) {
        int subType = opcode & 0x00FF;
        Consumer<Integer> action = fseriesDispatchTable.get(subType);
        if (action != null) {
            action.accept(opcode);
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

    public void loadRom(Path romPath) throws IOException {
        byte[] romBytes = Files.readAllBytes(romPath);
        for (int i = 0; i < romBytes.length; i++) {
            this.setMemory(0x200 + i, romBytes[i] & 0xFF);
        }
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

    public boolean[][] getDisplay() {
        return display;
    }

    public void setKeyState(int keyIndex, boolean pressed) {
        if (keyIndex < 0 || keyIndex >= keypad.length) {
            throw new IllegalArgumentException("Key index out of bounds: " + keyIndex);
        }
        keypad[keyIndex] = pressed;
    }

    public boolean isKeyPressed(int keyIndex) {
        if (keyIndex < 0 || keyIndex >= keypad.length) {
            throw new IllegalArgumentException("Key index out of bounds: " + keyIndex);
        }
        return keypad[keyIndex];
    }

    public void setSoundTimer(int value) {
        this.soundTimer = value & 0xFF;
    }

    public int getSoundTimer() {
        return soundTimer;
    }

    public void setAudioOutput(AudioOutput audioOutput) {
        this.audioOutput = audioOutput;
    }

    public void clearDisplay() {
        for (boolean[] display1 : display) {
            for (int j = 0; j < display1.length; j++) {
                display1[j] = false;
            }
        }
    }

    public void updateTimers() {
        if (delayTimer > 0) {
            delayTimer--;
        }
        if (soundTimer > 0) {
            soundTimer--;
        }

        if (soundTimer > 0) {
            audioOutput.start();
        } else {
            audioOutput.stop();
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
        this.incrementPC();
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
        this.incrementPC();
        if (xValue == nn) {
            this.incrementPC();
        }
    }

    public void execute4XNN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;   
        int xValue = this.getV(vx);
        this.incrementPC();
        if (xValue != nn) {
            this.incrementPC();
        }        
    }

    public void execute5XY0(int opcode) {
        // 5XY0 	Skip the following instruction if the value of register VX is equal to the value of register VY
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        this.incrementPC();
        if (xValue == yValue) {
            this.incrementPC();
        }
    }

    public void execute6XNN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;
        this.setV(vx, nn);
        this.incrementPC();
    }

    public void execute7XNN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;
        int xValue = this.getV(vx);
        int result = xValue + nn;
        result = result & 0xFF;
        this.setV(vx, result);
        this.incrementPC();
    }

    public void execute8XY0(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int value = this.getV(vy);
        this.setV(vx, value);
        this.incrementPC();
    }

    public void execute8XY1(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue | yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
        this.incrementPC();
    }

    public void execute8XY2(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue & yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
        this.incrementPC();
    }

    public void execute8XY3(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        int result = (xValue ^ yValue) & 0xFF; // mask the value so it doesn't exceed 8 bits (255).
        this.setV(vx, result);
        this.incrementPC();
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
        this.incrementPC();
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
        this.incrementPC();
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
        this.incrementPC();
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
        this.incrementPC();
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
        this.incrementPC();

    }

    public void executeANNN(int opcode) {
        int address = opcode & 0x0FFF;
        this.indexRegister = address;
        this.incrementPC();
    }

    public void executeBNNN(int opcode) {
        int address = opcode & 0x0FFF;
        this.pc = address + this.getV(0);
    }

    public void executeCXNN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;
        int randomValue = this.random.nextInt(256) & nn;
        this.setV(vx, randomValue);
        this.incrementPC();
    }

    public void executeEX9E(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int key = this.getV(vx);
        this.incrementPC();
        if (this.keypad[key]) {
            this.incrementPC();
        }
    }

    public void executeEXA1(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int key = this.getV(vx);
        this.incrementPC();
        if (!this.keypad[key]) {
            this.incrementPC();
        }
    }

    public void execute9XY0(int opcode) {
        // 9XY0 	Skip the following instruction if the value of register VX is NOT equal to the value of register VY
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int xValue = this.getV(vx);
        int yValue = this.getV(vy);
        this.incrementPC();
        if (xValue != yValue) {
            this.incrementPC();
        }

    }

    public void executeDXYN(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int vy = (opcode & 0x00F0) >> 4;
        int n = opcode & 0x000F;

        int x = this.getV(vx) % 64;
        int y = this.getV(vy) % 32;
        this.setV(0xF, 0);

        for (int row = 0; row < n; row++) {
            int spriteByte = this.memory[this.indexRegister + row] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                if ((spriteByte & (0x80 >> bit)) != 0) {
                    int screenX = (x + bit) % 64;
                    int screenY = (y + row) % 32;
                    boolean current = this.display[screenX][screenY];
                    if (current) {
                        this.setV(0xF, 1);
                    }
                    this.display[screenX][screenY] = current ^ true;
                }
            }
        }
        this.incrementPC();
    }

    public void executeFX07(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        this.setV(vx, this.delayTimer);
        this.incrementPC();
    }

    public void executeFX0A(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        for (int i = 0; i < this.keypad.length; i++) {
            if (this.keypad[i]) {
                this.setV(vx, i);
                this.incrementPC();
                return;
            }
        }
        // If no key is pressed, we do NOT increment the PC.
        // This effectively halts execution until a key is pressed.
    }

    public void executeFX15(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        this.delayTimer = this.getV(vx);
        this.incrementPC();
    }

    public void executeFX18(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        this.soundTimer = this.getV(vx);
        this.incrementPC();
    }

    private static final class NoopAudioOutput implements AudioOutput {
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    }

    public void executeFX1E(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        this.indexRegister += this.getV(vx);
        this.incrementPC();
    }

    public void executeFX29(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int digit = this.getV(vx);
        this.indexRegister = 0x050 + (digit * 5);
        this.incrementPC();
    }

    public void executeFX33(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        int value = this.getV(vx);
        int hundreds = value / 100;
        int tens = (value / 10) % 10;
        int ones = value % 10;
        this.memory[this.indexRegister] = hundreds;
        this.memory[this.indexRegister + 1] = tens;
        this.memory[this.indexRegister + 2] = ones;
        this.incrementPC();
    }

    public void executeFX55(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        for (int i = 0; i <= vx; i++) {
            this.memory[this.indexRegister + i] = this.getV(i);
        }
        this.indexRegister += vx + 1;
        this.incrementPC();
    }

    public void executeFX65(int opcode) {
        int vx = (opcode & 0x0F00) >> 8;
        for (int i = 0; i <= vx; i++) {
            this.setV(i, this.memory[this.indexRegister + i]);
        }
        this.indexRegister += vx + 1;
        this.incrementPC();
    }

}
