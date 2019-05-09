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
package org.wcs.smart.paws.ui;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.ui.config.ConfigEditorInput;
import org.wcs.smart.paws.ui.config.EditConfigHandler;
import org.wcs.smart.ui.SectionHeader;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Simple view for listing R scripts and queries
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class PawsView {

	public static final String ID = "org.wcs.smart.paws.view"; //$NON-NLS-1$

	private FormToolkit toolkit;
	
	private Composite content;
	
	private Composite resultsComposite;
	private Composite configComposite;
	
	private TableViewer tblConfigs;
	private TableViewer tblResults;
	
	private ToolBar tbConfig;
	private ToolBar tbResults;

	@Inject
	private IEclipseContext context;
	
	/**
	 * Default constructor
	 */
	public PawsView() {
		
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		refresh();
	}

	@Optional
	@Inject
	private void pawsConfigModified(@EventTopic(PawsEvent.PAWS_CONFIG_ALL) Object data){
		refresh();
	}
	
	@Optional
	@Inject
	private void pawsRunModified(@EventTopic(PawsEvent.PAWS_RUN_ALL) Object data){
		refresh();
	}
	
	@PreDestroy
	public void dispose() {		

	}
	
	private void refresh() {
		loadResults.schedule();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());

		
		Composite main = toolkit.createComposite(parent, SWT.BORDER);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;

		Composite headerMain = toolkit.createComposite(main, SWT.NONE);
		headerMain.setLayout(new GridLayout());
		headerMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerMain.getLayout()).marginWidth = 0;
		((GridLayout)headerMain.getLayout()).marginHeight = 0;
		
		SectionHeader header = new SectionHeader(headerMain, SWT.NONE,
				new String[] {"Results", "Configurations"},
				new Listener[] {
						e->{
							((StackLayout)content.getLayout()).topControl = resultsComposite;
							content.layout(true);
							tbConfig.setVisible(false);
							((GridData)tbConfig.getLayoutData()).widthHint = 0;
							
							tbResults.setVisible(true);
							((GridData)tbResults.getLayoutData()).widthHint = tbResults.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
							tbResults.getParent().layout(true);
						},
						e->{
							((StackLayout)content.getLayout()).topControl = configComposite;
							content.layout(true);
							tbConfig.setVisible(true);
							((GridData)tbConfig.getLayoutData()).widthHint = tbConfig.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
							tbResults.setVisible(false);
							((GridData)tbResults.getLayoutData()).widthHint = 0;
							tbResults.getParent().layout(true);
						}
				});
		
		((GridLayout)header.getLayout()).numColumns = 5;
		Label spacer = toolkit.createLabel(header, "", SWT.NONE); //$NON-NLS-1$
		spacer.setBackground(spacer.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		tbResults = new ToolBar(header, SWT.FLAT);
		tbResults.setBackground(header.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		tbResults.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		tbConfig = new ToolBar(header, SWT.FLAT );
		tbConfig.setBackground(header.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		tbConfig.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		tbResults.setVisible(false);
		tbConfig.setVisible(false);
		
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		content = toolkit.createComposite(main, SWT.NONE);
		content.setLayout(new StackLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		resultsComposite = createResultsPanel(content);
		configComposite = createConfigurationPanel(content);
		
		header.selectPanel(0);
		
		refresh();
	}
	
	
	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent){
	}
	
	@Focus
	public void setFocus() {
	}

	private Composite createResultsPanel(Composite parent) {
		Composite part = toolkit.createComposite(parent);
		part.setLayout(new GridLayout());
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		Composite tpart = toolkit.createComposite(part);
		tpart.setLayout(new TableColumnLayout());
		tpart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblResults = new TableViewer(tpart, SWT.FULL_SELECTION | SWT.MULTI );
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn col = new TableViewerColumn(tblResults, SWT.NONE);
		col.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof PawsRun) {
					PawsRun pc = (PawsRun)element;
					return pc.getId();
				}
				return super.getText(element);
			}
		});
		((TableColumnLayout)tpart.getLayout()).setColumnData(col.getColumn(),  new ColumnWeightData(1));
		tblResults.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblResults.addDoubleClickListener(e->editRun());
		
		Menu mnu = new Menu(tblResults.getTable());
		
		MenuItem mnuOpen = new MenuItem(mnu, SWT.PUSH);
		mnuOpen.setText("Open");
		mnuOpen.addListener(SWT.Selection, e->editRun());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setText("Re-Run");
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		mnuAdd.addListener(SWT.Selection, e->{
			Object x = tblResults.getStructuredSelection().getFirstElement();
			if (x instanceof PawsRun) {
				newRun( ((PawsRun)x).getConfiguration() );
			}
		});
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->deleteRun());
		
		tblResults.getTable().setMenu(mnu);
		
		ToolItem tiAdd = new ToolItem(tbResults, SWT.PUSH);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		tiAdd.setToolTipText("Re-run analysis");
		tiAdd.addListener(SWT.Selection, e->{
			Object x = tblResults.getStructuredSelection().getFirstElement();
			if (x instanceof PawsRun) {
				newRun( ((PawsRun)x).getConfiguration() );
			}
		});

		ToolItem tiDelete = new ToolItem(tbResults, SWT.PUSH);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.addListener(SWT.Selection, e->deleteRun());
		
		mnuOpen.setEnabled(false);
		mnuAdd.setEnabled(false);
		mnuDelete.setEnabled(false);
		tiAdd.setEnabled(false);
		tiDelete.setEnabled(false);
		
		tblResults.addSelectionChangedListener(e->{
			mnuOpen.setEnabled(!tblResults.getStructuredSelection().isEmpty());
			mnuAdd.setEnabled(!tblResults.getStructuredSelection().isEmpty());
			mnuDelete.setEnabled(!tblResults.getStructuredSelection().isEmpty());
			tiAdd.setEnabled(!tblResults.getStructuredSelection().isEmpty());
			tiDelete.setEnabled(!tblResults.getStructuredSelection().isEmpty());
		});
		return part;
	}
	
	
	private Composite createConfigurationPanel(Composite parent) {
		
		Composite part = toolkit.createComposite(parent);
		part.setLayout(new GridLayout());
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		Composite tpart = toolkit.createComposite(part);
		tpart.setLayout(new TableColumnLayout());
		tpart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblConfigs = new TableViewer(tpart, SWT.FULL_SELECTION | SWT.MULTI );
		tblConfigs.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn col = new TableViewerColumn(tblConfigs, SWT.NONE);
		col.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof PawsConfiguration) {
					PawsConfiguration pc = (PawsConfiguration)element;
					return pc.getName();
				}
				return super.getText(element);
			}
		});
		((TableColumnLayout)tpart.getLayout()).setColumnData(col.getColumn(),  new ColumnWeightData(1));
		tblConfigs.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblConfigs.addDoubleClickListener(e->{
			editConfiguration();
		});
		
		Menu mnu = new Menu(tblConfigs.getTable());
		
		MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addListener(SWT.Selection, e->newConfiguration());
		
		MenuItem mnuRun = new MenuItem(mnu, SWT.PUSH);
		mnuRun.setText("Run");
		mnuRun.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuRun.addListener(SWT.Selection, e->newRun(tblConfigs.getStructuredSelection().getFirstElement()));
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->editConfiguration());
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->deleteConfiguration());
		
		tblConfigs.getTable().setMenu(mnu);

		ToolItem tiAdd = new ToolItem(tbConfig, SWT.PUSH);
