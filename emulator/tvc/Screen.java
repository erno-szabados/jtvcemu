package emulator.tvc;

/*
 * Created on Aug 26, 2003
 *
 */
/**
 * @author gah
 *
 */
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.*;
import java.util.Arrays;
import javax.swing.JPanel;

/**
 * Videoton TVC displayable Screen canvas.
 *
 */
public class Screen extends JPanel implements ComponentListener {

    /**
     * Palette registers SN74LS670 4x4bit words.
     */
    private int[] paletteRegisters;
    /**
     * SN74LS175 4-bit
     */
    private int borderRegister;
    /**
     * Mode register SN74LS75 4bit latch
     */
    private int modeRegister;
    /**
     * 6-bit latch on TVC model HBA2 TODO
     */
    private int videoPageRegister;
    /**
     * if true, grayscale conversion performed.
     */
    private int colorKillEnable;
    /**
     * CRTC emulator instance.
     */
    private MC6845 mc6845;
    /**
     * Canvas default x resolution.
     */
    public static final int CANVAS_XRES = 640;
    /**
     * Canvas default y resolution.
     */
    public static final int CANVAS_YRES = 480;
    /**
     * Screen image resolutions.
     */
    public static final int IMG_XRES = 592;  // must be dividable by 8 (max. value: 800)
    public static final int IMG_YRES = 512;  // must be dividable by 2 (max. value: 625)
    private static final int IMG_YRES_H = 256;  // must be dividable by 2 (max. value: 625)
    /**
     * Visible canvas position.
     */
    private static final int DISPLAY_X_START = 152;
    //private static final int DISPLAY_Y_START = 78;
    private static final int DISPLAY_Y_START = 39;
    private static final int DISPLAY_X_STOP  = DISPLAY_X_START + IMG_XRES - 7;
    private static final int DISPLAY_Y_STOP  = DISPLAY_Y_START + IMG_YRES_H - 1;
    /**
     * Graphics mode identifiers.
     */
    public static final int GRAPHICS2 = 0x00;
    public static final int GRAPHICS4 = 0x1;
    public static final int GRAPHICS16 = 0x2;
    // Color IDs for your reference
    public static final int BLACK = 0x00;
    public static final int DARK_BLUE = 0x01;
    public static final int DARK_RED = 0x04;
    public static final int DARK_MAGENTA = 0x05;
    public static final int DARK_GREEN = 0x10;
    public static final int DARK_CYAN = 0x11;
    public static final int DARK_YELLOW = 0x14;
    public static final int DARK_GRAY = 0x15;
    public static final int LIGHT_BLACK = 0x40;
    public static final int BLUE = 0x41;
    public static final int RED = 0x44;
    public static final int MAGENTA = 0x45;
    public static final int GREEN = 0x50;
    public static final int CYAN = 0x51;
    public static final int YELLOW = 0x54;
    public static final int WHITE = 0x55;
    /**
     * Colors sorted to match 0I0G0R0B -> 000IRGB to speed up color conversion
     * by allowing shifting.
     */
    public static final int[] colorMap = new int[]{
        BLACK,
        DARK_BLUE,
        DARK_GREEN,
        DARK_CYAN,
        DARK_RED,
        DARK_MAGENTA,
        DARK_YELLOW,
        DARK_GRAY,
        LIGHT_BLACK,
        BLUE,
        GREEN,
        CYAN,
        RED,
        MAGENTA,
        YELLOW,
        WHITE
    };
    /**
     * Values for 16-color palette.
     */
    private static byte[][] colors = {
        {(byte) 0x00, (byte) 0x00, (byte) 0x00}, // black 0x00
        {(byte) 0x00, (byte) 0x00, (byte) 0x7f}, // dark blue 0x01
        {(byte) 0x7f, (byte) 0x00, (byte) 0x00}, // dark red 0x04
        {(byte) 0x7f, (byte) 0x00, (byte) 0x7f}, // dark magenta 0x05
        {(byte) 0x00, (byte) 0x7f, (byte) 0x00}, // dark green 0x10
        {(byte) 0x00, (byte) 0x7f, (byte) 0x7f}, // dark cyan 0x11
        {(byte) 0x7f, (byte) 0x7f, (byte) 0x00}, // dark yellow 0x14
        {(byte) 0x7f, (byte) 0x7f, (byte) 0x7f}, // dark gray 0x15
        {(byte) 0x00, (byte) 0x00, (byte) 0x00}, // light black 0x40
        {(byte) 0x00, (byte) 0x00, (byte) 0xff}, // blue 0x41
        {(byte) 0xff, (byte) 0x00, (byte) 0x00}, // red 0x44
        {(byte) 0xff, (byte) 0x00, (byte) 0xff}, // magenta 0x45
        {(byte) 0x00, (byte) 0xff, (byte) 0x00}, // green 0x50
        {(byte) 0x00, (byte) 0xff, (byte) 0xff}, // cyan 0x51
        {(byte) 0xff, (byte) 0xff, (byte) 0x00}, // yellow 0x54
        {(byte) 0xff, (byte) 0xff, (byte) 0xff}  // white 0x55
    };

