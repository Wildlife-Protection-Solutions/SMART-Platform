/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.i2.patrol.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.patrol.PatrolProfilePlugIn;
import org.wcs.smart.i2.patrol.internal.Messages;
import org.wcs.smart.i2.patrol.model.PatrolMotivatedRecord;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;
import org.wcs.smart.ui.ShowPerspectiveHandler;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Patrol contribution for link between patrol and intel record
 */
public class PatrolEditorContribution implements IPatrolEditorContribution {

	public PatrolEditorContribution() {
	}

	private TableViewer tblViewer;
	
	private Patrol patrol;
	
	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent, boolean canEdit) {

		Composite part = toolkit.createComposite(parent,SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		if (!IntelSecurityManager.INSTANCE.canViewRecordAny()) {
			Label l = new Label(part, SWT.NONE);
			l.setText(IntelligenceLabelProviderImpl.INSUFFICIENT_PRIVILEGES);
		}else {
			Composite tableComp = new Composite(part, SWT.NONE);
			tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			TableColumnLayout tlayout = new TableColumnLayout();
			tableComp.setLayout(tlayout);
			tblViewer = new TableViewer(tableComp, SWT.BORDER);
			tblViewer.setContentProvider(ArrayContentProvider.getInstance());
			
			TableViewerColumn col = new TableViewerColumn(tblViewer, SWT.NONE);
			tlayout.setColumnData(col.getColumn(), new ColumnWeightData(1));
			col.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					if (element instanceof PatrolMotivatedRecord) {
						return ((PatrolMotivatedRecord) element).getId().getIntelRecord().getTitle();
					}
					return super.getText(element);
				}
				
				public Image getImage(Object element) {
					if (element instanceof PatrolMotivatedRecord) {
						return Resources.INSTANCE.getImage(((PatrolMotivatedRecord) element).getId().getIntelRecord().getRecordSource());
					}
					return super.getImage(element);
				}
			});
			tblViewer.addDoubleClickListener(evt->{
				Object item = tblViewer.getStructuredSelection().getFirstElement();
				if (!(item instanceof PatrolMotivatedRecord)) return;
				PatrolMotivatedRecord record = (PatrolMotivatedRecord)item;
				
				(new ShowPerspectiveHandler()).execute(IntelDataAssessmentPerspective.ID, ((IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class)).getActive(MWindow.class));
				(new OpenRecordHandler()).openRecord(record.getId().getIntelRecord(), false);
			});
			
			Menu mnu = new Menu(tblViewer.getControl());
			
			MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
			miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
			miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			miAdd.addListener(SWT.Selection,e->addLink());
			
			MenuItem miRemove = new MenuItem(mnu, SWT.PUSH);
			miRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
			miRemove.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miRemove.addListener(SWT.Selection,e->removeLink());
			
			mnu.addMenuListener(new MenuListener() {			
				@Override
				public void menuShown(MenuEvent e) {
					boolean hasItem = (tblViewer.getStructuredSelection().getFirstElement() instanceof PatrolMotivatedRecord);
					miRemove.setEnabled(hasItem);
				}
				
				@Override
				public void menuHidden(MenuEvent e) {
				}
			});
			
			
			ToolBar tbar = new ToolBar(part, SWT.FLAT | SWT.VERTICAL);
			tbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			ToolItem tiAdd = new ToolItem(tbar, SWT.PUSH);
			tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			tiAdd.addListener(SWT.Selection,e->addLink());
			ToolItem tiRemove = new ToolItem(tbar, SWT.PUSH);
			tiRemove.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			tiRemove.addListener(SWT.Selection,e->removeLink());
			
			tblViewer.getControl().setMenu(mnu);
		}
		
		return part;
	}

	private void addLink() {
		//need to show a dialog of records for this conservation area
		//maybe we a date filter?
		RecordSelectorDialog dialog = new RecordSelectorDialog(tblViewer.getControl().getShell());
		if (dialog.open() != Window.OK) return;

		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				String query = "FROM PatrolMotivatedRecord WHERE id.patrol = :patrol"; //$NON-NLS-1$
				List<PatrolMotivatedRecord> current = session.createQuery(query, PatrolMotivatedRecord.class)
						.setParameter("patrol",  patrol) //$NON-NLS-1$
						.list();
				
				for (IntelRecord r : dialog.getSelection()) {
					PatrolMotivatedRecord record = new PatrolMotivatedRecord();
					record.getId().setPatrol(patrol);
					record.getId().setIntelRecord(r);
					if (!current.contains(record)) session.persist(record);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				PatrolProfilePlugIn.displayLog(MessageFormat.format(Messages.PatrolEditorContribution_AddError, ex.getMessage()), ex);
			}
		}
		refresh();
		
	}
	
	private void removeLink() {
		Object item = tblViewer.getStructuredSelection().getFirstElement();
		if (!(item instanceof PatrolMotivatedRecord)) return;
		PatrolMotivatedRecord record = (PatrolMotivatedRecord)item;
		
		if (!MessageDialog.openQuestion(tblViewer.getControl().getShell(), 
				DialogConstants.DELETE_BUTTON_TEXT, 
				MessageFormat.format(Messages.PatrolEditorContribution_ConfirmRemove, record.getId().getIntelRecord().getTitle()))){
			return;
		}
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.remove(record);
				session.getTransaction().commit();
			}catch (Exception ex) {
				if (session.getTransaction().isActive())session.getTransaction().rollback();
				PatrolProfilePlugIn.displayLog(MessageFormat.format(Messages.PatrolEditorContribution_DeleteError,  ex.getMessage()), ex);
			}
		}
		refresh();
		
	}
	@Override
	public String getName() {
		return Messages.PatrolEditorContribution_PatrolMotiviated;
	}

	
	@Override
	public void setPatrol(Patrol patrol) {
		this.patrol = patrol;
		refresh();
	}

	
	private void refresh() {
		if (tblViewer == null || tblViewer.getControl().isDisposed()) return;
		tblViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		updateValues.schedule();	
	}
	private Job updateValues = new Job("loading") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> records = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				String query = "FROM PatrolMotivatedRecord WHERE id.patrol = :patrol"; //$NON-NLS-1$
				List<PatrolMotivatedRecord> items = session.createQuery(query, PatrolMotivatedRecord.class)
						.setParameter("patrol",  patrol) //$NON-NLS-1$
						.list();
				items.forEach(r->{
					r.getId().getIntelRecord().getTitle();
					if (r.getId().getIntelRecord().getRecordSource() != null) r.getId().getIntelRecord().getRecordSource().getName();
				});
				for (int i = 0; i < items.size(); i ++) {
					if (IntelSecurityManager.INSTANCE.canViewRecords( ((PatrolMotivatedRecord)items.get(i)).getId().getIntelRecord().getProfile() )){
						records.add(items.get(i));
					}else {
						records.add(IntelligenceLabelProviderImpl.INSUFFICIENT_PRIVILEGES);
					}
				}
				
			}
			Display.getDefault().asyncExec(()->{
				if (tblViewer.getTable().isDisposed()) return;
				tblViewer.setInput(records);
			});
			return Status.OK_STATUS;
		}
		
	};
}
