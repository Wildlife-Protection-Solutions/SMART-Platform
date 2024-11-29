package org.wcs.smart.query.ui;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.QueryColumnConfigurationManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryColumnConfiguration;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

public class QueryColumnConfigurationListDialog extends SmartStyledTitleDialog{

	protected QueryColumnConfigurationListDialog(Shell parent) {
		super(parent);
	}

	private TableViewer tblConfigurations;
	private TableColumn sortColumn;
	private int sortDirection = 1;

	private ViewerComparator sorter = new ViewerComparator(){
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			 if (sortColumn == null) return 0;
			 
			 String s1 = getValue(sortColumn, e1);
			 String s2 = getValue(sortColumn, e2);
			 if (s1==null) s1 = ""; //$NON-NLS-1$
			 if (s2==null) s2 = ""; //$NON-NLS-1$
			 return Collator.getInstance().compare(s1,s2) * sortDirection;
		}
	};
	
	public enum TableColumn{
		QUERY_TYPE(Messages.QueryColumnConfigurationListDialog_QueryTypeOp, 250),
		NAME(Messages.QueryColumnConfigurationListDialog_ConfigurationName, 400);
		
		public String guiName;
		public int width;
		TableColumn(String guiName, int width){
			this.guiName = guiName;
			this.width = width;
		}
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	@Override
	public Point getInitialSize(){
		return new Point(800, 400);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
	
		Composite tableComp = new Composite(parent, SWT.NONE);
		tableComp.setLayout(new GridLayout(2, false));
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblConfigurations = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		tblConfigurations.setContentProvider(ArrayContentProvider.getInstance());
		tblConfigurations.setInput(new String[]{DialogConstants.LOADING_TEXT});
		tblConfigurations.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		for (TableColumn c : TableColumn.values()){
			TableViewerColumn typeColumn = new TableViewerColumn(tblConfigurations, SWT.NONE);
			typeColumn.getColumn().setText(c.guiName);
			typeColumn.getColumn().setMoveable(true);
			
			typeColumn.setLabelProvider(new ColumnLabelProvider(){
				@Override
				public String getText(Object element) {
					return getValue(c, element);
				}
				@Override
				public Image getImage(Object element) {
					return getColumnImage(c, element);
				}
			});
			
			typeColumn.getColumn().setWidth(c.width);
			
			typeColumn.getColumn().addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (c == sortColumn){
						sortDirection = sortDirection * -1;
					}else{
						sortColumn = c;
					}
					tblConfigurations.getTable().setSortColumn(typeColumn.getColumn());
					tblConfigurations.getTable().setSortDirection(sortDirection == 1 ? SWT.UP : SWT.DOWN);
					tblConfigurations.refresh();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) { }
			});
		}
