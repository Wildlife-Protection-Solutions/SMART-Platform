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
package org.wcs.smart.cybertracker.properties;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption.ProfileOptionID;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Factory for creating CyberTracker properties profile.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CyberTrackerProfileFactory {

	/**
	 * Creates a CyberTracker properties profile with all parameters set to defaults.
	 */
	public static CyberTrackerPropertiesProfile createUsingDefaults(String name) {
		CyberTrackerPropertiesProfile profile = new CyberTrackerPropertiesProfile();
		profile.setConservationArea(SmartDB.getCurrentConservationArea());
		profile.setName(name);
		profile.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		return profile;
	}
	
	/**
	 * Creates a CyberTracker properties profile using another configurable model as a template.
	 */
	public static CyberTrackerPropertiesProfile createProfileClone(CyberTrackerPropertiesProfile profile, String name, IProgressMonitor monitor) {
		monitor.beginTask(Messages.CyberTrackerProfileFactory_CloneProfile_TaskName, 1);
		
		monitor.subTask(Messages.CyberTrackerProfileFactory_CloneProfile_TaskName);
		CyberTrackerPropertiesProfile clone = createUsingDefaults(name);
		
		//NOTE: we are not coping isDefault and names
		Map<ProfileOptionID, CyberTrackerPropertiesProfileOption> options = profile.getOptions();
		for (ProfileOptionID id : options.keySet()) {
			CyberTrackerPropertiesProfileOption option = options.get(id);
			
			CyberTrackerPropertiesProfileOption newOption = new CyberTrackerPropertiesProfileOption();
			newOption.setProfile(clone);
			newOption.setOptionId(option.getOptionId());
			newOption.setDoubleValue(option.getDoubleValue());
			newOption.setIntegerValue(option.getIntegerValue());
			newOption.setStringValue(option.getStringValue());
			
			clone.getOptions().put(id, newOption);
		}

		if (monitor.isCanceled()) return null;
		monitor.done();
		return clone;
	}

}
