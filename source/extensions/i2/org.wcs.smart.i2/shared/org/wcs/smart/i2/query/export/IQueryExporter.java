package org.wcs.smart.i2.query.export;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;

import org.wcs.smart.i2.query.IPagedQueryResultSet;

public interface IQueryExporter {

	public enum ExportOption{
		DELIMITER,
		PROJECTION,
		LOCALE;
	}
	
	public void exportQuery(IPagedQueryResultSet results, Path destination, HashMap<ExportOption,Object> exportOptions) throws Exception;
	
	public boolean supportsOption(ExportOption option);
	
	public String getName(Locale l);
	
	public String getExtension();
}
