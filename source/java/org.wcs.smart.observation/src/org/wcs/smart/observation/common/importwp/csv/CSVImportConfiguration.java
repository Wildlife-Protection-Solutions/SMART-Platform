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

package org.wcs.smart.observation.common.importwp.csv;

import java.io.FileReader;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.ReprojectUtils;

import au.com.bytecode.opencsv.CSVReader;
/**
 * 
 * Class for tracking CSV import configuration 
 * information.  Maps CSV columns to waypoint fields.
 * 
 * 
 * @author Jeff
 * @author Emily
 *
 */
public class CSVImportConfiguration {

	private String filename; //the csv filename
	
	private int XColumn = -1;
	private int YColumn = -1;
	private int dateColumn = -1;
	private int timeColumn = -1;
	private int commentColumn = -1;
	private int idColumn = -1;
	
	private CoordinateReferenceSystem sourceCrs;
	private String dateFormat;
	
	private boolean skipHeaders;

	private CsvHeader[] availableColumns;
	private char delimiter;
	
	public CSVImportConfiguration(){
		
	}
	
	public char getDelimiter(){
		return this.delimiter;
	}
	
	public void setDelimiter(char delimiter){
		this.delimiter = delimiter;
	}
	
	public void setAvailableColumns(CsvHeader[] cols){
		this.availableColumns = cols;
	}
	public CsvHeader[] getAvailableColumns(){
		return this.availableColumns;
	}
	
	/**
	 * Gets all the waypoints in the given csv file
	 * @param monitor
	 * @param singleDay if only waypoints for one day are to imported this will
	 * be the day otherwise it will be null
	 * 
	 * @return
	 * @throws Exception 
	 */
	public List<Waypoint> getWaypoints(IProgressMonitor monitor, LocalDate singleDay) throws Exception {
	
		List<Waypoint> allPoints = new ArrayList<Waypoint>();
		DateTimeFormatter sdf = DateTimeFormatter.ofPattern(dateFormat);
		
		LocalDate day0 = singleDay;
		
		
		List<DateTimeFormatter> timeFormatters = new ArrayList<>();
		
		DateTimeFormatterBuilder ff = (new DateTimeFormatterBuilder())
				.parseCaseInsensitive().appendPattern("h:mm") //$NON-NLS-1$
				.optionalStart()
				.appendPattern(":ss") //$NON-NLS-1$
				.optionalEnd()
				.optionalStart()
				.appendPattern(" ") //$NON-NLS-1$
				.optionalEnd()
				.appendPattern("a"); //$NON-NLS-1$
		timeFormatters.add(ff.toFormatter());
		timeFormatters.add(ff.toFormatter(Locale.ROOT));
		
		ff = (new DateTimeFormatterBuilder())
				.parseCaseInsensitive().appendPattern("H:mm") //$NON-NLS-1$
				.optionalStart()
				.appendPattern(":ss") //$NON-NLS-1$
				.optionalEnd();
		timeFormatters.add(ff.toFormatter());
		
		
		try(CSVReader reader = new CSVReader(new FileReader(filename), getDelimiter())){
			int counter = 0;
			if(skipHeaders){
				counter = 1;
				reader.readNext();	//skip header
			}
			
			String[] row;
			while((row = reader.readNext()) != null){
				counter++;
				LocalDate ptDate = null; 
				if (row.length == 1 && row[0].trim().length() == 0){
					//this is a blank line; skip it
					break;
				}

				try {
					ptDate = LocalDate.parse( row[dateColumn].replaceAll("\\s+",""), sdf ); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (DateTimeParseException e) {
					throw new Exception(MessageFormat.format(Messages.CSVImportConfiguration_2, new Object[]{counter, row[dateColumn]}), e); 
				}
				
				LocalDate day1 = ptDate;
				if(singleDay == null ||  day0.isEqual(day1)){
					Waypoint curWP = new Waypoint();
					//reproject
					Point point = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(Double.parseDouble( row[XColumn].replaceAll("\\s+","")), Double.parseDouble( row[YColumn].replaceAll("\\s+","") ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					Point p = (Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));

					if(p.getX() > 180 || p.getX() < -180){
						throw new Exception(MessageFormat.format(Messages.CSVImportConfiguration_1, new Object[]{counter, row[XColumn]}));
					}
					if(p.getY() > 90 || p.getY() < -90){
						throw new Exception(MessageFormat.format(Messages.CSVImportConfiguration_0, new Object[]{counter, row[YColumn]}));
					}
				 	curWP.setRawX(p.getX());
				 	curWP.setRawY(p.getY());

				 	try {
				 		String strTime = row[timeColumn].replaceAll("\\s+",""); //$NON-NLS-1$ //$NON-NLS-2$
				 		
				 		LocalTime ltime = null;
				 		DateTimeParseException e = null;
				 		for (DateTimeFormatter f : timeFormatters) {
				 			try {
					 			ltime = LocalTime.parse(strTime,f);
					 		}catch (DateTimeParseException ex) {
					 			e = ex;
					 		}	
				 			if (ltime != null) break;
				 		}
				 		if (ltime == null) throw e;
				 		
				 		
				 		
					 	curWP.setDateTime(ptDate.atTime(ltime));
				 	} catch (DateTimeParseException e) {
				 		throw new Exception(MessageFormat.format(Messages.CSVImportConfiguration_9, new Object[]{counter, row[timeColumn]}), e);
				 	}
				 
				 
				 	if(idColumn != -1){
					 	curWP.setId(row[idColumn].strip());
				 	}else{
				 		//could put this back if we want to show ID's 1 through # of points in the select your points screen.				 		
				 		//curWP.setId(maxId + 1);
				 		//maxId++;
				 		curWP.setId("-1");
				 	}
				 	
				 	if(commentColumn != -1){
					 	curWP.setComment(row[commentColumn]);
				 	}
				 
				 	allPoints.add(curWP);
				}
			}
		}catch (Exception e) {
			throw new Exception(Messages.CSVImportConfiguration_12 + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
		}
				
		return allPoints;
	}


	public int getXColumn() {
		return XColumn;
	}


	public void setXColumn(int xColumn) {
		XColumn = xColumn;
	}


	public int getYColumn() {
		return YColumn;
	}


	public void setYColumn(int yColumn) {
		YColumn = yColumn;
	}


	public int getDateColumn() {
		return dateColumn;
	}


	public void setDateColumn(int dateColumn) {
		this.dateColumn = dateColumn;
	}


	public int getTimeColumn() {
		return timeColumn;
	}


	public void setTimeColumn(int timeColumn) {
		this.timeColumn = timeColumn;
	}


	public int getCommentsColumn() {
		return commentColumn;
	}


	public void setCommentsColumn(int commentsColumn) {
		this.commentColumn = commentsColumn;
	}


	public int getIdColumn() {
		return idColumn;
	}


	public void setIdColumn(int idColumn) {
		this.idColumn = idColumn;
	}


	public String getFilename() {
		return filename;
	}
	public void setFileName(String file) {
		this.filename = file;
	}


	public String getDateFormat() {
		return dateFormat;
	}


	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}


	public void setProjection(Projection projection) throws FactoryException {
		this.sourceCrs = ReprojectUtils.stringToCrs(projection.getDefinition());
	}


	public void setSkipHeaders(boolean skipHeaders) {
		this.skipHeaders = skipHeaders;
	}

}
