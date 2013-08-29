package org.wcs.smart.upgrade;

import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * A very simple progress monitor that display
 * progress in a dialog.
 * 
 * @author Emily
 *
 */
public class CustomProgressMonitor 
{
    private JOptionPane     pane;
    private JProgressBar    myBar;
    private JLabel          noteLabel;
    private String          note;
   
    private JDialog dialog; 
    public CustomProgressMonitor(Frame parentComponent,
                           String message,
                           String note) {
        this.note = note;
       
        myBar = new JProgressBar();
        myBar.setMinimum(0);
        myBar.setMaximum(100);
        myBar.setValue(0);
        
        noteLabel = new JLabel("Progress");
       
        pane = new JOptionPane(new Object[]{message, noteLabel, myBar}, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        
        dialog = pane.createDialog(parentComponent,UIManager.getString("ProgressMonitor.progressText"));
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        dialog.setModal(false);
        dialog.setVisible(true);
    }

    public void setProgress(int nv) {
        if (nv >= 100) {
            close();
        }
        else {
            if (myBar != null) {
                myBar.setValue(nv);
            }
        }
    }

    public void close() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
            pane = null;
            myBar = null;
        }
    }


  

   

    /**
     * Specifies the additional note that is displayed along with the
     * progress message. Used, for example, to show which file the
     * is currently being copied during a multiple-file copy.
     *
     * @param note  a String specifying the note to display
     * @see #getNote
     */
    public void setNote(String note) {
        this.note = note;
        if (noteLabel != null) {
            noteLabel.setText(note);
        }
    }


    /**
     * Specifies the additional note that is displayed along with the
     * progress message.
     *
     * @return a String specifying the note to display
     * @see #setNote
     */
    public String getNote() {
        return note;
    }

  

}
