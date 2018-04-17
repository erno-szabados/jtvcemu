package emulator.tvc;

/**
 * @author Gabor
 *
 */
public class Z80 implements Runnable {

    public Reg8 A, B, C, D, E, F, H, L, IM, I;
    public Reg16 AF, BC, DE, HL, IX, IY;
    public Reg16 PC, SP;
    public int AF1, BC1, DE1, HL1;
    public boolean IFF1, IFF2;
    private Reg8 r8[];
    private Reg16 r16[], r16AF[], r16SP[], r16IND[], IND;
    int pPC[], ppi = 0;
    public long instCount, t, lastWaitT;
    public boolean running;
    public int opcode;
    public boolean interrupt, halt = false;
    // Instruction Method tables
    static String flagNames[] = {"NZ", "Z", "NC", "C", "PO", "PE", "P", "N"};
    static int IM0 = 0, IM1 = 1, IM2 = 2;
    static int SZTable[] = {0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80};
    static int PSZTable[] = {0x44, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x00, 0x04, 0x04, 0x00,
        0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x04, 0x00, 0x00, 0x04,
        0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x00, 0x04, 0x04, 0x00,
        0x04, 0x00, 0x00, 0x04, 0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00,
        0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x00, 0x04, 0x04, 0x00,
        0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x04, 0x00, 0x00, 0x04,
        0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x04, 0x00, 0x00, 0x04,
        0x00, 0x04, 0x04, 0x00, 0x00, 0x04, 0x04, 0x00, 0x04, 0x00, 0x00, 0x04,
        0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x00, 0x04, 0x04, 0x00,
        0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x04, 0x00, 0x00, 0x04,
        0x04, 0x00, 0x00, 0x04, 0x00, 0x04, 0x04, 0x00, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84, 0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84, 0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80,
        0x80, 0x84, 0x84, 0x80, 0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84, 0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80, 0x84, 0x80, 0x80, 0x84,
        0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84, 0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84, 0x80, 0x84, 0x84, 0x80, 0x80, 0x84, 0x84, 0x80,
        0x84, 0x80, 0x80, 0x84};
    static int SET_S = 0x80, SET_Z = 0x40, SET_5 = 0x20, SET_H = 0x10, SET_3 = 0x08, SET_PV = 0x04, SET_N = 0x02, SET_C = 0x01;

    /*
     * Trace levels: 1 - General purpose
     * 				 2 - CALL/RET/RST
     * 				 3 - JP/JR
     * 				 4 - JP/JR cc
     * 				>5 - all
     */
    public int traceCPU;
    static int TRC_SUB = 1, TRC_GP = 2, TRC_CRR = 3, TRC_J = 4, TRC_Jcc = 5, TRC_ALL = 6;
    Memory memory;
    Port port;
    Log log;

    public Z80(Memory memory, Port port) {
        A = new Reg8("A");
        B = new Reg8("B");
        C = new Reg8("C");
        D = new Reg8("D");
        E = new Reg8("E");
        F = new Reg8("F");
        H = new Reg8("H");
        L = new Reg8("L");
        AF = new Reg16("AF", A, F);
        BC = new Reg16("BC", B, C);
        DE = new Reg16("DE", D, E);
        HL = new Reg16("HL", H, L);
        PC = new Reg16("PC");
        SP = new Reg16("SP");
        IX = new Reg16("IX");
        IY = new Reg16("IY");
        IM = new Reg8("IM");
        I = new Reg8("I");
        r8 = new Reg8[8];
        r8[0] = B;
        r8[1] = C;
        r8[2] = D;
        r8[3] = E;
        r8[4] = H;
        r8[5] = L;
        r8[6] = null;
        r8[7] = A;
        r16 = new Reg16[4];
        r16[0] = BC;
        r16[1] = DE;
        r16[2] = HL;
        r16[3] = SP;
        r16AF = new Reg16[4];
        r16AF[0] = BC;
        r16AF[1] = DE;
        r16AF[2] = HL;
        r16AF[3] = AF;
        r16IND = new Reg16[4];
        r16IND[0] = BC;
        r16IND[1] = DE;
        r16IND[3] = SP;
        pPC = new int[1024];

        log = Log.getInstance();
        this.memory = memory;
        this.port = port;

        initZ80();
    }

