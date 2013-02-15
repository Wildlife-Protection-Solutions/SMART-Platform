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
package org.wcs.smart.plan.model;


import org.eclipse.swt.graphics.Image;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;

/**
 * Represents a PlanTarget Status 
 * 
 * @author Jeff
 * @since 1.0.0
 */
public class PlanTargetStatus{

	/**
	 * Status options
	 * @author Emily
	 *
	 */
	public enum Status{
		COMPLETE (Messages.PlanTargetStatus_Complete, SmartPlanPlugIn.getDefault().getImageRegistry().get(SmartPlanPlugIn.STATUS_COMPLETE)),
		INCOMPLETE(Messages.PlanTargetStatus_Incomplete, SmartPlanPlugIn.getDefault().getImageRegistry().get(SmartPlanPlugIn.STATUS_INCOMPLETE));
		
		public String guiName;
		public Image guiImage;
		
		private Status (String defaultGuiValue, Image guiImage){
			this.guiName = defaultGuiValue;
			this.guiImage = guiImage;
		}
		
		
	}
	
	private Status status = Status.INCOMPLETE;
	private String displayText = null;
	
	/**
	 * Creates a new status with custom display string
	 * @param status
	 * @param displayString
	 */
	public PlanTargetStatus(Status status, String displayString){
		this(status);
		this.displayText = displayString;
	}
	
	/**
	 * Creates as new status using the default display string
	 * @param status
	 */
	public PlanTargetStatus(Status status){
		this.status = status;
	}
	
	/**
	 * 
	 * @return String representation of the target
	 */
	public Status getStatus(){
		return status;
	}

	/**
	 * If the display string is not set then it returns
	 * the default value associated with the status.
	 * 
	 * @return the string to display to the gui
	 */
	public String getDisplayString(){
		if (this.displayText == null){
			return this.status.guiName;
		}
		return this.displayText;
	}
	
	/**
	 * Sets the display string to show on the gui
	 * @param name
	 */
	public void setDisplayString(String name){
		this.displayText = name;
	}
	
	/**
	 * Sets the status
	 * @param s
	 */
	public void setStatus(Status status) {
		this.status = status;
	}	
}
