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
package org.wcs.smart.export.config.impl;

import org.wcs.smart.export.StationCsvExporter;
import org.wcs.smart.export.config.AbstractCsvExportConfig;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Configuration for current {@link CsvExportDialog} to export
 * stations into csv file
 * 
 * @author Emily
 * @since 2.0.0
 */
public class StationCsvExportConfig extends AbstractCsvExportConfig {

	private StationCsvExporter exporter = new StationCsvExporter();

	@Override
	public ICsvDataExporter getExporter() {
		return exporter;
	}
	
	@Override
	public String getDefaultFileName(){
		return SmartDB.getCurrentConservationArea().getId() + "_stations"; //$NON-NLS-1$
	}
	
	@Override
	public boolean includeHasHeader() {
		return true;
	}

	@Override
	public String getHasHeaderText() {
		return Messages.StationCsvExportConfig_IncludeHeaderText;
	}

	@Override
	public String getInfo() {
		return Messages.StationCsvExportConfig_Message1
				+ SmartUtils.LINE_SEPARATOR
				+Messages.StationCsvExportConfig_Message2
				+ SmartUtils.LINE_SEPARATOR + SmartUtils.LINE_SEPARATOR
				+ Messages.StationCsvExportConfig_Message3;
	}

	@Override
	public String getTitle() {
		return Messages.StationCsvExportConfig_Title;
	}

	@Override
	public String getMessage() {
		return Messages.StationCsvExportConfig_Message;
	}

	@Override
	public String getSuccessMessage() {
		return Messages.StationCsvExportConfig_OkMessage;
	}

	@Override
	public String getFailMessage() {
		return Messages.StationCsvExportConfig_FailedMessage;
	}

}