//		tiAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.addListener(SWT.Selection, e->newConfiguration());
		ToolItem tiEdit = new ToolItem(tbConfig, SWT.PUSH);
//		tiEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.addListener(SWT.Selection, e->editConfiguration());
		
		ToolItem tiDelete = new ToolItem(tbConfig, SWT.PUSH);
//		tiDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.addListener(SWT.Selection, e->deleteConfiguration());

		tblConfigs.addSelectionChangedListener(e->{
			mnuEdit.setEnabled(!tblConfigs.getStructuredSelection().isEmpty());
			mnuRun.setEnabled(!tblConfigs.getStructuredSelection().isEmpty());
			mnuDelete.setEnabled(!tblConfigs.getStructuredSelection().isEmpty());
			tiEdit.setEnabled(!tblConfigs.getStructuredSelection().isEmpty());
			tiDelete.setEnabled(!tblConfigs.getStructuredSelection().isEmpty());
		});

		mnuEdit.setEnabled(false);
		mnuRun.setEnabled(false);
		mnuDelete.setEnabled(false);
		tiEdit.setEnabled(false);
		tiDelete.setEnabled(false);
		
		return part;
	}
	
	private void newRun(Object config) {
		if (!(config instanceof PawsConfiguration)) return;
		try {
			ContextInjectionFactory.make(NewRunHandler.class, context).createAndRun((PawsConfiguration)config);
		}catch (Exception ex) {
			PawsPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	private void editRun() {
		Object x = tblResults.getStructuredSelection().getFirstElement();
		if (!(x instanceof PawsRun)) return;
		PawsRun pw = (PawsRun)x;
		(new ShowRunHandler()).execute(context.get(MWindow.class), pw);
	}
	
	private void deleteRun() {
		List<PawsRun> runs = new ArrayList<>();
		for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object c = (Object) iterator.next();
			if (c instanceof PawsRun) {
				runs.add((PawsRun)c);
			}
		}
		if (runs.isEmpty()) return;
		
		if (!MessageDialog.openConfirm(content.getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected results?  This action cannot be undone.", runs.size()))) {
			return;
		}
		
		try {
			PawsManager.INSTANCE.deleteRun(runs, context.get(IEventBroker.class));
		}catch (Exception ex) {
			PawsPlugIn.displayLog(ex.getMessage(), ex);
		}
		refresh();
	}
	
	private void newConfiguration() {
		(new EditConfigHandler()).execute(context.get(MWindow.class), new ConfigEditorInput(new PawsConfiguration()));
	}
	
	private void editConfiguration() {
		Object x = tblConfigs.getStructuredSelection().getFirstElement();
		if (!(x instanceof PawsConfiguration)) return;
		
		PawsConfiguration pw = (PawsConfiguration)x;
		(new EditConfigHandler()).execute(context.get(MWindow.class), new ConfigEditorInput(pw));
	}
	
	private void deleteConfiguration() {
		List<PawsConfiguration> configs = new ArrayList<>();
		for (Iterator<?> iterator = tblConfigs.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object c = (Object) iterator.next();
			if (c instanceof PawsConfiguration) {
				configs.add((PawsConfiguration)c);
			}
		}
		if (configs.isEmpty()) return;
		
		if (!MessageDialog.openConfirm(content.getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected configurations?  This action cannot be undone.", configs.size()))) {
			return;
		}
		
		try {
			PawsManager.INSTANCE.deleteConfigurations(configs, context.get(IEventBroker.class));
		}catch (Exception ex) {
			PawsPlugIn.displayLog(ex.getMessage(), ex);
		}
		refresh();
	}
    
	private Job loadResults = new Job("loading PAWS data") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<PawsRun> runs= new ArrayList<>();
			List<PawsConfiguration> configs = new ArrayList<>();
			
			try(Session s = HibernateManager.openSession()){
				runs.addAll(QueryFactory.buildQuery(s, PawsRun.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				configs.addAll(QueryFactory.buildQuery(s, PawsConfiguration.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				
			}
			
			configs.sort((a,b)->Collator.getInstance().compare(a.getName(),  b.getName()));
			runs.sort((a,b)->{
				if (a.getRunDate() == b.getRunDate()) return Collator.getInstance().compare(a.getId(), b.getId());
				if (a.getRunDate() == null) return -1;
				if (b.getRunDate() == null) return 1;
				return a.getRunDate().compareTo(b.getRunDate());
			});
			
			Display.getDefault().syncExec(()->{
				tblConfigs.setInput(configs);
				tblResults.setInput(runs);
			});
			return Status.OK_STATUS;
		}
		
	};
	
	public static class PawsViewWrapper extends DIViewPart<PawsView>{
		public PawsViewWrapper(){
			super(PawsView.class);
		}
	}
	
}