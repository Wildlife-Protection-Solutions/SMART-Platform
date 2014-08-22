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
package org.wcs.smart.er.ui.mision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;

/**
 * Composite for editing mission property values.
 * @author Emily
 *
 */
public class MissionPropertyValuesComposite extends MissionComposite implements ModifyListener {

	private Composite parts;
	private ScrolledComposite sc;
	private HashMap<MissionAttribute, Object> controls = new HashMap<MissionAttribute, Object>();
	private HashMap<MissionAttribute, ControlDecoration> decorations;
	
	@Override
	public Control createControl(Composite parent) {
		sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		parts = new Composite(sc, SWT.NONE);
		parts.setLayout(new GridLayout());
		parts.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sc.setContent(parts);
		return parts;
	}

	@Override
	public void init(Mission mission, Session session) {
		controls.clear();
		decorations = new HashMap<MissionAttribute, ControlDecoration>();
		for (Control kid : parts.getChildren()){
			kid.dispose();
		}
		List<MissionProperty> properties = mission.getSurvey().getSurveyDesign().getMissionProperties();
		
		Composite outer = new Composite(parts, SWT.NONE);
		outer.setLayout(new GridLayout(2, false));
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//create controls for all properties
		for (MissionProperty mp : properties){
			Label l = new Label(outer, SWT.NONE);
			l.setText(mp.getAttribute().getName() + ":"); //$NON-NLS-1$
			
			if (mp.getAttribute().getType() == AttributeType.TEXT){
				Text txt = new Text(outer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				controls.put(mp.getAttribute(), txt);
				txt.addModifyListener(this);
			} else if(mp.getAttribute().getType() == AttributeType.NUMERIC){
				Text txt = new Text(outer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				txt.addModifyListener(this);
				controls.put(mp.getAttribute(), txt);
				
				ControlDecoration cd = createDecoration(txt);
				decorations.put(mp.getAttribute(), cd);
			} else if(mp.getAttribute().getType() == AttributeType.LIST){
				ComboViewer cmbViewer = new ComboViewer(outer, SWT.DROP_DOWN | SWT.READ_ONLY);
				cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
				cmbViewer.setLabelProvider(new LabelProvider(){
					@Override
					public String getText(Object element){
						if (element instanceof MissionAttributeListItem){
							return ((MissionAttributeListItem) element).getName();
						}
						return super.getText(element);
					}
				});
				cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
					
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						validate();
						fireChangeListeners();
					}
				});
				List<Object> items = new ArrayList<Object>();
				items.add(""); //$NON-NLS-1$
				items.addAll(mp.getAttribute().getAttributeList());
				
				cmbViewer.setInput(items);
				controls.put(mp.getAttribute(), cmbViewer);
			}
		}
		parts.layout(true);
		
		// init controls with values
		if (mission.getMissionPropertyValues() != null){
			for (MissionPropertyValue mpv : mission.getMissionPropertyValues()){
				Object control = controls.get(mpv.getMissionAttribute());
				AttributeType type = mpv.getMissionAttribute().getType();
				if (type == AttributeType.TEXT){
					((Text)control).setText(mpv.getStringValue());
				}else if (type == AttributeType.NUMERIC){
					((Text)control).setText(mpv.getNumberValue().toString());
				}else if (type == AttributeType.LIST){
					((ComboViewer)control).setSelection(new StructuredSelection(mpv.getAttributeListItem()));
				}
			}
		}
		validate();
		sc.setMinSize(parts.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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
	
	@Override
	public void updateDesign(Mission mission) {
		List<MissionPropertyValue> newValues = new ArrayList<MissionPropertyValue>();
		if (controls != null){
		for (Entry<MissionAttribute, Object> entry : controls.entrySet()){
			MissionPropertyValue mpv = new MissionPropertyValue();
			mpv.setMission(mission);
			mpv.setMissionAttribute(entry.getKey());

			boolean add = false;
			if (mpv.getMissionAttribute().getType() == AttributeType.TEXT){
				Text txt = (Text)entry.getValue();
				if (!txt.getText().isEmpty()){
					mpv.setStringValue( txt.getText() );
					add = true;
				}
			}else if (mpv.getMissionAttribute().getType() == AttributeType.NUMERIC){
				Text txt = (Text)entry.getValue();
				if (!txt.getText().isEmpty()){
					try{
						mpv.setNumberValue( Double.valueOf(txt.getText()) );
						add = true;
					}catch (Exception ex){
						//not a double
					}
				}
			}else if (mpv.getMissionAttribute().getType() == AttributeType.LIST){
				ComboViewer cmbViewer = (ComboViewer)entry.getValue();
				if (!cmbViewer.getSelection().isEmpty()){
					Object mali = (Object) ((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
					if (mali instanceof MissionAttributeListItem){
						mpv.setAttributeListItem((MissionAttributeListItem)mali);
						add = true;
					}
				}
			}
			if (add){
				newValues.add(mpv);
			}
		}
		}
		
		
		if (mission.getMissionPropertyValues() != null){
			List<MissionPropertyValue> toDelete = new ArrayList<MissionPropertyValue>();
			
			for (MissionPropertyValue mpv : mission.getMissionPropertyValues()){
				MissionPropertyValue f = null;
				
				for (MissionPropertyValue mmm : newValues){
					if (mmm.getMissionAttribute().equals(mpv.getMissionAttribute())){
						f = mmm;
						break;
					}
				}
				if (f == null){
					toDelete.add(mpv);
				}else{
					newValues.remove(f);
					mpv.setValue(f.getValue());
				}	
			}
			
			mission.getMissionPropertyValues().removeAll(toDelete);
			for (MissionPropertyValue mpv : toDelete){
				mpv.setId(null);
			}
			mission.getMissionPropertyValues().addAll(newValues);
		}else{
			mission.setMissionPropertyValues(new ArrayList<MissionPropertyValue>());
			mission.getMissionPropertyValues().addAll(newValues);
		}
		
		
	}

	private boolean validate(){
		if (controls == null) return false;
		boolean error = false;
		for (Entry<MissionAttribute, Object> entry : controls.entrySet()){
			if (entry.getKey().getType() == AttributeType.NUMERIC){
				String txt = ((Text)entry.getValue()).getText();
				ControlDecoration cd = decorations.get(entry.getKey());
				if (!txt.isEmpty()){
					try{
						Double.valueOf(txt);
						cd.hide();
					}catch (Exception ex){
						cd.setDescriptionText(Messages.MissionPropertyValuesComposite_InvalidNumber);
						cd.show();
						error = true;
					}
				}else{
					cd.hide();
				}
			}
		}
		return error;
	}
	
	@Override
	public boolean isValid() {
		return !validate();
	}

	@Override
	public String getTitle() {
		return Messages.MissionPropertyValuesComposite_Title;
	}

	@Override
	public String getDescription() {
		return Messages.MissionPropertyValuesComposite_Description;
	}

	@Override
	public void modifyText(ModifyEvent e) {
		fireChangeListeners();
		validate();
	}

}
