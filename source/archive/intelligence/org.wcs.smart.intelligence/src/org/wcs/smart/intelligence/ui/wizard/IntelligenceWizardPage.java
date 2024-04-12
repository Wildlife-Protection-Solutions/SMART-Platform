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
package org.wcs.smart.intelligence.ui.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.wcs.smart.intelligence.model.Intelligence;

/**
 * An abstract class for intelligence wizard pages.
 * 
 * @author elitvin
 *
 */
public abstract class IntelligenceWizardPage extends WizardPage {

    /**
     * @param pageName
     */
    public IntelligenceWizardPage(String pageName) {
        super(pageName);
   }

    /**
     * Updates the current intelligence with the new values inputed
     * in the wizard page.
     * 
     * @param intelligence intelligence to update
     * @return <code>true</code> of model updated; <code>false</code> if error 
     */
    abstract protected boolean updateModel(Intelligence intelligence);

    /**
     * Updates the current page gui components with the values
     * from the intelligence
     * 
     * @param intelligence intelligence to use when updating gui components
     */
    abstract protected void initFromModel(Intelligence intelligence);
}
