/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.wcs.smart.datamodelmatcher.ui;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;



public class DataModelMatcherDialog extends Composite {

	Thread t;
	
	Text restoreFileName;
	Text startMistTxtFileName;
	Text mergeMistTxtFileName;
	
	Text startPass;
	Text mergePass;
	Text startUser;
	Text mergeUser;
	
	Text startXMLFileName;
	Text mergeSessionTxtFileName;
	MatchSession ms;
	ProcessingDialog wait; 
	
	Button r_open;
	Button s3_open;
	Button m_open; 
	
	public DataModelMatcherDialog(Composite c){
		super(c, SWT.None);
		
		GridLayout shellLayout = new GridLayout(1, false);
	    this.setLayout(shellLayout);
	    
		
	    //main composite and layout
		Composite main = new Composite(this, SWT.None);
		GridLayout layout = new GridLayout(1, true);
	    main.setLayout(layout);
	    
	    GridData mainGridData = new GridData(SWT.FILL,SWT.FILL, true, false);
	    main.setLayoutData(mainGridData);
	    
	    
	    
	    
//Restore	    
	    Label label_r = new Label(main, SWT.NONE);
	    label_r.setText("Restore Existing Session");
	    
	    //restore composite and layout etc
	    Composite restore = new Composite(main, SWT.BORDER);
		GridLayout restoreLayout = new GridLayout(3, false);
	    restore.setLayout(restoreLayout);
	    
	    GridData restore1GridData = new GridData(SWT.FILL,SWT.CENTER, true, false);
	    restore1GridData.horizontalIndent = 25;
	    restore.setLayoutData(restore1GridData);
	    
	    //restore area labels and file selector
	    Label label_r1 = new Label(restore, SWT.NONE);
	    label_r1.setText("Session File:");
	    
	    restoreFileName = new Text(restore, SWT.BORDER);
	    restoreFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    restoreFileName.setToolTipText("The location of your session file to load.");

	    Button open = new Button(restore, SWT.PUSH);
		open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false));
		((GridData)open.getLayoutData()).heightHint = 10;
	    open.setText("...");
	    open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
	    		dlg.setFilterNames(new String[] {"(CSV)"});
	    		dlg.setFilterExtensions(new String[] {"*.csv"});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			restoreFileName.setText(fn);
	    		}
	    	}
	    });
	    
	  //Existing go button
		  
	    r_open = new Button(restore, SWT.PUSH);
		r_open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false,3,0));
		((GridData)open.getLayoutData()).heightHint = 10;
		r_open.setText("Restore Session");
		r_open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		File existingSessionFile = new File(restoreFileName.getText());
	    		if (!existingSessionFile.exists()){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
	    			messageBox.setMessage("Match Session File not found: '" + existingSessionFile.toString() + "'" );
	    			messageBox.open();
	    			return;
	    		}
	    		
	    		
	    		MatchSession ms = new MatchSession(getShell());
	    		ms.setSaveLocation(restoreFileName.getText());
	    		String result = ms.loadSessionFromFile();
	    		if(result != null){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
	    			messageBox.setMessage(result);
	    			messageBox.open();
	    			return;
	    		}
	    		
	    		enableButtons(false);
	    		MatchSessionDialog matchSession = new MatchSessionDialog(getShell(), ms);
	    		
	    		matchSession.open();
	    		if(ms.isDirty()){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION
	    					| SWT.YES | SWT.NO);
	                messageBox.setMessage("Do you want to save your changes?");
	                messageBox.setText("Exiting Application");
	                int response = messageBox.open();
	                if (response == SWT.YES)
	                		ms.save();
	    		}
	    		enableButtons(true);
	    	}
	    });

	    
	    
