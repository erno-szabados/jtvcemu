/**
 *
 */
package emulator.tvc;

import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.MaskFormatter;

/**
 *
 * @author szabados
 */
public class MenuController implements ActionListener, ItemListener {

    public static final String MEMSAVE_DEFAULT = "save.jtvc";
    // Preferences keys for File IO
    public static final String CAS_DIR_KEY = "cas_file_path";
    public static final String MEM_DIR_KEY = "mem_file_path";
    private Preferences prefs;
    private GUI gui;
    private Log log;

    public MenuController(GUI gui) {
        this.gui = gui;
        log = Log.getInstance();
        prefs = Preferences.userRoot();
    }

    public void actionPerformed(ActionEvent e) {
        log.write(e.getActionCommand());
        if (GUI.ACTIONCOMMAND_EXIT.equals(e.getActionCommand())) {
            gui.tvc.shutdown();
        } else if (GUI.ACTIONCOMMAND_SCALE_FAST.equals(e.getActionCommand())) {
            gui.screen.setScalingQuality(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        } else if (GUI.ACTIONCOMMAND_SCALE_SMOOTH.equals(e.getActionCommand())) {
            gui.screen.setScalingQuality(RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        } else if (GUI.ACTIONCOMMAND_COLORKILL_ON.equals(e.getActionCommand())) {
            gui.screen.setColorKillEnable(1);
        } else if (GUI.ACTIONCOMMAND_COLORKILL_OFF.equals(e.getActionCommand())) {
            gui.screen.setColorKillEnable(0);
        } else if (GUI.ACTIONCOMMAND_SAVE_BIN.equals(e.getActionCommand())) {
            actionCommandSaveBinary();
        } else if (GUI.ACTIONCOMMAND_LOAD_BIN.equals(e.getActionCommand())) {
            actionCommandLoadBinary();
        } else if (GUI.ACTIONCOMMAND_LOAD_CAS.equals(e.getActionCommand())) {
            actionCommandLoadCas();
        } else if (GUI.ACTIONCOMMAND_RESET_COLD.equals(e.getActionCommand())) {
            gui.tvc.reset(true);
        } else if (GUI.ACTIONCOMMAND_RESET_WARM.equals(e.getActionCommand())) {
            gui.tvc.reset(false);
        } else if (GUI.ACTIONCOMMAND_ABOUT.equals(e.getActionCommand())) {
            actionCommandAbout();
        }
    }

    private void actionCommandAbout() {
        JTextPane aboutPane = new JTextPane();
        aboutPane.setContentType("text/html");
        aboutPane.setText("<html>Java TVC Emulator<br/>Version 0.1.1<br/>&copy;2003-2005 Hoffer G&aacute;bor<br/>&copy;2013 Szabados Ern&ouml;<br/></html>");
        JOptionPane.showMessageDialog(null, aboutPane, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Creates a dialog to gather page:offset:size data and saves data to file.
     */
    private void actionCommandSaveBinary() {

        log.write("actionCommandSaveBinary");
        try {
            gui.tvc.stop();
            MaskFormatter df = new MaskFormatter("HHHH");

            JFormattedTextField offsetField = new JFormattedTextField(df);
            offsetField.setColumns(5);
            offsetField.setValue("0000");
            JFormattedTextField sizeField = new JFormattedTextField(df);
            sizeField.setColumns(5);
            sizeField.setValue("4000");
            String pageList[] = {"U0", "U1", "U2", "U3", "VID"};
            JComboBox pageComboBox = new JComboBox(pageList);

            final JComponent[] inputs = {
                new JLabel("Memory Page"),
                pageComboBox,
                new JLabel("Offset (0x)"),
                offsetField,
                new JLabel("Size (0x)"),
                sizeField
            };

            int choice = JOptionPane.showOptionDialog(null, inputs,
                    "Save Memory contents to file...",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null, null, null);
            log.write("C:" + choice);
            if (choice == 0) {
                String memPath = prefs.get(MEM_DIR_KEY, System.getProperty("user.home"));
                log.write("Save binary:" + memPath);
                final JFileChooser fc = new JFileChooser(new File(memPath));
                fc.setApproveButtonText("Save");
                fc.setDialogTitle("Enter filename to save binary data...");
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int ret = fc.showOpenDialog(gui.mainFrame);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    int size = Integer.parseInt(sizeField.getText(), 16);
                    int offset = Integer.parseInt(offsetField.getText(), 16);
                    String pageName = (String) pageComboBox.getSelectedObjects()[0];
                    Memory.Page page = gui.tvc.getMemory().getPageByName(pageName);

                    File file = fc.getSelectedFile();
                    gui.tvc.getFileIO().saveBinary(file.getAbsolutePath(), page, offset, size);
                    memPath = file.getParent();
                    prefs.put(MEM_DIR_KEY, memPath);
                    prefs.flush();
                }
            }
        } catch (ParseException pe) {
            // format is const
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(gui.screen, "Save failed", ioe.getLocalizedMessage(), JOptionPane.ERROR_MESSAGE);
        } catch (BackingStoreException bse) {
            log.write(bse.getMessage());
        } finally {
            gui.tvc.start();
            gui.screen.requestFocusInWindow();
        }
    }

    /**
     * Creates a dialog to gather page:offset data and loads data from file.
     */
    private void actionCommandLoadBinary() {
        log.write("actionCommandLoadBinary");
        try {
            gui.tvc.stop();
            String memPath = prefs.get(MEM_DIR_KEY, System.getProperty("user.home"));
            log.write("Load Binary:" + memPath);
            final JFileChooser fc = new JFileChooser(new File(memPath));
            fc.setApproveButtonText("Load");
            fc.setDialogTitle("Select file to load binary data...");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int ret = fc.showOpenDialog(gui.mainFrame);
            if (ret == JFileChooser.APPROVE_OPTION) {

                MaskFormatter df = new MaskFormatter("HHHH");

                JFormattedTextField offsetField = new JFormattedTextField(df);
                offsetField.setColumns(5);
                offsetField.setValue("0000");
                String pageList[] = {"U0", "U1", "U2", "U3", "VID"};
                JComboBox pageComboBox = new JComboBox(pageList);

                final JComponent[] inputs = {
                    new JLabel("Page"),
                    pageComboBox,
                    new JLabel("Offset (0x)"),
                    offsetField
                };

                int choice = JOptionPane.showOptionDialog(null, inputs,
                        "Load Memory contents from file...",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null, null, null);
                log.write("C:" + choice);
                if (choice == 0) {
                    int offset = Integer.parseInt(offsetField.getText(), 16);
                    String pageName = (String) pageComboBox.getSelectedObjects()[0];
                    Memory.Page page = gui.tvc.getMemory().getPageByName(pageName);

                    File file = fc.getSelectedFile();
                    gui.tvc.getFileIO().loadBinary(file.getAbsolutePath(), page, offset);
                    // Save the last used path
                    memPath = file.getParent();
                    prefs.put(MEM_DIR_KEY, memPath);
                    prefs.flush();
                }
            }
        } catch (ParseException pe) {
            // format is const, not thrown
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(gui.screen, "Load error", ioe.getLocalizedMessage(), JOptionPane.ERROR_MESSAGE);
        } catch (BackingStoreException bse) {
            log.write(bse.getMessage());
        } finally {
            gui.tvc.start();
            gui.screen.requestFocusInWindow();
        }
    }

    private void actionCommandLoadCas() {
        try {
            gui.tvc.stop();
            String casPath = prefs.get(CAS_DIR_KEY, System.getProperty("user.home"));
            log.write("Load CAS:" + casPath);
            final JFileChooser fc = new JFileChooser(new File(casPath));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setDialogTitle("Select CAS file for direct load.");
            fc.setApproveButtonText("Load");
            FileFilter filter = new FileNameExtensionFilter("Casette files", "CAS", "cas");
            fc.addChoosableFileFilter(filter);
            int ret = fc.showOpenDialog(gui.mainFrame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                gui.tvc.getFileIO().loadCAS(file.getCanonicalPath());
                casPath = file.getParent();
                prefs.put(CAS_DIR_KEY, casPath);
                prefs.flush();
                //log.write("Saving path:" + casPath + ":" + prefs.absolutePath());  
            }
        } catch (IOException ioe) {
            log.write("Could not load CAS file: " + ioe.getMessage());
        } catch (BackingStoreException bse) {
            log.write("Could not save last used path: " + bse.getMessage());
        } finally {
            gui.tvc.start();
            gui.screen.requestFocusInWindow();
        }
    }

    public void itemStateChanged(ItemEvent e) {
        JToggleButton toggleButton = (JToggleButton) e.getSource();
        if (GUI.ACTIONCOMMAND_PAUSE.equals(toggleButton.getActionCommand())) {
            if (toggleButton.isSelected()) {
                gui.tvc.stop();
            } else {
                if (!gui.z80.running) {
                    gui.tvc.start();
                }
            }
        } else if (GUI.ACTIONCOMMAND_TRACE.equals(toggleButton.getActionCommand())) {
            if (toggleButton.isSelected()) {
                gui.z80.traceCPU = 10;
            } else {
                gui.z80.traceCPU = 0;
            }


        }
    }
}
