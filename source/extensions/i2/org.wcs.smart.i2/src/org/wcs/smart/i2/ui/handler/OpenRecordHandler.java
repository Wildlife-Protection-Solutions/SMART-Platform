package org.wcs.smart.i2.ui.handler;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.editors.EntityEditor;
import org.wcs.smart.i2.ui.editors.EntityEditorInput;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

public class OpenRecordHandler {

	public void openRecord(RecordEditorInput input, boolean editMode){
		try {
			String pId = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getPerspective().getId();
			
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, RecordEditor.ID);
			if (editor instanceof RecordEditor){
				if (pId.equals(IntelDataAssessmentPerspective.ID) || editMode){
					((RecordEditor)editor).setEditMode(true);
				}
			}
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void openRecord(IntelRecord record, boolean editMode){
		
		RecordEditorInput input = new RecordEditorInput(record);
		openRecord(input, editMode);
		
		
	}
}
