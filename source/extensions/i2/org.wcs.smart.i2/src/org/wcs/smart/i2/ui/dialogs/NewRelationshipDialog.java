package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RelationshipTypeManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

public class NewRelationshipDialog extends TitleAreaDialog{

	private static final String LAST_TYPE_KEY = "org.wcs.smart.i2.ui.dialogs.newrelationship.lasttype";
	
	private IntelEntity entity;
	
	private TableComboViewer cmbRelationshipType;
	
	@Inject
	private IEventBroker broker;
	
	private Job loadEntityTypes = new Job("load relationship types"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<IntelRelationshipType> types = new ArrayList<IntelRelationshipType>();
			Session s = HibernateManager.openSession();
//			s.addEventListeners(new IntelEntityListener());
			try{
				List<IntelRelationshipType> local = RelationshipTypeManager.INSTANCE.getRelationshipTypes(s, SmartDB.getCurrentConservationArea());
				for (IntelRelationshipType l : local){
					if (l.getSourceEntityType() == entity.getEntityType() || l.getTargetEntityType() == entity.getEntityType()){
						types.add(l);
					}
				}
			}finally{
				s.close();
			}
			
			IntelRelationshipType toSelect = types.isEmpty() ? null : types.get(0);
			String uuid = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(LAST_TYPE_KEY);
			if (uuid != null){
				try{
					UUID u = UuidUtils.stringToUuid(uuid);
					for (IntelRelationshipType t : types){
						if (t.getUuid().equals(u)){
							toSelect = t;
							break;
						}
					}
				}catch (Exception ex){
					//eat me
				}
			}
			
			final IntelRelationshipType sel = toSelect;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					cmbRelationshipType.setInput(types);
					if (sel != null) cmbRelationshipType.setSelection(new StructuredSelection(sel));
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	public NewRelationshipDialog(Shell parentShell, IntelEntity entity) {
		super(parentShell);
		this.entity = entity;
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.4));
	}
	
	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				cd.dispose();
				
			}
		});
		return cd;
	}
	
	protected void okPressed() {
		//TODO: Implement this - new Relationship
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	private void modified(){
		boolean isError = false;
	
		getButton(IDialogConstants.OK_ID).setEnabled(!isError);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Relationship Type:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		cmbRelationshipType = new TableComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbRelationshipType.setContentProvider(ArrayContentProvider.getInstance());
		cmbRelationshipType.setLabelProvider(new RelationshipTypeLabelProvider());
		cmbRelationshipType.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbRelationshipType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbRelationshipType.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbRelationshipType.getSelection()).getFirstElement();
				if (x instanceof IntelEntityType){
					IntelEntityType type = (IntelEntityType)x;
					Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(LAST_TYPE_KEY, UuidUtils.uuidToString(type.getUuid()));
				}
				
			}
		});
		l = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		
		setTitle("New Relationship");
		getShell().setText("New Relationship");
		setMessage("Create a new relationsihp");
		
		loadEntityTypes.setSystem(true);
		loadEntityTypes.schedule(0);
		
		return parent;
	}
	
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
}