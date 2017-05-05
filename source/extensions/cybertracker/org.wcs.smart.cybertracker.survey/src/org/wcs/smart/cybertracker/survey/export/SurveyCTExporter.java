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
package org.wcs.smart.cybertracker.survey.export;

import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;

/**
 * Exporter for Surveys using {@link ConfigurableModel} to CyberTracker application
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyCTExporter extends CyberTrackerConfExporter {

	@Override
	protected ScreensUtil createScreensUtil(CyberTrackerUtil ctu) {
		return new SurveyScreensUtil(ctu);
	}
	
	@Override
	protected void processExportSource(Elements elems, Object exportSource) {
		super.processExportSource(elems, exportSource);
		CyberTrackerId id = new CyberTrackerId();
		ElementsUtil.addElementsItem(elems, SurveyScreensUtil.RESULT_SURVEY_DESIGN, id.getItemId(), getSourceKey(exportSource));
		
	}
	
	private String getSourceKey(Object src) {
		if (src instanceof SurveyDesignEditorInput) {
			return ((SurveyDesignEditorInput) src).getSurveyDesignKey();
		} else if (src instanceof SurveyDesign) {
			return ((SurveyDesign) src).getKeyId();
		}
		throw new IllegalArgumentException("Unsupported export source object"); //$NON-NLS-1$
	}
}
