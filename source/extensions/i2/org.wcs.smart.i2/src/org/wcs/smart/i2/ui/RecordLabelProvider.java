package org.wcs.smart.i2.ui;

import java.text.DateFormat;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

public class RecordLabelProvider extends ColumnLabelProvider{

	public enum RecordField{
		DATE_RECIEVED,
		LAST_MODIFIED,
		TITLE;
		
		public String getLabel(IntelRecord record){
			switch(this){
			case DATE_RECIEVED:
				return DateFormat.getDateInstance().format(record.getDateCreated());
			case LAST_MODIFIED:
				return DateFormat.getDateInstance().format(record.getDateModified());
			case TITLE:
				return record.getTitle();
			default:
				break;
			
			}
			return "";
		}
	};
	
	private RecordField field;
	
	public RecordLabelProvider(){
		this(RecordField.TITLE);
	}
	
	public RecordLabelProvider(RecordField field){
		this.field = RecordField.TITLE;
	}
	
	public String getText(Object element){
		if (element instanceof IntelRecord){
			return field.getLabel((IntelRecord) element);
		}else if (element instanceof RecordEditorInput && field == RecordField.TITLE){
			return ((RecordEditorInput) element).getName();
		}
		return super.getText(element);
	}
}
