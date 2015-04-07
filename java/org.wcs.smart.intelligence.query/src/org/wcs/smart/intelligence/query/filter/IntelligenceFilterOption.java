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
package org.wcs.smart.intelligence.query.filter;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.patrol.SmartPatrolPlugIn;

/**
 * Intelligence filter options.  These are the fields 
 * that can be queried in intelligence record queries.
 * 
 * @author Emily
 *
 */
public enum IntelligenceFilterOption {
	NAME(Messages.IntelligenceFilterOption_NameOption, "name", null ), //$NON-NLS-1$
	SOURCE(Messages.IntelligenceFilterOption_SoureOption, "source", null), //$NON-NLS-1$
	PATROLID(Messages.IntelligenceFilterOption_PatrolIdOption, "patrolid", SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON)), //$NON-NLS-1$
	DESCRIPTION(Messages.IntelligenceFilterOption_DescriptionOption, "description", null), //$NON-NLS-1$
	INFORMANTID(Messages.IntelligenceFilterOption_InformationIdOption, "informantid", IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INFORMANT_ICON)); //$NON-NLS-1$
	
	
	private String name;
	private String key;
	private Image image;
	
	IntelligenceFilterOption(String name, String key, Image image){
		this.name = name;
		this.key = key;
		this.image = image;
	}
	
	/**
	 * 
	 * @return the option gui name
	 */
	public String getGuiName(){
		return this.name;
	}
	
	/**
	 * 
	 * @return the option unique key
	 */
	public String getKey(){
		return this.key;
	}
	
	/**
	 * 
	 * @return the image or null if no image
	 */
	public Image getImage(){
		return this.image;
	}
	
}
