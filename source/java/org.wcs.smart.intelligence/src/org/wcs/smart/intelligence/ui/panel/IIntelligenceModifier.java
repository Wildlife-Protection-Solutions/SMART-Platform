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

import org.wcs.smart.intelligence.model.Intelligence;

/**
 * Should be implemented by ui components that can modify {@link Intelligence} object
 * 
 * @author elitvin
 * @since 1.0.0
 */
public interface IIntelligenceModifier {

    /**
     * Updates the current intelligence with the new values inputed
     * in the gui components.
     * 
     * @param intelligence intelligence to update
     * @return <code>true</code> of model updated; <code>false</code> if error 
     */
    public boolean updateModel(Intelligence intelligence);

    /**
     * Updates the current page gui components with the values
     * from the intelligence
     * 
     * @param intelligence intelligence to use when updating gui components
     */
    public void initFromModel(Intelligence intelligence);
	

	/**
	 * Returns if data input in gui components is valid and can be save in database.
	 */
    public boolean isDataValid();
}
