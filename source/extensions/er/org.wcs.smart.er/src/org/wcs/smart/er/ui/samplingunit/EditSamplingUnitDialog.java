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
package org.wcs.smart.er.ui.samplingunit;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.State;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * Dalog for editing sampling unit.  This dialog
 * updates the sampling unit in the database and fires
 * appropriate events.
 * 
 * @author Emily
 *
 */
public class EditSamplingUnitDialog extends TitleAreaDialog{

	private Session session;
	private SamplingUnit su;
	
	private Text txtType;
	private Text txtId;
	private ComboViewer cmbState;
		
	private ControlDecoration cdId;
	
	private HashMap<SamplingUnitAttribute, Object> attribute2Control;
	private HashMap<SamplingUnitAttribute, ControlDecoration> attribute2Error;
	
	private List<SamplingUnit> siblings;
	public EditSamplingUnitDialog(Shell parentShell, SamplingUnit su, List<SamplingUnit> siblings) {
		super(parentShell);
		this.siblings = siblings;
		this.su = su;
	}
	
	@Override
	public Point getInitialSize(){
		Point p = super.getInitialSize();
		if (p.y > 400){
			p.y = 400;
		}
		return p;
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText(DialogConstants.SAVE_TEXT);
	}
	
	@Override
	public void okPressed(){
		if (validate()) return;
		
		session.beginTransaction();

		try {
			su.setId(txtId.getText().trim());
			su.setState((State) ((IStructuredSelection) cmbState.getSelection())
					.getFirstElement());

			List<SamplingUnitAttributeValue> toDelete = new ArrayList<SamplingUnitAttributeValue>();
			List<SamplingUnitAttributeValue> toAdd = new ArrayList<SamplingUnitAttributeValue>();
			for (SamplingUnitAttribute sua : attribute2Control.keySet()) {
				Object control = attribute2Control.get(sua);
				
				boolean isEmpty = false;
				Object value = null;
				if (sua.getType() == AttributeType.NUMERIC || 
						sua.getType() == AttributeType.TEXT){
					if (((Text)control).getText().trim().isEmpty()){
						isEmpty = true;
					}else{
						value = ((Text)control).getText();
					}
					
				}else{
					if (((ComboViewer)control).getSelection().isEmpty() || 
						!(((StructuredSelection)((ComboViewer)control).getSelection()).getFirstElement() instanceof SamplingUnitAttributeListItem)){ 
						isEmpty = true;
					}else{
						value = ((StructuredSelection)(((ComboViewer)control).getSelection())).getFirstElement();
					}
				}
				if (isEmpty) {
					// remove
					for (SamplingUnitAttributeValue suav : su.getAttributes()) {
						if (suav.getSamplingUnitAttribute().equals(sua)) {
							toDelete.add(suav);
							break;
						}
					}
				} else {
					SamplingUnitAttributeValue toUpdate = null;
					for (SamplingUnitAttributeValue suav : su.getAttributes()) {
						if (suav.getSamplingUnitAttribute().equals(sua)) {
							toUpdate = suav;
						}
					}

					if (toUpdate == null) {
						toUpdate = new SamplingUnitAttributeValue();
						toUpdate.setSamplingUnit(su);
						toUpdate.setSamplingUnitAttribute(sua);
						toAdd.add(toUpdate);
					}

					if (sua.getType() == AttributeType.TEXT) {
						toUpdate.setStringValue(((String)value));
						toUpdate.setNumberValue(null);
						toUpdate.setAttributeListItem(null);
					} else if (sua.getType() == AttributeType.NUMERIC) {
						toUpdate.setNumberValue(Double.valueOf((String)value));
						toUpdate.setStringValue(null);
						toUpdate.setAttributeListItem(null);
					} else if (sua.getType() == AttributeType.LIST){
						toUpdate.setAttributeListItem((SamplingUnitAttributeListItem)value);
						toUpdate.setNumberValue( null );
						toUpdate.setStringValue( null );
					}

				}
			}
			for (SamplingUnitAttributeValue a : toDelete) {
				a.setId(null);
			}
			su.getAttributes().removeAll(toDelete);
			su.getAttributes().addAll(toAdd);
			session.getTransaction().commit();
		
			//close so event handlers can open their own
			session.close();
			
			SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, su.getSurveyDesign());
			
			super.okPressed();
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(
					MessageFormat.format(Messages.EditSamplingUnitDialog_ErrorSavingChanges, new Object[]{su.getId()}) + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
		}finally{
			if (session.isOpen()){
				session.close();
			}
		}
	}

	
	@Override
	public Composite createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		
		session = HibernateManager.openSession();
		this.su = (SamplingUnit) session.load(SamplingUnit.class, su.getUuid());
		
