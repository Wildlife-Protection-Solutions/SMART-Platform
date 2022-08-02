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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.icon.FixedIconSet;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;

/**
 * Patrol metadata fields
 * 
 * @author Emily
 *
 */
public enum PatrolMetadataField {
	
	TRANSPORT("SMART_PatrolTransport", true, false, PatrolTransportType.LIBRARY_ICON_KEY), //$NON-NLS-1$
	ARMED("SMART_Armed", true, true, "patrol_is_armed"), //$NON-NLS-1$ //$NON-NLS-2$
	STATION("SMART_Station", false, true, Station.LIBRARY_ICON_KEY), //$NON-NLS-1$
	TEAM("SMART_Team", false, true, Team.LIBRARY_ICON_KEY), //$NON-NLS-1$
	MANDATE("SMART_Mandate", true, false, PatrolMandate.LIBRARY_ICON_KEY), //$NON-NLS-1$
	OBJECTIVE("SMART_Objective", false, true, "patrol_objective"), //$NON-NLS-1$ //$NON-NLS-2$
	COMMENT("SMART_Comments", false, true, "patrol_comment"), //$NON-NLS-1$ //$NON-NLS-2$
	MEMBERS("SMART_Members", true, false, "patrol_members"), //$NON-NLS-1$ //$NON-NLS-2$
	LEADER("SMART_Leader", true, false, "patrol_leader"), //$NON-NLS-1$ //$NON-NLS-2$
	PILOT("SMART_Pilot", false, false, "patrol_pilot"); //$NON-NLS-1$ //$NON-NLS-2$
	
	public static final String PATROL_RESOURCE_ID = "patrol"; //$NON-NLS-1$
	public static final String PATROL_ICONSET_KEY = "patrol_metadata_iconset"; //$NON-NLS-1$
	
	private String jsonKey;
	private boolean isRequired;
	private boolean isFixed;
	
	private String libraryIcon;
	
	PatrolMetadataField(String jsonKey, boolean isRequired, boolean isFixed, String libraryIcon){
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
			Logger.getLogger(PatrolMetadataField.class.getName()).log(Level.WARNING, e.getMessage(), e); 
		}
		return null;
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
	
	/**
	 * Generates the metadata field keyid for a custom patrol attribute
	 * 
	 * @param attribute
	 * @return
	 */
	public static String generateKey(PatrolAttribute attribute) {
		return "custom_" + attribute.getKeyId(); //$NON-NLS-1$
	}
}
