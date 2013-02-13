package org.wcs.smart.export.config.impl;

import org.wcs.smart.export.AgencyCsvExport;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvDialogConfig;
import org.wcs.smart.export.config.ICsvExportDialogConfig;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Configuration for current {@link CsvExportDialog} to export
 * agencies and ranks into csv file
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class AgencyCsvExportConfig implements ICsvExportDialogConfig {

	private AgencyCsvExport exporter = new AgencyCsvExport();

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
		return Messages.CsvConfig_Agency_Export_Info +
				SmartUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Export_Info_Content +
				SmartUtils.LINE_SEPARATOR + SmartUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Example_Label +
				SmartUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Example_HeaderRow +
				SmartUtils.LINE_SEPARATOR +
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

	@Override
	public String getActionButtonText() {
		return ICsvDialogConfig.EXPORT_ACTION_TEXT;
	}

}