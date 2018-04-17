package emulator.tvc;

/**
 * $Id: $
 */

/**
 *
 * TODO argument processing.
 */
public class Main {
    public static void main(String[] argv) {
        Log log = Log.getInstance();
        log.open();
        GUI surface = new GUI();
        log.close();
    }
}
