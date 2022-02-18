/**
 *
 */
package emulator.tvc;

import java.awt.event.*;

/**
 * Collects TVC Keyboard input.
 */
public class KeyboardListener implements KeyListener, FocusListener {

    Keyboard keyboard;
    GUI gui;

    public KeyboardListener(Keyboard keyboard, GUI gui) {
        this.keyboard = keyboard;
        this.gui = gui;
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
        int keyCode = e.getExtendedKeyCode();

        //Log.getInstance().write("keyCode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.VK_F7:
                if (pressed) gui.tvc.WarmReset = true;
                break;
            case KeyEvent.VK_F8:
                if (pressed) gui.tvc.ColdReset = true;
                break;
            case KeyEvent.VK_F11:
                if (pressed) gui.SwitchFullScreen();
                break;
            default:
                keyboard.setMatrix(keyCode, pressed);
                break;
        }
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        keyboard.clearMatrix();
    }
}
