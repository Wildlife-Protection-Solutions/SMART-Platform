package org.wcs.smart.i2.ui.views.entity.search;

import java.text.DateFormat;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.entity.search.AllEntityContentProvider.EntityTableData;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.FilterComposite;

public class AllPanel extends Composite {

	private static final String BASIC_ALLTYPES_OP = Messages.EntitySearchView_AllTypesOption;

	@Inject
	private IEclipseContext context;
	
	private EntitySearchView view;

	private FilterComposite txtSearch;
	private TableComboViewer cmbEntityType;
	
	private Composite tableComposite;
	private FormToolkit toolkit;
	
	public AllPanel(Composite parent, EntitySearchView view, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.view = view;
		this.toolkit = toolkit;
		createContents();
		addDisposeListener(e->toolkit.dispose());
	}

	private void createContents() {
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		Button btn = toolkit.createButton(this, "SETUP", SWT.PUSH);
		btn.addListener(SWT.Selection, e->{
			refresh();
		});
	
		tableComposite = toolkit.createComposite(this);
		tableComposite.setLayout(new GridLayout());
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)tableComposite.getLayout()).marginWidth = 0;
		((GridLayout)tableComposite.getLayout()).marginHeight = 0;
	}
	
	public void refresh() {
		for (Control c : tableComposite.getChildren()) c.dispose();
		toolkit.createLabel(tableComposite, "Loading....");
		tableComposite.layout();
		
		refreshJob.schedule();
	}

	
	private Job refreshJob = new Job("refresh entity table") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			AllEntityContentProvider provider = new AllEntityContentProvider();
			EntityTableData data = provider.generateData();
			if (data == null) {
				Display.getDefault().syncExec(()->{
					toolkit.createLabel(tableComposite, "ERROR");
					tableComposite.layout(true);
				});
				return Status.OK_STATUS;

			}
			Display.getDefault().syncExec(()->{
				for (Control c : tableComposite.getChildren()) c.dispose();

				final TableViewer entityTable = new TableViewer(tableComposite, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION);
				entityTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				entityTable.setContentProvider(provider);
				entityTable.getTable().setHeaderVisible(true);
				entityTable.getTable().setLinesVisible(true);
				entityTable.addDoubleClickListener(new IDoubleClickListener() {
					
					@Override
					public void doubleClick(DoubleClickEvent event) {
						
						Object item = entityTable.getStructuredSelection().getFirstElement();
						if (item == null) return;
						if (!(item instanceof EntityTableRowItem)) return;
						
						UUID entityUuid = ((EntityTableRowItem)item).getEntityUuid();
						IntelEntity temp = null;
						try(Session session = HibernateManager.openSession()){
							temp = session.get(IntelEntity.class, entityUuid);
							if (temp != null) {
								temp.getIdAttributeAsText();
								temp.getEntityType();
							}
						}
						if (temp == null) {
							MessageDialog.openInformation(context.get(Shell.class), "Not Found", "Entity Not Found.");
							return;
						}
						(new OpenEntityHandler()).openEntity(temp, context);
						
					}
				});
				
				TableViewerColumn idColumn = new TableViewerColumn(entityTable, SWT.NONE);
				idColumn.getColumn().setText("Entity ID");
				idColumn.getColumn().pack();
				
				idColumn.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element == null) return "...";
						String keyId = ((EntityTableRowItem)element).getId();
						
						Object value =  ((EntityTableRowItem)element).getAttributeValue(keyId);
						IntelAttribute attribute = null;
						for (IntelAttribute aa : data.getAttributes()) {
							if (aa.getKeyId().equals(keyId)) {
								attribute = aa;
								break;
							}
						}
						if (value == null || attribute == null) return "";
						switch(attribute.getType()) {
						case BOOLEAN:
							if (((Double)value) < 0.5) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
							return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
						case DATE:
							return DateFormat.getDateInstance().format(java.sql.Date.valueOf((String)value));
						case EMPLOYEE:
						case LIST:
						case NUMERIC:
						case POSITION:
						case TEXT:
							return value.toString();
						}
						
						return ((EntityTableRowItem)element).getId();
					}
				});
				
				TableViewerColumn typeColumn = new TableViewerColumn(entityTable, SWT.NONE);
				typeColumn.getColumn().setText("Entity Type");
				typeColumn.getColumn().pack();
				typeColumn.getColumn().addListener(SWT.Selection, e->{
					provider.setSortColumn(AllEntityContentProvider.COL_ENTITY_TYPE_NAME);
					entityTable.getTable().setSortColumn(typeColumn.getColumn());
					entityTable.getTable().setSortDirection(provider.getSortDirection());
				});
				typeColumn.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element == null) return "...";
						return ((EntityTableRowItem)element).getType();
					}
				});
				
				for (IntelAttribute attribute : data.getAttributes()) {
					TableViewerColumn aColumn = new TableViewerColumn(entityTable, SWT.NONE);
					aColumn.getColumn().setText(attribute.getName());
					aColumn.getColumn().addListener(SWT.Selection, e->{
						provider.setSortColumn(attribute.getKeyId());
						entityTable.getTable().setSortColumn(aColumn.getColumn());
						entityTable.getTable().setSortDirection(provider.getSortDirection());
					});
					
					aColumn.setLabelProvider(new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							if (element == null) return "...";
							Object value =  ((EntityTableRowItem)element).getAttributeValue(attribute.getKeyId());
							if (value == null) return "";
							switch(attribute.getType()) {
							case BOOLEAN:
								if (((Double)value) < 0.5) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
								return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
							case DATE:
								return DateFormat.getDateInstance().format(java.sql.Date.valueOf((String)value));
							case EMPLOYEE:
							case LIST:
							case NUMERIC:
							case POSITION:
							case TEXT:
								return value.toString();
							}
							return element.toString();
						}
					});
					aColumn.getColumn().pack();
				}
				
				tableComposite.layout(true);
				
				entityTable.setInput(data);
				entityTable.setItemCount((int)data.getTotalCount());
				
			});
			
			return Status.OK_STATUS;
		}
		
	};


}