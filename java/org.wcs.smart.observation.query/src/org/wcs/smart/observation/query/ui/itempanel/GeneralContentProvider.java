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
package org.wcs.smart.observation.query.ui.itempanel;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.query.internal.Messages;

/**
 * Tree content provider for "General" Items
 * @author Emily
 *
 */
public class GeneralContentProvider implements ITreeContentProvider{

	public static final LabelProvider LABELPROVIDER = new LabelProvider(){
		public String getText(Object element){
			if (element instanceof GeneralItems){
				return ((GeneralItems) element).guiName;
			}
			return super.getText(element);
		}
		
		public Image getImage(Object element){
			if (element instanceof GeneralItems){
				return ((GeneralItems) element).image;
			}
			return super.getImage(element);
		}
	};
	/**
	 * Other item children
	 */
	public enum GeneralItems{
		WAYPOINT_SOURCE(Messages.QueryFilterContentProvider_WaypointSourceName,
				ObservationPlugIn.getDefault().getImageRegistry().get(ObservationPlugIn.WAYPOINT_SOURCE_ICON));
		
		public String guiName;
		private Image image;
		GeneralItems(String guiName, Image image){
			this.guiName = guiName;
			this.image = image;
		}
	}
	
	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return GeneralItems.values();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}

}
