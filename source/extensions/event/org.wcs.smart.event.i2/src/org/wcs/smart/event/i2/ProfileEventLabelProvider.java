package org.wcs.smart.event.i2;

import java.util.Locale;

import org.wcs.smart.event.i2.internal.Messages;

public class ProfileEventLabelProvider implements IProfileEventLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg1) return Messages.AdvIntelLabelProvider_CreateActionTypeMsg1;
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg2) return Messages.AdvIntelLabelProvider_CreateActionTypeMsg2;
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg3) return Messages.AdvIntelLabelProvider_CreateActionTypeMsg3;
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg4) return Messages.AdvIntelLabelProvider_CreateActionTypeMsg4;
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg5) return Messages.AdvIntelLabelProvider_CreateActionTypeMsg5;
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_AttributeNotFound) return Messages.CreateEntityActionType_AttributeNotFound;
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_InvalidProfile) return Messages.CreateEntityActionType_InvalidProfile;
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_ProfileNotFound) return Messages.CreateEntityActionType_ProfileNotFound;
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_ProfileParameterNotSet) return Messages.CreateEntityActionType_ProfileParameterNotSet;
		
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_InvalidProfile) return Messages.CreateRecordActionType_InvalidProfile;
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_ProfileNotFound) return Messages.CreateRecordActionType_ProfileNotFound;
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_ProfileParameterNotSet) return Messages.CreateRecordActionType_ProfileParameterNotSet;
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_WaypointIdLabel) return Messages.CreateRecordActionType_WaypointIdLabel;
		
		return null;
	}

}
