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
package org.wcs.smart.er.query.ui.panels.item;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.panels.item.FilterContentProvider.Node;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Tree node for the filter list that represents the 
 * survey filter options.
 * 
 * @author Emily
 *
 */
public class FiltersTreeNode implements IItemTreeNode{

	public static final String KEY = "surveyitem"; //$NON-NLS-1$
	
	private ITreeContentProvider provider;
	private LabelProvider labelprovider;
		
	
	/**
	 * type of node
	 * @param type
	 */
	public FiltersTreeNode(){
		provider = new FilterContentProvider();
		labelprovider = new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof FilterContentProvider.Node){
					return ((FilterContentProvider.Node)element).guiName;
				}else if (element instanceof MissionProperty){
					return ((MissionProperty) element).getAttribute().getName();
				}else if (element instanceof MissionAttribute){
					return ((MissionAttribute)element).getName();
				}else if (element instanceof Survey){
					return ((Survey)element).getId();
				}else if (element instanceof Mission){
					return ((Mission)element).getId();
				}else if (element instanceof SamplingUnit){
					return ((SamplingUnit) element).getId();
				}else if (element instanceof MissionTrack){
					return ((MissionTrack) element).getId();
				}
				return super.getText(element);
			}
			
			@Override
			public Image getImage(Object element){
				if (element instanceof FilterContentProvider.Node){
					FilterContentProvider.Node node = (Node) element;
					if (node == Node.MISSION_ID){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_ICON);
					}else if (node == Node.MISSION_PROP){
						return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
					}else if (node == Node.SURVEY_ID){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
					}else if (node == Node.SURVEY_MISSION){
						return ERQueryPlugIn.getDefault().getImageRegistry().get(ERQueryPlugIn.ALL_SURVEY_ICON);
					}else if (node == Node.SAMPLING_UNITS){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_ICON);
					}else if (node == Node.OBSERVER){
						return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EMPLOYEE_ICON);
					}
				}
				if (element instanceof SamplingUnit){
					return ((SamplingUnit) element).getType().getImage();
				}
				if (element instanceof MissionTrack){
					return SamplingUnit.SamplingUnitType.OPEN_TRANSECT.getImage();
				}
				if (element instanceof MissionProperty){
					element = ((MissionProperty) element).getAttribute();
				}
				if (element instanceof MissionAttribute){
					return ((MissionAttribute) element).getType().getImage();
				}
				if (element instanceof Survey){
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
				}
				if (element instanceof Mission){
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_ICON);
				}
				return null;
			}
		};
	}
	@Override
	public String getName() {
		return Messages.SurveyItemTreeNode_SurveyFiltersNode;
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return labelprovider;
	}
	@Override
	public String getKey() {
		return KEY;
	}
	
	@Override
	public Image getImage() {
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
	}

}
