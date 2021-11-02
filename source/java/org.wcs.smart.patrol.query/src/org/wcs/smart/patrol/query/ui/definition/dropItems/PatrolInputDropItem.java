/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.ui.definition.dropItems;

import java.util.Locale;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import org.wcs.smart.filter.Operator;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IPatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;

/**
 * Patrol attribute drop item for string and numeric attributes
 *  
 * @author Emily
 * @since 7.4.0
 */
public class PatrolInputDropItem  extends DropItem implements IFilterDropItem{

	private String text;
	private String key;
	
	private Label lblAttribute;
	private Text value;
	private Combo operators;
	
	private Font smallerFont;
	private ControlDecoration cd;
	
	private String currentValue = null;
	private String currentOp = null;

	private Operator[] ops;
	
	private IPatrolQueryOption poption;
	
	/**
	 * Creates a new patrol text drop item
	 * 
	 * @param parent parent
	 * @param target drop panel target
	 * @param PatrolFilterOption id patrol filter option
	 */
	public PatrolInputDropItem(IPatrolQueryOption option) {
		//super(parent, target);
		this.poption = option;
		if (option.getType() == PatrolQueryOptionType.STRING) {
			ops =  Operator.STRING_OPS;
		}else if (option.getType() == PatrolQueryOptionType.NUMBER) {
			ops = Operator.NUMERIC_OPS;
		}
		
		this.text = option.getGuiName(Locale.getDefault());
		this.key = option.getKey();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return this.text + " " + ops[operators.getSelectionIndex()].getGuiValue() + " " ;//+ value.getText() ; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		if (poption.getType() == PatrolQueryOptionType.NUMBER) {
			return this.key + " " +  ops[operators.getSelectionIndex()].asSmartValue() + " " + value.getText() + ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$	
		}
		return this.key + " " +  ops[operators.getSelectionIndex()].asSmartValue() + " \"" + value.getText() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		operators = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		
		FontData fd = (operators.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		operators.setFont(smallerFont);
		operators.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentOp != null && currentOp.equals(operators.getText())){
					//do nothing as has not changed
				}else{
					queryChanged();
					currentOp = operators.getText();
				}
			}
		});
		
		value = new Text(main, SWT.BORDER);
		value.setFont(smallerFont);
		
		GridData gd = new GridData();
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		value.setLayoutData(gd);
		
		value.addModifyListener(new ModifyListener() {			
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentValue != null && currentValue.equals(value.getText())){
					//ignore; not changed
				}else{
					queryChanged();
					value.setToolTipText(value.getText());
					currentValue = value.getText();
				}
				if (poption.getType() == PatrolQueryOptionType.NUMBER){
					try {
						Double.parseDouble(currentValue);
						cd.hide();
					}catch (Exception ex) {
						cd.show();
					}
				}
			}
		});
		
		if (poption.getType() == PatrolQueryOptionType.NUMBER){
			
			cd = new ControlDecoration(value, SWT.LEFT);
			cd.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
			cd.setDescriptionText(Messages.PatrolInputDropItem_invalidnumber);
			cd.show();
			((GridData)value.getLayoutData()).horizontalIndent = 5;

		}
		
		
		initDrag(main);
		initDrag(lblAttribute);
		
		
		lblAttribute.setText(formatStringForLabel(this.text));
		
		int index = 0;
		for (int i = 0; i < ops.length; i ++){
			operators.add(ops[i].getGuiValue());
			if (currentOp != null && ops[i].getGuiValue().equals(currentOp)){
				index =i;
			}
		}
		operators.select(index);
		if (currentValue != null){
			value.setText(currentValue);
		}
	}

	/**
	 * @param data an array of string containing the operator gui value and filter value
	 */
	@Override
	public void initializeData(Object data) {
		this.currentOp = ((String[])data)[0];
		this.currentValue = ((String[])data)[1];
	}
	
}
