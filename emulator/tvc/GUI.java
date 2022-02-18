package emulator.tvc;

/*
 * Created on Jul 24, 2003
 *
 */
/**
 * @author gah
 *
 */
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class GUI {

    public static final String FRAME_TITLE = "Videoton TV-Computer Emulator";
    public static final String ACTIONCOMMAND_TRACE = "ACTIONCOMMAND_TRACE";
    public static final String ACTIONCOMMAND_PAUSE = "ACTIONCOMMAND_PAUSE";
    public static final String ACTIONCOMMAND_SAVE_BIN = "ACTIONCOMMAND_SAVE_BIN";
    public static final String ACTIONCOMMAND_LOAD_BIN = "ACTIONCOMMAND_LOAD_BIN";
    public static final String ACTIONCOMMAND_LOAD_CAS = "ACTIONCOMMAND_LOAD_CAS";
    public static final String ACTIONCOMMAND_OPEN_SD0 = "ACTIONCOMMAND_OPEN_SD0";
    public static final String ACTIONCOMMAND_OPEN_SD1 = "ACTIONCOMMAND_OPEN_SD1";
    public static final String ACTIONCOMMAND_RESET_COLD = "ACTIONCOMMAND_RESET_COLD";
    public static final String ACTIONCOMMAND_RESET_WARM = "ACTIONCOMMAND_RESET_WARM";
    public static final String ACTIONCOMMAND_EXIT = "ACTIONCOMMAND_EXIT";
    public static final String ACTIONCOMMAND_SCALE_FAST = "ACTIONCOMMAND_SCALE_FAST";
    public static final String ACTIONCOMMAND_SCALE_SMOOTH = "ACTIONCOMMAND_SCALE_SMOOTH";
    public static final String ACTIONCOMMAND_COLORKILL_ON = "ACTIONCOMMAND_COLORKILL_ON";
    public static final String ACTIONCOMMAND_COLORKILL_OFF = "ACTIONCOMMAND_COLORKILL_OFF";
    public static final String ACTIONCOMMAND_SCREENSIZE_05X = "ACTIONCOMMAND_SCREENSIZE_05X";
    public static final String ACTIONCOMMAND_SCREENSIZE_1X = "ACTIONCOMMAND_SCREENSIZE_1X";
    public static final String ACTIONCOMMAND_SCREENSIZE_2X = "ACTIONCOMMAND_SCREENSIZE_2X";
    public static final String ACTIONCOMMAND_FULLSCREEN = "ACTIONCOMMAND_FULLSCREEN";
    public static final String ACTIONCOMMAND_TRACE_ENABLE = "ACTIONCOMMAND_TRACE_ENABLE";
    public static final String ACTIONCOMMAND_ABOUT = "ACTIONCOMMAND_ABOUT";
    public JFrame mainFrame, fullFrame;
    public Button start, stop, trace;
    public BorderLayout layout;
    public GridBagConstraints c;
    public TVC tvc;
    public Z80 z80;
    public Screen screen;
    public Memory mem;
    private MenuController menuController;
    private JMenuBar menuBar;
    private JMenuItem itemEnableButtons;
    private JPanel footer;
    private JToggleButton pauseButton;
    private JToggleButton traceButton;
    private boolean FullScreen;

    public GUI() {
        tvc = new TVC(this);
        z80 = tvc.getZ80();
        screen = tvc.getScreen();
        initialize();
        tvc.start();
    }

    private void initialize() {
        menuController = new MenuController(this);
        // Menubar
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem itemSaveBin = new JMenuItem("Save Binary");
        itemSaveBin.setActionCommand(ACTIONCOMMAND_SAVE_BIN);
        itemSaveBin.addActionListener(menuController);
        JMenuItem itemLoadBin = new JMenuItem("Load Binary");
        itemLoadBin.setActionCommand(ACTIONCOMMAND_LOAD_BIN);
        itemLoadBin.addActionListener(menuController);
        fileMenu.add(itemSaveBin);
        fileMenu.add(itemLoadBin);
        fileMenu.addSeparator();
        JMenuItem itemLoadCas = new JMenuItem("Load CAS");
        itemLoadCas.setActionCommand(ACTIONCOMMAND_LOAD_CAS);
        itemLoadCas.addActionListener(menuController);
        fileMenu.add(itemLoadCas);
        fileMenu.addSeparator();
        JMenuItem itemOpenSD0 = new JMenuItem("Open SD image 0");
        itemOpenSD0.setActionCommand(ACTIONCOMMAND_OPEN_SD0);
        itemOpenSD0.addActionListener(menuController);
        fileMenu.add(itemOpenSD0);
        JMenuItem itemOpenSD1 = new JMenuItem("Open SD image 1");
        itemOpenSD1.setActionCommand(ACTIONCOMMAND_OPEN_SD1);
        itemOpenSD1.addActionListener(menuController);
        fileMenu.add(itemOpenSD1);
        fileMenu.addSeparator();
        JMenuItem itemExit = new JMenuItem("Exit");
        itemExit.setActionCommand(ACTIONCOMMAND_EXIT);
        itemExit.addActionListener(menuController);
        fileMenu.add(itemExit);

        JMenu machineMenu = new JMenu("Machine");
        JMenuItem itemColdReset = new JMenuItem("Cold Reset");
        itemColdReset.setActionCommand(ACTIONCOMMAND_RESET_COLD);
        itemColdReset.addActionListener(menuController);
        JMenuItem itemWarmReset = new JMenuItem("Warm Reset");
        itemWarmReset.setActionCommand(ACTIONCOMMAND_RESET_WARM);
        itemWarmReset.addActionListener(menuController);
        machineMenu.add(itemColdReset);
        machineMenu.add(itemWarmReset);

        JMenu screenMenu = new JMenu("Screen");
        JMenuItem itemScaleFast = new JMenuItem("Scaling:Fast");
        itemScaleFast.setActionCommand(ACTIONCOMMAND_SCALE_FAST);
        itemScaleFast.addActionListener(menuController);
        JMenuItem itemScaleSmooth = new JMenuItem("Scaling:Smooth");
        itemScaleSmooth.setActionCommand(ACTIONCOMMAND_SCALE_SMOOTH);
        itemScaleSmooth.addActionListener(menuController);
        screenMenu.add(itemScaleFast);
        screenMenu.add(itemScaleSmooth);
        screenMenu.addSeparator();
        JMenuItem itemColorKillOn = new JMenuItem("Color Off");
        itemColorKillOn.setActionCommand(ACTIONCOMMAND_COLORKILL_ON);
        itemColorKillOn.addActionListener(menuController);
        JMenuItem itemColorKillOff = new JMenuItem("Color On");
        itemColorKillOff.setActionCommand(ACTIONCOMMAND_COLORKILL_OFF);
        itemColorKillOff.addActionListener(menuController);
        screenMenu.add(itemColorKillOn);
        screenMenu.add(itemColorKillOff);
        screenMenu.addSeparator();
        JMenuItem itemSize05x = new JMenuItem("Size 0.5");
        itemSize05x.setActionCommand(ACTIONCOMMAND_SCREENSIZE_05X);
        itemSize05x.addActionListener(menuController);
        JMenuItem itemSize1x = new JMenuItem("Size x1");
        itemSize1x.setActionCommand(ACTIONCOMMAND_SCREENSIZE_1X);
        itemSize1x.addActionListener(menuController);
        JMenuItem itemSize2x = new JMenuItem("Size x2");
        itemSize2x.setActionCommand(ACTIONCOMMAND_SCREENSIZE_2X);
        itemSize2x.addActionListener(menuController);
        screenMenu.add(itemSize05x);
        screenMenu.add(itemSize1x);
        screenMenu.add(itemSize2x);
        screenMenu.addSeparator();
        JMenuItem itemFullScr = new JMenuItem("Full screen");
        itemFullScr.setActionCommand(ACTIONCOMMAND_FULLSCREEN);
        itemFullScr.addActionListener(menuController);
        screenMenu.add(itemFullScr);

        JMenu traceMenu = new JMenu("Trace");
        itemEnableButtons = new JMenuItem("Enable buttons");
        traceMenu.add(itemEnableButtons);
        itemEnableButtons.setActionCommand(ACTIONCOMMAND_TRACE_ENABLE);
        itemEnableButtons.addActionListener(menuController);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem itemAbout = new JMenuItem("About");
        helpMenu.add(itemAbout);
        itemAbout.setActionCommand(ACTIONCOMMAND_ABOUT);
        itemAbout.addActionListener(menuController);

        menuBar.add(fileMenu);
        menuBar.add(screenMenu);
        menuBar.add(machineMenu);
        menuBar.add(traceMenu);
        menuBar.add(helpMenu);

        footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pauseButton = new JToggleButton("Pause");
        pauseButton.addItemListener(menuController);
        pauseButton.setActionCommand(ACTIONCOMMAND_PAUSE);
        footer.add(pauseButton);
        traceButton = new JToggleButton("Trace");
        traceButton.addItemListener(menuController);
        traceButton.setActionCommand(ACTIONCOMMAND_TRACE);
        footer.add(traceButton);
        // Disable buttons
        pauseButton.setEnabled(false);
        traceButton.setEnabled(false);

        // Frame
        mainFrame = new JFrame();
        layout = new BorderLayout();
        mainFrame.setJMenuBar(menuBar);
        mainFrame.setMinimumSize(new Dimension(320, 300));

        mainFrame.addComponentListener(tvc.getScreen());
        mainFrame.addWindowFocusListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                screen.requestFocusInWindow();
            }
        });


        mainFrame.setLayout(layout);
        mainFrame.setTitle(FRAME_TITLE);
        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        mainFrame.add(screen, BorderLayout.CENTER);
        screen.requestFocusInWindow();
        mainFrame.add(footer, BorderLayout.SOUTH);
        mainFrame.pack();
        mainFrame.setSize(mainFrame.getPreferredSize());
        mainFrame.setVisible(true);
        FullScreen = false;
    }

    public void TraceButtonsEnable() {
        boolean enable = !pauseButton.isEnabled();
        pauseButton.setEnabled(enable);
        traceButton.setEnabled(enable);
        itemEnableButtons.setText(enable? "Disable buttons": "Enable buttons");
    }

    public void SwitchFullScreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];

        if (!FullScreen) {
            if (!device.isFullScreenSupported()) {
                Log.getInstance().write("Full screen mode not supported");
            } else {
                mainFrame.setVisible(false);
                mainFrame.remove(screen);
                fullFrame = new JFrame();
                fullFrame.setUndecorated(true);
                fullFrame.add(screen, BorderLayout.CENTER);
                fullFrame.setTitle(FRAME_TITLE);
                device.setFullScreenWindow(fullFrame);
                HideMouseCursor(fullFrame);
                FullScreen = true;
            }
        } else {
            device.setFullScreenWindow(null);
            fullFrame.remove(screen);
            fullFrame.dispose();
            mainFrame.add(screen, BorderLayout.CENTER);
            mainFrame.setVisible(true);
            FullScreen = false;
        }
    }

    private void HideMouseCursor(JFrame frame) {
        // Transparent 1 x 1 pixel cursor image.
        BufferedImage cursorImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        // Create a new blank cursor.
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
            cursorImg, new Point(0, 0), "blank cursor");

        // Set the blank cursor to the JFrame.
        frame.getContentPane().setCursor(blankCursor);
    }
}
