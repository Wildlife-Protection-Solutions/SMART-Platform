package org.wcs.smart.event.i2.entity;

import org.wcs.smart.event.i2.CreateEntityActionTypeExecutor;
import org.wcs.smart.event.i2.CreateRecordActionTypeExecutor;

public class ParameterKeys {

	public static final String MAPPING_PARAM_KEY = CreateEntityActionTypeExecutor.KEY + ".mapping"; //$NON-NLS-1$
	
	public static final String ENTITYTYPE_PARAM_KEY = CreateEntityActionTypeExecutor.KEY + ".entitytype"; //$NON-NLS-1$

	public static final String PROFILE_PARAM_KEY = "org.wcs.smart.profile.common.profile"; //$NON-NLS-1$
	
	public static final String RECORD_SOURCE_PARAM_KEY = CreateRecordActionTypeExecutor.KEY + ".source"; //$NON-NLS-1$
	
	public static final String RECORD_TITLE_PARAM_KEY =  CreateRecordActionTypeExecutor.KEY + ".title"; //$NON-NLS-1$



}
