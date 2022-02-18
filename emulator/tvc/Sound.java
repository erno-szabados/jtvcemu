package emulator.tvc;

import javax.sound.sampled.*;

/**
 * Sound HW emulation
 *
 */
public class Sound {

    private int counter12bit;
    private int counter4bit;
    private int pitch;
    private int amp;
    private int sampler;
    private int sindx;
    private boolean SoundIntEn;
    private boolean SoundEn;
    private SourceDataLine line;
    private byte buffer[];
    private final int SAMPLING_RATE    = 22050;         // Audio sampling rate
    private final int AUDIO_BUFF_SIZE  = 4410;          // max. 200 ms delay on the audio playback
    private final int SAMPLE_BUFF_SIZE = 100;           // ~6.4 ms
    private final int SAMPLE_SIZE = 1;                  // Audio sample size in bytes
    private final int NSAMPLE = ((int)((3125000 + SAMPLING_RATE - 1)/ SAMPLING_RATE));  // CPU cycles for an audio sample
    public static final boolean AudioPlay = true;

    public void setAmp(int port6) {
        this.amp = port6 & 0x03c;
    }

    public void setPitchHigh(int port5) {
        this.pitch = (pitch & 0x0ff) | ((port5 & 0x0f) << 8);
        this.SoundEn = (boolean)((port5 & 0x10) != 0);
        this.SoundIntEn = (boolean)((port5 & 0x20) != 0);
    }

    public void setPitchLow(int port4) {
        this.pitch = (pitch & 0xf00) | (port4 & 0xff);
    }

    public Sound() {
        SoundIntEn = false;
        SoundEn = true;
        sampler = 0;
        sindx  = 0;
        buffer = new byte[SAMPLE_BUFF_SIZE];
    }

    public void Reset() {
        amp = 0;
        pitch = pitch & 0x0FF;
        SoundEn = false;
        SoundIntEn = false;
    }

    public void Open() {
        if (AudioPlay) {
            //Open up audio output, using 15625Hz sampling rate, 8 bit samples, mono, and big
            // endian byte ordering
            AudioFormat format = new AudioFormat(SAMPLING_RATE, (SAMPLE_SIZE * 8), 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)){
               System.out.println("Line matching " + info + " is not supported.");
            }
            try {
                line = (SourceDataLine)AudioSystem.getLine(info);
                line.open(format, AUDIO_BUFF_SIZE);
                line.start();
            } catch (Exception e) {
                System.out.println("Can not access audio line.");
                return;
            }

            // Make our buffer size match audio system's buffer
            int buffsize = line.getBufferSize();
            String log = String.format("Audio buffer size: %d", buffsize);
            System.out.println(log);
        }
    }

    public void Close() {
        if (AudioPlay) {
            line.stop();
            line.flush();
            line.close();
        }
    }

    public final boolean run(int cycles_320ns) {

        boolean soundint = false;

        for (int i = cycles_320ns; i > 0; i--) {
            // increment counter chain
            counter12bit++;
            if (counter12bit >= 4096) {
                counter12bit = pitch;
                if (pitch != 4095) {
                    counter4bit = (counter4bit +1) & 0x0f;
                }
            }
            // generate sound interrupt if enabled
            if (((counter4bit & 0x08) != 0) && SoundIntEn)
            {
                soundint = true;
            }
            if (AudioPlay) {
                // Generate wave for playback
                sampler++;
                if (sampler == NSAMPLE) {
                    sampler = 0;
                    byte sample = SoundEn ? (((counter4bit & 0x08) != 0)? (byte)amp : 0) : ((byte)amp);
                    buffer[sindx++] = sample;
                    if (sindx == SAMPLE_BUFF_SIZE) {
                        int offs = 0;
                        do {
                            int lineava = line.available();
                            if (lineava > 0) {
                                int len = (lineava > sindx) ? sindx: lineava;
                                sindx  -= len;
                                try {
                                    line.write(buffer, offs, len);
                                    offs += len;
                                } catch (Exception e) {
                                    System.out.println("Can not write audio line.");
                                }
                            } else {
                                try {
                                  Thread.sleep(1);
                                } catch (Exception e) {
                                }
                            }
                        } while (sindx != 0);
                    }
                }
            }
        }
        return soundint;
    }
}
