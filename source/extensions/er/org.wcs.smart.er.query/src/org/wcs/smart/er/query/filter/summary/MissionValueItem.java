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

import org.hibernate.Session;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Total mission track length value item.
 * 
 * @author Emily
 *
 */
public class MissionValueItem implements IValueItem {

	public enum ValueItem{
		TRACK_LENGTH(Messages.MissionLegnthValueDropItem_TrackLengthLabel, "s:missiontracklength"), //$NON-NLS-1$
		MISSION_COUNT(Messages.MissionValueItem_NumberOfMissionsLabel, "s:missioncount"), //$NON-NLS-1$
		SURVEY_COUNT("Number of Surveys", "s:surveycount");
		
		public String guiName;
		public String key;
		
		private ValueItem(String name, String key){
			this.guiName = name;
			this.key = key;
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
	 * Create new mission count item
	 * @return
	 */
	public static MissionValueItem createMissionCountItem(){
		return new MissionValueItem(ValueItem.MISSION_COUNT);
	}
	
	/**
	 * Create new survey count item
	 * @return
	 */
	public static MissionValueItem createSurveyCountItem(){
		return new MissionValueItem(ValueItem.SURVEY_COUNT);
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
	public String getName(Session session) {
		return item.guiName;
	}

	@Override
	public String getFullName(Session session) {
		return getName(session);
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		if (item == ValueItem.TRACK_LENGTH){
			return SurveyDropItemFactory.INSTANCE.createMissionLengthValueItem();
		}else if (item == ValueItem.MISSION_COUNT){
			return SurveyDropItemFactory.INSTANCE.createMissionCountValueItem();
		}else if (item == ValueItem.SURVEY_COUNT){
			return SurveyDropItemFactory.INSTANCE.createSurveyCountValueItem();
		}
		return new ErrorDropItem(Messages.MissionValueItem_ValueItemNotSupported + item.guiName);
	}

	@Override
	public Object getDropItemInitializeData() {
		return null;
	}

	@Override
	public void accept(IValueVisitor visitor) {
		visitor.visit(this);
	}

}
