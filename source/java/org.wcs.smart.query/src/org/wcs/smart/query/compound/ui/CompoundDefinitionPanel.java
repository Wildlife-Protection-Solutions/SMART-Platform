package org.wcs.smart.query.compound.ui;

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.ListDefinitionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;

public class CompoundDefinitionPanel extends ListDefinitionPanel {

	public static final String ID = "org.wcs.smart.query.compound.definition";
	
	private ListDefinitionPanel queryList;
	
	public CompoundDefinitionPanel() {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getGuiName() {
		return "Query List";
	}

	@Override
	public String validate() {
		// TODO Auto-generated method stub
		return null;
	}

}
