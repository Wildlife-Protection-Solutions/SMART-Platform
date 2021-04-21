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
package org.wcs.smart.observation.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SignatureTypeManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.ui.SmartStyledInputDialog;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.util.GeometryUtils;

/**
 * Property page for editing patrol options
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationOptionsPropertyPage extends AbstractPropertyJHeaderDialog {

	private ObservationOptions patrolOption = null;
	private Text txtEditTime;
	private ControlDecoration cdEditTime;
	private Button btnTrackDistanceDirection;
	private Button btnTrackObserver;
	private String errorEditTimeMessage = MessageFormat.format(Messages.PatrolOptionsPropertyPage_Error_EditTimeInvalid, -1, Short.MAX_VALUE);

	private TableViewer tblSignatures;
	private List<SignatureType> types;
	private List<SignatureType> deleteTypes;
	
	/**
	 * @param parent
	 * @param title
	 */
	public ObservationOptionsPropertyPage(Shell parent) {
		super(parent, Messages.PatrolOptionsPropertyPage_DialogTitle);
		try(Session s = HibernateManager.openSession()){
			patrolOption = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), s);
			types = SignatureTypeManager.INSTANCE.getTypes(s, SmartDB.getCurrentConservationArea());
			types.forEach(t->t.getNames().size());
		}
		deleteTypes = new ArrayList<>();
	}
	

	@Override
	public boolean  close(){
		return super.close();
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartUiUtils.createHeaderLabel(container, Messages.ObservationOptionsPropertyPage_SignatureHeader);
		Composite csig = new Composite(container, SWT.NONE);
		csig.setLayout(new GridLayout(2, false));
		csig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(csig, SWT.WRAP);
		l.setText(Messages.ObservationOptionsPropertyPage_SignatureInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 350;
		
		Composite tcomp = new Composite(csig, SWT.NONE);
		tcomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)tcomp.getLayoutData()).heightHint = 100;
		tcomp.setLayout(new TableColumnLayout());
		
		tblSignatures = new TableViewer(tcomp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tblSignatures.getTable().setHeaderVisible(true);
		tblSignatures.getTable().setLinesVisible(true);

		TableViewerColumn viewerColumn = new TableViewerColumn(tblSignatures,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setText(Messages.ObservationOptionsPropertyPage_SignatureNameColumn);
		column.setResizable(true);
		column.setMoveable(true);
		TableColumnLayout layout = (TableColumnLayout) tcomp.getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((SignatureType)element).getName();
			}
		});
		
		viewerColumn = new TableViewerColumn(tblSignatures,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.ObservationOptionsPropertyPage_SignatureKeyColumn);
		column.setResizable(true);
		column.setMoveable(true);
		layout.setColumnData(column, new ColumnWeightData(2,ColumnWeightData.MINIMUM_WIDTH, true));
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((SignatureType)element).getKeyId();
			}
		});
		
		tblSignatures.setContentProvider(ArrayContentProvider.getInstance());
		tblSignatures.setInput(types);
		tblSignatures.getTable().setHeaderVisible(true);
		tblSignatures.getTable().setLinesVisible(true);

		tblSignatures.getTable().addListener(SWT.MouseDoubleClick, e->{
			int idx = tblSignatures.getCell(new Point(e.x, e.y)).getColumnIndex();
			if (idx == 0) {
				editSignatureType();
			}else {
				editSignatureKey();
			}
		});
		
		Menu mnu = new Menu(tblSignatures.getControl());
		
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.addListener(SWT.Selection,  e->addSignatureType());
		
		MenuItem miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.addListener(SWT.Selection,  e->editSignatureType());
		miEdit.setEnabled(false);
		
		MenuItem miEditKey = new MenuItem(mnu, SWT.PUSH);
		miEditKey.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEditKey.setText(DialogConstants.EDIT_KEY_BUTTON_TEXT);
		miEditKey.addListener(SWT.Selection,  e->editSignatureKey());
		miEditKey.setEnabled(false);
		
		MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.addListener(SWT.Selection,  e->deleteSignatureType());
		miDelete.setEnabled(false);
		
		tblSignatures.getControl().setMenu(mnu);
		
		
		Composite btns = new Composite(csig, SWT.NONE);
		btns.setLayout(new GridLayout());
		btns.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout)btns.getLayout()).marginWidth = 0;
		((GridLayout)btns.getLayout()).marginHeight = 0;
		
		Button btnAdd = new Button(btns, SWT.NONE);
		btnAdd.setBackground(csig.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.addListener(SWT.Selection, e->addSignatureType());
		
		Button btnEdit = new Button(btns, SWT.NONE);
		btnEdit.setBackground(csig.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.addListener(SWT.Selection, e->editSignatureType());
		btnEdit.setEnabled(false);
		
		Button btnDelete = new Button(btns, SWT.NONE);
		btnDelete.setBackground(csig.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.addListener(SWT.Selection, e->deleteSignatureType());
		btnDelete.setEnabled(false);
		
		SmartUiUtils.createHeaderLabel(container, Messages.ObservationOptionsPropertyPage_DistanceBearingOpLabel);
		
		tblSignatures.addSelectionChangedListener(e->{
			boolean v = !tblSignatures.getSelection().isEmpty();
			miEdit.setEnabled(v);
			miEditKey.setEnabled(v);
			btnEdit.setEnabled(v);
			miDelete.setEnabled(v);
			btnDelete.setEnabled(v);
		});
		Composite g = new Composite(container, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Link lbl1 = new Link(g, SWT.WRAP);
		lbl1.setText(Messages.PatrolOptionsPropertyPage_DistanceDirection_DescLabel1);
		lbl1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl1.getLayoutData()).widthHint = 350;
		lbl1.addListener(SWT.Selection, e->{
			
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.ObservationOptionsPropertyPage_ComputationDetails);
			sb.append("\n\n"); //$NON-NLS-1$
			sb.append("d = distance (meters)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("b = bearing (degrees)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("RADIUS = " + GeometryUtils.EARTH_RADIUS); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("dR = d / RADIUS"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("rb = toRadians(b)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("ry = toRadians(y)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("rx = toRadians(x)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("rprjy = asin( sin(ry) * cos(dR) + cos(ry) * sin(dR) * cos(rb) ) "); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("rprjx = rx + atan2( Math.sin(rb) * sin(dR) * cos(ry), cos(dR) - sin(ry) * sin(rprjy) )"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("prj_x = toDegrees(rprjx)"); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append("prj_y = toDegrees(rprjy)"); //$NON-NLS-1$
			sb.append("\n\n"); //$NON-NLS-1$
			
			MessageDialog md = new MessageDialog(getShell(), Messages.ObservationOptionsPropertyPage_DistanceBearingTitle, null, null, MessageDialog.NONE, 0, IDialogConstants.OK_LABEL) {
				@Override
			    protected Control createCustomArea(Composite parent) {
					parent.getParent().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
					parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
					
			    	Text txtWarnings = new Text(parent, SWT.MULTI | SWT.BORDER);
			    	txtWarnings.setEditable(false);
			    	txtWarnings.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			    	GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			    	txtWarnings.setLayoutData(gd);
			    	
			    	txtWarnings.setText(sb.toString());
			        return txtWarnings;
			    }
			};
			md.open();
		});
		
		btnTrackDistanceDirection = new Button(g, SWT.CHECK | SWT.WRAP);
		btnTrackDistanceDirection.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnTrackDistanceDirection.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
		});
		btnTrackDistanceDirection.setText(Messages.PatrolOptionsPropertyPage_RecordDistanceDirectory_Op);
		
		SmartUiUtils.createHeaderLabel(container, Messages.ObservationOptionsPropertyPage_ObserverOp);

		g = new Composite(container, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(g, SWT.WRAP);
		lbl.setText(Messages.ObservationOptionsPropertyPage_ObserverDescription);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		btnTrackObserver = new Button(g, SWT.CHECK);
		btnTrackObserver.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnTrackObserver.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
		});
		btnTrackObserver.setText(Messages.ObservationOptionsPropertyPage_ObserverLabel);
		
		
		lbl = new Label(container, SWT.NONE);  //spacer
		
		SmartUiUtils.createHeaderLabel(container, Messages.PatrolOptionsPropertyPage_PatrolEditOptions_Label);
		
		g = new Composite(container, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lbl = new Label(g, SWT.WRAP);
		lbl.setText(Messages.PatrolOptionsPropertyPage_PatrolEditOptions_DescLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)lbl.getLayoutData()).widthHint = 350;
		
		cdEditTime = createDecoration(lbl);
		cdEditTime.setDescriptionText(errorEditTimeMessage);
		cdEditTime.hide();
		
		txtEditTime = new Text(g, SWT.BORDER);
		txtEditTime.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData)txtEditTime.getLayoutData()).widthHint = 30;
		txtEditTime.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
			}
		});
		lbl = new Label(g, SWT.NONE);
		lbl.setText(Messages.PatrolOptionsPropertyPage_PatrolEditOptions_DaysLabel);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		//init values
		if (patrolOption != null){
			btnTrackDistanceDirection.setSelection(patrolOption.getTrackDistanceDirection());
			btnTrackObserver.setSelection(patrolOption.getTrackObserver());
			
			if (patrolOption.getEditTime() != null){
				txtEditTime.setText(patrolOption.getEditTime().toString());
			}else{
				txtEditTime.setText("-1"); //$NON-NLS-1$
			}
		}
		
		setTitle(Messages.PatrolOptionsPropertyPage_PageName);
		setMessage(Messages.PatrolOptionsPropertyPage_DialogMessage);
		setChangesMade(false);
		return container;
	}

	private void addSignatureType() {
		SmartStyledInputDialog nameD = new SmartStyledInputDialog(getShell(),
				Messages.ObservationOptionsPropertyPage_NewTypeHeader, Messages.ObservationOptionsPropertyPage_NewTypeMessage, Messages.ObservationOptionsPropertyPage_NewTypeHeader2,
				e->{
					if(e.trim().isEmpty()) return Messages.ObservationOptionsPropertyPage_NameRequired;
					return null;
				});
		if (nameD.open() == Window.OK) {
			SignatureType newType = SignatureTypeManager.INSTANCE.createType(SmartDB.getCurrentConservationArea(), nameD.getValue());
			newType.setKeyId( NamedKeyItem.generateKey(newType.getName(), types) );
			
			types.add(newType);
		
			tblSignatures.refresh();
			setChangesMade(true);
		}
	}
	
	private void editSignatureKey() {
		Object x = tblSignatures.getStructuredSelection().getFirstElement();
		
		if (x instanceof SignatureType) {
			InputDialog id = new KeyInputDialog(getShell(), ((SignatureType) x).getKeyId(), types);
			int ret = id.open();
			if (ret != Window.CANCEL) {
				((SignatureType)x).setKeyId(id.getValue());
				tblSignatures.refresh(x);
			}
		}
	}
	
	private void editSignatureType() {
		Object x = tblSignatures.getStructuredSelection().getFirstElement();
		if (x instanceof SignatureType) {
			TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), (SignatureType)x);
			if (dialog.open() ==  Window.OK){
				tblSignatures.refresh();
				setChangesMade(true);
			}
		}
	}
	
	private void deleteSignatureType() {
		for (Iterator<?> iterator = tblSignatures.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof SignatureType) {
				types.remove(item);
				deleteTypes.add((SignatureType) item);
			}
		}
		tblSignatures.refresh();
		setChangesMade(true);
	}
	
	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}

	protected void validate() {
		if (!isEditTimeValid()) {
			cdEditTime.show();
			setErrorMessage(errorEditTimeMessage);
			if (getButton(IDialogConstants.OK_ID) != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}
			return;
		}
		cdEditTime.hide();
		setErrorMessage(null);
	}
	
	private boolean isEditTimeValid() {
		try {
			int edittime = Integer.parseInt(txtEditTime.getText());
			if (edittime < -1 || edittime > Short.MAX_VALUE) {
				return false;
			} else {
				return true;
			}
		} catch (Exception ex) {
			return false;
		}

	}
	
	@Override
	protected void setChangesMade(boolean ischanged) {
		super.setChangesMade(ischanged);
		validate();
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		if (patrolOption == null){
			return false;
		}
		if (!isEditTimeValid()) {
			//this should be impossible to be here, but added just in case
			ObservationPlugIn.displayLog(errorEditTimeMessage, null);
			return false;
		}
		patrolOption.setTrackDistanceDirection(btnTrackDistanceDirection.getSelection());
		patrolOption.setTrackObserver(btnTrackObserver.getSelection());
		patrolOption.setEditTime(Integer.parseInt(txtEditTime.getText()));
	
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				s.saveOrUpdate(patrolOption);
				
				for (SignatureType t : deleteTypes) {
					SignatureTypeManager.INSTANCE.deleteType(t, s);
				}
				
				types.forEach(t->SignatureTypeManager.INSTANCE.saveType(t, s));
				
				s.getTransaction().commit();
				setChangesMade(false);
				
				//fire event for options modified
				WaypointEventManager.getInstance().waypointOptionsModified();
				
				return true;
			}catch (Exception ex){
				s.getTransaction().rollback();
				ObservationPlugIn.displayLog(Messages.PatrolOptionsPropertyPage_Error_CouldNotSave + ex.getLocalizedMessage(), ex);
			}
		}
		return false;
	}
	
	
}