//Start New	    
	    Label label_n = new Label(main, SWT.NONE);
	    label_n.setText("Start New Session");
	    
	    //start composite and layout etc
	    Composite start = new Composite(main, SWT.BORDER);
		GridLayout startLayout = new GridLayout(3, false);
		start.setLayout(startLayout);
	    
	    GridData startGridData = new GridData(SWT.FILL,SWT.CENTER, true, false);
	    startGridData.horizontalIndent = 25;
	    start.setLayoutData(startGridData);
	    
	    //start area labels and file selector
	    //line1
	    Label label_s1 = new Label(start, SWT.NONE);
	    label_s1.setText("MIST Database File:");
	    
	    startMistTxtFileName = new Text(start, SWT.BORDER);
	    GridData gd1 = new GridData(SWT.FILL, SWT.CENTER, true, false);
	    gd1.widthHint = 200;
	    startMistTxtFileName.setLayoutData(gd1);
	    startMistTxtFileName.setToolTipText("The location of your source MIST Database file.");

	    Button s_open = new Button(start, SWT.PUSH);
		s_open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false));
		((GridData)open.getLayoutData()).heightHint = 10;
		s_open.setText("...");
		s_open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
	    		dlg.setFilterNames(new String[] {"FDB, GDB"});
	    		dlg.setFilterExtensions(new String[] {"*.*db"});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			startMistTxtFileName.setText(fn);
	    		}
	    	}
	    });

		//Line 2 user/pass
		Composite userPassLine = new Composite(start, SWT.NONE);
		userPassLine.setLayout(new GridLayout(4, true));
		userPassLine.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3,0));

		Label startUserLabel = new Label(userPassLine, SWT.NONE);
		startUserLabel.setText("DB Username:");
		startUserLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		startUser = new Text(userPassLine, SWT.NONE);
		startUser.setText("SYSDBA");
		GridData textGridData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		textGridData.widthHint = 75;
		startUser.setLayoutData(textGridData);
		
		Label startPassLabel = new Label(userPassLine, SWT.NONE);
		startPassLabel.setText("DB Password:");
		startPassLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		startPass= new Text(userPassLine, SWT.NONE |SWT.PASSWORD);
		startPass.setText("masterkey");
		startPass.setLayoutData(textGridData);
		
		//start new line 3
	    Label label_s2 = new Label(start, SWT.NONE);
	    label_s2.setText("SMART Data Model:");
	    
	    startXMLFileName = new Text(start, SWT.BORDER);
	    startXMLFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    startXMLFileName.setToolTipText("Location of your SMART data model XML export file.");

	    Button s2_open = new Button(start, SWT.PUSH);
		s2_open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false));
		((GridData)open.getLayoutData()).heightHint = 10;
		s2_open.setText("...");
		s2_open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
	    		dlg.setFilterNames(new String[] {"(XML)"});
	    		dlg.setFilterExtensions(new String[] {"*.xml"});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			startXMLFileName.setText(fn);
	    		}
	    	}
	    });
		
		//start new line 3
	  
	    s3_open = new Button(start, SWT.PUSH);
		s3_open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false,3,0));
		((GridData)open.getLayoutData()).heightHint = 10;
		s3_open.setText("Start New Session");
		s3_open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		File dbFile = new File(startMistTxtFileName.getText());
	    		File xmlFile = new File(startXMLFileName.getText());
	    		if (!dbFile.exists()){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
	    			messageBox.setMessage("MIST database file not found: '" + dbFile.toString() + "'" );
	    			messageBox.open();
	    			return;
	    		}
	    		if (!xmlFile.exists()){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
	    			messageBox.setMessage("SMART XML data model file not found: '" + xmlFile.toString() + "'" );
	    			messageBox.open();
	    			return;
	    		}	    		
	    		
	    		
	    		ms = new MatchSession(getShell());
	    		ms.setUserNamePass(startUser.getText(), startPass.getText());
	    		
	    		ms.setMistLocation(startMistTxtFileName.getText());
	    		ms.setSmartXmlLocation(startXMLFileName.getText());
	    		
	    		try {
	    			final Shell dlgShell = new Shell(getShell(), SWT.APPLICATION_MODAL);
	    			
					wait = new ProcessingDialog(dlgShell, dlgShell.getBounds());
	    			wait.open();
	    			
	    			
	    			t = new Thread(new Runnable() { public void run() { 
	    				try {
							ms.loadRows(); //loads the mist data	   
						} catch (Exception e) {
							ms.setError(e.toString());
							e.printStackTrace();
						} 
	    				
	    			}});
	    			
	    			t.start();
	    			Display display = getShell().getDisplay();
	    			
	    			while(t.isAlive()){
    			          if (!display.readAndDispatch()) {
    			            display.sleep();
    			          }
	    			}
	    			dlgShell.dispose();
	    			
	    			if(!ms.getError().equals("")){
						MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
				    	messageBox.setMessage("Error while processing data, most likely an incorrect user/pass:  " + ms.getError());
				    	messageBox.open();
				    	return;
	    			}
    				
				} catch (Exception e2) {
			    	MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
			    	messageBox.setMessage("MIST database error:" + e2);
			    	messageBox.open();
			    	return;
				} 
	    		
	    		try {
					ms.loadSmartDataModel();
				} catch (Exception e1) {
					MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
					e1.printStackTrace();
	    			messageBox.setMessage("SMART XML data model file error, ensure the file was created in SMART by selecting 'Export to XML' under the 'Conservation Area -> Data Model...' menu. " + e1 );
	    			messageBox.open();
	    			return;
				}
	    		enableButtons(false);
	    		MatchSessionDialog matchSession = new MatchSessionDialog(getShell(), ms);
	    		
	    		matchSession.open();
	    		
	    		if(ms.isDirty()){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
	    			messageBox.setMessage("Do you want to save your changes?");
	    			messageBox.setText("Exiting Application");
	    			int response = messageBox.open();
	    			if (response == SWT.YES)
	              	  	ms.save();
	    		}
	            enableButtons(true);
	    	}
	    });

		
