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

import java.util.HashMap;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;

/**
 * Abstract class for ui component for displaying report parameters.
 * <p>Each report parameter types needs a distinct implementation.<p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class AbstractBirtParameter implements IBirtParameterComponent {

	private String paramName = null;
	private String displayText = null;
	protected Object defaultValue = null;
	/**
	 * Creates anew parameter element
	 * @param name the parameter name
	 * @param displayText the parameter display text
	 */
	public AbstractBirtParameter(String name, String displayText, Object defaultValue){
		this.paramName = name;
		this.displayText = displayText;
		this.defaultValue = defaultValue;
	}
	
	/**
	 * Gets the parameter value to initialize the display with.  If a default
	 * value has been provided in the parameter, this will return the
	 * default value, otherwise it will look in the settings
	 * for a setting with the key as the parameter name and return 
	 * that.  If none found null is returned. 
	 * @return
	 */
	protected Object getInitializeValue(IDialogSettings settings) {
		if (this.defaultValue != null) return this.defaultValue;
		String x = settings.get(getParameterName());
		if (x != null) return x;
		return null;
	}
	
	/**
	 * Creates the ui component for collecting the parameter information
	 * @param parent
	 * @return
	 */
	public abstract Composite createComposite(Composite parent, IDialogSettings settings);
	
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
	
	public abstract Object getParameterValue();
	/**
	 * 
	 * @return the parameter value selected by the user
	 */
	@Override
	public HashMap<String, Object> getParameters(){
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(getParameterName(), getParameterValue());
		return params;
		
	}
	
}
