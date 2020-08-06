package org.wcs.smart.asset.query.model;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;


/**
 * Represents the possible asset values for summary
 * queries.
 * 
 * @author egouge
 * @since 1.0.0
 */
public enum AssetValueOption {

	ASSET_HOURS("assethours", AssetDeployment.class), //$NON-NLS-1$
	ASSET_ACTIVEHOURS("assetactivehours", AssetDeployment.class); //$NON-NLS-1$
	
	String key;		//unique key
	Class<?> clazz; //class that contains the value variable
	
	AssetValueOption(String queryKey, Class<?> clazz){
		this.key = queryKey;
		this.clazz = clazz;
	}
	
	public String getGuiName(Locale l){
		return SmartContext.INSTANCE.getClass(IQueryAssetLabelProvider.class).getLabel(this, l);
	}
			
	public String getKeyPart(){
		return this.key;
	}
	
	public Class<?> getOptionClass(){
		return this.clazz;
	}
}

