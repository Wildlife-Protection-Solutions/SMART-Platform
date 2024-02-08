package org.wcs.smart.smartcollection.json;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.smartcollect.model.ISmartCollectLabelProvider;

public class SmartCollectJsonImportWarning extends JsonImportWarning {

	public enum WarningType{
		NO_USER,
		MISSING_DEVICE_ID,
		FEATURE_DISCARDED,
		USER_BLACKLISTED_FEATURE_DISCARDED;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(ISmartCollectLabelProvider.class).getLabel(this, l);
		}
	}
	

	public SmartCollectJsonImportWarning(WarningType type, Object...data) {
		super(l->type.getMessage(l), data);
	}
}
