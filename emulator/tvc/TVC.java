package emulator.tvc;

/**
 * @author Gabor
 *
 * Top-level class for the emulated computer.
 *
 * Singleton.
 */
public class TVC implements Runnable {

    private Memory memory;
    private Z80 z80;
    private Port port;
    private Screen screen;
    private Keyboard keyboard;
    private FileIO fileIO;
    private Log log;
    private Sound soundhw;
    private static TVC instance = null;
    public boolean running;
    public boolean SoundINT;
    public boolean WarmReset;
    public boolean ColdReset;


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
        this.running = true;
        new Thread(this).start();
        screen.transferFocus();
    }

    public void stop() {
        this.running = false;
    }

    public void shutdown() {
        log.write("Shutting down.");
        log.close();
        System.exit(0);
    }

    public TVC(GUI gui) {
        log = Log.getInstance();
        log.write("Videoton TV-Computer Emulator started.");
        log.write("(C) Hoffer Gabor, 2003");

        keyboard = new Keyboard();
        memory  = new Memory();
        screen  = new Screen(this, memory);
        soundhw = new Sound();
        fileIO  = new FileIO(memory);
        screen.requestFocus();
        KeyboardListener keyblistener = new KeyboardListener(keyboard, gui);
        screen.addKeyListener(keyblistener);
        screen.addFocusListener(keyblistener);
        port = new Port(keyboard, memory, screen, soundhw);
        // port.tracePORT = true;
        z80 = new Z80(memory, port);
        z80.traceCPU = 0;
        fileIO.loadRomSet();
        SoundINT = false;
    }

    private void reset(boolean cold) {
        // Enable cold or warm reset
        if (cold) {
            getMemory().U0.set(0x0b21, 0xFF);
        } else {
            getMemory().U0.set(0x0b21, 0x00);
        }
        // SYS-U1-VID-CART
        this.port.setPort(2, 0);
        screen.Reset();
        soundhw.Reset();
        z80.reset();
    }

    public void run() {
        long instCount, lastWaitT;
        long m1, lastWaitMills;
        long t0;
        double d1, d2;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        soundhw.Open();
        z80.t = 0;
        instCount = 0;
        lastWaitT = 0;
        lastWaitMills = System.currentTimeMillis();
        m1 = lastWaitMills;
        while (running) {

            if (ColdReset || WarmReset) {
                reset(ColdReset);
                ColdReset = false;
                WarmReset = false;
            }
            t0 = z80.t;
            if (screen.getCursorInt() == 0) {
                z80.interrupt();
            }
            running = z80.run();
            instCount++;
            int mcycles = (int)(z80.t - t0);
            SoundINT = soundhw.run(mcycles);
            boolean screen_ready = screen.run(mcycles);
            /*
             * Z80 frequency: 3.125Mhz ==> 3125000 T Cycle/s
             * 10 ms ==> 31250 T Cycle
             * 20 ms ==> 62500 T Cycle
             */
            if (!soundhw.AudioPlay && screen_ready) {
                long cputime_ms = (z80.t - lastWaitT) / 3125;
                lastWaitT = z80.t;
                long h = cputime_ms - (System.currentTimeMillis() - lastWaitMills);
                if (h > 20) h = 20;
                if (h < 1 ) h = 1;
                lastWaitMills = System.currentTimeMillis();
                if ( h > 0) {
                  try {
                    Thread.sleep( h);
                  } catch (Exception e) {
                  }
               }
            }
        }

        m1 = System.currentTimeMillis() - m1;
        log.write("Elapsed time: " + Long.toString(m1));
        log.write("Executed instuctions: " + Long.toString(instCount));
        log.write("T Cycles:" + Long.toString(z80.t));
        d1 = z80.t;
        d2 = (double) m1 / 1000;
        log.write("Frequency: " + Double.toString((d1 / d2) / 1000000));
        soundhw.Close();
    }
}
