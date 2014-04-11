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

import org.wcs.smart.export.config.AbstractCsvImportConfig;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.export.dialog.CsvImportDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.util.SmartUtils;

/**
 * Configuration for current {@link CsvImportDialog} to import
 * stations into csv file
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolTransportCsvImportConfig extends AbstractCsvImportConfig {

	private PatrolTransportCsvExporter exporter = new PatrolTransportCsvExporter();
	private PatrolTransportCsvImporter importer = new PatrolTransportCsvImporter();
	
	@Override
	public String getDefaultFileName(){
		return SmartDB.getCurrentConservationArea().getId() + "_transports"; //$NON-NLS-1$
	}
	
	@Override
	public ICsvDataImporter getImporter() {
		return importer;
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
		return Messages.PatrolTransportCsvImportConfig_Message1
				+ SmartUtils.LINE_SEPARATOR
				+ Messages.PatrolTransportCsvImportConfig_Message1a
				+ SmartUtils.LINE_SEPARATOR + SmartUtils.LINE_SEPARATOR
				+ Messages.PatrolTransportCsvImportConfig_Message2 + 
				PatrolType.Type.AIR.name() + ","+ PatrolType.Type.MARINE.name() + "," +	PatrolType.Type.GROUND.name() + ". " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Messages.PatrolTransportCsvImportConfig_Message3;
	}

	@Override
	public String getTitle() {
		return Messages.PatrolTransportCsvImportConfig_Title;
	}

	@Override
	public String getMessage() {
		return Messages.PatrolTransportCsvImportConfig_DialogMessage;
	}

	@Override
	public String getSuccessMessage() {
		return Messages.PatrolTransportCsvImportConfig_OkMessage;
	}

	@Override
	public String getFailMessage() {
		return Messages.PatrolTransportCsvImportConfig_FailedMessage;
	}

}
