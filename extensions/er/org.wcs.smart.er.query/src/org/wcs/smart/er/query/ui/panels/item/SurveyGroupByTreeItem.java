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
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.panels.item.SurveyGroupByContentProvider.Node;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Survey group by tree node.
 * 
 * @author Emily
 *
 */
public class SurveyGroupByTreeItem implements IItemTreeNode{

	public static final String KEY = "surveygroupby"; //$NON-NLS-1$
	
	private SurveyGroupByContentProvider provider;
	
	@Override
	public String getName() {
		return Messages.SurveyGroupByTreeItem_TreeNodeLabel;
	}

	@Override
	public Image getImage() {
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		if (provider == null){
			provider = new SurveyGroupByContentProvider();
		}
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return lblProvider;
	}

	@Override
	public String getKey() {
		return KEY;
	}
	
	private static final LabelProvider lblProvider = new LabelProvider(){
		
		public String getText(Object element){
			if (element instanceof SurveyGroupByContentProvider.Node){
				return ((SurveyGroupByContentProvider.Node)element).guiName;
			}else if (element instanceof MissionAttribute){
				return ((MissionAttribute)element).getName();
			}else if (element instanceof MissionProperty){
				return ((MissionProperty) element).getAttribute().getName();
			}else if (element instanceof SamplingUnitAttribute){
				return ((SamplingUnitAttribute)element).getName();
			}
			return super.getText(element);
		}
		
		public Image getImage(Object element){
			if (element instanceof MissionAttribute){
				return ((MissionAttribute) element).getType().getImage();
			}else if (element instanceof MissionProperty){
				return ((MissionProperty) element).getAttribute().getType().getImage();
			}else if (element instanceof SurveyGroupByContentProvider.Node){
				SurveyGroupByContentProvider.Node node = (Node) element;
				if (node == Node.MISSION_ID){
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_ICON);
				}else if (node == Node.MISSION_PROP){
					return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
				}else if (node == Node.SURVEY_ID){
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
				}else if (node == Node.SAMPLING_UNITS){
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_ICON);
				}else if (node == Node.SAMPLING_UNITS_ATT){
					return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_ATTRIBUTE_ICON);
				}else if (node == Node.OBSERVER){
					return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EMPLOYEE_ICON);
				}
			}else if (element instanceof SamplingUnitAttribute){
				return ((SamplingUnitAttribute)element).getType().getImage();
			}
			return super.getImage(element);
		}
	};

}

