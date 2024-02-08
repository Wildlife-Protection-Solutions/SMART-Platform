package org.wcs.smart.event.i2;

import org.wcs.smart.ISharedLabelProvider;

public interface IProfileEventLabelProvider extends ISharedLabelProvider {

	public enum EventMessages{
		AdvIntelLabelProvider_CreateActionTypeMsg1,
		AdvIntelLabelProvider_CreateActionTypeMsg2,
		AdvIntelLabelProvider_CreateActionTypeMsg3,
		AdvIntelLabelProvider_CreateActionTypeMsg4,
		AdvIntelLabelProvider_CreateActionTypeMsg5,
		CreateEntityActionType_AttributeNotFound,
		CreateEntityActionType_InvalidProfile,
		CreateEntityActionType_ProfileNotFound,
		CreateEntityActionType_ProfileParameterNotSet,
		
		CreateRecordActionType_InvalidProfile,
		CreateRecordActionType_ProfileNotFound,
		CreateRecordActionType_ProfileParameterNotSet,
		CreateRecordActionType_WaypointIdLabel,
		
	}
}
