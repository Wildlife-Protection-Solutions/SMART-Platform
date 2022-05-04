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
package org.wcs.smart.upgrade;

import java.util.Map;

import org.wcs.smart.SmartPlugIn;

/**
 * Abstract class for internal core updates. The isUpdateToDate function isn't used
 * so it always returns false;
 * 
 * @author elitvin
 * @since 3.0.0
 */
public abstract class AbstractInteralDatabaseUpgrader implements IDatabaseUpgrader{

	/**
	 * Returns true if the database is update to date; otherwise false
	 * if upgrade is required
	 *  
	 * @param currentVersion a map from the plugin id to the current version in the database
	 * @return
	 */
	@Override
	public boolean isUpdateToDate(Map<String,String> currentVersions) {
		return false;
	}
	
	@Override
	public String getPluginId() {
		return SmartPlugIn.PLUGIN_ID;
	}
	
	@Override
	public String getPluginName() {
		return getName(SmartPlugIn.getDefault().getBundle());
	}
	
}
