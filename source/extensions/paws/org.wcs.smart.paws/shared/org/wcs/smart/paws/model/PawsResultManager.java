package org.wcs.smart.paws.model;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;

public class PawsResultManager {

	private PawsRun run;
	
	private Integer numRows = null;
	private String headers[] = null;
	
	private Path resultsFile = null;
	
	public PawsResultManager(PawsRun run) {
		this.run = run;
		
//		resultsFile = PawsManager.INSTANCE.getDirectory(run).resolve(run.getResultLocation());
		resultsFile = Paths.get("C:\\data\\SMART\\paws\\predictions_March-April_2020.csv");
	}
	
	public Path getResultsFile() {
		return this.resultsFile;
	}

	public PawsRun getRun() {
		return this.run;
	}
	
	/**
	 * populate the data array with size elements starting at start index
	 * @param startIndex
	 * @param size
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public void getData(int startIndex, int size, List<String[]> data) throws Exception{		
		try(BufferedReader breader = Files.newBufferedReader(resultsFile)){
			try(CSVReader reader = new CSVReader(breader, CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, startIndex)){
				int cnt = 0;
				while(cnt < size) {
					cnt++;
					String[] item = reader.readNext();
					if (item != null) {
						data.add(item);
					}else {
						break;
					}
					
				}
			}
		}catch (Exception ex) {
			PawsPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	
	public synchronized String[] getHeaders() {
		if (headers == null) loadSummary();
		return headers;
	}
	
	public synchronized int getNumRows() {
		if (numRows == null) loadSummary();
		return numRows;
	}
	
	private synchronized void loadSummary() {
		try(BufferedReader breader = Files.newBufferedReader(resultsFile)){
			try(CSVReader reader = new CSVReader(breader)){
				int size = 0;
				headers = reader.readNext();
				while(reader.readNext() != null) size++;
				
				this.numRows = size;
			}
		}catch (Exception ex) {
			PawsPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	public List<Path> getRasterFiles() {
		List<Path> items = new ArrayList<>();
		for (String x : getHeaders()) {
			if (!x.startsWith("threshold")) continue;
			String fname = x.replaceAll("=","_").replaceAll(".", "_");
			items.add(PawsManager.INSTANCE.getDirectory(run).resolve(fname));
		}
		return items;
	}
	
	public void createOutput(Path resultLayer) throws Exception{
		if (Files.exists(resultLayer)) return; //created
		
		//create a raster files out of this layer
		String headername = resultLayer.getFileName().toString();
		SharedUtils.getFilenameWithoutExtension(headername);
		String columnname = headername.replaceAll("threshold_", "threshold=").replaceAll("_", ".");
		
		String[] headers = getHeaders();
		int index = -1;
		int xindex = -1;
		int yindex = -1;
		for (int i = 0; i < headers.length; i ++ ) {
			if(headers[i].equalsIgnoreCase(columnname)) {
				index = i;
			}else if (headers[i].equalsIgnoreCase("x")) {
				xindex = -1;
			}else if (headers[i].equalsIgnoreCase("y")) {
				yindex = -1;
			}
		}
		if (index < 0) throw new Exception(MessageFormat.format("No results layer with name {0} found", columnname));
		if (xindex < 0) throw new Exception("X coordinate column not found");
		if (yindex < 0) throw new Exception("Y coordinate column not found");
		
		//create raster file with data in column index 
		//TODO: implement this
	}
}
