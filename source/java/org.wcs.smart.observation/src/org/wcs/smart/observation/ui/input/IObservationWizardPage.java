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
package org.wcs.smart.observation.ui.input;

import org.eclipse.jface.wizard.IWizardPage;

/**
 * Interface for observation wizard pages.
 * 
 * @author Emily
 * @since 1.0.0
 */
public interface IObservationWizardPage {

	/**
	 * Called before the wizard page moves to the next page
	 * @param targetPage the page being moved to
	 * @return <code>true</code> if okay to move on; <code>false</code> otherwise
	 */
	boolean beforeMoveNext(IWizardPage targetPage);
	
	/**
	 * Called before the page is shown.
	 * 
	 * <p>Can be used to update wizard 
	 * buttons.</p>
	 */
	void beforeShow();
}
