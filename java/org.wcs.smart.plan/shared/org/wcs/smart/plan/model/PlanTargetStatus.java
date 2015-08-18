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


import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.plan.IPlanLabelProvider;

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
		COMPLETE ("C"), //$NON-NLS-1$
		INCOMPLETE("I"), //$NON-NLS-1$
		UNKNOWN("U" ); //$NON-NLS-1$
		
		public String key;
		
		private Status (String key){
			this.key = key;
		}
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IPlanLabelProvider.class).getLabel(this, l);
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
	public String getDisplayString(Locale l){
		if (this.displayText == null){
			return this.status.getGuiName(l);
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
