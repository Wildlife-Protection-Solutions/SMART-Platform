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

import java.util.HashMap;
import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.patrol.metadata.PatrolAttributeMetadata;
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
				case MIXED: return Messages.getString("PatrolLabelProvider.MixedPatrol", l); //$NON-NLS-1$
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
	
	@Override
	public HashMap<Locale, String> getNames(PatrolAttributeMetadata.FixedMetadata metadataOption){
		HashMap<Locale, String> translations = new HashMap<>();
		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
		case ARMED: 
			key = "PatrolLabelProvider.ArmedMetadata"; //$NON-NLS-1$
			break;
		case COMMENT:
			key = "PatrolLabelProvider.CommentMetadata"; //$NON-NLS-1$
			break;
		case EMPLOYEES:
			key = "PatrolLabelProvider.MembersMetadata"; //$NON-NLS-1$
			break;
		case ENDDATE:
			key = "PatrolLabelProvider.EndDateMetadata"; //$NON-NLS-1$
			break;
		case LEADER:
			key = "PatrolLabelProvider.LeaderMetadata"; //$NON-NLS-1$
			break;
		case MANDATE:
			key = "PatrolLabelProvider.MandateMetadata"; //$NON-NLS-1$
			break;
		case OBJECTIVE:
			key = "PatrolLabelProvider.ObjectiveMetadata"; //$NON-NLS-1$
			break;
		case PATROLID:
			key = "PatrolLabelProvider.PatrolIdMetadata"; //$NON-NLS-1$
			break;
		case PILOT:
			key = "PatrolLabelProvider.PilotMetadata"; //$NON-NLS-1$
			break;
		case STARTDATE:
			key = "PatrolLabelProvider.StartDateMetadata"; //$NON-NLS-1$
			break;
		case STATION:
			key = "PatrolLabelProvider.StationMetadata"; //$NON-NLS-1$
			break;
		case TEAM:
			key = "PatrolLabelProvider.TeamMetadata"; //$NON-NLS-1$
			break;
		case TRANSPORT_TYPE:
			key = "PatrolLabelProvider.TransporttypeMetadata"; //$NON-NLS-1$
			break;
		default:
			break;
		
		}
		
		
		//english
		Locale en = Locale.forLanguageTag("en"); //$NON-NLS-1$
		String enl = Messages.getString(key, Locale.ROOT);
		translations.put(en, enl);

		for (Locale locale : Locale.getAvailableLocales()) {
			if (locale.equals(en)) continue;
			
			String value = Messages.getLocaleString(key, locale);
			if (value != null) {
				translations.put(locale, value);
			}
		}
		
		return translations;
	}

}
