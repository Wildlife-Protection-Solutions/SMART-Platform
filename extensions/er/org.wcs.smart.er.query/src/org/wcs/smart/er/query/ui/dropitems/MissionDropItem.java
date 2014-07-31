package org.wcs.smart.er.query.ui.dropitems;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.query.filter.MissionFilter;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.util.SmartUtils;

public class MissionDropItem extends DropItem implements IFilterDropItem{

	private Mission mission;
	
	public MissionDropItem (Mission mission){
		this.mission = mission;
	}
	
	@Override
	public String getText() {
		return MissionFilter.UUID_QUERY_KEY + ":" + SmartUtils.encodeHex(mission.getUuid());
	}

	@Override
	public String asQueryPart() {
		return getText();
	}

	@Override
	public void initializeData(Object data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Mission: " + mission.getId() + " (" + mission.getSurvey().getId() + ")");
		
		
	}

}
