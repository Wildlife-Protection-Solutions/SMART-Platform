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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.parser.internal.filter.Operator;

/**
 * Aattribute drop item for numeric, text, and boolean attributes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeDropItem extends DropItem{

	private String text;
	private String key;
	
	private String currentValue = null;
	private String currentOp = null;	
	private Label lblAttribute;
	private Text value;
	private Combo operators;
	private AttributeType type = null;
	
	private Font smallerFont;
	
	/**
	 * Creates a new attribute drop item
	 * 
	 * @param parent parent composite
	 * @param target drop target
	 * @param att the category attribute to make up the drop item
	 */
	public AttributeDropItem(CategoryAttribute att) {
		//super(parent, target);
		this.type = att.getAttribute().getType();
		this.text = att.getAttribute().getName() + " (" + att.getCategory().getFullCategoryName() + ")";		
		this.key = "category:" + att.getCategory().getHkey() + ":attribute:" + att.getAttribute().getType().queryKey +":" + att.getAttribute().getKeyId();
	}
	
	/**
	 * Creates a new attribute drop item
	 * 
	 * @param parent parent composite
	 * @param target drop target
	 * @param att the category attribute to make up the drop item
	 */
	public AttributeDropItem(Attribute att) {
		//super(parent, target);
		this.type = att.getType();
		this.text = att.getName();
		this.key = "attribute:" + att.getType().queryKey + ":" + att.getKeyId();
	}
	
	/**
	 * For String or Numberic Attribute:<br>
	 * data is a string array of length two where the first 
	 * value represents the operator and second the associated value
	 * 
	 * For Boolean Attributes:<b>
	 * data should be null
	 * @param data - a string array of the operator and value
	 */
	public void initializeData(Object data){
		if (data != null){
			String[] d = (String[]) data;
			this.currentOp = d[0];
			this.currentValue = d[1];
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		if (operators != null){
			return this.text + " " + operators.getItem(operators.getSelectionIndex()) + " " ;//+ value.getText() ;
		}else{
			return this.text;
		}
		
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder querypart = new StringBuilder();
		if (type == AttributeType.NUMERIC){
			querypart.append (this.key);
			querypart.append( " ");
			querypart.append(Operator.NUMERIC_OPS[operators.getSelectionIndex()].asSmartValue());
			querypart.append(" ");
			querypart.append(value.getText());
			
		}else if (type == AttributeType.TEXT){
			querypart.append (this.key);
			querypart.append( " ");
			
			querypart.append(Operator.STRING_OPS[operators.getSelectionIndex()].asSmartValue());
			querypart.append(" \"");
			querypart.append(value.getText());
			querypart.append("\"");
		}else if (type == AttributeType.BOOLEAN){
			querypart.append(this.key);
		}
		return querypart.toString();
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
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);
		if (type == AttributeType.TEXT || type == AttributeType.NUMERIC) {
			operators = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
			operators.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (currentOp != null
							&& currentOp.equals(operators.getText())) {
						// no change
					} else {
						currentOp = operators.getText();
						queryChanged();
					}
				}
			});
			FontData fd = (operators.getFont().getFontData()[0]);
			fd.setHeight(fd.getHeight() - 1);
			smallerFont = new Font(Display.getCurrent(), fd);
			operators.setFont(smallerFont);

			value = new Text(main, SWT.BORDER);
			value.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					if (currentValue != null
							&& currentValue.equals(value.getText())) {
						// nothing changed
					} else {
						queryChanged();
						value.setToolTipText(value.getText());
						currentValue = value.getText();
					}
				}
			});
			GridData gd = new GridData();
			gd.minimumWidth = 50;
			gd.widthHint = 100;
			value.setLayoutData(gd);

			Operator[] options = null;
			if (type == AttributeType.NUMERIC) {
				options = Operator.NUMERIC_OPS;
			} else if (type == AttributeType.TEXT) {
				options = Operator.STRING_OPS;
			}
			if (options != null) {
				int index = 0;
				for (int i = 0; i < options.length; i++) {
					operators.add(options[i].asString());
					if (currentOp != null
							&& currentOp.equals(options[i].asString())) {
						index = i;
					}
				}
				operators.select(index);
			}
		}
		
		initDrag(main);
		initDrag(lblAttribute);
		
		lblAttribute.setText(this.text);
		if (currentValue != null){
			value.setText(currentValue);
		}
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isValueItem()
	 */
	@Override
	public boolean isValueItem(){
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isFilterItem()
	 */
	@Override
	public boolean isFilterItem(){
		return true;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isGroupByItem()
	 */
	@Override
	public boolean isGroupByItem(){
		return false;
	}

}
