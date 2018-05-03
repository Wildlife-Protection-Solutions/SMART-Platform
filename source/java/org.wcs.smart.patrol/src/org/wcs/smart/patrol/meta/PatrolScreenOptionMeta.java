/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.patrol.meta;


/**
 * Meta options for patrol screens
 * @author elitvin
 */
public enum PatrolScreenOptionMeta {

	TRANSPORT("SMART_PatrolTransport", true), //$NON-NLS-1$
	ARMED("SMART_Armed", true), //$NON-NLS-1$
	STATION("SMART_Station", false), //$NON-NLS-1$
	TEAM("SMART_Team", false), //$NON-NLS-1$
	MANDATE("SMART_Mandate", true), //$NON-NLS-1$
	OBJECTIVE("SMART_Objective", false), //$NON-NLS-1$
	COMMENT("SMART_Comments", false), //$NON-NLS-1$
	MEMBERS("SMART_Members", true), //$NON-NLS-1$
	LEADER("SMART_Leader", true), //$NON-NLS-1$
	PILOT("SMART_Pilot", false); //$NON-NLS-1$
	
	public static final String PATROL_RESOURCE_ID = "patrol"; //$NON-NLS-1$
	
	public String key;
	private boolean required;
	
	PatrolScreenOptionMeta(String key, boolean required) {
		this.key = key;
		this.required = required;
	}
	
	public boolean isRequired() {
		return this.required;
	}
}
