package org.wcs.smart.upgrade.v112;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class NameFixer {

	public void fixNames(Connection c) throws Exception{
		String sql = "SELECT element_uuid, language_uuid, value from smart.i18n_label";
		
		ArrayList<Object[]> toUpdate = new ArrayList<Object[]>();
		ResultSet rs = c.createStatement().executeQuery(sql);
		while(rs.next()){
			byte[] element = rs.getBytes(1);
			byte[] language = rs.getBytes(2);
			String name = rs.getString(3);
			
			if (!FixDataModel.validateName(name)){
				String newName = FixDataModel.fixName(name);
				if (!FixDataModel.validateName(newName)){
					throw new Exception("The name " + name + " is invalid and cannot be fixed.");
				}else{
					toUpdate.add(new Object[]{element,language,newName});
				}
			}
		}
		
		PreparedStatement ps = c.prepareStatement("UPDATE smart.i18n_label set value = ? where element_uuid = ? and language_uuid = ? ");
		for (Object[] x : toUpdate){
			ps.setString(1, (String)x[2]);
			ps.setBytes(2, (byte[])x[0]);
			ps.setBytes(3,(byte[]) x[1]);
			
			ps.execute();
		}
	}
}
