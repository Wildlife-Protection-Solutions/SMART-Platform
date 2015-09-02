package org.wcs.smart.dataentry;

import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttributeOption;

public enum CmAttributeOptionLabelProvider {
	INSTANCE;
	
	public String getGuiName(CmAttributeOption.EnterOnceType optiontype){
		switch(optiontype){
			case END: return Messages.CmAttributeOption_EnterOnceType_END;
			case NONE: return Messages.CmAttributeOption_EnterOnceType_NONE;
			case START: return Messages.CmAttributeOption_EnterOnceType_START;
		}
		return "";
	}

}
