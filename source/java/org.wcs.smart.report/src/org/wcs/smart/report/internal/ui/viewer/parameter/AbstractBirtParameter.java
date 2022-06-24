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

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.impl.ScalarParameterDefn;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 * Abstract class for ui component for displaying report parameters.
 * <p>Each report parameter types needs a distinct implementation.<p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class AbstractBirtParameter implements IBirtParameterComponent {

	protected Object defaultValue = null;
	
	protected IParameterDefn def;
	/**
	 * Creates anew parameter element
	 * @param name the parameter name
	 * @param displayText the parameter display text
	 */
	public AbstractBirtParameter(IParameterDefn def){
		this.def = def;
		if (def instanceof ScalarParameterDefn){
			defaultValue = ((ScalarParameterDefn)def).getDefaultValue(); 
		}
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
		String x = settings.get(getParameterName());
		if (x != null) return x;
		
		if (this.defaultValue != null) return this.defaultValue;

		return null;
	}
	
	/**
	 * Creates the ui component for collecting the parameter information
	 * @param parent
	 * @return
	 */
	public abstract void createComposite(Composite parent, IDialogSettings settings, Listener onModified);
	
	protected void createNameLabel(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(getDisplayText() + ": "); //$NON-NLS-1$
		if (def.getHelpText() != null) lbl.setToolTipText(def.getHelpText());
	}
	
	/**
	 * 
	 * @return the parameter name
	 */
	public String getParameterName(){
		return def.getName();
	}
	
	/**
	 * 
	 * @return the display text
	 */
	protected String getDisplayText(){
		if (def.getDisplayName() != null) return def.getDisplayName();
		return def.getName();
	}
	
	public abstract Object getParameterValue();
	/**
	 * 
	 * @return the parameter value selected by the user
	 */
	@Override
	public HashMap<IParameterDefn, Object> getParameters(){
		HashMap<IParameterDefn, Object> params = new HashMap<IParameterDefn, Object>();
		params.put(def, getParameterValue());
		return params;
		
	}
	
}