//MERGE
	    Label label_m = new Label(main, SWT.NONE);
	    label_m.setText("Merge MIST Data Model with Existing Session");
		
	    //merge composite and layout etc
	    Composite merge = new Composite(main, SWT.BORDER);
		GridLayout mergeLayout = new GridLayout(3, false);
		merge.setLayout(mergeLayout);
	    
	    GridData mergeGridData = new GridData(SWT.FILL,SWT.CENTER, true, false);
	    mergeGridData.horizontalIndent = 25;
	    merge.setLayoutData(mergeGridData);
	    
		//Line 1 Merge files
	    Label label_m1 = new Label(merge, SWT.NONE);
	    label_m1.setText("Existing Session File:");
	    
	    mergeSessionTxtFileName = new Text(merge, SWT.BORDER);
	    mergeSessionTxtFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    mergeSessionTxtFileName.setToolTipText("The location of your existing session file to load.");

	    Button mopen = new Button(merge, SWT.PUSH);
		mopen.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false));
		((GridData)mopen.getLayoutData()).heightHint = 10;
	    mopen.setText("...");
	    mopen.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
	    		dlg.setFilterNames(new String[] {"(CSV)"});
	    		dlg.setFilterExtensions(new String[] {"*.csv"});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			mergeSessionTxtFileName.setText(fn);
	    		}
	    	}
	    });
	    
	  //Line 2 user/pass
	  		Composite mergeUserPassLine = new Composite(merge, SWT.NONE);
	  		mergeUserPassLine.setLayout(new GridLayout(4, true));
	  		mergeUserPassLine.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3,0));

	  		Label mergeUserLabel = new Label(mergeUserPassLine, SWT.NONE);
	  		mergeUserLabel.setText("DB Username:");
	  		mergeUserLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

	  		mergeUser = new Text(mergeUserPassLine, SWT.NONE);
	  		mergeUser.setText("SYSDBA");
	  		mergeUser.setLayoutData(textGridData);
	  		
	  		Label mergePassLabel = new Label(mergeUserPassLine, SWT.NONE);
	  		mergePassLabel.setText("DB Password:");
	  		mergePassLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
	  		
	  		mergePass= new Text(mergeUserPassLine, SWT.NONE |SWT.PASSWORD);
	  		mergePass.setText("masterkey");
	  		mergePass.setLayoutData(textGridData);
	  		
	  		
	    //Line 3 of merge, database file
	    Label label_m2 = new Label(merge, SWT.NONE);
	    label_m2.setText("MIST Database File:");
	    
	    mergeMistTxtFileName = new Text(merge, SWT.BORDER);
	    mergeMistTxtFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    mergeMistTxtFileName.setToolTipText("The location of your source MIST Database file.");

	    Button m2open = new Button(merge, SWT.PUSH);
		m2open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false));
		((GridData)open.getLayoutData()).heightHint = 10;
		m2open.setText("...");
		m2open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
	    		dlg.setFilterNames(new String[] {"FDB, GDB"});
	    		dlg.setFilterExtensions(new String[] {"*.*db"});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			mergeMistTxtFileName.setText(fn);
	    		}
	    		
	    	}
	    });
	    
		
		m_open = new Button(merge, SWT.PUSH);
		m_open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false,3,0));
		((GridData)open.getLayoutData()).heightHint = 10;
		m_open.setText("Merge Session");
		m_open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		File dbFile = new File(mergeMistTxtFileName.getText());
	    		File sessionFile = new File(mergeSessionTxtFileName.getText());
	    		if (!dbFile.exists()){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
	    			messageBox.setMessage("MIST database file not found: '" + dbFile.toString() + "'" );
	    			messageBox.open();
	    			return;
	    		}
	    		if (!sessionFile.exists()){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
	    			messageBox.setMessage("SMART XML data model file not found: '" + sessionFile.toString() + "'" );
	    			messageBox.open();
	    			return;
	    		}	    		
	    		
	    		
	    		ms = new MatchSession( getShell() );
	    		
	    		ms.setUserNamePass(mergeUser.getText(), mergePass.getText());
	    		
	    		ms.setMistLocation(mergeMistTxtFileName.getText());
	    		ms.setSaveLocation(mergeSessionTxtFileName.getText());
	    		
	    		String result = ms.loadSessionFromFile();
	    		
	    		ms.setSaveLocation(null);//once the session is loaded, remove the filename so the user doesn't overwite it and is asked for a new filename when saving. 
	    		
	    		if(result != null){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
	    			messageBox.setMessage(result);
	    			messageBox.open();
	    			return;
	    		}
	    		
	    		try {

	    			final Shell dlgShell = new Shell(getShell(), SWT.APPLICATION_MODAL);
	    			
					wait = new ProcessingDialog(dlgShell, dlgShell.getBounds());
	    			wait.open();
	    			
	    			
	    			Thread t = new Thread(new Runnable() { public void run() { 
	    				try {
							ms.mergeRows(); //loads the mist data	   
						} catch (Exception e) {
							ms.setError(e.toString());
							e.printStackTrace();
						} 
	    				
	    			}});
	    			
	    			t.start();

	    			Display display = getShell().getDisplay();
	    			
	    			while(t.isAlive()){
    			          if (!display.readAndDispatch()) {
    			            display.sleep();
    			          }
	    			}

	    			dlgShell.dispose();
	    			
	    			if(!ms.getError().equals("")){
						MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
				    	messageBox.setMessage("Error while processing data, most likely an incorrect user/pass or the file is being used by another application:   " + ms.getError());
				    	messageBox.open();
				    	return;
	    			}
    				
				} catch (Exception e2) {
			    	MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
			    	messageBox.setMessage("MIST database error:" + e2);
			    	messageBox.open();
			    	return;
				} 
	    		
	    		try {
					ms.loadSmartDataModel();
				} catch (Exception e1) {
					MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
					e1.printStackTrace();
	    			messageBox.setMessage("SMART XML data model file error, ensure the file was created in SMART by selecting 'Export to XML' under the 'Conservation Area -> Data Model...' menu. " + e1 );
	    			messageBox.open();
	    			return;
				}
	    		
	    		enableButtons(false);
	    		MatchSessionDialog matchSession = new MatchSessionDialog(getShell(), ms);
	    		
	    		matchSession.open();
	    		if(ms.isDirty()){
	    			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
	    			messageBox.setMessage("Do you want to save your changes?");
	    			messageBox.setText("Exiting Application");
	    			int response = messageBox.open();
	    			if (response == SWT.YES)
	    				ms.save();
	    		}
	            enableButtons(true);
	            
	    	}

			
	    });
		
	}
	private void enableButtons(boolean b) {
		m_open.setEnabled(b);
		s3_open.setEnabled(b);
		r_open.setEnabled(b);
	}

}



