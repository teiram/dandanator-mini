package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

import com.grelobites.dandanator.util.emulator.zxspectrum.CRT;
import com.grelobites.dandanator.util.emulator.zxspectrum.InPort;
import com.grelobites.dandanator.util.emulator.zxspectrum.J80;
import com.grelobites.dandanator.util.emulator.zxspectrum.Peripheral;
import com.grelobites.dandanator.util.emulator.zxspectrum.Polling;
import com.grelobites.dandanator.util.emulator.zxspectrum.VDU;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.HashMap;

/**
 * $Id: Screen.java 330 2010-09-14 10:29:28Z mviara $
 * <p>
 * Sinclair ZX spectrum screen/keyboard emulator
 * <p>
 * <p>
 * $Log: Screen.java,v $
 * Revision 1.6  2008/05/22 22:13:02  mviara
 * <p>
 * Remove debug code.
 * <p>
 * Revision 1.5  2008/05/14 16:53:38  mviara
 * Added support to terminate the simulation pressing the key F10.
 * <p>
 * Revision 1.4  2005/03/18 16:40:48  mviara
 * Added support for speaker.
 * <p>
 * Revision 1.3  2004/11/22 16:50:34  mviara
 * Some cosmetic change.
 * <p>
 * Revision 1.2  2004/07/18 11:22:29  mviara
 * Better 128K emulator.
 * <p>
 * Revision 1.1  2004/06/20 16:25:58  mviara
 * Split spectrum emulator in more files.
 * Added partial support for 128K.
 */