    public void run() {
        long m1, lastWaitMills;
        double d1, d2;
        InterruptTimer ITimer;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        m1 = System.currentTimeMillis();
        lastWaitT = 0;
        lastWaitMills = System.currentTimeMillis();
        running = true;
        interrupt = false;
        ITimer = new InterruptTimer(this);
        ITimer.setDeltaT(20000);
        new Thread(ITimer).start();
        while (running) {
            if (traceCPU >= TRC_SUB) {
                traceSubroutine();
            }
            switch (PC.r) {
                case 0xf99c:
                case 0xc9f4a:
                    prevPCs();
                    traceCPU = 10;
                    IFF1 = false;
                    memory.dump(0x1831, 8);
                    memory.dump(0x1732, 8);
                    break;
                case 0xdc9fa:
                    traceCPU = 0;
                    memory.dump(0x1732, 8);
                    memory.dump(0x1831, 8);
                    break;
                case 0xdc88a:
                    log.write("Haloooo");
                    break;
            }
            pPC[ppi] = PC.r;
            ppi = (ppi + 1) & 0x3ff;
            if (interrupt) {
                interrupt();
            }
            opcode = memory.getByte(PC.r);
            switch (opcode) {
                // LD r8,r8
                case 0x40:
                case 0x41:
                case 0x42:
                case 0x43:
                case 0x44:
                case 0x45:
                case 0x47:
                case 0x48:
                case 0x49:
                case 0x4a:
                case 0x4b:
                case 0x4c:
                case 0x4d:
                case 0x4f:
                case 0x50:
                case 0x51:
                case 0x52:
                case 0x53:
                case 0x54:
                case 0x55:
                case 0x57:
                case 0x58:
                case 0x59:
                case 0x5a:
                case 0x5b:
                case 0x5c:
                case 0x5d:
                case 0x5f:
                case 0x60:
                case 0x61:
                case 0x62:
                case 0x63:
                case 0x64:
                case 0x65:
                case 0x67:
                case 0x68:
                case 0x69:
                case 0x6a:
                case 0x6b:
                case 0x6c:
                case 0x6d:
                case 0x6f:
                case 0x78:
                case 0x79:
                case 0x7a:
                case 0x7b:
                case 0x7c:
                case 0x7d:
                case 0x7f:
                    instLD_r8_r8();
                    break;
                // LD r8,n8
                case 0x06:
                case 0x0e:
                case 0x16:
                case 0x1e:
                case 0x26:
                case 0x2e:
                case 0x3e:
                    instLD_r8_n8();
                    break;
                // LD r8,(HL)
                case 0x46:
                case 0x4e:
                case 0x56:
                case 0x5e:
                case 0x66:
                case 0x6e:
                case 0x7e:
                    instLD_r8_iHL();
                    break;
                case 0x70:
                case 0x71:
                case 0x72:
                case 0x73:
                case 0x74:
                case 0x75:
                case 0x77:
                    instLD_iHL_r8();
                    break;
                // LD r16,n16
                case 0x01:
                case 0x11:
                case 0x21:
                case 0x31:
                    instLD_r16_n16();
                    break;
                case 0x36:
                    instLD_iHL_n8();
                    break;
                case 0x3a:
                    instLD_A_in16();
                    break;
                case 0x32:
                    instLD_in16_A();
                    break;
                case 0x0a:
                    instLD_A_iBC();
                    break;
                case 0x1a:
                    instLD_A_iDE();
                    break;
                case 0x22:
                    instLD_in16_HL();
                    break;
                case 0x2a:
                    instLD_HL_in16();
                    break;
                case 0x12:
                    instLD_iDE_A();
                    break;
                case 0x02:
                    instLD_iBC_A();
                    break;
                case 0xf9:
                    instLD_SP_HL();
                    break;
                // IN/OUT
                case 0xd3:
                    instOUT_n_A();
                    break;
                case 0xdb:
                    instIN_A_n();
                    break;
                case 0x00:
                    instNOP();
                    break;
                case 0x08:
                    instEX_AF_AF();
                    break;
                // Rotate
                case 0x0f:
                    instRRCA();
                    break;
                case 0x07:
                    instRLCA();
                    break;
                case 0x17:
                    instRLA();
                    break;
                case 0x1f:
                    instRRA();
                    break;
                // JR (FLAG),n
                case 0x20:
                    instJR_NZ_n8();
                    break;
                case 0x28:
                    instJR_Z_n8();
                    break;
                case 0x30:
                    instJR_NC_n8();
                    break;
                case 0x38:
                    instJR_C_n8();
                    break;
                case 0x10:
                    instDJNZ();
                    break;
                // CP
                case 0xb8:
                case 0xb9:
                case 0xba:
                case 0xbb:
                case 0xbc:
                case 0xbd:
                case 0xbf:
                    instCP_r8();
                    break;
                case 0xfe:
                    instCP_n8();
                    break;
                case 0xbe:
                    instCP_iHL();
                    break;
                // JP
                case 0x18:
                    instJR_n8();
                    break;
                case 0xc3:
                    instJP();
                    break;
                case 0xe9:
                    instJP_HL();
                    break;
                case 0xc2:
                case 0xca:
                case 0xd2:
                case 0xda:
                case 0xe2:
                case 0xea:
                case 0xf2:
                case 0xfa:
                    instJP_cc_n16();
                    break;
                // CALL,RETx
                case 0xc4:
                case 0xcc:
                case 0xd4:
                case 0xdc:
                case 0xe4:
                case 0xec:
                case 0xf4:
                case 0xfc:
                    instCALL_cc_n16();
                    break;
                case 0xcd:
                    instCALL_n16();
                    break;
                case 0xc9:
                    instRET();
                    break;
                case 0xc0:
                case 0xc8:
                case 0xd0:
                case 0xd8:
                case 0xe0:
                case 0xe8:
                case 0xf0:
                case 0xf8:
                    instRET_cc();
                    break;
                case 0xc7:
                case 0xcf:
                case 0xd7:
                case 0xdf:
                case 0xe7:
                case 0xef:
                case 0xf7:
                case 0xff:
                    instRST_p();
                    break;
                // EX
                case 0xd9:
                    instEXX();
                    break;
                case 0xe3:
                    instEX_iSP_HL();
                    break;
                case 0xeb:
                    instEX_DE_HL();
                    break;
                // General-purpose, Arithmethic and CPU Control group
                case 0xf3:
                    instDI();
                    break;
                case 0xfb:
                    instEI();
                    break;
                case 0x76:
                    instHALT();
                    break;
                case 0x03:
                case 0x13:
                case 0x23:
                case 0x33:
                    instINC_r16();
                    break;
                case 0x0b:
                case 0x1b:
                case 0x2b:
                case 0x3b:
                    instDEC_r16();
                    break;
                // ADD
                case 0x80:
                case 0x81:
                case 0x82:
                case 0x83:
                case 0x84:
                case 0x85:
                case 0x87:
                    instADD_r8();
                    break;
                case 0x86:
                    instADD_iHL();
                    break;
                case 0xc6:
                    instADD_n8();
                    break;
                case 0x88:
                case 0x89:
                case 0x8a:
                case 0x8b:
                case 0x8c:
                case 0x8d:
                case 0x8f:
                    instADC_r8();
                    break;
                case 0xce:
                    instADC_n8();
                    break;
                case 0x8e:
                    instADC_iHL();
                    break;
                case 0x09:
                case 0x19:
                case 0x29:
                case 0x39:
                    instADD_HL_r16();
                    break;
                case 0x27:
                    instDAA();
                    break;
                // SUB
                case 0x90:
                case 0x91:
                case 0x92:
                case 0x93:
                case 0x94:
                case 0x95:
                case 0x97:
                    instSUB_r8();
                    break;
                case 0xd6:
                    instSUB_n8();
                    break;
                case 0x96:
                    instSUB_iHL();
                    break;
                case 0x98:
                case 0x99:
                case 0x9a:
                case 0x9b:
                case 0x9c:
                case 0x9d:
                case 0x9f:
                    instSBC_r8();
                    break;
                case 0x9e:
                    instSBC_iHL();
                    break;
                // OR
                case 0xb0:
                case 0xb1:
                case 0xb2:
                case 0xb3:
                case 0xb4:
                case 0xb5:
                case 0xb7:
                    instOR_r8();
                    break;
                case 0xf6:
                    instOR_n8();
                    break;
                case 0xb6:
                    instOR_iHL();
                    break;
                // AND
                case 0xa0:
                case 0xa1:
                case 0xa2:
                case 0xa3:
                case 0xa4:
                case 0xa5:
                case 0xa7:
                    instAND_r8();
                    break;
                case 0xe6:
                    instAND_n8();
                    break;
                case 0xa6:
                    instAND_iHL();
                    break;
                // XOR
                case 0xa8:
                case 0xa9:
                case 0xaa:
                case 0xab:
                case 0xac:
                case 0xad:
                case 0xaf:
                    instXOR_r8();
                    break;
                case 0xee:
                    instXOR_n8();
                    break;
                case 0xae:
                    instXOR_iHL();
                    break;
                // INC
                case 0x04:
                case 0x0c:
                case 0x14:
                case 0x1c:
                case 0x24:
                case 0x2c:
                case 0x3c:
                    instINC_r8();
                    break;
                case 0x34:
                    instINC_iHL();
                    break;
                // DEC
                case 0x05:
                case 0x0d:
                case 0x15:
                case 0x1d:
                case 0x25:
                case 0x2d:
                case 0x3d:
                    instDEC_r8();
                    break;
                case 0x35:
                    instDEC_iHL();
                    break;
                // POP/PUSH
                case 0xc1:
                case 0xd1:
                case 0xe1:
                case 0xf1:
                    instPOP_r16();
                    break;
                case 0xc5:
                case 0xd5:
                case 0xe5:
                case 0xf5:
                    instPUSH_r16();
                    break;
                // CPL/SCF/CCF
                case 0x2f:
                    instCPL();
                    break;
                case 0x37:
                    instSCF();
                    break;
                case 0x3f:
                    instCCF();
                    break;
                case 0xcb: {
                    opcode = memory.getByte(PC.r + 1);
                    switch (opcode) {
                        case 0x00:
                        case 0x01:
                        case 0x02:
                        case 0x03:
                        case 0x04:
                        case 0x05:
                        case 0x07:
                            instRLC_r8();
                            break;
                        case 0x08:
                        case 0x09:
                        case 0x0a:
                        case 0x0b:
                        case 0x0c:
                        case 0x0d:
                        case 0x0f:
                            instRRC_r8();
                            break;
                        case 0x18:
                        case 0x19:
                        case 0x1a:
                        case 0x1b:
                        case 0x1c:
                        case 0x1d:
                        case 0x1f:
                            instRR_r8();
                            break;
                        case 0x10:
                        case 0x11:
                        case 0x12:
                        case 0x13:
                        case 0x14:
                        case 0x15:
                        case 0x17:
                            instRL_r8();
                            break;
                        case 0x28:
                        case 0x29:
                        case 0x2a:
                        case 0x2b:
                        case 0x2c:
                        case 0x2d:
                        case 0x2f:
                            instSRA_r8();
                            break;
                        case 0x38:
                        case 0x39:
                        case 0x3a:
                        case 0x3b:
                        case 0x3c:
                        case 0x3d:
                        case 0x3f:
                            instSRL_r8();
                            break;
                        case 0x20:
                        case 0x21:
                        case 0x22:
                        case 0x23:
                        case 0x24:
                        case 0x25:
                        case 0x27:
                            instSLA_r8();
                            break;
                        case 0x40:
                        case 0x41:
                        case 0x42:
                        case 0x43:
                        case 0x44:
                        case 0x45:
                        case 0x47:
                        case 0x48:
                        case 0x49:
                        case 0x4a:
                        case 0x4b:
                        case 0x4c:
                        case 0x4d:
                        case 0x4f:
                        case 0x50:
                        case 0x51:
                        case 0x52:
                        case 0x53:
                        case 0x54:
                        case 0x55:
                        case 0x57:
                        case 0x58:
                        case 0x59:
                        case 0x5a:
                        case 0x5b:
                        case 0x5c:
                        case 0x5d:
                        case 0x5f:
                        case 0x60:
                        case 0x61:
                        case 0x62:
                        case 0x63:
                        case 0x64:
                        case 0x65:
                        case 0x67:
                        case 0x68:
                        case 0x69:
                        case 0x6a:
                        case 0x6b:
                        case 0x6c:
                        case 0x6d:
                        case 0x6f:
                        case 0x70:
                        case 0x71:
                        case 0x72:
                        case 0x73:
                        case 0x74:
                        case 0x75:
                        case 0x77:
                        case 0x78:
                        case 0x79:
                        case 0x7a:
                        case 0x7b:
                        case 0x7c:
                        case 0x7d:
                        case 0x7f:
                            instBIT_b_r8();
                            break;
                        case 0x46:
                        case 0x4e:
                        case 0x56:
                        case 0x5e:
                        case 0x66:
                        case 0x6e:
                        case 0x76:
                        case 0x7e:
                            instBIT_b_iHL();
                            break;
                        case 0xc0:
                        case 0xc1:
                        case 0xc2:
                        case 0xc3:
                        case 0xc4:
                        case 0xc5:
                        case 0xc7:
                        case 0xc8:
                        case 0xc9:
                        case 0xca:
                        case 0xcb:
                        case 0xcc:
                        case 0xcd:
                        case 0xcf:
                        case 0xd0:
                        case 0xd1:
                        case 0xd2:
                        case 0xd3:
                        case 0xd4:
                        case 0xd5:
                        case 0xd7:
                        case 0xd8:
                        case 0xd9:
                        case 0xda:
                        case 0xdb:
                        case 0xdc:
                        case 0xdd:
                        case 0xdf:
                        case 0xe0:
                        case 0xe1:
                        case 0xe2:
                        case 0xe3:
                        case 0xe4:
                        case 0xe5:
                        case 0xe7:
                        case 0xe8:
                        case 0xe9:
                        case 0xea:
                        case 0xeb:
                        case 0xec:
                        case 0xed:
                        case 0xef:
                        case 0xf0:
                        case 0xf1:
                        case 0xf2:
                        case 0xf3:
                        case 0xf4:
                        case 0xf5:
                        case 0xf7:
                        case 0xf8:
                        case 0xf9:
                        case 0xfa:
                        case 0xfb:
                        case 0xfc:
                        case 0xfd:
                        case 0xff:
                            instSET_b_r8();
                            break;
                        case 0xc6:
                        case 0xce:
                        case 0xd6:
                        case 0xde:
                        case 0xe6:
                        case 0xee:
                        case 0xf6:
                        case 0xfe:
                            instSET_b_iHL();
                            break;
                        case 0x80:
                        case 0x81:
                        case 0x82:
                        case 0x83:
                        case 0x84:
                        case 0x85:
                        case 0x87:
                        case 0x88:
                        case 0x89:
                        case 0x8a:
                        case 0x8b:
                        case 0x8c:
                        case 0x8d:
                        case 0x8f:
                        case 0x90:
                        case 0x91:
                        case 0x92:
                        case 0x93:
                        case 0x94:
                        case 0x95:
                        case 0x97:
                        case 0x98:
                        case 0x99:
                        case 0x9a:
                        case 0x9b:
                        case 0x9c:
                        case 0x9d:
                        case 0x9f:
                        case 0xa0:
                        case 0xa1:
                        case 0xa2:
                        case 0xa3:
                        case 0xa4:
                        case 0xa5:
                        case 0xa7:
                        case 0xa8:
                        case 0xa9:
                        case 0xaa:
                        case 0xab:
                        case 0xac:
                        case 0xad:
                        case 0xaf:
                        case 0xb0:
                        case 0xb1:
                        case 0xb2:
                        case 0xb3:
                        case 0xb4:
                        case 0xb5:
                        case 0xb7:
                        case 0xb8:
                        case 0xb9:
                        case 0xba:
                        case 0xbb:
                        case 0xbc:
                        case 0xbd:
                        case 0xbf:
                            instRES_b_r8();
                            break;
                        case 0x86:
                        case 0x8e:
                        case 0x96:
                        case 0x9e:
                        case 0xa6:
                        case 0xae:
                        case 0xb6:
                        case 0xbe:
                            instRES_b_iHL();
                            break;
                        default:
                            traceCPU = 10;
                            traceCPU(2, "Unknown instruction!!!");
                            prevPCs();
                            running = false;
                            break;
                    }
                }
                break;
                case 0xed: {
                    opcode = memory.getByte(PC.r + 1);
                    switch (opcode) {
                        case 0x56:
                            instIM_1();
                            break;
                        case 0xa1:
                            instCPI();
                            break;
                        case 0xb0:
                            instLDIR();
                            break;
                        case 0xb8:
                            instLDDR();
                            break;
                        case 0xa8:
                            instLDD();
                            break;
                        case 0x44:
                            instNEG();
                            break;
                        case 0x40:
                        case 0x48:
                        case 0x50:
                        case 0x58:
                        case 0x60:
                        case 0x68:
                        case 0x70:
                        case 0x78:
                            instIN_r8_C();
                            break;
                        case 0x41:
                        case 0x49:
                        case 0x51:
                        case 0x59:
                        case 0x61:
                        case 0x69:
                        case 0x79:
                            instOUT_C_r8();
                            break;
                        case 0x43:
                        case 0x53:
                        case 0x63:
                        case 0x73:
                            instLD_in16_r16();
                            break;
                        case 0x42:
                        case 0x52:
                        case 0x62:
                        case 0x72:
                            instSBC_HL_r16();
                            break;
                        case 0x4b:
                        case 0x5b:
                        case 0x6b:
                        case 0x7b:
                            instLD_r16_in16();
                            break;
                        case 0xa0:
                            instLDI();
                            break;
                        case 0x4a:
                        case 0x5a:
                        case 0x6a:
                        case 0x7a:
                            instADC_HL_r16();
                            break;
                        case 0x4d:
                            instRETI();
                            break;
                        case 0x6f:
                            instRLD();
                            break;
                        case 0x67:
                            instRRD();
                            break;
                        case 0x5f:
                            instLD_A_IM();
                            break;
                        case 0x57:
                            instLD_A_I();
                            break;
                        default:
                            traceCPU = 10;
                            traceCPU(2, "Unknown instruction!!!");
                            prevPCs();
                            running = false;
                            break;
                    }
                }
                break;
                case 0xfd:
                case 0xdd: {
                    r16IND[2] = IND = (opcode == 0xdd) ? IX : IY;
                    int prefix = opcode;
                    opcode = memory.getByte(PC.r + 1);
                    switch (opcode) {
                        case 0x21:
                            instLD_IND_n16();
                            break;
                        case 0x22:
                            instLD_in16_IND();
                            break;
                        case 0x2d: 
                            if (prefix == 0xdd) {
                                // dd 2d
                                instDEC_IXL();
                            } else {
                                // fd 2d
                                instDEC_IYL(); 
                            }
                            break;
                        case 0xe1:
                            instPOP_IND();
                            break;
                        case 0xe3:
                            instEX_iSP_IND();
                            break;
                        case 0xe5:
                            instPUSH_IND();
                            break;
                        case 0xe9:
                            instJP_IND();
                            break;
                        case 0x09:
                        case 0x19:
                        case 0x29:
                        case 0x39:
                            instADD_IND_r16();
                            break;
                        case 0x2a:
                            instLD_IND_in16();
                            break;
                        case 0x34:
                            instINC_iINDd();
                            break;
                        case 0x35:
                            instDEC_iINDd();
                            break;
                        case 0x36:
                            instLD_iINDd_n8();
                            break;
                        case 0x23:
                            instINC_IND();
                            break;
                        case 0x2b:
                            instDEC_IND();
                            break;
                        case 0xae:
                            instXOR_iINDd();
                            break;
                        // LD r,(IND+d)
                        case 0x46:
                        case 0x4e:
                        case 0x56:
                        case 0x5e:
                        case 0x66:
                        case 0x6e:
                        case 0x7e:
                            instLD_r8_iINDd();
                            break;
                        // LD (IND+d),r
                        case 0x70:
                        case 0x71:
                        case 0x72:
                        case 0x73:
                        case 0x74:
                        case 0x75:
                        case 0x77:
                            instLD_iINDd_r8();
                            break;
                        case 0xcb: {
                            opcode = memory.getByte(PC.r + 3);
                            switch (opcode) {
                                case 0x46:
                                case 0x4e:
                                case 0x56:
                                case 0x5e:
                                case 0x66:
                                case 0x6e:
                                case 0x76:
                                case 0x7e:
                                    instBIT_b_iINDd();
                                    break;
                                case 0x86:
                                case 0x8e:
                                case 0x96:
                                case 0x9e:
                                case 0xa6:
                                case 0xae:
                                case 0xb6:
                                case 0xbe:
                                    instRES_b_iINDd();
                                    break;
                                case 0xc6:
                                case 0xce:
                                case 0xd6:
                                case 0xde:
                                case 0xe6:
                                case 0xee:
                                case 0xf6:
                                case 0xfe:
                                    instSET_b_iINDd();
                                    break;
                                default: {
                                    traceCPU = 1;
                                    traceCPU(4,
                                            "Unknown instruction!!!");
                                    prevPCs();
                                    running = false;
                                }
                                break;
                            }
                        }
                        break;
                        default:
                            traceCPU = 1;
                            traceCPU(2, "Unknown instruction!!!");
                            prevPCs();
                            running = false;
                            break;
                    }
                }
                break;
                default:
                    traceCPU = 10;
                    traceCPU(1, "Unknown instruction!!!");
                    prevPCs();
                    running = false;
                    break;
            }
            instCount++;
            /*
             * Z80 frequency: 3.125Mhz ==> 3125000 T Cycle/s
             * 10 ms alatt ==> 31250 T Cycle
             */
            if (lastWaitT > 62125) {
                lastWaitT = 0;
                long h = 50 - (System.currentTimeMillis() - lastWaitMills);
                lastWaitMills = System.currentTimeMillis();
                // if ( h > 0) {
                try {
//						Thread.sleep( h);
                    Thread.sleep(20);
                } catch (Exception e) {
                }
                //}
            }
        }

        m1 = System.currentTimeMillis() - m1;
        log.write("Elapsed time: " + Long.toString(m1));
        log.write("Executed instuctions: " + Long.toString(instCount));
        log.write("T Cycles:" + Long.toString(t));
        d1 = t;
        d2 = (double) m1 / 1000;
        log.write("Frequency: " + Double.toString((d1 / d2) / 1000000));

    }

    private boolean initZ80() {
        int tt[] = {0, 1, 2, 3, 4, 5, 7}, i, k;
        instCount = 0;
        t = 0;
        String s = "";
        for (k = 0; k < 8; k++) {
            s += "case 0x" + toHexString(0x86 | k << 3, 2) + ": ";
        }
        //System.out.println(s);
		/*		for ( k = 0; k < 8; k++)
         for ( i = 0; i < 7; i ++)
         s += "case 0x" + toHexString( 0x80 | (k<<3) | tt[i], 2) + ": ";
         */
        /*		for ( k = 0; k < 256; k++) {
         i  = ((k & 0x01) != 0) ? 1 : 0;
         i += ((k & 0x02) != 0) ? 1 : 0;
         i += ((k & 0x04) != 0) ? 1 : 0;
         i += ((k & 0x08) != 0) ? 1 : 0;
         i += ((k & 0x10) != 0) ? 1 : 0;
         i += ((k & 0x20) != 0) ? 1 : 0;
         i += ((k & 0x40) != 0) ? 1 : 0;
         i += ((k & 0x80) != 0) ? 1 : 0;
         s += "," + toHexString(
         ( i%2 == 0) ? SZTable[k] | 0x04 : SZTable[k], 2);
         }
         */
        //		for ( k = 0; k < 8; k++) s += "instXX[" + toHexString( 0xc2 | k<<3, 2) + "] = ";
        log.write(s);
        return true;
    }

    public final void setFlagS(boolean x) {
        if (x) {
            F.r |= SET_S;
        } else {
            F.r &= ~SET_S;
        }
    }

