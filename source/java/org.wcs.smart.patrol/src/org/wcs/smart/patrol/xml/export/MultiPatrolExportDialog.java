package org.wcs.smart.patrol.xml.export;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.control.XmlMultiExportDialog;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.IPatrolFilteringView;
import org.wcs.smart.patrol.internal.ui.views.PatrolFilterDialog;
import org.wcs.smart.patrol.internal.ui.views.PatrolViewFilter;

/**
 * Dialog to allow users to export multiple patrols at once.
 * 
 * @author egouge
 *
 */
public class MultiPatrolExportDialog extends XmlMultiExportDialog implements IPatrolFilteringView {

	private static final String OUTPUT_DIR = "outputDir"; //$NON-NLS-1$
	private static final String INCLUDE_ATTACHMENT = "attachements"; //$NON-NLS-1$
	
	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.patrol.export.dialog"); //$NON-NLS-1$
	static{
		dialogSettings.put(INCLUDE_ATTACHMENT, true);
	}

	private PatrolViewFilter currentFilter = new PatrolViewFilter();
	
	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 * @param patrol patrol to export
	 */
	public MultiPatrolExportDialog(Shell parentShell) {
		super(parentShell, Messages.MultiPatrolExportDialog_ChangeFilter);
		this.currentFilter.setDateFilter(DateFilter.LAST_30_DAYS, null, null);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.MultiPatrolExportDialog_PageTitle);
		setMessage(Messages.MultiPatrolExportDialog_Message);
		getShell().setText(Messages.MultiPatrolExportDialog_Title);
		return super.createDialogArea(parent);
	}
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		dialogSettings.put(OUTPUT_DIR, getDirectory());
		dialogSettings.put(INCLUDE_ATTACHMENT, getIncludeAttachments());
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
		PatrolFilterDialog pfd = new PatrolFilterDialog(getShell(), MultiPatrolExportDialog.this);
		pfd.open();
	}

	@Override
	protected void loadObjectData() {
		Job loadPatrols = new Job(Messages.MultiPatrolExportDialog_LoadPatrolJobName){
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
						
						String pname = (String)row[1] + " [" + DateFormat.getDateInstance(DateFormat.SHORT).format((Timestamp)row[3]) + " - " + DateFormat.getDateInstance(DateFormat.SHORT).format( (Timestamp)row[4]) + "]";   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	public PatrolViewFilter getFilter() {
		return currentFilter;
	}

}
