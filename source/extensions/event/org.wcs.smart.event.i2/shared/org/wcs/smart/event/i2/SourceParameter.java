package org.wcs.smart.event.i2;

import java.util.Locale;

import org.wcs.smart.event.model.IActionParameter;

public class SourceParameter implements IActionParameter{

	public final static SourceParameter INSTANCE = new SourceParameter();
	
	@Override
	public String getKey() {
		return CreateRecordActionType.KEY + ".source";
	}

	@Override
	public String getName(Locale l) {
		return "Record Source";
	}

	@Override
	public boolean isRequired() {
		return false;
	}

}
