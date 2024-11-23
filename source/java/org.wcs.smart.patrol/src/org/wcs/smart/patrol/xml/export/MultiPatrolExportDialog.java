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

package org.wcs.smart.patrol.xml.export;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.common.control.XmlMultiExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.IPatrolFilteringView;
import org.wcs.smart.patrol.internal.ui.views.PatrolFilterDialog;
import org.wcs.smart.patrol.internal.ui.views.PatrolViewFilter;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.patrol.xml.XmlExtraDataContributionFactory;
import org.wcs.smart.patrol.xml.external.IPatrolExportContribution;
import org.wcs.smart.util.SmartUtils;

import jakarta.persistence.Tuple;

/**
 * Dialog to allow users to export multiple patrols at once.
 * 
 * This code performs validation to ensure at least one patrol is selected
 * and that the output directory is a valid directory.
 * 
 * @author egouge
 *
 */
public class MultiPatrolExportDialog extends XmlMultiExportDialog implements IPatrolFilteringView {

	private static final String OUTPUT_DIR = "outputDir"; //$NON-NLS-1$
	private static final String INCLUDE_ATTACHMENT = "attachements"; //$NON-NLS-1$
	private static final String EXPORT_DIALOGTITLE = Messages.ExportPatrolHandler_ExportDialog_Title2;

	private static IDialogSettings dialogSettings = new DialogSettings("org.wcs.smart.patrol.export.dialog"); //$NON-NLS-1$
	static{
		dialogSettings.put(INCLUDE_ATTACHMENT, true);
	}

	private PatrolViewFilter currentFilter = PatrolViewFilter.newInstance();
	
	private List<PatrolEditorInput> initValues;
	private List<IPatrolExportContribution> options;
	
	/**
	 * Creates a new dialog.
	 * 
	 * @param parentShell parent shell
	 * @param patrol patrol to export
	 */
	public MultiPatrolExportDialog(Shell parentShell, PatrolViewFilter filter, List<PatrolEditorInput> initValues) {
		super(parentShell, Messages.MultiPatrolExportDialog_ChangeFilter2);
		if (filter != null) this.currentFilter = filter;
		this.initValues = initValues;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.MultiPatrolExportDialog_PageTitle2);
		setMessage(Messages.MultiPatrolExportDialog_Message2);
		getShell().setText(Messages.MultiPatrolExportDialog_Title2);
		return super.createDialogArea(parent);
	}
	
	public HashMap<Object,Object> getExportOptions(){
		HashMap<Object,Object> ops = new HashMap<>();
		for (IPatrolExportContribution c: options) {
			ops.putAll(c.getOptions());
		}
		return ops;
	}
	
	@Override
	public void addOptions(Composite parent) {
		options = new ArrayList<>();
		for (IPatrolExportContribution c : XmlExtraDataContributionFactory.getUiContributions()) {
			options.add(c);
			c.createControls(parent);
		}
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
			MessageDialog.openInformation(getShell(), EXPORT_DIALOGTITLE, Messages.ExportPatrolHandler_Error_NothingToExport);
			return false;
		}
		
		Path dir = Paths.get(txtFile.getText());
		if (!Files.exists(dir)) {
			if (!MessageDialog.openQuestion(getShell(), EXPORT_DIALOGTITLE, MessageFormat.format(Messages.ExportPatrolHandler_Warning_DirNotExist, new Object[]{dir.toAbsolutePath().toString()}))) {
				return false;
			}
			if (!SmartUtils.createDirectory(dir)){
				SmartPatrolPlugIn.displayLog(Messages.MultiPatrolExportDialog_CouldNotCreateDirectory, null);
				return false;
			}
		}else if (!Files.isDirectory(dir)){
			SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.MultiPatrolExportDialog_InvalidDirectory, new Object[]{dir.toString()}),null);
			return false;
		}
		
		for (IPatrolExportContribution c : options) {
			String x = c.validate();
			if (x != null) {
				MessageDialog.openInformation(getShell(), EXPORT_DIALOGTITLE, x);
				return false;
			}
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
		PatrolFilterDialog pfd = new PatrolFilterDialog(getShell(), MultiPatrolExportDialog.this);
		pfd.open();
	}

	@Override
	protected void loadObjectData() {
		Job loadPatrols = new Job(Messages.MultiPatrolExportDialog_LoadPatrolJobName2){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<RowItem> items = new ArrayList<>();
				
				try(Session s = HibernateManager.openSession()){
					s.beginTransaction();
					try{
						Query<Tuple> q = currentFilter.buildQuery(s);
						for(Tuple row : q.list()){
							StringBuilder sb = new StringBuilder();
							sb.append((String)row.get(1));
							sb.append(" ["); //$NON-NLS-1$
							sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format((LocalDate)row.get(3)));
							sb.append(" - "); //$NON-NLS-1$
							sb.append( DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format((LocalDate)row.get(4)));
							sb.append("]"); //$NON-NLS-1$
							
							items.add(new RowItem(sb.toString(), (UUID)row.get(0), (String)row.get(1)));
						}
					}finally{
						if (s.getTransaction().isActive()){
							s.getTransaction().commit();
						}
					}
				}

				getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						getTableViewer().setInput(items);
						if (initValues != null) {
							Object[] i = initValues.stream().map(e->new RowItem(e.getUuid())).collect(Collectors.toList()).toArray();
							getTableViewer().setCheckedElements(i);
						}
						getTableViewer().refresh();
					}
				});
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
