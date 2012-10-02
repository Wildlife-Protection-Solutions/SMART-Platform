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
package org.wcs.smart.ui.internal.ca.properties;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Category;

/**
 * Category information panel for displaying and editing
 * category information.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class CategoryInfoPanel extends NameKeyComposite {

	protected Button chMultiple;
	protected Language lang;

	/**
	 * Creates a new category information panel
	 * @param parent
	 * @param style
	 * @param canEdit  if the fields should be editable or only viewable
	 * @param createNew if the current category is being modified or created 
	 * @param lang
	 */
	public CategoryInfoPanel(Composite parent, int style, boolean canEdit, boolean createNew, Language lang) {
		super(parent, style);
		this.lang = lang;
		setLayout(new GridLayout(3, false));
		
		createNameKeyFields(this, canEdit, createNew);
		
		new Label(this, SWT.NONE);
		
		chMultiple = new Button(this, SWT.CHECK);
		chMultiple.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		chMultiple.setText("Can have multiple observations");
		chMultiple.setSelection(true);
		if (!canEdit){
			chMultiple.setEnabled(false);
		}else{
			chMultiple.addSelectionListener(new SelectionAdapter(){

				@Override
				public void widgetSelected(SelectionEvent e) {
					validate();	
				}
			});
		}
		
	}

	/**
	 * Updates the fields of the composite with the values
	 * from the category.
	 * @param c the category
	 */
	public void setCategory(Category c){
		initFields(c, lang);
		chMultiple.setSelection(c.getIsMultiple());
	}
	/**
	 * Updates the given category with the fields
	 * from the gui.
	 * 
	 * @param c the category to update
	 */
	public void updateCategory(Category c){
		updateFields(c, lang);
		c.setIsMultiple(chMultiple.getSelection());
	}
	
	@Override
	protected boolean validate(){
		return super.validate();
	}
}
