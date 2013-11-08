package org.wcs.smart.datamodelmatcher.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class DataModelMatcherDialog extends Composite {

	Text txtFileName;
	Text startMistTxtFileName;
	Text mergeMistTxtFileName;
	
	Text startSessionTxtFileName;
	Text mergeSessionTxtFileName;
	
	
	public DataModelMatcherDialog(Shell shell) {
		super(shell, SWT.BORDER);
		
		GridLayout shellLayout = new GridLayout(1, false);
	    shell.setLayout(shellLayout);
	    
		
	    //main composite and layout
		Composite main = new Composite(shell, SWT.BORDER);
		GridLayout layout = new GridLayout(1, true);
	    main.setLayout(layout);
	    
	    GridData mainGridData = new GridData(SWT.FILL,SWT.CENTER, true, false);
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
	    
	    txtFileName = new Text(restore, SWT.BORDER);
	    txtFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    txtFileName.setToolTipText("The location of your session file to load.");

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
	    			txtFileName.setText(fn);
	    		}
	    	}
	    });

	    
	    
//Start New	    
	    Label label_n = new Label(main, SWT.NONE);
	    label_n.setText("Start New Session");
	    
	    //restore composite and layout etc
	    Composite start = new Composite(main, SWT.BORDER);
		GridLayout startLayout = new GridLayout(3, false);
		start.setLayout(startLayout);
	    
	    GridData startGridData = new GridData(SWT.FILL,SWT.CENTER, true, false);
	    startGridData.horizontalIndent = 25;
	    start.setLayoutData(startGridData);
	    
	    //restore area labels and file selector
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
	    		dlg.setFilterNames(new String[] {"(FDB)"});
	    		dlg.setFilterExtensions(new String[] {"*.fdb"});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			startMistTxtFileName.setText(fn);
	    		}
	    	}
	    });

		//restore line 2
	    Label label_s2 = new Label(start, SWT.NONE);
	    label_s2.setText("SMART Data Model:");
	    
	    startSessionTxtFileName = new Text(start, SWT.BORDER);
	    startSessionTxtFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    startSessionTxtFileName.setToolTipText("Location of your SMART data model XML export file.");

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
	    			startSessionTxtFileName.setText(fn);
	    		}
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
	    
	    //Line 2 of merge, database file
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
	    		dlg.setFilterNames(new String[] {"(FDB)"});
	    		dlg.setFilterExtensions(new String[] {"*.fdb"});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			mergeMistTxtFileName.setText(fn);
	    		}
	    	}
	    });
	    
		
	}
	
    
}
