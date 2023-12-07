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
package org.wcs.smart.dataentry.dialog.composite;

import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Info composite for {@link CmAttribute} of text type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class GeometryAttributeInfoComposite extends CmAttributeInfoComposite {


	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public GeometryAttributeInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, model, session);
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite#createTypeSpecificControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);

		
	}
	
}
