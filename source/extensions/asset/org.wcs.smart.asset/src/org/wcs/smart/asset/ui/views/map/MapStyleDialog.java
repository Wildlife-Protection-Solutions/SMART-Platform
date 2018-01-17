package org.wcs.smart.asset.ui.views.map;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetMapStyle;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.properties.DialogConstants;

public class MapStyleDialog extends TitleAreaDialog {

	private Button btnSaveNew, btnOverride;
	private Text txtName;
	private ComboViewer cmbExisting;
	
	private AssetMapStyle current;
	private StyleBlackboard toSave;
	
	public MapStyleDialog(Shell parentShell, AssetMapStyle current, StyleBlackboard toSave) {
		super(parentShell);
		this.toSave = toSave;
		this.current = current;
	}
	
	
	@Override
	public void okPressed() {
		if(!validate()) return;
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				if (btnSaveNew.getSelection()) {
					AssetMapStyle style = new AssetMapStyle();
					style.setName(txtName.getText().trim());
					style.setConservationArea(SmartDB.getCurrentConservationArea());
					style.setStyleString(StyleManager.INSTANCE.asString(toSave));
					session.save(style);
				}else {
					AssetMapStyle style = (AssetMapStyle) cmbExisting.getStructuredSelection().getFirstElement();
					style.setStyleString(StyleManager.INSTANCE.asString(toSave));
					session.saveOrUpdate(style);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
			
		}catch (Exception ex) {
			AssetPlugIn.log("Unable to save map style to database: " +ex.getMessage(), ex);
			return;
		}
		
		super.okPressed();
	}
	
	private boolean validate() {
		setErrorMessage(null);
		if (btnOverride.getSelection()) {
			if (cmbExisting.getStructuredSelection().getFirstElement() == null || !(cmbExisting.getStructuredSelection().getFirstElement() instanceof AssetMapStyle)) {
				setErrorMessage("An existing map style must be selected.");
				enableOk(false);
				return false;
			}
		}else if (btnSaveNew.getSelection()) {
			if (txtName.getText().trim().isEmpty()) {
				setErrorMessage("An style name required.");
				enableOk(false);
				return false;
			}
		}
		enableOk(true);
		return true;
	}
	private void enableOk(boolean enabled) {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok == null) return;
		ok.setEnabled(enabled);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(c, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Save As:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		btnSaveNew = new Button(main, SWT.RADIO);
		btnSaveNew.setText("New Style:");
		
		txtName = new Text(main, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addListener(SWT.Modify, e->validate());
		
		btnOverride= new Button(main, SWT.RADIO);
		btnOverride.setText("Override:");
		
		cmbExisting = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbExisting.setContentProvider(ArrayContentProvider.getInstance());
		cmbExisting.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AssetMapStyle)  return ((AssetMapStyle) element).getName();
				return super.getText(element);
			}
		});
		cmbExisting.setInput(DialogConstants.LOADING_TEXT);
		cmbExisting.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
					
		if (current == null) {
			btnSaveNew.setSelection(true);
			btnOverride.setSelection(false);
			txtName.setEnabled(true);
			cmbExisting.getControl().setEnabled(false);
		}else {
			btnSaveNew.setSelection(true);
			btnOverride.setSelection(false);
			txtName.setEnabled(false);
			cmbExisting.getControl().setEnabled(true);
		}
		
		btnSaveNew.addListener(SWT.Selection, e->{
			cmbExisting.getControl().setEnabled(btnOverride.getSelection());
			txtName.setEnabled(btnSaveNew.getSelection());
		});
		btnOverride.addListener(SWT.Selection, e->{
			cmbExisting.getControl().setEnabled(btnOverride.getSelection());
			txtName.setEnabled(btnSaveNew.getSelection());
		});
		setMessage("Save current map style");
		setTitle("New Style");
		getShell().setText("New Style");
		
		loadStyle.setSystem(true);
		loadStyle.schedule();
		
		return c;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		btnOk.setEnabled(false);
	}
	
	Job loadStyle = new Job("loading asset styles") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<AssetMapStyle> types = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				types.addAll(QueryFactory.buildQuery(session, AssetMapStyle.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).getResultList());
				
			}
			
			Display.getDefault().syncExec(()->{
				cmbExisting.setInput(types);
				if (current != null) {
					cmbExisting.setSelection(new StructuredSelection(current));
				}
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	@Override
	public boolean isResizable() {
		return true;
	}

}