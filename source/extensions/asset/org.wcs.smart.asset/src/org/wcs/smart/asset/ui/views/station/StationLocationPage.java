package org.wcs.smart.asset.ui.views.station;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class StationLocationPage {

	private StationEditor parentEditor;
	
	public StationLocationPage(StationEditor editor) {
		this.parentEditor = editor;
	}
	
	public void createControl(Composite parent, FormToolkit toolkit) {
		toolkit.createLabel(parent, "CURRENT PAGE");
	}
}
