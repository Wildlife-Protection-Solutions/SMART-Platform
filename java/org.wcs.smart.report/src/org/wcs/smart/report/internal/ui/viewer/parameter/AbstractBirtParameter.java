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
package org.wcs.smart.report.internal.ui.viewer.parameter;

import org.eclipse.swt.widgets.Composite;

/**
 * Abstract class for ui component for displaying report parameters.
 * <p>Each report parameter types needs a distinct implementation.<p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class AbstractBirtParameter {

	private String paramName = null;
	private String displayText = null;
	
	/**
	 * Creates anew parameter element
	 * @param name the parameter name
	 * @param displayText the parameter display text
	 */
	public AbstractBirtParameter(String name, String displayText){
		this.paramName = name;
		this.displayText = displayText;
	}
	
	/**
	 * Creates the ui component for collecting the parameter information
	 * @param parent
	 * @return
	 */
	public abstract Composite createComponent(Composite parent);
	
	/**
	 * 
	 * @return the parameter name
	 */
	public String getParameterName(){
		return this.paramName;
	}
	
	/**
	 * 
	 * @return the display text
	 */
	protected String getDisplayText(){
		if (this.displayText != null){
			return this.displayText;
		}else{
			return this.paramName;
		}
	}
	
	/**
	 * 
	 * @return the parameter value selected by the user
	 */
	public abstract Object getParameterValue();
	
}
