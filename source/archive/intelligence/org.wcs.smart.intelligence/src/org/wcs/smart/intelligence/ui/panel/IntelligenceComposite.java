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
package org.wcs.smart.intelligence.ui.panel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.model.Intelligence;

/**
 * Composite used to create controls that work with {@link Intelligence} object
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class IntelligenceComposite extends Composite implements IIntelligenceModifier {

	private String message;
	private String errorMessage;
	
	private List<IInputChangeListener> inputListeners = new ArrayList<IInputChangeListener>();

	/**
	 * @param parent
	 * @param style
	 */
	public IntelligenceComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * Returns if data in page is valid and can be saved in database.
	 * <code>true</code> by default
	 */
	@Override
	public final boolean isDataValid() {
		return getErrorMessage() == null;
	}

	/**
	 * Subclasses must implement this method and properly set error message
	 * if validation is required for the component. <br/>
	 * Example: <br/><code>
	 * if (isValid()) { <br/>
	 * 	  setErrorMessage(null); <br/>
	 * } else { <br/>
	 *    setErrorMessage("Data is not valid"); <br/>
	 * } </code>
	 */
	protected void validate() {
		//nothing by default
	}
	
	@Override
	public final boolean updateModel(Intelligence intelligence) {
		validate();
		if (isDataValid()) {
			updateModelInternal(intelligence);
			return true;
		}
		IntelligencePlugIn.displayLog(getErrorMessage(), null);
        return false;
	}

	protected abstract void updateModelInternal(Intelligence intelligence);
	
	public void addInputChangeListener(IInputChangeListener listener) {
		inputListeners.add(listener);
	}

	public void removeInputChangeListener(IInputChangeListener listener) {
		inputListeners.remove(listener);
	}
	
	protected void fireInputChangeListeners() {
		validate();
		for (IInputChangeListener listener : inputListeners) {
			listener.inputChanged(this);
		}
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	protected void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
}
