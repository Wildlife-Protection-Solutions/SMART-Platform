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
package org.wcs.smart.patrol.internal.ui;

import java.util.Locale;

import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.ui.LabelConstants;

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
			return LabelConstants.getLabel( (PatrolType.Type)item);
		}else if (item instanceof PatrolWaypointSource){
			return Messages.PatrolWaypointSource_PatrolWaypointSourceName;
		}
		if (item.equals(PATROLTEAM_TABLENAME_KEY)) return Messages.PatrolLabelProvider_PatrolTeamColumnNameKey;
		if (item.equals(PATROLTT_TABLENAME_KEY)) return Messages.PatrolLabelProvider_PatrolTransportTypeColumnName;
		if (item.equals(PATROLMANDATE_TABLENAME_KEY)) return Messages.PatrolLabelProvider_PatrolMandateColumnName;
		if (item.equals(TEAMNAME_KEY)) return Messages.PatrolLabelProvider_TeamColumnName;
		if (item.equals(TEAMDESCRIPTION_KEY)) return Messages.PatrolLabelProvider_DescriptionColumnName;
		if (item.equals(TEAMMANDATE_KEY)) return Messages.PatrolLabelProvider_MandateColumnName;
		if (item.equals(TEAMACTIVE_KEY)) return Messages.PatrolLabelProvider_TeamActiveColumnName;
		if (item.equals(TRANSPORTNAME_KEY)) return Messages.PatrolLabelProvider_TransportTypeColumnName;
		if (item.equals(TRANSPORTACTIVE_KEY)) return Messages.PatrolLabelProvider_TransportActiveColumnName;
		if (item.equals(TRANSPORTTYPE_KEY)) return Messages.PatrolLabelProvider_PatrolTypeColumnName;
		if (item.equals(MANDATENAME_KEY)) return Messages.PatrolLabelProvider_PatrolMandateColumnName;
		if (item.equals(MANDATEACTIVE_KEY)) return Messages.PatrolLabelProvider_MandateActiveColumnName;
		return null;
	}

}
