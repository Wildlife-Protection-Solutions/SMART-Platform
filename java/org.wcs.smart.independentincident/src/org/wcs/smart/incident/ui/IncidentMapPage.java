package org.wcs.smart.incident.ui;

import org.eclipse.ui.part.MultiPageEditorPart;
import org.wcs.smart.ui.map.SmartMapEditorPart;

public class IncidentMapPage extends SmartMapEditorPart {

	private IncidentEditor parent;
	
	public IncidentMapPage(IncidentEditor e){
		this.parent = e;
	}
	
	@Override
	public MultiPageEditorPart getParentEditor() {
		return parent;
	}

}
