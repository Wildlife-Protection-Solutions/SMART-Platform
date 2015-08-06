package org.wcs.smart.entity.ui;

import java.util.Locale;

import org.wcs.smart.entity.IEntityLabelProvider;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;

public class EntityLabelProvider implements IEntityLabelProvider{

	public static final String ID_FIELD_NAME = Messages.Entity_IDFieldName;
	public static final String STATUS_FIELD_NAME = Messages.Entity_StatusFieldName;
	public static final String X_FIELD_NAME = Messages.Entity_XFieldName;
	public static final String Y_FIELD_NAME = Messages.Entity_YFieldName;
	public static final String CA_FIELD_NAME = Messages.Entity_CaIdFieldName;
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item == Status.ACTIVE) return Messages.Entity_ActiveStatusLabel;
		if (item == Status.INACTIVE) return Messages.Entity_InActiveStatusLabel;
		if (item == EntityType.Type.FIXED) return Messages.EntityType_FixedTypeLabel;
		if (item == EntityType.Type.TRANSIENT) return Messages.EntityType_TransientTypeLabel;

		return null;
	}

}
