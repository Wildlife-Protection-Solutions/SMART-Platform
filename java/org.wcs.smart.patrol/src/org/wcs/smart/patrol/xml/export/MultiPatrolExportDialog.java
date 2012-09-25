package org.wcs.smart.patrol.xml.export;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Dialog to allow users to export multiple patrols at once.
 * 
 * @author egouge
 *
 */
public class MultiPatrolExportDialog extends TitleAreaDialog {

	private static final int MAX_RESULTS = 20; 
	
	private static final String SHOW_ALL_RESULTS = "Only the last " + MAX_RESULTS + " patrols are shown.  Click <a>here</a> to see all patrols";
	private static final String SHOW_RESULTS = "Click <a>here</a> to show only the last " + MAX_RESULTS + " patrols.";
	
	private static final String OUTPUT_DIR = "outputDir";
	private static final String INCLUDE_ATTACHMENT = "attachements";
	
	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.patrol.export.dialog");
	static{
		dialogSettings.put(INCLUDE_ATTACHMENT, true);
	}
	private Text txtFile;
	private Button btnIncludeAttachments;
	private CheckboxTableViewer patrols ;
	
	private String dirName;
	private boolean includeAttachements;
	private List<byte[]> patrolUuids;

	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 * @param patrol patrol to export
	 */
	public MultiPatrolExportDialog(Shell parentShell) {
		super(parentShell);

	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		dirName = txtFile.getText();
		includeAttachements = btnIncludeAttachments.getSelection();
		
		this.patrolUuids = new ArrayList<byte[]>();
		Object[] checked = patrols.getCheckedElements();
		for (int i = 0; i < checked.length; i ++){
			patrolUuids.add(  (byte[])((Object[])checked[i])[1] );
		}
		
		dialogSettings.put(OUTPUT_DIR, dirName);
		dialogSettings.put(INCLUDE_ATTACHMENT, includeAttachements);
		
		super.buttonPressed(buttonId);
	}

	/**
	 * @return the filename selected by user
	 */
	public String getDirectory() {
		return this.dirName;
	}

	/**
	 * @return if attachments should be included
	 */
	public boolean getIncludeAttachments() {
		return this.includeAttachements;
	}

	/**
	 * 
	 * @return list of patrol uuids to export
	 */
	public List<byte[]> getPatrolUuids(){
		return this.patrolUuids;
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button b = createButton(parent, IDialogConstants.OK_ID, "Export", true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);

		b.setEnabled(false);
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Destination Folder*:");
		txtFile = new Text(main, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		
		txtFile.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFile.getText() != null && txtFile.getText().length() > 0) {
					if (getButton(IDialogConstants.OK_ID) != null){
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}
				}
			}
		});
		String value = dialogSettings.get(OUTPUT_DIR);
		if (value != null){
			txtFile.setText(value);
		}
				
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText("Browse...");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
			
				if (txtFile.getText().length() > 0) {
					dd.setFilterPath(txtFile.getText());
				}
				String f = dd.open();
				if (f != null) {
					txtFile.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Include Attachments**:");
		btnIncludeAttachments = new Button(main, SWT.CHECK);
		btnIncludeAttachments.setSelection(dialogSettings.getBoolean(INCLUDE_ATTACHMENT));
			
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		btnIncludeAttachments.setLayoutData(gd);
		
		
		final Link lnk = new Link(main, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		lnk.setLayoutData(gd);
		lnk.setText(SHOW_ALL_RESULTS);
		lnk.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				String[][] loadingdata = {{"Loading", null}}; 
				patrols.setInput( loadingdata );
				patrols.refresh();
			
				if (lnk.getText().equals(SHOW_ALL_RESULTS)){
					loadPatrols(true);
					lnk.setText(SHOW_RESULTS);
				}else{
					loadPatrols(false);
					lnk.setText(SHOW_ALL_RESULTS);
				}
			}
		});
		
		patrols = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.MULTI);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		gd.heightHint = 200;
		patrols.getControl().setLayoutData(gd);
		patrols.getTable().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (patrols.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)patrols.getSelection());
					selection.getFirstElement();
					boolean value = patrols.getChecked(   selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						patrols.setChecked(tp, !value);
					}
					e.doit = false;
							
				}
				
			}
		});
		patrols.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Object[]){
					return (String)((Object[])element)[0];
				}
				return super.getText(element);
			}
		});
		patrols.setContentProvider(ArrayContentProvider.getInstance());
		String[][] loadingdata = {{"Loading", null}}; 
		patrols.setInput( loadingdata );
		loadPatrols(false);
		
		Composite lowerComp = new Composite(main, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.verticalSpacing = gl.marginLeft = gl.marginRight = gl.marginTop = gl.marginBottom = 0;
		lowerComp.setLayout(gl);
		
		final Link selectAll = new Link(lowerComp, SWT.NONE);
		selectAll.setText("<a>Select All</a>");
		
		lbl = new Label(lowerComp, SWT.VERTICAL | SWT.SEPARATOR);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lbl.setLayoutData(gd);
		
		final Link deselectAll = new Link(lowerComp, SWT.NONE);
		deselectAll.setText("<a>De-Select All</a>");
		
		Listener listener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				patrols.setAllChecked(event.widget == selectAll);
			}};
		deselectAll.addListener(SWT.Selection, listener);
		selectAll.addListener(SWT.Selection, listener);
			
		lbl = new Label(main, SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd.widthHint = 250;
		lbl.setText("*Existing files may automatically be overwriteen.");
		lbl.setLayoutData(gd);
		
		
		lbl = new Label(main, SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd.widthHint = 250;
		lbl.setText("**If attachments are included a zip file will be generated that includes the patrol and attachments.  Otherwise only xml file is exported.");
		lbl.setLayoutData(gd);
		
		setMessage("Export selected patrols to xml files.");
		getShell().setText("Export Patrols");
		return composite;

	}
	
	private void loadPatrols(final boolean loadAll){
		Job loadPatrols = new Job("loading patrols"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					String query = "SELECT uuid, id, startDate, endDate FROM " + Patrol.class.getSimpleName() + " WHERE conservationArea = :ca ORDER BY startDate Desc";
					Query q = s.createQuery(query);
					q.setParameter("ca", SmartDB.getCurrentConservationArea());
					if (!loadAll){
						q.setMaxResults(MAX_RESULTS);
					}
					List<?> results = q.list();
					final Object[][] data = new Object[results.size()][2];
					int counter = 0;
					for(Object x : results){
						Object[] row = (Object[])x;
						
						String pname = (String)row[1] + " [" + DateFormat.getDateInstance(DateFormat.SHORT).format((Timestamp)row[2]) + " - " + DateFormat.getDateInstance(DateFormat.SHORT).format( (Timestamp)row[3]) + "]";  
						Object[] thisdata = {pname, (byte[])row[0]};
						data[counter++] = thisdata;
					}
					
					getShell().getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							patrols.setInput(data);
							patrols.refresh();
						}
					});
					
				}finally{
					if (s.getTransaction().isActive()){
						s.getTransaction().commit();
					}
					s.close();
				}
				return Status.OK_STATUS;
			}
			
		};
		loadPatrols.schedule();
		
	}

	@Override
	public boolean isResizable(){
		return true;
	}
}
