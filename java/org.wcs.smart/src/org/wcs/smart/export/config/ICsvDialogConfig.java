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
package org.wcs.smart.export.config;

import org.wcs.smart.export.dialog.AbstractCsvDialog;

/**
 * Gui configuration for {@link AbstractCsvDialog}
 * 
 * @author elitvin
 * @since 1.0.0
 */
public interface ICsvDialogConfig {

	/**
	 * The default file name to export data to.
	 * @return
	 */
	public String getDefaultFileName();
	
	/**
	 * Indicates weather checkbox responsible for csv headers in present in gui
	 * @return boolean
	 */
	public boolean includeHasHeader();

	/**
	 * Defines text message for headers checkbox.
	 * Will appear only if includeHasHeader() returns <code>true</code>
	 * @return text message for headers checkbox
	 */
	public String getHasHeaderText();

	/**
	 * Defines text info message. Usually its a description of output or input format.
	 * @return text info message
	 */
	public String getInfo();

	/**
	 * Defines dialog title.
	 * @return dialog title
	 */
	public String getTitle();

	/**
	 * Defines dialog message.
	 * @return dialog message
	 */
	public String getMessage();

	/**
	 * Defines the message displayed after import/export successfully finished.
	 * @return text for success message
	 */
	public String getSuccessMessage();

	/**
	 * Defines the message displayed after import/export failed.
	 * @return text for fail message
	 */
	public String getFailMessage();

	/**
	 * Label for action button. Usually "Export" or "Import".
	 * @return text for action button
	 */
	public String getActionButtonText();

	/**
	 * Defines file dialog style. Usually SWT.SAVE (for export) or SWT.OPEN (for import).
	 * @return file dialog style
	 */
	public int getFileDialogStyle();
	
	/**
	 * Specifies if dialog should try to append file extension if it is not present.
	 * If <code>true</code> - extension will be appended
	 * @return boolean
	 */
	public boolean appendFileExtension();
	
}
