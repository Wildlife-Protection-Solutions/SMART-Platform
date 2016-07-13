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
package org.wcs.smart.cybertracker.model;

import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.OptionID;

/**
 * Class responsible for representing CyberTracker Properties related to Conservation Area
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerProperties {

	public enum Protocol{
		GEOJSON(4),
		GEOJSON_COMPRESSED(6);
//		ERSIJSON(7);
		
		public int ctValue;
		
		private Protocol(int ctValue){
			this.ctValue = ctValue;
		}
	}
	
	public static final int STORAGE_TIME_MIN_VALUE = 0;
	public static final int STORAGE_TIME_MAX_VALUE = 365;
	public static final int STORAGE_TIME_DEFAULT_VALUE = 30;

	private Map<OptionID, CyberTrackerPropertiesOption> options;
	
	public Map<OptionID, CyberTrackerPropertiesOption> getOptions() {
		if (options == null)
			options = new HashMap<OptionID, CyberTrackerPropertiesOption>();
		return options;
	}
	
	private CyberTrackerPropertiesOption getOption(OptionID optionId) {
		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
		CyberTrackerPropertiesOption option = map.get(optionId);
		if (option == null) {
			option = new CyberTrackerPropertiesOption();
			option.setOptionId(optionId);
			map.put(optionId, option);
		}
		return option;
	}

	
	private int getIntValue(OptionID optionId, int defaultValue) {
		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
		CyberTrackerPropertiesOption option = map.get(optionId);
		return (option != null) ? option.getIntegerValue() : defaultValue;
	}
	
//	private boolean getBooleanValue(OptionID optionId, boolean defaultValue) {
//		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
//		CyberTrackerPropertiesOption option = map.get(optionId);
//		return (option != null) ? option.getBooleanValue() : defaultValue;
//	}
//
//	
//	private double getDoubleValue(OptionID optionId, double defaultValue) {
//		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
//		CyberTrackerPropertiesOption option = map.get(optionId);
//		return (option != null) ? option.getDoubleValue() : defaultValue;
//	}
//	
//	private String getStringValue(OptionID optionId, String defaultValue) {
//		Map<OptionID, CyberTrackerPropertiesOption> map = getOptions();
//		CyberTrackerPropertiesOption option = map.get(optionId);
//		return (option != null) ? option.getStringValue() : defaultValue;
//	}


	public int getStorageTime() {
		return getIntValue(OptionID.STORAGE_TIME, STORAGE_TIME_DEFAULT_VALUE);
	}
	public void setStorageTime(int storageTime) {
		getOption(OptionID.STORAGE_TIME).setIntegerValue(storageTime);
	}

}
