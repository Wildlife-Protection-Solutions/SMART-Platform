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
import java.sql.Time;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
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
	
	private Projection projection;
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
	public List<Waypoint> getWaypoints(IProgressMonitor monitor, Date singleDay) throws Exception {
	
		GeometryFactory gf = new GeometryFactory();
		List<Waypoint> allPoints = new ArrayList<Waypoint>();
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		
		Date day0 = null;
		if( singleDay != null){
			day0 = SmartUtils.getDatePart(singleDay, false);
		}
		
		try{
			CSVReader reader = new CSVReader(new FileReader(filename), getDelimiter() );

			int counter = 0;
			if(skipHeaders){
				counter = 1;
				reader.readNext();	//skip header
			}
			
			String[] row;
			while((row = reader.readNext()) != null){
				counter++;
				Date ptDate = null; 
				if (row.length == 1 && row[0].trim().length() == 0){
					//this is a blank line; skip it
					break;
				}

				try {
					ptDate = sdf.parse( row[dateColumn].replaceAll("\\s+","") ); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (ParseException e) {
					throw new Exception(MessageFormat.format(Messages.CSVImportConfiguration_2, new Object[]{counter, row[dateColumn]}), e); 
				}
				
				Date day1 =SmartUtils.getDatePart(ptDate, false);
				if(singleDay == null ||  day0.equals(day1)){
					Waypoint curWP = new Waypoint();
					//reproject
					CoordinateReferenceSystem sourceCrs = projection.getCrs();
					Point point = gf.createPoint(new Coordinate(Double.parseDouble( row[XColumn].replaceAll("\\s+","")), Double.parseDouble( row[YColumn].replaceAll("\\s+","") ))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					Point p = (Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));

					if(p.getX() > 180 || p.getX() < -180){
						throw new Exception(MessageFormat.format(Messages.CSVImportConfiguration_1, new Object[]{counter, row[XColumn]}));
					}
					if(p.getY() > 90 || p.getY() < -90){
						throw new Exception(MessageFormat.format(Messages.CSVImportConfiguration_0, new Object[]{counter, row[YColumn]}));
					}
				 	curWP.setX(p.getX());
				 	curWP.setY(p.getY());

				 	try {
				 		String strTime = row[timeColumn].replaceAll("\\s+",""); //$NON-NLS-1$ //$NON-NLS-2$
				 		SimpleDateFormat format;
				 		String seconds;
				 		int minute_break = strTime.indexOf(":");  //$NON-NLS-1$
				 		if(strTime.length() > (minute_break + 3)){
				 			if( strTime.charAt(minute_break + 3) == ':'){
				 				seconds = ":ss"; //$NON-NLS-1$
				 			}else{
				 				seconds = ""; //$NON-NLS-1$
				 			}
				 		}else{
				 			seconds = ""; //$NON-NLS-1$
				 		}
				 		if(strTime.contains("+") || strTime.contains("-")){ //$NON-NLS-1$ //$NON-NLS-2$
				 			format = new SimpleDateFormat(dateFormat + " HH:mm" + seconds + "z"); //$NON-NLS-1$ //$NON-NLS-2$
				 		}else if(strTime.contains("AM") || strTime.contains("PM") || strTime.contains("am") || strTime.contains("pm")){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				 			format = new SimpleDateFormat(dateFormat + " hh:mm" + seconds + "a"); //$NON-NLS-1$ //$NON-NLS-2$
				 		}else{
				 			format = new SimpleDateFormat(dateFormat + " HH:mm" + seconds); //$NON-NLS-1$
				 		}
					 	Date dateTime = new Date(); 
					 	dateTime.setTime(ptDate.getTime());
					 	dateTime = format.parse( row[dateColumn].replaceAll("\\s+","") + " " + strTime ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					 	Time time = new Time(dateTime.getTime());
					 	curWP.setDateTime(SmartUtils.combineDateTime(ptDate, time));
				 	} catch (ParseException e) {
				 		throw new Exception(MessageFormat.format(Messages.CSVImportConfiguration_9, new Object[]{counter, row[timeColumn]}), e);
				 	}
				 
				 
				 	if(idColumn != -1){
					 	try {
						 	curWP.setId(Integer.parseInt( (row[idColumn].replaceAll("\\s+","")) )); //$NON-NLS-1$ //$NON-NLS-2$
					 	} catch (NumberFormatException e) {
					 		ObservationPlugIn.displayLog(MessageFormat.format(Messages.CSVImportConfiguration_11, new Object[]{counter, row[idColumn]}), e );   
					 		//curWP.setId(maxId + 1);
					 		//maxId++;
					 		curWP.setId(-1);
					 	}
				 	}else{
				 		//could put this back if we want to show ID's 1 through # of points in the select your points screen.				 		
				 		//curWP.setId(maxId + 1);
				 		//maxId++;
				 		curWP.setId(-1);
				 	}
				 	
				 	if(commentColumn != -1){
					 	curWP.setComment(row[commentColumn]);
				 	}
				 
				 	allPoints.add(curWP);
				}
				counter++;
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


	public void setProjection(Projection projection) {
		this.projection = projection;
	}


	public void setSkipHeaders(boolean skipHeaders) {
		this.skipHeaders = skipHeaders;
	}

}
