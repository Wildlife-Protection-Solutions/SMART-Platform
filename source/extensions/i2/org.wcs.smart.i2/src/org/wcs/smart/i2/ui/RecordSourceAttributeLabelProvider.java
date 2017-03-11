/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;

/**
 * Record source label provider that provides the attribute label or entity type as label
 * @author Emily
 *
 */
public class RecordSourceAttributeLabelProvider extends ColumnLabelProvider implements IColorProvider{
	
	public enum Column{
		ROOTNAME_ICON,
		NAME,
		MULTI
		
	}
	private AttributeLabelProvider attributeProvider = new AttributeLabelProvider();
	private EntityTypeLabelProvider etProvider = new EntityTypeLabelProvider();
	
	private Column column;
	
	public RecordSourceAttributeLabelProvider(){
		this(Column.ROOTNAME_ICON);
	}
	
	public RecordSourceAttributeLabelProvider(Column column){
		this.column = column;
	}
	
	public String getText(Object element){
		if (element instanceof IntelRecordSourceAttribute){
			IntelRecordSourceAttribute a= (IntelRecordSourceAttribute)element;
			if (column == Column.MULTI){
				if ((a.getAttribute() != null && a.getAttribute().getType() == AttributeType.LIST) || a.getEntityType() != null){
					if (a.getIsMultiple() != null && a.getIsMultiple()){
						//true
						return Messages.RecordSourceAttributeLabelProvider_YesLabel;
					}else{
						//false
						return Messages.RecordSourceAttributeLabelProvider_NoLabel;
					}
				}else{
					return null;
				}
			}
			if (column == Column.NAME){
				if (a.getName()!=null&&!a.getName().isEmpty()){
					return a.getName();
				}
			}
			if (a.getAttribute() != null) return attributeProvider.getText(a.getAttribute());
			if (a.getEntityType() != null) return etProvider.getText((Object)a.getEntityType());
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element){
		if (element instanceof IntelRecordSourceAttribute){
			IntelRecordSourceAttribute a = (IntelRecordSourceAttribute)element;
			if (column == Column.MULTI){
				return null;
			}
			
			if (column == Column.ROOTNAME_ICON){
				if (a.getAttribute() != null) return attributeProvider.getImage(a.getAttribute());
				if (a.getEntityType() != null) return etProvider.getImage(a.getEntityType());
			}
		}
		return super.getImage(element);
	}
	
	@Override
	public Color getForeground(Object element) {
		if (element instanceof IntelRecordSourceAttribute){
			IntelRecordSourceAttribute a= (IntelRecordSourceAttribute)element;
			if (column == Column.NAME){
				if (a.getName() == null || a.getName().isEmpty()){
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
			}
		}
		return null;
	}
	
	@Override
	public Color getBackground(Object element) {
		return null;
	}
	
	@Override
	public void dispose(){
		attributeProvider.dispose();
		etProvider.dispose();
		super.dispose();
	}
}