		ModifyListener validateListener = new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}};
		
		ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite comp = new Composite(sc, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sc.setContent(comp);
		
		Label l = new Label(comp, SWT.NONE);
		l.setText(Messages.EditSamplingUnitDialog_TypeLabel);
		
		txtType = new Text(comp, SWT.BORDER);
		txtType.setEnabled(false);
		txtType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = new Label(comp, SWT.NONE);
		l.setText(Messages.EditSamplingUnitDialog_StateLabel);
		
		cmbState = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbState.setContentProvider(ArrayContentProvider.getInstance());
		cmbState.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof SamplingUnit.State){
					return ((SamplingUnit.State) element).getGuiName();
				}
				return super.getText(element);
			}
		});
		cmbState.setInput(SamplingUnit.State.values());
		cmbState.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = new Label(comp, SWT.NONE);
		l.setText(Messages.EditSamplingUnitDialog_IdLabel);
		
		txtId = new Text(comp, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cdId = createDecoration(txtId);
		cdId.hide();
		txtId.addModifyListener(validateListener);
		
		l = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		List<SurveyDesignSamplingUnitAttribute> attributes = new ArrayList<SurveyDesignSamplingUnitAttribute>();
		attributes.addAll(su.getSurveyDesign().getSamplingUnitAttributes());
		Collections.sort(attributes, new Comparator<SurveyDesignSamplingUnitAttribute>() {
			@Override
			public int compare(SurveyDesignSamplingUnitAttribute arg0,
					SurveyDesignSamplingUnitAttribute arg1) {
				return Collator.getInstance().compare(arg0.getSamplingUnitAttribute().getName(),
						arg1.getSamplingUnitAttribute().getName());
			}
		});
	
		attribute2Control = new HashMap<SamplingUnitAttribute, Object>();
		attribute2Error = new HashMap<SamplingUnitAttribute, ControlDecoration>();
		
		for (SurveyDesignSamplingUnitAttribute a : attributes){
			l = new Label(comp, SWT.NONE);
			l.setText(a.getSamplingUnitAttribute().getName() + ":"); //$NON-NLS-1$
			
			if (a.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC ||
					a.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
				final Text txtAttribute = new Text(comp, SWT.BORDER);
				txtAttribute.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				txtAttribute.addModifyListener(validateListener);
				final ControlDecoration cd = createDecoration(txtAttribute);
				cd.hide();
			
				attribute2Control.put(a.getSamplingUnitAttribute(), txtAttribute);
				attribute2Error.put(a.getSamplingUnitAttribute(), cd);
			}else if (a.getSamplingUnitAttribute().getType() == AttributeType.LIST){
				ComboViewer lstViewer = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
				lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				lstViewer.setContentProvider(ArrayContentProvider.getInstance());
				lstViewer.setLabelProvider(SamplingUnitLabelProvider.INSTANCE);
				List<Object> values = new ArrayList<Object>();
				values.add(""); //$NON-NLS-1$
				values.addAll(a.getSamplingUnitAttribute().getAttributeList());
				lstViewer.setInput(values);
				
				attribute2Control.put(a.getSamplingUnitAttribute(), lstViewer);
			}
		}
		
		txtType.setText(su.getType().getGuiName());
		cmbState.setSelection(new StructuredSelection(su.getState()));
		txtId.setText(su.getId());
		
		for(SamplingUnitAttributeValue v : su.getAttributes()){
			Object control = attribute2Control.get(v.getSamplingUnitAttribute()); 
			if (v.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
				((Text)control).setText(v.getStringValue());
			}else if (v.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
				((Text)control).setText(v.getNumberValue().toString());
			}else if (v.getSamplingUnitAttribute().getType() == AttributeType.LIST){
				if (v.getSamplingUnitAttribute() != null){
					((ComboViewer)control).setSelection(new StructuredSelection(v.getAttributeListItem()));
				}
			}
		}
		
		sc.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		getShell().setText(Messages.EditSamplingUnitDialog_ShellTitle);
		setTitle(Messages.EditSamplingUnitDialog_Title + su.getId());
		setMessage(Messages.EditSamplingUnitDialog_Message);
		
		return parent;
	}
	
	/**
	 * 
	 * @return true if error otherwise false
	 */
	private boolean validate(){
		boolean error = false;
		
		//look and see if id is unique
		String id = txtId.getText();
		for (SamplingUnit s : siblings){
			if (s.getId().toLowerCase().equals(id.toLowerCase())){
				error = true;
				cdId.show();
				cdId.setDescriptionText(Messages.EditSamplingUnitDialog_IdUnique);
			}
		}

		if (!error){
			if (!SmartUtils.isSimpleString(txtId.getText().trim(), RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, SamplingUnit.ID_MAX_LENGTH)){
				cdId.setDescriptionText(MessageFormat.format(Messages.EditSamplingUnitDialog_IdError, new Object[]{RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, SamplingUnit.ID_MAX_LENGTH}));
				cdId.show();
				error = true;
			}else{
				cdId.hide();
			}
		}
				
		for (Entry<SamplingUnitAttribute, Object> entry : attribute2Control.entrySet()){

			Object txtAttribute = entry.getValue();
			ControlDecoration cd = attribute2Error.get(entry.getKey());
			
			if (entry.getKey().getType() == AttributeType.TEXT){
				if (((Text)txtAttribute).getText().trim().length() > SamplingUnitAttribute.MAX_STRING_LENGTH){
					cd.setDescriptionText(MessageFormat.format(Messages.EditSamplingUnitDialog_StringError, new Object[]{SamplingUnitAttribute.MAX_STRING_LENGTH}));
					cd.show();
					error = true;
				}else{
					cd.hide();
				}
			}else if (entry.getKey().getType() == AttributeType.NUMERIC){
				if (((Text)txtAttribute).getText().trim().length() != 0){
					try{
						Double.valueOf(((Text)txtAttribute).getText());
						cd.hide();
					}catch (Exception ex){
						cd.setDescriptionText(Messages.EditSamplingUnitDialog_NumberError);
						cd.show();
						error = true;
					}
				}else{
					cd.hide();
				}
			}
		}
		
		if (getButton(IDialogConstants.OK_ID) != null){
			getButton(IDialogConstants.OK_ID).setEnabled(!error);
		}
		return error;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private ControlDecoration createDecoration(Control parent){
		ControlDecoration cd = new ControlDecoration(parent, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
}
