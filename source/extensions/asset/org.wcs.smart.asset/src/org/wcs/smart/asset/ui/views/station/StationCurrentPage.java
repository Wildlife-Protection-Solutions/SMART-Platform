package org.wcs.smart.asset.ui.views.station;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class StationCurrentPage {

	private StationEditor parentEditor;
	
	public StationCurrentPage(StationEditor editor) {
		this.parentEditor = editor;
	}
	
	public void createControl(Composite parent, FormToolkit toolkit) {
		toolkit.createLabel(parent, "CURRENT PAGE");
	}
}
