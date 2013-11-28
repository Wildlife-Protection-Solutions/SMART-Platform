package org.wcs.smart.datamodelmatcher.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.internal.ca.datamodel.xml.XmlSmartDataModelManager;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;
import org.wcs.smart.mist.dataReader.MistDatabase;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;


public class MatchSession {

	private Shell shell;
	
	private String saveLocation;
	private String mistLocation;
	private String smartXmlLocation;
	
	private DataModel smartDataModel;
	
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
	
	public void loadRows() throws Exception{
		rows = new ArrayList<MatchRow>();

	    File mistFile = new File(mistLocation);
	    Connection c = MistDatabase.getConnection(mistFile.getAbsolutePath());
		ResultSet rs = c
				.createStatement()
				.executeQuery(
						"SELECT DISTINCT LO.OBSERVATION_GROUP, GPO.OBSERVATION, GPO.OBSERVATION_CODE, LRC.ITEM, LRC1.ITEM AS SUBCODE1, LRC2.ITEM AS SUBCODE2, LRC3.ITEM AS SUBCODE3, LRC4.ITEM AS SUBCODE4, LRC5.ITEM AS SUBCODE5 FROM GROUND_PATROL_OBSERVATIONS GPO LEFT JOIN OBSERVATION_REMARKS OBR ON OBR.OBS_REMARKS_RELATION = GPO.OBS_REMARKS_RELATION LEFT JOIN LK_REMARKS_CURRENT LRC ON LRC.OBSERVATION_RELATION = GPO.OBSERVATION_RELATION LEFT JOIN LK_REMARKS_CURRENT LRC1 ON LRC1.OBS_REMARKS = LRC.PARENT_ITEM LEFT JOIN LK_REMARKS_CURRENT LRC2 ON LRC2.OBS_REMARKS = LRC1.PARENT_ITEM LEFT JOIN LK_REMARKS_CURRENT LRC3 ON LRC3.OBS_REMARKS = LRC2.PARENT_ITEM LEFT JOIN LK_REMARKS_CURRENT LRC4 ON LRC4.OBS_REMARKS = LRC3.PARENT_ITEM LEFT JOIN LK_REMARKS_CURRENT LRC5 ON LRC5.OBS_REMARKS = LRC4.PARENT_ITEM LEFT JOIN LK_OBS_CODES LOC ON GPO.OBSERVATION_RELATION = LOC.OBSERVATION_RELATION LEFT JOIN LK_OBSERVATIONS LO ON LO.OBS_CODE_RELATION = LOC.OBS_CODE_RELATION WHERE  (LRC.OBS_REMARKS = OBR.OBS_REMARKS OR (LRC.OBS_REMARKS IS NULL AND OBR.OBS_REMARKS IS NULL)) ");
		while (rs.next()) {
			rows.add(new MatchRow(false, new MistItem(rs.getString(1) , rs.getString(2) , rs.getString(3) , rs.getString(4) , rs.getString(5) , rs.getString(6) , rs.getString(7) , rs.getString(8) , rs.getString(9)), new SmartItem("") ) );
		}

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
				String[] rowStrings = { row.getMistItem().getText(), row.getSmartItem().getText(), row.getMistItem().getCat1() , row.getMistItem().getCat2() , row.getMistItem().getCat3() , row.getMistItem().getCat4() , row.getMistItem().getCat5() , row.getMistItem().getCat6() , row.getMistItem().getCat7() , row.getMistItem().getCat8() , row.getMistItem().getCat9()};
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
			CSVReader reader = new CSVReader(new FileReader(saveLocation),','); 
		
			String[] header = reader.readNext();
			smartXmlLocation = header[0];
			String[] line = reader.readNext();
		
			while ( line != null){
				MatchRow matchRow = new MatchRow();
				matchRow.setMistItem(line[0]);
				matchRow.getMistItem().setCat1(line[2]);
				matchRow.getMistItem().setCat2(line[3]);
				matchRow.getMistItem().setCat3(line[4]);
				matchRow.getMistItem().setCat4(line[5]);
				matchRow.getMistItem().setCat5(line[6]);
				matchRow.getMistItem().setCat6(line[7]);
				matchRow.getMistItem().setCat7(line[8]);
				matchRow.getMistItem().setCat8(line[9]);
				matchRow.getMistItem().setCat9(line[10]);
				
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
	
	
	public void loadSmartDataModel() throws JAXBException, ParseException, IOException{
		File xmlFile = new File(smartXmlLocation);
		FileInputStream is = new FileInputStream(xmlFile);
		this.smartDataModel = XmlSmartDataModelManager.readDataModel(is);
	}
	
	public DataModel getXmlDataModel(){
		return smartDataModel;
	}
}
