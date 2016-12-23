package org.wcs.smart.i2.ui.views.query;

import java.text.MessageFormat;

import org.wcs.smart.ca.Area;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

public class AreaFilterItem extends BasicFilterItem{

	private String key;
	private String type;
	
	public AreaFilterItem(Area area) {
		super(area.getName());
		this.key = area.getKeyId();
		this.type = area.getType().name();
	}

	
	@Override
	public DropItem[] asDropItem() {
		String queryKey = "area:" + type + ":" + key;
		return new DropItem[]{new TextDropItem(MessageFormat.format("{0} [{1}]", getName(), type), queryKey)};
	}
}
