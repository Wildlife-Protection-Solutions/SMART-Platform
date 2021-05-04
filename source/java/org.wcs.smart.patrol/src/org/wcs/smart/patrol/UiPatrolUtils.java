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
package org.wcs.smart.patrol;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * A set of utility classes for the UI.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class UiPatrolUtils {

	public static final String EDIT_LINK_TEXT = DialogConstants.EDIT_LINK_TEXT;
	/**
	 * Returns the image to use for a given patrol type or
	 * null if no image found
	 * 
	 * @param type patrol type
	 * 
	 * @return image associated with patrol type
	 */
	public static Image getImage(PatrolType.Type type){
		if (type == PatrolType.Type.GROUND){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.GROUND_PATROL_ICON);
		}else if (type == PatrolType.Type.MARINE){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.MARINE_PATROL_ICON);
		}else if (type == PatrolType.Type.AIR){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.AIR_PATROL_ICON);
		}else if (type == PatrolType.Type.MIXED){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.MIXED_PATROL_ICON);
		}
		return null;
	}
	
	/**
	 * Returns the image descriptor to use for a given patrol type or
	 * null if no image found
	 * 
	 * @param type patrol type
	 * 
	 * @return image descriptor associated with patrol type
	 */
	public static ImageDescriptor getImageDescriptor(PatrolType.Type type){
		if (type == PatrolType.Type.GROUND){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.GROUND_PATROL_ICON);
		}else if (type == PatrolType.Type.MARINE){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.MARINE_PATROL_ICON);
		}else if (type == PatrolType.Type.AIR){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.AIR_PATROL_ICON);
		}else if (type == PatrolType.Type.MIXED){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPatrolPlugIn.MIXED_PATROL_ICON);
		}
		return null;
	}
	
}
