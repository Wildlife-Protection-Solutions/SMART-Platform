/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.preference;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileRecordSource;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.dialogs.RecordSourceDialog;
import org.wcs.smart.i2.ui.handler.EditRecordTemplateHandler;
import org.wcs.smart.ui.NamedItemViewerFilter;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Dialog for listing entity types.
 * @author Emily
 *
 */
public class RecordsPreferencePage extends PreferencePage implements IIntelPreferencePage {

	@Inject
	private IEventBroker broker;
	@Inject
	private IEclipseContext context;
	
	private TableViewer cmbSources;

	private List<IntelRecordSource> sources = null;
	private NamedItemViewerFilter filter;
	private IStructuredSelection currentSelection;
	
	private MenuItem mnuEdit;
	private MenuItem mnuAdd;
	private MenuItem mnuDelete;
	
	private Button btnNew;
	private Button btnEdit;
	private Button btnDelete;
	 
	private ColumnLabelProvider lblProvider = new ColumnLabelProvider() {
		RecordSourceLabelProvider src = new RecordSourceLabelProvider();
		public String getText(Object element) { return src.getText(element); }
		public Image getImage(Object element) { return src.getImage(element); }
		public void dispose() { super.dispose(); src.dispose(); }
	};
	
	private Job loadTypes = new Job("load record sources"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			sources = null;
			List<IntelProfile> profiles = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				List<IntelRecordSource> srcs = RecordManager.INSTANCE.getSources(session); 
				srcs.forEach(e -> {
					e.getNames().size();
					e.getProfiles().size();
					if (e.getAttributes() != null){
						e.getAttributes().forEach(a->{
							a.getNames().size();
							a.getAttribute();
							a.getEntityType();
						});
					}
				});
				sources = srcs;
				
				profiles.addAll(ProfilesManager.INSTANCE.getProfiles(session, false));
				for (IntelProfile p : profiles) p.getName();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (cmbSources.getControl().isDisposed()) return;
					
					TableColumnLayout tlayout = (TableColumnLayout) cmbSources.getControl().getParent().getLayout(); 
					
					for (TableColumn c : cmbSources.getTable().getColumns()) c.dispose();
					
					TableViewerColumn col = new TableViewerColumn(cmbSources, SWT.NONE);
					col.setLabelProvider(lblProvider);
					col.getColumn().setText(Messages.RecordsPreferencePage_SourceColName);
					tlayout.setColumnData(col.getColumn(), new ColumnWeightData(3));
					
					ProfileLabelProvider temp = new ProfileLabelProvider();
					cmbSources.getTable().addListener(SWT.Dispose, e->temp.dispose());
					
					for (IntelProfile p : profiles) {
						TableViewerColumn col2 = new TableViewerColumn(cmbSources, SWT.NONE);
						col2.getColumn().setText(p.getName());
						col2.getColumn().setToolTipText(p.getName());
						col2.getColumn().setWidth(30);	
						col2.getColumn().setImage(temp.getImage(p));
						col2.setLabelProvider(new ColumnLabelProvider() {
							
							@Override
							public String getText(Object element) {
								if (element instanceof IntelRecordSource) return ""; //$NON-NLS-1$
								return super.getText(element);
							}
							@Override
							public Image getImage(Object element) {
								if (element instanceof IntelRecordSource) {
									for(IntelProfileRecordSource s : ((IntelRecordSource)element).getProfiles()) {
										if (s.getProfile().equals(p)) {
											return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CHECK);	
										}
									}
									return null;
								}
								return super.getImage(element);
							}
						});
						tlayout.setColumnData(col2.getColumn(), new ColumnWeightData(1));
					}
					
