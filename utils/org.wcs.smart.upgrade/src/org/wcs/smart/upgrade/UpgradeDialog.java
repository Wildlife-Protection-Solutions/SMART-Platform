package org.wcs.smart.upgrade;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This is a simple dialog that can be used
 * in SMART upgrade scripts.  It asks the users
 * for a backup file then runs the update action.
 * 
 * @author Emily
 *
 */
public class UpgradeDialog extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private JTextField txtInput;
	private IUpgradeAction updateAction;
	
	private JButton btnClose;
	private JButton btnUpgrade;
	
	public UpgradeDialog(String srcVersions, String targetVersion, IUpgradeAction upgradeAction){
		super();
		this.updateAction = upgradeAction;
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
			
		});
		createGui(srcVersions, targetVersion);
	}
	
	private void exit(){
		UpgradeDialog.this.dispose();
		System.exit(0);
	}
	
	private void createGui(String srcVersions, String targetVersion){
		setTitle("SMART Upgrade");
		
		URL url = getClass().getResource("smart16.gif");
		if (url != null){
			setIconImage((new ImageIcon(url)).getImage());
		}
		//setLayout(new GridLayout(2, 3));
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		
		
		JPanel headerPanel = new JPanel();
		headerPanel.setOpaque(true);
		headerPanel.setBackground(Color.WHITE);
		BorderLayout headerLayout = new BorderLayout();
		headerPanel.setLayout(headerLayout);
		headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		
		layout.putConstraint(SpringLayout.NORTH, headerPanel, 0, SpringLayout.NORTH, getContentPane());
		layout.putConstraint(SpringLayout.WEST, headerPanel, 0, SpringLayout.WEST, this.getContentPane());
		layout.putConstraint(SpringLayout.EAST, headerPanel, 0, SpringLayout.EAST, this.getContentPane());
		
		JLabel descriptionLabel = new JLabel("<html><p style='font-size:1.1em;font-weight:bold;margin-bottom:3px;'>SMART Upgrade Tool</p><p>This tool will upgrade a " + srcVersions + " SMART database backup to a " + targetVersion + " database that can be used in SMART version " + targetVersion + "</p></html>");
		descriptionLabel.setOpaque(true);
		descriptionLabel.setBackground(Color.WHITE);
		descriptionLabel.setBorder(new EmptyBorder(0,10,0,0));
		headerPanel.add(descriptionLabel, BorderLayout.CENTER);
		
		url = getClass().getResource("smart48.gif");
		if (url != null){
			ImageIcon img = (new ImageIcon(url));
			JLabel lbl = new JLabel(img);
			headerPanel.add(lbl, BorderLayout.LINE_START);
		}
		
		add(headerPanel);
		
		JSeparator spacer = new JSeparator(SwingConstants.HORIZONTAL);
		add(spacer);
		layout.putConstraint(SpringLayout.WEST, spacer, 0, SpringLayout.WEST, this.getContentPane());
		layout.putConstraint(SpringLayout.EAST, spacer, 0, SpringLayout.EAST, this.getContentPane());
		layout.putConstraint(SpringLayout.NORTH, spacer, 0, SpringLayout.SOUTH, headerPanel);
		
		JLabel jlbHelloWorld = new JLabel("SMART Backup File:");
		add(jlbHelloWorld);
		
//		txtInput = new JTextField("C:\\Users\\Emily\\Desktop\\SMART_test.bak.zip");
		txtInput = new JTextField("");
		add(txtInput);

		JButton browseButton = new JButton("...");
		add(browseButton);
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
			    FileNameExtensionFilter filter = new FileNameExtensionFilter("Zip Files", "zip");
			    chooser.setFileFilter(filter);
			    chooser.setSelectedFile(new File(txtInput.getText()));
			    int returnVal = chooser.showOpenDialog(UpgradeDialog.this);
			    
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	txtInput.setText(chooser.getSelectedFile().getAbsolutePath());
			    }
			}
		});
		
		layout.putConstraint(SpringLayout.WEST, jlbHelloWorld, 15, SpringLayout.WEST, this.getContentPane());
		layout.putConstraint(SpringLayout.NORTH, jlbHelloWorld, 15, SpringLayout.NORTH, spacer);
		layout.putConstraint(SpringLayout.WEST, txtInput, 5, SpringLayout.EAST, jlbHelloWorld);
		layout.putConstraint(SpringLayout.EAST, txtInput, -5, SpringLayout.WEST, browseButton);
		layout.putConstraint(SpringLayout.NORTH, txtInput, 15, SpringLayout.NORTH, spacer);
		layout.putConstraint(SpringLayout.EAST, browseButton, -15, SpringLayout.EAST, this.getContentPane());
		layout.putConstraint(SpringLayout.NORTH, browseButton, 15, SpringLayout.NORTH, spacer);
		layout.putConstraint(SpringLayout.SOUTH, browseButton, 0, SpringLayout.SOUTH, txtInput);
	
		btnClose = new JButton("Cancel");
		add(btnClose);
		btnClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});
		
		
		btnUpgrade = new JButton("Upgrade");
		add(btnUpgrade);
		btnUpgrade.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final File f = new File(txtInput.getText());
				if (!f.isFile() || !f.exists()){
					JOptionPane.showMessageDialog(UpgradeDialog.this, "The file " + f.getAbsolutePath() + " cannot be found.", "File Not Found", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (updateAction != null){
					
						
					final ProgressMonitor pm = new ProgressMonitor(UpgradeDialog.this, "Performing Upgrade", "", 0, 100);
					pm.setMillisToPopup(0);
					pm.setMillisToDecideToPopup(0);
					pm.setProgress(0);
					SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
						private Exception ex = null;
						private File outputFile = null;
						@Override
						protected Void doInBackground() throws Exception {
							try {
								outputFile = updateAction.performUpgrade(f, pm);
							} catch (Exception e1) {
								ex = e1;
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							return null;
						}
						protected void done() {
							
							if (ex != null){
								showErrorDialog(ex);
							}else{
								JOptionPane.showMessageDialog(UpgradeDialog.this, "Upgrade Complete.  Upgraded backup file located at: " + outputFile.toString() );
							}
							
							btnClose.setEnabled(true);
							btnUpgrade.setEnabled(true);
							pm.setProgress(100);
					    }};
					
					    btnClose.setEnabled(false);
					btnUpgrade.setEnabled(false);	
					worker.execute();
					
				}
			 
			}
		});
		
		layout.putConstraint(SpringLayout.EAST, btnClose, -5, SpringLayout.WEST, btnUpgrade);
		layout.putConstraint(SpringLayout.SOUTH, btnClose, -15, SpringLayout.SOUTH, this.getContentPane());
		layout.putConstraint(SpringLayout.EAST, btnUpgrade, -15, SpringLayout.EAST, this.getContentPane());
		layout.putConstraint(SpringLayout.SOUTH, btnUpgrade, -15, SpringLayout.SOUTH, this.getContentPane());
		
		
		this.setSize(550, 200);
		setMinimumSize(new Dimension(300, 180));
		//pack();
		this.setLocationRelativeTo(null);
		setVisible(true);
		
	}
	
	
	private void showErrorDialog(Exception e) {
		// create and configure a text area - fill it with exception text.
		
		JPanel area = new JPanel();
		BorderLayout layout = new BorderLayout();
		area.setLayout(layout);
		
		String message = e.getMessage();
		if (message == null){
			message = "Unknown";
		}
		if ( message.length() > 80){
			message = message.substring(0, 80) + "...";
		}
		JLabel msg = new JLabel("<html><p><b>Upgrade Error: " + message + "</b><br><br>If reporting this error to your administrator please include the entire message below in your error report.<br></html>");
		area.add(msg, BorderLayout.PAGE_START);
		
		final JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		textArea.setText(writer.toString());
		textArea.setCaretPosition(0);
		
		// stuff it in a scrollpane with a controlled size.
		JScrollPane scrollPane = new JScrollPane(textArea);		
		scrollPane.setPreferredSize(new Dimension(350, 150));
		area.add(scrollPane, BorderLayout.CENTER);
		// pass the scrollpane to the joptionpane.				
		JOptionPane.showMessageDialog(UpgradeDialog.this, area, "An Error Has Occurred", JOptionPane.ERROR_MESSAGE);
	}
	
	public static void main(String args[]){
		
		 try {
            //Set cross-platform Java L&F (also called "Metal")
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    }catch (Exception ex){}
		UpgradeDialog bd = new UpgradeDialog("test", "test", new IUpgradeAction() {
			
			@Override
			public File performUpgrade(File file, ProgressMonitor pm) throws Exception{
				throw new Exception("This is not a valid 1.1.2 database export file.");
			}
		});
	}
	
	public interface IUpgradeAction{
		public File performUpgrade(File file, ProgressMonitor pm) throws Exception;
		
	}
}