    public final void setFlagZ(boolean x) {
        if (x) {
            F.r |= SET_Z;
        } else {
            F.r &= ~SET_Z;
        }
    }

    public final void setFlagH(boolean x) {
        if (x) {
            F.r |= SET_H;
        } else {
            F.r &= ~SET_H;
        }
    }

    public final void setFlagPV(boolean x) {
        if (x) {
            F.r |= SET_PV;
        } else {
            F.r &= ~SET_PV;
        }
    }

    public final void setFlagN(boolean x) {
        if (x) {
            F.r |= SET_N;
        } else {
            F.r &= ~SET_N;
        }
    }

    public final void setFlagC(boolean x) {
        if (x) {
            F.r |= SET_C;
        } else {
            F.r &= ~SET_C;
        }
    }

    public final void setFlag3(boolean x) {
        if (x) {
            F.r |= SET_3;
        } else {
            F.r &= ~SET_3;
        }
    }

    public final void setFlag5(boolean x) {
        if (x) {
            F.r |= SET_5;
        } else {
            F.r &= ~SET_5;
        }
    }

    public final boolean getFlagS() {
        return ((F.r & SET_S) != 0);
    }

    public final boolean getFlagZ() {
        return ((F.r & SET_Z) != 0);
    }

    public final boolean getFlagH() {
        return ((F.r & SET_H) != 0);
    }

    public final boolean getFlagPV() {
        return ((F.r & SET_PV) != 0);
    }

    public final boolean getFlagN() {
        return ((F.r & SET_N) != 0);
    }

    public final boolean getFlagC() {
        return ((F.r & SET_C) != 0);
    }

    public final boolean getFlag3() {
        return ((F.r & SET_3) != 0);
    }

    public final boolean getFlag5() {
        return ((F.r & SET_5) != 0);
    }

    public final boolean getccValue(int pIndex) {
        switch (pIndex) {
            case (0x00):
                return (!getFlagZ());
            case (0x01):
                return (getFlagZ());
            case (0x02):
                return (!getFlagC());
            case (0x03):
                return (getFlagC());
            case (0x04):
                return (!getFlagPV());
            case (0x05):
                return (getFlagPV());
            case (0x06):
                return (!getFlagS());
            case (0x07):
                return (getFlagS());
        }
        return false;
    }

    public final String getccName(int p) {
        return flagNames[p];
    }

