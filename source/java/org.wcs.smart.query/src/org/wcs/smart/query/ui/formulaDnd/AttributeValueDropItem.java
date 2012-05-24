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
package org.wcs.smart.query.ui.formulaDnd;

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
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;

/**
 * A drop item that represents a value-attribute
 * for summary queries.  It can optionally have a category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AttributeValueDropItem extends DropItem {

	private Attribute attribute = null;;
	private Category category = null; 
	
	private Aggregation selectedAggregation;
	private ComboViewer listViewer;
	
	private Font smallerFont;
	
	public AttributeValueDropItem(Attribute attribute){
		this.attribute = attribute;
		if (this.attribute.getAggregations().size() > 0){
			selectedAggregation = this.attribute.getAggregations().get(0);
		}
		selectedAggregation = null;
	}
	
	public AttributeValueDropItem(CategoryAttribute categoryAttribute){
		this(categoryAttribute.getAttribute());
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
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		if (selectedAggregation != null){
			sb.append(selectedAggregation.getGuiName());
		}
		sb.append(attribute.getName());
		if (category != null){
			sb.append(" (" + category.getName() + ") ");
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		if (category != null){
			sb.append("category:");
			sb.append(category.getHkey() + ":");
		}
		sb.append("attribute:");
		sb.append(attribute.getType().typeKey);
		sb.append(":");
		if (selectedAggregation != null){
			sb.append(selectedAggregation.getName());
		}
		sb.append(":");
		sb.append(attribute.getKeyId());
	
		return sb.toString();
	}

	/**
	 * @param data - the selected Aggregation
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {
		selectedAggregation = (Aggregation)data;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isValueItem()
	 */
	@Override
	public boolean isValueItem() {
		return true;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isFilterItem()
	 */
	@Override
	public boolean isFilterItem() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isGroupByItem()
	 */
	@Override
	public boolean isGroupByItem() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
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
			lblAgg.setText(attribute.getAggregations().get(0).getGuiName());
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
						return ((Aggregation) element).getGuiName();
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
		sb.append(attribute.getName());
		if (category != null){
			sb.append(" (" + category.getName() + ") ");
		}
		lblText.setText(sb.toString());
		
		initDrag(main);
		initDrag(lblText);

	}

}