public class Screen extends JComponent
        implements Peripheral, InPort, VDU, Polling, ActionListener,
        KeyListener, FocusListener, Spectrum {


    /**
     * Screen definition
     */
    public static final int pixelWidth = 256;
    public static final int pixelHeight = 192;
    public static final int pixelScale = 1;

    /**
     * Memory attribute definition
     */
    public static final byte ATTRIBUTE_FLASH = (byte) 0x80;
    public static final byte ATTRIBUTE_BRIGHT = (byte) 0x40;

    // R,G,B Value used for color
    public static final int COLOR = 0xcc;

    // R,G,B Value used for bright color
    public static final int COLORBRIGHT = 0xff;

    // Swing components
    private JLabel label;
    private JTextField perc;
    private int percValue = 0;
    private JButton buttonPause = new JButton("Pause");
    private JButton buttonResume = new JButton("Resume");
    private JButton buttonSnapshot = new JButton("Snapshot");
    private JButton buttonExit = new JButton("Exit");
    private int waveCount;
    private JFrame frame;
    private boolean terminate = false;

    // Flash state
    private boolean flash = false;


    // Screen memory
    private byte screenMemory[] = null;
    private int screenOffset;

    // Connected CPU
    private J80 cpu;

    // Screen size
    private Dimension size = null;
    private Image image = null;
    private HashMap patterns = new HashMap();
    private Color colors[] = new Color[16];

    // Array to hold the screen change
    private int changed[] = new int[SCREEN_MEMORY_SIZE * 4];
    private int changedWrite = 0;
    private boolean screenChanged = false;

    /**
     * Spectrum key codes table
     */
    private int keyCodes[][] =
            {
                    {KeyEvent.VK_SHIFT, KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V},
                    {KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_F, KeyEvent.VK_G},
                    {KeyEvent.VK_Q, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_R, KeyEvent.VK_T},
                    {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5},
                    {KeyEvent.VK_0, KeyEvent.VK_9, KeyEvent.VK_8, KeyEvent.VK_7, KeyEvent.VK_6},
                    {KeyEvent.VK_P, KeyEvent.VK_O, KeyEvent.VK_I, KeyEvent.VK_U, KeyEvent.VK_Y},
                    {KeyEvent.VK_ENTER, KeyEvent.VK_L, KeyEvent.VK_K, KeyEvent.VK_J, KeyEvent.VK_H},
                    {KeyEvent.VK_SPACE, '\\', KeyEvent.VK_M, KeyEvent.VK_N, KeyEvent.VK_B},
            };

    private int keyState[] = new int[8];

    public Screen() {
        // Enable keyboard event
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.INPUT_METHOD_EVENT_MASK);
        addKeyListener(this);

        // Enable focus event
        addFocusListener(this);

        // Reset keyboard
        keyboardReset();

    }

    /**
     * Reset the emulated keyboard (called when focus is gained)
     */
    private void keyboardReset() {
        for (int i = 0; i < 8; i++)
            keyState[i] = 0xff;
    }

    public boolean isFocusable() {
        return true;
    }

    public boolean hasFocus() {
        return true;
    }

    public void focusGained(FocusEvent e) {
        keyboardReset();
    }

    public void focusLost(FocusEvent e) {
    }


    public Dimension getPreferredSize() {
        if (size == null) {
            size = new Dimension(pixelWidth * pixelScale, pixelHeight * pixelScale);
        }

        return size;
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /**
     * Process a keyboard event
     *
     * @param code    - Code key
     * @param pressed - True if key is pressed
     */
    private void doKey(int code, boolean pressed) {
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 5; j++)
                if (keyCodes[i][j] == code) {
                    int mask = 1 << j;
                    if (pressed)
                        keyState[i] &= ~mask;
                    else
                        keyState[i] |= mask;
                    break;
                }
    }

    public void keyPressed(KeyEvent e) {
        doKey(e.getKeyCode(), true);
        e.consume();

    }

    public void keyReleased(KeyEvent e) {
        doKey(e.getKeyCode(), false);
        e.consume();

    }


    public void keyTyped(KeyEvent e) {
    }

    private Image getImage(JComponent comp, int pixel, int color) {
        if (flash & (color & ATTRIBUTE_FLASH) != 0) {
            int c1 = color & 7;
            int c2 = (color >> 3) & 7;
            color &= 0xC0;
            color |= c1 << 3 | c2;
        }

        color &= 0x7f;

        // Calculate hashing using pixel and color
        int hashValue = pixel << 8 | color;
        Integer keyImage = new Integer(hashValue);
        Image img = (Image) patterns.get(keyImage);


        // Image not found draw it
        if (img == null) {
            int base = 0;
            Color bg;
            Color fg;

            if ((color & ATTRIBUTE_BRIGHT) != 0)
                base = 8;


            // Get color
            bg = colors[base + ((color >> 3) & 7)];
            fg = colors[base + (color & 7)];

            // Create one image
            img = comp.createImage(8 * pixelScale, 1 * pixelScale);
            Graphics g = img.getGraphics();

            // Draw the pattern
            for (int c = 0; c < 8; c++) {
                Color clr = bg;

                if ((pixel & (0x80 >> c)) != 0)
                    clr = fg;
                g.setColor(clr);
                g.fillRect(c * pixelScale, 0, pixelScale, 1 * pixelScale);

            }

            patterns.put(keyImage, img);
        }

        return img;
    }

    private int getMemory(int offset) {
        if (screenMemory == null) {
            return 0;
        }

        return screenMemory[screenOffset + offset] & 0xff;
    }

    private void drawByte(int addr, Graphics g) {
        int pixel = getMemory(addr);
        int x = ((addr & 0x1f) << 3);
        int y = (((int) (addr & 0x00e0)) >> 2) +
                (((int) (addr & 0x0700)) >> 8) +
                (((int) (addr & 0x1800)) >> 5);
        int X = (x * pixelScale);
        int Y = (y * pixelScale);

        int attr = getMemory(SCREEN_MEMORY_SIZE + (addr & 0x1f) + ((y >> 3) * 32)) & 0xff;
        Image chars = getImage(this, pixel, attr);
        g.drawImage(chars, X, Y, null);

    }

    private void drawScreen() {
        if (!screenChanged)
            return;


        Graphics g = image.getGraphics();

        synchronized (changed) {

            for (int i = 0; i < changedWrite; i++) {
                drawByte(changed[i], g);
            }

            screenChanged = false;
            changedWrite = 0;
        }
    }

    public void paint(Graphics g) {
        if (image == null) {
            image = createImage(size.width, size.height);

        }

        drawScreen();

        g.drawImage(image, 0, 0, this);

    }

    private void resume() {

        buttonPause.setEnabled(true);
        buttonResume.setEnabled(false);
        buttonSnapshot.setEnabled(false);
        cpu.resume();
    }

    private void pause() {
        buttonPause.setEnabled(false);
        buttonResume.setEnabled(true);
        buttonSnapshot.setEnabled(true);
        cpu.pause();
    }

    private void snapshot() {
        JFileChooser fc = new JFileChooser(".");
        fc.showOpenDialog(frame);
        File file = fc.getSelectedFile();

        try {
            cpu.loadSnapshot(file.toString());
        } catch (Exception e) {
            System.out.println(e);
        }

    }


    public void resetCPU(J80 cpu) {
        flash = false;
    }

    public void disconnectCPU(J80 cpu) {
    }


    public void connectCPU(J80 cpu) throws Exception {
        this.cpu = cpu;
        cpu.addInPort(254, this);

        // Spectrum display initialization

        colors[0] = new Color(0, 0, 0);
        colors[1] = new Color(0, 0, COLOR);
        colors[2] = new Color(COLOR, 0, 0);
        colors[3] = new Color(COLOR, 0, COLOR);
        colors[4] = new Color(0, COLOR, 0);
        colors[5] = new Color(0, COLOR, COLOR);
        colors[6] = new Color(COLOR, COLOR, 0);
        colors[7] = new Color(COLOR, COLOR, COLOR);

        // Bright color
        for (int i = 0; i < 8; i++)
            colors[8 + i] = new Color(colors[i].getRed() != 0 ? COLORBRIGHT : 0,
                    colors[i].getGreen() != 0 ? COLORBRIGHT : 0,
                    colors[i].getBlue() != 0 ? COLORBRIGHT : 0);

        frame = new JFrame(J80.version);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        perc = new JTextField("  100%");
        perc.setEditable(false);
        perc.setHorizontalAlignment(perc.RIGHT);
        label = new JLabel("Z80  at " + cpu.getMhz() + " Mhz");
        g.fill = g.NONE;
        g.insets = new Insets(2, 2, 2, 2);
        g.gridx = 0;
        g.gridy = 0;
        g.gridheight = 1;
        g.gridwidth = 2;
        g.anchor = g.CENTER;
        p.add(this, g);
        g.gridwidth = 1;
        g.gridy++;
        g.anchor = g.SOUTHEAST;
        p.add(label, g);
        g.gridx++;
        p.add(perc, g);

        g.gridy = 0;
        g.gridx = 2;
        g.gridwidth = 1;
        g.gridheight = 2;
        g.fill = g.HORIZONTAL;
        g.anchor = g.NORTH;
        JPanel box = new JPanel(new GridBagLayout());
        GridBagConstraints g1 = new GridBagConstraints();
        g1.fill = g1.HORIZONTAL;
        g1.gridx = 0;
        g1.gridy = 0;
        box.add(buttonResume, g1);
        g1.gridy++;
        box.add(buttonPause, g1);
        g1.gridy++;
        box.add(buttonSnapshot, g1);
        g1.gridy++;
        box.add(buttonExit, g1);
        g1.gridy++;

        p.add(box, g);
        frame.setContentPane(p);
        frame.pack();
        frame.setVisible(true);

        buttonResume.setEnabled(false);
        buttonSnapshot.setEnabled(false);

        buttonPause.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pause();
            }
        });

        buttonResume.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resume();
            }
        });

        buttonExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        buttonSnapshot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                snapshot();
            }
        });

        requestFocus();

        Timer timer = new Timer(20, this);


        timer.setRepeats(true);
        timer.start();

        cpu.addPolling(320, this);

    }

    /**
     * Polling called every 320 ms
     */
    public void polling(J80 cpu) {
        // Reverse flash attribute
        flash = !flash;
        for (int i = 0; i <= SCREEN_ATTRIBUTE_SIZE; i++) {

            if ((getMemory(i + SCREEN_MEMORY_SIZE) & ATTRIBUTE_FLASH) != 0) {
                repaintAttribute(i);
            }
        }

    }

    void repaintAttribute(int addr) {
        int scrAddr = ((addr & 0x300) << 3) | (addr & 0xff);

        for (int i = 0; i < 8; i++) {
            repaintScreen(scrAddr);

            // Next address in memory
            scrAddr += 256;
        }
    }

    void repaintScreen(int add) {
        synchronized (changed) {
            if (changedWrite < changed.length) {
                screenChanged = true;
                changed[changedWrite++] = add;
            }
        }
    }

    /**
     * Timer called every 20 ms to update the screen
     */
    public void actionPerformed(ActionEvent e) {
        if (screenChanged) {
            repaint();
        }
    }


    /**
     * j80.InPort
     */
    public int inb(int port, int hi) {
        int result = 0xff;

        port &= 0xff;
        port |= (hi & 0xff) << 8;


        for (int i = 0; i < 8; i++)
            if ((port & (0x100 << i)) == 0) {
                result &= keyState[i];
            }
        ///System.out.println("in "+Integer.toHexString(port));
        //System.out.println("inb "+port);


        //System.out.println("port "+Integer.toHexString(port)+" = "+Integer.toHexString(result)+" rows "+rows);
        return result;
    }


    public void setScreenMemory(byte memory[], int offset) {
        screenMemory = memory;
        screenOffset = offset;
        for (int i = 0; i < SCREEN_MEMORY_SIZE; i++)
            repaintScreen(i);

    }

    /**
     * j80.VDU Implementation
     */
    public void putchar(char c) {
        System.out.print(c);
    }

    public void print(String s) {
        System.out.print(s);
    }

    public void println(String s) {
        System.out.println(s);
    }


    public void showIdle(boolean mode) {
    }

    public void showUtilization(int value) {

        percValue = value;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                perc.setText(percValue + "%");
            }
        });
    }

    public int getNumCol() {
        return 79;
    }

    public void setNumCol(int col) {
    }

    public int getNumRow() {
        return 25;
    }

    public void setNumRow(int row) {
    }

    public void setCRT(CRT crt) {
    }

    public boolean isTerminate() {
        return terminate;
    }

    public void terminate() {
        terminate = true;
    }

    public String toString() {
        return "Spectrum Screen $Revision: 330 $";
    }

}
