package org.wcs.smart.i2.internal;

import java.util.Locale;

import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;

public class IntelligenceLabelProviderImpl implements
		IIntelligenceLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof IAttributeType){
			IAttributeType type = (IAttributeType) item;
			
			if (type == IAttributeType.BOOLEAN){
				return "BOOLEAN";
			}else if (type == IAttributeType.DATE){
				return "DATE";
			}else if (type == IAttributeType.LIST){
				return "LIST";
			}else if (type == IAttributeType.NUMERIC){
				return "NUMERIC";
			}else if (type == IAttributeType.TEXT){
				return "TEXT";
			}
		}
		// TODO Auto-generated method stub
		return null;
		
	}

}
