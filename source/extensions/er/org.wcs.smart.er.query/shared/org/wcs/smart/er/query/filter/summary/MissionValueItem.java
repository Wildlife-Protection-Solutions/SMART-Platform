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
package org.wcs.smart.er.query.filter.summary;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.er.query.ISurveyQueryLabelProvider;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.summary.IValueItem;

/**
 * Total mission track length value item.
 * 
 * @author Emily
 *
 */
public class MissionValueItem implements IValueItem {

	public enum ValueItem{
		TRACK_LENGTH("s:missiontracklength"), //$NON-NLS-1$
		MISSION_COUNT("s:missioncount"),  //$NON-NLS-1$
		SURVEY_COUNT("s:surveycount"),  //$NON-NLS-1$
		DAY_COUNT("s:missiondaycount"),  //$NON-NLS-1$
		HOUR_COUNT("s:missionhourcount"),  //$NON-NLS-1$
		MANHOURS_COUNT("s:missionpersonhourcount"),  //$NON-NLS-1$
		
		TRACK_LENGTH_TOTAL("s:totalmissiontracklength"), //$NON-NLS-1$
		MISSION_COUNT_TOTAL("s:totalmissioncount"), //$NON-NLS-1$
		SURVEY_COUNT_TOTAL("s:totalsurveycount"); //$NON-NLS-1$
		
		public String key;
		
		private ValueItem(String key){
			this.key = key;
		}
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(ISurveyQueryLabelProvider.class).getLabel(this,l);
		}
	};

	/**
	 * Create new mission length item
	 * @return
	 */
	public static MissionValueItem createTrackLengthItem(){
		return new MissionValueItem(ValueItem.TRACK_LENGTH);
	}
	
	/**
	 * Create new mission length item
	 * @return
	 */
	public static MissionValueItem createTotalTrackLengthItem(){
		return new MissionValueItem(ValueItem.TRACK_LENGTH_TOTAL);
	}
	
	/**
	 * Create new mission count item
	 * @return
	 */
	public static MissionValueItem createMissionCountItem(){
		return new MissionValueItem(ValueItem.MISSION_COUNT);
	}
	
	/**
	 * Create new total mission count item
	 * @return
	 */
	public static MissionValueItem createTotalMissionCountItem(){
		return new MissionValueItem(ValueItem.MISSION_COUNT_TOTAL);
	}
	
	/**
	 * Create new survey count item
	 * @return
	 */
	public static MissionValueItem createSurveyCountItem(){
		return new MissionValueItem(ValueItem.SURVEY_COUNT);
	}
	
	/**
	 * Create new total survey count item
	 * @return
	 */
	public static MissionValueItem createTotalSurveyCountItem(){
		return new MissionValueItem(ValueItem.SURVEY_COUNT_TOTAL);
	}
	
	/**
	 * Create new mission day count item
	 * @return
	 */
	public static MissionValueItem createMissionDayCountItem(){
		return new MissionValueItem(ValueItem.DAY_COUNT);
	}
	
	/**
	 * Create new mission hour count item
	 * @return
	 */
	public static MissionValueItem createMissionHoursCountItem(){
		return new MissionValueItem(ValueItem.HOUR_COUNT);
	}
	
	/**
	 * Create new mission hour count item
	 * @return
	 */
	public static MissionValueItem createMissionPersonHoursCountItem(){
		return new MissionValueItem(ValueItem.MANHOURS_COUNT);
	}
	
	private MissionValueItem.ValueItem item;
	
	public MissionValueItem(ValueItem item){
		this.item = item;
	}
	
	public MissionValueItem.ValueItem getValueItem(){
		return this.item;
	}
	
	@Override
	public String asString() {
		return item.key;
	}

	@Override
	public void accept(IValueVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * If this particular item should filter values based on group by
	 * 
	 * @return
	 */
	public boolean requiresGroupByFilter(){
		if (item == ValueItem.TRACK_LENGTH_TOTAL || 
				item == ValueItem.MISSION_COUNT_TOTAL || 
				item == ValueItem.SURVEY_COUNT_TOTAL){
			return false;
		}
		return true;
	}
}
