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
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolWaypointSource;

/**
 * Implementation for patrol label provider.
 * 
 * @author Emily
 *
 */
public class PatrolLabelProvider implements IPatrolLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof PatrolType){
			return getLabel(((PatrolType)item).getType(), l);
		}else if (item instanceof PatrolType.Type){
			switch((PatrolType.Type)item){
				case AIR: return Messages.getString("PatrolLabelProvider.AirPatrol", l); //$NON-NLS-1$
				case GROUND: return Messages.getString("PatrolLabelProvider.GroundPatrol", l); //$NON-NLS-1$
				case MARINE: return Messages.getString("PatrolLabelProvider.WaterPatrol", l); //$NON-NLS-1$
			}
		}else if (item instanceof PatrolWaypointSource){
			return Messages.getString("PatrolLabelProvider.WpSource", l); //$NON-NLS-1$
		}
		if (item.equals(PATROLTEAM_TABLENAME_KEY)) return Messages.getString("PatrolLabelProvider.TeamTableName", l); //$NON-NLS-1$
		if (item.equals(PATROLTT_TABLENAME_KEY)) return Messages.getString("PatrolLabelProvider.TransportTypeTableName", l); //$NON-NLS-1$
		if (item.equals(PATROLMANDATE_TABLENAME_KEY)) return Messages.getString("PatrolLabelProvider.MandateTableName", l); //$NON-NLS-1$
		if (item.equals(TEAMNAME_KEY)) return Messages.getString("PatrolLabelProvider.TeamNameColumn", l); //$NON-NLS-1$
		if (item.equals(TEAMDESCRIPTION_KEY)) return Messages.getString("PatrolLabelProvider.TeamDescriptionColumn", l); //$NON-NLS-1$
		if (item.equals(TEAMMANDATE_KEY)) return Messages.getString("PatrolLabelProvider.TeamMandateColumn", l); //$NON-NLS-1$
		if (item.equals(TEAMACTIVE_KEY)) return Messages.getString("PatrolLabelProvider.TeamActiveColumn", l); //$NON-NLS-1$
		if (item.equals(TRANSPORTNAME_KEY)) return Messages.getString("PatrolLabelProvider.TransportTypeColumn", l); //$NON-NLS-1$
		if (item.equals(TRANSPORTACTIVE_KEY)) return Messages.getString("PatrolLabelProvider.TransportActiveColumn", l); //$NON-NLS-1$
		if (item.equals(TRANSPORTTYPE_KEY)) return Messages.getString("PatrolLabelProvider.TransportPatrolColumn", l); //$NON-NLS-1$
		if (item.equals(MANDATENAME_KEY)) return Messages.getString("PatrolLabelProvider.MandateColumn", l); //$NON-NLS-1$
		if (item.equals(MANDATEACTIVE_KEY)) return Messages.getString("PatrolLabelProvider.MandateActiveColumn", l); //$NON-NLS-1$
		return null;
	}

}
