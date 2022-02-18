package emulator.tvc;

/**
 * SD cartridge emulation
 *
 */
public class SDcartridge extends Memory.Page {

    // HW konstansok
    public static final byte PIN_CS0     = (byte)(1<<7);
    public static final byte PIN_CS1     = (byte)(1<<6);
    public static final byte CLR_DSKCHNG = (byte)(1<<5);

    public static final byte PIN_WPROT   = (byte)(1<<7);
    public static final byte PIN_INSRT   = (byte)(1<<6);
    public static final byte PIN_DSKCHNG = (byte)(1<<5);

    public static final int ROM_SIZE = 0x0A000;
    public static final int RAM_SIZE = 0x02000;


    // SD kártyák és foglalatok
    public SDcard SD_Card0, SD_Card1;

    // illesztõkártya HW
    private int  ROMpage, RAMpage;
    private byte SPIdatain, IFstatus;
    private byte SD_ROM[], SD_RAM[];
    private boolean bHiSpeed;


    //
    // --- SD kartya image-ek betoltese ---
    //

    public void OpenDiskImage(String fname, int sdstate, int con)
    {
        SDcard sd_card = (con == 0)? SD_Card0 : SD_Card1;

        sd_card.OpenDiskImage(fname, sdstate);
        IFstatus = PIN_DSKCHNG | PIN_WPROT | PIN_INSRT;     // nincs kártya behelyezve és írásvédett jelzés;
        if ((SD_Card0.bInserted) || (SD_Card1.bInserted))
        {
            IFstatus &= ~PIN_INSRT;     // kártya behelyezve (alacsony aktív jel)
        }
        if (!SD_Card0.bWrProt)
        {
            IFstatus &= ~PIN_WPROT;     // nem írásvédett    (alacsony aktív jel)
        }
    }


    //
    // --- Cartridge emulálása ---
    //

    private byte SD_IOread(int addr)
    {
        switch(addr & 0x03)
        {
            case 0x00:      // SPI data in
                return ((SD_Card0.bSPI_CS)? SD_Card0.SPIdatain: SD_Card1.SPIdatain);
            case 0x01:      // Státusz regiszter
                return IFstatus;
        }
        return 0;
    }

    private void SD_IOwrite(int addr, byte value)
    {
        switch(addr & 0x03)
        {
            case 0x00:      // SPI data out
                SD_Card0.Send(value);
                SD_Card1.Send(value);
                break;
            case 0x01:      // Control regiszter
                SD_Card0.bSPI_CS_Prev = SD_Card0.bSPI_CS;
                SD_Card1.bSPI_CS_Prev = SD_Card1.bSPI_CS;
                SD_Card0.bSPI_CS = (boolean)((value & PIN_CS0) != 0);
                SD_Card1.bSPI_CS = (boolean)((value & PIN_CS1) != 0);
                if ((value & CLR_DSKCHNG) != 0)
                {
                    IFstatus &= ~(PIN_DSKCHNG);
                }
                break;
            case 0x02:      // ROM page
                ROMpage = value & 0x0F0;
                break;
            case 0x03:      // RAM page
                RAMpage = value & 0x018;
                break;
        }
    }

    public int get(int addr)
    {
        byte data;

        if ((addr & 0x02000) == 0)  // ROM kiválasztása (alsó 8kB)
        {
            data = SD_ROM[((ROMpage & 0x0E0) << 8) | (addr & 0x01FFF)];
        }
        else                        // RAM és I/O kiválasztása (felsõ 8kB)
        {
            if ((addr & 0x03C00) != 0x03C00)
            {                       // RAM kiválasztása (max. 7kB)
                data = SD_RAM[(addr & 0x01FFF)];
            }
            else                    // I/O kiválasztása
            {
                if (bHiSpeed)
                {
                    if (SD_Card0.bSPI_CS)
                    {
                        data = SD_Card0.SPIdatain;
                        SD_Card0.Send((byte)0x0FF);
                    }
                    else
                    {
                        data = SD_Card1.SPIdatain;
                        SD_Card1.Send((byte)0x0FF);
                    }
                }
                else
                {
                    data = SD_IOread(addr);
                }
            }
        }
        return ((int)data & 0x0FF);
    }

    public void set(int addr, int p)
    {
        if ((addr & 0x02000) == 0)  // ROM kiválasztása (alsó 8kB)
        {
            return;
        }
        else                        // RAM és I/O kiválasztása (felsõ 8kB)
        {
            if ((addr & 0x03C00) != 0x03C00)
            {                       // RAM kiválasztása (max. 7kB)
                SD_RAM[(addr & 0x01FFF)] = (byte)p;
            }
            else                    // I/O kiválasztása
            {
                if ((addr & 0x0003) == 0x0003)
                {
                    bHiSpeed = (boolean)((p & 0x080) == 0x080);
                }
                else
                {
                    SD_IOwrite(addr, (byte)p);
                }
            }
        }
    }

    public void setrom(int addr, int p)
    {
        SD_ROM[addr] = (byte)p;
    }

    public SDcartridge(Memory mem, String name)
    {
        mem.super(name, false);

        SD_ROM = new byte[ROM_SIZE];
        SD_RAM = new byte[RAM_SIZE];
        IFstatus = PIN_WPROT | PIN_INSRT;       // nincs kártya behelyezve és írásvédett jelzés
        bHiSpeed = false;

        SD_Card0 = new SDcard();
        SD_Card0.bSPI_CS_Prev = false;
        SD_Card0.bSPI_CS      = false;

        SD_Card1 = new SDcard();
        SD_Card1.bSPI_CS_Prev = false;
        SD_Card1.bSPI_CS      = false;
    }
}