    public final void instNOP() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "NOP");
        }
        PC.r++;
        t += 4;
    }

    public final void interrupt() {
        interrupt = false;
        // If not a non-maskable interrupt
        if (!IFF1) {
            return;
        }
        // If we were in HALT cycle
        if (halt) {
            PC.add(1);
            halt = false;
        }
        switch (IM.r) {
            case 0:
            // IM0
            case 1:
                // IM1
                // Push PC
                SP.r = (SP.r - 2) & 0xffff;
                memory.setWord(SP.r, PC.r);
                IFF1 = false;
                IFF2 = false;
                PC.r = 0x38;
                return;
            case 2:
                // IM2
                // Push PC
                SP.r = (SP.r - 2) & 0xffff;
                memory.setWord(SP.r, PC.r);
                IFF1 = false;
                IFF2 = false;
                int t = (I.r << 8) | 0x00ff;
                PC.r = memory.getWord(t);
                return;
        }
        return;
    }

    public final void instJP() {
        int addr = memory.getWord(PC.r + 1);
        if (traceCPU >= TRC_J) {
            traceCPU(3, "JP    " + toHexString(addr, 4));
        }
        PC.r = addr;
        t += 10;
    }

    public final void instJP_HL() {
        int addr = HL.get();
        if (traceCPU >= TRC_J) {
            traceCPU(3, "JP    HL");
        }
        PC.r = addr;
        t += 4;
    }

    public final void instJP_IND() {
        if (traceCPU >= TRC_J) {
            traceCPU(3, "JP    (" + IND.name + ")");
        }
        PC.r = IND.r;
        t += 8;
    }

    public final void instJR_n8() {
        int n = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_J) {
            traceCPU(2, "JR    " + toHexString(PC.r + 2 + (byte) n, 4));
        }
        PC.r = (PC.r + 2 + (byte) n) & 0xffff;
        t += 12;
    }

    public final void instCALL_n16() {
        int addr = memory.getWord(PC.r + 1);
        int newSP = (SP.r - 2) & 0xffff;
        memory.setWord(newSP, (PC.r + 3) & 0xffff);
        if (traceCPU >= TRC_CRR) {
            traceCPU(3, "CALL  " + toHexString(addr, 4) + "  ; (SP) <= "
                    + toHexString((PC.r + 3) & 0xffff, 4));
            memory.dump(newSP, 8);
        }
        PC.r = addr;
        SP.r = newSP;
        t += 17;
    }

    public final void instCALL_cc_n16() {
        int cc = (opcode & 0x38) >> 3;
        int addr = memory.getWord(PC.r + 1);
        if (traceCPU >= TRC_CRR) {
            traceCPU(3, "CALL  " + getccName(cc)
                    + (getccValue(cc) ? "(1)" : "(0)") + ","
                    + toHexString(addr, 4) + "  ; (SP) <= "
                    + toHexString((PC.r + 3) & 0xffff, 4));
        }
        PC.r = (PC.r + 3) & 0xffff;
        t += 10;
        if (getccValue(cc)) {
            SP.r = (SP.r - 2) & 0xffff;
            memory.setWord(SP.r, PC.r);
            PC.r = addr;
            t += 7;
        }
    }

    public final void instRST_p() {
        int p = opcode & 0x38;
        if (traceCPU >= TRC_CRR) {
            traceCPU(1, "RST   " + toHexString(p, 2));
        }
        SP.r = (SP.r - 2) & 0xffff;
        memory.setWord(SP.r, (PC.r + 1) & 0xffff);
        PC.r = p;
        t += 11;
    }

    public final void instRET() {
        int addr = memory.getWord(SP.r);
        if (traceCPU >= TRC_CRR) {
            traceCPU(1, "RET     ; PC <= " + toHexString(addr, 4));
            memory.dump(SP.r, 8);
        }
        SP.r = (SP.r + 2) & 0xffff;
        PC.r = addr;
        t += 10;
    }

    public final void instRETI() {
        int addr = memory.getWord(SP.r);
        if (traceCPU >= TRC_CRR) {
            traceCPU(1, "RETI    ; PC <= " + toHexString(addr, 4));
            memory.dump(SP.r, 8);
        }
        SP.r = (SP.r + 2) & 0xffff;
        PC.r = addr;
        t += 14;
    }

    public final void instRET_cc() {
        int cc = (opcode & 0x38) >> 3;
        int newSP = (SP.r + 2) & 0xffff;
        int addr = memory.getWord(SP.r);
        if (traceCPU >= TRC_CRR) {
            traceCPU(1, "RET   " + getccName(cc)
                    + (getccValue(cc) ? "(1)" : "(0)") + "  ; PC <= "
                    + toHexString(addr, 4));
            memory.dump(SP.r, 8);
        }
        if (getccValue(cc)) {
            SP.r = newSP;
            PC.r = addr;
            t += 11;
        } else {
            addPC_T(1, 5);
        }
    }

    public final void instDI() {
        if (traceCPU >= TRC_GP) {
            traceCPU(1, "DI");
        }
        IFF1 = IFF2 = false;
        addPC_T(1, 4);
    }

    public final void instEI() {
        if (traceCPU >= TRC_GP) {
            traceCPU(1, "EI");
        }
        IFF1 = IFF2 = true;
        addPC_T(1, 4);
    }

    public final void instHALT() {
        if (traceCPU >= TRC_GP) {
            traceCPU(1, "HALT");
        }
        addPC_T(0, 4);
        halt = true;
    }

    /**
     * Load interrupt vector register to A.
     */
    public final void instLD_A_I() {
        A.r = I.r;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    " + A.name + "," + IM.name);
        }

        setFlagS(I.r < 0);
        setFlagZ(I.r == 0);
        setFlagH(false);
        // if an interrupt occurs during the execution of this 
        // instruction, PV is set to zero.        
        setFlagPV(IFF2);
        setFlagN(false);

        addPC_T(2, 9);
    }

    /**
     * Load Memory refresh register to A.
     */
    public final void instLD_A_IM() {
        A.r = IM.r;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    " + A.name + "," + IM.name);
        }

        setFlagS(IM.r < 0);
        setFlagZ(IM.r == 0);
        setFlagH(false);
        // if an interrupt occurs during the execution of this 
        // instruction, PV is set to zero.        
        setFlagPV(IFF2);
        setFlagN(false);

        addPC_T(2, 9);
    }

    /* 8bit load group
     * LD r8, r8
     * LD r8, n8
     */
    public final void instLD_r8_r8() {
        Reg8 r1 = r8[(opcode & 0x38) >> 3];
        Reg8 r2 = r8[(opcode & 0x07)];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    " + r1.name + "," + r2.name);
        }
        r1.r = r2.r;
        addPC_T(1, 4);
    }
    
    public final void instLD_SP_HL() {
        SP.r = HL.r;
                if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    " + SP.name + "," + HL.name);
        }
        addPC_T(1, 6);
    }

    public final void instLD_r8_n8() {
        int n8 = memory.getByte(PC.r + 1);
        Reg8 r1 = r8[(opcode & 0x38) >> 3];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "LD    " + r1.name + "," + b2hds(n8));
        }
        r1.r = n8;
        addPC_T(2, 7);
    }

    public final void instLD_r8_iHL() {
        int n8 = memory.getByte(HL.get());
        Reg8 r1 = r8[(opcode & 0x38) >> 3];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    " + r1.name + ",(HL)" + "  ; (HL)=" + w2hds(n8));
        }
        r1.r = n8;
        addPC_T(1, 7);
    }

    public final void instLD_iHL_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    (HL)," + r1.name);
        }
        memory.setByte(HL.get(), r1.r);
        addPC_T(1, 7);
    }

    public final void instLD_in16_HL() {
        int addr = memory.getWord(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(3, "LD    (" + toHexString(addr, 4) + "),HL");
        }
        memory.setWord(addr, HL.get());
        addPC_T(3, 16);
    }

    public final void instLD_in16_IND() {
        int addr = memory.getWord(PC.r + 2);
        if (traceCPU >= TRC_ALL) {
            traceCPU(3, "LD    (" + toHexString(addr, 4) + ")," + IND.name);
        }
        memory.setWord(addr, IND.r);
        addPC_T(4, 20);
    }

    public final void instLD_HL_in16() {
        int addr = memory.getWord(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(3, "LD    HL,(" + toHexString(addr, 4) + ")");
        }
        HL.set(memory.getWord(addr));
        addPC_T(3, 16);
    }

    public final void instLD_IND_in16() {
        int addr = memory.getWord(PC.r + 2);
        if (traceCPU >= TRC_ALL) {
            traceCPU(4, "LD    " + IND.name + ",(" + toHexString(addr, 4) + ")");
        }
        IND.r = memory.getWord(addr);
        addPC_T(4, 20);
    }

    public final void instLD_in16_r16() {
        Reg16 R1 = r16[(opcode & 0x30) >> 4];
        int addr = memory.getWord(PC.r + 2);
        if (traceCPU >= TRC_ALL) {
            traceCPU(4, "LD    (" + toHexString(addr, 4) + ")," + R1.name);
        }
        memory.setWord(addr, R1.get());
        addPC_T(4, 20);
    }

    public final void instLD_r16_in16() {
        Reg16 R1 = r16[(opcode & 0x30) >> 4];
        int addr = memory.getWord(PC.r + 2);
        if (traceCPU >= TRC_ALL) {
            traceCPU(4, "LD    " + R1.name + ",(" + toHexString(addr, 4) + ")");
        }
        R1.set(memory.getWord(addr));
        PC.r += 4;
        t += 20;
    }

    public final void instLD_iHL_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "LD    (HL)," + toHexString(n8, 2) + "   ; (HL)="
                    + w2hds(HL.get()));
        }
        memory.setByte(HL.get(), n8);
        addPC_T(2, 10);
    }

    public final void instLD_r8_iINDd() {
        int d = memory.getByte(PC.r + 2);
        int n8 = memory.getByte((IND.r + (byte) d) & 0xffff);
        Reg8 r1 = r8[(opcode & 0x38) >> 3];
        if (traceCPU >= TRC_ALL) {
            traceCPU(3, "LD    " + r1.name + ",(IY+d)");
        }
        r1.r = n8;
        addPC_T(3, 19);
    }

    public final void instLD_iINDd_r8() {
        int d = memory.getByte(PC.r + 2);
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(3, "LD    (" + IND.name + "+d)," + r1.name);
        }
        memory.setByte((IND.r + (byte) d) & 0xffff, r1.r);
        addPC_T(3, 19);
    }

    public final void instLD_iINDd_n8() {
        int d = memory.getByte(PC.r + 2);
        int n8 = memory.getByte(PC.r + 3);
        if (traceCPU >= TRC_ALL) {
            traceCPU(4, "LD    (" + IND.name + "+d)," + b2hds(n8));
        }
        memory.setByte((IND.r + (byte) d) & 0xffff, n8);
        addPC_T(4, 19);
    }

    public final void instLD_IND_n16() {
        IND.r = memory.getWord(PC.r + 2);
        if (traceCPU >= TRC_ALL) {
            traceCPU(4, "LD    " + IND.name + "," + w2hds(IND.r));
        }
        addPC_T(4, 14);
    }

    public final void instLD_iDE_A() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    (DE),A");
        }
        memory.setByte(DE.get(), A.r);
        addPC_T(1, 7);
    }

    public final void instLD_iBC_A() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    (BC),A");
        }
        memory.setByte(BC.get(), A.r);
        addPC_T(1, 7);
    }

    public final void instLD_A_in16() {
        int addr = memory.getWord(PC.r + 1);
        int n8 = memory.getByte(addr);
        if (traceCPU >= TRC_ALL) {
            traceCPU(3, "LD    A,(" + toHexString(addr, 4) + ")    ; ("
                    + toHexString(addr, 4) + ")=" + b2hds(n8));
        }
        A.r = n8;
        addPC_T(3, 13);
    }

    public final void instLD_in16_A() {
        int addr = memory.getWord(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(3, "LD    (" + toHexString(addr, 4) + "),A");
        }
        memory.setByte(addr, A.r);
        addPC_T(3, 13);
    }

    public final void instIM_1() {
        if (traceCPU >= TRC_GP) {
            traceCPU(1, "IM    1");
        }
        IM.r = 1;
        addPC_T(2, 8);
    }

    public final void instEX_AF_AF() {
        int temp;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "EX    AF,AF'");
        }
        temp = AF.get();
        AF.set(AF1);
        AF1 = temp;
        addPC_T(1, 4);
    }

    public final void instEXX() {
        int temp;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "EXX");
        }
        temp = BC.get();
        BC.set(BC1);
        BC1 = temp;
        temp = DE.get();
        DE.set(DE1);
        DE1 = temp;
        temp = HL.get();
        HL.set(HL1);
        HL1 = temp;
        addPC_T(1, 4);
    }

    public final void instEX_iSP_HL() {
        int temp;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "EX    (SP),HL");
        }
        temp = memory.getWord(SP.r);
        memory.setWord(SP.r, HL.get());
        HL.set(temp);
        addPC_T(1, 19);
    }

    public final void instEX_iSP_IND() {
        int temp;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "EX    (SP)," + IND.name);
        }
        temp = memory.getWord(SP.r);
        memory.setWord(SP.r, IND.r);
        IND.r = temp;
        addPC_T(2, 23);
    }

    public final void instEX_DE_HL() {
        int t;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "EX    DE,HL");
        }
        t = HL.get();
        HL.set(DE.get());
        DE.set(t);
        addPC_T(1, 4);
    }

    public final void instOUT_n_A() {
        int n = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "OUT   (" + toHexString(n, 2) + "),A");
        }
        port.setPort(n, A.r);
        addPC_T(2, 11);
    }

    public final void instOUT_C_r8() {
        Reg8 r1 = r8[(memory.getByte(PC.r + 1) & 0x38) >> 3];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "OUT   (C)," + r1.name);
        }
        port.setPort(C.r, r1.r);
        addPC_T(2, 12);
    }

    public final void instIN_A_n() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "IN    A,(" + toHexString(n8, 2) + ")");
        }
        A.r = port.getPort(n8);
        addPC_T(2, 11);
    }

    public final void instIN_r8_C() {
        // r = 00xxx000
        int reg = (opcode & 0x38) >> 3;
        int b = port.getPort(C.r);
        int x = b;
        x ^= x >> 4;
        x ^= x >> 2;
        x ^= x >> 1;
        x &= 1;

        setFlagS((b & SET_S) != 0);
        setFlagZ(b == 0);
        setFlagPV(x == 1);
        setFlagH(false);
        setFlagN(false);
        if (0x6 == reg) {
            // r = 110 set flags only            
            if (traceCPU >= TRC_ALL) {
                traceCPU(2, "IN    C,(?)");
            }
        } else {
            Reg8 r1 = r8[reg];
            r1.r = b;
            if (traceCPU >= TRC_ALL) {
                traceCPU(2, "IN    C,(" + r1.name + ")");
            }
        }

        addPC_T(2, 12);
    }

    public final void instLD_r16_n16() {
        int n16 = memory.getWord(PC.r + 1);
        Reg16 R1 = r16[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(3, "LD    " + R1.name + "," + w2hds(n16));
        }
        R1.set(n16);
        addPC_T(3, 10);
    }

    public final void instINC_r16() {
        Reg16 R1 = r16[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "INC   " + R1.name);
        }
        R1.set((R1.get() + 1) & 0xffff);
        addPC_T(1, 6);
    }

    public final void instINC_IND() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "INC   " + IND.name);
        }
        IND.r = (IND.r + 1) & 0xffff;
        addPC_T(2, 10);
    }

    public final void instDEC_r16() {
        Reg16 R1 = r16[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "DEC   " + R1.name);
        }
        R1.set((R1.get() - 1) & 0xffff);
        addPC_T(1, 6);
    }

    public final void instDEC_IND() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "DEC   " + IND.name);
        }
        IND.r = (IND.r - 1) & 0xffff;
        addPC_T(2, 10);
    }

    public final void instOR_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "OR    " + r1.name);
        }
        or_a(r1.r);
        addPC_T(1, 4);
    }

    public final void instOR_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "OR    " + b2hds(n8));
        }
        or_a(n8);
        addPC_T(2, 7);
    }

    public final void instOR_iHL() {
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "OR    (HL)  ; (HL)=" + b2hds(n8));
        }
        or_a(n8);
        addPC_T(1, 7);
    }

    public final void instAND_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "AND   " + r1.name);
        }
        and_a(r1.r);
        addPC_T(1, 4);
    }

    public final void instAND_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "AND   " + b2hds(n8));
        }
        and_a(n8);
        addPC_T(2, 7);
    }

    public final void instAND_iHL() {
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "AND   (HL) ; (HL)=" + b2hds(n8));
        }
        and_a(n8);
        addPC_T(1, 7);
    }

    public final void instXOR_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "XOR   " + r1.name);
        }
        xor_a(r1.r);
        addPC_T(1, 4);
    }

    public final void instXOR_iHL() {
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "XOR   (HL)  ; (HL)=" + b2hds(n8));
        }
        xor_a(n8);
        addPC_T(1, 7);
    }

    public final void instXOR_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "XOR   " + b2hds(n8));
        }
        xor_a(n8);
        addPC_T(1, 7);
    }

    public final void instXOR_iINDd() {
        int d = memory.getByte(PC.r + 2);
        int addr = (IND.r + (byte) d) & 0xffff;
        int n8 = memory.getByte(addr);
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "XOR   (" + IND.name + "+" + b2hds(n8) + ")");
        }
        xor_a(n8);
        addPC_T(3, 19);
    }

    public final void instINC_r8() {
        Reg8 r1 = r8[(opcode & 0x38) >> 3];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "INC   " + r1.name);
        }
        r1.r = inc8(r1.r);
        addPC_T(1, 4);
    }

    public final void instINC_iHL() {
        int nv, addr = HL.get();
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "INC   (HL)");
        }
        nv = memory.getByte(addr);
        nv = inc8(nv);
        memory.setByte(addr, nv);
        addPC_T(1, 11);
    }

    public final void instDEC_r8() {
        Reg8 r1 = r8[(opcode & 0x38) >> 3];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "DEC   " + r1.name);
        }
        r1.r = dec8(r1.r);
        addPC_T(1, 4);
    }
    
    public final void instDEC_IYL() {
        Reg8 r1 = this.IY.rl;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "DEC   " + r1.name);
        }
        // FIXME since this is undocumented, T and M only guessed
        // TODO there are many variants of this instruction IYH, IXH, INC...
        r1.r = dec8(r1.r);
        addPC_T(1, 4);
    }
    
    public final void instDEC_IXL() {
        Reg8 r1 = this.IX.rl;
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "DEC   " + r1.name);
        }
        // FIXME since this is undocumented, T and M only guessed
        // TODO there are many variants of this instruction
        r1.r = dec8(r1.r);
        addPC_T(1, 4);
    }

    public final void instDEC_iHL() {
        int addr = HL.get();
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "DEC   (HL)");
        }
        memory.setByte(addr, dec8(memory.getByte(addr)));
        addPC_T(1, 11);
    }

    public final void instINC_iINDd() {
        int d = memory.getByte(PC.r + 2);
        int addr = (IND.r + (byte) d) & 0xffff;
        int n8 = memory.getByte(addr);
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "INC   (" + IND.name + "+" + b2hds(n8) + ")");
        }
        memory.setByte(addr, inc8(n8));
        addPC_T(3, 23);
    }

    public final void instDEC_iINDd() {
        int d = memory.getByte(PC.r + 2);
        int addr = (IND.r + (byte) d) & 0xffff;
        int n8 = memory.getByte(addr);
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "DEC   (" + IND.name + "+" + b2hds(n8) + ")");
        }
        memory.setByte(addr, dec8(n8));
        addPC_T(3, 23);
    }

    public final void instLD_A_iBC() {
        int n8 = memory.getByte(BC.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    A,(BC)  ; (BC)=" + b2hds(n8));
        }
        A.r = n8;
        addPC_T(1, 7);
    }

    public final void instLD_A_iDE() {
        int n8 = memory.getByte(DE.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "LD    A,(DE)  ; (DE)=" + b2hds(n8));
        }
        A.r = n8;
        addPC_T(1, 7);
    }

    public final void instCPI() {
        int n8 = memory.getByte(HL.get());
        boolean c = getFlagC();
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "CPI     ; (HL)=" + b2hds(n8));
        }
        cp_a(n8);
        HL.set(HL.get() + 1);
        BC.set(BC.get() - 1);
        setFlagPV((BC.get() != 0));
        setFlagC(c);
        addPC_T(2, 16);
    }

    public final void instLDIR() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "LDIR");
        }
        do {
            memory.setByte(DE.get(), memory.getByte(HL.get()));
            DE.add(1);
            HL.add(1);
            BC.sub(1);
            t += 21;
        } while (BC.get() != 0);
        setFlagH(false);
        setFlagPV(false);
        setFlagN(false);
        addPC_T(2, 16);
    }

    public final void instLDDR() {
        int de = DE.get(), hl = HL.get(), bc = BC.get();
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "LDDR");
        }
        do {
            memory.setByte(de, memory.getByte(hl));
            de = (de - 1) & 0xffff;
            hl = (hl - 1) & 0xffff;
            bc = (bc - 1) & 0xffff;
            t += 21;
        } while (bc != 0);
        DE.set(de);
        HL.set(hl);
        BC.set(bc);
        setFlagH(false);
        setFlagPV(false);
        setFlagN(false);
        addPC_T(2, 16);
    }

    public final void instLDD() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "LDD");
        }
        memory.setByte(DE.get(), memory.getByte(HL.get()));
        DE.sub(1);
        HL.sub(1);
        BC.sub(1);
        setFlagH(false);
        setFlagPV(BC.get() != 0);
        setFlagN(false);
        addPC_T(2, 16);
    }

    public final void instLDI() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "LDI");
        }
        memory.setByte(DE.get(), memory.getByte(HL.get()));
        DE.add(1);
        HL.add(1);
        BC.sub(1);
        setFlagH(false);
        setFlagPV((BC.get() != 0));
        setFlagN(false);
        addPC_T(2, 16);
    }

    public final void instJR_NZ_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_Jcc) {
            traceCPU(2, "JR    NZ" + (!getFlagZ() ? "(1)" : "(0)") + ","
                    + toHexString(PC.r + 2 + (byte) n8, 4));
        }
        addPC_T(2, 7);
        if (!getFlagZ()) {
            PC.r += (byte) n8;
            t += 5;
        }
    }

    public final void instJP_cc_n16() {
        int cc = (opcode & 0x38) >> 3;
        int addr = memory.getWord(PC.r + 1);
        if (traceCPU >= TRC_Jcc) {
            traceCPU(3, "JP    " + getccName(cc)
                    + (getccValue(cc) ? "(1)" : "(0)") + "," + w2hds(addr));
        }
        if (getccValue(cc)) {
            PC.r = addr;
        } else {
            PC.add(3);
        }
        t += 10;
    }

    public final void instJR_Z_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_Jcc) {
            traceCPU(2, "JR    Z" + (getFlagZ() ? "(1)" : "(0)") + ","
                    + toHexString(PC.r + 2 + (byte) n8, 4));
        }
        addPC_T(2, 7);
        if (getFlagZ()) {
            PC.r = (PC.r + (byte) n8) & 0xffff;
            t += 5;
        }
    }

    public final void instJR_C_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_Jcc) {
            traceCPU(2, "JR    C" + (getFlagC() ? "(1)" : "(0)") + ","
                    + toHexString(PC.r + 2 + (byte) n8, 4));
        }
        addPC_T(2, 7);
        if (getFlagC()) {
            PC.r = (PC.r + (byte) n8) & 0xffff;
            t += 5;
        }
    }

    public final void instJR_NC_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_Jcc) {
            traceCPU(2, "JR    NC" + (!getFlagC() ? "(1)" : "(0)") + ","
                    + toHexString(PC.r + 2 + (byte) n8, 4));
        }
        addPC_T(2, 7);
        if (!getFlagC()) {
            PC.r = (PC.r + (byte) n8) & 0xffff;
            t += 5;
        }
    }

    public final void instDJNZ() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_Jcc) {
            traceCPU(2, "DJNZ  " + toHexString(PC.r + 2 + (byte) n8, 4));
        }
        B.r--;
        B.r &= 0xff;
        addPC_T(2, 8);
        if (B.r != 0) {
            PC.r = (PC.r + (byte) n8) & 0xffff;
            t += 5;
        }
    }

    public final void instPUSH_r16() {
        Reg16 R1 = r16AF[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "PUSH  " + R1.name);
        }
        SP.sub(2);
        memory.setWord(SP.r, R1.get());
        addPC_T(1, 11);
    }

    public final void instPUSH_IND() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "PUSH  " + IND.name);
        }
        SP.sub(2);
        memory.setWord(SP.r, IND.r);
        addPC_T(2, 14);
    }

    public final void instPOP_r16() {
        Reg16 R1 = r16AF[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "POP   " + R1.name);
        }
        R1.set(memory.getWord(SP.r));
        SP.add(2);
        addPC_T(1, 14);
    }

    public final void instPOP_IND() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "POP   " + IND.name);
        }
        IND.r = memory.getWord(SP.r);
        SP.add(2);
        addPC_T(2, 14);
    }

    public final void instRLCA() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "RLCA");
        }
        rlc_a();
        addPC_T(1, 4);
    }

    public final void instRLA() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "RLA");
        }
        rl_a();
        addPC_T(1, 4);
    }

    private final void rl_a() {
        int ans = A.r;
        boolean c = (ans & 0x80) != 0;
        if (getFlagC()) {
            ans = (ans << 1) | 0x01;
        } else {
            ans <<= 1;
        }
        ans &= 0xff;
        setFlagN(false);
        setFlagH(false);
        setFlagC(c);
        A.r = ans;
    }

    public final void instRLC_r8() {
        Reg8 r1 = r8[opcode];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "RLC   " + r1.name);
        }
        r1.r = rlc(r1.r);
        addPC_T(2, 8);
    }

    private final int rlc(int ans) {
        boolean c = (ans & 0x80) != 0;
        if (c) {
            ans = (ans << 1) | 0x01;
        } else {
            ans <<= 1;
        }
        ans &= 0xff;
        F.r = PSZTable[ans];
        setFlagC(c);
        return (ans);
    }

    public final void instRRC_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "RRC   " + r1.name);
        }
        r1.r = rrc(r1.r);
        addPC_T(2, 8);
    }

    private final int rrc(int ans) {
        boolean c = (ans & 0x01) != 0;
        if (c) {
            ans = (ans >> 1) | 0x80;
        } else {
            ans >>= 1;
        }
        F.r = PSZTable[ans];
        setFlagC(c);
        return (ans);
    }

    public final void instRRA() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "RRA");
        }
        rr_a();
        addPC_T(1, 4);
    }

    public final void instRRCA() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "RRCA");
        }
        rrc_a();
        addPC_T(1, 4);
    }

    public final void instSUB_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "SUB   " + r1.name);
        }
        sub_a(r1.r);
        addPC_T(1, 4);
    }

    public final void instSUB_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "SUB   " + b2hds(n8));
        }
        sub_a(n8);
        addPC_T(2, 7);
    }

    public final void instSUB_iHL() {
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "SUB   (HL)  ; (HL)=" + b2hds(n8));
        }
        sub_a(n8);
        addPC_T(1, 7);
    }

    public final void instCP_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "CP    " + b2hds(n8));
        }
        cp_a(n8);
        addPC_T(2, 7);
    }

    public final void instCP_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "CP    " + r1.name);
        }
        cp_a(r1.r);
        addPC_T(1, 4);
    }

    public final void instCP_iHL() {
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "CP    (HL)	; (HL)=" + b2hds(n8));
        }
        cp_a(n8);
        addPC_T(1, 7);
    }
    // Add group

    public final void instADD_HL_r16() {
        Reg16 R1 = r16[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "ADD   HL," + R1.name);
        }
        HL.set(add16(HL.get(), R1.get()));
        addPC_T(1, 11);
    }

    public final void instADD_IND_r16() {
        Reg16 R1 = r16IND[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "ADD   " + IND.name + "," + R1.name);
        }
        IND.set(add16(IND.get(), R1.get()));
        addPC_T(2, 15);
    }

    public final void instADC_HL_r16() {
        Reg16 R1 = r16[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "ADC   HL," + R1.name);
        }
        HL.set(adc16(HL.get(), R1.get()));
        addPC_T(2, 11);
    }

    public final void instADD_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "ADD   " + r1.name);
        }
        add_a(r1.r);
        addPC_T(1, 4);
    }

    public final void instADD_iHL() {
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "ADD   A,(HL)  ; (HL)=" + b2hds(n8));
        }
        add_a(n8);
        addPC_T(1, 7);
    }

    public final void instADD_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "ADD   A," + b2hds(n8));
        }
        add_a(n8);
        addPC_T(2, 7);
    }

    public final void instDAA() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "DAA   ");
        }
        daa_a();
        addPC_T(1, 4);
    }

    public final void instADC_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "ADC   " + r1.name);
        }
        adc_a(r1.r);
        addPC_T(1, 4);
    }

    public final void instADC_n8() {
        int n8 = memory.getByte(PC.r + 1);
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "ADC   " + b2hds(n8));
        }
        adc_a(n8);
        addPC_T(2, 7);
    }

    public final void instADC_iHL() {
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "ADC   (HL)  ; (HL)=" + b2hds(n8));
        }
        adc_a(n8);
        addPC_T(1, 7);
    }

    public final void instBIT_b_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        int b = (opcode & 0x38) >> 3;
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "BIT   " + Integer.toString(b) + "," + r1.name);
        }
        bit(b, r1.r);
        addPC_T(2, 8);
    }

    public final void instBIT_b_iHL() {
        int b = (opcode & 0x38) >> 3;
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "BIT   " + Integer.toString(b) + ",(HL)  ; (HL)="
                    + b2hds(n8));
        }
        bit(b, n8);
        addPC_T(2, 12);
    }

    public final void instBIT_b_iINDd() {
        int d = memory.getByte(PC.r + 2), b = (opcode & 0x38) >> 3;
        int addr = (IND.r + (byte) d) & 0xffff;
        if (traceCPU >= TRC_ALL) {
            traceCPU(4, "BIT   " + Integer.toString(b) + ",(" + IND.name + "+"
                    + b2hds(d).trim() + ")  ; " + IND.name + "+"
                    + b2hds(d).trim() + " => " + Integer.toHexString(addr));
        }
        bit(b, memory.getByte(addr));
        addPC_T(4, 20);
    }

    public final void instRES_b_iINDd() {
        int d = memory.getByte(PC.r + 2), b = (opcode & 0x38) >> 3, addr = (IND.r + (byte) d) & 0xffff;
        if (traceCPU >= TRC_ALL) {
            traceCPU(4, "RES   " + Integer.toString(b) + ",(" + IND.name + "+"
                    + b2hds(d).trim() + ")  ; " + IND.name + "+"
                    + b2hds(d).trim() + " => " + Integer.toHexString(addr));
        }
        memory.setByte(addr, memory.getByte(addr) & (~(1 << b)));
        addPC_T(4, 20);
    }

    public final void instSET_b_iINDd() {
        int d = memory.getByte(PC.r + 2), b = (opcode & 0x38) >> 3, addr = (IND.r + (byte) d) & 0xffff;
        if (traceCPU >= TRC_ALL) {
            traceCPU(4, "SET   " + Integer.toString(b) + ",(" + IND.name + "+"
                    + b2hds(d).trim() + ")  ; " + IND.name + "+"
                    + b2hds(d).trim() + " => " + Integer.toHexString(addr));
        }
        memory.setByte(addr, memory.getByte(addr) | (1 << b));
        addPC_T(4, 20);
    }

    public final void instSET_b_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        int b = (opcode & 0x38) >> 3;
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "SET   " + Integer.toString(b) + "," + r1.name);
        }
        r1.r = r1.r | (1 << b);
        addPC_T(2, 8);
    }

    public final void instSET_b_iHL() {
        int b = (opcode & 0x38) >> 3;
        int addr = HL.get();
        int n8 = memory.getByte(addr);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "SET   " + Integer.toString(b) + ",(HL) ; (HL)="
                    + b2hds(n8));
        }
        n8 = n8 | (1 << b);
        memory.setByte(addr, n8);
        addPC_T(2, 15);
    }

    public final void instRES_b_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        int b = (opcode & 0x38) >> 3;
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "RES   " + Integer.toString(b) + "," + r1.name);
        }
        r1.r = r1.r & (~(1 << b));
        addPC_T(2, 8);
    }

    public final void instRES_b_iHL() {
        int b = (opcode & 0x38) >> 3;
        int addr = HL.get();
        int n8 = memory.getByte(addr);
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "RES   " + Integer.toString(b) + ",(HL) ; (HL)="
                    + b2hds(n8));
        }
        n8 = n8 & (~(1 << b));
        memory.setByte(addr, n8);
        addPC_T(2, 15);
    }

    public final void instRR_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "RR    " + r1.name);
        }
        r1.r = rr(r1.r);
        addPC_T(2, 8);
    }

    public final void instSRL_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "SRL   " + r1.name);
        }
        r1.r = srl(r1.r);
        addPC_T(2, 8);
    }

    public final void instRL_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "RL    " + r1.name);
        }
        r1.r = rl(r1.r);
        addPC_T(2, 8);
    }

    public final void instSBC_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "SBC   " + r1.name);
        }
        sbc_a(r1.r);
        addPC_T(1, 4);
    }

    public final void instSBC_iHL() {
        int n8 = memory.getByte(HL.get());
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "SBC   (HL)  ; (HL) = " + b2hds(n8));
        }
        sbc_a(n8);
        addPC_T(1, 4);
    }

    public final void instSRA_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "SRA   " + r1.name);
        }
        r1.r = sra(r1.r);
        addPC_T(2, 8);
    }

    public final void instSLA_r8() {
        Reg8 r1 = r8[opcode & 0x07];
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "SLA   " + r1.name);
        }
        r1.r = sla(r1.r);
        addPC_T(2, 8);
    }

    public final void instSBC_HL_r16() {
        Reg16 R1 = r16[(opcode & 0x30) >> 4];
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "SBC   HL," + R1.name);
        }
        HL.set(sbc16(HL.get(), R1.get()));
        addPC_T(2, 15);
    }

    public final void instNEG() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(2, "NEG");
        }
        neg_a();
        addPC_T(2, 8);
    }

    public final void instCPL() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "CPL");
        }
        A.r ^= 0xff; // Invert A
        A.r &= 0xff;
        F.r |= SET_N | SET_H;
        addPC_T(1, 4);
    }

    public final void instSCF() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "SCF");
        }
        F.r |= SET_C; // Set C
        F.r &= ~(SET_H | SET_N); // Reset H,N
        addPC_T(1, 4);
    }

    public final void instCCF() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "CCF");
        }
        setFlagH(getFlagC()); // save C
        F.r ^= SET_C; // Invert C
        F.r &= ~SET_N; // Reset N
        PC.r += 1;
        t += 4;
    }

    public final void instRLD() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "RLD");
        }
        rld_a();
        addPC_T(2, 18);
    }

    public final void instRRD() {
        if (traceCPU >= TRC_ALL) {
            traceCPU(1, "RRD");
        }
        rrd_a();
        addPC_T(2, 18);
    }
    /*
     * Submethods for calculations
     */
    // 16bit Arithmetic

    private final int add16(int a, int b) {
        int lans = a + b;
        int ans = lans & 0xffff;
        setFlagC((lans & 0x10000) != 0);
        setFlagH((((a & 0x0fff) + (b & 0x0fff)) & 0x1000) != 0);
        setFlagN(false);
        return (ans);
    }

    private final int adc16(int a, int b) {
        int c = getFlagC() ? 1 : 0;
        int lans = a + b + c;
        int ans = lans & 0xffff;
        setFlagS((ans & (SET_S << 8)) != 0);
        setFlagZ((ans) == 0);
        setFlagC((lans & 0x10000) != 0);
        setFlagPV(((a ^ ~b) & (a ^ ans) & 0x8000) != 0);
        setFlagH((((a & 0x0fff) + (b & 0x0fff) + c) & 0x1000) != 0);
        setFlagN(false);
        return (ans);
    }

    private final int sbc16(int a, int b) {
        int c = F.r & 0x01;
        int lans = a - b - c;
        int ans = lans & 0xffff;
        setFlagS((ans & (SET_S << 8)) != 0);
        setFlagZ((ans) == 0);
        setFlagC((lans & 0x10000) != 0);
        setFlagPV(((a ^ b) & (a ^ ans) & 0x8000) != 0);
        setFlagH((((a & 0x0fff) - (b & 0x0fff) - c) & 0x1000) != 0);
        setFlagN(true);
        /*
         int I = F.r & SET_C;
         int JW = ( a - b - I) & 0xFFFF;
         F.r = 	SET_N |
         ((((a - b - I) & 0x10000) == 0x10000) ? SET_C : 0) |
         ((((a ^ b) & (a ^ JW) & 0x8000) == 0x8000) ? SET_PV : 0) |
         ((((a ^ b ^ JW) & 0x1000) == 0x1000) ? SET_H : 0) |
         ((JW != 0) ? 0 : SET_Z) |
         ((JW >> 8) & SET_S);
         return( JW);
         */
        return (ans);
    }
    // 8bit Arithmetic

    private final void add_a(int n) {
        int a = A.r;
        int wans = a + n;
        int ans = wans & 0xff;
        F.r = SZTable[ans];
        setFlagC((wans & 0x100) != 0);
        setFlagPV(((a ^ ~n) & (a ^ ans) & 0x80) != 0);
        setFlagH((((a & 0x0f) + (n & 0x0f)) & SET_H) != 0);
        setFlagN(false);
        A.r = ans;
    }

    private final void adc_a(int b) {
        int a = A.r;
        int c = getFlagC() ? 1 : 0;
        int wans = a + b + c;
        int ans = wans & 0xff;
        F.r = SZTable[ans] | ((wans & 0x100) >> 8);
        setFlagPV(((a ^ ~b) & (a ^ ans) & 0x80) != 0);
        setFlagH((((a & 0x0f) + (b & 0x0f) + c) & SET_H) != 0);
        A.r = ans;
    }

    private final void sub_a(int b) {
        int a = A.r;
        int wans = a - b;
        int ans = wans & 0xff;
        /*		F.r = 	SZTable[ans] |				// S, Z
         SET_N | 					// N
         ((wans & 0x100) >> 8) |		// C
         ((((A.r ^ b ) & (A.r ^ ans) & 0x80) == 0x80) ? SET_PV : 0) |
         (A.r ^ b ^ ans) & SET_H;	// H
         */
        F.r = SZTable[ans] | SET_N;
        setFlagC((wans & 0x100) != 0);
        setFlagPV(((a ^ b) & (a ^ ans) & 0x80) != 0);
        setFlagH((((a & 0x0f) - (b & 0x0f)) & SET_H) != 0);
        A.r = ans;
    }

    private final void cp_a(int b) {
        int a = A.r;
        int wans = a - b;
        int ans = wans & 0xff;
        F.r = SZTable[ans] | SET_N;
        setFlagC((wans & 0x100) != 0);
        setFlagH((((a & 0x0f) - (b & 0x0f)) & SET_H) != 0);
        setFlagPV(((a ^ b) & (a ^ ans) & 0x80) != 0);
    }

    private final void and_a(int n) {
        A.r &= n;
        F.r = PSZTable[A.r] | SET_H;
    }

    private final void or_a(int n) {
        A.r |= n;
        F.r = PSZTable[A.r];
    }

    private final void xor_a(int n) {
        A.r = (A.r ^ n) & 0xff;
        F.r = PSZTable[A.r];
    }

    private final void daa_a() {
        int index = A.r;
        if (getFlagC()) {
            index |= 256;
        }
        if (getFlagH()) {
            index |= 512;
        }
        if (getFlagN()) {
            index |= 1024;
        }
        AF.set(DAATable[index]);
    }

    private final int inc8(int ans) {
        boolean pv = (ans == 0x7f);
        boolean h = (((ans & 0x0f) + 1) & SET_H) != 0;
        ans = (ans + 1) & 0xff;
        setFlagS((ans & SET_S) != 0);
        setFlagZ((ans) == 0);
        setFlagPV(pv);
        setFlagH(h);
        setFlagN(false);
        return (ans);
    }

    private final int dec8(int ans) {
        boolean pv = (ans == 0x80);
        boolean h = (((ans & 0x0f) - 1) & SET_H) != 0;
        ans = (ans - 1) & 0xff;
        setFlagS((ans & SET_S) != 0);
        setFlagZ((ans) == 0);
        setFlagPV(pv);
        setFlagH(h);
        setFlagN(true);
        return (ans);
    }
    /*
     * Rotate, Shift group
     */

    private final void rrd_a() {
        int ans = A.r;
        int addr = HL.get();
        int t = memory.getByte(addr);
        int q = t;
        int saveC = F.r & SET_C;
        t = (t >> 4) | (ans << 4);
        ans = (ans & 0xf0) | (q & 0x0f);
        memory.setByte(addr, t);
        F.r = PSZTable[ans] | saveC;
        //		setFlagPV( IFF2);   // komment
        A.r = ans;
    }

    private final void rld_a() {
        int ans = A.r;
        int addr = HL.get();
        int t = memory.getByte(addr);
        int q = t;
        int saveC = F.r & SET_C;
        t = (t << 4) | (ans & 0x0f);
        ans = (ans & 0xf0) | (q >> 4);
        memory.setByte(addr, (t & 0xff));
        F.r = PSZTable[ans] | saveC;
        //		setFlagPV( IFF2);  // komment
        A.r = ans;
    }

    private final int sla(int ans) {
        F.r = ans >> 7;
        ans = (ans << 1) & 0xff;
        F.r |= PSZTable[ans];
        return (ans);
    }

    private final int sra(int ans) {
        F.r = ans & SET_C;
        ans = (ans >> 1) | (ans & 0x80);
        F.r |= PSZTable[ans];
        return (ans);
    }

    private final void sbc_a(int b) {
        int a = A.r;
        int c = F.r & 0x01;
        int wans = a - b - c;
        int ans = wans & 0xff;
        F.r = SZTable[ans] | SET_N;
        setFlagC((wans & 0x100) != 0);
        setFlagPV(((a ^ b) & (a ^ ans) & 0x80) != 0);
        setFlagH((((a & 0x0f) - (b & 0x0f) - c) & SET_H) != 0);
        A.r = ans;
    }

    private final int rl(int ans) {
        int c = ans >> 7;
        ans <<= 1;
        if (getFlagC()) {
            ans |= 0x01;
        }
        ans &= 0xff;
        F.r = PSZTable[ans] | c;
        return (ans);
    }

    private final int rr(int ans) {
        int c = ans & SET_C;
        ans >>= 1;
        if (getFlagC()) {
            ans |= 0x80;
        }
        F.r = PSZTable[ans] | c;
        return (ans);
    }

    private final void rlc_a() {
        int ans = A.r;
        boolean c = (ans & 0x80) != 0;
        ans <<= 1;
        if (c) {
            ans |= 0x01;
        }
        ans &= 0xff;
        setFlagN(false);
        setFlagH(false);
        setFlagC(c);
        A.r = ans;
    }

    private final void rrc_a() {
        int ans = A.r;
        boolean c = (ans & 0x01) != 0;
        ans >>= 1;
        if (c) {
            ans |= 0x80;
        }
        setFlagN(false);
        setFlagH(false);
        setFlagC(c);
        A.r = ans;
    }

    private final void rr_a() {
        int ans = A.r;
        boolean c = (ans & SET_C) != 0;
        ans >>= 1;
        if (getFlagC()) {
            ans |= 0x80;
        }
        setFlagN(false);
        setFlagH(false);
        setFlagC(c);
        A.r = ans;
    }

    private final int srl(int ans) {
        F.r = ans & SET_C;
        ans = ans >> 1;
        F.r |= PSZTable[ans] & (~SET_S); // ??? S
        return (ans);
    }

    private final void neg_a() {
        int t = A.r;
        A.r = 0;
        sub_a(t);
    }

    private final void bit(int b, int n) {
        setFlagZ(((n & (1 << b)) == 0));
        setFlagH(true);
        setFlagN(false);
    }

    /**
     *
     * @param n machine cycles.
     * @param td clock periods.
     */
    public void addPC_T(int n, int td) {
        PC.r = (PC.r + n) & 0xffff;
        t += td;
        lastWaitT += td;
    }

    public void registerTrace() {
        log.write("A =" + b2hds(A.r) + "F =(" + (getFlagS() ? "S=1," : "S=0,")
                + (getFlagZ() ? "Z=1," : "Z=0,")
                + (getFlagH() ? "H=1," : "H=0,")
                + (getFlagPV() ? "P/V=1," : "P/V=0,")
                + (getFlagN() ? "N=1," : "N=0,")
                + (getFlagC() ? "C=1)" : "C=0)") + "=" + b2hds(F.r));
        log.write("B =" + b2hds(B.r) + "C =" + b2hds(C.r) + "BC="
                + w2hds(BC.get()));
        log.write("D =" + b2hds(D.r) + "E =" + b2hds(E.r) + "DE="
                + w2hds(DE.get()));
        log.write("H =" + b2hds(H.r) + "L =" + b2hds(L.r) + "HL="
                + w2hds(HL.get()));
        log.write("IX=" + w2hds(IX.r) + "IY=" + w2hds(IY.r) + "SP="
                + w2hds(SP.r));
    }

    public void traceCPU(int pLen, String pMnemoic) {
        registerTrace();
        log
                .write("------------------------------------------------------------| "
                + toHexString(PC.r, 4)
                + ": "
                + tracePCBytes(pLen)
                + pMnemoic);
    }

    public String w2hds(int pValue) {
        return (toHexString(pValue, 4) + "(" + toDecString(pValue, 5) + ","
                + toDecString((short) pValue, 6) + ")" + "                             ")
                .substring(0, 16);
    }

    public String b2hds(int pValue) {
        return (toHexString(pValue, 2) + "(" + toDecString(pValue, 4) + ","
                + toDecString((byte) (pValue), 4) + ")" + "                             ")
                .substring(0, 16);
    }

    public String toHexString(int pValue, int pLen) {
        String s = "0000000" + Integer.toHexString(pValue);
        return s.substring(s.length() - pLen);
    }

    public String toHexString2(int pValue, int pLen) {
        String s = "0000000" + Integer.toHexString(pValue);
        return s.substring(s.length() - pLen);
    }

    public String toDecString(int pValue, int pLen) {
        return Integer.toString(pValue);
    }

    public final String tracePCBytes(int pLen) {
        String s = "";
        for (int i = 0; i < pLen; i++) {
            s += toHexString2(memory.getByte(PC.r + i), 2) + " ";
        }
        return (s + "            ").substring(0, 12);
    }

    public final void prevPCs() {
        String s = "";
        for (int i = 0; i < 1024; i++) {
            s += toHexString(pPC[(ppi + i) & 0x3ff], 4) + "/";
        }
        log.write("PrevPCs: " + s);
    }

    public final void traceSubroutine() {
        String s = "";
        switch (PC.r) {
            case 0xc363:
                s = "Function call control";
                break;
            case 0xdcfc:
                s = "BASIC initialize";
                break;
            case 0xfe7f:
                s = "Write text (HL=length)";
                break;
            case 0xfe9a:
                s = "Write char (A=Ascii code)";
                break;
            case 0xd9ef:
                s = "BASIC Start";
                break;
            case 0xc9a9:
                s = "VIDEO V0  - Video return";
                break;
            case 0xc98f:
                s = "VIDEO V1  - Video dispatcher";
                break;
            case 0xc9aa:
                s = "VIDEO V2  - Video jump table dispatcher";
                break;
            case 0xc9b3:
                s = "VIDEO V3  - Coordinate transform X";
                break;
            case 0xc9ba:
                s = "VIDEO V4  - Coordinate transform Y";
                break;
            case 0xc9c1:
                s = "VIDEO V5  - HL coordinate transform. HL/2, or 4, or 8, based on Video mode. Mode:";
                break;
            case 0xc9c6:
                s = "VIDEO V51 - HL>>1, if b != 0, B=" + Integer.toString(B.r);
                break;
            case 0xc9cd:
                s = "VIDEO V6  - Display settings and IY.";
                break;
            case 0xc9f2:
                s = "VIDEO INIC- Video mode 4color";
                break;
            case 0xca38:
                s = "VIDEO 12  - PAL-DEF";
                break;
            case 0xca49:
                s = "VIDEO 5 05H - CLS";
                break;
        }
        if (s != "") {
            log.write("SUB ==> " + s);
        }
    }
    /*
     *
     * @author gah
     *
     * Cursor interrupt in every 20ms.
     */

    public class InterruptTimer implements Runnable {

        long deltaT;
        Z80 z80;

        public InterruptTimer(Z80 z80) {
            this.z80 = z80;
        }

        public void setDeltaT(long p) {
            deltaT = p;
        }

        public void run() {
            long nextInt;
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            while (z80.running) {
                //				nextInt = J3DTimer.getValue() + deltaT;
                //				while ( nextInt > J3DTimer.getValue());
                z80.interrupt = true;
                z80.port.port_in[0x59] &= ~0x10;

                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                }
            }
        }
    }
    /**
     * Helper table for Decimal adjust after addition (DAA) instruction.
     */
    public static int DAATable[] = {
        0x0044, 0x0100, 0x0200, 0x0304, 0x0400, 0x0504, 0x0604, 0x0700, 0x0808, 0x090C, 0x1010, 0x1114, 0x1214, 0x1310, 0x1414, 0x1510, 0x1000, 0x1104, 0x1204, 0x1300, 0x1404, 0x1500, 0x1600, 0x1704, 0x180C, 0x1908, 0x2030, 0x2134, 0x2234, 0x2330, 0x2434, 0x2530,
        0x2020, 0x2124, 0x2224, 0x2320, 0x2424, 0x2520, 0x2620, 0x2724, 0x282C, 0x2928, 0x3034, 0x3130, 0x3230, 0x3334, 0x3430, 0x3534, 0x3024, 0x3120, 0x3220, 0x3324, 0x3420, 0x3524, 0x3624, 0x3720, 0x3828, 0x392C, 0x4010, 0x4114, 0x4214, 0x4310, 0x4414, 0x4510,
        0x4000, 0x4104, 0x4204, 0x4300, 0x4404, 0x4500, 0x4600, 0x4704, 0x480C, 0x4908, 0x5014, 0x5110, 0x5210, 0x5314, 0x5410, 0x5514, 0x5004, 0x5100, 0x5200, 0x5304, 0x5400, 0x5504, 0x5604, 0x5700, 0x5808, 0x590C, 0x6034, 0x6130, 0x6230, 0x6334, 0x6430, 0x6534,
        0x6024, 0x6120, 0x6220, 0x6324, 0x6420, 0x6524, 0x6624, 0x6720, 0x6828, 0x692C, 0x7030, 0x7134, 0x7234, 0x7330, 0x7434, 0x7530, 0x7020, 0x7124, 0x7224, 0x7320, 0x7424, 0x7520, 0x7620, 0x7724, 0x782C, 0x7928, 0x8090, 0x8194, 0x8294, 0x8390, 0x8494, 0x8590,
        0x8080, 0x8184, 0x8284, 0x8380, 0x8484, 0x8580, 0x8680, 0x8784, 0x888C, 0x8988, 0x9094, 0x9190, 0x9290, 0x9394, 0x9490, 0x9594, 0x9084, 0x9180, 0x9280, 0x9384, 0x9480, 0x9584, 0x9684, 0x9780, 0x9888, 0x998C, 0x0055, 0x0111, 0x0211, 0x0315, 0x0411, 0x0515,
        0x0045, 0x0101, 0x0201, 0x0305, 0x0401, 0x0505, 0x0605, 0x0701, 0x0809, 0x090D, 0x1011, 0x1115, 0x1215, 0x1311, 0x1415, 0x1511, 0x1001, 0x1105, 0x1205, 0x1301, 0x1405, 0x1501, 0x1601, 0x1705, 0x180D, 0x1909, 0x2031, 0x2135, 0x2235, 0x2331, 0x2435, 0x2531,
        0x2021, 0x2125, 0x2225, 0x2321, 0x2425, 0x2521, 0x2621, 0x2725, 0x282D, 0x2929, 0x3035, 0x3131, 0x3231, 0x3335, 0x3431, 0x3535, 0x3025, 0x3121, 0x3221, 0x3325, 0x3421, 0x3525, 0x3625, 0x3721, 0x3829, 0x392D, 0x4011, 0x4115, 0x4215, 0x4311, 0x4415, 0x4511,
        0x4001, 0x4105, 0x4205, 0x4301, 0x4405, 0x4501, 0x4601, 0x4705, 0x480D, 0x4909, 0x5015, 0x5111, 0x5211, 0x5315, 0x5411, 0x5515, 0x5005, 0x5101, 0x5201, 0x5305, 0x5401, 0x5505, 0x5605, 0x5701, 0x5809, 0x590D, 0x6035, 0x6131, 0x6231, 0x6335, 0x6431, 0x6535,
        0x6025, 0x6121, 0x6221, 0x6325, 0x6421, 0x6525, 0x6625, 0x6721, 0x6829, 0x692D, 0x7031, 0x7135, 0x7235, 0x7331, 0x7435, 0x7531, 0x7021, 0x7125, 0x7225, 0x7321, 0x7425, 0x7521, 0x7621, 0x7725, 0x782D, 0x7929, 0x8091, 0x8195, 0x8295, 0x8391, 0x8495, 0x8591,
        0x8081, 0x8185, 0x8285, 0x8381, 0x8485, 0x8581, 0x8681, 0x8785, 0x888D, 0x8989, 0x9095, 0x9191, 0x9291, 0x9395, 0x9491, 0x9595, 0x9085, 0x9181, 0x9281, 0x9385, 0x9481, 0x9585, 0x9685, 0x9781, 0x9889, 0x998D, 0xA0B5, 0xA1B1, 0xA2B1, 0xA3B5, 0xA4B1, 0xA5B5,
        0xA0A5, 0xA1A1, 0xA2A1, 0xA3A5, 0xA4A1, 0xA5A5, 0xA6A5, 0xA7A1, 0xA8A9, 0xA9AD, 0xB0B1, 0xB1B5, 0xB2B5, 0xB3B1, 0xB4B5, 0xB5B1, 0xB0A1, 0xB1A5, 0xB2A5, 0xB3A1, 0xB4A5, 0xB5A1, 0xB6A1, 0xB7A5, 0xB8AD, 0xB9A9, 0xC095, 0xC191, 0xC291, 0xC395, 0xC491, 0xC595,
        0xC085, 0xC181, 0xC281, 0xC385, 0xC481, 0xC585, 0xC685, 0xC781, 0xC889, 0xC98D, 0xD091, 0xD195, 0xD295, 0xD391, 0xD495, 0xD591, 0xD081, 0xD185, 0xD285, 0xD381, 0xD485, 0xD581, 0xD681, 0xD785, 0xD88D, 0xD989, 0xE0B1, 0xE1B5, 0xE2B5, 0xE3B1, 0xE4B5, 0xE5B1,
        0xE0A1, 0xE1A5, 0xE2A5, 0xE3A1, 0xE4A5, 0xE5A1, 0xE6A1, 0xE7A5, 0xE8AD, 0xE9A9, 0xF0B5, 0xF1B1, 0xF2B1, 0xF3B5, 0xF4B1, 0xF5B5, 0xF0A5, 0xF1A1, 0xF2A1, 0xF3A5, 0xF4A1, 0xF5A5, 0xF6A5, 0xF7A1, 0xF8A9, 0xF9AD, 0x0055, 0x0111, 0x0211, 0x0315, 0x0411, 0x0515,
        0x0045, 0x0101, 0x0201, 0x0305, 0x0401, 0x0505, 0x0605, 0x0701, 0x0809, 0x090D, 0x1011, 0x1115, 0x1215, 0x1311, 0x1415, 0x1511, 0x1001, 0x1105, 0x1205, 0x1301, 0x1405, 0x1501, 0x1601, 0x1705, 0x180D, 0x1909, 0x2031, 0x2135, 0x2235, 0x2331, 0x2435, 0x2531,
        0x2021, 0x2125, 0x2225, 0x2321, 0x2425, 0x2521, 0x2621, 0x2725, 0x282D, 0x2929, 0x3035, 0x3131, 0x3231, 0x3335, 0x3431, 0x3535, 0x3025, 0x3121, 0x3221, 0x3325, 0x3421, 0x3525, 0x3625, 0x3721, 0x3829, 0x392D, 0x4011, 0x4115, 0x4215, 0x4311, 0x4415, 0x4511,
        0x4001, 0x4105, 0x4205, 0x4301, 0x4405, 0x4501, 0x4601, 0x4705, 0x480D, 0x4909, 0x5015, 0x5111, 0x5211, 0x5315, 0x5411, 0x5515, 0x5005, 0x5101, 0x5201, 0x5305, 0x5401, 0x5505, 0x5605, 0x5701, 0x5809, 0x590D, 0x6035, 0x6131, 0x6231, 0x6335, 0x6431, 0x6535,
        0x0604, 0x0700, 0x0808, 0x090C, 0x0A0C, 0x0B08, 0x0C0C, 0x0D08, 0x0E08, 0x0F0C, 0x1010, 0x1114, 0x1214, 0x1310, 0x1414, 0x1510, 0x1600, 0x1704, 0x180C, 0x1908, 0x1A08, 0x1B0C, 0x1C08, 0x1D0C, 0x1E0C, 0x1F08, 0x2030, 0x2134, 0x2234, 0x2330, 0x2434, 0x2530,
        0x2620, 0x2724, 0x282C, 0x2928, 0x2A28, 0x2B2C, 0x2C28, 0x2D2C, 0x2E2C, 0x2F28, 0x3034, 0x3130, 0x3230, 0x3334, 0x3430, 0x3534, 0x3624, 0x3720, 0x3828, 0x392C, 0x3A2C, 0x3B28, 0x3C2C, 0x3D28, 0x3E28, 0x3F2C, 0x4010, 0x4114, 0x4214, 0x4310, 0x4414, 0x4510,
        0x4600, 0x4704, 0x480C, 0x4908, 0x4A08, 0x4B0C, 0x4C08, 0x4D0C, 0x4E0C, 0x4F08, 0x5014, 0x5110, 0x5210, 0x5314, 0x5410, 0x5514, 0x5604, 0x5700, 0x5808, 0x590C, 0x5A0C, 0x5B08, 0x5C0C, 0x5D08, 0x5E08, 0x5F0C, 0x6034, 0x6130, 0x6230, 0x6334, 0x6430, 0x6534,
        0x6624, 0x6720, 0x6828, 0x692C, 0x6A2C, 0x6B28, 0x6C2C, 0x6D28, 0x6E28, 0x6F2C, 0x7030, 0x7134, 0x7234, 0x7330, 0x7434, 0x7530, 0x7620, 0x7724, 0x782C, 0x7928, 0x7A28, 0x7B2C, 0x7C28, 0x7D2C, 0x7E2C, 0x7F28, 0x8090, 0x8194, 0x8294, 0x8390, 0x8494, 0x8590,
        0x8680, 0x8784, 0x888C, 0x8988, 0x8A88, 0x8B8C, 0x8C88, 0x8D8C, 0x8E8C, 0x8F88, 0x9094, 0x9190, 0x9290, 0x9394, 0x9490, 0x9594, 0x9684, 0x9780, 0x9888, 0x998C, 0x9A8C, 0x9B88, 0x9C8C, 0x9D88, 0x9E88, 0x9F8C, 0x0055, 0x0111, 0x0211, 0x0315, 0x0411, 0x0515,
        0x0605, 0x0701, 0x0809, 0x090D, 0x0A0D, 0x0B09, 0x0C0D, 0x0D09, 0x0E09, 0x0F0D, 0x1011, 0x1115, 0x1215, 0x1311, 0x1415, 0x1511, 0x1601, 0x1705, 0x180D, 0x1909, 0x1A09, 0x1B0D, 0x1C09, 0x1D0D, 0x1E0D, 0x1F09, 0x2031, 0x2135, 0x2235, 0x2331, 0x2435, 0x2531,
        0x2621, 0x2725, 0x282D, 0x2929, 0x2A29, 0x2B2D, 0x2C29, 0x2D2D, 0x2E2D, 0x2F29, 0x3035, 0x3131, 0x3231, 0x3335, 0x3431, 0x3535, 0x3625, 0x3721, 0x3829, 0x392D, 0x3A2D, 0x3B29, 0x3C2D, 0x3D29, 0x3E29, 0x3F2D, 0x4011, 0x4115, 0x4215, 0x4311, 0x4415, 0x4511,
        0x4601, 0x4705, 0x480D, 0x4909, 0x4A09, 0x4B0D, 0x4C09, 0x4D0D, 0x4E0D, 0x4F09, 0x5015, 0x5111, 0x5211, 0x5315, 0x5411, 0x5515, 0x5605, 0x5701, 0x5809, 0x590D, 0x5A0D, 0x5B09, 0x5C0D, 0x5D09, 0x5E09, 0x5F0D, 0x6035, 0x6131, 0x6231, 0x6335, 0x6431, 0x6535,
        0x6625, 0x6721, 0x6829, 0x692D, 0x6A2D, 0x6B29, 0x6C2D, 0x6D29, 0x6E29, 0x6F2D, 0x7031, 0x7135, 0x7235, 0x7331, 0x7435, 0x7531, 0x7621, 0x7725, 0x782D, 0x7929, 0x7A29, 0x7B2D, 0x7C29, 0x7D2D, 0x7E2D, 0x7F29, 0x8091, 0x8195, 0x8295, 0x8391, 0x8495, 0x8591,
        0x8681, 0x8785, 0x888D, 0x8989, 0x8A89, 0x8B8D, 0x8C89, 0x8D8D, 0x8E8D, 0x8F89, 0x9095, 0x9191, 0x9291, 0x9395, 0x9491, 0x9595, 0x9685, 0x9781, 0x9889, 0x998D, 0x9A8D, 0x9B89, 0x9C8D, 0x9D89, 0x9E89, 0x9F8D, 0xA0B5, 0xA1B1, 0xA2B1, 0xA3B5, 0xA4B1, 0xA5B5,
        0xA6A5, 0xA7A1, 0xA8A9, 0xA9AD, 0xAAAD, 0xABA9, 0xACAD, 0xADA9, 0xAEA9, 0xAFAD, 0xB0B1, 0xB1B5, 0xB2B5, 0xB3B1, 0xB4B5, 0xB5B1, 0xB6A1, 0xB7A5, 0xB8AD, 0xB9A9, 0xBAA9, 0xBBAD, 0xBCA9, 0xBDAD, 0xBEAD, 0xBFA9, 0xC095, 0xC191, 0xC291, 0xC395, 0xC491, 0xC595,
        0xC685, 0xC781, 0xC889, 0xC98D, 0xCA8D, 0xCB89, 0xCC8D, 0xCD89, 0xCE89, 0xCF8D, 0xD091, 0xD195, 0xD295, 0xD391, 0xD495, 0xD591, 0xD681, 0xD785, 0xD88D, 0xD989, 0xDA89, 0xDB8D, 0xDC89, 0xDD8D, 0xDE8D, 0xDF89, 0xE0B1, 0xE1B5, 0xE2B5, 0xE3B1, 0xE4B5, 0xE5B1,
        0xE6A1, 0xE7A5, 0xE8AD, 0xE9A9, 0xEAA9, 0xEBAD, 0xECA9, 0xEDAD, 0xEEAD, 0xEFA9, 0xF0B5, 0xF1B1, 0xF2B1, 0xF3B5, 0xF4B1, 0xF5B5, 0xF6A5, 0xF7A1, 0xF8A9, 0xF9AD, 0xFAAD, 0xFBA9, 0xFCAD, 0xFDA9, 0xFEA9, 0xFFAD, 0x0055, 0x0111, 0x0211, 0x0315, 0x0411, 0x0515,
        0x0605, 0x0701, 0x0809, 0x090D, 0x0A0D, 0x0B09, 0x0C0D, 0x0D09, 0x0E09, 0x0F0D, 0x1011, 0x1115, 0x1215, 0x1311, 0x1415, 0x1511, 0x1601, 0x1705, 0x180D, 0x1909, 0x1A09, 0x1B0D, 0x1C09, 0x1D0D, 0x1E0D, 0x1F09, 0x2031, 0x2135, 0x2235, 0x2331, 0x2435, 0x2531,
        0x2621, 0x2725, 0x282D, 0x2929, 0x2A29, 0x2B2D, 0x2C29, 0x2D2D, 0x2E2D, 0x2F29, 0x3035, 0x3131, 0x3231, 0x3335, 0x3431, 0x3535, 0x3625, 0x3721, 0x3829, 0x392D, 0x3A2D, 0x3B29, 0x3C2D, 0x3D29, 0x3E29, 0x3F2D, 0x4011, 0x4115, 0x4215, 0x4311, 0x4415, 0x4511,
        0x4601, 0x4705, 0x480D, 0x4909, 0x4A09, 0x4B0D, 0x4C09, 0x4D0D, 0x4E0D, 0x4F09, 0x5015, 0x5111, 0x5211, 0x5315, 0x5411, 0x5515, 0x5605, 0x5701, 0x5809, 0x590D, 0x5A0D, 0x5B09, 0x5C0D, 0x5D09, 0x5E09, 0x5F0D, 0x6035, 0x6131, 0x6231, 0x6335, 0x6431, 0x6535,
        0x0046, 0x0102, 0x0202, 0x0306, 0x0402, 0x0506, 0x0606, 0x0702, 0x080A, 0x090E, 0x0402, 0x0506, 0x0606, 0x0702, 0x080A, 0x090E, 0x1002, 0x1106, 0x1206, 0x1302, 0x1406, 0x1502, 0x1602, 0x1706, 0x180E, 0x190A, 0x1406, 0x1502, 0x1602, 0x1706, 0x180E, 0x190A,
        0x2022, 0x2126, 0x2226, 0x2322, 0x2426, 0x2522, 0x2622, 0x2726, 0x282E, 0x292A, 0x2426, 0x2522, 0x2622, 0x2726, 0x282E, 0x292A, 0x3026, 0x3122, 0x3222, 0x3326, 0x3422, 0x3526, 0x3626, 0x3722, 0x382A, 0x392E, 0x3422, 0x3526, 0x3626, 0x3722, 0x382A, 0x392E,
        0x4002, 0x4106, 0x4206, 0x4302, 0x4406, 0x4502, 0x4602, 0x4706, 0x480E, 0x490A, 0x4406, 0x4502, 0x4602, 0x4706, 0x480E, 0x490A, 0x5006, 0x5102, 0x5202, 0x5306, 0x5402, 0x5506, 0x5606, 0x5702, 0x580A, 0x590E, 0x5402, 0x5506, 0x5606, 0x5702, 0x580A, 0x590E,
        0x6026, 0x6122, 0x6222, 0x6326, 0x6422, 0x6526, 0x6626, 0x6722, 0x682A, 0x692E, 0x6422, 0x6526, 0x6626, 0x6722, 0x682A, 0x692E, 0x7022, 0x7126, 0x7226, 0x7322, 0x7426, 0x7522, 0x7622, 0x7726, 0x782E, 0x792A, 0x7426, 0x7522, 0x7622, 0x7726, 0x782E, 0x792A,
        0x8082, 0x8186, 0x8286, 0x8382, 0x8486, 0x8582, 0x8682, 0x8786, 0x888E, 0x898A, 0x8486, 0x8582, 0x8682, 0x8786, 0x888E, 0x898A, 0x9086, 0x9182, 0x9282, 0x9386, 0x9482, 0x9586, 0x9686, 0x9782, 0x988A, 0x998E, 0x3423, 0x3527, 0x3627, 0x3723, 0x382B, 0x392F,
        0x4003, 0x4107, 0x4207, 0x4303, 0x4407, 0x4503, 0x4603, 0x4707, 0x480F, 0x490B, 0x4407, 0x4503, 0x4603, 0x4707, 0x480F, 0x490B, 0x5007, 0x5103, 0x5203, 0x5307, 0x5403, 0x5507, 0x5607, 0x5703, 0x580B, 0x590F, 0x5403, 0x5507, 0x5607, 0x5703, 0x580B, 0x590F,
        0x6027, 0x6123, 0x6223, 0x6327, 0x6423, 0x6527, 0x6627, 0x6723, 0x682B, 0x692F, 0x6423, 0x6527, 0x6627, 0x6723, 0x682B, 0x692F, 0x7023, 0x7127, 0x7227, 0x7323, 0x7427, 0x7523, 0x7623, 0x7727, 0x782F, 0x792B, 0x7427, 0x7523, 0x7623, 0x7727, 0x782F, 0x792B,
        0x8083, 0x8187, 0x8287, 0x8383, 0x8487, 0x8583, 0x8683, 0x8787, 0x888F, 0x898B, 0x8487, 0x8583, 0x8683, 0x8787, 0x888F, 0x898B, 0x9087, 0x9183, 0x9283, 0x9387, 0x9483, 0x9587, 0x9687, 0x9783, 0x988B, 0x998F, 0x9483, 0x9587, 0x9687, 0x9783, 0x988B, 0x998F,
        0xA0A7, 0xA1A3, 0xA2A3, 0xA3A7, 0xA4A3, 0xA5A7, 0xA6A7, 0xA7A3, 0xA8AB, 0xA9AF, 0xA4A3, 0xA5A7, 0xA6A7, 0xA7A3, 0xA8AB, 0xA9AF, 0xB0A3, 0xB1A7, 0xB2A7, 0xB3A3, 0xB4A7, 0xB5A3, 0xB6A3, 0xB7A7, 0xB8AF, 0xB9AB, 0xB4A7, 0xB5A3, 0xB6A3, 0xB7A7, 0xB8AF, 0xB9AB,
        0xC087, 0xC183, 0xC283, 0xC387, 0xC483, 0xC587, 0xC687, 0xC783, 0xC88B, 0xC98F, 0xC483, 0xC587, 0xC687, 0xC783, 0xC88B, 0xC98F, 0xD083, 0xD187, 0xD287, 0xD383, 0xD487, 0xD583, 0xD683, 0xD787, 0xD88F, 0xD98B, 0xD487, 0xD583, 0xD683, 0xD787, 0xD88F, 0xD98B,
        0xE0A3, 0xE1A7, 0xE2A7, 0xE3A3, 0xE4A7, 0xE5A3, 0xE6A3, 0xE7A7, 0xE8AF, 0xE9AB, 0xE4A7, 0xE5A3, 0xE6A3, 0xE7A7, 0xE8AF, 0xE9AB, 0xF0A7, 0xF1A3, 0xF2A3, 0xF3A7, 0xF4A3, 0xF5A7, 0xF6A7, 0xF7A3, 0xF8AB, 0xF9AF, 0xF4A3, 0xF5A7, 0xF6A7, 0xF7A3, 0xF8AB, 0xF9AF,
        0x0047, 0x0103, 0x0203, 0x0307, 0x0403, 0x0507, 0x0607, 0x0703, 0x080B, 0x090F, 0x0403, 0x0507, 0x0607, 0x0703, 0x080B, 0x090F, 0x1003, 0x1107, 0x1207, 0x1303, 0x1407, 0x1503, 0x1603, 0x1707, 0x180F, 0x190B, 0x1407, 0x1503, 0x1603, 0x1707, 0x180F, 0x190B,
        0x2023, 0x2127, 0x2227, 0x2323, 0x2427, 0x2523, 0x2623, 0x2727, 0x282F, 0x292B, 0x2427, 0x2523, 0x2623, 0x2727, 0x282F, 0x292B, 0x3027, 0x3123, 0x3223, 0x3327, 0x3423, 0x3527, 0x3627, 0x3723, 0x382B, 0x392F, 0x3423, 0x3527, 0x3627, 0x3723, 0x382B, 0x392F,
        0x4003, 0x4107, 0x4207, 0x4303, 0x4407, 0x4503, 0x4603, 0x4707, 0x480F, 0x490B, 0x4407, 0x4503, 0x4603, 0x4707, 0x480F, 0x490B, 0x5007, 0x5103, 0x5203, 0x5307, 0x5403, 0x5507, 0x5607, 0x5703, 0x580B, 0x590F, 0x5403, 0x5507, 0x5607, 0x5703, 0x580B, 0x590F,
        0x6027, 0x6123, 0x6223, 0x6327, 0x6423, 0x6527, 0x6627, 0x6723, 0x682B, 0x692F, 0x6423, 0x6527, 0x6627, 0x6723, 0x682B, 0x692F, 0x7023, 0x7127, 0x7227, 0x7323, 0x7427, 0x7523, 0x7623, 0x7727, 0x782F, 0x792B, 0x7427, 0x7523, 0x7623, 0x7727, 0x782F, 0x792B,
        0x8083, 0x8187, 0x8287, 0x8383, 0x8487, 0x8583, 0x8683, 0x8787, 0x888F, 0x898B, 0x8487, 0x8583, 0x8683, 0x8787, 0x888F, 0x898B, 0x9087, 0x9183, 0x9283, 0x9387, 0x9483, 0x9587, 0x9687, 0x9783, 0x988B, 0x998F, 0x9483, 0x9587, 0x9687, 0x9783, 0x988B, 0x998F,
        0xFABE, 0xFBBA, 0xFCBE, 0xFDBA, 0xFEBA, 0xFFBE, 0x0046, 0x0102, 0x0202, 0x0306, 0x0402, 0x0506, 0x0606, 0x0702, 0x080A, 0x090E, 0x0A1E, 0x0B1A, 0x0C1E, 0x0D1A, 0x0E1A, 0x0F1E, 0x1002, 0x1106, 0x1206, 0x1302, 0x1406, 0x1502, 0x1602, 0x1706, 0x180E, 0x190A,
        0x1A1A, 0x1B1E, 0x1C1A, 0x1D1E, 0x1E1E, 0x1F1A, 0x2022, 0x2126, 0x2226, 0x2322, 0x2426, 0x2522, 0x2622, 0x2726, 0x282E, 0x292A, 0x2A3A, 0x2B3E, 0x2C3A, 0x2D3E, 0x2E3E, 0x2F3A, 0x3026, 0x3122, 0x3222, 0x3326, 0x3422, 0x3526, 0x3626, 0x3722, 0x382A, 0x392E,
        0x3A3E, 0x3B3A, 0x3C3E, 0x3D3A, 0x3E3A, 0x3F3E, 0x4002, 0x4106, 0x4206, 0x4302, 0x4406, 0x4502, 0x4602, 0x4706, 0x480E, 0x490A, 0x4A1A, 0x4B1E, 0x4C1A, 0x4D1E, 0x4E1E, 0x4F1A, 0x5006, 0x5102, 0x5202, 0x5306, 0x5402, 0x5506, 0x5606, 0x5702, 0x580A, 0x590E,
        0x5A1E, 0x5B1A, 0x5C1E, 0x5D1A, 0x5E1A, 0x5F1E, 0x6026, 0x6122, 0x6222, 0x6326, 0x6422, 0x6526, 0x6626, 0x6722, 0x682A, 0x692E, 0x6A3E, 0x6B3A, 0x6C3E, 0x6D3A, 0x6E3A, 0x6F3E, 0x7022, 0x7126, 0x7226, 0x7322, 0x7426, 0x7522, 0x7622, 0x7726, 0x782E, 0x792A,
        0x7A3A, 0x7B3E, 0x7C3A, 0x7D3E, 0x7E3E, 0x7F3A, 0x8082, 0x8186, 0x8286, 0x8382, 0x8486, 0x8582, 0x8682, 0x8786, 0x888E, 0x898A, 0x8A9A, 0x8B9E, 0x8C9A, 0x8D9E, 0x8E9E, 0x8F9A, 0x9086, 0x9182, 0x9282, 0x9386, 0x3423, 0x3527, 0x3627, 0x3723, 0x382B, 0x392F,
        0x3A3F, 0x3B3B, 0x3C3F, 0x3D3B, 0x3E3B, 0x3F3F, 0x4003, 0x4107, 0x4207, 0x4303, 0x4407, 0x4503, 0x4603, 0x4707, 0x480F, 0x490B, 0x4A1B, 0x4B1F, 0x4C1B, 0x4D1F, 0x4E1F, 0x4F1B, 0x5007, 0x5103, 0x5203, 0x5307, 0x5403, 0x5507, 0x5607, 0x5703, 0x580B, 0x590F,
        0x5A1F, 0x5B1B, 0x5C1F, 0x5D1B, 0x5E1B, 0x5F1F, 0x6027, 0x6123, 0x6223, 0x6327, 0x6423, 0x6527, 0x6627, 0x6723, 0x682B, 0x692F, 0x6A3F, 0x6B3B, 0x6C3F, 0x6D3B, 0x6E3B, 0x6F3F, 0x7023, 0x7127, 0x7227, 0x7323, 0x7427, 0x7523, 0x7623, 0x7727, 0x782F, 0x792B,
        0x7A3B, 0x7B3F, 0x7C3B, 0x7D3F, 0x7E3F, 0x7F3B, 0x8083, 0x8187, 0x8287, 0x8383, 0x8487, 0x8583, 0x8683, 0x8787, 0x888F, 0x898B, 0x8A9B, 0x8B9F, 0x8C9B, 0x8D9F, 0x8E9F, 0x8F9B, 0x9087, 0x9183, 0x9283, 0x9387, 0x9483, 0x9587, 0x9687, 0x9783, 0x988B, 0x998F,
        0x9A9F, 0x9B9B, 0x9C9F, 0x9D9B, 0x9E9B, 0x9F9F, 0xA0A7, 0xA1A3, 0xA2A3, 0xA3A7, 0xA4A3, 0xA5A7, 0xA6A7, 0xA7A3, 0xA8AB, 0xA9AF, 0xAABF, 0xABBB, 0xACBF, 0xADBB, 0xAEBB, 0xAFBF, 0xB0A3, 0xB1A7, 0xB2A7, 0xB3A3, 0xB4A7, 0xB5A3, 0xB6A3, 0xB7A7, 0xB8AF, 0xB9AB,
        0xBABB, 0xBBBF, 0xBCBB, 0xBDBF, 0xBEBF, 0xBFBB, 0xC087, 0xC183, 0xC283, 0xC387, 0xC483, 0xC587, 0xC687, 0xC783, 0xC88B, 0xC98F, 0xCA9F, 0xCB9B, 0xCC9F, 0xCD9B, 0xCE9B, 0xCF9F, 0xD083, 0xD187, 0xD287, 0xD383, 0xD487, 0xD583, 0xD683, 0xD787, 0xD88F, 0xD98B,
        0xDA9B, 0xDB9F, 0xDC9B, 0xDD9F, 0xDE9F, 0xDF9B, 0xE0A3, 0xE1A7, 0xE2A7, 0xE3A3, 0xE4A7, 0xE5A3, 0xE6A3, 0xE7A7, 0xE8AF, 0xE9AB, 0xEABB, 0xEBBF, 0xECBB, 0xEDBF, 0xEEBF, 0xEFBB, 0xF0A7, 0xF1A3, 0xF2A3, 0xF3A7, 0xF4A3, 0xF5A7, 0xF6A7, 0xF7A3, 0xF8AB, 0xF9AF,
        0xFABF, 0xFBBB, 0xFCBF, 0xFDBB, 0xFEBB, 0xFFBF, 0x0047, 0x0103, 0x0203, 0x0307, 0x0403, 0x0507, 0x0607, 0x0703, 0x080B, 0x090F, 0x0A1F, 0x0B1B, 0x0C1F, 0x0D1B, 0x0E1B, 0x0F1F, 0x1003, 0x1107, 0x1207, 0x1303, 0x1407, 0x1503, 0x1603, 0x1707, 0x180F, 0x190B,
        0x1A1B, 0x1B1F, 0x1C1B, 0x1D1F, 0x1E1F, 0x1F1B, 0x2023, 0x2127, 0x2227, 0x2323, 0x2427, 0x2523, 0x2623, 0x2727, 0x282F, 0x292B, 0x2A3B, 0x2B3F, 0x2C3B, 0x2D3F, 0x2E3F, 0x2F3B, 0x3027, 0x3123, 0x3223, 0x3327, 0x3423, 0x3527, 0x3627, 0x3723, 0x382B, 0x392F,
        0x3A3F, 0x3B3B, 0x3C3F, 0x3D3B, 0x3E3B, 0x3F3F, 0x4003, 0x4107, 0x4207, 0x4303, 0x4407, 0x4503, 0x4603, 0x4707, 0x480F, 0x490B, 0x4A1B, 0x4B1F, 0x4C1B, 0x4D1F, 0x4E1F, 0x4F1B, 0x5007, 0x5103, 0x5203, 0x5307, 0x5403, 0x5507, 0x5607, 0x5703, 0x580B, 0x590F,
        0x5A1F, 0x5B1B, 0x5C1F, 0x5D1B, 0x5E1B, 0x5F1F, 0x6027, 0x6123, 0x6223, 0x6327, 0x6423, 0x6527, 0x6627, 0x6723, 0x682B, 0x692F, 0x6A3F, 0x6B3B, 0x6C3F, 0x6D3B, 0x6E3B, 0x6F3F, 0x7023, 0x7127, 0x7227, 0x7323, 0x7427, 0x7523, 0x7623, 0x7727, 0x782F, 0x792B,
        0x7A3B, 0x7B3F, 0x7C3B, 0x7D3F, 0x7E3F, 0x7F3B, 0x8083, 0x8187, 0x8287, 0x8383, 0x8487, 0x8583, 0x8683, 0x8787, 0x888F, 0x898B, 0x8A9B, 0x8B9F, 0x8C9B, 0x8D9F, 0x8E9F, 0x8F9B, 0x9087, 0x9183, 0x9283, 0x9387, 0x9483, 0x9587, 0x9687, 0x9783, 0x988B, 0x998F
    };
}