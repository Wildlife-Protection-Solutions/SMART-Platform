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
package org.wcs.smart.entity.ui.editor;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.query.EntitySightingQuery;
import org.wcs.smart.entity.query.SightingPagedResults;
import org.wcs.smart.entity.query.SightingQueryColumn;
import org.wcs.smart.entity.ui.editor.sightings.EntityFilterComposite;
import org.wcs.smart.entity.ui.editor.sightings.SightingTable;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.importexport.ExportQueryWizard;
import org.wcs.smart.ui.properties.DialogConstants;
/**
 * Sightings editor page that allows users to perform simple queries
 * for entity sightings.
 * 
 * @author Emily
 *
 */
public class SightingPage extends EditorPart implements IEntityTypeEditorPage {


	private EntityTypeEditor parentEditor;
	private EntityFilterComposite entityFilter;
	private SightingTable sightingTable;
	private QueryDateFilterComposite dateComp ;
	private EntitySightingQuery currentQuery;
	
	private Label lblQueryProgress;
	
	/**
	 * Creates a new sighting page
	 * @param editor
	 */
	public SightingPage(EntityTypeEditor editor){
		this.parentEditor = editor;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public EntitySightingQuery getCurrentQuery(){
		return this.currentQuery;
	}
	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(Display.getCurrent());
		toolkit.setBorderStyle(SWT.BORDER);
		
		Form form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		GridLayout glayout = new GridLayout();
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		form.getBody().setLayout(glayout);
		form.setText(MessageFormat.format(Messages.SightingPage_SightingPageName, new Object[]{getEditorInput().getName()}));
		
		Section sec = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.CLIENT_INDENT);		
		sec.setText(Messages.SightingPage_FiltersLabel);
		sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		sec.setLayout(new GridLayout());
		
		
		Composite g = toolkit.createComposite(sec);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sec.setClient(g);
		toolkit.createLabel(g, Messages.SightingPage_DateFilterLabel);
		
		dateComp = new QueryDateFilterComposite(g, null, SightingQueryColumn.SIGHTING_DATE_FILTERS);
		dateComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		dateComp.adapt(toolkit);
		
		toolkit.createLabel(g, Messages.SightingPage_EntityFilterLabel);
		
		entityFilter = new EntityFilterComposite(g);
		entityFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		entityFilter.adapt(toolkit);
		
		
		Button btnRefresh = toolkit.createButton(g, Messages.SightingPage_ReloadButtonLabel, SWT.PUSH);
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateResultsTable();
			}

		});
		
		lblQueryProgress = toolkit.createLabel(g, ""); //$NON-NLS-1$
		lblQueryProgress.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		sec = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.CLIENT_INDENT);		
		sec.setText(Messages.SightingPage_QueryResultSectionTitle);
		sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sec.setLayout(new GridLayout());
			
		Composite compSighting = toolkit.createComposite(sec);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 10;
		gl.marginWidth = 0;
		compSighting.setLayout(gl);
		compSighting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sec.setClient(compSighting);
		
		sightingTable = new SightingTable(compSighting);
		sightingTable.getTable().getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(sightingTable.getTable().getTable());
		
		Hyperlink exportLink = toolkit.createHyperlink(compSighting, DialogConstants.EXPORT_BUTTON_TEXT, SWT.NONE);
		exportLink.setToolTipText(Messages.SightingPage_ExportTooltip);
		exportLink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		exportLink.addHyperlinkListener(new IHyperlinkListener() {
			
			@Override
			public void linkExited(HyperlinkEvent e) {
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (currentQuery == null){
					MessageDialog.openError(getSite().getShell(), Messages.SightingPage_ErrorDialogTitle, Messages.SightingPage_NothingToExport);
					return;
				}
				ExportQueryWizard wizard = new ExportQueryWizard(currentQuery);
				WizardDialog dialog = new WizardDialog(parentEditor.getSite().getShell(), wizard);
				dialog.open();
			}
		});
		
		parentEditor.getSite().setSelectionProvider(sightingTable.getTable());
	}

	
	
	private void updateResultsTable(){
		currentQuery = new EntitySightingQuery(parentEditor.getEntityType(),
			dateComp.getDateFilter(), entityFilter.getFilter());
		currentQuery.setQueryColumns(sightingTable.getCurrentColumns());
		
		runJob.cancel();
		runJob.schedule();
	}
	
	
	
	@Override
	public void setFocus() {
		
	}
	

	@Override
	public void updatePage(Session currentSession, boolean typeModified) {
		entityFilter.setEntities(parentEditor.getEntities(currentSession));
		
		if (typeModified){
			sightingTable.setEntityType(parentEditor.getEntityType());
			currentQuery = null;
			//clear query results
			sightingTable.setInput(null);
		}
	}

	
	private Job runJob = new Job(Messages.SightingPage_ExecuteJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					sightingTable.setInput(null);
				}});
			
			
			
			IProgressMonitor lblProgressMonitor = new ProgressMonitorWrapper(monitor) {
				private String tName = ""; //$NON-NLS-1$
				public void beginTask(String name, int totalWork) {
					super.beginTask(name, totalWork);
					tName = name;
					updateLabel(name);
				}
				
				public void setTaskName(String name) {
					super.setTaskName(name);
					tName = name;
					updateLabel(name);
				}

				public void subTask(String name) {
					super.subTask(name);
					updateLabel(tName + " (" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				public void done(){
					super.done();
					updateLabel(""); //$NON-NLS-1$
				}
				private void updateLabel(final String text){
					Display.getDefault().asyncExec(new Runnable(){

						@Override
						public void run() {
							lblQueryProgress.setText(text);
						}});
				}
			};
			
			EntitySightingQuery query = currentQuery;
			try{
				final SightingPagedResults results = (SightingPagedResults) query.executeQuery(lblProgressMonitor, null);
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						sightingTable.setInput(results);
						if (parentEditor.getMapPage() != null){
							parentEditor.getMapPage().updatePage(null, false);
						}
					}});
				
			}catch (Exception ex){
				EntityPlugIn.displayLog(Messages.SightingPage_QueryError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			}
			lblProgressMonitor.done();
			return Status.OK_STATUS;
		}
		
	};
}
