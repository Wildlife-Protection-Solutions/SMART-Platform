package org.wcs.smart.datamodelmatcher.ui;

import java.io.FileWriter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import au.com.bytecode.opencsv.CSVWriter;



public class MatchSession {
	
	private Shell shell;
	
	private String saveLocation;
	private String mistLocation;
	private String smartXmlLocation;
	
	public void MatchSession(Shell shell){
		this.shell = shell;
	}
	
	public void setSaveLocation(String s){
		this.saveLocation = s;
	}
	
	public String getSaveLocation(){
		return this.saveLocation;
	}
	
	//Writes the Session to the saveLocation file.
	public void save(){
		//write out the basic session file information.
		try{
			CSVWriter sessionWriter = new CSVWriter(new FileWriter(saveLocation), ','); 
			String[] header = {mistLocation, smartXmlLocation, }; 
			sessionWriter.writeNext(header);
			sessionWriter.close();
		}catch(Exception exception){
			 MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR);
			 messageBox.setMessage("Could not write to specified Session File: " + exception);
			 int result = messageBox.open();
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
		//for(MatchRow row: rows){
		//	
		//}
		//TODO
		return new Integer(20);
	}

	public Integer getNumTotal() {
		//TODO
		return new Integer(50);
	}
}
