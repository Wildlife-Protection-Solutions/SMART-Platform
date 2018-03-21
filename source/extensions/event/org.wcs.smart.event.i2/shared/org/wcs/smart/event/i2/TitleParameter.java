package org.wcs.smart.event.i2;

import java.util.Locale;

import org.wcs.smart.event.model.IActionParameter;

public class TitleParameter implements IActionParameter {

public final static TitleParameter INSTANCE = new TitleParameter();
	
	@Override
	public String getKey() {
		return CreateRecordActionType.KEY + ".title";
	}

	@Override
	public String getName(Locale l) {
		return "Record Title";
	}

	@Override
	public boolean isRequired() {
		return false;
	}


}
