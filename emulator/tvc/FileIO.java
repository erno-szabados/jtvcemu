/**
 * $Id: $
 */
package emulator.tvc;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * File IO routines to read/save emulator state, memory, tape images, etc.
 */
public class FileIO {

    public static final String ROM_TVC12_D4BIN = "/rom/TVC12_D4.BIN";
    public static final String ROM_TVC12_D3BIN = "/rom/TVC12_D3.BIN";
    public static final String ROM_TVC12_D7BIN = "/rom/TVC12_D7.BIN";
    public static final int UPM_HEADER_SIZE = 128;
    public static final int UPM_NONBUFFERED_HEADER_SIZE = 16;
    public static final int UPM_HEADER_FILETYPE_OFFSET = 0x0;
    public static final int UPM_HEADER_COPYPROTECT_OFFSET = 0x1;
    public static final int UPM_HEADER_BLOCK_NUMBER_OFFSET_LO = 0x2;
    public static final int UPM_HEADER_BLOCK_NUMBER_OFFSET_HI = 0x3;
    public static final int UPM_HEADER_LAST_BLOCK_BYTES = 0x4;
    // ends at x7f
    public static final int UPM_HEADER_NONBUFFERED_ID = 0x80;
    public static final int UPM_HEADER_NONBUFFERED_TYPE = 0x81;
    public static final int UPM_HEADER_NONBUFFERED_SIZE_LO = 0x82;
    public static final int UPM_HEADER_NONBUFFERED_SIZE_HI = 0x83;
    public static final int UPM_HEADER_NONBUFFERED_AUTORUN = 0x84;
    public static final int UPM_HEADER_NONBUFFERED_VERSION = 0x8F;
    public static final int UPM_HEADER_NONBUFFERED_DATA_START = 0x90;
    public static final int FILETYPE_BUFFERED = 0x01;
    public static final int FILETYPE_NONBUFFERED = 0x11;
    public static final int NONBUFFERED_AUTOSTART = 0xFF;
    public static final int UPM_BLOCK_SIZE = 0x80;
    public static final int BASIC_OFFSET_START = 0x19EF;
    private Log log;
    private Memory memory;

    public FileIO(Memory memory) {
        log = Log.getInstance();
        this.memory = memory;
    }

