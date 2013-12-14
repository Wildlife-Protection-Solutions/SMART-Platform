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
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
	
	//Writes the Session to the saveLocation file and asks for a filename if one doesn't already exist.
	public void save(){
		
		//get a save location if we don't have one already
		if( saveLocation == null){
			this.saveLocation = CreateNewSessionFile();
		}
		
		
		try{
			CSVWriter sessionWriter = new CSVWriter(new FileWriter(saveLocation), ',','\0','\0'); 

			//write out the basic session file information.
			String output = smartXmlLocation.replace("\\","\\\\");
			String[] header = {output,"","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","-"};
			//ensure the files always looks like it has enough columns in it by putting this "-" in the 36th column, otherwise opening and closing in Excel will break loading many files
			
			sessionWriter.writeNext(header);
			
			//write out all the row-matching data
			
			for(MatchRow row: rows){
				SmartItem s = row.getSmartItem();
				String b1 = "TRUE"; 
				if(!s.getB1()){
					b1 = "FALSE";
				}
				String b2 = "TRUE"; 
				if(!s.getB2()){
					b2 = "FALSE";
				}
				String b3 = "TRUE"; 
				if(!s.getB3()){
					b3 = "FALSE";
				}
				String b4 = "TRUE"; 
				if(!s.getB4()){
					b4 = "FALSE";
				}
				String b5 = "TRUE"; 
				if(!s.getB5()){
					b5 = "FALSE";
				}

				String[] rowStrings = { row.getMistItem().getText(), row.getSmartItem().getText(), row.getMistItem().getCat1() , row.getMistItem().getCat2() , row.getMistItem().getCat3() , row.getMistItem().getCat4() , row.getMistItem().getCat5() , row.getMistItem().getCat6() , row.getMistItem().getCat7() , row.getMistItem().getCat8() , row.getMistItem().getCat9(),
									s.getCategoryKey(), s.getAttr1key(), s.getAttr2key(), s.getAttr3key(), s.getAttr4key(), s.getAttr5key(),b1 , s.getText1(), s.getList1(), s.getTree1()
									, b2, s.getText2(), s.getList2(), s.getTree2()
									, b3,  s.getText3(), s.getList3(), s.getTree3()
									, b4, s.getText4(), s.getList4(), s.getTree4()
									, b5, s.getText5(), s.getList5(), s.getTree5()};
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
		int x=0;
		try{
			CSVReader reader = new CSVReader(new FileReader(saveLocation),','); 
		
			String[] header = reader.readNext();
			smartXmlLocation = header[0];
			loadSmartDataModel();
			
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
					String[] list = {line[11], line[12], line[13], line[14], line[15], line[16], line[17], line[18], line[19], line[20], line[21], line[22]
							, line[23], line[24], line[25], line[26], line[27], line[28], line[29], line[30], line[31], line[32], line[33], line[34], line[35], line[36]};

					matchRow.setSmartAttributes(list);
					matchRow.setSmartItem(line[1]);
				}
				rows.add(matchRow);
				line = reader.readNext();
				x++;
			}
			reader.close();
		}catch (Exception e){
			return e + " ; Invalid Match Session File, line: '" + x + " of file: " + saveLocation.toString() + "'";
		}
		return null;
	}
	
	
	public void loadSmartDataModel() throws JAXBException, ParseException, IOException{
		File xmlFile = new File(smartXmlLocation);
		FileInputStream is = new FileInputStream(xmlFile);

		//read file directly instead of using the XmlSmartDataModelManager
		//because that manager uses classes which require hibernate and
		//we don't have to have to include hibernate in our build
		//this.smartDataModel = XmlSmartDataModelManager.readDataModel(is);
		JAXBContext context = JAXBContext.newInstance("org.wcs.smart.internal.ca.datamodel.xml.generate");
		Unmarshaller un = context.createUnmarshaller();	
		Object o = un.unmarshal(is);
		this.smartDataModel = (DataModel) o;
		
	}
	
	public DataModel getXmlDataModel(){
		return smartDataModel;
	}

	public void sort(int direction, int column) {
		MatchRowSorter sorter = new MatchRowSorter();
		sorter.setDirection(direction);
		sorter.setCol(column);
		Collections.sort(rows, sorter);
	}

	public void mergeRows() throws Exception{
		File mistFile = new File(mistLocation);
	    Connection c = MistDatabase.getConnection(mistFile.getAbsolutePath());
		ResultSet rs = c
				.createStatement()
				.executeQuery(
						"SELECT DISTINCT LO.OBSERVATION_GROUP, GPO.OBSERVATION, GPO.OBSERVATION_CODE, LRC.ITEM, LRC1.ITEM AS SUBCODE1, LRC2.ITEM AS SUBCODE2, LRC3.ITEM AS SUBCODE3, LRC4.ITEM AS SUBCODE4, LRC5.ITEM AS SUBCODE5 FROM GROUND_PATROL_OBSERVATIONS GPO LEFT JOIN OBSERVATION_REMARKS OBR ON OBR.OBS_REMARKS_RELATION = GPO.OBS_REMARKS_RELATION LEFT JOIN LK_REMARKS_CURRENT LRC ON LRC.OBSERVATION_RELATION = GPO.OBSERVATION_RELATION LEFT JOIN LK_REMARKS_CURRENT LRC1 ON LRC1.OBS_REMARKS = LRC.PARENT_ITEM LEFT JOIN LK_REMARKS_CURRENT LRC2 ON LRC2.OBS_REMARKS = LRC1.PARENT_ITEM LEFT JOIN LK_REMARKS_CURRENT LRC3 ON LRC3.OBS_REMARKS = LRC2.PARENT_ITEM LEFT JOIN LK_REMARKS_CURRENT LRC4 ON LRC4.OBS_REMARKS = LRC3.PARENT_ITEM LEFT JOIN LK_REMARKS_CURRENT LRC5 ON LRC5.OBS_REMARKS = LRC4.PARENT_ITEM LEFT JOIN LK_OBS_CODES LOC ON GPO.OBSERVATION_RELATION = LOC.OBSERVATION_RELATION LEFT JOIN LK_OBSERVATIONS LO ON LO.OBS_CODE_RELATION = LOC.OBS_CODE_RELATION WHERE  (LRC.OBS_REMARKS = OBR.OBS_REMARKS OR (LRC.OBS_REMARKS IS NULL AND OBR.OBS_REMARKS IS NULL)) ");
		while (rs.next()) {
			MistItem mist = new MistItem(rs.getString(1) , rs.getString(2) , rs.getString(3) , rs.getString(4) , rs.getString(5) , rs.getString(6) , rs.getString(7) , rs.getString(8) , rs.getString(9));
			boolean alreadyExists = false;
			for(MatchRow r: rows){
				MistItem m = r.getMistItem();
				if(m.equalTo(mist)){
					alreadyExists = true;
					break;
				}
			}
			if(!alreadyExists){
				rows.add(new MatchRow(false, mist , new SmartItem("") ) );
			}
		}

		
	}
	
	
}


