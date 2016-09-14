package org.wcs.smart.i2.ui.handler;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.editors.EntityEditor;
import org.wcs.smart.i2.ui.editors.EntityEditorInput;

public class OpenEntityHandler {

	
	public void openEntity(IntelEntity entity){
		EntityEditorInput input = new EntityEditorInput(entity.getIdAttributeAsText(), entity.getUuid(), entity.getEntityType());

		try {
			String pId = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getPerspective().getId();
			
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, EntityEditor.ID);
			if (editor instanceof EntityEditor){
				if (pId.equals(IntelDataAssessmentPerspective.ID)){
					((EntityEditor)editor).setEditMode(true);
				}
			}
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
