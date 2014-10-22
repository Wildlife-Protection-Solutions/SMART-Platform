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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.summary.MissionValueItem;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;

/**
 * Survey values tree node.
 * 
 * @author Emily
 *
 */
public class SurveyValuesTreeNode implements IItemTreeNode{

	public static final String KEY = "surveyitem"; //$NON-NLS-1$
	
	private ITreeContentProvider provider;
	private LabelProvider labelprovider;
		
	public enum Node {
		MISSION_LENGTH(MissionValueItem.ValueItem.TRACK_LENGTH.guiName, ERQueryPlugIn.getDefault().getImageRegistry().get(ERQueryPlugIn.TRACK_DISTANCE_ICON)),
		MISSION_COUNT(MissionValueItem.ValueItem.MISSION_COUNT.guiName, ERQueryPlugIn.getDefault().getImageRegistry().get(ERQueryPlugIn.MISSION_COUNT_ICON)),
		SURVEY_COUNT(MissionValueItem.ValueItem.SURVEY_COUNT.guiName, ERQueryPlugIn.getDefault().getImageRegistry().get(ERQueryPlugIn.SURVEY_COUNT_ICON)),
		MISSION_DAY_COUNT(MissionValueItem.ValueItem.DAY_COUNT.guiName, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_DAYS_ICON)),
		MISSION_HOUR_COUNT(MissionValueItem.ValueItem.HOUR_COUNT.guiName, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_NUM_HOURS_ICON));

		private String guiName;
		private Image image;
		
		private Node(String name, Image image){
			guiName = name;
			this.image = image;
		}
	};
	/**
	 * type of node
	 * @param type
	 */
	public SurveyValuesTreeNode(){
		provider = new ITreeContentProvider(){

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return Node.values();
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return SurveyValuesTreeNode.this;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
			
		};
		labelprovider = new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Node){
					return ((Node)element).guiName;
				}
				return super.getText(element);
			}
			
			@Override
			public Image getImage(Object element){
				if (element instanceof Node){
					return ((Node)element).image;
				}
				return null;
			}
		};
	}
	@Override
	public String getName() {
		return Messages.SurveyValuesTreeNode_SurveyValuesNodeLabel;
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
