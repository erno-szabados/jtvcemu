package emulator.tvc;
/*
 * Created on Jul 17, 2003
 */

/**
 * @author gah
 *
 */
public class Port {
    public static final int PORT_MAX = 256;
    public int port_out[], port_in[];
    public static final boolean tracePort = false;
    Memory memory;
    Screen screen;
    Sound  soundctrl;
    Keyboard keyboard;
    private Log log;

    public Port(Keyboard keyboard, Memory memory, Screen screen, Sound soundctrl) {
        log = Log.getInstance();
        this.keyboard = keyboard;
        this.memory = memory;
        this.screen = screen;
        this.soundctrl = soundctrl;

        port_out = new int[PORT_MAX];
        port_in  = new int[PORT_MAX];
        port_in[0x5a] = 0xff; // peripheral card id 0xff --> all slots empty
        port_in[0x5e] = 0xff; // peripheral card id 0xff --> all slots empty
    }

    /**
     * Write data to specified port.
     *
     * @param num
     * @param p
     */
    public void setPort(int num, int p) {
        if (tracePort) {
            log.write("Port write: " + toHexString(num, 2) + " <== 0x" + toHexString(p, 2));
        }
        switch (num) {
            case 0x00:
                // Border control
                screen.setBorderRegister(p);
                break;
            case 0x02:
                // Write paging register
                memory.setPages(p);
                break;
            case 0x03:
                // Keyboard row and if card selector
                port_out[num] = p;
                break;
            case 0x04:
                // Sound control
                soundctrl.setPitchLow(p);
                break;
            case 0x05:
                // Sound control
                soundctrl.setPitchHigh(p);
                break;
            case 0x06:
                // Write screen mode register
                screen.setModeRegister(p & 0x3);
                // Write sound amplitude
                soundctrl.setAmp(p);
                // The other functions are not implemented
                break;
            case 0x07:
                // acknowledge interrupt
                screen.clearCursorInt();
                break;
            case 0x50:
            case 0x51:
            case 0x52:
            case 0x53:
            case 0x54:
            case 0x55:
            case 0x56:
            case 0x57:
                // tape output
                break;
            // palette registers (with shadow/undecoded adresses)
            case 0x60:
            case 0x61:
            case 0x62:
            case 0x63:
            case 0x64:
            case 0x65:
            case 0x66:
            case 0x67:
            case 0x68:
            case 0x69:
            case 0x6A:
            case 0x6B:
            case 0x6C:
            case 0x6D:
            case 0x6E:
            case 0x6F:
                // write value to port
                screen.setPaletteRegister(num & 0x03, p);
                break;
            case 0x70:
            case 0x72:
            case 0x74:
            case 0x76:
            case 0x78:
            case 0x7A:
            case 0x7C:
            case 0x7E:
                screen.getMc6845().writeAddress(p);
                break;
            case 0x71:
            case 0x73:
            case 0x75:
            case 0x77:
            case 0x79:
            case 0x7B:
            case 0x7D:
            case 0x7F:
                screen.getMc6845().writeReg(p);
                break;
            default:
                port_out[num] = p;
                break;
        }
    }

    /**
     * Read data from specified port.
     *
     * @param num
     * @return
     */
    public int getPort(int num) {
        int result = 0;

        switch (num) {
            case 0x58:
            case 0x5C:
                // Return the kbd matrix row that the CPU scans.
                result = keyboard.getMatrixAt(port_out[0x03] & 0x0f);
                break;
            case 0x59:
            case 0x5D:
                result = screen.getCursorInt() | screen.getColorKillEnable();
                result = result | 0x0f;  // no interface card interrupt
                break;
            case 0x5A:
            case 0x5E:
                // peripheral card id
                result = port_in[0x5A];
                break;
            case 0x5B:
            case 0x5F:
                // reset/reload sound oscillator
                break;
            case 0x70:
            case 0x72:
            case 0x74:
            case 0x76:
            case 0x78:
            case 0x7A:
            case 0x7C:
            case 0x7E:
                result = screen.getMc6845().readAddress();
                break;
            case 0x71:
            case 0x73:
            case 0x75:
            case 0x77:
            case 0x79:
            case 0x7B:
            case 0x7D:
            case 0x7F:
                result = screen.getMc6845().readReg();
                break;
            default:
                result = 0x0FF;
                break;
        }

        if (tracePort) {
            log.write("Port read:  " + toHexString(num, 2) + " ==> 0x" + toHexString(result, 2));
        }

        return result;
    }

    /**
     * Helper to dump hex strings.
     *
     * @param pValue
     * @param pLen
     * @return
     */
    private String toHexString( int pValue, int pLen) {

        String s = "0000000" + Integer.toHexString( pValue);
        return s.substring( s.length()- pLen);
    }
}
