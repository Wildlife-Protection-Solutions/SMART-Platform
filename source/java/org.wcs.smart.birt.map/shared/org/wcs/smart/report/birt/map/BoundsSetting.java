package org.wcs.smart.report.birt.map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.wcs.smart.report.birt.map.item.LayerItem;

public class BoundsSetting {

	public enum BoundsOption{
		MAP_EXTENTS,
		ALL_QUERY_LAYERS,
		LAYER,
		CUSTOM
	};
	
	private BoundsOption option;
	private ReferencedEnvelope re;
	
	private LayerItem item;
	
	public BoundsSetting() {
		this(BoundsOption.MAP_EXTENTS);
	}
	public BoundsSetting(BoundsOption option) {
		this.option = option;
		this.re = null;
	}
	
	public BoundsSetting(ReferencedEnvelope env) {
		this.option = BoundsOption.CUSTOM;
		this.re = env;
	}
	
	public BoundsOption getOption() {
		return this.option;
	}
	
	public ReferencedEnvelope getEnvelope() {
		return re;
	}
	
	public LayerItem getLayerItem() {
		return item;
	}
	
	public void setLayerItem(LayerItem item) {
		this.item  = item;
	}
	
	public BoundsSetting clone() {
		BoundsSetting clone = new BoundsSetting();
		clone.option = option;
		if (re != null) {
			clone.re = new ReferencedEnvelope(re);
		}
		return clone;		
	}
}
