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
package org.wcs.smart.cybertracker.survey.model;

import java.net.URI;
import java.net.URISyntaxException;

import org.wcs.smart.ca.icon.IconManager;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.er.model.MissionAttribute;

/**
 * Survey Metadata fields
 * @author Emily
 *
 */
public enum MissionMetadataField {
	
	COMMENT("SMART_Comments", false, "patrol_comment"), //$NON-NLS-1$ //$NON-NLS-2$
	MEMBERS("SMART_Members", true, "patrol_members"), //$NON-NLS-1$ //$NON-NLS-2$
	LEADER("SMART_Leader", true, "patrol_leader"), //$NON-NLS-1$ //$NON-NLS-2$
	SAMPING_UNIT("SMART_SamplingUnit", true, null); //$NON-NLS-1$
	
	public static final String MISSION_ICONSET_KEY = "mission_metadata_iconset"; //$NON-NLS-1$
	
	private String jsonKey;
	private boolean isRequired;
	private String libraryIcon;
	
	MissionMetadataField(String jsonKey, boolean isRequired, String libraryIcon){
		this.jsonKey = jsonKey;
		this.isRequired = isRequired;
		this.libraryIcon = libraryIcon;
	}
	
	public String getJsonKey() {
		return this.jsonKey;
	}
	
	public boolean isRequired() {
		return this.isRequired;
	}
	
	/**
	 * The prefix for the key for custom mission attributes
	 * 
	 * @return
	 */
	public static String getCustomAttributePrefix() {
		return "custom_"; //$NON-NLS-1$
	}
	/**
	 * Generates the metadata field keyid for a custom mission attribute
	 * 
	 * @param attribute
	 * @return
	 */
	public static String generateKey(MissionAttribute attribute) {
		return getCustomAttributePrefix() + attribute.getKeyId(); 
	}
	
	public URI getIcon(IconSet set) {
		if (this.libraryIcon == null) return null;
		String filename = IconManager.INSTANCE.getLibraryFile(this.libraryIcon, set);
		if (filename == null) {
			IconSet temp = new IconSet();
			temp.setKeyId(IconManager.FixedIconSet.COLOR.key);
			filename = IconManager.INSTANCE.getLibraryFile(this.libraryIcon, temp);
		}
		//this shouldn't happen
		if (filename == null) return null;
		
		try {
			return new URI(filename);
		} catch (URISyntaxException e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
		}
		return null;
	}
}
