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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

/**
 * Label provider for IntelRecord and intelRecordEditor input
 * @author Emily
 *
 */
public class RecordLabelProvider extends ColumnLabelProvider{

	public enum RecordField{
		DATE_CREATED,
		LAST_MODIFIED,
		TITLE,
		TITLE_CREATED;
		
		public String getLabel(IntelRecord record){
			switch(this){
			case DATE_CREATED:
				return DateFormat.getDateInstance().format(record.getDateCreated());
			case LAST_MODIFIED:
				return DateFormat.getDateInstance().format(record.getDateModified());
			case TITLE:
				return record.getTitle();
			case TITLE_CREATED:
				return MessageFormat.format("{0} ({1})", record.getTitle(), DateFormat.getDateInstance().format(record.getDateCreated()));
			default:
				break;
			
			}
			return "";
		}
	};
	
	private RecordField field;
	
	public RecordLabelProvider(){
		this(RecordField.TITLE_CREATED);
	}
	
	public RecordLabelProvider(RecordField field){
		this.field = field;
	}
	
	public String getText(Object element){
		if (element instanceof IntelRecord){
			return field.getLabel((IntelRecord) element);
		}else if (element instanceof RecordEditorInput && field == RecordField.TITLE){
			return ((RecordEditorInput) element).getName();
		}else if (element instanceof RecordEditorInput && field == RecordField.TITLE_CREATED){
			return MessageFormat.format("{0} ({1})", ((RecordEditorInput) element).getName(), DateFormat.getDateInstance().format(((RecordEditorInput) element).getDateCreated()));
		}
		return super.getText(element);
	}
	
	/**
	 * Converts record status to GUI label
	 * @param status
	 * @return
	 */
	public static String getRecordStatusLabel(IntelRecord.Status status) {
		return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(status, Locale.getDefault());
	}
	
	/**
	 * Gets an image for the record status
	 * @param status
	 * @return
	 */
	public static Image getRecordStatusImage(IntelRecord.Status status) {
		return null;
	}
}
