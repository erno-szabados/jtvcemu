package emulator.tvc;

import java.awt.event.KeyEvent;

/*
 * Created on Sep 1, 2003
 *
 */
/**
 * @author gah
 *
 */
public class Keyboard {

    public static final int MATRIX_ROWS = 10;
    public static final int MATRIX_COLS = 8;
    private int matrix[];
    // For Hungarian keyboard
    private int keyMatrixHU[] = {
        // Row 0
        KeyEvent.VK_4, KeyEvent.VK_1, 0x10000ED, KeyEvent.VK_6,
        KeyEvent.VK_0, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_5,
        // Row 1
        KeyEvent.VK_7, 0x10000D6, 0x10000F3, 0x6A,
        0x10000FC, KeyEvent.VK_9, KeyEvent.VK_8, KeyEvent.VK_CIRCUMFLEX,
        // Row 2
        KeyEvent.VK_R, KeyEvent.VK_Q, KeyEvent.VK_AT, KeyEvent.VK_Z,
        KeyEvent.VK_SEMICOLON, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_T,
        // Row 3
        KeyEvent.VK_U, KeyEvent.VK_P, 0x10000FA, KeyEvent.VK_OPEN_BRACKET,
        0x1000151, KeyEvent.VK_O, KeyEvent.VK_I, KeyEvent.VK_CLOSE_BRACKET,
        // Row 4 0x6f = less than /
        KeyEvent.VK_F, KeyEvent.VK_A, 0x6F, KeyEvent.VK_H,
        KeyEvent.VK_BACK_SLASH, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_G,
        // Row 5
        KeyEvent.VK_J, 0x10000E9, 0x1000171, KeyEvent.VK_ENTER,
        0x10000E1, KeyEvent.VK_L, KeyEvent.VK_K, KeyEvent.VK_BACK_SPACE,
        // Row 6
        KeyEvent.VK_V, KeyEvent.VK_Y, KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_N,
        KeyEvent.VK_SHIFT, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_B,
        // Row 7
        KeyEvent.VK_M, KeyEvent.VK_MINUS, KeyEvent.VK_SPACE, KeyEvent.VK_CONTROL,
        KeyEvent.VK_ESCAPE, KeyEvent.VK_PERIOD, KeyEvent.VK_COMMA, KeyEvent.VK_ALT,
        // Row 8
        KeyEvent.VK_UNDEFINED, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_PAGE_UP,
        KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_INSERT,
        // Row 9
        KeyEvent.VK_UNDEFINED, KeyEvent.VK_KP_LEFT, KeyEvent.VK_KP_RIGHT, KeyEvent.VK_END,
        KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_KP_UP, KeyEvent.VK_KP_DOWN,};

    public Keyboard() {
        matrix = new int[MATRIX_ROWS];
        for (int i = 0; i < MATRIX_ROWS; i++) {
            matrix[i] = 0xff;
        }
    }

    public int getMatrixAt(int i) {
        return matrix[i];
    }

    /**
     * Put a key into the keyboard matrix array when a key is pressed on the PC
     * keyboard.
     *
     * @param kbd pressed key
     * @param pressed true if pressed, false if released.
     */
    public void setMatrix(int keyCode, boolean pressed) {
        int row, column, i = 0;

        for (i = 0; i < keyMatrixHU.length; i++) {
            if (keyCode == keyMatrixHU[i]) {
                // /8
                row = i >> 3;
                // mod 8
                column = 7 - (i & 7);
                if (pressed) {
                    matrix[row] &= ~(1 << column);
                } else {
                    // released
                    matrix[row] |= (1 << column);
                }
            }
        }
    }
}
