/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
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

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == SamplingUnit.GeometryType.PLOT) return Messages.getString("ErLabelProvider.Point", l); //$NON-NLS-1$
		if (item == SamplingUnit.GeometryType.TRANSECT) return Messages.getString("ErLabelProvider.Line", l);  //$NON-NLS-1$
		if (item == SamplingUnit.State.ACTIVE) return Messages.getString("ErLabelProvider.Active", l); //$NON-NLS-1$
		if (item == SamplingUnit.State.INACTIVE) return Messages.getString("ErLabelProvider.InActive", l); //$NON-NLS-1$
		if (item == SurveyDesign.State.ACTIVE) return Messages.getString("ErLabelProvider.SDActive", l); //$NON-NLS-1$ 
		if (item == SurveyDesign.State.INACTIVE) return Messages.getString("ErLabelProvider.SDInactive", l); //$NON-NLS-1$
		if (item == MissionTrack.TrackType.TRACK) return Messages.getString("ErLabelProvider.MissionTrackUnassociated", l); //$NON-NLS-1$
		if (item == MissionTrack.TrackType.SAMPLING_UNIT) return Messages.getString("ErLabelProvider.MissionTrackSU", l); //$NON-NLS-1$
		if (item instanceof SurveyWaypointSource) return  Messages.getString("ErLabelProvider.MissionTrackSurvey", l); //$NON-NLS-1$
		if (item.equals(ID_COLUMN_KEY)) return Messages.getString("ErLabelProvider.IDColumnName", l); //$NON-NLS-1$
		if (item.equals(LENGTH_COLUMN_KEY)) return Messages.getString("ErLabelProvider.LengthColumName", l); //$NON-NLS-1$
		if (item.equals(STATE_COLUMN_KEY)) return Messages.getString("ErLabelProvider.StatusColumnName", l); //$NON-NLS-1$
		if (item.equals(SU_TABLE_LONGNAME_KEY)) return Messages.getString("ErLabelProvider.SuTableLogName", l); //$NON-NLS-1$
		if (item.equals(SD_TABLE_LONGNAME_KEY)) return Messages.getString("ErLabelProvider.SuveyDesignTableName", l); //$NON-NLS-1$
		if (item.equals(SD_DESCRIPTION_COL_KEY)) return Messages.getString("ErLabelProvider.DescriptionColumn", l); //$NON-NLS-1$
		if (item.equals(SD_ENDDATE_COL_KEY)) return Messages.getString("ErLabelProvider.EndDateColumn", l); //$NON-NLS-1$
		if (item.equals(SD_STARTDATE_COL_KEY)) return Messages.getString("ErLabelProvider.StartDateColumn", l); //$NON-NLS-1$
		if (item.equals(SD_STATUS_COL_KEY)) return Messages.getString("ErLabelProvider.StatusColumn", l); //$NON-NLS-1$
		if (item.equals(SD_KEY_COL_KEY)) return Messages.getString("ErLabelProvider.KeyColumn", l); //$NON-NLS-1$
		if (item.equals(SD_NAME_COL_KEY)) return Messages.getString("ErLabelProvider.NameColumn", l); //$NON-NLS-1$
		return null;
	}
}
