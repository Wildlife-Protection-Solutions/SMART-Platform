/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.survey.json;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.survey.model.ISurveyCyberTrackerLabelProvider;

/**
 * @since 8.0
 */
public class MissionJsonImportWarning extends JsonImportWarning {
	
	public enum WarningType{
		TRACK_POINT_MULTI_MATCHES,
		SU_NOT_FOUND,
		REST_TIME_ERROR,
		MISSION_NOT_FOUND,
		SURVEY_DESIGN_NOTFOUND,
		MISSION_ATTRIBUTE_NOT_FOUND,
		MULTIPLE_ATTRIBUTES_FOUND,
		LIST_ITEM_NOT_FOUND,
		MEMBER_NOT_FOUND;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(ISurveyCyberTrackerLabelProvider.class).getLabel(this, l);
		}
	}

	public MissionJsonImportWarning(WarningType type, Object...data) {
		super(l->type.getMessage(l), data);
	}
}
