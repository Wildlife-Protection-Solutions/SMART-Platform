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
package org.wcs.smart.er.ui.surveydesign;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Configurable data model attribute.
 * 
 * @author Emily
 *
 */
public class ConfigurableModelComposite  extends SurveyDesignComposite {
	
	private static final String dataModelObject = "SMART DataModel";
	private ComboViewer cmbConfig;
	private List<ConfigurableModel> models;
	
	public ConfigurableModelComposite (List<ConfigurableModel> models){
		super();
		this.models = models;
	}
	
	
	public Control createControl(Composite parent){
		Composite part = new Composite(parent, SWT.NONE);
		
		part.setLayout(new GridLayout(2, false));
		
		Label l = new Label(part, SWT.NONE);
		l.setText("Configurable Model:");
		
		cmbConfig = new ComboViewer(part);
		cmbConfig.setContentProvider(ArrayContentProvider.getInstance());
		cmbConfig.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof ConfigurableModel){
					return ((ConfigurableModel)element).getName();
				}else if (element instanceof String){
					return (String)element;
				}
				return super.getText(element);
			}
		});
		cmbConfig.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		
		List<Object> inputs = new ArrayList<Object>();
		inputs.addAll(models);
		inputs.add(dataModelObject);
		
		cmbConfig.setInput(inputs);
		cmbConfig.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbConfig.getControl().getLayoutData()).widthHint = 200;
		
		part.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		return part;
	}


	@Override
	public void init(SurveyDesign design, Session session) {
		if (design.getConfigurableModel() != null){
			cmbConfig.setSelection(new StructuredSelection(design.getConfigurableModel()));
		}else{
			cmbConfig.setSelection(new StructuredSelection(dataModelObject));
		}		
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		if (cmbConfig.getSelection().isEmpty()){
			design.setConfigurableModel(null);
			return;
		}
		
		Object x = ((StructuredSelection)cmbConfig.getSelection()).getFirstElement();
		if (x instanceof ConfigurableModel){
			design.setConfigurableModel((ConfigurableModel)x);
		}else{
			design.setConfigurableModel(null);
		}
	}

	@Override
	public boolean isValid() {
		return !cmbConfig.getSelection().isEmpty();
	}
	
	@Override
	public String getTitle(){
		return "Data Model";
	}
	
	@Override
	public String getDescription(){
		return "Select the configurable model which represents the survey design data.";
	}
}
