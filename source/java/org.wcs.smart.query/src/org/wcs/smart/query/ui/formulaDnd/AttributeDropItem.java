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
import org.wcs.smart.query.parser.internal.Operator;

/**
 * Aattribute drop item for numeric, text, and boolean attributes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeDropItem extends DropItem{

	private String text;
	private String key;
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
	public AttributeDropItem(Composite parent, DropTargetPanel target, CategoryAttribute att) {
		super(parent, target);

		this.type = att.getAttribute().getType();
		this.text = att.getAttribute().getName() + " (" + att.getCategory().getFullCategoryName() + ")";
		lblAttribute.setText(this.text);		
		if (att.getAttribute().getType() == AttributeType.NUMERIC){
			this.key = "category:" + att.getCategory().getHkey() + " and attribute:n:" + att.getAttribute().getKeyId();
			for (int i = 0; i < Operator.NUMERIC_OPS.length; i ++){
				operators.add(Operator.NUMERIC_OPS[i].asString());
			}
		}else if (att.getAttribute().getType() == AttributeType.TEXT){
			this.key = "category:" + att.getCategory().getHkey() + " and attribute:s:" + att.getAttribute().getKeyId();			
			for (int i = 0; i < Operator.STRING_OPS.length; i ++){
				operators.add(Operator.STRING_OPS[i].asString());
			}
		}
		operators.select(0);
	}
	
	/**
	 * Creates a new attribute drop item
	 * 
	 * @param parent parent composite
	 * @param target drop target
	 * @param att the category attribute to make up the drop item
	 */
	public AttributeDropItem(Composite parent, DropTargetPanel target, Attribute att) {
		super(parent, target);
		this.type = att.getType();
		this.text = att.getName();
		lblAttribute.setText(this.text);		
		if (att.getType() == AttributeType.NUMERIC){
			this.key = "attribute:n:" + att.getKeyId();
			for (int i = 0; i < Operator.NUMERIC_OPS.length; i ++){
				operators.add(Operator.NUMERIC_OPS[i].asString());
			}
		}else if (att.getType() == AttributeType.TEXT){
			this.key = "attribute:s:" + att.getKeyId();
			for (int i = 0; i < Operator.STRING_OPS.length; i ++){
				operators.add(Operator.STRING_OPS[i].asString());
			}
		}
		operators.select(0);
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return this.text + " " + operators.getItem(operators.getSelectionIndex()) + " " ;//+ value.getText() ;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		if (type == AttributeType.NUMERIC){
			return this.key + " " + operators.getItem(operators.getSelectionIndex()) + " " + value.getText() ;
		}else if (type == AttributeType.TEXT){
			return this.key + " " + operators.getItem(operators.getSelectionIndex()) + " \"" + value.getText() + "\"";
		}
		return null;
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
	public void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);
		operators = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		
		FontData fd = (operators.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		operators.setFont(smallerFont);
		
		value = new Text(main, SWT.BORDER);
		value.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				fireListeners();
				value.setToolTipText(value.getText());
			}
		});
		GridData gd = new GridData();
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		value.setLayoutData(gd);
		
		initDrag(main);
		initDrag(lblAttribute);
	}

}