    public void loadCAS(String fileName) throws FileNotFoundException, IOException {
        byte[] header = new byte[UPM_HEADER_SIZE];
        byte[] nbheader = new byte[UPM_NONBUFFERED_HEADER_SIZE];
        FileInputStream fis = new FileInputStream(fileName);
        int size = (int) fis.getChannel().size();
        if (size < UPM_HEADER_SIZE) {
            throw new IOException("File smaller than UPM header:" + fileName);
        }
        fis.read(header);
        byte fileType = header[UPM_HEADER_FILETYPE_OFFSET];
        int blockCount = ((header[UPM_HEADER_BLOCK_NUMBER_OFFSET_HI] << 8) & 0xFF00) | ((header[UPM_HEADER_BLOCK_NUMBER_OFFSET_LO] & 0x00FF));
        // size in bytes
        if (fileType != FILETYPE_NONBUFFERED) {
            throw new IOException("Only non-buffered CAS files supported for now:" + fileName);
        }
        int dataSize = ((blockCount - 1) << 7) + header[UPM_HEADER_LAST_BLOCK_BYTES];
        // substract nonbuffered header size
        dataSize -= UPM_NONBUFFERED_HEADER_SIZE;
        fis.read(nbheader);
        log.write("CAS: type: " + (fileType == FILETYPE_NONBUFFERED ? "non-buffered" : "buffered"));
        log.write("CAS: blocks: " + blockCount + ":" + header[UPM_HEADER_BLOCK_NUMBER_OFFSET_LO] + "-" + header[UPM_HEADER_BLOCK_NUMBER_OFFSET_HI]);
        log.write("CAS: bytes: " + dataSize);
        log.write("CAS: version " + nbheader[UPM_HEADER_NONBUFFERED_VERSION - 0x80]);

        if (size < UPM_HEADER_SIZE + dataSize) {
            throw new IOException("File smaller than reported data:" + fileName);
        }

        byte[] data = new byte[dataSize];
        fis.read(data);

        // Start loading with U0
        Memory.Page page = memory.getPageByName("U0");        
        int pageDataSize = Math.min(dataSize, Memory.PAGE_SIZE - BASIC_OFFSET_START);
        for (int i = 0; i < pageDataSize; i++) {
            page.set(i + BASIC_OFFSET_START, data[i] & 0xFF);
        }
        int offset = 0;
        if (dataSize > pageDataSize) {
            dataSize -= pageDataSize;            
            // Increase offset by amount of data loaded to page U0
            offset += Memory.PAGE_SIZE - BASIC_OFFSET_START;
            // More data left, load to page U1
            page = memory.getPageByName("U1");
            pageDataSize = Math.min(dataSize, Memory.PAGE_SIZE);
            for (int i = 0; i < pageDataSize; i++) {
                page.set(i, data[i + offset] & 0xFF);
            }
        }
        if (dataSize > pageDataSize) {
            dataSize -= pageDataSize;            
            // Increase offset by amount of data loaded to page U1
            offset += Memory.PAGE_SIZE;
            // More data left, load to page U1
            page = memory.getPageByName("U2");
            pageDataSize = Math.min(dataSize, Memory.PAGE_SIZE);
            for (int i = 0; i < pageDataSize; i++) {
                page.set(i, data[i + offset] & 0xFF);
            }
        }
        if (dataSize > pageDataSize) {
            dataSize -= pageDataSize;            
            // Increase offset by amount of data loaded to page U2
            offset += Memory.PAGE_SIZE;
            // More data left, load to page U1
            page = memory.getPageByName("U3");
            pageDataSize = Math.min(dataSize, Memory.PAGE_SIZE);
            for (int i = 0; i < pageDataSize; i++) {
                page.set(i, data[i + offset] & 0xFF);
            }
        }
        dataSize -= pageDataSize;
        if (dataSize != 0) {
            log.write("Memory full, D:" + dataSize + "P:" + pageDataSize );
        }

        fis.close();
    }


    /**
     * Load ROM from binary file.
     *
     * @param fileName ROM binary.
     * @param page destination memory page.
     * @param offset offset at destination page.
     * @throws FileNotFoundException if ROM is not available.
     * @throws IOException if ROM is not readable.
     */
    private void loadRom(String fileName, Memory.Page page, int size, int offset) throws FileNotFoundException, IOException {
        byte imgBytes[] = new byte[size];

        InputStream fis = getClass().getResourceAsStream(fileName);
        int result = fis.read(imgBytes);
        fis.close();
        if (result != imgBytes.length) {
            throw new IOException("ROM file read error:" + fileName + " at:" + result);
        }
        for (int i = 0; i < size; i++) {
            page.set(i + offset, imgBytes[i] & 0xFF);
        }
        log.write("ROM " + fileName + " has been loaded into " + page.name + ":" + offset);
    }

    /**
     * Load binary file to specified page:offset from file.
     *
     * @param filename
     * @param page
     * @param offset
     */
    public void loadBinary(String fileName, Memory.Page page, int offset) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(fileName);
        int size = (int) fis.getChannel().size();

