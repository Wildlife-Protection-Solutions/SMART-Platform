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
package org.wcs.smart.er.ui;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypointSource;

/**
 * Label provider for survey elements.
 * 
 * @author Emily
 *
 */
public class ErLabelProvider implements IErLabelProvider{

	public static final String ID_COL_NAME = Messages.SamplingUnitEditorPage_IdColumnName;
	public static final String LENGTH_COL_NAME = Messages.SamplingUnitEditorPage_LengthColumnName;
	public static final String STATE_COL_NAME = Messages.SamplingUnitEditorPage_StateColumnName;
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item == SamplingUnit.GeometryType.PLOT){
			return Messages.SamplingUnit_PointGeomType;
		}
		if (item == SamplingUnit.GeometryType.TRANSECT){
			return Messages.SamplingUnit_LinearGeomType; 
		} 
		if (item == SamplingUnit.State.ACTIVE){
			return Messages.SamplingUnit_ActiveState;
		}
		if (item == SamplingUnit.State.INACTIVE){
			return Messages.SamplingUnit_InActiveState;
		}
		if (item == SurveyDesign.State.ACTIVE){
			return Messages.SurveyDesign_ActiveStateLabel;
		}
		if (item == SurveyDesign.State.INACTIVE){
			return Messages.SurveyDesign_InActiveStateLabel;
		}
		if (item == MissionTrack.TrackType.TRACK){
			return Messages.MissionTrack_TrackGuiName;
		}
		if (item == MissionTrack.TrackType.SAMPLING_UNIT){
			return Messages.MissionTrack_SuTrackGuiName;
		}
		if (item instanceof SurveyWaypointSource){
			return  Messages.SurveyWaypointSource_Name;
		}
		
		if (item.equals(ID_COLUMN_KEY)){
			return ID_COL_NAME;
		}
		if (item.equals(LENGTH_COLUMN_KEY)){
			return LENGTH_COL_NAME;
		}
		if (item.equals(STATE_COLUMN_KEY)){
			return STATE_COL_NAME;
		}
		if (item.equals(SU_TABLE_LONGNAME_KEY)){
			return Messages.ErLabelProvider_su_table_name;
		}
		if (item.equals(SD_TABLE_LONGNAME_KEY)){
			return Messages.ErLabelProvider_sd_table_name;
		}
		
		if (item.equals(SD_DESCRIPTION_COL_KEY)) return Messages.ErLabelProvider_DescriptionColumn;
		if (item.equals(SD_ENDDATE_COL_KEY)) return Messages.ErLabelProvider_EndDateColumn;
		if (item.equals(SD_STARTDATE_COL_KEY)) return Messages.ErLabelProvider_StartDateColumn;
		if (item.equals(SD_STATUS_COL_KEY)) return Messages.ErLabelProvider_StatusColumn;
		if (item.equals(SD_KEY_COL_KEY)) return Messages.ErLabelProvider_KeyColumn;
		if (item.equals(SD_NAME_COL_KEY)) return Messages.ErLabelProvider_NameColumn;
		return null;
	}

	public static Image getImage(Object item){
		if (item == SamplingUnit.GeometryType.PLOT){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_TRANSECT_ICON);
		}
		if (item == SamplingUnit.GeometryType.TRANSECT){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_PLOT_ICON); 
		}
		return null;
	}
}
