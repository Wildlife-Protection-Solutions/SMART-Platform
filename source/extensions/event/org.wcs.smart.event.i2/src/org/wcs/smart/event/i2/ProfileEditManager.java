/*
 * Copyright (C) 2020 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.event.i2;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.event.i2.entity.CreateEntityActionType;
import org.wcs.smart.event.i2.entity.EntityMapping;
import org.wcs.smart.event.i2.entity.EntityTypeParameter;
import org.wcs.smart.event.i2.entity.MappingParameter;
import org.wcs.smart.event.i2.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.IEditValidator;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileEntityType;
import org.wcs.smart.i2.model.IntelProfileRecordSource;
import org.wcs.smart.i2.model.IntelRecordSource;

/**
 * Edit manager to validate edits to Profiles, RecordSources, or EntityTypes
 * do not invalidate event configurations.
 * 
 * @author Emily
 *
 */
public class ProfileEditManager implements IEditValidator {

	@Override
	public String isValid(Object object, Session session) {
		if (object instanceof IntelProfile) return isValid((IntelProfile)object, session);
		if (object instanceof IntelRecordSource) return isValid((IntelRecordSource)object, session);
		if (object instanceof IntelEntityType) return isValid((IntelEntityType)object, session);
		return null;
	}
	
	private String isValid(IntelProfile profile, Session session) {
		
		List<EAction> actions = QueryFactory.buildQuery(session, EAction.class, 
				new Object[] {"conservationArea", profile.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"actionTypeKey", CreateRecordActionType.KEY}).list(); //$NON-NLS-1$
		
		for(EAction a : actions) {
			EActionParameterValue value = a.findParameter(ProfileParameter.INSTANCE.getKey());
			if (value == null) continue;
			if (!value.getParameterValue().equals(profile.getKeyId())) continue;
			
			EActionParameterValue src = a.findParameter(SourceParameter.INSTANCE.getKey());
			if (src == null) continue;
			String recordSourceKey = src.getParameterValue();
			
			//ensure source is associated with profile
			boolean found = false;
			for (IntelProfileRecordSource s : profile.getRecordSources()) {
				if (s.getRecordSource().getKeyId().equalsIgnoreCase(recordSourceKey)) {
					found = true;
					break;
				}
			}
			if (!found) return MessageFormat.format(Messages.ProfileEditManager_RecordProfileExists, recordSourceKey, profile.getKeyId());			

		}
		
		
		actions = QueryFactory.buildQuery(session, EAction.class, 
				new Object[] {"conservationArea", profile.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"actionTypeKey", CreateEntityActionType.KEY}).list(); //$NON-NLS-1$
		
		for(EAction a : actions) {
			EActionParameterValue value = a.findParameter(ProfileParameter.INSTANCE.getKey());
			if (value == null) continue;
			if (!value.getParameterValue().equals(profile.getKeyId())) continue;
			
			EActionParameterValue type = a.findParameter(EntityTypeParameter.INSTANCE.getKey());
			if (type == null) continue;
			String entityTypeKey = type.getParameterValue();
			
			//ensure source is associated with profile
			boolean found = false;
			for (IntelProfileEntityType s : profile.getEntityTypes()) {
				if (s.getEntityType().getKeyId().equalsIgnoreCase(entityTypeKey)) {
					found = true;
					break;
				}
			}
			if (!found) return MessageFormat.format(Messages.ProfileEditManager_EntityTypeProfileExits, entityTypeKey, profile.getKeyId());			

		}
		
		return null;
		
	}
	
	
	private String isValid(IntelRecordSource source, Session session) {
		
		List<EAction> actions = QueryFactory.buildQuery(session, EAction.class, 
				new Object[] {"conservationArea", source.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"actionTypeKey", CreateRecordActionType.KEY}).list(); //$NON-NLS-1$
		
		for(EAction a : actions) {
			EActionParameterValue value = a.findParameter(SourceParameter.INSTANCE.getKey());
			if (value == null) continue;
			if (!value.getParameterValue().equals(source.getKeyId())) continue;
			
			EActionParameterValue profile = a.findParameter(ProfileParameter.INSTANCE.getKey());
			if (profile == null) continue;
			String profileKey = profile.getParameterValue();
			
			//ensure source is associated with profile
			boolean found = false;
			for (IntelProfileRecordSource s : source.getProfiles()) {
				if (s.getProfile().getKeyId().equalsIgnoreCase(profileKey)) {
					found = true;
					break;
				}
			}
			if (!found) return MessageFormat.format(Messages.ProfileEditManager_RecordSourceProfileExists, source.getName(), profileKey);			

		}
		return null;
	}
	
	private String isValid(IntelEntityType entityType, Session session) {
		
		List<EAction> actions = QueryFactory.buildQuery(session, EAction.class, 
				new Object[] {"conservationArea", entityType.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"actionTypeKey", CreateEntityActionType.KEY}).list(); //$NON-NLS-1$
		
		for(EAction a : actions) {
			EActionParameterValue value = a.findParameter(EntityTypeParameter.INSTANCE.getKey());
			if (value == null) continue;
			if (!value.getParameterValue().equals(entityType.getKeyId())) continue;
			
			EActionParameterValue profile = a.findParameter(ProfileParameter.INSTANCE.getKey());
			if (profile == null) continue;
			String profileKey = profile.getParameterValue();
			
			//ensure source is associated with profile
			boolean found = false;
			for (IntelProfileEntityType s : entityType.getProfiles()) {
				if (s.getProfile().getKeyId().equalsIgnoreCase(profileKey)) {
					found = true;
					break;
				}
			}
			if (!found) return MessageFormat.format(Messages.ProfileEditManager_EnityTypeProfileExists2, entityType.getName(), profileKey);			

			//also need to make sure all attributes are valid
			EActionParameterValue mappingValue = a.findParameter(MappingParameter.INSTANCE.getKey());
			if (mappingValue == null) continue;
			
			List<EntityMapping> mappings = EntityMapping.parse(mappingValue.getParameterValue(), session, entityType.getConservationArea());
			for (EntityMapping m : mappings) {
				if (m.getEntityAttribute() == null) continue;
				
				found = false;
				for (IntelEntityTypeAttribute att : entityType.getAttributes()) {
					if (att.getAttribute().getKeyId().equalsIgnoreCase(m.getEntityAttribute().getKeyId())) {
						found = true;
						break;
					}
				}
				if (!found) return MessageFormat.format(Messages.ProfileEditManager_AttributeConfigurationError,  entityType.getName(), m.getEntityAttribute().getName());
			}
		}
		return null;
	}
}
