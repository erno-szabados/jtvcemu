/**
 *
 */
package emulator.tvc;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Collects TVC Keyboard input.
 */
public class KeyboardListener implements KeyListener {

    TVC tvc;
    Log log;
    Keyboard keyboard;

    public KeyboardListener(Keyboard keyboard) {
        log = Log.getInstance();
        this.keyboard = keyboard;
    }

    public void keyTyped(KeyEvent e) {
        return;
    }

    public void keyPressed(KeyEvent e) {

        keyHandling(e, true);
    }

    public void keyReleased(KeyEvent e) {
        keyHandling(e, false);
    }

    public void keyHandling(KeyEvent e, boolean pressed) {
        keyboard.setMatrix(e.getExtendedKeyCode(), pressed);

        //keyboard.setMatrix(e.getKeyCode(), pressed);
//        Log.getInstance().write("Pressed:" + e.getKeyCode() + ":"
//                + e.getKeyChar() + ":" + e.getKeyLocation() + ":"
//                + e.getKeyText(e.getKeyCode()) + "p:" + pressed);
        return;
    }
}
