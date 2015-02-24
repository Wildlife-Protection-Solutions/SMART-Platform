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
package org.wcs.smart.er.ui.mision.udig;

import org.locationtech.udig.catalog.IServiceInfo;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;

/**
 * Mission service info
 * @author Emily
 * @author elitvin
 *
 */
public class MissionServiceInfo extends IServiceInfo{

	public MissionServiceInfo(MissionService service){
		this.description = Messages.MissionServiceInfo_Description;
		this.icon = EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.MISSION_ICON);
		this.keywords = new String[]{Messages.MissionServiceInfo_Keyword1, Messages.MissionServiceInfo_Keyword2, Messages.MissionServiceInfo_Keyword3, Messages.MissionServiceInfo_Keyword4};
		this.title = service.getMissionRecord().getId();
	}
	
}
