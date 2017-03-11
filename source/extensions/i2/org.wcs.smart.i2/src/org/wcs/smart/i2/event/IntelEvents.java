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
	public static final String ENTITY_ALL = "INTEL_ENTITY/*"; //$NON-NLS-1$
	public static final String ENTITY_NEW = "INTEL_ENTITY/NEW"; //$NON-NLS-1$
	public static final String ENTITY_DELETE = "INTEL_ENTITY/DELETE"; //$NON-NLS-1$
	public static final String ENTITY_MODIFIED = "INTEL_ENTITY/UPDATED"; //$NON-NLS-1$
	

	public static final String ENTITY_TYPE_ALL = "INTEL_ENTITY_TYPE/*"; //$NON-NLS-1$
	public static final String ENTITY_TYPE_NEW = "INTEL_ENTITY_TYPE/NEW"; //$NON-NLS-1$
	public static final String ENTITY_TYPE_DELETE = "INTEL_ENTITY_TYPE/DELETE"; //$NON-NLS-1$
	public static final String ENTITY_TYPE_MODIFIED = "INTEL_ENTITY_TYPE/UPDATED"; //$NON-NLS-1$
	
	//identifies when the entity type template needs refreshing
	//only needs refreshing when the entity attributes are modified or
	//relationship attributes are modified
	/**
	 * payload may be a single entity type
	 * or a list of entity types
	 */
	public static final String ENTITY_TYPE_TEMPLATE_REFRESH = "INTEL_ENTITY_TYPE_TEMPLATE/REFRESH"; //$NON-NLS-1$
	
	public static final String RELATION_TYPE_ALL = "INTEL_RELATION_TYPE/*"; //$NON-NLS-1$
	public static final String RELATION_TYPE_NEW = "INTEL_RELATION_TYPE/NEW"; //$NON-NLS-1$
	public static final String RELATION_TYPE_DELETE = "INTEL_RELATION_TYPE/DELETE"; //$NON-NLS-1$
	public static final String RELATION_TYPE_MODIFIED = "INTEL_RELATION_TYPE/UPDATED"; //$NON-NLS-1$
	
	public static final String RECORD_ALL = "INTEL_RECORD/*"; //$NON-NLS-1$
	//payload can be single record or collection or records
	public static final String RECORD_NEW = "INTEL_RECORD/NEW"; //$NON-NLS-1$
	public static final String RECORD_SAVED = "INTEL_RECORD/SAVED"; //$NON-NLS-1$
	//payload can be single record or collection or records
	public static final String RECORD_DELETE = "INTEL_RECORD/DELETE"; //$NON-NLS-1$
	public static final String RECORD_MODIFIED = "INTEL_RECORD/UPDATED"; //$NON-NLS-1$
	
	public static final String WS_ALL = "INTEL_WS/*"; //$NON-NLS-1$
	public static final String WS_NEW = "INTEL_WS/NEW"; //$NON-NLS-1$
	public static final String WS_DELETE = "INTEL_WS/DELETE"; //$NON-NLS-1$
	public static final String WS_MODIFIED = "INTEL_WS/UPDATED"; //$NON-NLS-1$
	
	
	//payload can be a single query or a list of queries
	public static final String QUERY_ALL = "INTEL_QUERY/*"; //$NON-NLS-1$
	public static final String QUERY_NEW = "INTEL_QUERY/NEW"; //$NON-NLS-1$
	public static final String QUERY_MODIFIED = "INTEL_QUERY/UPDATED"; //$NON-NLS-1$
	public static final String QUERY_DELETED = "INTEL_QUERY/DELETED"; //$NON-NLS-1$
	
	public static final String ACTIVE_WS_SET = "INTEL_WS_SET"; //$NON-NLS-1$
	
	//payload needs to be an instance of LayerVisibleEvent
	public static final String ACTIVE_WS_LAYER_VISIBILITY = "INTEL_WS_LAYER_VISIBILITY"; //$NON-NLS-1$
	
	//payload needs to be a array of two dates [startdate, enddate]
	public static final String ACTIVE_WS_LAYER_DATEFILTER = "INTEL_WS_LAYER_DATEFILTER"; //$NON-NLS-1$
	
	//payload will be a single intelentitysearch
	public static final String ENTITY_SEARCH_ALL = "INTEL_ENTITY_SEARCH/*"; //$NON-NLS-1$
	public static final String ENTITY_SEARCH_NEW = "INTEL_ENTITY_SEARCH/NEW"; //$NON-NLS-1$
	public static final String ENTITY_SEARCH_MODIFIED = "INTEL_ENTITY_SEARCH/UPDATED"; //$NON-NLS-1$
	public static final String ENTITY_SEARCH_DELETED = "INTEL_ENTITY_SEARCH/DELETED"; //$NON-NLS-1$

	//record sources modified; no payload
	public static final String RECORD_SOURCE_ALL = "INTEL_RECORD_SOURCE"; //$NON-NLS-1$
	
	
	//event to fire with list of attachments of items to
	//search
	public static final String ATTACHMENT_SEARCH = "INTEL_ATTACHMENT_SEARCH"; //$NON-NLS-1$
}
