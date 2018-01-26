package org.wcs.smart.asset.query.internal;

import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.asset.query.parser.internal.summary.AssetValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.ValueItemLabelProvider;

public class AssetValueItemLabelProvider extends ValueItemLabelProvider {

	public static AssetValueItemLabelProvider INSTANCE = new AssetValueItemLabelProvider();
	
	protected AssetValueItemLabelProvider() {
		super();
	}
	
	@Override
	public String getName(IValueItem item, Session session){
		if (item instanceof AssetValueItem){
			return getName((AssetValueItem)item, session);
		}
		return super.getName(item, session);
	}
	
	@Override
	public String getFullName(IValueItem item, Session session){
		if (item instanceof AssetValueItem){
			return getName((AssetValueItem)item, session);
		}
		return super.getFullName(item, session);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(AssetValueItem item, Session session){
		return item.getAssetValueOption().getGuiName(Locale.getDefault());
	}


}
