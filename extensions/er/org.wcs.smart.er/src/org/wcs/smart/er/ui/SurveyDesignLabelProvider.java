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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;

/**
 * Label provider and image provder for survey designs,
 * surveys and missions.
 * 
 * @author Emily
 *
 */
public class SurveyDesignLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element){
		if (element instanceof SurveyListTreeNode){
			return ((SurveyListTreeNode)element).getLabel();
		}
		if (element instanceof Mission ){
			return ((Mission) element).getId();
		}
		if (element instanceof Survey){
			return ((Survey)element).getId();
		}
		if (element instanceof SurveyDesign){
			return ((SurveyDesign) element).getName();
		}
		if (element instanceof SurveyDesignInput){
			return ((SurveyDesignInput) element).getName();
		}
		return super.getText(element);
	}
	
	public Image getImage(Object element) {
		if (element instanceof Survey || 
			((element instanceof SurveyListTreeNode) &&
				((SurveyListTreeNode)element).getType() == Type.SURVEY)){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
		}
		if (element instanceof Mission ||
				((element instanceof SurveyListTreeNode) &&
					((SurveyListTreeNode)element).getType() == Type.MISSION)){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_ICON);
		}
		if(element instanceof SurveyDesign || 
				element instanceof SurveyDesignInput){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_DESIGN_ICON);
			
		}
		if (element instanceof SurveyListTreeNode && ((SurveyListTreeNode)element).getType() == Type.SURVEY_DESIGN){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_DESIGN_ICON); 
		}
			
		return null;
	}
}