//		tblRoutines.getTable().setLinesVisible(true);
		tblConfigurations.setComparator(sorter);
		tblConfigurations.getTable().setHeaderVisible(true);
		tblConfigurations.addDoubleClickListener(event->edit());
		
		Composite buttonPnl = new Composite(tableComp, SWT.NONE);
		buttonPnl.setLayout(new GridLayout());
		((GridLayout)buttonPnl.getLayout()).marginWidth = 0;
		((GridLayout)buttonPnl.getLayout()).marginHeight = 0;
		buttonPnl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnNew = new Button(buttonPnl, SWT.PUSH);
		btnNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnNew.addListener(SWT.Selection, e->add());
		btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnEdit = new Button(buttonPnl, SWT.PUSH);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addListener(SWT.Selection, e->edit());
		btnEdit.setEnabled(false);
		
		Button btnDelete = new Button(buttonPnl, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.addListener(SWT.Selection, e->delete());
		btnDelete.setEnabled(false);
		
		Menu mnu = new Menu(tblConfigurations.getControl());
		tblConfigurations.getControl().setMenu(mnu);
		MenuItem mnuNew = new MenuItem(mnu, SWT.PUSH);
		mnuNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuNew.addListener(SWT.Selection, e->add());
		
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->edit());
		mnuEdit.setEnabled(false);
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->delete());
		mnuDelete.setEnabled(false);

		tblConfigurations.addSelectionChangedListener(e->{
			
			Object x = tblConfigurations.getStructuredSelection().getFirstElement();
			boolean enabled = x instanceof QueryColumnConfiguration;
			
			mnuDelete.setEnabled(enabled);
			mnuEdit.setEnabled(enabled);
			btnDelete.setEnabled(enabled);
			btnEdit.setEnabled(enabled);
			
		});
		
		refresh();
		
		getShell().setText(Messages.QueryColumnConfigurationListDialog_Title);
		setTitle(Messages.QueryColumnConfigurationListDialog_Title);
		setMessage(Messages.QueryColumnConfigurationListDialog_Message);
		return tableComp;
	}
	
	private QueryColumnConfiguration getSelectedConfiguration() {
		if (tblConfigurations.getSelection().isEmpty()) return null;
		Object x = tblConfigurations.getStructuredSelection().getFirstElement();
		if (x instanceof QueryColumnConfiguration config) {
			return config;
		}
		return null;
	}
	private void add(){
		QueryColumnConfiguration config = new QueryColumnConfiguration();
		config.setConservationArea(SmartDB.getCurrentConservationArea());
		config.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), Messages.QueryColumnConfigurationListDialog_DefaultName);
		config.updateName(SmartDB.getCurrentLanguage(), Messages.QueryColumnConfigurationListDialog_DefaultName);
		config.setName(Messages.QueryColumnConfigurationListDialog_DefaultName);
		
		QueryColumnConfigurationDialog dialog = new QueryColumnConfigurationDialog(getShell(), config);
		dialog.open();
		refresh();
		
	}
	
	private void edit(){
		QueryColumnConfiguration toEdit = getSelectedConfiguration();
		if (toEdit == null) return;
		QueryColumnConfigurationDialog dialog = new QueryColumnConfigurationDialog(getShell(), toEdit);
		dialog.open();
		refresh();		
	}
	
	private void delete(){
		QueryColumnConfiguration toDelete = getSelectedConfiguration();
		if (toDelete == null) return;
		
		if (!MessageDialog.openConfirm(getShell(), 
				DialogConstants.DELETE_DIALOG_TITLE,
				MessageFormat.format(Messages.QueryColumnConfigurationListDialog_DeleteConfirm, toDelete.getName()))){
			return;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				QueryColumnConfigurationManager.INSTANCE.deleteConfiguration(toDelete, session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				QueryPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		refresh();
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private Image getColumnImage(TableColumn field, Object element) {
		if (element == null) return null; 
		if (!(element instanceof QueryColumnConfiguration qc)) return null;
		if (field != TableColumn.QUERY_TYPE) return null;
		return QueryTypeManager.INSTANCE.findQueryType(qc.getQueryTypeKey()).getImage();
	}
	
	private String getValue(TableColumn field, Object element){
		if (element == null) return ""; //$NON-NLS-1$
		if (!(element instanceof QueryColumnConfiguration qc)) return element.toString();
		switch(field){
			case NAME:
				return qc.getName();
			case QUERY_TYPE:
				return QueryTypeManager.INSTANCE.findQueryType(qc.getQueryTypeKey()).getGuiName();
		}
		return ""; //$NON-NLS-1$
	}

	
	private void refresh(){
		tblConfigurations.setInput(new String[]{DialogConstants.LOADING_TEXT});
		refreshJob.setSystem(true);
		refreshJob.schedule();
	}
	
	private Job refreshJob = new Job("refresh"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<QueryColumnConfiguration> configs = null;
			try(Session s = HibernateManager.openSession()){
				configs = QueryColumnConfigurationManager.INSTANCE.getColumnConfigurations(SmartDB.getCurrentConservationArea(), s);
				Collections.sort(configs);
			}
			
			final List<QueryColumnConfiguration> fconfigs = configs;
			Display.getDefault().asyncExec(()->{
				if (tblConfigurations.getControl().isDisposed()) return;
				tblConfigurations.setInput(fconfigs);
			});
			return Status.OK_STATUS;
		}
		
	};

}