        size = Math.min(size, Memory.PAGE_SIZE - offset);
        byte[] buf = new byte[size];
        fis.read(buf);
        for (int i = 0; i < size; i++) {
            page.set(i + offset, buf[i] & 0xFF);
        }
    }

    /**
     * Save bytes from the specified page:offset...size range.
     *
     * @param fileName target file.
     * @param page source page.
     * @param offset page offset.
     * @param size saved data size.
     * @throws IOException on write error.
     */
    public void saveBinary(String fileName, Memory.Page page, int offset, int size) throws IOException {
        OutputStream os = new FileOutputStream(fileName);
        DataOutputStream daos = new DataOutputStream(os);
        if (offset + size > Memory.PAGE_SIZE) {
            throw new IllegalArgumentException("offset:size exceeeds page size:" + size);
        }

        for (int i = 0; i < size; i++) {
            daos.writeByte(page.get(i + offset) & 0xFF);
        }
        daos.flush();
        daos.close();
        os.close();

        Log.getInstance().write("Binary data saved.");
    }

    /**
     * Very rudimentary memory dump.
     *
     * The z80 should be halted while loading mem.
     *
     * @param fileName
     * @throws IOException
     */
    public void loadUserMem(String fileName) throws FileNotFoundException, IOException {
        InputStream fis = new FileInputStream(fileName);
        byte headerbuf[] = new byte[6];
        int result = fis.read(headerbuf);
        ByteBuffer bbuf = ByteBuffer.wrap(headerbuf);
        byte magic[] = new byte[4];
        bbuf.get(magic);
        String magicString = new String(magic);
        if (!magicString.equals(Memory.MAGIC)) {
            throw new IOException("Magic not found:" + magicString + "]");
        }
        byte version[] = new byte[2];
        bbuf.get(version);
        Log.getInstance().write("Version:" + version[0] + "." + version[1]);

        byte imgBytes[] = new byte[Memory.PAGE_SIZE * 5];
        result = fis.read(imgBytes);
        fis.close();
        if (result != imgBytes.length) {
            throw new IOException("File read error:" + fileName + " at:" + result);
        }
        for (int i = 0; i < Memory.PAGE_SIZE; i++) {
            memory.U0.set(i, imgBytes[i] & 0xFF);
            memory.U1.set(i, imgBytes[i + Memory.PAGE_SIZE] & 0xFF);
            memory.U2.set(i, imgBytes[i + Memory.PAGE_SIZE * 2] & 0xFF);
            memory.U3.set(i, imgBytes[i + Memory.PAGE_SIZE * 3] & 0xFF);
            memory.VID.set(i, imgBytes[i + Memory.PAGE_SIZE * 4] & 0xFF);
        }
        Log.getInstance().write("RAM loaded.");
    }

    /**
     * The z80 should be halted while saving mem.
     *
     * @param fileName
     * @throws IOException
     */
    public void saveUserMem(String fileName) throws IOException {
        OutputStream os = new FileOutputStream(fileName);
        DataOutputStream daos = new DataOutputStream(os);
        daos.write(Memory.MAGIC.getBytes());
        daos.write(Memory.VERSION);

        for (int i = 0; i < Memory.PAGE_SIZE; i++) {
            daos.writeByte(memory.U0.get(i) & 0xFF);
        }
        for (int i = 0; i < Memory.PAGE_SIZE; i++) {
            daos.writeByte(memory.U1.get(i) & 0xFF);
        }
        for (int i = 0; i < Memory.PAGE_SIZE; i++) {
            daos.writeByte(memory.U2.get(i) & 0xFF);
        }
        for (int i = 0; i < Memory.PAGE_SIZE; i++) {
            daos.writeByte(memory.U3.get(i) & 0xFF);
        }
        for (int i = 0; i < Memory.PAGE_SIZE; i++) {
            daos.writeByte(memory.VID.get(i) & 0xFF);
        }
        daos.flush();
        daos.close();
        os.close();

        Log.getInstance().write("RAM dumped.");
    }

    /**
     * Load the 1.2 ROM set and set default paging.
     *
     * @return true on success, false on error.
     */
    public boolean loadRomSet() {
        try {
            loadRom(ROM_TVC12_D4BIN, memory.SYS, Memory.ROM_SIZE, 0);
            loadRom(ROM_TVC12_D3BIN, memory.SYS, Memory.ROM_SIZE, Memory.ROM_SIZE);
            loadRom(ROM_TVC12_D7BIN, memory.EXT, Memory.ROM_SIZE, Memory.ROM_SIZE);
        } catch (FileNotFoundException e) {
            log.write(e.getMessage());
            return false;
        } catch (IOException ee) {
            log.write(ee.getMessage());
            return false;
        }

        // SYS-U1-VID-CART
        memory.setPages(0);
        /*
         * @note when the ROM initialization finishes, it should be 0x70:
         * U0-U1-U2-SYS
         */

        return true;
    }
}
