package emulator.tvc;

import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * SD card emulation
 *
 */
public class SDcard {

    // SD states
    public static final int SD_STATE_NOCARD     = 0;
    public static final int SD_STATE_IMG_NONSTD = 1;
    public static final int SD_STATE_INSERTED   = 2;
    
    // MMC/SD command (in SPI)
    public static final int CMD0  = (0x40+0);    /* GO_IDLE_STATE */
    public static final int CMD1  = (0x40+1);    /* SEND_OP_COND */
    public static final int CMD8  = (0x40+8);    /* SEND_IF_COND */
    public static final int CMD9  = (0x40+9);    /* SEND_CSD */
    public static final int CMD10 = (0x40+10);   /* SEND_CID */
    public static final int CMD12 = (0x40+12);   /* STOP_TRANSMISSION */
    public static final int CMD16 = (0x40+16);   /* SET_BLOCKLEN */
    public static final int CMD17 = (0x40+17);   /* READ_SINGLE_BLOCK */
    public static final int CMD18 = (0x40+18);   /* READ_MULTIPLE_BLOCK */
    public static final int CMD24 = (0x40+24);   /* WRITE_BLOCK */
    public static final int CMD25 = (0x40+25);   /* WRITE_MULTIPLE_BLOCK */
    public static final int CMD55 = (0x40+55);   /* APP_CMD */
    public static final int CMD58 = (0x40+58);   /* READ_OCR */
    public static final int ACMD41= (0x40+41);   /* SEND_OP_COND (ACMD) */

    public static final int  MAX_BLOCKLEN    = 2048;
    public static final byte SD_RDY          = (byte)0x0FF;
    public static final byte START_RD_TOKEN  = (byte)0x0FE;
    public static final byte STOP_TRAN_TOKEN = (byte)0x0FD;
    public static final byte START_MWR_TOKEN = (byte)0x0FC;
    public static final byte RD_ERROR_TOKEN  = (byte)0x009;   // out of range error

    // R1 típusú response-ok
    public static final byte R1_NOERROR      = (byte)0x000;
    public static final byte R1_IDLE         = (byte)0x001;
    public static final byte R1_ERASRESET    = (byte)0x002;
    public static final byte R1_ILLEGALCMD   = (byte)0x004;
    public static final byte R1_CRCERROR     = (byte)0x008;
    public static final byte R1_ERASERROR    = (byte)0x010;
    public static final byte R1_ADDRERROR    = (byte)0x020;
    public static final byte R1_PARAMERROR   = (byte)0x040;

    // Data Response-ok
    public static final byte DATA_ACCEPTED   = (byte)0x005;
    public static final byte DATA_WRERROR    = (byte)0x00D;

    public static final byte[] CSD1v0 = {
        (byte)0x000,(byte)0x015,(byte)0x000,(byte)0x05A,(byte)0x05F,(byte)0x05A,(byte)0x080,(byte)0x000,
        (byte)0x03E,(byte)0x0F8,(byte)0x04F,(byte)0x0FF,(byte)0x092,(byte)0x080,(byte)0x040,(byte)0x0FF};

    public static final byte[] CSD2v0 = {
        (byte)0x040,(byte)0x00E,(byte)0x000,(byte)0x05A,(byte)0x05B,(byte)0x059,(byte)0x000,(byte)0x000,
        (byte)0x001,(byte)0x001,(byte)0x07F,(byte)0x080,(byte)0x00A,(byte)0x040,(byte)0x000,(byte)0x0FF};

    private byte[] SD_OCR = {(byte)0x080,(byte)0x0FF,(byte)0x0FF,(byte)0x000};

    private byte[] SD_CID = {
        0x003,0x053,0x044,'S','D','E','M','U',
        0x010,0x001,0x002,0x003,0x004,0x001,0x009,(byte)0x0FF};

    private byte[] SD_RespR7 = {0x000,0x000,0x001,0x000};

    public enum SDCARDSTATES {
        IDLE,
        INITIALIZED,
        APPCMD,
        RECEIVE_DATA,
        SEND_DATA,
        SEND_R1,
        SEND_R3,
        SEND_R7
    };

    // SD card internal HW
    private int  SD_DataPtr;
    private byte SD_Data[];
    private byte SD_DataBuff[];

    private byte SD_CSD[];

    private byte SD_RespR1;
    private byte SD_DataResponse;
    private int  SD_DataLen;
    private int  SD_BlockLen;
    private int  SD_DataAddr;      // adat helye az image fájlban, 512 bájtos blokkban megadva
    private int  SD_CardSize;      // kártya mérete, 512 bájtos blokkban megadva (max. 1TB)

