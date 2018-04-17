package emulator.tvc;

/**
 * @author Gabor
 *
 * Top-level class for the emulated computer.
 *
 * Singleton.
 */
public class TVC {

    private Memory memory;
    private Z80 z80;
    private Port port;
    private Screen screen;
    private Keyboard keyboard;
    private FileIO fileIO;
    private Log log;
    private static TVC instance = null;

    public static TVC getInstance() {
        if (null == instance) {
            instance = new TVC();
        }
        return instance;
    }

    public Screen getScreen() {
        return screen;
    }

    public Z80 getZ80() {
        return z80;
    }

    public Memory getMemory() {
        return memory;
    }

    public FileIO getFileIO() {
        return fileIO;
    }

    public void start() {
        new Thread(z80).start();
        new Thread(screen).start();
        screen.transferFocus();
    }

    public void stop() {
        z80.running = false;
    }

    private TVC() {
        log = Log.getInstance();
        log.write("Videoton TV-Computer Emulator started.");
        log.write("(C) Hoffer Gabor, 2003");

        keyboard = new Keyboard();
        memory = new Memory();
        screen = new Screen(memory);
        fileIO = new FileIO(memory);
        screen.requestFocus();
        screen.addKeyListener(new KeyboardListener(keyboard));
        port = new Port(keyboard, memory, screen);
        // port.tracePORT = true;
        z80 = new Z80(memory, port);
        z80.traceCPU = 0;
        fileIO.loadRomSet();
    }

    /**
     * Perform hardware reset.
     * 
     * @param cold if true, perform cold reset, otherwise warm reset.
     */
    public void reset(boolean cold) {
        log.write("Reset");

        log.write("Warm Reset");
        // Enable warm reset
        if (cold) {
            getMemory().U0.set(0x0b21, 0xFF);
        } else {
            getMemory().U0.set(0x0b21, 0x00);
        }
        // SYS-U1-VID-CART
        this.port.setPort(2, 0);
        // Graphics mode 4
        this.port.setPort(6, 0x1);
        // disable interrupts
        getZ80().IFF1 = false;
        // Jump to address 0
        getZ80().PC.r = 0;
    }

    public void shutdown() {
        log.write("Shutting down.");
        log.close();
        System.exit(0);
    }
}
