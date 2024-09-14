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
package org.wcs.smart.patrol.internal.export;

import org.wcs.smart.export.config.AbstractCsvExportConfig;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.util.SharedUtils;

/**
 * Configuration for current {@link CsvExportDialog} to export
 * track types and transport types into a single csv file
 * 
 * @author Emily
 * @since 2.0.0
 */
public class PatrolTransportCsvExportConfig extends AbstractCsvExportConfig {

	public static final String TRACKTYPE = "tracktype"; //$NON-NLS-1$
	public static final String TRANSPORTTYPE = "transporttype"; //$NON-NLS-1$
	public static final String GROUP = "group"; //$NON-NLS-1$
	
	private PatrolTransportCsvExporter exporter = new PatrolTransportCsvExporter();

	@Override
	public String getDefaultFileName(){
		return SmartDB.getCurrentConservationArea().getId() + "_tracks_and_transports.csv"; //$NON-NLS-1$
	}
	
	@Override
	public ICsvDataExporter getExporter() {
		return exporter;
	}
	
	@Override
	public boolean includeHasHeader() {
		return false;
	}

	@Override
	public String getHasHeaderText() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getInfo() {
		return Messages.PatrolTransportCsvExportConfig_Message1
				+ SharedUtils.LINE_SEPARATOR
				+ Messages.PatrolTransportCsvExportConfig_Message1a1
				+ SharedUtils.LINE_SEPARATOR + 
				Messages.PatrolTransportCsvExportConfig_Message3;
	}

	@Override
	public String getTitle() {
		return Messages.PatrolTransportCsvExportConfig_Title1;
	}

	@Override
	public String getMessage() {
		return Messages.PatrolTransportCsvExportConfig_DialogMessage1;
	}

	@Override
	public String getSuccessMessage() {
		return Messages.PatrolTransportCsvExportConfig_OkMessage;
	}

	@Override
	public String getFailMessage() {
		return Messages.PatrolTransportCsvExportConfig_FailedMessage;
	}

}
