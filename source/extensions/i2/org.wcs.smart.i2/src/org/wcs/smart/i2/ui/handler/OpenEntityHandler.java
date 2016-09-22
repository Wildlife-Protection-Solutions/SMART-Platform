package org.wcs.smart.i2.ui.handler;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.editors.EntityEditor;
import org.wcs.smart.i2.ui.editors.EntityEditorInput;
import org.wcs.smart.util.E3Utils;

public class OpenEntityHandler {

	public void openEntity(IntelEntity entity, IEclipseContext context){
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
