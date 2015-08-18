package org.wcs.smart.export.config.impl;

import org.wcs.smart.export.AgencyCsvExporter;
import org.wcs.smart.export.config.AbstractCsvExportConfig;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SharedUtils;

/**
 * Configuration for current {@link CsvExportDialog} to export
 * agencies and ranks into csv file
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class AgencyCsvExportConfig extends AbstractCsvExportConfig {

	private AgencyCsvExporter exporter = new AgencyCsvExporter();

	@Override
	public ICsvDataExporter getExporter() {
		return exporter;
	}
	
	@Override
	public String getDefaultFileName(){
		return SmartDB.getCurrentConservationArea().getId() + "_agencies"; //$NON-NLS-1$
	}
	
	@Override
	public String getInfo() {
		return Messages.CsvConfig_Agency_Export_Info +
				SharedUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Export_Info_Content +
				SharedUtils.LINE_SEPARATOR + SharedUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Example_Label +
				SharedUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Example_HeaderRow +
				SharedUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Example_ContentRow;
	}

	@Override
	public String getTitle() {
		return Messages.CsvConfig_Agency_Export_Title;
	}

	@Override
	public String getMessage() {
		return Messages.CsvConfig_Agency_Export_Message;
	}

	@Override
	public String getSuccessMessage() {
		return Messages.CsvConfig_Agency_Export_Success;
	}

	@Override
	public String getFailMessage() {
		return Messages.CsvConfig_Agency_Export_Fail;
	}

}