/**
 *
 */
package emulator.tvc;

/**
 * TODO just a start for the CRTC, not used anywhere.
 *
 * @author szabados
 */
public class MC6845 {

    public static final int HORIZONTAL_TOTAL_REG = 0;
    public static final int HORIZONTAL_DISPLAYED_REG = 1;
    public static final int SYNC_POSITION_REG = 2;
    public static final int HORIZONTAL_SYNC_WIDTH_REG = 3;
    public static final int VERTICAL_TOTAL_REG = 4;
    public static final int VERTICAL_TOTAL_ADJUST_REG = 5;
    public static final int VERTICAL_DISPLAYED_REG = 6;
    public static final int VERTICAL_SYNC_POSITION_REG = 7;
    public static final int INTERLACE_MODE_REG = 8;
    public static final int MAX_SCAN_LINE_ADDRESS_REG = 9;
    public static final int CURSOR_START_REG = 10;
    public static final int CURSOR_END_REG = 11;
    public static final int START_ADDRESS_REG1 = 12;
    public static final int START_ADDRESS_REG2 = 13;
    public static final int CURSOR_ADDRESS_REG1 = 14;
    public static final int CURSOR_ADDRESS_REG2 = 15;
    public static final int LIGHT_PEN_REG1 = 16;
    public static final int LIGHT_PEN_REG2 = 17;
    /**
     * CRTC registers.
     */
    private Reg8Masked registers[];
    private Reg8Masked address;

    /**
     * Initialize CRTC and palette registers.
     */
    private void initRegisters() {
        address = new Reg8Masked("Address", 0x1F);
        address.set(HORIZONTAL_TOTAL_REG);
        registers = new Reg8Masked[18];
        registers[HORIZONTAL_TOTAL_REG] = new Reg8Masked("Horizontal Total", 0xFF);
        registers[HORIZONTAL_DISPLAYED_REG] = new Reg8Masked("Horizontal Displayed", 0xFF);
        registers[SYNC_POSITION_REG] = new Reg8Masked("H.Sync Position", 0xFF);
        registers[HORIZONTAL_SYNC_WIDTH_REG] = new Reg8Masked("Sync Width", 0xFF);
        registers[VERTICAL_TOTAL_REG] = new Reg8Masked("Vertical Total", 0x7F);
        registers[VERTICAL_TOTAL_ADJUST_REG] = new Reg8Masked("V. Total Adjust", 0x1F);
        registers[VERTICAL_DISPLAYED_REG] = new Reg8Masked("Vertical Displayed", 0x7F);
        registers[VERTICAL_SYNC_POSITION_REG] = new Reg8Masked("V. Sync Position", 0x7F);
        registers[INTERLACE_MODE_REG] = new Reg8Masked("Interlace Mode and Skew", 0xF3);
        registers[MAX_SCAN_LINE_ADDRESS_REG] = new Reg8Masked("Max Scan Line Address", 0x1F);
        registers[CURSOR_START_REG] = new Reg8Masked("Cursor Start", 0x7F);
        registers[CURSOR_END_REG] = new Reg8Masked("Cursor End", 0x1F);
        registers[START_ADDRESS_REG1] = new Reg8Masked("Start Address(H)", 0xFF);
        registers[START_ADDRESS_REG2] = new Reg8Masked("Start Address(L)", 0xFF);
        registers[CURSOR_ADDRESS_REG1] = new Reg8Masked("Cursor(H)", 0xFF);
        registers[CURSOR_ADDRESS_REG2] = new Reg8Masked("Cursor(L)", 0xFF);
        registers[LIGHT_PEN_REG1] = new Reg8Masked("Light Pen(H)", 0xFF);
        registers[LIGHT_PEN_REG2] = new Reg8Masked("Light Pen(L)", 0xFF);
    }

    /**
     * Write the currently addressed CRTC register.
     * 
     * The CRTC registers are io mapped at port 0x70 and 0x71.
     * @param data 
     */
    public void writeReg(int data) {
        int a = address.get();
        if (a < registers.length) {
            writeReg(a, data);
        }
    }

    /**
     * Read the currently addressed CRTC register.
     * @return 
     */
    public int readReg() {
        if (address.get() < registers.length) {
            return readReg(address.get());
        }
        return registers[0].get();
    }

    /**
     * Write only registers are excluded.
     *
     * @param i
     * @return
     */
    private int readReg(int i) {
        if (i > 11) {
            return registers[i].r;
        }
        return 0xFF;
    }

    /**
     * Read only registers are excluded.
     *
     * @param i
     * @param value
     */
    private void writeReg(int i, int value) {
        if (i < 16) {
            registers[i].r = value;
        }
    }
    
    public int readAddress() {
        Log.getInstance().write("address:"+address.get());
        return address.get();
    }
    
    public void writeAddress(int i) {
        address.set(i);
    }


    public MC6845() {
        initRegisters();
    }
}
