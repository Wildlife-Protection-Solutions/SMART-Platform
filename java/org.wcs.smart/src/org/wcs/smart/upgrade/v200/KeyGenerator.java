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
package org.wcs.smart.upgrade.v200;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Key generator for generating keys for given tables from names.
 * Tests on teams, mandates, and transport types.
 * 
 * @author Emily
 *
 */
public class KeyGenerator {
	private static final String INVALID_START_CHARS_KEY_PATTERN = "[^a-z]+"; 
	
	public void generateKeys(Connection c, String tableName) throws Exception{
		
		String sql = "SELECT a.uuid, b.value, a.ca_uuid from " + tableName + 
			" a LEFT JOIN ( smart.i18n_label b JOIN smart.LANGUAGE c ON b.language_uuid = c.uuid and c.isdefault ) " +
			"on a.uuid = b.element_uuid";
		

		ResultSet rs = c.createStatement().executeQuery(sql);
		
		ArrayList<Object[]> keys = new ArrayList<Object[]>();
		HashMap<String,HashSet<String>> usedKeys = new HashMap<String,HashSet<String>>();
		
		
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String name = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			HashSet<String> existingKeys = usedKeys.get(Arrays.toString(cauuid));
			if (existingKeys == null){
				existingKeys = new HashSet<String>();
				usedKeys.put(Arrays.toString(cauuid), existingKeys);
			}
			if (name == null){
				name = tableName;
			}
			
			String key = generateKey(name, existingKeys);
			
			existingKeys.add(key);
			keys.add(new Object[]{uuid, key});
		}

		
		PreparedStatement ps = c.prepareStatement("UPDATE " + tableName + " set keyid = ? where uuid = ?");
		for (Object[] updates : keys){
			ps.setString(1, (String)updates[1]);
			ps.setBytes(2, (byte[])updates[0]);
			ps.execute();
		}
		
		c.commit();
	}
	
	private static String generateKey(String name, HashSet<String> usedkeys){

		String raw = name.toLowerCase().replaceAll("[^a-z0-9_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (raw.isEmpty()){
			raw = "key";
		}
		
		//remove _ and 1 
		while(raw.length() > 0 && Pattern.matches(INVALID_START_CHARS_KEY_PATTERN, raw.subSequence(0, 1))){
			raw = raw.replaceFirst(INVALID_START_CHARS_KEY_PATTERN, "");
		}
		if(raw.isEmpty()){
			raw = "key";
		}
		
		String temp = raw;
		int i = 1;
		while(usedkeys.contains(temp)){
			temp = raw + i;
			i++;
		}
		raw = temp;
		return raw;
	}
	
	private static Connection getConnection(File databaseFile) throws Exception{
		String driver = "org.apache.derby.jdbc.EmbeddedDriver"; 
		Class.forName(driver).newInstance();
		
		String queryString = "jdbc:derby:" + databaseFile.getAbsolutePath() + ";user=smart_admin;password=smart_derby";
		
		Connection c = DriverManager.getConnection(queryString);
		c.setAutoCommit(false);
		return c;
	}
	
//	public static void main(String args[]) throws Exception{
//		File dbFile = new File("C:\\data\\SMART\\Source\\Version1\\trunk\\source\\java\\org.wcs.smart\\data\\database\\smartdb");
//		Connection c = getConnection(dbFile);
//		
//		KeyGenerator kg = new KeyGenerator();
//		kg.generateKeys(c, "smart.patrol_mandate");
//		kg.generateKeys(c, "smart.team");
//		kg.generateKeys(c, "smart.patrol_transport");
//		
//	}
	
}
