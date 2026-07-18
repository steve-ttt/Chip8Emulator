package tech.thorley;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class DisplayPanel extends JPanel {

    private final Chip8 chip8;
    private final int scale = 12; // Scale factor to make pixels visible

    private final int screenWidth = 64 * scale;
    private final int screenHeight = 32 * scale;

    public DisplayPanel(Chip8 chip8) {
        this.chip8 = chip8;
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);

        boolean[][] display = chip8.getDisplay();

        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                if (display[x][y]) {
                    g.fillRect(x * scale, y * scale, scale, scale);
                }
            }
        }
    }
}