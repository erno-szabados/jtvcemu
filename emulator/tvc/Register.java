/**
 *
 */
package emulator.tvc;

/**
 * A simple register
 */
class Reg8 {

    int r;
    String name;

    public Reg8(String name) {
        this.name = name;
        this.r = 0;
    }
}

/**
 * A maskable register.
 */
class Reg8Masked extends Reg8 {

    private int mask;

    public Reg8Masked(String name, int mask) {
        super(name);
        this.mask = mask;
    }

    public void set(int r) {
        this.r = this.mask & r;
    }

    public int get() {
        return this.r;
    }
}

class Reg16 {

    Reg8 rh, rl;
    int r;
    String name;
    boolean pair;

    public Reg16(String name, Reg8 rh, Reg8 rl) {
        this.pair = true;
        this.name = name;
        this.rh = rh;
        this.rl = rl;
    }

    public Reg16(String name) {
        this.name = name;
        this.r = 0;
        this.pair = false;
    }

    public final int get() {
        return (pair) ? (rh.r << 8) | rl.r : r;
    }

    public final void set(int p) {
        if (pair) {
            rh.r = (p >> 8);
            rl.r = (p & 0xff);
        } else {
            r = p;
        }
    }

    public final void add(int n) {
        set((get() + n) & 0xffff);
    }

    public final void sub(int n) {
        set((get() - n) & 0xffff);
    }
}
