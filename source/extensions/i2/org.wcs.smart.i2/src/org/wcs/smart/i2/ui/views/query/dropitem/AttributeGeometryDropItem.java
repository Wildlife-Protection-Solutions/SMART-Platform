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
package org.wcs.smart.i2.ui.views.query.dropitem;

import java.util.Locale;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
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
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.i2.query.Operator;


/**
 * Attribute drop item for numeric, text, and boolean attributes in observation
 * filters
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeGeometryDropItem extends DropItem {

	protected String text;
	protected String key;
	
	private AttributeFilter.GeometryProperty currentProperty = null;
	private Double currentValue = null;
	private Operator currentOp = null;	
	
	
	private Label lblAttribute;
	private Text value;
	private Combo fields;
	private Combo operators;
	private AttributeType type = null;
	
	private Font smallerFont;
	private ControlDecoration cd;
	
	private boolean canEdit = true;
	
	private AttributeFilter.GeometryProperty[] propertyOptions;
	private static Operator[] operatorOptions = Operator.NUMERIC_OPS;

	/**
	 * Creates a new attribute drop item
	 * 
	 * @param parent parent composite
	 * @param target drop target
	 * @param att the category attribute to make up the drop item
	 */
	public AttributeGeometryDropItem(CategoryAttribute att) {
		this.type = att.getAttribute().getType();
		this.text = att.getAttribute().getName() + " (" + att.getCategory().getFullCategoryName() + ")";		 //$NON-NLS-1$ //$NON-NLS-2$
		this.key = "dm_attribute:" + att.getAttribute().getType().typeKey + ":" + att.getCategory().getHkey() +":" + att.getAttribute().getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (!this.type.isGeometry()) throw new IllegalStateException();
	}
	
	/**
	 * Creates a new attribute drop item
	 * 
	 */
	public AttributeGeometryDropItem(Attribute att) {
		this.type = att.getType();
		this.text = att.getName();
		this.key = "dm_attribute:" + att.getType().typeKey + "::" + att.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$
		if (!this.type.isGeometry()) throw new IllegalStateException();
	}
	
	/**
	 * Create a new attribute drop item
	 * 
	 * @param type the type
	 * @param text name
	 * @param key key
	 */
	public AttributeGeometryDropItem(Attribute.AttributeType type, String text, String key, boolean canEdit) {
		this.type = type;
		this.text = text;
		this.key = key;
		this.canEdit = canEdit;
		if (!this.type.isGeometry()) throw new IllegalStateException();
	}
	
	/**
	 * <p>
	 * For String or Numberic Attribute:
	 * data is a string array of length two where the first 
	 * value represents the operator and second the associated value
	 * </p><p>
	 * For Boolean Attributes:
	 * data should be null
	 * </p>
	 * @param data - a string array of the operator and value
	 */
	public void setData(AttributeFilter.GeometryProperty property, Operator op, Double value) {
		this.currentProperty = property;
		this.currentOp = op;
		this.currentValue = value;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		if (operators != null){
			return this.text + " " + operators.getItem(operators.getSelectionIndex()) + " " ;//+ value.getText() ; //$NON-NLS-1$ //$NON-NLS-2$
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
		
		querypart.append (this.key);
		querypart.append( " "); //$NON-NLS-1$
		querypart.append(getGeometryProperty().getKey() );
		querypart.append( " "); //$NON-NLS-1$
		querypart.append(Operator.NUMERIC_OPS[operators.getSelectionIndex()].getKey());
		querypart.append(" "); //$NON-NLS-1$
		querypart.append(value.getText());
			
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

	private AttributeFilter.GeometryProperty getGeometryProperty(){
		return propertyOptions[fields.getSelectionIndex()];
	}
	
	private Operator getOperator() {
		return operatorOptions[operators.getSelectionIndex()];
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
		main.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		lblAttribute = new Label(main, SWT.NONE);
		fields = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		fields.addListener(SWT.Modify, e->{
			if (currentProperty != null && currentProperty.equals(getGeometryProperty())) return; //no change
			currentProperty = getGeometryProperty();
			queryChanged();
		});
		
		if (type == AttributeType.POLYGON) {
			propertyOptions = new AttributeFilter.GeometryProperty[] {
					AttributeFilter.GeometryProperty.AREA,
					AttributeFilter.GeometryProperty.PERIMETER
			};
			
		}else {
			propertyOptions = new AttributeFilter.GeometryProperty[] {
				AttributeFilter.GeometryProperty.PERIMETER
			};
		}
		for (AttributeFilter.GeometryProperty prop : propertyOptions) {
			//TODO:
			fields.add(prop.name());
			if (prop == currentProperty) {
				fields.select(fields.getItemCount() - 1);
			}
		}
		if (fields.getSelectionIndex() < 0) fields.select(0);
		
		
		operators = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		operators.addListener(SWT.Modify, e->{
			if (currentOp != null && currentOp.equals(getOperator())) return; //no change
			currentOp = getOperator();
			queryChanged();
		});
		
		FontData fd = (operators.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		
		operators.setFont(smallerFont);
		fields.setFont(smallerFont);

		value = new Text(main, SWT.BORDER);
		value.addListener(SWT.Modify, e->{
			Double newValue = null;
			try {
				newValue = Double.parseDouble(value.getText());
				cd.hide();
			}catch (Exception ex) {
				cd.show();
			}
			
			if (currentValue != null && currentValue.equals(newValue)) return; //no change

			queryChanged();
			value.setToolTipText(value.getText());
			currentValue = newValue;
			
		});
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		value.setLayoutData(gd);
		((GridData)value.getLayoutData()).horizontalIndent = 5;
		
				
		cd = new ControlDecoration(value, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
		cd.setDescriptionText("valid numeric valid not provided");
		cd.show();
				
		for (int i = 0; i < operatorOptions.length; i++) {
			operators.add(operatorOptions[i].getLabel(Locale.getDefault()));
			if (currentOp != null && currentOp.equals(operatorOptions[i])) {
				operators.select(i);
			}
		}
		
		
		initDrag(main);
		initDrag(lblAttribute);
		
		lblAttribute.setText(formatStringForLabel(this.text));
		if (currentValue != null){
			if (value != null){
				value.setText(currentValue.toString());
			}
		
		}
		
		this.fields.setEnabled(canEdit);
		this.operators.setEnabled(canEdit);
		this.value.setEnabled(canEdit);
	}

}
