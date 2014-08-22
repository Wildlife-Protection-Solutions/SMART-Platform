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
package org.wcs.smart.er.ui.samplingunit.wizard;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.ui.samplingunit.load.CsvSamplingUnitImporter;
import org.wcs.smart.er.ui.samplingunit.load.ISamplingUnitImporter;
import org.wcs.smart.ui.ProjectionLabelProvider;

/**
 * Import wizard page, attribute field wizard page.
 * 
 * @author Emily
 *
 */
public class AttributePage extends WizardPage {
	
	private SurveyDesign design;
	private ComboViewer idViewer;
	private ComboViewer xViewer;
	private ComboViewer yViewer;
	private ComboViewer x2Viewer;
	private ComboViewer y2Viewer;
	private ComboViewer projViewer;
	private HashMap<SamplingUnitAttribute, ComboViewer> viewers;
	private List<Projection> proj;
	private Composite main;
	
	public AttributePage(SurveyDesign design, List<Projection> proj){
		super("ATTRIBUTE_PAGE");
		this.proj = proj;
		this.design = design;
	}
	
	@Override
	public void createControl(Composite parent) {
		main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		setControl(main);
		
		setTitle("Sampling Unit Attributes");
		setMessage("Select the field which represents the associated attribute.");
	}
	
	public void setFields(ISamplingUnitImporter importer, String[] fields){
		for (Control kid : main.getChildren()){
			kid.dispose();
		}
		
		Group g = new Group(main, SWT.NONE);
		g.setText("Required Fields");
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(g, SWT.NONE);
		l.setText("ID:");
		
		idViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		idViewer.setLabelProvider(new LabelProvider());
		idViewer.setContentProvider(ArrayContentProvider.getInstance());
		idViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (importer.getClass().equals(CsvSamplingUnitImporter.class)){
			
			if ( ((ImportWizard)getWizard()).getSamplingUnitType() == SamplingUnitType.PLOT ){
				l = new Label(g, SWT.NONE);
				l.setText("X:");
			
				xViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				xViewer.setLabelProvider(new LabelProvider());
				xViewer.setContentProvider(ArrayContentProvider.getInstance());
				xViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				xViewer.setInput(fields);
			
				l = new Label(g, SWT.NONE);
				l.setText("Y:");
			
				yViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				yViewer.setLabelProvider(new LabelProvider());
				yViewer.setContentProvider(ArrayContentProvider.getInstance());
				yViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				yViewer.setInput(fields);
			}else{
				l = new Label(g, SWT.NONE);
				l.setText("X1:");
			
				xViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				xViewer.setLabelProvider(new LabelProvider());
				xViewer.setContentProvider(ArrayContentProvider.getInstance());
				xViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				xViewer.setInput(fields);
			
				l = new Label(g, SWT.NONE);
				l.setText("Y1:");
			
				yViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				yViewer.setLabelProvider(new LabelProvider());
				yViewer.setContentProvider(ArrayContentProvider.getInstance());
				yViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				yViewer.setInput(fields);
				
				l = new Label(g, SWT.NONE);
				l.setText("X2:");
			
				x2Viewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				x2Viewer.setLabelProvider(new LabelProvider());
				x2Viewer.setContentProvider(ArrayContentProvider.getInstance());
				x2Viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				x2Viewer.setInput(fields);
			
				l = new Label(g, SWT.NONE);
				l.setText("Y2:");
			
				y2Viewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				y2Viewer.setLabelProvider(new LabelProvider());
				y2Viewer.setContentProvider(ArrayContentProvider.getInstance());
				y2Viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				y2Viewer.setInput(fields);
			}
			
			l = new Label(g, SWT.NONE);
			l.setText("Projection:");
			
			projViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
			projViewer.setLabelProvider(ProjectionLabelProvider.getInstance());
			projViewer.setContentProvider(ArrayContentProvider.getInstance());
			projViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			projViewer.setInput(proj);
		}
		
		Group g2 = new Group(main, SWT.NONE);
		g2.setText("Optional Fields");
		g2.setLayout(new GridLayout(2, false));
		g2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		viewers = new HashMap<SamplingUnitAttribute, ComboViewer>();
		for (SurveyDesignSamplingUnitAttribute sda: design.getSamplingUnitAttributes()){
			l = new Label(g2, SWT.NONE);
			l.setText(sda.getSamplingUnitAttribute().getName() + ":");
			
			ComboViewer viewer = new ComboViewer(g2, SWT.DROP_DOWN | SWT.READ_ONLY);
			viewer.setLabelProvider(new LabelProvider());
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			viewer.setInput(fields);
			
			viewers.put(sda.getSamplingUnitAttribute(), viewer);
		}
		
		String[] idItems = new String[fields.length +1];
		idItems[0] = "< System Generated >";
		for (int i = 0; i < fields.length; i ++){
			idItems[i+1] = fields[i];
		}
		idViewer.setInput(idItems);
		idViewer.setSelection(new StructuredSelection(idItems[0]));
		
		main.layout(true);
	}
	
	public String getIdField(){
		return (String) ((IStructuredSelection)idViewer.getSelection()).getFirstElement();
	}
	
	public String getX1Field(){
		if (xViewer == null) return null;
		return (String) ((IStructuredSelection)xViewer.getSelection()).getFirstElement();
	}
	
	public String getY1Field(){
		if (yViewer == null) return null;
		return (String) ((IStructuredSelection)yViewer.getSelection()).getFirstElement();
	}
	
	public String getX2Field(){
		if (x2Viewer == null) return null;
		return (String) ((IStructuredSelection)xViewer.getSelection()).getFirstElement();
	}
	
	public String getY2Field(){
		if (y2Viewer == null) return null;
		return (String) ((IStructuredSelection)yViewer.getSelection()).getFirstElement();
	}
	
	public Projection getProjection(){
		if (projViewer == null) return null;
		return (Projection) ((IStructuredSelection)projViewer.getSelection()).getFirstElement();
	}
	
	public HashMap<SamplingUnitAttribute, String> getAttributeFields(){
		HashMap<SamplingUnitAttribute, String> items = new HashMap<SamplingUnitAttribute, String>();
		
		for(Entry<SamplingUnitAttribute, ComboViewer> entry : viewers.entrySet()){
			String value = null;
			if (!entry.getValue().getSelection().isEmpty()){
				value = (String) ((IStructuredSelection)entry.getValue().getSelection()).getFirstElement();
			}
			
			items.put(entry.getKey(), value);
		}
		return items;
	}

}