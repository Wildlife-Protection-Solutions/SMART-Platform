package org.wcs.smart.patrol.internal.ui.importwp.csv;

import java.io.FileReader;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import au.com.bytecode.opencsv.CSVReader;

public class CSVImportConfiguration {

	private static final GeometryFactory gf = new GeometryFactory();
	private static String filename; //the csv filename
	
	private int XColumn = -1;
	private int YColumn = -1;
	private int DateColumn = -1;
	private int TimeColumn = -1;
	private int CommentsColumn = -1;
	private int IdColumn = -1;
	
	private Projection projection;

	private String DateFormat;
	
	private boolean skipHeaders;
	private int maxId = 0;
	
	public List<Waypoint> getWaypoints(IProgressMonitor monitor, Date singleDay) {
		List<Waypoint> allPoints = new ArrayList<Waypoint>();
		try{
			CSVReader reader = new CSVReader(new FileReader(filename) );
			List<String[]> csvData = reader.readAll();
			int counter; 
			if(skipHeaders){
				counter = 1;
			}else{
				counter = 0;
			}
			
			String[] row;
			while(counter < csvData.size()-1){
				Date ptDate = null; 
				row = csvData.get(counter);
				
				if(row.length <4){
					break;//sometimes files will have blank rows at the end etc
				}
				SimpleDateFormat sdf = new SimpleDateFormat(DateFormat);
				try {
					ptDate = sdf.parse( row[DateColumn].replaceAll("\\s+","") );
				} catch (ParseException e) {
				 	SmartPatrolPlugIn.displayLog("Error Parsing Date in row: " + counter + "(" + row[DateColumn] + ")", e ); 
				}
				Date day0 = new Date(0);
				if( singleDay != null){
					day0 = SmartUtils.getDatePart(singleDay, false);
				}
				Date day1 =SmartUtils.getDatePart(ptDate, false);
				if(singleDay == null ||  day0.equals(day1)){
				 
					Waypoint curWP = new Waypoint();
					//reproject

					CoordinateReferenceSystem sourceCrs = projection.getCrs();
					Point point = gf.createPoint(new Coordinate(Double.parseDouble( row[XColumn].replaceAll("\\s+","")), Double.parseDouble( row[YColumn].replaceAll("\\s+","") )));
					Point p = (Point) JTS.transform(point, CRS.findMathTransform(sourceCrs, SmartDB.DATABASE_CRS));

					
				 	curWP.setX(p.getX() );
				 	curWP.setY(p.getY());
				 	curWP.setImportedDate(ptDate);

				 	try {
					 	SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
					 	Date dateTime = format.parse( row[TimeColumn].replaceAll("\\s+","") );
					 	Time time = new Time(dateTime.getTime());
					 	curWP.setTime( time);
				 	} catch (ParseException e) {
				 		SmartPatrolPlugIn.displayLog("could not parse date on row" + counter + "(" + row[TimeColumn].replaceAll("\\s+","") + ").", e );
				 	}
				 
				 
				 	if(IdColumn != -1){
					 	try {
						 	curWP.setId(Integer.parseInt( (row[IdColumn].replaceAll("\\s+","")) ));
					 	} catch (NumberFormatException e) {
					 		SmartPatrolPlugIn.displayLog("Alls waypoint IDs must be integers, ignoring ID in row: " + counter + "(" + row[IdColumn] + ")", e ); 
					 	}
				 	}else{
				 		curWP.setId(maxId + 1);
				 		maxId++;
				 	}
				 	if(CommentsColumn != -1){
					 	curWP.setComment(row[CommentsColumn]);
				 	}
				 

				 	allPoints.add(curWP);
				}
				counter++;
			}
		}catch (Exception e) {
			SmartPatrolPlugIn.displayLog("Error parsing selected CSV file, confirm the file is valid and try again.", e);
			return null;
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
		return DateColumn;
	}


	public void setDateColumn(int dateColumn) {
		DateColumn = dateColumn;
	}


	public int getTimeColumn() {
		return TimeColumn;
	}


	public void setTimeColumn(int timeColumn) {
		TimeColumn = timeColumn;
	}


	public int getCommentsColumn() {
		return CommentsColumn;
	}


	public void setCommentsColumn(int commentsColumn) {
		CommentsColumn = commentsColumn;
	}


	public int getIdColumn() {
		return IdColumn;
	}


	public void setIdColumn(int idColumn) {
		IdColumn = idColumn;
	}


	public void setFileName(String file) {
		this.filename = file;
	}


	public String getDateFormat() {
		return DateFormat;
	}


	public void setDateFormat(String dateFormat) {
		DateFormat = dateFormat;
	}


	public void setProjection(Projection projection) {
		this.projection = projection;
	}


	public void setSkipHeaders(boolean skipHeaders) {
		this.skipHeaders = skipHeaders;
	}

	public void setMaxId(int max){
		this.maxId = max;
	}
}
