/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.event;

/**
 * Intelligence system events.
 * 
 * @author Emily
 *
 */
public class IntelEvents {

	/**
	 * Payload for entity events can be a single entity or a collection
	 * of entities
	 */
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
	public static final String RECORD_SAVED = "INTEL_RECORD/SAVED";
	//payload can be single record or collection or records
	public static final String RECORD_DELETE = "INTEL_RECORD/DELETE";
	public static final String RECORD_MODIFIED = "INTEL_RECORD/UPDATED";
	
	public static final String WS_ALL = "INTEL_WS/*";
	public static final String WS_NEW = "INTEL_WS/NEW";
	public static final String WS_DELETE = "INTEL_WS/DELETE";
	public static final String WS_MODIFIED = "INTEL_WS/UPDATED";
	
	
	//payload can be a single query or a list of queries
	public static final String QUERY_ALL = "INTEL_QUERY/*";
	public static final String QUERY_NEW = "INTEL_QUERY/NEW";
	public static final String QUERY_MODIFIED = "INTEL_QUERY/UPDATED";
	public static final String QUERY_DELETED = "INTEL_QUERY/DELETED";
	
	public static final String ACTIVE_WS_SET = "INTEL_WS_SET";
	
	//payload needs to be an instance of LayerVisibleEvent
	public static final String ACTIVE_WS_LAYER_VISIBILITY = "INTEL_WS_LAYER_VISIBILITY";
	
	//payload needs to be a array of two dates [startdate, enddate]
	public static final String ACTIVE_WS_LAYER_DATEFILTER = "INTEL_WS_LAYER_DATEFILTER";
	
	//payload will be a single intelentitysearch
	public static final String ENTITY_SEARCH_ALL = "INTEL_ENTITY_SEARCH/*";
	public static final String ENTITY_SEARCH_NEW = "INTEL_ENTITY_SEARCH/NEW";
	public static final String ENTITY_SEARCH_MODIFIED = "INTEL_ENTITY_SEARCH/UPDATED";
	public static final String ENTITY_SEARCH_DELETED = "INTEL_ENTITY_SEARCH/DELETED";
	
}
