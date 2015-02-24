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
package org.wcs.smart.patrol.udig.catalog;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.locationtech.udig.catalog.IServiceInfo;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Smart service information.
 * @author Emily
 * @since 1.0.0
 */
public class SmartServiceInfo extends IServiceInfo{

	public SmartServiceInfo(PatrolService service){
		this.description = Messages.SmartServiceInfo_PatrolService_Description;
		this.icon = AbstractUIPlugin.imageDescriptorFromPlugin(SmartPlugIn.PLUGIN_ID,"images/icons/smart16.gif"); //$NON-NLS-1$
		this.keywords = new String[]{Messages.SmartServiceInfo_PatrolService_Keyword6, Messages.SmartServiceInfo_PatrolService_Keyword1, Messages.SmartServiceInfo_PatrolService_Keyword2, Messages.SmartServiceInfo_PatrolService_Keyword3, Messages.SmartServiceInfo_PatrolService_Keyword4};
		this.title = Messages.SmartServiceInfo_PatrolService_Keyword5 + service.getPatrolID();
	}
	
}
