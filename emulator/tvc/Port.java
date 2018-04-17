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
    public boolean tracePort = false;
    Memory memory;
    Screen screen;
    Keyboard keyboard;
    private Log log;

    public Port(Keyboard keyboard, Memory memory, Screen screen) {       
        log = Log.getInstance();
        this.keyboard = keyboard;
        this.memory = memory;
        this.screen = screen;

        port_out = new int[PORT_MAX];
        port_in = new int[PORT_MAX];
        port_in[0x5a] = 0xff; // peripheral card id 0xff --> all slots empty
        port_in[0x59] = 0xff; // peripheral card id 0xff --> all slots empty
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
                port_out[num] = p;
                break;
            case 0x02: 
                // Write paging register
                memory.setPages(p);
                port_out[num] = p;
                break;
            case 0x04: 
                port_out[num] = p;
                // Sound control
                // TODO
                break;
            case 0x05: // Sound control
                port_out[num] = p;
                break;
            case 0x06:
                // Write screen mode register
                screen.setModeRegister(p & 0x3);
                port_out[num] = p;
                // The other functions are not implemented
                break;
            case 0x07:
                port_in[0x59] |= 0x10;
                break;	// acknowledge interrupt
//			case 0x50: port_out[num] ^= 0xff; log.write( "Flip-Flop" + port_out[num]); break;
            // palette registers
            case 0x60:
            case 0x61:
            case 0x62:
            case 0x63:
                // write value to port
                screen.setPaletteRegister(num & 0x03, p);
                break;
            case 0x70:
                screen.getMc6845().writeAddress(p);
                break;
            case 0x71:
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
            case 0x00:
                result = screen.getBorderRegister();
                break;
            case 0x06:
                // TODO mask bits together
                result = port_in[num];//|= screen.getModeRegister();
                break;
            case 0x58:
                // Return the kdb matrix row that the CPU scans.
                result = keyboard.getMatrixAt(port_out[0x03] & 0x0f);
                break;
            case 0x59:
                result = port_in[num] | screen.getColorKillEnable();
                break;
            case 0x60:
            case 0x61:
            case 0x62:
            case 0x63:
                result = screen.getPaletteRegister(num & 0x3);
                break;
            case 0x70:
                result = screen.getMc6845().readAddress();
                break;
            case 0x71:
                result = screen.getMc6845().readReg();
                break;
            default:
                result = port_in[num];
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
