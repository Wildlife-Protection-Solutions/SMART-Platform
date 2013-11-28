package org.wcs.smart.incident.ui;

import java.io.File;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.control.XmlMultiExportDialog;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog to allow users to export multiple patrols at once.
 * 
 * This code performs validation to ensure at least one patrol is selected
 * and that the output directory is a valid directory.
 * 
 * @author egouge
 *
 */
public class MultiIncidentExportDialog extends XmlMultiExportDialog implements IIncidentFilteringView {

	private static final String OUTPUT_DIR = "outputDir"; //$NON-NLS-1$
	private static final String INCLUDE_ATTACHMENT = "attachements"; //$NON-NLS-1$
	private static final String EXPORT_DIALOGTITLE = "Export Incidents";

	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.patrol.export.dialog"); //$NON-NLS-1$
	static{
		dialogSettings.put(INCLUDE_ATTACHMENT, true);
	}

	private IncidentFilter currentFilter = new IncidentFilter();
	
	/**
	 * Creates a new dialog.
	 * 
	 * @param parentShell parent shell
	 * @param patrol patrol to export
	 */
	public MultiIncidentExportDialog(Shell parentShell) {
		super(parentShell, "The incidents below have been filtered.  Click <a>here</a> to change filter.");
		this.currentFilter.setDateFilter(DateFilter.LAST_30_DAYS, null, null);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(EXPORT_DIALOGTITLE);
		setMessage("Export selected incidents to file.");
		getShell().setText(EXPORT_DIALOGTITLE);
		return super.createDialogArea(parent);
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID){
			if(!validate()){
				return;
			}
		}
		super.buttonPressed(buttonId);
		dialogSettings.put(OUTPUT_DIR, getDirectory());
		dialogSettings.put(INCLUDE_ATTACHMENT, getIncludeAttachments());
	}
	
	private boolean validate(){
		
		if (super.getTableViewer().getCheckedElements().length == 0) {
			MessageDialog.openInformation(getShell(), EXPORT_DIALOGTITLE, "Nothing to export.");
			return false;
		}
		
		File dir = new File(txtFile.getText());
		if (!dir.exists()) {
			if (!MessageDialog.openQuestion(getShell(), EXPORT_DIALOGTITLE, MessageFormat.format("The directory {0} does not exist and will be created.  Do you want to continue?", new Object[]{dir.getAbsolutePath()}))) {
				return false;
			}
			if (!SmartUtils.createDirectory(dir)){
				IncidentPlugIn.displayLog("Could not create directory.", null);
				return false;
			}
		}else if (!dir.isDirectory()){
			IncidentPlugIn.displayLog(MessageFormat.format("{0} is not a valid directory.", new Object[]{dir.toString()}),null);
			return false;
		}
		return true;
	}

	@Override
	protected boolean getDefaultIncludeAttachments() {
		return dialogSettings.getBoolean(INCLUDE_ATTACHMENT);
	}
	
	@Override
	protected String getDefaultOutputFolder() {
		return dialogSettings.get(OUTPUT_DIR);
	}
	
	@Override
	protected void handleFilterLinkClicked() {
		IncidentFilterDialog pfd = new IncidentFilterDialog(getShell(), MultiIncidentExportDialog.this);
		pfd.open();
	}

	@Override
	protected void loadObjectData() {
		Job loadPatrols = new Job("Loading Incidents..."){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					Query q = currentFilter.buildQuery(s);
					List<?> results = q.list();
					final Object[][] data = new Object[results.size()][2];
					int counter = 0;
					for(Object x : results){
						Object[] row = (Object[])x;
						
						String pname = row[1] + " [" + DateFormat.getDateInstance(DateFormat.SHORT).format((Timestamp)row[2]) + "]";   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						Object[] thisdata = {pname, (byte[])row[0], row[1]};
						data[counter++] = thisdata;
					}
					
					getShell().getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							getTableViewer().setInput(data);
							getTableViewer().refresh();
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

	@Override
	public void updateContent() {
		loadObjectData();
	}

	@Override
	public IncidentFilter getFilter() {
		return currentFilter;
	}

}
