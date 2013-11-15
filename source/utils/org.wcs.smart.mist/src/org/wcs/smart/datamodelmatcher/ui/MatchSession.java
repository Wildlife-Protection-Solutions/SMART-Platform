package org.wcs.smart.datamodelmatcher.ui;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;



public class MatchSession {

	private Shell shell;
	
	private String saveLocation;
	private String mistLocation;
	private String smartXmlLocation;
	
	private List<MatchRow> rows;
	
	public MatchSession(Shell shell){
		this.shell = shell;
		rows = new ArrayList<MatchRow>();
	}
	
	public void setSaveLocation(String s){
		this.saveLocation = s;
	}
	
	public String getSaveLocation(){
		return this.saveLocation;
	}
	
	public void loadRows(){
		rows = new ArrayList<MatchRow>();
	    rows.add(new MatchRow(false, new MistItem("humanactivities.land.cleared.peopleconfronted.localpeople."), new SmartItem("") ));
	    rows.add(new MatchRow(false, new MistItem("blah.land.cleared.flah.foobar."), new SmartItem("Blah.Land.Bar") ));
	    
		//TODO load from the database query
		/*
	    		try{
	    			Connection c = MistDatabase.getConnection(dbFile.getAbsolutePath());
	    		}catch(Exception exception){
	    			 MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
	    			  messageBox.setMessage("MIST database connection could not be established." + exception);
	    			  int result = messageBox.open();
	    		}

		try{
		 
			ResultSet rs = c
				.createStatement()
				.executeQuery(
						"SELECT distinct D.DEPARTMENT_NAME, DU.UNIT_NAME FROM DEPARTMENT_UNITS DU LEFT JOIN DEPARTMENTS D ON D.DEPARTMENT_ID = DU.DEPARTMENT_ID "); //$NON-NLS-1$
			while (rs.next()) {
			    String[] entries = {rs.getString(1), rs.getString(2)};
			    ms.addRow()...
			}
		}finally{
			
		}
		*/
		
		
		
	}
	public List<MatchRow> getRows(){
		return rows;
	}
	
	//Writes the Session to the saveLocation file and asks for a filename is one doens't already exist.
	public void save(){
		
		//get a save location and confirm file overwrite etc, probably do this later.
		if( saveLocation == null){
			this.saveLocation = CreateNewSessionFile();
		}
		
		
		try{
			CSVWriter sessionWriter = new CSVWriter(new FileWriter(saveLocation), ','); 
			
			//write out the basic session file information.
			String[] header = {smartXmlLocation}; 
			sessionWriter.writeNext(header);
			
			//write out all the row-matching data
			for(MatchRow row: rows){
				String[] rowStrings = { row.getMistItem().getText(), row.getSmartItem().getText()};
				sessionWriter.writeNext(rowStrings);
			}
			sessionWriter.close();
		}catch(Exception exception){
			 MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR);
			 messageBox.setMessage("Save Failed. Could not write to specified Session File: " + exception);
			 messageBox.open();
		}
		
	}

	public String getMistLocation() {
		return mistLocation;
	}

	public void setMistLocation(String mistLocation) {
		this.mistLocation = mistLocation;
	}

	public String getSmartXmlLocation() {
		return smartXmlLocation;
	}

	public void setSmartXmlLocation(String smartXmlLocation) {
		this.smartXmlLocation = smartXmlLocation;
	}

	public Integer getNumMatched() {
		int matched =0;
		for(MatchRow row: rows){
			if(row.getMatched()){
				matched++;
			}
		}
		return matched;
	}

	public Integer getNumTotal() {
		return rows.size();
	}
	
	

	protected String CreateNewSessionFile() {
			//ask for save location
			FileDialog dialog = new FileDialog (shell, SWT.SAVE);
			String [] filterNames = new String [] {"Session File"};
			String [] filterExtensions = new String [] {"*.csv"};
			String filterPath = "/";
			dialog.setFilterNames (filterNames);
			dialog.setFilterExtensions (filterExtensions);
			dialog.setFilterPath (filterPath);
			dialog.setFileName ("SessionName");
			dialog.setText("Save Session As:");
			return dialog.open();
	}
	
	public String loadSessionFromFile(){
		try{
			CSVReader reader = new CSVReader(new FileReader(saveLocation),','); //$NON-NLS-1$
		
			String[] header = reader.readNext();
			smartXmlLocation = header[0];
			String[] line = reader.readNext();
			while ( line != null){
				MatchRow matchRow = new MatchRow();
				matchRow.setMistItem(line[0]);
				if(!line[1].equals("") ){
					matchRow.setSmartItem(line[1]);
				}
				rows.add(matchRow);
				line = reader.readNext();
			}
			reader.close();
		}catch (Exception e){
			return e + " ; Match Session File read error on file: '" + saveLocation.toString() + "'";
		}
		return null;
	}
	    
}
