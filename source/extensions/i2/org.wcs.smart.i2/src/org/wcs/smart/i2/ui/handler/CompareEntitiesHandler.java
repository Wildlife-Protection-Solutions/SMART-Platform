package org.wcs.smart.i2.ui.handler;

import java.util.List;

import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.birt.IntelEntityReportPerspective;
import org.wcs.smart.i2.birt.IntelRecordTemplateEditorInput;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.ui.editors.EntityComparisonEditor;
import org.wcs.smart.i2.ui.editors.EntityComparisonInput;

public class CompareEntitiesHandler {

	public void compare(List<IntelEntity> entities) throws Exception{
		IntelEntityType type = null;
		for (IntelEntity e : entities){
			if (type == null){
				type = e.getEntityType();
			}else{
				if (!e.getEntityType().equals(type)){
					throw new Exception("Cannot compare entities of different types.");
				}
			}
		}
		if (entities.isEmpty()) throw new Exception("Must select as least on entity.");
		
		//TODO: look for a part to add this to before creating a new editor
		EntityComparisonInput input = new EntityComparisonInput(type, entities);

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, EntityComparisonEditor.ID);
	}
}