					cmbSources.setInput(sources);
					cmbSources.setSelection(currentSelection);
					cmbSources.getControl().getParent().layout(true);
				}
			});
			return Status.OK_STATUS;
		}
		
	};

	public RecordsPreferencePage() {
		super();
		noDefaultAndApplyButton();
		setTitle(Messages.RecordsPreferencePage_Title);
	}

	@Override
	protected Control createContents(Composite parent) {
		
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FilterComposite typeFilter = new FilterComposite(parent, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				filter.setFilterString(typeFilter.getPatternFilter());
			}
		});
		Label l = new Label(parent, SWT.NONE);
		l.setVisible(false);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Composite tablecomp = new Composite(parent, SWT.NONE);
		tablecomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableColumnLayout tlayout = new TableColumnLayout();
		tablecomp.setLayout(tlayout);
		
		
		cmbSources = new TableViewer(tablecomp, SWT.BORDER | SWT.FULL_SELECTION);
		cmbSources.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbSources.getTable().setHeaderVisible(true);
		cmbSources.setContentProvider(ArrayContentProvider.getInstance());
		cmbSources.setInput(new String[]{DialogConstants.LOADING_TEXT});
		
		TableViewerColumn col = new TableViewerColumn(cmbSources, SWT.NONE);
		col.setLabelProvider(lblProvider);
		
		tlayout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		
		cmbSources.getControl().setFocus();
		cmbSources.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		cmbSources.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnEdit.setEnabled(!cmbSources.getSelection().isEmpty());
				btnDelete.setEnabled(!cmbSources.getSelection().isEmpty());
				mnuEdit.setEnabled(!cmbSources.getSelection().isEmpty());
				mnuDelete.setEnabled(!cmbSources.getSelection().isEmpty());
			}
		});
		
		filter = new NamedItemViewerFilter(cmbSources);
		cmbSources.setFilters(new ViewerFilter[]{filter});
		
		Composite buttonPanel = new Composite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		
		btnNew = new Button(buttonPanel, SWT.PUSH);
		btnNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnNew.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}
		});
		btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
		});
		
		btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				delete();
			}
		});
		
		
		Hyperlink hk = new Hyperlink(parent, SWT.NONE);
		hk.setText(Messages.RecordSourceAttributeDialog_EditTemplateLink);
		hk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		hk.setUnderlined(true);
		hk.setForeground(hk.getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND));
		
		hk.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((IntelPreferenceDialog)getContainer()).close();
				(new EditRecordTemplateHandler()).execute();
				
			}
		});
		
		Menu menu = new Menu(cmbSources.getControl());
		cmbSources.getControl().setMenu(menu);

		mnuAdd = new MenuItem(menu, SWT.DEFAULT);
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}
		});
		
		mnuEdit = new MenuItem(menu, SWT.DEFAULT);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RENAME_ICON));
		mnuEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
		});
		mnuDelete = new MenuItem(menu, SWT.DEFAULT);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				delete();
			}
		});
		
		btnNew.setEnabled(true);
		btnEdit.setEnabled(false);
		btnDelete.setEnabled(false);
		mnuAdd.setEnabled(true);
		mnuEdit.setEnabled(false);
		mnuDelete.setEnabled(false);
		
		setMessage(Messages.RecordsPreferencePage_Message);
		setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_RECORD));

		loadTypes.setSystem(true);
		loadTypes.schedule();
		SmartUiUtils.makeTransparent(parent);

		return parent;
	}

	
	private void add(){
		IntelRecordSource newItem = new IntelRecordSource();
		newItem.setAttributes(new ArrayList<>());
		newItem.setProfiles(new HashSet<>());
		newItem.setConservationArea(SmartDB.getCurrentConservationArea());
		newItem.setName(""); //$NON-NLS-1$
		newItem.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), newItem.getName());
		openDialog(newItem);
	}
	
	private void openDialog(IntelRecordSource type){
		RecordSourceDialog dialog = new RecordSourceDialog(getShell(), type);
		ContextInjectionFactory.inject(dialog, context);
		cmbSources.setInput(new String[]{DialogConstants.LOADING_TEXT});

		dialog.open();
		refresh();
	}
	
	private void edit(){
		Object x = ((IStructuredSelection)cmbSources.getSelection()).getFirstElement();
		if (x instanceof IntelRecordSource){
			IntelRecordSource type = (IntelRecordSource)x;
			openDialog(type);
		}
	}
	
	private void delete(){
		
		StructuredSelection s = (StructuredSelection) cmbSources.getSelection();
		List<IntelRecordSource> delete = new ArrayList<IntelRecordSource>();
		
		for (Iterator<?> iterator = s.iterator(); iterator.hasNext();) {
			Object x = (Object)iterator.next();
			if (x instanceof IntelRecordSource){
				delete.add((IntelRecordSource)x);
			}
		}
		
		
		if (delete.isEmpty()) return;
		if (!MessageDialog.openConfirm(getShell(), Messages.RecordSourceAttributeDialog_DeleteSourceDialogTitle, MessageFormat.format(Messages.RecordSourceAttributeDialog_DeleteSourceDialogMsg, delete.size()))){
			return;
		}

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {

					monitor.beginTask(Messages.RecordsPreferencePage_DeleteTaskName, delete.size());
					List<IntelRecordSource> deleted = new ArrayList<>();
					try(Session session = HibernateManager.openSession()){

						for (IntelRecordSource source : delete){
							monitor.subTask(source.getName());
							
							try {
								if (!DeleteManager.canDelete(source, session)) {
									throw new Exception(Messages.RecordsPreferencePage_DeleteError);
								}
							}catch (Exception ex) {
								Display.getDefault().syncExec(()->{
									MessageDialog.openError(btnNew.getShell(), Messages.RecordsPreferencePage_ErrorTitle, MessageFormat.format(Messages.RecordsPreferencePage_DeleteErrorMsg, source.getName(), ex.getMessage()));
								});
								continue;
							}
							
							session.beginTransaction();
							try{
								if (source.getUuid() == null) continue;
								
								//delete all attributes for records with this source
								Query<?> q2 = session.createQuery("DELETE FROM IntelRecordAttributeValueList where id.value IN (SELECT a FROM IntelRecordAttributeValue a join a.record b WHERE b.recordSource = :source)"); //$NON-NLS-1$
								q2.setParameter("source", source); //$NON-NLS-1$
								q2.executeUpdate();
								
								Query<?> q1 = session.createQuery("DELETE FROM IntelRecordAttributeValue where record IN (FROM IntelRecord WHERE recordSource = :source)"); //$NON-NLS-1$
								q1.setParameter("source", source); //$NON-NLS-1$
								q1.executeUpdate();
								
								//update intelligence records to have no source
								Query<?> q = session.createQuery("UPDATE IntelRecord SET recordSource = null WHERE recordSource = :source"); //$NON-NLS-1$
								q.setParameter("source", source); //$NON-NLS-1$
								q.executeUpdate();
								
								//remove all attributes associated with source
								for (IntelRecordSourceAttribute a : source.getAttributes()){
									//delete any values associated with this attribute 
									q = session.createQuery("DELETE FROM IntelRecordAttributeValue a where a.attribute = :attribute "); //$NON-NLS-1$
									q.setParameter("attribute", a); //$NON-NLS-1$
									q.executeUpdate();
									//delete the attributes
									session.delete(a);
								}
								//delete source
								session.delete(source);
								
								session.getTransaction().commit();
								deleted.add(source);
							}catch(Exception ex){
								session.getTransaction().rollback();
								Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.EntityTypeListDialog_DeleteError, source.getName(), ex.getMessage()), ex);
							}
							monitor.worked(1);
						}
					}
					monitor.done();
					broker.send(IntelEvents.RECORD_SOURCE_ALL, null);
					 
				}
			});
		} catch (Exception e) {
			Intelligence2PlugIn.displayLog(Messages.EntityTypeListDialog_DeleteError2 +e.getMessage(), e);
		}
		refresh();
		
	}
	
	@Override
	public void refresh(){
		if (cmbSources.getControl().isDisposed()) return;
		for (TableColumn c : cmbSources.getTable().getColumns()) c.dispose();
		currentSelection = (IStructuredSelection) cmbSources.getSelection();
		cmbSources.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadTypes.schedule(0);
	}
	
	


	
}
