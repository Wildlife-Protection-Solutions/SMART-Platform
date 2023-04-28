/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.model;


/**
 * Patrol metadata fields
 * 
 * @author Emily
 *
 */
public enum PatrolMetadataField {
	
	TRANSPORT("SMART_PatrolTransport", true, false), //$NON-NLS-1$
	ARMED("SMART_Armed", true, true), //$NON-NLS-1$
	STATION("SMART_Station", false, true), //$NON-NLS-1$
	TEAM("SMART_Team", false, true), //$NON-NLS-1$
	MANDATE("SMART_Mandate", true, false), //$NON-NLS-1$
	OBJECTIVE("SMART_Objective", false, true), //$NON-NLS-1$
	COMMENT("SMART_Comments", false, true), //$NON-NLS-1$
	MEMBERS("SMART_Members", true, false), //$NON-NLS-1$
	LEADER("SMART_Leader", true, false), //$NON-NLS-1$
	PILOT("SMART_Pilot", false, false), //$NON-NLS-1$
	CM_ID("SMART_cmUuid", true, true);  //$NON-NLS-1$ //JsonCtParser.CM_UUID_KEY
	
	public static final String PATROL_RESOURCE_ID = "patrol"; //$NON-NLS-1$
	
	private String jsonKey;
	private boolean isRequired;
	private boolean isFixed;
	
	PatrolMetadataField(String jsonKey, boolean isRequired, boolean isFixed){
		this.jsonKey = jsonKey;
		this.isRequired = isRequired;
		this.isFixed = isFixed;
	}
	
	public String getJsonKey() {
		return this.jsonKey;
	}
	
	public boolean isRequired() {
		return this.isRequired;
	}
	
	/**
	 * If true then the item is an attribute of the patrol
	 * and cannot be changed.  If false the item is an attribute
	 * of a patrol leg and can be modified by the user during
	 * the patrol
	 * 
	 * @return
	 */
	public boolean isFixed() {
		return this.isFixed;
	}
}