    private boolean bSD_VERSION2, bSD_HC;
    private boolean bFirstData, bSD_MultiBlock;
    private boolean bNonStdFAT;

    private byte sdcmd, cmd_array[];
    private int  cmd_counter;
    private SDCARDSTATES SDcard_state, StateAfterACMD, StateAfterR1, StateAfterR7;

    private RandomAccessFile SDImgFile = null;

    public boolean bInserted;
    public boolean bWrProt;
    public boolean bSPI_CS;
    public boolean bSPI_CS_Prev;
    public byte SPIdatain;

    public SDcard() {
        SD_CSD = new byte[16];
        cmd_array   = new byte[6];
        SD_DataBuff = new byte[2052];
    }

    private void CalcCardParams(int cardsize)
    {
        int blocknr, i;
        int csize, mult;

        // ha nagyobb 2GB-nál, akkor SDHC-t emulál ver.2.0 CSD regiszterrel
        if (cardsize > 0x000400000) {
            bSD_VERSION2 = true;
            bSD_HC = true;
            SD_OCR[0] = (byte)0x0C0;
            for (i=0; i<16; i++) SD_CSD[i] = CSD2v0[i];

            // méret osztása 512kB-al (már csak 1024-el, mert 512 bájtos blokkokban van), majd -1
            csize = (cardsize / 0x00000400)-1;
            SD_CSD[8] = (byte)(csize >> 8);
            SD_CSD[9] = (byte)(csize & 0x0FF);
            SD_BlockLen = 512;
        }
        else {
            bSD_VERSION2 = false;
            bSD_HC = false;
            SD_OCR[0] = (byte)0x080;
            for (i=0; i<16; i++) SD_CSD[i] = CSD1v0[i];

            // ha az image fájl nagyobb 1GB-nál, akkor a blokkméret 1024 bájt
            SD_BlockLen = (cardsize > 0x000200000)?1024:512;

            // blokkszám
            blocknr = cardsize / (SD_BlockLen / 512);
            // device size
            mult = 2;
            csize = blocknr / (1 << mult);
            while ((csize > 4096) && (mult < 9))
            {
                mult++;
                csize = blocknr / (1 << mult);
            }
            csize = (csize - 1) & 0x0FFF;

            // CSD regiszter beállítása a számított értékekre

            // max. read block length (READ_BL_LEN)
            SD_CSD[5] = (cardsize > 0x000200000)? (byte)0x05A: (byte)0x059;
            // device size (C_SIZE)
            SD_CSD[6] = (byte)(((int)SD_CSD[6] & 0x0FC) | (csize >> 10));
            SD_CSD[7] = (byte)((csize >> 2) & 0x0FF);
            SD_CSD[8] = (byte)(((csize << 6) & 0x0C0) | ((int)SD_CSD[8] & 0x03F));
            // device size multiplier (C_SIZE_MULT)
            mult -=2;
            SD_CSD[9]  = (byte)(((int)SD_CSD[9] & 0x0FC) | ((mult >> 1) & 0x003));
            SD_CSD[10] = (byte)(((mult << 7) & 0x080) | ((int)SD_CSD[10] & 0x07F));
            // max. write block length (WRITE_BL_LEN)
            SD_CSD[13] = (cardsize > 0x000200000)? (byte)0x080: (byte)0x040;
        }
    }

    public void OpenDiskImage(String fname, int sdstate)
    {
        bInserted = false;  // nincs kártya behelyezve
        bWrProt   = true;   // írásvédett

        if (SDImgFile != null)
        {
            try {
            SDImgFile.close();
            } catch (IOException e) {};
        }

        if (sdstate != SD_STATE_NOCARD)
        {
            try {
                SDImgFile   = new RandomAccessFile(fname, "rw");
                SD_CardSize = (int)(SDImgFile.length() / 512);
                CalcCardParams(SD_CardSize);
                SDcard_state = SDCARDSTATES.IDLE;
                bNonStdFAT   = (boolean)(sdstate == SD_STATE_IMG_NONSTD);  
                // HW beállítások
                bInserted = true;   // kártya behelyezve (alacsony aktív jel)
                bWrProt   = false;  // nem írásvédett    (alacsony aktív jel)

            } catch (IOException e) {
                Log.getInstance().write("SD image can not open!");
            }
        }
    }

