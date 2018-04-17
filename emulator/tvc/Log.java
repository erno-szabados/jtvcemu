package emulator.tvc;

/*
 * Created on Jul 14, 2003
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Date;

public class Log {

    public static final String TVC_LOGFILE = "tvc.log";
    public static FileOutputStream logFile;
    public static PrintStream pStream;
    public static MessageFormat formatter = new MessageFormat("{0,time,HH:MM:ss:SSS} {1}");

    private static Log instance;

    public static Log getInstance() {
        if (null == instance) {
            instance = new Log();
        }
        return instance;
    }

    private Log() {
    }

    public void open() {
        try {
            logFile = new FileOutputStream(TVC_LOGFILE);
            pStream = new PrintStream(logFile);

        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    public void write(String s) {

        Object[] args = {
            new Date(System.currentTimeMillis()),
            s
        };

        pStream.println(formatter.format(args));
        System.out.println(formatter.format(args));
    }

    public void close() {
        try {
            pStream.close();
            logFile.close();
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }
}
