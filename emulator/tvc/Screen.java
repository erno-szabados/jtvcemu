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
 * Videoton TVC displayable Screen canvas. Indexed images that support 2,4,16
 * color modes.
 */
public class Screen extends JPanel implements Runnable, ComponentListener {

    /**
     * Palette registers SN74LS670 4x4bit words.
     */
    private Reg8Masked[] paletteRegisters;
    /**
     * Set if palette registers modified.
     */
    private boolean paletteRegistersChanged;
    /**
     * SN74LS175 4-bit
     */
    private Reg8Masked borderRegister;
    /**
     * Set if border register modified.
     */
    private boolean borderRegisterChanged;
    /**
     * Mode register SN74LS75 4bit latch
     */
    private Reg8Masked modeRegister;
    /**
     * Set if mode register modified.
     */
    private boolean modeRegisterChanged;
    /**
     * 6-bit latch on TVC model HBA2 TODO
     */
    private Reg8Masked videoPageRegister;
    /**
     * if true, grayscale conversion performed.
     *
     * TODO replace palette instead, probably cheaper.
     */
    private int colorKillEnable;
    /**
     * CRTC emulator instance. TODO
     */
    private MC6845 mc6845;
    /**
     * 40 ms = ca 25 full pictures in a second (approx. PAL)
     */
    public static final int SCREEN_REPAINT_INTERVAL = 40;
    /**
     * Border on canvas.
     */
    public static final int BORDER_SIZE = 0;
    /**
     * Canvas default x resolution.
     */
    public static final int CANVAS_XRES = 640;
    /**
     * Canvas default y resolution.
     */
    public static final int CANVAS_YRES = 480;
    /**
     * TVC mode 2 x resolution.
     */
    public static final int MODE2_XRES = 512;
    public static final int MODE2_BORDER = 32;
    /**
     * TVC mode 4 x resolution
     */
    public static final int MODE4_XRES = 256;
    public static final int MODE4_BORDER = 16;
    /**
     * TVC mode 16 x resolution
     */
    public static final int MODE16_XRES = 128;
    public static final int MODE16_BORDER = 8;
    /**
     * RAM contents could fill 243 lines. Sure?
     */
    public static final int MODE_YRES = 243;
    /**
     * Visible canvas size.
     */
    public static final int MODE_YRES_VISIBLE = 240;
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
        {0x00, 0x00, 0x00}, // black 0x00
        {0x00, 0x00, 0x7f}, // dark blue 0x01
        {0x00, 0x7f, 0x00}, // dark green 0x10
        {0x00, 0x7f, 0x7f}, // dark cyan 0x11
        {0x7f, 0x00, 0x00}, // dark red 0x04
        {0x7f, 0x00, 0x7f}, // dark magenta 0x05
        {0x7f, 0x7f, 0x00}, // dark yellow 0x14
        {0x7f, 0x7f, 0x7f}, // dark gray 0x15
        {0x00, 0x00, 0x00}, // light black 0x40
        {0x00, 0x00, (byte) 0xff}, // blue 0x41
        {0x00, (byte) 0xff, 0x00}, // green 0x50
        {0x00, (byte) 0xff, (byte) 0xff}, // cyan 0x51
        {(byte) 0xff, 0x00, 0x00}, // red 0x44
        {(byte) 0xff, 0x00, (byte) 0xff}, // magenta 0x45
        {(byte) 0xff, (byte) 0xff, 0x00}, // yellow 0x54
        {(byte) 0xff, (byte) 0xff, (byte) 0xff} // white 0x55
    };
    /**
     * Calculated from colors using the formula: Y=0.03*R+0.59G+0.11B Added + 45
     * for brighter RGB colors (0xFF-brightest color)
     *
     */
    private static byte[][] mono_colors = {
        {(byte) 0x00, (byte) 0x00, (byte) 0x00}, // black 0x00
        {(byte) 0x53, (byte) 0x53, (byte) 0x53}, // dark blue 0x01
        {(byte) 0x90, (byte) 0x90, (byte) 0x90}, // dark green 0x10
        {(byte) 0x9e, (byte) 0x9e, (byte) 0x9e}, // dark cyan 0x11
        {(byte) 0x49, (byte) 0x49, (byte) 0x49}, // dark red 0x04
        {(byte) 0x5b, (byte) 0x5b, (byte) 0x5b}, // dark magenta 0x05
        {(byte) 0x94, (byte) 0x94, (byte) 0x94}, // dark yellow 0x14
        {(byte) 0xd2, (byte) 0xd2, (byte) 0xd2}, // dark gray 0x15
        {(byte) 0x00, (byte) 0x00, (byte) 0x00}, // light black 0x40
        {(byte) 0x61, (byte) 0x61, (byte) 0x61}, // blue 0x41
        {(byte) 0xdb, (byte) 0xdb, (byte) 0xdb}, // green 0x50
        {(byte) 0xf7, (byte) 0xf7, (byte) 0xf7}, // cyan 0x51
        {(byte) 0x4d, (byte) 0x4d, (byte) 0x4d}, // red 0x44
        {(byte) 0x69, (byte) 0x69, (byte) 0x69}, // magenta 0x45
        {(byte) 0xe3, (byte) 0xe3, (byte) 0xe3}, // yellow 0x54
        {(byte) 0xff, (byte) 0xff, (byte) 0xff} // white 0x55
    };
    private static byte[][] palette;
    /**
     * Reference to memory object.
     */
    private Memory memory;
    // Indexed images - palette operations are cheap.
    private BufferedImage img2, img4, img16, activeImage;
    /**
     * Customizable scaling quality.
     */
    private Object scalingQuality;

    /**
     * Initialize screen buffers and registers.
     *
     * @param memory used to read video memory.
     */
    public Screen(Memory memory) {
        this.memory = memory;
        colorKillEnable = 0x0;
        palette = colors;
        scalingQuality = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        mc6845 = new MC6845();
        // These registers store values as 4 IGRB bits (in reality)
        paletteRegisters = new Reg8Masked[4];
        borderRegister = new Reg8Masked("Border", 0xF);
        modeRegister = new Reg8Masked("VideoMode", 0x3);
        videoPageRegister = new Reg8Masked("VideoPage", 0x3F); // unused yet, HBA2 only (TVC64K+)
        for (int i = 0; i < paletteRegisters.length; i++) {
            paletteRegisters[i] = new Reg8Masked("Palette " + i, 0xF);
        }
        setModeRegister(GRAPHICS4);
        IndexColorModel cm17 = createPalette17();
        IndexColorModel cm5 = createPalette5();
        img2 = new BufferedImage(MODE2_XRES + (MODE2_BORDER << 1), MODE_YRES_VISIBLE + (MODE2_BORDER << 1), BufferedImage.TYPE_BYTE_INDEXED, cm5);
        // Fill with border color
        fillBufferedImage(img2, 4);
        img4 = new BufferedImage(MODE4_XRES + (MODE4_BORDER << 1), MODE_YRES_VISIBLE + (MODE4_BORDER << 1), BufferedImage.TYPE_BYTE_INDEXED, cm5);
        fillBufferedImage(img4, 4);
        img16 = new BufferedImage(MODE16_XRES + +(MODE16_BORDER << 1), MODE_YRES_VISIBLE + (MODE16_BORDER << 1), BufferedImage.TYPE_BYTE_INDEXED, cm17);
        // Fill with border color
        fillBufferedImage(img16, 16);
        setSize(CANVAS_XRES + 2 * BORDER_SIZE, CANVAS_YRES + 2 * BORDER_SIZE);
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

    /**
     * Set scaling quality.
     *
     * @param scalingQuality
     */
    public void setScalingQuality(Object scalingQuality) {
        this.scalingQuality = scalingQuality;
    }

    /**
     * Fill buffered image with specified value.
     *
     * @param img
     * @param x
     */
    private void fillBufferedImage(BufferedImage img, int x) {
        WritableRaster raster = img.getRaster();
        Rectangle bounds = raster.getBounds();
        int[] pixels = new int[bounds.width * bounds.height];
        Arrays.fill(pixels, x);
        raster.setPixels(0, 0, (int) bounds.getWidth(), (int) bounds.getHeight(), pixels);
    }

    /**
     * Create indexed color model from TVC Palette colors (16+border).
     *
     * An index is reserved for the border color.
     *
     * @return
     */
    private IndexColorModel createPalette17() {

        byte rb[] = new byte[17];
        byte gb[] = new byte[17];
        byte bb[] = new byte[17];

        for (int i = 0; i < 16; i++) {
            rb[i] = palette[i][0];
            gb[i] = palette[i][1];
            bb[i] = palette[i][2];
        }
        // 17th is the border color
        int b = borderRegister.get();
        Log.getInstance().write("createPalette17 palette br:" + b);
        gb[16] = (byte) palette[b][0];
        rb[16] = (byte) palette[b][1];
        bb[16] = (byte) palette[b][2];

        return new IndexColorModel(5, 17, rb, gb, bb);
    }

    /**
     * Create a new indexed color model from the palette registers (+border).
     *
     * @return color model for the palette registers and the border register.
     */
    private IndexColorModel createPalette5() {
        byte rb[] = new byte[5];
        byte gb[] = new byte[5];
        byte bb[] = new byte[5];
        int b;
        for (int i = 0; i < 4; i++) {
            b = paletteRegisters[i].get();
            gb[i] = (byte) palette[b][0];
            rb[i] = (byte) palette[b][1];
            bb[i] = (byte) palette[b][2];
        }
        b = borderRegister.get();
        //Log.getInstance().write("palette br:" + b);
        // 5th is the border color
        gb[4] = (byte) palette[b][0];
        rb[4] = (byte) palette[b][1];
        bb[4] = (byte) palette[b][2];

        return new IndexColorModel(4, 5, rb, gb, bb);
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, scalingQuality);
        g2d.drawImage(activeImage, 0, 0, getWidth(), getHeight(), null);
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
        if (igrb == paletteRegisters[i].get()) {
            return;
        }
        paletteRegisters[i].set(igrb);
        paletteRegistersChanged = true;
    }

    /**
     * Get palette register value.
     *
     * @param i register index 0..3
     * @return 0I0G0R0B bytes
     */
    public final int getPaletteRegister(int i) {
        int igrb = paletteRegisters[i].get();
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
        if (igrb == borderRegister.get()) {
            return;
        }
        borderRegister.set(igrb);
        borderRegisterChanged = true;
    }

    /**
     * Get border register value.
     *
     * @return I0G0R0B0 bytes
     */
    public final int getBorderRegister() {
        int igrb = borderRegister.get();
        int b = (igrb & 0x8) << 4 | (igrb & 0x4) << 3 | (igrb & 0x2) << 2 | (igrb & 0x1);
        return b;
    }

    /**
     * Set mode register.
     *
     * @param b supported graphics mode required.
     */
    public final void setModeRegister(int b) {
        modeRegister.set(b);
        modeRegisterChanged = true;
    }

    /**
     * Get current mode register.
     *
     * @return current mode register.
     */
    public final int getModeRegister() {
        return modeRegister.get();
    }

    /**
     * Set by UI (push button on real HW).
     *
     * @param colorKillEnable 1 or 0
     */
    public void setColorKillEnable(int colorKillEnable) {
        if (colorKillEnable != 0) {
            palette = mono_colors;
        } else {
            palette = colors;
        }
        paletteRegistersChanged = true;
        this.colorKillEnable = (colorKillEnable << 5) & 0x20;
    }

    /**
     * Read by Ports.
     *
     * Mapped to IO port.
     *
     * @return color kill bit status.
     */
    public int getColorKillEnable() {
        return colorKillEnable;
    }

    /**
     * Returns the BufferedImage associated with the specified videomode.
     *
     * @param mode
     * @return
     */
    private BufferedImage getModePage(int mode) {
        switch (mode) {
            case GRAPHICS16:
                return img16;
            case GRAPHICS4:
                return img4;
            case GRAPHICS2:
                return img2;
            default:
                throw new IllegalArgumentException("Invalid mode page:" + mode);
        }
    }

    /**
     * Updates the BufferedImage associated with the specified mode with the
     * specified BufferedImage.
     *
     * Used after palette modifications.
     *
     * @param mode the affected mode.
     * @param bi the new BufferedImage.
     */
    private void setModePage(int mode, BufferedImage bi) {
        switch (mode) {
            case GRAPHICS16:
                img16 = bi;
                break;
            case GRAPHICS4:
                img4 = bi;
                break;
            case GRAPHICS2:
                img2 = bi;
                break;
            default:
                break;
        }
    }

    /**
     * Perform changes on screen and update graphics register changed statuses.
     *
     */
    public final void updateRegisters(int mr) {
        IndexColorModel icm;

        // Border changed, all image palettes needs to be updated.
        if (borderRegisterChanged) {
            // The borderregister updated is implemented by a palette color swap
            // each palette has an extra color for the border so it can be
            // replaced without affecting image pixels.
            // affects all pages
            icm = createPalette17();
            BufferedImage bi = new BufferedImage(icm, img16.getRaster(), false, null);
            setModePage(GRAPHICS16, bi);
            icm = createPalette5();
            bi = new BufferedImage(icm, img2.getRaster(), false, null);
            setModePage(GRAPHICS2, bi);
            bi = new BufferedImage(icm, img4.getRaster(), false, null);
            setModePage(GRAPHICS4, bi);
            // Don't do it twice.
            paletteRegistersChanged = false;
            borderRegisterChanged = false;
            Log.getInstance().write("borderRegisterChanged");
        }

        // Mode changed, show the new modepage.
        if (modeRegisterChanged) {
            modeRegisterChanged = false;
        }

        if (paletteRegistersChanged) {
            // 16-color mode is unaffected by palette changes
            icm = createPalette17();
            BufferedImage bi = new BufferedImage(icm, img16.getRaster(), false, null);
            setModePage(GRAPHICS16, bi);
            // Palette registers
            // Create a new color model for the current image.
            icm = createPalette5();
            bi = new BufferedImage(icm, img2.getRaster(), false, null);
            setModePage(GRAPHICS2, bi);
            bi = new BufferedImage(icm, img4.getRaster(), false, null);
            setModePage(GRAPHICS4, bi);

            paletteRegistersChanged = false;
        }
        // Graphics mode
        activeImage = getModePage(mr);
        redrawScreen();
    }

    /**
     * Canvas updater thread.
     */
    public void run() {
        Memory.VideoPage vid = ((Memory.VideoPage) memory.VID);
        int m[] = vid.mem;
        boolean pageChanged[] = vid.changed;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        BufferedImage img = null;
        WritableRaster raster = null;

        while (true) {
            if (memory.isVideoChanged()) {
                int mr = getModeRegister();
                updateRegisters(mr);
                try {
                    int vMinA = (vid.minA >> 6) << 6;
                    int vMaxA = (vid.maxA >> 6) << 6;
                    // 1 row is always 64 bytes long.
                    int minY = vMinA >> 6;
                    int maxY = vMaxA >> 6;
                    // The maximum modified address
                    // The TVC happily writes to the nonvisible area but we have a
                    // smaller canvas
                    //maxY = Math.min(maxY, minY + 4);
                    // 3 rows are written on the border. This would cause boundary
                    // exception without the border unless truncated to visible area.
                    //maxY = Math.min(maxY, MODE_YRES_VISIBLE);

                    int a = 0; // byte address
                    switch (mr) {
                        case GRAPHICS2: {
                            img = img2;
                            raster = img.getRaster();
                            for (int j = minY; j < maxY; j++) {
                                for (int i = 0; i < MODE2_XRES; i += 8) {
                                    // copy video ram contents to raster
                                    int b = m[vMinA + a];
                                    int pixels[] = new int[8];
                                    for (int mask = 0x80, cnt = 0; cnt < 7; mask >>= 1, cnt++) {
                                        pixels[cnt] = (b & mask) >> (7 - cnt);
                                    }
                                    raster.setPixels(i + MODE2_BORDER, j + MODE2_BORDER, 8, 1, pixels);
                                    a++;
                                }
                            }
                        }
                        break;

                        // this is the default
                        case GRAPHICS4: {
                            img = img4;
                            raster = img.getRaster();
                            for (int j = minY; j < maxY; j++) {
                                for (int i = 0; i < MODE4_XRES; i += 4) {
                                    // copy video ram contents to raster
                                    int b = m[vMinA + a];
                                    int pixels[] = new int[4];
                                    // L1 L2 L3 L4 H1 H2 H3 H4
                                    pixels[0] = ((b & 0x80) >> 7) | ((b & 0x8) >> 2);
                                    pixels[1] = ((b & 0x40) >> 6) | ((b & 0x4) >> 1);
                                    pixels[2] = ((b & 0x20) >> 5) | ((b & 0x2));
                                    pixels[3] = ((b & 0x10) >> 4) | ((b & 0x1) << 1);
                                    raster.setPixels(i + MODE4_BORDER, j + MODE4_BORDER, 4, 1, pixels);
                                    a++;
                                }
                            }
                        }
                        break;
                        case GRAPHICS16: {
                            img = img16;
                            raster = img.getRaster();
                            for (int j = minY; j < maxY; j++) {
                                for (int i = 0; i < MODE16_XRES; i += 2) {
                                    // copy video ram contents to raster
                                    int b = m[vMinA + a];
                                    int pixels[] = new int[2];
                                    // convert I1 I2 G1 G2 R1 R2 B1 B2 pixel data
                                    // P1 I0G0R0B0 -> 0000IRGB
                                    pixels[0] = ((b & 0x80) >> 4) | ((b & 0x20) >> 4) | ((b & 0x8) >> 1) | ((b & 0x2) >> 1);
                                    // P2 0I0G0R0B -> 0000IRGB
                                    pixels[1] = ((b & 0x40) >> 3) | ((b & 0x10) >> 3) | ((b & 0x4)) | ((b & 0x1));
                                    raster.setPixels(i + MODE16_BORDER, j + MODE16_BORDER, 2, 1, pixels);
                                    a++;
                                }
                            }
                        }
                        break;
                    }
                    Arrays.fill(pageChanged, vMinA, vMinA + a, false);
                    vMinA += a;
                    vMaxA += a;
                    // Reset minimum and maximum affected rows to top:bottom
                    vid.minA = vMinA;
                    vid.maxA = vMaxA;

                } finally {
                    memory.setVideoChanged(false);
                }
                img.setData(raster);
                repaint();
            }

            try {
                Thread.sleep(SCREEN_REPAINT_INTERVAL);
            } catch (InterruptedException ie) {
            }
        }
    }

    /**
     * Mark video memory bytes as changed.
     */
    public void redrawScreen() {
        Memory.VideoPage vid = ((Memory.VideoPage) memory.VID);
        boolean changed[] = vid.changed;
        for (int i = 0; i < 0x3d0e; i++) {
            changed[i] = true;
        }
        vid.maxA = 0x3cff;
        vid.minA = 0;
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