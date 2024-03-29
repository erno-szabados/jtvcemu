package emulator.tvc;

/**
 * @author Gabor
 *
 */
import java.util.Arrays;

public class Memory {

    public static final int ROM_SIZE = 0x2000;
    public static final int PAGE_SIZE = 0x4000;
    public static final String MAGIC = "JTVC";
    public static final byte VERSION[] = {1, 1};

    class Page {

        int mem[];
        String name;
        boolean rom;

        /**
         * Construct new memory page.
         *
         * Pages has a fixed 16K SIZE.
         *
         *
         * @param name name of page
         * @param video true if this is a video page.
         */
        public Page(String name, boolean rom) {
            mem = new int[PAGE_SIZE];
            Arrays.fill(mem, 0);

            this.name = name;
            this.rom  = rom;
        }

        /**
         * Read byte from specified memory address of page.
         *
         * @param addr
         * @return
         */
        public int get(int addr) {
            return mem[addr];
        }

        /**
         * Write byte to specified memory address of page.
         *
         * @param addr
         * @param p
         */
        public void set(int addr, int p) {
            if (!rom) {
                mem[addr] = p;
            }
          /*  else {
                String s = String.format("Write ROM: addr = 0x%04X  data = 0x%02X", addr, p);
                log.write(s);
            }*/
        }

        public void setrom(int addr, int p) {
            mem[addr] = p;
        }
    }

    /**
     * All the supported memory pages.
     */
    Page SYS, EXT, VID, CART, U0, U1, U2, U3, PAGES[];
    private Log log;

    public Memory() {
        log  = Log.getInstance();
        SYS  = new Page("SYS", true);
        EXT  = new Page("EXT", true);
        VID  = new Page("VID", false);
        CART = new SDcartridge(this, "SDCART");
        U0 = new Page("U0", false);
        U1 = new Page("U1", false);
        U2 = new Page("U2", false);
        U3 = new Page("U3", false);
        PAGES = new Page[4];
    }

    public Page getPageByName(String name) {
        if (U0.name.equals(name)) {
            return U0;
        }
        if (U1.name.equals(name)) {
            return U1;
        }
        if (U2.name.equals(name)) {
            return U2;
        }
        if (U3.name.equals(name)) {
            return U3;
        }
        if (VID.name.equals(name)) {
            return VID;
        }

        throw new IllegalArgumentException("Could not retrieve page by name: " + name);
    }

    /**
     * Read byte from specified address of memory.
     *
     * Calculate page and offset.
     *
     * @param addr
     * @return
     */
    public final int getByte(int addr) {

        int page = (addr >> 14) & 0x03, offset = addr & 0x3fff;

        //return PAGES[page].mem[offset];
        return PAGES[page].get(offset);
    }

    /**
     * Write byte to specified memory address.
     *
     * Calculate page and offset.
     *
     * @param addr
     * @param p
     */
    public final void setByte(int addr, int p) {

        int page = (addr >> 14) & 0x03, offset = addr & 0x3fff;

        PAGES[page].set(offset, p);
    }

    /**
     * Read word from specified address of memory.
     *
     * Calculate page and offset.
     *
     * @param addr
     * @return
     */
    public final int getWord(int addr) {

        return getByte(addr) | (getByte(addr + 1) << 8);
    }

    /**
     * Write word to specified memory address.
     *
     * Calculate page and offset.
     *
     * @param addr
     * @param p
     */
    public final void setWord(int addr, int p) {

        setByte(addr, p & 0xff);
        setByte(addr + 1, (p >> 8));
    }

    /**
     * Set visible pages for Z80.
     *
     * @note Some page combinations are illegal.
     *
     * @param p code for required page.
     */
    public void setPages(int p) {

        //String s = String.format("page = 0x%02x  ", p);

        // Page 0.
        switch (p & 0x18) {
            case (0x00):
                PAGES[0] = SYS;
                //s += "SYS";
                break;
            case (0x08):
                PAGES[0] = CART;
                //s += "CART";
                break;
            case (0x10):
                PAGES[0] = U0;
                //s += "U0";
                break;
            case (0x18):
                PAGES[0] = U3;
                //s += "U3";
                break;
            default:
                // TODO what happens in the real machine?
                //throw new IllegalArgumentException("Illegal page 0 set:" + p);
                break;
        }

        // Page 1.
        PAGES[1] = U1;
        //s += "-U1";

        // Page 2.
        switch (p & 0x20) {
            case (0x00):
                PAGES[2] = VID;
                //s += "-VID";
                break;
            case (0x20):
                PAGES[2] = U2;
                //s += "-U2";
                break;
            default:
                // TODO what happens in the real machine?
                //throw new IllegalArgumentException("Illegal page 2 set:" + p);
                break;
        }

        // Page 3.
        switch (p & 0xc0) {
            case (0x00):
                PAGES[3] = CART;
                //s += "-CART";
                break;
            case (0x40):
                PAGES[3] = SYS;
                //s += "-SYS";
                break;
            case (0x80):
                PAGES[3] = U3;
                //s += "-U3";
                break;
            case (0xc0):
                PAGES[3] = EXT;
                //s += "-EXT";
                break;
            default:
                // TODO what happens in the real machine?
                //throw new IllegalArgumentException("Illegal page 3 set:" + p);
                break;
        }

        //log.write("Memory map changed: " + s);
    }

    /**
     * Dump memory to log for debug.
     *
     * @param pAddr
     * @param pSize
     */
    public void dump(int pAddr, int pSize) {

        String s1 = new String("");
        String s2 = new String("");
        String s3 = new String("");
        int wByte;

        log.write("Memory dump");
        //log.write("Address: 00 01 02 03 04 05 06 07 - 08 09 0a 0b 0c 0d 0e 0f");
        //log.write("----------------------------------------------------------");

        for (int i = 0; i < pSize; i++) {
            if (i % 16 == 0) {
                if (i != 0) {
                    log.write(s1 + "  " + s3);
                }
                s2 = "0000" + Integer.toHexString(pAddr + i);
                s2 = s2.substring(s2.length() - 4);
                s1 = "0x" + s2 + " : ";
                s3 = "";
            }

            wByte = getByte(pAddr + i);
            s2 = "00" + Integer.toHexString(wByte);
            s2 = s2.substring(s2.length() - 2);
            if (i % 8 == 0 && i % 16 != 0) {
                s1 += "- " + s2 + " ";
            } else {
                s1 += s2 + " ";
            }
            s3 += (char) wByte;
        }
        log.write(s1);
    }
}