    private void RdDiskImage(int addr, int addrbyte, int length, byte[] buffer)
    {
        long addr_start;

        try {
            addr_start = (((long)addr) << 9) + addrbyte;
            SDImgFile.seek(addr_start);
            SDImgFile.read(buffer,0,length);
            if ((addr == 0) && (bNonStdFAT) &&
                (addrbyte < 511) && ((addrbyte + length) > 511))
            {
                // FAT signature bájtok behamisítása
                buffer[510 - addrbyte] = (byte)0x055;
                buffer[511 - addrbyte] = (byte)0x0AA;
            }
        } catch (IOException e) {
            Log.getInstance().write("SD image can not read!");
        }
    }

    private void WrDiskImage(int addr, int length, byte[] buffer)
    {
        long addr_start;

        try {
            addr_start = ((long)addr) << 9;
            SDImgFile.seek(addr_start);
            SDImgFile.write(buffer,0,length);
        } catch (IOException e) {
            Log.getInstance().write("SD image can not write!");
        }
    }

    private boolean ReceiveCMD(byte data)
    {
        if ((cmd_counter == 0) && ((data & 0x0C0) != 0x040)) return false;
        cmd_array[cmd_counter++] = data;
        if (cmd_counter == 6)
        {
            sdcmd = cmd_array[0];
            cmd_counter = 0;
            return true;
        }
        return false;
    }

