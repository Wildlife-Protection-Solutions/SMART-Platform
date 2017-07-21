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
package org.wcs.smart.dataentry;

import javax.persistence.Transient;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;
import org.wcs.smart.hibernate.SmartDB;

/**
 * @author elitvin
 * @since 6.0.0
 *
 */
public class CmAttributeConfigUtil {
	
	@Transient
	public static final void assignCustomName(CmAttributeConfig cfg, CmAttribute cmAttr) {
		String name = Messages.CmAttributeConfig_Custom_Prefix + " " + cmAttr.findName(SmartDB.getCurrentLanguage()); //$NON-NLS-1$
		cfg.setName(name);
		cfg.updateName(SmartDB.getCurrentLanguage(), name);
	}

	@Transient
	public static final CmAttributeConfig createConfig(ConfigurableModel model, Attribute attribute, boolean isDefault) {
		CmAttributeConfig cfg = new CmAttributeConfig();
		cfg.setModel(model);
		cfg.setAttribute(attribute);
		cfg.setDisplayMode(DisplayMode.DEFAULT_DISPLAY_MODE);
		cfg.setDefault(isDefault);
		return cfg;
	}
	
}
