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
package org.wcs.smart.er.ui.samplingunit.load.wizard;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
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
	
	private ScrolledComposite sc ;

	private boolean attributesOnly;
	
	public AttributePage(boolean attributesOnly,
			SurveyDesign design, List<Projection> proj){
		super("ATTRIBUTE_PAGE"); //$NON-NLS-1$
		this.attributesOnly = attributesOnly;
		this.proj = proj;
		this.design = design;
	}
	
	@Override
	public void createControl(Composite parent) {
		sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);
		
		main = new Composite(sc, SWT.NONE);
		main.setLayout(new GridLayout());

		sc.setContent(main);
		
		setControl(sc);
		
		setTitle(Messages.AttributePage_Title);
		setMessage(Messages.AttributePage_Message);
	}
	
	/**
	 * Updates the fields and drop down boxes.
	 * 
	 * @param importer
	 * @param fields
	 */
	public void setFields(ISamplingUnitImporter importer, String[] fields){
		for (Control kid : main.getChildren()){
			kid.dispose();
		}
		
		Group g = new Group(main, SWT.NONE);
		g.setText(Messages.AttributePage_RequiredLabels);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(g, SWT.NONE);
		l.setText(Messages.AttributePage_IdLabel);
		
		idViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		idViewer.setLabelProvider(new LabelProvider());
		idViewer.setContentProvider(ArrayContentProvider.getInstance());
		idViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (!attributesOnly){
			createGeometryAttributeFields(importer, g, fields);
		}
		
		Group g2 = new Group(main, SWT.NONE);
		g2.setText(Messages.AttributePage_OptionalLabel);
		g2.setLayout(new GridLayout(2, false));
		g2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		viewers = new HashMap<SamplingUnitAttribute, ComboViewer>();
		String[] attFields = new String[fields.length +1];
		attFields[0] = ""; //$NON-NLS-1$
		for (int i = 0; i < fields.length; i ++){
			attFields[i+1] = fields[i];
		}
		List<SurveyDesignSamplingUnitAttribute> atts = new ArrayList<SurveyDesignSamplingUnitAttribute>();
		atts.addAll(design.getSamplingUnitAttributes());
		Collections.sort(atts, new Comparator<SurveyDesignSamplingUnitAttribute>() {
			@Override
			public int compare(SurveyDesignSamplingUnitAttribute o1, SurveyDesignSamplingUnitAttribute o2) {
				return Collator.getInstance().compare(o1.getSamplingUnitAttribute().getName(), o2.getSamplingUnitAttribute().getName());
			}
		});
		
		for (SurveyDesignSamplingUnitAttribute sda: atts){
			l = new Label(g2, SWT.NONE);
			l.setText(sda.getSamplingUnitAttribute().getName() + ":"); //$NON-NLS-1$
			
			ComboViewer viewer = new ComboViewer(g2, SWT.DROP_DOWN | SWT.READ_ONLY);
			viewer.setLabelProvider(new LabelProvider());
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			viewer.setInput(attFields);
			
			viewers.put(sda.getSamplingUnitAttribute(), viewer);
		}
		
		if (attributesOnly){
			idViewer.setInput(fields);
			idViewer.setSelection(new StructuredSelection(fields[0]));
		}else{
			String[] idItems = new String[fields.length +1];
			idItems[0] = Messages.AttributePage_SystemGeneratedKeyLabel;
			for (int i = 0; i < fields.length; i ++){
				idItems[i+1] = fields[i];
			}
			idViewer.setInput(idItems);
			idViewer.setSelection(new StructuredSelection(idItems[0]));
		}
		
		
		Point p = main.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		main.setSize(p);
		sc.setMinSize(p);
	}
		
	private void createGeometryAttributeFields(ISamplingUnitImporter importer,
			Composite g, String[] fields) {
		Label l;

		if (importer.getClass().equals(CsvSamplingUnitImporter.class)) {

			if (((ImportWizard) getWizard()).getSamplingUnitType() == SamplingUnitType.PLOT) {
				l = new Label(g, SWT.NONE);
				l.setText(Messages.AttributePage_xLabel);

				xViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				xViewer.setLabelProvider(new LabelProvider());
				xViewer.setContentProvider(ArrayContentProvider.getInstance());
				xViewer.getControl().setLayoutData(
						new GridData(SWT.FILL, SWT.FILL, true, false));
				xViewer.setInput(fields);
				xViewer.getControl().setData(Messages.AttributePage_xLabel2);

				l = new Label(g, SWT.NONE);
				l.setText(Messages.AttributePage_yLabel);

				yViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				yViewer.setLabelProvider(new LabelProvider());
				yViewer.setContentProvider(ArrayContentProvider.getInstance());
				yViewer.getControl().setLayoutData(
						new GridData(SWT.FILL, SWT.FILL, true, false));
				yViewer.setInput(fields);
				yViewer.getControl().setData(Messages.AttributePage_yLabel2);
			} else {
				l = new Label(g, SWT.NONE);
				l.setText(Messages.AttributePage_x1Label);

				xViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				xViewer.setLabelProvider(new LabelProvider());
				xViewer.setContentProvider(ArrayContentProvider.getInstance());
				xViewer.getControl().setLayoutData(
						new GridData(SWT.FILL, SWT.FILL, true, false));
				xViewer.setInput(fields);
				xViewer.getControl().setData(Messages.AttributePage_x1Label2);

				l = new Label(g, SWT.NONE);
				l.setText(Messages.AttributePage_y1Label);

				yViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				yViewer.setLabelProvider(new LabelProvider());
				yViewer.setContentProvider(ArrayContentProvider.getInstance());
				yViewer.getControl().setLayoutData(
						new GridData(SWT.FILL, SWT.FILL, true, false));
				yViewer.setInput(fields);
				yViewer.getControl().setData(Messages.AttributePage_y1Label2);

				l = new Label(g, SWT.NONE);
				l.setText(Messages.AttributePage_x2Label);

				x2Viewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				x2Viewer.setLabelProvider(new LabelProvider());
				x2Viewer.setContentProvider(ArrayContentProvider.getInstance());
				x2Viewer.getControl().setLayoutData(
						new GridData(SWT.FILL, SWT.FILL, true, false));
				x2Viewer.setInput(fields);

				l = new Label(g, SWT.NONE);
				l.setText(Messages.AttributePage_y2Label);

				y2Viewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
				y2Viewer.setLabelProvider(new LabelProvider());
				y2Viewer.setContentProvider(ArrayContentProvider.getInstance());
				y2Viewer.getControl().setLayoutData(
						new GridData(SWT.FILL, SWT.FILL, true, false));
				y2Viewer.setInput(fields);
			}

			l = new Label(g, SWT.NONE);
			l.setText(Messages.AttributePage_ProjectionLabel);

			projViewer = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
			projViewer.setLabelProvider(ProjectionLabelProvider.getInstance());
			projViewer.setContentProvider(ArrayContentProvider.getInstance());
			projViewer.getControl().setLayoutData(
					new GridData(SWT.FILL, SWT.FILL, true, false));
			projViewer.setInput(proj);
		}
	}
	
	/**
	 * Validates to ensure the required fields are selected.
	 * 
	 * @return error message or null if no errors
	 */
	public String validate(){
		if (xViewer != null){
			if (getX1Field() == null || getX1Field().trim().length() == 0){
				return MessageFormat.format(Messages.AttributePage_FieldRequired, new Object[]{xViewer.getControl().getData()});
			}
		}
		if (yViewer != null){
			if (getY1Field() == null || getY1Field().trim().length() == 0){
				return MessageFormat.format(Messages.AttributePage_FieldRequired, new Object[]{yViewer.getControl().getData()});
			}
		}
		if (x2Viewer != null){
			if (getX2Field() == null || getX2Field().trim().length() == 0){
				return MessageFormat.format(Messages.AttributePage_FieldRequired, new Object[]{Messages.AttributePage_x2Label});
			}
		}
		if (y2Viewer != null){
			if (getY2Field() == null || getY2Field().trim().length() == 0){
				return MessageFormat.format(Messages.AttributePage_FieldRequired, new Object[]{Messages.AttributePage_y2Label});
			}
		}
		if (projViewer != null){
			if (getProjection() == null){
				return Messages.AttributePage_ProjectionRequired;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @return the id field or null if should be system generated
	 */
	public String getIdField(){
		if (!attributesOnly && idViewer.getCombo().getSelectionIndex() == 0){
			return null;
		}
		return (String) ((IStructuredSelection)idViewer.getSelection()).getFirstElement();
	}
	
	/**
	 * 
	 * @return x1 field or null if not applicable
	 */
	public String getX1Field(){
		if (xViewer == null) return null;
		return (String) ((IStructuredSelection)xViewer.getSelection()).getFirstElement();
	}
	
	/**
	 * 
	 * @return y1 field or null if not applicable
	 */
	public String getY1Field(){
		if (yViewer == null) return null;
		return (String) ((IStructuredSelection)yViewer.getSelection()).getFirstElement();
	}
	
	/**
	 * 
	 * @return x2 field or null if not applicable
	 */
	public String getX2Field(){
		if (x2Viewer == null) return null;
		return (String) ((IStructuredSelection)x2Viewer.getSelection()).getFirstElement();
	}
	
	/**
	 * 
	 * @return y2 field or null if not applicable
	 */
	public String getY2Field(){
		if (y2Viewer == null) return null;
		return (String) ((IStructuredSelection)y2Viewer.getSelection()).getFirstElement();
	}
	
	/**
	 * 
	 * @return projection value not null if not applicable
	 */
	public Projection getProjection(){
		if (projViewer == null) return null;
		return (Projection) ((IStructuredSelection)projViewer.getSelection()).getFirstElement();
	}
	
	/**
	 * 
	 * @return mapping of samplingunitattribute to attribute field
	 */
	public HashMap<SamplingUnitAttribute, String> getAttributeFields(){
		HashMap<SamplingUnitAttribute, String> items = new HashMap<SamplingUnitAttribute, String>();
		
		for(Entry<SamplingUnitAttribute, ComboViewer> entry : viewers.entrySet()){
			String value = null;
			if (!entry.getValue().getSelection().isEmpty()){
				value = (String) ((IStructuredSelection)entry.getValue().getSelection()).getFirstElement();
			}
			if (value != null && value.isEmpty()){
				value = null;
			}
			items.put(entry.getKey(), value);
		}
		return items;
	}

}