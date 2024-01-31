package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.event.i2.IProfileEventLabelProvider;

public class ProfileEventLabelProvider implements IProfileEventLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
				
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg1) return "Record automatically created by trigger system (Action: {0}; Filter: {1})";
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg2) return "Waypoint Source: {0}";
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg3) return "Waypoint Date: {0}";
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg4) return "Waypoint Comment: {0}";
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg5) return "Observation: {0}";
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_AttributeNotFound) return "Not attribute with key {0} found for entity type {1}. Attribute will not be mapped.";
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_InvalidProfile) return "Error Creating Profile Entity (Triggers Module): The profile {0} is not associated with the entity type {1}.  Cannot create new entity.";
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_ProfileNotFound) return "Error Creating Profile Entity (Triggers Module): Profile not found.";
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_ProfileParameterNotSet) return "Error Creating Profile Entity (Triggers Module): Unable to find profile to associate with entity (parameter not set).";
		
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_InvalidProfile) return "Error Creating Profile Record (Triggers Module): The profile {0} is not associated with the record source {1}.  Cannot create new record.";
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_ProfileNotFound) return "Error Creating Profile Record (Triggers Module): Profile not found.";
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_ProfileParameterNotSet) return "Error Creating Profile Record (Triggers Module): Unable to find profile to associate with record (parameter not set).";
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_WaypointIdLabel) return "Waypoint ID: {0}";
		
		return null;
	}

}
