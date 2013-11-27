package org.wcs.smart.incident.ui;

import org.eclipse.swt.widgets.Composite;
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
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
	}


}
