/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.incident.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wcs.smart.ca.icon.FixedIconSet;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.ca.icon.IconUtils;

/**
 * Incident metadata fields for SMART Mobile
 * 
 * @author Emily
 *
 */
public enum IncidentMetadataField {
	
	MEMBERS("SMART_Employee", true, false, "patrol_members"), //$NON-NLS-1$ //$NON-NLS-2$
	TYPES("SMART_IncidentType", true, false, null); //$NON-NLS-1$
	
	private String jsonKey;
	private boolean isRequired;
	private boolean isFixed;
	private String libraryIcon;
	
	IncidentMetadataField(String jsonKey, boolean isRequired, boolean isFixed, String libraryIcon){
		this.jsonKey = jsonKey;
		this.isRequired = isRequired;
		this.isFixed = isFixed;
		this.libraryIcon = libraryIcon;
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
	
	public URI getIcon(IconSet set) {
		if (this.libraryIcon == null) return null;
		String filename = IconUtils.INSTANCE.getLibraryFile(this.libraryIcon, set);
		if (filename == null) {
			IconSet temp = new IconSet();
			temp.setKeyId(FixedIconSet.COLOR.key);
			filename = IconUtils.INSTANCE.getLibraryFile(this.libraryIcon, temp);
		}
		//this shouldn't happen
		if (filename == null) return null;
		
		try {
			return new URI(filename);
		} catch (URISyntaxException e) {
			Logger.getLogger(IncidentMetadataField.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
		return null;
	}
}
