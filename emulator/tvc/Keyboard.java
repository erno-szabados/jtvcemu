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
    private boolean AltPressed, AltGrPressed, CtrlPressed;
    private int matrix[];
    // For Hungarian keyboard
    private static int keyMatrixHU[] = {
        // Row 0 -> 4, 1, Í, 6, 0, 2, 3, 5
        KeyEvent.VK_4, KeyEvent.VK_1, 0x10000ED, KeyEvent.VK_6,
        KeyEvent.VK_0, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_5,

        // Row 1 -> 7, Ö, Ó, *, Ü, 9, 8, ^
        KeyEvent.VK_7, 0x10000D6, 0x10000F3, 0x6A,
        0x10000FC, KeyEvent.VK_9, KeyEvent.VK_8, KeyEvent.VK_CIRCUMFLEX,

        // Row 2 -> R, Q, @, Z, ;, W, E, T
        KeyEvent.VK_R, KeyEvent.VK_Q, KeyEvent.VK_AT, KeyEvent.VK_Z,
        KeyEvent.VK_SEMICOLON, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_T,

        // Row 3 -> U, P, Ú, [, Õ, O, I, ]
        KeyEvent.VK_U, KeyEvent.VK_P, 0x10000FA, KeyEvent.VK_OPEN_BRACKET,
        0x1000151, KeyEvent.VK_O, KeyEvent.VK_I, KeyEvent.VK_CLOSE_BRACKET,

        // Row 4 -> F, A, <, H, \, S, D, G
        KeyEvent.VK_F, KeyEvent.VK_A, 0x6F, KeyEvent.VK_H,
        KeyEvent.VK_BACK_SLASH, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_G,

        // Row 5 -> J, É, Û, Ret, Á, L, K, Del
        KeyEvent.VK_J, 0x10000E9, 0x1000171, KeyEvent.VK_ENTER,
        0x10000E1, KeyEvent.VK_L, KeyEvent.VK_K, KeyEvent.VK_BACK_SPACE,

        // Row 6 -> V, Y, Lock, N, Shift, X, C, B
        KeyEvent.VK_V, KeyEvent.VK_Y, KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_N,
        KeyEvent.VK_SHIFT, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_B,

        // Row 7 -> M, -, Space, Ctrl, Esc, ., ,, Alt
        KeyEvent.VK_M, KeyEvent.VK_MINUS, KeyEvent.VK_SPACE, KeyEvent.VK_CONTROL,
        KeyEvent.VK_ESCAPE, KeyEvent.VK_PERIOD, KeyEvent.VK_COMMA, KeyEvent.VK_ALT,

        // Row 8 /right joy/ ->  empty, left, right, acc, fire, down, up, Ins
        KeyEvent.VK_UNDEFINED, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_PAGE_UP,
        KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_INSERT,

        // Row 9 /left  joy/ ->  empty, left, right, acc, fire, down, up
        KeyEvent.VK_UNDEFINED, KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD6, KeyEvent.VK_SUBTRACT,
        KeyEvent.VK_ADD, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD8};

    private static int[][] AltGrLUT = {
        
        // keycode, matrix pos, +Shift
        {KeyEvent.VK_COMMA, 20, 0},      // ;
        {0x10000ED,         34, 0},      // <
        {KeyEvent.VK_Y,     34, 1},      // >
        {KeyEvent.VK_X,     11, 1},      // #
        {KeyEvent.VK_C,      4, 1},      // &
        {KeyEvent.VK_V,     18, 0},      // @
        {KeyEvent.VK_B,     27, 1},      // {
        {KeyEvent.VK_N,     31, 1},      // }
        {KeyEvent.VK_MINUS, 11, 0},      // *
        {KeyEvent.VK_1,     15, 1},      // ~
        {KeyEvent.VK_3,     15, 0},      // ^
        {KeyEvent.VK_Q,     36, 0},      // '\'
        {KeyEvent.VK_W,     36, 1},      // |
        {KeyEvent.VK_F,     27, 0},      // [
        {KeyEvent.VK_G,     31, 0},      // ]
        {0x10000E9,         20, 1}       // $
    };

    private static int[] ShiftNumLUT = {

         1,   // '
         5,   // " (?)
         6,   // +
         0,   // !
         3,   // /
         8,   // =
         14,  // (
         13,  // )
    };

    public Keyboard() {
        matrix = new int[MATRIX_ROWS];
        clearMatrix();
    }

    public void clearMatrix() {
        for (int i = 0; i < MATRIX_ROWS; i++) {
            matrix[i] = 0xff;
        }
        AltPressed   = false;
        CtrlPressed  = false;
        AltGrPressed = false;
    }

    public int getMatrixAt(int i) {
        return ((i < MATRIX_ROWS)? matrix[i] : 0xff);
    }

    /**
     * Put a key into the keyboard matrix array when a key is pressed on the PC
     * keyboard.
     *
     * @param kbd pressed key
     * @param pressed true if pressed, false if released.
     */
    public void setMatrix(int keyCode, boolean pressed) {
        int row, column, i = -1;

        switch(keyCode) {
            case KeyEvent.VK_ALT:
                if ((AltGrPressed && !pressed)) clearMatrix();
                AltPressed = pressed;
                AltGrPressed = AltPressed && CtrlPressed; // on Windows AltGr = ALT + CTRL
                i = 63;
                break;
            case KeyEvent.VK_CONTROL:
                if ((AltGrPressed && !pressed)) clearMatrix();
                CtrlPressed = pressed;
                AltGrPressed = AltPressed && CtrlPressed; // on Windows AltGr = ALT + CTRL
                i = 59;
                break;
            case KeyEvent.VK_ALT_GRAPH:
                if ((AltGrPressed && !pressed)) clearMatrix();
                AltGrPressed = pressed;
                i = 63;
                break;
            case KeyEvent.VK_END:    // END -> ESC
                i = 60;
                break;
            case KeyEvent.VK_DELETE: // Delete -> DEL
                i = 47;
                break;
            case KeyEvent.VK_RIGHT_PARENTHESIS: // )
                i++;
            case KeyEvent.VK_LEFT_PARENTHESIS:  // (
                i++;
            case KeyEvent.VK_EQUALS:            // =
                i++;
            case KeyEvent.VK_SLASH:             // /
                i++;
            case KeyEvent.VK_EXCLAMATION_MARK:  // !
                i++;
            case KeyEvent.VK_PLUS:              // +
                i++;
            case KeyEvent.VK_QUOTEDBL:          // " (?)
                i++;
            case KeyEvent.VK_QUOTE:             // '
                i++;
                // change SHIFT state
                matrix[6]= pressed? (matrix[6] & (~0x008)) : (matrix[6] | 0x008);
                // get numeric key position
                i = ShiftNumLUT[i];
                break;
            default:
                if (AltGrPressed) {
                    matrix[7] |= 0x11; // clear CTRL and ALT from the matrix
                    for (i = (AltGrLUT.length -1); i > (-1) ; i--) {
                        if (keyCode == AltGrLUT[i][0]) {
                            if (AltGrLUT[i][2] > 0) {
                                // change also SHIFT state if required
                                matrix[6]= pressed? (matrix[6] & (~0x008)) : (matrix[6] | 0x008);
                            }
                            i = AltGrLUT[i][1];
                            break;
                        }
                    }             
                } else {
                    for (i = (keyMatrixHU.length -1); i > (-1) ; i--) {
                        if (keyCode == keyMatrixHU[i]) break;
                    }
                }
                break;
        }
        if (i > (-1)) {
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
