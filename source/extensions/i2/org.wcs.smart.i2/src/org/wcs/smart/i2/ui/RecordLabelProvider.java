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

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.security.IntelSecurityManager;
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
		TITLE_CREATED,
		STATUS,
		SOURCE,
		PROFILE,
		PRIMARY_DATE;
		
		public String getLabel(IntelRecord record){
			switch(this){
			case DATE_CREATED:
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(record.getDateCreated());
			case LAST_MODIFIED:
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(record.getDateModified());
			case PRIMARY_DATE:
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(record.getPrimaryDate());
			case STATUS:
				return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(record.getStatus(), Locale.getDefault());
			case PROFILE:
				return record.getProfile().getName();
			case SOURCE:
				if (record.getRecordSource() == null) return ""; //$NON-NLS-1$
				return record.getRecordSource().getName();
			case TITLE:
				return record.getTitle();
			case TITLE_CREATED:
				return MessageFormat.format(Messages.RecordLabelProvider_0Title1DateCreated, record.getTitle(), DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(record.getDateCreated()));
			default:
				break;
			
			}
			return ""; //$NON-NLS-1$
		}
	};
	
	private RecordField field;
	
	public RecordLabelProvider(){
		this(RecordField.TITLE_CREATED);
	}
	
	public RecordLabelProvider(RecordField field){
		this.field = field;
	}
	
	
	@Override
	public String getText(Object element){
		if (element instanceof IntelRecord){
			IntelRecord record = (IntelRecord)element;
			if (field == RecordField.PROFILE) {
				return field.getLabel(record);
			}
			if (IntelSecurityManager.INSTANCE.canViewRecords( record.getProfile())) {
				return field.getLabel(record);
			}else {
				if (field == RecordField.TITLE) return IntelligenceLabelProviderImpl.INSUFFICIENT_PRIVILEGES;
				return ""; //$NON-NLS-1$
			}
		}else if (element instanceof RecordEditorInput) {
			RecordEditorInput input = (RecordEditorInput)element;
			IntelProfile temp = new IntelProfile();
			temp.setUuid(input.getRecordProfileUuid());
			if (IntelSecurityManager.INSTANCE.canViewRecords(temp)) {
				if (field == RecordField.TITLE){
					return input.getName();
				}else if (field == RecordField.TITLE_CREATED){
					return MessageFormat.format(Messages.RecordLabelProvider_0Name1DateCreated, input.getName(), DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(input.getDateCreated()));
				}
			}else {
				if (field == RecordField.TITLE) return IntelligenceLabelProviderImpl.INSUFFICIENT_PRIVILEGES;
				return ""; //$NON-NLS-1$
			}
		}
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element){
		
		
		if (field == RecordField.PROFILE) {
			if (element instanceof IntelRecord) {
				IntelRecord record = (IntelRecord)element;
				return Resources.INSTANCE.getImage( record.getProfile() );
			}else if (element instanceof RecordEditorInput) {
				RecordEditorInput i = (RecordEditorInput)element;
				IntelProfile t = new IntelProfile();
				t.setUuid(i.getRecordProfileUuid());
				return Resources.INSTANCE.getImage(t);
			}
		}
		if (field == RecordField.TITLE) {
			IntelRecordSource src = null;
			if (element instanceof IntelRecord){
				src = ((IntelRecord) element).getRecordSource();
			}else if (element instanceof RecordEditorInput){
				src = new IntelRecordSource();
				src.setUuid(((RecordEditorInput) element).getRecordSourceUuid());
			}
			if (src != null) return Resources.INSTANCE.getImage(src);
		}
		if (field == RecordField.STATUS) {
			if (element instanceof IntelRecord){
				if (IntelSecurityManager.INSTANCE.canViewRecords(((IntelRecord)element).getProfile())) {
					return Resources.INSTANCE.getImage(((IntelRecord) element).getStatus());
				}
			}
		}
			
		return null;
	}
	
	/**
	 * Converts record status to GUI label
	 * @param status
	 * @return
	 */
	public static String getRecordStatusLabel(IntelRecord.Status status) {
		return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(status, Locale.getDefault());
	}
}
