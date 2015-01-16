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
package org.wcs.smart.intelligence.upgrade;

import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * @author elitvin
 * @since 3.0.0
 */
public enum UpgradeFromVersion {
	
	V0(IntelligenceDbUpgrader0To30.class),
	V30(IntelligenceDbUpgrader30To32.class);
	
	public Class<? extends IIntelligenceUpgrader> upgradeEngine;
	
	private UpgradeFromVersion(Class<? extends IIntelligenceUpgrader> engine){
		this.upgradeEngine = engine;
	}
	
	public IIntelligenceUpgrader createUpgradeEngine() {
		try {
			return upgradeEngine.newInstance();
		} catch (Exception e) {
			SmartPlugIn.displayLog(Messages.UpgradeFromVersion_InstanceCreateError, e);
			return null;
		}
	}
	
	public static final UpgradeFromVersion fromString(String v) {
		if (v == null || v.isEmpty() || "1.0".equals(v)) { //$NON-NLS-1$
			return V0;
		} else if ("3.0".equals(v)) { //$NON-NLS-1$
			return V30;
		}
		return null;
	}
}
