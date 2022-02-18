/**
 *
 */
package emulator.tvc;

/**
 * TODO just a start for the CRTC.
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
    private int registers[];
    private int address;
    private int r10_cursorMode;
    private boolean r08_interlacedMode, r08_interlacedMode2;

    /**
     * Internal variables
     */

    // Horizontal, vertical and address counters
    private int h_counter     = 0;
    private int line_counter  = 0;
    private int row_counter   = 0;
    private int field_counter = 0;
    private int ma_row_start  = 0;
    private boolean odd_field = false;
    
    // Video timing and sync counters
    private int h_sync_counter = 0;
    private int v_sync_counter = 0;
    private boolean h_display  = false;
    private boolean v_display  = false;

    // Cursor control
    private boolean cursor_i    = false;
    private boolean cursor_line = false;

    // Light pen capture
    private boolean lpstb_i = false;

    /**
     * Interface signals
     */
    public int RA = 0;
    public int MA = 0;
    public boolean HSYNC  = false;
    public boolean VSYNC  = false;
    public boolean DE     = false;
    public boolean CURSOR = false;
    public boolean LPSTB  = false;

    /**
     * Initialize CRTC registers.
     */
    private void initRegisters() {
        address = HORIZONTAL_TOTAL_REG;
        registers = new int[18];
    }

    /**
     * Write the currently addressed CRTC register.
     *
     * The CRTC registers are io mapped at port 0x70 and 0x71.
     * @param data
     */
    public void writeReg(int data) {
        if (address < registers.length) {
            writeReg(address, data);
        }
    }

    /**
     * Read the currently addressed CRTC register.
     * @return
     */
    public int readReg() {
        if (address < registers.length) {
            return readReg(address);
        }
        return registers[0];
    }

    /**
     * Write only registers are excluded.
     *
     * @param i
     * @return
     */
    private int readReg(int i) {
        if (i > 11) {
            return registers[i];
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
            registers[i] = value;
        }
        switch (i) {
            case INTERLACE_MODE_REG:
                r08_interlacedMode  = ((value & 0x01) == 0x01);
                r08_interlacedMode2 = ((value & 0x03) == 0x03);
                break;
            case CURSOR_START_REG:
                r10_cursorMode = (value >> 5) & 0x03;
                break;
            default:
                break;
        }
    }

    public int readAddress() {
        //Log.getInstance().write("address:"+address.r);
        return address;
    }

    public void writeAddress(int i) {
        address = i & 0x1F;
    }

    public MC6845() {
        initRegisters();
    }

    /* Reset CRTC */
    public void reset() {

    // Reset registers to defaults
        registers[HORIZONTAL_TOTAL_REG]       = 0;
        registers[HORIZONTAL_DISPLAYED_REG]   = 0;
        registers[SYNC_POSITION_REG]          = 0;
        registers[HORIZONTAL_SYNC_WIDTH_REG]  = 0;
        registers[VERTICAL_TOTAL_REG]         = 0;
        registers[VERTICAL_TOTAL_ADJUST_REG]  = 0;
        registers[VERTICAL_DISPLAYED_REG]     = 0;
        registers[VERTICAL_SYNC_POSITION_REG] = 0;
        registers[INTERLACE_MODE_REG]         = 0;
        registers[MAX_SCAN_LINE_ADDRESS_REG]  = 0;
        registers[CURSOR_START_REG]           = 0;
        registers[CURSOR_END_REG]             = 0;
        registers[START_ADDRESS_REG1]         = 0;
        registers[START_ADDRESS_REG2]         = 0;
        registers[CURSOR_ADDRESS_REG1]        = 0;
        registers[CURSOR_ADDRESS_REG2]        = 0;
        registers[LIGHT_PEN_REG1]             = 0;
        registers[LIGHT_PEN_REG2]             = 0;

        r08_interlacedMode  = false;
        r08_interlacedMode2 = false;
        r10_cursorMode = 0;

    // Horizontal, vertical and address counters
        // H
        h_counter = 0;

        // V
        line_counter = 0;
        row_counter  = 0;
        odd_field    = false;

        // Fields (cursor flash)
        field_counter = 0;

        // Addressing
        ma_row_start = 0;

    // Video timing and sync counters
        // H
        h_display = false;
        h_sync_counter = 0;

        // V
        v_display = false;
        v_sync_counter = 0;

    // Address generation
        RA = 0;
        MA = 0;

    // Cursor control
        cursor_i = false;
        cursor_line = false;

    // Light pen capture
        lpstb_i = false;
        LPSTB   = false;

        HSYNC  = false;
        VSYNC  = false;
        DE     = h_display && v_display;
        CURSOR = false;
    }

    /**
     * Execute one clock cycle on the CRTC

       Based on original vhdl code of:
       --
       -- MC6845 CRTC
       --
       -- Synchronous implementation for FPGA
       --
       -- (C) 2011 Mike Stirling
       --
     */
    public void run() {

    // Horizontal, vertical and address counters

        // Horizontal counter increments on each clock, wrapping at h_total
        int max_scan_line;
        if ((h_counter & 0xff) == registers[HORIZONTAL_TOTAL_REG]) {
            // h_total reached
            h_counter = 0;

            // In interlace sync + video mode mask off the LSb of the max scan line address
            if (r08_interlacedMode2) {
                max_scan_line = registers[MAX_SCAN_LINE_ADDRESS_REG] & 0x1e;
            }
            else {
                max_scan_line = registers[MAX_SCAN_LINE_ADDRESS_REG];
            }

            if ((row_counter & 0x7f) == registers[VERTICAL_TOTAL_REG]) {
                max_scan_line += registers[VERTICAL_TOTAL_ADJUST_REG];
            }
            // Scan line counter increments, wrapping at max_scan_line_addr
            if ((line_counter & 0x3f) == max_scan_line) {
                // Next character row
                line_counter = 0;
                if ((row_counter & 0x7f) == registers[VERTICAL_TOTAL_REG]) {
                    // If in interlace mode we toggle to the opposite field.
                    // Save on some logic by doing this here rather than at the
                    // end of v_total_adj - it shouldn't make any difference to the
                    // output
                    if (r08_interlacedMode) {
                        odd_field = !odd_field;
                    }
                    else {
                        odd_field = false;
                    }

                    // Address is loaded from start address register at the top of
                    // each field and the row counter is reset
                    ma_row_start = (registers[START_ADDRESS_REG1] << 8) + registers[START_ADDRESS_REG2];
                    row_counter  = 0;

                    // Increment field counter
                    field_counter++;
                }
                else {
                    // On all other character rows within the field the row start address is
                    // increased by h_displayed and the row counter is incremented
                    ma_row_start = ma_row_start + registers[HORIZONTAL_DISPLAYED_REG];
                    row_counter++;
                }
            }
            else {
                // Next scan line.  Count in twos in interlaced sync+video mode
                if (r08_interlacedMode2) {
                    line_counter = line_counter + 2;
                    line_counter = line_counter & 0x7e; // Force to even
                }
                else {
                    line_counter++;
                }
            }

            // Memory address preset to row start at the beginning of each scan line
            MA = ma_row_start;
        }
        else {
            // Increment horizontal counter
            h_counter++;
            // Increment memory address
            MA++;
        }

        // Signals to mark hsync and half way points for generating
        // vsync in even and odd fields
        boolean h_total    = (h_counter == 0);
        boolean h_half_way = (h_counter == (registers[HORIZONTAL_TOTAL_REG] >> 1));

    // Video timing and sync counters

        // Horizontal active video
        if (h_total) {
            // Start of active video
            h_display = true;
        }
        if (h_counter == registers[HORIZONTAL_DISPLAYED_REG]) {
            // End of active video
            h_display = false;
        }

        // Horizontal sync
        if (h_counter == registers[SYNC_POSITION_REG]) {
            // In horizontal sync
            HSYNC = true;
        }
        if ((h_sync_counter & 0x0f) == (registers[HORIZONTAL_SYNC_WIDTH_REG] & 0x0f)) {
            // Terminate hsync after h_sync_width (0 means no hsync so this
            // can immediately override the setting above)
            HSYNC = false;
        }
        h_sync_counter = HSYNC ? (h_sync_counter + 1) : 0;

        // Vertical active video
        if ((row_counter & 0x7f) == 0) {
            // Start of active video
            v_display = true;
        }
        if ((row_counter & 0x7f) == registers[VERTICAL_DISPLAYED_REG]) {
            // End of active video
            v_display = false;
        }

        // External signals
        DE = h_display && v_display;
        
        // Vertical sync occurs either at the same time as the horizontal sync (even fields)
        // or half a line later (odd fields)
        if ((!odd_field && h_total) || (odd_field && h_half_way)) {
            if ((((row_counter & 0x7f) == registers[VERTICAL_SYNC_POSITION_REG]) && ((line_counter & 0x7f) == 0)) || VSYNC) {
                // In vertical sync
                VSYNC = true;
                v_sync_counter++;
            }
            else {
                v_sync_counter = 0;
            }
            if (((v_sync_counter & 0x0f) == (registers[HORIZONTAL_SYNC_WIDTH_REG] >> 4)) && VSYNC) {
                // Terminate vsync after v_sync_width (0 means 16 lines so this is
                // masked by 'VSYNC' to ensure a full turn of the counter in this case)
                VSYNC = false;
            }
        }

    // Address generation

        // Character row address is just the scan line counter delayed by
        // one clock to line up with the syncs.
        if (r08_interlacedMode2) {
            // In interlace sync and video mode the LSb is determined by the
            // field number.  The line counter counts up in 2s in this case.
            RA = odd_field ? (line_counter | 0x01): line_counter;
        }
        else {
            RA = line_counter;
        }

    // Cursor control
        if (DE && (MA == ((registers[CURSOR_ADDRESS_REG1] << 8) + registers[CURSOR_ADDRESS_REG2]))) {
            if ((line_counter & 0x7f) == 0) {
                // Suppress wrap around if last line is > max scan line
                cursor_line = false;
            }
            if ((line_counter & 0x7f) == registers[CURSOR_START_REG]) {
                // First cursor scanline
                cursor_line = true;
            }

            // Cursor output is asserted within the current cursor character on the selected lines only
            cursor_i = cursor_line;

            if ((line_counter & 0x7f) == registers[CURSOR_END_REG]) {
                // Last cursor scanline
                cursor_line = false;
            }
        }
        else {
            // Cursor is off in all character positions apart from the selected one
            cursor_i = false;
        }
 
        // Cursor output generated combinatorially from the internal signal in
        // accordance with the currently selected cursor mode
        CURSOR = (r10_cursorMode == 0) ? cursor_i :
                 (r10_cursorMode == 1) ? false :
                 (r10_cursorMode == 2) ? (cursor_i && ((field_counter & 0x10) != 0)):
                                         (cursor_i && ((field_counter & 0x20) != 0));

    // Light pen capture
        if (LPSTB && !lpstb_i) {
            // Capture address on rising edge
            registers[LIGHT_PEN_REG1] = (MA >> 8) & 0x3f;
            registers[LIGHT_PEN_REG2] = MA & 0xff;
        }
        // Register light-pen strobe input
        lpstb_i = LPSTB;

    }
}
