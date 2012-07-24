package org.wcs.smart.query.ui.gridded;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.query.model.SummaryQueryResult;

public class GriddedResults extends Composite{
	
	private GriddedResults results;

	GriddedResults(Composite parent, GriddedResults results, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		
		this.results = results;
	}

}
