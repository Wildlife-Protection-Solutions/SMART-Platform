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
package org.wcs.smart.asset.ui.views.data;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;

/**
 * Action to perform on the imported file data. 
 * 
 * 
 * @author Emily
 *
 */
public interface ImportAction {

	/**
	 * Performs the action and returns true if changes have been made.
	 * 
	 * @param processor the processor
	 * @param selectedItem the particular item selected
	 * @return true if changes are made or false otherwise
	 */
	public boolean preformAction(FileProcessor processor, FileProxy selectedItem);
	
	/**
	 * Action label to display in menu or other gui
	 * @return
	 */
	public String getMenuLabel();
	
	/**
	 * Action image to dispaly in menu or other gui element
	 * @return
	 */
	public default Image getMenuImage() {
		return null;
	}
}
