/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.er;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.qa.er.internal.Messages;
import org.wcs.smart.qa.model.IQaDataProvider;

/**
 * Image provider for patrol data providers
 * 
 * @author Emily
 *
 */
public class ErLabelProvider extends ILabelProvider {

	@Override
	public Image getImage(Class<? extends IQaDataProvider> clazz) {
		if (clazz.equals(ErTrackDataProvider.class))
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON) ;
	
		if (clazz.equals(ErWaypointDataProvider.class))
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON) ;

		return null;
	}

	@Override
	public String getString(Key key, Locale l) {
		switch(key){
		case ErTrackDataProvider_Name:
			return Messages.ErLabelProvider_MissionTrackDataProviderName;
		case ErTrackDataProvider_TrackNotFound:
			return Messages.ErLabelProvider_TrackNotFoundError;
		case ErWaypointDataProvider_Name:
			return Messages.ErLabelProvider_MissionWpDataProviderName;
		case ErWaypointDataProvider_WpIdLbl:
			return Messages.ErLabelProvider_WaypointIdLbl;
		case ErWaypointDataProvider_WpNotFound:
			return Messages.ErLabelProvider_WaypointNotFoundError;
		default:
		
		}
		return ""; //$NON-NLS-1$
	}

}
