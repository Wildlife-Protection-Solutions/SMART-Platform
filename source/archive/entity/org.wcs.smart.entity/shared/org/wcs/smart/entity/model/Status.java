package org.wcs.smart.entity.model;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.entity.IEntityLabelProvider;

public enum Status {
	
	ACTIVE, INACTIVE;
		
	public String getGuiName(Locale l){
		return SmartContext.INSTANCE.getClass(IEntityLabelProvider.class).getLabel(this, l);
	}
}
