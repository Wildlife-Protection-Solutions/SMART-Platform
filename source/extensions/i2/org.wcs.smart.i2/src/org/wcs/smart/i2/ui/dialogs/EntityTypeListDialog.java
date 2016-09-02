package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

public class EntityTypeListDialog extends TitleAreaDialog {

	private TableViewer cmbTypes;
	private List<IntelEntityType> types = null;
	private NamedItemViewerFilter filter;
	private IStructuredSelection currentSelection;
	
	private Job loadTypes = new Job("load entity types"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			types = null;
			Session session = HibernateManager.openSession();
			try{
				types = EntityTypeManager.INSTANCE.getEntityTypes(session, SmartDB.getCurrentConservationArea());
				for (IntelEntityType t : types){
					t.getNames().size();
					for (IntelEntityTypeAttribute a : t.getAttributes()){
						a.getAttribute().getNames().size();
						if (a.getAttribute().getAttributeList() != null){
							for (IntelAttributeListItem i : a.getAttribute().getAttributeList()){
								i.getNames().size();
							}
						}
					}
				}
			}finally{
				session.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					cmbTypes.setInput(types);
					cmbTypes.setSelection(currentSelection);
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	public EntityTypeListDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*2));
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FilterComposite typeFilter = new FilterComposite(parent, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				filter.setFilterString(typeFilter.getPatternFilter());
			}
		});
		
		Label l = new Label(parent, SWT.NONE);
		l.setVisible(false);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		cmbTypes = new TableViewer(parent);
		cmbTypes.setContentProvider(ArrayContentProvider.getInstance());
		cmbTypes.setLabelProvider(EntityTypeLabelProvider.INSTANCE);
		cmbTypes.setInput(new String[]{"Loading..."});
		cmbTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbTypes.getControl().setFocus();
		
		filter = new NamedItemViewerFilter(cmbTypes);
		cmbTypes.setFilters(new ViewerFilter[]{filter});
		
		Composite buttonPanel = new Composite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnNew = new Button(buttonPanel, SWT.PUSH);
		btnNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}
		});
		Button btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
		});
		
		Button btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		setTitle("Entity Types");
		getShell().setText("Entity Types");
		setMessage("Manage the entity types in the system.");
		
		loadTypes.setSystem(true);
		loadTypes.schedule();
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	private void add(){
		IntelEntityType type = new IntelEntityType();
		type.setConservationArea(SmartDB.getCurrentConservationArea());
		type.setAttributes(new ArrayList<IntelEntityTypeAttribute>());
		
		EntityTypeDialog ed = new EntityTypeDialog(getShell(), type);
		ed.open();
		
		refresh();
	}
	
	private void edit(){
		Object x = ((IStructuredSelection)cmbTypes.getSelection()).getFirstElement();
		if (x instanceof IntelEntityType){
			IntelEntityType type = (IntelEntityType)x;
			EntityTypeDialog ed = new EntityTypeDialog(getShell(), type);
			ed.open();
			refresh();
		}
		
		
	}
	
	private void refresh(){
		currentSelection = (IStructuredSelection) cmbTypes.getSelection();
		cmbTypes.setInput(new String[]{"Loading..."});
		loadTypes.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
