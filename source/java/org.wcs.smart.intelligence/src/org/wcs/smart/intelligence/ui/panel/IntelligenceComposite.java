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
import org.wcs.smart.intelligence.model.Intelligence;

/**
 * Composite used to create controls that work with {@link Intelligence} object
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class IntelligenceComposite extends Composite implements IIntelligenceModifier {

	private CompositeMode mode = CompositeMode.WIZARD;
	private String message;
	private List<IDataValidStateListener> stateListeners = new ArrayList<IDataValidStateListener>();

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
	public boolean isDataValid() {
		return true; //default value
	}

	public void addDataValidStateListener(IDataValidStateListener listener) {
		stateListeners.add(listener);
	}

	public void removeDataValidStateListener(IDataValidStateListener listener) {
		stateListeners.remove(listener);
	}
	
	public void fireDataValidStateListeners() {
		for (IDataValidStateListener listener : stateListeners) {
			listener.stateChanged(isDataValid());
		}
	}

	public CompositeMode getMode() {
		return mode;
	}

	public void setMode(CompositeMode mode) {
		if (this.mode != mode) {
			this.mode = mode;
			applyNewMode(this.mode);
		}
	}

	protected void applyNewMode(CompositeMode state) {
		//nothing; by default gui is the same in all modes
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}


	/**
	 * Possible states for composite.
	 */
	public enum CompositeMode {
		WIZARD,
		EDITOR;
	}

}