    public void Send(byte host_data)
    {
        int SD_DataAddrByte;

        if (!bInserted) return; // ha nincs kártya behelyezve, visszatérés
        if (bSPI_CS)
        {
            if (!bSPI_CS_Prev)
            {
                // kártya kiválasztásakor parancs kezdetének szinkronizálása
                cmd_counter = 0;
                if (SDcard_state!= SDCARDSTATES.IDLE)
                {
                    SDcard_state = SDCARDSTATES.INITIALIZED;
                }
            }

            SPIdatain = SD_RDY;
            switch(SDcard_state)
            {
                case IDLE:
                    if (ReceiveCMD(host_data))
                    {
                        SD_RespR1    = R1_IDLE;
                        SDcard_state = SDCARDSTATES.SEND_R1;
                        switch(sdcmd)
                        {
                            case CMD1:  /* SEND_OP_COND */
                                StateAfterR1 = SDCARDSTATES.INITIALIZED;
                                break;
                            case CMD8:  /* SEND_IF_COND */
                                if (bSD_VERSION2 == true)
                                {
                                    SD_RespR7[3] = cmd_array[4];
                                    SD_DataLen   = 0;
                                    SDcard_state = SDCARDSTATES.SEND_R1;
                                    StateAfterR1 = SDCARDSTATES.SEND_R7;
                                    StateAfterR7 = SDCARDSTATES.IDLE;
                                }
                                else
                                {
                                    SDcard_state = SDCARDSTATES.IDLE;
                                }
                                break;
                            case CMD55: /* APP_CMD */
                                StateAfterR1   = SDCARDSTATES.APPCMD;
                                StateAfterACMD = SDCARDSTATES.IDLE;
                                break;
                            default:
                                StateAfterR1 = SDCARDSTATES.IDLE;
                        }
                    }
                    break;
                case INITIALIZED:
                    if (ReceiveCMD(host_data))
                    {
                        bSD_MultiBlock = false;
                        bFirstData   = true;
                        SD_RespR1    = R1_NOERROR;
                        SDcard_state = SDCARDSTATES.SEND_R1;
                        StateAfterR1 = SDCARDSTATES.INITIALIZED;
                        switch(sdcmd)
                        {
                            case CMD0:  /* GO_IDLE_STATE */
                                SD_RespR1 = R1_IDLE;
                                StateAfterR1 = SDCARDSTATES.IDLE;
                                break;
                            case CMD1:  /* SEND_OP_COND */
                                StateAfterR1 = SDCARDSTATES.INITIALIZED;
                                break;
                            case CMD9:  /* SEND_CSD */
                                SD_Data      = SD_CSD;
                                SD_DataPtr   = 0;
                                SD_DataLen   = 16;
                                StateAfterR1 = SDCARDSTATES.SEND_DATA;
                                break;
                            case CMD10: /* SEND_CID */
                                SD_Data      = SD_CID;
                                SD_DataPtr   = 0;
                                SD_DataLen   = 16;
                                StateAfterR1 = SDCARDSTATES.SEND_DATA;
                                break;
                            case CMD12: /* STOP_TRANSMISSION */
                                StateAfterR1 = SDCARDSTATES.INITIALIZED;
                                break;
                            case CMD16: /* SET_BLOCKLEN */
                                SD_BlockLen  = (((int)cmd_array[1] & 0x0FF) << 24) + (((int)cmd_array[2] & 0x0FF)<< 16) +
                                               (((int)cmd_array[3] & 0x0FF) << 8)  +  ((int)cmd_array[4] & 0x0FF);
                                StateAfterR1 = SDCARDSTATES.INITIALIZED;
                                break;
                            case CMD18: /* READ_MULTIPLE_BLOCK */
                                bSD_MultiBlock = true;
                            case CMD17: /* READ_SINGLE_BLOCK */
                                SD_Data     = SD_DataBuff;
                                SD_DataPtr  = 0;
                                SD_DataAddr = (((int)cmd_array[1] & 0x0FF) << 24) + (((int)cmd_array[2] & 0x0FF)<< 16) +
                                              (((int)cmd_array[3] & 0x0FF) << 8)  +  ((int)cmd_array[4] & 0x0FF);
                                if (bSD_HC == true)
                                {
                                    SD_DataLen = 512;
                                    SD_DataAddrByte = 0;
                                }
                                else
                                {
                                    SD_DataLen = (SD_BlockLen <= MAX_BLOCKLEN)? SD_BlockLen: MAX_BLOCKLEN;
                                    SD_DataAddrByte = SD_DataAddr % 512;
                                    SD_DataAddr /= 512;
                                }
                                if (((SD_DataAddr + (int)(SD_DataLen / 512)) > SD_CardSize) ||
                                   (((SD_DataAddr + (int)(SD_DataLen / 512)) == SD_CardSize) && (SD_DataAddrByte != 0)))
                                {
                                    SD_RespR1 = R1_PARAMERROR;
                                    StateAfterR1 = SDCARDSTATES.INITIALIZED;
                                }
                                else
                                {
                                    RdDiskImage(SD_DataAddr, SD_DataAddrByte, SD_DataLen, SD_DataBuff);
                                    StateAfterR1 = SDCARDSTATES.SEND_DATA;
                                }
                                break;
                            case CMD25: /* WRITE_MULTIPLE_BLOCK */
                                bSD_MultiBlock = true;
                            case CMD24: /* WRITE_BLOCK */
                                SD_DataAddr  = (((int)cmd_array[1] & 0x0FF) << 24) + (((int)cmd_array[2] & 0x0FF)<< 16) +
                                               (((int)cmd_array[3] & 0x0FF) << 8)  +  ((int)cmd_array[4] & 0x0FF);
                                if (bSD_HC == true)
                                {
                                    SD_DataLen = 512;
                                }
                                else
                                {
                                    SD_DataLen = (SD_BlockLen <= MAX_BLOCKLEN)? SD_BlockLen: MAX_BLOCKLEN;
                                    if ((SD_DataAddr % SD_BlockLen) != 0)
                                    {
                                        SD_RespR1 = R1_ADDRERROR;
                                        StateAfterR1 = SDCARDSTATES.INITIALIZED;
                                    }
                                    SD_DataAddr /= 512;
                                }
                                if (SD_RespR1 != R1_ADDRERROR)
                                {
                                    if ((SD_DataAddr + (int)(SD_DataLen / 512)) > SD_CardSize)
                                    {
                                        SD_RespR1 = R1_PARAMERROR;
                                        StateAfterR1 = SDCARDSTATES.INITIALIZED;
                                    }
                                    else
                                    {
                                        StateAfterR1 = SDCARDSTATES.RECEIVE_DATA;
                                    }
                                }
                                break;
                            case CMD55: /* APP_CMD */
                                StateAfterR1   = SDCARDSTATES.APPCMD;
                                StateAfterACMD = SDCARDSTATES.INITIALIZED;
                                break;
                            case CMD58: /* READ_OCR */
                                SD_DataLen   = 0;
                                StateAfterR1 = SDCARDSTATES.SEND_R3;
                                break;
                            default:
                                SD_RespR1 = R1_ILLEGALCMD;
                                break;
                        }
                    }
                    break;
                case APPCMD:
                    if (ReceiveCMD(host_data))
                    {
                        SDcard_state = SDCARDSTATES.SEND_R1;
                        if (sdcmd == ACMD41)
                        {
                            SD_RespR1    = R1_NOERROR;
                            StateAfterR1 = SDCARDSTATES.INITIALIZED;
                        }
                        else
                        {
                            SD_RespR1    = R1_ILLEGALCMD;
                            StateAfterR1 = StateAfterACMD;
                        }
                    }
                    break;
                case RECEIVE_DATA:
                    if (bFirstData)             // token fogadása
                    {
                        if (host_data == STOP_TRAN_TOKEN)
                        {
                            SDcard_state = SDCARDSTATES.INITIALIZED;
                        }
                        if ((host_data == START_RD_TOKEN) || (host_data == START_MWR_TOKEN))
                        {
                            SD_DataResponse =(((!bSD_HC) && (SD_BlockLen != 512)) || ((SD_DataAddr + 1)> SD_CardSize))? DATA_WRERROR: DATA_ACCEPTED;
                            SD_Data    = SD_DataBuff;
                            SD_DataPtr = 0;
                            SD_DataLen = 512;
                            bFirstData = false;
                        }
                    }
                    else if (SD_DataLen > 0)    // adatok fogadása
                    {
                        SD_Data[SD_DataPtr] = host_data;
                        SD_DataPtr++;
                        SD_DataLen--;
                    }
                    else if (SD_DataLen > -2)   // CRC fogadása
                    {
                        SD_DataLen--;
                    }
                    else                        // Data Response küldése
                    {
                        SPIdatain = SD_DataResponse;
                        if (SD_DataResponse != DATA_WRERROR)
                        {
                            WrDiskImage(SD_DataAddr, 512, SD_DataBuff);
                            SD_DataAddr += 1;
                        }
                        bFirstData = true;
                        SDcard_state = (bSD_MultiBlock)? SDCARDSTATES.RECEIVE_DATA: SDCARDSTATES.INITIALIZED;
                    }
                    break;
                case SEND_DATA:
                    if ((ReceiveCMD(host_data)) && (sdcmd == CMD12))    /* STOP_TRANSMISSION */
                    {
                        SD_RespR1    = R1_NOERROR;
                        SDcard_state = SDCARDSTATES.SEND_R1;
                        StateAfterR1 = SDCARDSTATES.INITIALIZED;
                    }
                    else
                    {
                        if (bFirstData)             // token küldése
                        {
                            SPIdatain  = START_RD_TOKEN;
                            bFirstData = false;
                        }
                        else if (SD_DataLen > 0)    // adatok küldése
                        {
                            SPIdatain = SD_Data[SD_DataPtr];
                            SD_DataPtr++;
                            SD_DataLen--;
                        }
                        else if (SD_DataLen > -2)   // CRC küldése
                        {
                            SPIdatain = 0x00;
                            SD_DataLen--;
                        }
                        else
                        {
                            if (!bSD_MultiBlock)
                            {
                                SDcard_state = SDCARDSTATES.INITIALIZED;
                            }
                            else
                            {
                                SD_DataLen  = (bSD_HC) ? 512 : (SD_BlockLen <= MAX_BLOCKLEN)? SD_BlockLen: MAX_BLOCKLEN;
                                SD_DataAddrByte = SD_DataLen % 512;
                                SD_DataAddr = SD_DataAddr + (SD_DataLen / 512);
                                if (((SD_DataAddr + (int)(SD_DataLen / 512)) > SD_CardSize) ||
                                   (((SD_DataAddr + (int)(SD_DataLen / 512)) == SD_CardSize) && (SD_DataAddrByte != 0)))
                                {
                                    SPIdatain = RD_ERROR_TOKEN;
                                    SDcard_state = SDCARDSTATES.INITIALIZED;
                                }
                                else
                                {
                                    RdDiskImage(SD_DataAddr, SD_DataAddrByte, SD_DataLen, SD_DataBuff);
                                    SD_Data    = SD_DataBuff;
                                    SD_DataPtr = 0;
                                    bFirstData = true;
                                }
                            }
                        }
                    }
                    break;
                case SEND_R1:
                    SPIdatain = SD_RespR1;
                    SDcard_state = StateAfterR1;
                    break;
                case SEND_R3:
                    SPIdatain = SD_OCR[SD_DataLen++];
                    if (SD_DataLen == 4)
                    {
                        SDcard_state = SDCARDSTATES.INITIALIZED;
                    }
                    break;
                case SEND_R7:
                    SPIdatain = SD_RespR7[SD_DataLen++];
                    if (SD_DataLen == 4)
                    {
                        SDcard_state = StateAfterR7;
                    }
                    break;
                default:
                    SDcard_state = SDCARDSTATES.IDLE;
                    break;
            }
        }
        else
        {
            SPIdatain = (byte)0xFF;
        }
        bSPI_CS_Prev = bSPI_CS;
    }

}
