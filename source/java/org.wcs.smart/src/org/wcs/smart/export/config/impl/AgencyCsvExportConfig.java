package org.wcs.smart.export.config.impl;

import org.wcs.smart.export.AgencyCsvExport;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvDialogConfig;
import org.wcs.smart.export.config.ICsvExportDialogConfig;
import org.wcs.smart.export.dialog.CsvExportDialog;

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
		return "Include header";
	}

	@Override
	public String getInfo() {
		return "Agency and rank info";
	}

	@Override
	public String getTitle() {
		return "Export Agencies and Ranks";
	}

	@Override
	public String getMessage() {
		return "Export Agencies and Ranks into csv file";
	}

	@Override
	public String getSuccessMessage() {
		return "Agencies and Ranks successfully exported.";
	}

	@Override
	public String getFailMessage() {
		return "Failed to export Agencies and Ranks.";
	}

	@Override
	public String getActionButtonText() {
		return ICsvDialogConfig.EXPORT_ACTION_TEXT;
	}

}