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
package org.wcs.smart.query.ui.model.impl;

import java.util.HashMap;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;


/**
 * A drop item that represents a value-attribute
 * for summary queries.  It can optionally have a category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AttributeValueDropItem extends AbstractValueDropItem {

	private Attribute attribute = null;
	private Category category = null; 
	
	private Aggregation selectedAggregation;
	private ComboViewer listViewer;
	
	private Font smallerFont;
	private HashMap<Aggregation, String> aggLabels = new HashMap<Aggregation, String>();
	
	public AttributeValueDropItem(boolean hasEncounter, Attribute attribute){
		super(hasEncounter);
		
		this.attribute = attribute;
		Job j = new Job("load aggregation labels") { //$NON-NLS-1$
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					for (Aggregation a : AttributeValueDropItem.this.attribute.getAggregations()){
						aggLabels.put(a, Aggregation.getGuiName(a, s, Locale.getDefault()));
					}
					s.getTransaction().commit();
				}finally{
					s.close();
				}
				return null;
			}
		};
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			QueryPlugIn.log(e.getMessage(),e);
		}
		if (this.attribute.getAggregations().size() > 0){
			selectedAggregation = this.attribute.getAggregations().get(0);
		}
		selectedAggregation = null;
	}
	
	public AttributeValueDropItem(boolean hasEncounter, CategoryAttribute categoryAttribute){
		this(hasEncounter, categoryAttribute.getAttribute());
		this.category = categoryAttribute.getCategory();
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		listViewer = null;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueText()
	 */
	@Override
	public String getValueText() {
		StringBuilder sb = new StringBuilder();
		if (selectedAggregation != null){
			sb.append(aggLabels.get(selectedAggregation));
		}
		sb.append(attribute.getName());
		if (category != null){
			sb.append(" (" + category.getName() + ") "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueQueryPart()
	 */
	@Override
	public String getValueQueryPart() {
		StringBuilder sb = new StringBuilder();
		if (category != null){
			sb.append("category:"); //$NON-NLS-1$
			sb.append(category.getHkey() + ":"); //$NON-NLS-1$
		}
		sb.append("attribute:"); //$NON-NLS-1$
		sb.append(attribute.getType().typeKey);
		sb.append(":"); //$NON-NLS-1$
		if (selectedAggregation != null){
			sb.append(selectedAggregation.getName());
		}
		sb.append(":"); //$NON-NLS-1$
		sb.append(attribute.getKeyId());
	
		return sb.toString();
	}

	/**
	 * @param data - the selected Aggregation
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeValueData(Object data) {
		selectedAggregation = (Aggregation)data;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createValueComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		if (attribute.getAggregations().size() == 0){
			GridLayout gl = new GridLayout(2, false);
			gl.marginTop = 0;
			gl.marginBottom = 0;
			gl.marginWidth = 0;
			gl.marginHeight = 0;
			main.setLayout(gl);
			main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
			
		}else if (attribute.getAggregations().size() == 1){
			//only have one option
			GridLayout gl = new GridLayout(2, false);
			gl.marginTop = 0;
			gl.marginBottom = 0;
			gl.marginWidth = 0;
			gl.marginHeight = 0;
			main.setLayout(gl);
			main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
			Label lblAgg = new Label(main, SWT.NONE);
			lblAgg.setText( formatStringForLabel(aggLabels.get(attribute.getAggregations().get(0))));
			selectedAggregation = attribute.getAggregations().get(0);
			initDrag(lblAgg);
		}else {
			GridLayout gl = new GridLayout(2, false);
			gl.marginTop = 0;
			gl.marginBottom = 0;
			gl.marginWidth = 0;
			gl.marginHeight = 0;
			main.setLayout(gl);
			main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
			
			//multiple options
			listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
			
			FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
			fd.setHeight(fd.getHeight() - 1);
			smallerFont = new Font(Display.getCurrent(), fd);
			listViewer.getCombo().setFont(smallerFont);
			listViewer.setContentProvider(ArrayContentProvider.getInstance());
			listViewer.setLabelProvider(new LabelProvider(){
				public String getText(Object element) {
					if (element instanceof Aggregation){
						return aggLabels.get(((Aggregation) element));
					}
					return super.getText(element);
				}
			});
			
			listViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					Aggregation newSelection = (Aggregation) ((IStructuredSelection)listViewer.getSelection()).getFirstElement();
					if (! (selectedAggregation != null && selectedAggregation.equals(newSelection))){
						selectedAggregation = newSelection;
						queryChanged();	
					}				 
				}
			});
			listViewer.setInput(this.attribute.getAggregations().toArray());
			
			if (selectedAggregation != null){
				listViewer.setSelection(new StructuredSelection(selectedAggregation));
			}
		}
		
		Label lblText = new Label(main, SWT.NONE);
		StringBuilder sb = new StringBuilder();
		StringBuilder tooltip = new StringBuilder();
		sb.append(attribute.getName());
		tooltip.append(attribute.getName());
		if (category != null){
			sb.append(" (" + category.getName() + ") "); //$NON-NLS-1$ //$NON-NLS-2$
			tooltip.append( " - "); //$NON-NLS-1$
			tooltip.append(category.getFullCategoryName());
		}
		lblText.setText( formatStringForLabel(sb.toString()));
		lblText.setToolTipText(tooltip.toString());
		
		initDrag(main);
		initDrag(lblText);

	}

	@Override
	public boolean isAllowed() {
		if (attribute.getAggregations() == null || attribute.getAggregations().isEmpty()) {
			setNotAllowedMessage(Messages.AttributeValueDropItem_NoAggregationOptionFound);
			return false;
		}
		return super.isAllowed();
	}
}
