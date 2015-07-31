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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.panels.item.FilterContentProvider.Node;
import org.wcs.smart.er.ui.ErLabelProvider;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;
import org.wcs.smart.query.model.IQueryType;

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
	public FiltersTreeNode(IQueryType qType){
		provider = new FilterContentProvider(qType);
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
					String value = ((MissionTrack) element).getId();
					if (value == null){
						value = ""; //$NON-NLS-1$
					}
					return MessageFormat.format("{0} [{1}]", new Object[]{value, DateFormat.getDateInstance().format(((MissionTrack)element).getMissionDay().getDate())});  //$NON-NLS-1$
				}else if (element instanceof SamplingUnitAttribute){
					return ((SamplingUnitAttribute) element).getName();
				}else if (element instanceof MissionTrack.TrackType){
					return ((MissionTrack.TrackType)element).getGuiName(Locale.getDefault());
				}else if (element instanceof SamplingUnitWrapper){
					return getText(((SamplingUnitWrapper) element).getSamplingUnit());
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
					}else if (node == Node.SAMPLING_UNITS ||
							node == Node.SAMPLING_UNITS_OBS || 
							node == Node.SAMPLING_UNITS_TRK ){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_ICON);
					}else if (node == Node.OBSERVER){
						return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EMPLOYEE_ICON);
					}else if (node == Node.SAMPLING_UNIT_ATTRIBUTE ||
							node == Node.SAMPLING_UNIT_ATTRIBUTE_OBS || 
							node == Node.SAMPLING_UNIT_ATTRIBUTE_TRK ){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_ATTRIBUTE_ICON);
					}else if (node == Node.MISSION_LEADER){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_LEADER_ICON);
					}else if (node == Node.MISSION_MEMBER){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_MEMBER_ICON);
					}else if (node == Node.TRACKTYPE){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_RECON_ICON);
					}else if (node == Node.OBSERVATION_SAMPLING_UNIT){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.OBS_SAMPLING_UNIT_ICON);
					}else if (node == Node.TRACK_SAMPLING_UNIT){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.TRK_SAMPLING_UNIT_ICON);
					}
				}
				if (element instanceof SamplingUnit){
					if (element.equals(SamplingUnitFilter.NONE)){
						return null;
					}
					return ErLabelProvider.getImage(((SamplingUnit)element).getType());
				}
				if (element instanceof SamplingUnitWrapper){
					return getImage(((SamplingUnitWrapper) element).getSamplingUnit());
				}
//				if (element instanceof MissionTrack){
//					return SamplingUnit.GeometryType.RECON.getImage();
//				}
				if (element instanceof MissionProperty){
					element = ((MissionProperty) element).getAttribute();
				}
				if (element instanceof MissionAttribute){
					return DataModel.getAttributeImage(((MissionAttribute) element).getType());
				}
				if (element instanceof Survey){
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
				}
				if (element instanceof Mission){
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_ICON);
				}
				if (element instanceof SamplingUnitAttribute){
					return DataModel.getAttributeImage(((SamplingUnitAttribute) element).getType());
				}
				if (element instanceof MissionTrack.TrackType){
					if (element == MissionTrack.TrackType.SAMPLING_UNIT){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_TRANSECT_ICON);
					}else if (element == MissionTrack.TrackType.TRACK){
						return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_RECON_ICON);
					}
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
