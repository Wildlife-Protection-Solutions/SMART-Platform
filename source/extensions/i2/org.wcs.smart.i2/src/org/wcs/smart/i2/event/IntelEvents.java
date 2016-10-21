package org.wcs.smart.i2.event;


public class IntelEvents {

	public static final String ENTITY_ALL = "INTEL_ENTITY/*";
	public static final String ENTITY_NEW = "INTEL_ENTITY/NEW";
	public static final String ENTITY_DELETE = "INTEL_ENTITY/DELETE";
	public static final String ENTITY_MODIFIED = "INTEL_ENTITY/UPDATED";
	

	public static final String ENTITY_TYPE_ALL = "INTEL_ENTITY_TYPE/*";
	public static final String ENTITY_TYPE_NEW = "INTEL_ENTITY_TYPE/NEW";
	public static final String ENTITY_TYPE_DELETE = "INTEL_ENTITY_TYPE/DELETE";
	public static final String ENTITY_TYPE_MODIFIED = "INTEL_ENTITY_TYPE/UPDATED";
	
	//identifies when the entity type template needs refreshing
	//only needs refreshing when the entity attributes are modified or
	//relationship attributes are modified
	/**
	 * payload may be a single entity type
	 * or a list of entity types
	 */
	public static final String ENTITY_TYPE_TEMPLATE_REFRESH = "INTEL_ENTITY_TYPE_TEMPLATE/REFRESH";
	
	public static final String RELATION_TYPE_ALL = "INTEL_RELATION_TYPE/*";
	public static final String RELATION_TYPE_NEW = "INTEL_RELATION_TYPE/NEW";
	public static final String RELATION_TYPE_DELETE = "INTEL_RELATION_TYPE/DELETE";
	public static final String RELATION_TYPE_MODIFIED = "INTEL_RELATION_TYPE/UPDATED";
	
	public static final String RECORD_ALL = "INTEL_RECORD/*";
	public static final String RECORD_NEW = "INTEL_RECORD/NEW";
	public static final String RECORD_DELETE = "INTEL_RECORD/DELETE";
	public static final String RECORD_MODIFIED = "INTEL_RECORD/UPDATED";
	
	public static final String WS_ALL = "INTEL_WS/*";
	public static final String WS_NEW = "INTEL_WS/NEW";
	public static final String WS_DELETE = "INTEL_WS/DELETE";
	public static final String WS_MODIFIED = "INTEL_WS/UPDATED";
	
	public static final String ACTIVE_WS_SET = "INTEL_WS_SET";
	
	//payload needs to be an instance of LayerVisibleEvent
	public static final String ACTIVE_WS_LAYER_VISIBILITY = "INTEL_WS_LAYER_VISIBILITY";
	
	//playload needs to be a array of two dates [startdate, enddate]
	public static final String ACTIVE_WS_LAYER_DATEFILTER = "INTEL_WS_LAYER_DATEFILTER";
	
}
