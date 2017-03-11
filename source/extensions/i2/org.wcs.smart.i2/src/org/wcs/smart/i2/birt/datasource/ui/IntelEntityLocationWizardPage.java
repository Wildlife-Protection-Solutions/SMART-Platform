/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.birt.datasource.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.wcs.smart.i2.birt.entity.location.EntityLocationDataset;
import org.wcs.smart.i2.internal.Messages;

/**
 * Dataset wizard for entity location dataset.
 * @author Emily
 *
 */
public class IntelEntityLocationWizardPage extends AbstractIntelEntityTypeListWizardPage {

	private static final String SELECT_ENTITY_TYPE = Messages.IntelEntityLocationWizardPage_SelectType;
	
	/**
	 * Constructor
	 * 
	 * @param pageName
	 */
	public IntelEntityLocationWizardPage(String pageName) {
		super(pageName, SELECT_ENTITY_TYPE);
	}

	/**
	 * Constructor
	 * 
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public IntelEntityLocationWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage, SELECT_ENTITY_TYPE);
	}
	
	@Override
	protected String getDatasetType(){
		return EntityLocationDataset.DATASET_TYPE;
	}
	
}
