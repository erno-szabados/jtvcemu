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
import java.awt.Image;
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
    public static final String ACTIONCOMMAND_RESET_COLD = "ACTIONCOMMAND_RESET_COLD";
    public static final String ACTIONCOMMAND_RESET_WARM = "ACTIONCOMMAND_RESET_WARM";
    public static final String ACTIONCOMMAND_EXIT = "ACTIONCOMMAND_EXIT";
    public static final String ACTIONCOMMAND_SCALE_FAST = "ACTIONCOMMAND_SCALE_FAST";
    public static final String ACTIONCOMMAND_SCALE_SMOOTH = "ACTIONCOMMAND_SCALE_SMOOTH";
    public static final String ACTIONCOMMAND_COLORKILL_ON = "ACTIONCOMMAND_COLORKILL_ON";
    public static final String ACTIONCOMMAND_COLORKILL_OFF = "ACTIONCOMMAND_COLORKILL_OFF";
    public static final String ACTIONCOMMAND_ABOUT = "ACTIONCOMMAND_ABOUT";
    public JFrame mainFrame;
    public Button start, stop, trace;
    public BorderLayout layout;
    public GridBagConstraints c;
    public Image img;
    TVC tvc;
    Z80 z80;
    Screen screen;
    Memory mem;
    MenuController menuController;
    JMenuBar menuBar;
    JPanel footer;

    public GUI() {
        tvc = TVC.getInstance();
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
        //itemLoadCas.setEnabled(false);
        fileMenu.add(itemLoadCas);
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

        JMenu helpMenu = new JMenu("Help");
        JMenuItem itemAbout = new JMenuItem("About");
        helpMenu.add(itemAbout);
        itemAbout.setActionCommand(ACTIONCOMMAND_ABOUT);
        itemAbout.addActionListener(menuController);

        menuBar.add(fileMenu);
        menuBar.add(screenMenu);
        menuBar.add(machineMenu);
        menuBar.add(helpMenu);

        footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JToggleButton pauseButton = new JToggleButton("Pause");
        pauseButton.addItemListener(menuController);
        pauseButton.setActionCommand(ACTIONCOMMAND_PAUSE);
        footer.add(pauseButton);
        JToggleButton traceButton = new JToggleButton("Trace");
        traceButton.addItemListener(menuController);
        traceButton.setActionCommand(ACTIONCOMMAND_TRACE);
        footer.add(traceButton);

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
    }
}
