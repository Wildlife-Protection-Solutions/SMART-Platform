package org.wcs.smart.db;

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
		

		ArrayList<Object[]> keys = new ArrayList<Object[]>();
		HashMap<String,HashSet<String>> usedKeys = new HashMap<String,HashSet<String>>();
		try(ResultSet rs = c.createStatement().executeQuery(sql)){
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
		}

		try(PreparedStatement ps = c.prepareStatement("UPDATE " + tableName + " set keyid = ? where uuid = ?")){
			for (Object[] updates : keys){
				ps.setString(1, (String)updates[1]);
				ps.setBytes(2, (byte[])updates[0]);
				ps.execute();
			}
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
	
	public static void main(String args[]) throws Exception{
		File dbFile = new File("D:\\SMART\\Source\\trunk\\source\\java\\org.wcs.smart\\data\\database\\smartdb");
		Connection c = getConnection(dbFile);
		
		KeyGenerator kg = new KeyGenerator();
		kg.generateKeys(c, "smart.patrol_mandate");
		kg.generateKeys(c, "smart.team");
		kg.generateKeys(c, "smart.patrol_transport");
		
	}
	
	private static void test(){
		String[] keys = new String[]{ "1this is Me I am.#@$%@$#%!$#_@%^ASDF @!$#%",
				"1234",
				"I Am . Big",
				"@#$%&",
				"_an underscore",
				"min",
				"max",
				"and",
				"i8 this and that"
		};
		for (String key : keys){
			String newKey = KeyGenerator.generateKey(key, new HashSet<String>());
			System.out.println(key + " : " + newKey);
		}
		
		System.out.println(" ------------------------------------------------ ");
		HashSet<String> ukeys = new HashSet<String>();
		ukeys.add("abc");
		ukeys.add("abc1");
		
		keys = new String[]{ "abc",
				"1abc",
		};
		for (String key : keys){
			String newKey = KeyGenerator.generateKey(key, ukeys);
			System.out.println(key + " : " + newKey);
		}
	}
}