    /**
     * Reference to memory object.
     */
    private TVC tvc;
    private Memory memory;
    private BufferedImage img, img_work, img_16, img_bw;
    private WritableRaster raster;
    private int DisplayCycles;
    private int HPos, VPos;
    private boolean CPprev, CursorInt;
    /**
     * Customizable scaling quality.
     */
    private Object scalingQuality;

    /**
     * Initialize screen buffers and registers.
     *
     * @param memory used to read video memory.
     */
    public Screen(TVC tvc, Memory memory) {
        this.memory = memory;
        this.tvc = tvc;
        DisplayCycles = 0;
        HPos = 0;
        VPos = 0;
        CPprev  = false;
        scalingQuality = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        mc6845 = new MC6845();
        // These registers store values as 4 IGRB bits (in reality)
        paletteRegisters    = new int[4];
        paletteRegisters[0] = 0x00;
        paletteRegisters[1] = 0x00;
        paletteRegisters[2] = 0x00;
        paletteRegisters[3] = 0x00;
        borderRegister      = 0x00;
        modeRegister        = 0x00;
        videoPageRegister   = 0x3F;  // unused yet, HBA2 only (TVC64K+)
        colorKillEnable     = 0x40;  // color mode enabled

        IndexColorModel cm16 = createPalette16(colors);
        img_16 = new BufferedImage(IMG_XRES, IMG_YRES_H, BufferedImage.TYPE_BYTE_INDEXED, cm16);
        img_work = img_16;
        raster = img_work.getRaster();

        byte Y;
        byte[][] mono_colors = new byte[16][3];
        for (int i = 0; i < 16; i++) {
            Y = (byte)((0.3 * ((int)colors[i][0] & 0x0FF)) + (0.59 * ((int)colors[i][1] & 0x0FF)) + (0.11 * ((int)colors[i][2] & 0x0FF)));
            mono_colors[i][0] = Y;
            mono_colors[i][1] = Y;
            mono_colors[i][2] = Y;
        }
        IndexColorModel cm_bw = createPalette16(mono_colors);
        img_bw = new BufferedImage(IMG_XRES, IMG_YRES_H, BufferedImage.TYPE_BYTE_INDEXED, cm_bw);

        setSize(CANVAS_XRES, CANVAS_YRES);
        setFocusable(true);
        setRequestFocusEnabled(true);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                requestFocus();
                super.mouseClicked(e);
            }
        });
    }

    public MC6845 getMc6845() {
        return mc6845;
    }

    public void Reset() {
        borderRegister = 0;
    }

    /**
     * Set scaling quality.
     *
     * @param scalingQuality
     */
    public void setScalingQuality(Object scalingQuality) {
        this.scalingQuality = scalingQuality;
    }

    /**
     * Create indexed color model from TVC Palette colors (16 colors).
     *
     * @return
     */
    private IndexColorModel createPalette16(byte[][] palette) {

        byte rb[] = new byte[16];
        byte gb[] = new byte[16];
        byte bb[] = new byte[16];

        for (int i = 0; i < 16; i++) {
            rb[i] = palette[i][0];
            gb[i] = palette[i][1];
            bb[i] = palette[i][2];
        }
        return new IndexColorModel(4, 16, rb, gb, bb);
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, scalingQuality);
        g2d.drawImage(img, 0, 0, getWidth(), getHeight(), null);
    }

    public void update(Graphics g) {
        paint(g);
    }

    /**
     * Set palette register value by shifting the passed value.
     *
     * @param i register index 0..3
     * @param b 0I0G0R0B bytes
     */
    public final void setPaletteRegister(int i, int b) {
        int igrb = ((b & 0x40) >> 3) | ((b & 0x10) >> 2) | ((b & 0x04) >> 1) | (b & 0x01);
        paletteRegisters[i] = igrb;
    }

    /**
     * Get palette register value.
     *
     * @param i register index 0..3
     * @return 0I0G0R0B bytes
     */
    public final int getPaletteRegister(int i) {
        int igrb = paletteRegisters[i];
        int b = (igrb & 0x8) << 3 | (igrb & 0x4) << 2 | (igrb & 0x2) << 1 | (igrb & 0x1);
        return b;
    }

    /**
     * Set border register by shifting the passed value.
     *
     * @param b I0G0R0B0 byte
     */
    public final void setBorderRegister(int b) {
        // I0G0R0B0 -> 0000IGRB
        int igrb = ((b & 0x80) >> 4) | ((b & 0x20) >> 3) | ((b & 0x08) >> 2) | ((b & 0x02) >> 1);
        borderRegister = igrb;
    }

    /**
     * Get border register value.
     *
     * @return I0G0R0B0 bytes
     */
    public final int getBorderRegister() {
        int igrb = borderRegister;
        int b = (igrb & 0x8) << 4 | (igrb & 0x4) << 3 | (igrb & 0x2) << 2 | (igrb & 0x1);
        return b;
    }

    /**
     * Set mode register.
     *
     * @param b supported graphics mode required.
     */
    public final void setModeRegister(int b) {
        modeRegister = b;
    }

    /**
     * Get current mode register.
     *
     * @return current mode register.
     */
    public final int getModeRegister() {
        return modeRegister;
    }

    /**
     * Set by UI (push button on real HW).
     *
     * @param colorKillEnable 1 or 0
     */
    public final void setColorKillEnable(int colorKillEnable) {
        this.colorKillEnable = (colorKillEnable << 6) & 0x40;
    }

    /**
     * Read by Ports.
     *
     * Mapped to IO port.
     *
     * @return color kill bit status.
     */
    public final int getColorKillEnable() {
        return colorKillEnable;
    }

    /**
     * Clear interrupt register.

     */
    public final void clearCursorInt() {
        CursorInt = false;
    }

    /**
     * Read by Ports.
     *
     * Mapped to IO port.
     *
     * @return cursor/sound interrupt status.
     */
    public final int getCursorInt() {
        return (CursorInt? 0x00 : 0x10);   // 0 - active
    }

    /**
     * Video hardware and TV display emulation
     */
    public final boolean run(int cycles_320ns) {

        int[] vid_mem = ((Memory.Page) memory.VID).mem;
        boolean frame_ready = false;

        DisplayCycles += cycles_320ns;
        while (DisplayCycles > 1) {

            DisplayCycles -= 2;     // decrase by 2*320 ns (character cycle is 640ns)

            // Run CRTC for one 640ns cycle
            mc6845.run();

            // Step cathode ray position on TV screen
            if (mc6845.HSYNC) {
                if (HPos > 0) VPos++;
                HPos = 0;
            }
            else HPos+=8;
            if (mc6845.VSYNC) {
                // Update image at vertical synchron
                if (VPos > 50) {
                    frame_ready = true;
                    img_work.setData(raster);
                    img = img_work;
                    repaint();
                    img_work  = (colorKillEnable == 0)? img_bw : img_16;
                    raster = img_work.getRaster();
                }
                VPos = 0;
            }

            // Draw image if the beam is inside the displayed area
            if ((HPos >= DISPLAY_X_START) && (HPos < DISPLAY_X_STOP) &&
                (VPos >= DISPLAY_Y_START) && (VPos < DISPLAY_Y_STOP)) {

                int pixels[] = new int[8];
                if (mc6845.DE) {
                    // Draw video memory content
                    int adr = ((mc6845.MA & 0x0FC0) << 2) | ((mc6845.RA & 0x03) << 6) | (mc6845.MA & 0x003F);
                    int b   = vid_mem[adr];
                    switch (modeRegister) {
                        case GRAPHICS2: {
                             for (int cnt = 7; cnt >= 0; cnt--) {
                                 pixels[cnt] = paletteRegisters[b & 0x01];
                                 b >>= 1;
                             }
                        }
                        break;
                        case GRAPHICS4: {
                             pixels[0] = paletteRegisters[((b & 0x80) >> 7) | ((b & 0x8) >> 2)];
                             pixels[1] = pixels[0];
                             pixels[2] = paletteRegisters[((b & 0x40) >> 6) | ((b & 0x4) >> 1)];
                             pixels[3] = pixels[2];
                             pixels[4] = paletteRegisters[((b & 0x20) >> 5) | ((b & 0x2))];
                             pixels[5] = pixels[4];
                             pixels[6] = paletteRegisters[((b & 0x10) >> 4) | ((b & 0x1) << 1)];
                             pixels[7] = pixels[6];
                        }
                        break;
                        case GRAPHICS16:  // 0x02 or 0x03
                        default: {
                             // convert I1 I2 G1 G2 R1 R2 B1 B2 pixel data
                             // P1 I0G0R0B0 -> 0000IGRB
                             pixels[0] = ((b & 0x80) >> 4) | ((b & 0x20) >> 3) | ((b & 0x8) >> 2) | ((b & 0x2) >> 1);
                             pixels[1] = pixels[0];
                             pixels[2] = pixels[0];
                             pixels[3] = pixels[0];
                             // P2 0I0G0R0B -> 0000IGRB
                             pixels[4] = ((b & 0x40) >> 3) | ((b & 0x10) >> 2) | ((b & 0x4) >> 1) | ((b & 0x1));
                             pixels[5] = pixels[4];
                             pixels[6] = pixels[4];
                             pixels[7] = pixels[4];
                        }
                        break;
                    }
                }
                else {
                    // Draw border
                    pixels[0] = borderRegister;
                    pixels[1] = pixels[0];
                    pixels[2] = pixels[0];
                    pixels[3] = pixels[0];
                    pixels[4] = pixels[0];
                    pixels[5] = pixels[0];
                    pixels[6] = pixels[0];
                    pixels[7] = pixels[0];
                }
                raster.setPixels(HPos - DISPLAY_X_START, VPos - DISPLAY_Y_START, 8, 1, pixels);
            }
            // Generate interrupt on falling edge of cursor
            boolean CP = !(mc6845.CURSOR || tvc.SoundINT);
            CursorInt  = CursorInt || (CP && !CPprev); // rising edge on CP
            CPprev = CP;
        }
        return frame_ready;
    }

    /**
     * Resize canvas when parent resized.
     *
     * @param e
     */
    public void componentResized(ComponentEvent e) {
        Log.getInstance().write("parent resized");
        setSize(getParent().getSize());
        revalidate();
    }

    /**
     * Required by interface.
     *
     * @param e
     */
    public void componentMoved(ComponentEvent e) {
    }

    /**
     * Required by interface.
     *
     * @param e
     */
    public void componentShown(ComponentEvent e) {
    }

    /**
     * Required by interface.
     *
     * @param e
     */
    public void componentHidden(ComponentEvent e) {
    }
}