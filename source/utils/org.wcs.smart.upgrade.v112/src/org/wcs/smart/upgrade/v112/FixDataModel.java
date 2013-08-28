package org.wcs.smart.upgrade.v112;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.parser.internal.summary.SumQueryDefinition;



public class FixDataModel {
	/**
	 * Maximum length of the key identifier
	 */
	public static final int MAX_KEY_LENGTH = 128;
	/**
	 * Various pre-defined regex expressions
	 * for validating strings in smart.
	 */
	public static enum RegExLevel{
		/**
		 * Allows only letters and digits (a-Z and 0-9)
		 */
		ALLOWED_CHARS_SIMPLE_REGEX("[^\\p{L}\\p{M}\\p{Nd}-]"),  //\p{L} - letters \p{Nd} - digits	is a digit zero through nine //$NON-NLS-1$
		/**
		 * Allows chars, digits, - _ and :
		 */
		ALLOWED_CHARS_MED_REGEX("[^^\\p{L}\\p{M}\\p{Nd}-:_]" ), //$NON-NLS-1$
		/**
		 * Allows chars, digits, _ : & and spaces
		 */
		ALLOWED_CHARS_COMPLEX_REGEX("[^^\\p{L}\\p{M}\\p{Nd}- :_&'<>,\\(\\)\\.\\#;\\/]"); //$NON-NLS-1$
		
		public final String regex;
	
		RegExLevel(String regex){
			this.regex = regex;
		}
	}
	
	public static final String HKEY_SEPERATOR = "."; //$NON-NLS-1$

	private static final String VALID_DM_KEY_PATTERN = "[a-z]{1}[a-z0-9_]*"; //$NON-NLS-1$
	
	/*
	 * These are keywords that cannot be used as keys; for querying purposes.
	 */
	private static final String[] KEYWORDS = new String[]{"and", "or", "not", "contains", "notcontains", "equals"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	/**
	 * Maximum length of the name identifier
	 */
	public static final int MAX_NAME_LENGTH = 1024;
	
	/**
	 * Validates a data model object key.
	 * <p>Keys must not be empty, less than DmObject.MAX_KEY_LENGTH characters,
	 * and different from their siblings.</p>
	 * 
	 * @param key the key to validate.
	 * @param otherValues set of {@link DmObject} the key value must be different from
	 * @return <code>null</code> if the key is valid otherwise a string description of the error
	 */
	public static boolean validateKey(String key, Collection<KeyItem> otherValues){
		if (key == null || key.isEmpty()){
			return  false;
		}
		if (key.length() > MAX_KEY_LENGTH ){
			return false;
		}
		if (!key.matches(VALID_DM_KEY_PATTERN)){
			return false;
		}
		if (checkKeyExists(key, otherValues)){
			return false;
		}
		for (String keyword: KEYWORDS){
			if (keyword.equals(key)){
				return false;
			}
		}
		return true;
	}
	
	
	private static String[] prefixes = new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
	
	private static boolean isKeyword(String key){
		for (String keyword: KEYWORDS){
			if (keyword.equals(key)){
				return true;
			}
		}
		return false;
	}
	
	private static boolean containKey(String key,  Collection<KeyItem> otherValues){
		for (KeyItem item: otherValues){
			if (item.getCurrentKey().equals(key)){
				return true;
			}
		}
		return false;
	}
	public static String fixKey(String key, Collection<KeyItem> otherValues) throws Exception{
		if (key == null || key.isEmpty()){
			String newKey = "key";
			int i = 0;
			while (containKey(newKey, otherValues) && i < 100){
				newKey = "key" + i;
				i++;
			}
			if (containKey(newKey, otherValues)){
				throw new Exception("Key is blank and cannot be updated to keyN");
			}
			return key;
		}

		
		String newKey = key;
		
		if (!newKey.matches(VALID_DM_KEY_PATTERN) || isKeyword(newKey)){
			
			//replace any '.' with underscores
			newKey = newKey.replaceAll("\\.", "_");
			//remove any non-valid characters
			newKey = newKey.replaceAll("[^a-z0-9_]","");
			
			if (!newKey.matches(VALID_DM_KEY_PATTERN) || containKey(newKey, otherValues) || isKeyword(newKey)){
				int i = 1;
				String tmpKey = prefixes[0] + newKey;
				while(containKey(tmpKey, otherValues) && i < prefixes.length){
					tmpKey = prefixes[i] + newKey;
					i++;
				}

				if (containKey(tmpKey, otherValues)){
					throw new Exception("Key " + key + " is invalid and cannot be fixed.");
				}
				newKey = tmpKey;
			}
		}
		
		
		
		if (key.length() > MAX_KEY_LENGTH){
			String tmpKey = newKey.substring(0, MAX_KEY_LENGTH);
			int i = 0;
			while(containKey(tmpKey, otherValues) && i < 100){
				tmpKey = newKey.substring(0, MAX_KEY_LENGTH - String.valueOf(i).length()) + i;
				i++;
			}
			if (containKey(tmpKey, otherValues)){
				throw new Exception("Key " + key + " is too long and cannot be shortened.");
			}
			newKey = tmpKey;
		}
		
		return newKey;
	}
	
	
	private static boolean checkKeyExists(String key, Collection<KeyItem> otherValues){
		for (KeyItem it : otherValues){
			if (key.equals(it.getNewKey())){
				return true;
			}
		}
		return false;
	}
	
	public static boolean validateName(String name){
		if (!SmartUtils.isSimpleString(name.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, MAX_NAME_LENGTH,0)){
			return false;
		}
		return true;
	}
	public static String fixName(String name){
		return name.replaceAll(SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.regex, "");
	}
	
	
	public static void fixItems(HashMap<ByteWrapper, List<KeyItem>> items) throws Exception{
		for (Iterator<List<KeyItem>> iterator = items.values().iterator(); iterator.hasNext();) {
			List<KeyItem> keys = (List<KeyItem>) iterator.next();
			List<KeyItem> copyKeys = new ArrayList<KeyItem>();
			for (KeyItem k : keys){
				copyKeys.clear();
				copyKeys.addAll(keys);
				copyKeys.remove(k);
				if (!FixDataModel.validateKey(k.getOriginalKey(), copyKeys)){
					String newKey = FixDataModel.fixKey(k.getOriginalKey(), copyKeys);
					if (!FixDataModel.validateKey(newKey, copyKeys)){
						throw new Exception("Key " + k.getOriginalKey() + " is invalid and cannot be fixed.");
					}
					k.updateKey(newKey);
				}
				
				
				for (KeyItem.KeyName name : k.getNames()){
					if (!FixDataModel.validateName(name.getOldName())){
						String newName = FixDataModel.fixName(name.getOldName());
						if (!FixDataModel.validateName(newName)){
							throw new Exception("Name " + name.getOldName() + " is invalid and cannot be fixed.");
						}
						name.updateName(newName);
					}
				}
			}
		}
	}
	
	
	public static void updateQueries(List<KeyItem> keysUpdated, IDataProcessor processor, Connection c) throws Exception{
		/*------------------- Waypoint Queries -----------------------*/
		String sql = "SELECT uuid, query_filter, ca_uuid from smart.waypoint_query";
		ResultSet rs = c.createStatement().executeQuery(sql);
		List<Object[]> updatedQueries = new ArrayList<Object[]>();
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String query = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			if (query != null && query.length() > 0){
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				IFilter filter = parser.QueryFilter();
				boolean processed = false;
				
				for (KeyItem it : keysUpdated){
					if (Arrays.equals(cauuid, it.getCaUuid())){
						boolean b = processor.processFilter(filter,it.getOriginalKey(), it.getNewKey());
						processed = b || processed;
						
					}
				}
				if (processed){
					System.out.println("------------------");
					System.out.println(query);
					System.out.println(filter.asString());
					System.out.println("------------------");
					updatedQueries.add(new Object[]{uuid, filter.asString()});
				}
			}
		}
		rs.close();
		PreparedStatement ps = c.prepareStatement("UPDATE smart.waypoint_query set query_filter = ? where uuid = ?");
		for (Object[] x : updatedQueries){
			ps.setString(1, (String)x[1]);
			ps.setBytes(2, (byte[])x[0]);
			ps.execute();
		}
		ps.close();
		
		
		/*------------------- Patrol Queries -----------------------*/
		sql = "SELECT uuid, query_filter, ca_uuid from smart.patrol_Query";
		rs = c.createStatement().executeQuery(sql);
		updatedQueries = new ArrayList<Object[]>();
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String query = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			if (query != null && query.length() > 0){
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				IFilter filter = parser.QueryFilter();
				boolean processed = false;
				for (KeyItem it : keysUpdated){
					if (Arrays.equals(cauuid, it.getCaUuid())){
						boolean b = processor.processFilter(filter,it.getOriginalKey(), it.getNewKey());
						processed = b || processed;
					}
				}
				
				if (processed){
					System.out.println("------------------");
					System.out.println(query);
					System.out.println(filter.asString());
					System.out.println("------------------");
					updatedQueries.add(new Object[]{uuid, filter.asString()});
				}
			}
		}
		rs.close();
		ps = c.prepareStatement("UPDATE smart.patrol_query set query_filter = ? where uuid = ?");
		for (Object[] x : updatedQueries){
			ps.setString(1, (String)x[1]);
			ps.setBytes(2, (byte[])x[0]);
			ps.execute();
		}
		ps.close();
		
		/*------------------- Gridded Queries -----------------------*/
		sql = "SELECT uuid, query_def, ca_uuid from smart.gridded_query";
		rs = c.createStatement().executeQuery(sql);
		updatedQueries = new ArrayList<Object[]>();
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String query = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			if (query != null && query.length() > 0){
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				GridQueryDefinition gd = parser.GridQuery();
				
				
				IFilter rateFilter = gd.getRateFilter();
				IFilter valueFilter = gd.getValueFilter();
				IValueItem valueItem = gd.getValuePart();
				boolean processed = false;
				for (KeyItem it : keysUpdated){
					if (Arrays.equals(cauuid, it.getCaUuid())){
						boolean ba = processor.processFilter(rateFilter,it.getOriginalKey(), it.getNewKey());
						boolean bb = processor.processFilter(valueFilter,it.getOriginalKey(), it.getNewKey());
						boolean bc = processor.processValueItem(valueItem,it.getOriginalKey(), it.getNewKey());
						processed = ba || bb || bc || processed;
					}
				}
				
				if (processed){
					System.out.println("------------------");
					System.out.println(query);
					System.out.println(gd.asQuery());
					System.out.println("------------------");
					updatedQueries.add(new Object[]{uuid, gd.asQuery()});
				}
			}
		}
		rs.close();
		ps = c.prepareStatement("UPDATE smart.gridded_query set query_def = ? where uuid = ?");
		for (Object[] x : updatedQueries){
			ps.setString(1, (String)x[1]);
			ps.setBytes(2, (byte[])x[0]);
			ps.execute();
		}
		ps.close();
		
		
		/*------------------- Summary Queries -----------------------*/
		sql = "SELECT uuid, query_def, ca_uuid from smart.summary_query";
		rs = c.createStatement().executeQuery(sql);
		updatedQueries = new ArrayList<Object[]>();
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String query = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			if (query != null && query.length() > 0){
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				 SumQueryDefinition gd = parser.SumQuery();
				
				IFilter rateFilter = gd.getRateFilter();
				IFilter valueFilter = gd.getValueFilter();
				boolean processed = false;
				
				for (KeyItem it : keysUpdated){
					if (Arrays.equals(cauuid, it.getCaUuid())){
						boolean ba = processor.processFilter(rateFilter,it.getOriginalKey(), it.getNewKey());
						boolean bb = processor.processFilter(valueFilter,it.getOriginalKey(), it.getNewKey());
						processed = processed || ba || bb;
						for (IValueItem valueItem : gd.getValuePart().getValueItems()){
							boolean b = processor.processValueItem(valueItem,it.getOriginalKey(), it.getNewKey());
							processed = processed || b ;
						}
						
						for (IGroupBy groupBy : gd.getColumnGroupByPart().getGroupBys()){
							boolean b = processor.processGroupBy(groupBy,it.getOriginalKey(), it.getNewKey());
							processed = processed || b ;
						}
						for (IGroupBy groupBy : gd.getRowGroupByPart().getGroupBys()){
							boolean b = processor.processGroupBy(groupBy,it.getOriginalKey(), it.getNewKey());
							processed = processed || b ;
						}
					}
				}
				
				if (processed){
					System.out.println("------------------");
					System.out.println(query);
					System.out.println(gd.asQuery());
					System.out.println("------------------");
					updatedQueries.add(new Object[]{uuid, gd.asQuery()});
				}
			}
		}
		rs.close();
		ps = c.prepareStatement("UPDATE smart.summary_query set query_def = ? where uuid = ?");
		for (Object[] x : updatedQueries){
			ps.setString(1, (String)x[1]);
			ps.setBytes(2, (byte[])x[0]);
			ps.execute();
		}
		ps.close();
	}
	
	
	public static void updateQueries(HashMap<KeyItem, String> keysUpdated, IDataProcessor processor, Connection c) throws Exception{
		//load each query;  parse the query
		
		/*------------------- Waypoint Queries -----------------------*/
		String sql = "SELECT uuid, query_filter, ca_uuid from smart.waypoint_query";
		ResultSet rs = c.createStatement().executeQuery(sql);
		List<Object[]> updatedQueries = new ArrayList<Object[]>();
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String query = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			if (query != null && query.length() > 0){
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				IFilter filter = parser.QueryFilter();
				boolean processed = false;
				for (Entry<KeyItem,String> it : keysUpdated.entrySet()){
					if (Arrays.equals(it.getKey().getCaUuid(), cauuid)){
						boolean p = processor.processFilter(filter,it.getKey().getOriginalHkey(), it.getValue());
						processed = processed || p;
					}
				}
				if (processed){
					
					System.out.println("------------------");
					System.out.println(query);
					System.out.println(filter.asString());
					System.out.println("------------------");
					updatedQueries.add(new Object[]{uuid, filter.asString()});
				}
			}
		}
		rs.close();
		PreparedStatement ps = c.prepareStatement("UPDATE smart.waypoint_query set query_filter = ? where uuid = ?");
		for (Object[] x : updatedQueries){
			ps.setString(1, (String)x[1]);
			ps.setBytes(2, (byte[])x[0]);
			ps.execute();
		}
		ps.close();
		
		
		/*------------------- Patrol Queries -----------------------*/
		sql = "SELECT uuid, query_filter, ca_uuid from smart.patrol_Query";
		rs = c.createStatement().executeQuery(sql);
		updatedQueries = new ArrayList<Object[]>();
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String query = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			if (query != null && query.length() > 0){
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				IFilter filter = parser.QueryFilter();
				boolean processed = false;
				for (Entry<KeyItem,String> it : keysUpdated.entrySet()){
					if (Arrays.equals(it.getKey().getCaUuid(), cauuid)){
						boolean p = processor.processFilter(filter,it.getKey().getOriginalHkey(), it.getValue());
						processed = processed || p;
					}
				}
				if (processed){
					System.out.println("------------------");
					System.out.println(query);
					System.out.println(filter.asString());
					System.out.println("------------------");
					updatedQueries.add(new Object[]{uuid, filter.asString()});
				}
			}
		}
		rs.close();
		ps = c.prepareStatement("UPDATE smart.patrol_query set query_filter = ? where uuid = ?");
		for (Object[] x : updatedQueries){
			ps.setString(1, (String)x[1]);
			ps.setBytes(2, (byte[])x[0]);
			ps.execute();
		}
		ps.close();
		
		/*------------------- Gridded Queries -----------------------*/
		sql = "SELECT uuid, query_def,ca_uuid from smart.gridded_query";
		rs = c.createStatement().executeQuery(sql);
		updatedQueries = new ArrayList<Object[]>();
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String query = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			if (query != null && query.length() > 0){
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				GridQueryDefinition gd = parser.GridQuery();
				
				IFilter rateFilter = gd.getRateFilter();
				IFilter valueFilter = gd.getValueFilter();
				IValueItem valueItem = gd.getValuePart();
				boolean processed = false;
				for (Entry<KeyItem,String> it : keysUpdated.entrySet()){
					if (Arrays.equals(it.getKey().getCaUuid(), cauuid)){
						boolean ba = processor.processFilter(rateFilter,it.getKey().getOriginalHkey(), it.getValue());
						boolean bb = processor.processFilter(valueFilter,it.getKey().getOriginalHkey(), it.getValue());
						boolean bc = processor.processValueItem(valueItem,it.getKey().getOriginalHkey(), it.getValue());
						processed = processed || ba || bb || bc;
					}
				}
				
				if (processed){
					System.out.println("------------------");
					System.out.println(query);
					System.out.println(gd.asQuery());
					System.out.println("------------------");
					updatedQueries.add(new Object[]{uuid, gd.asQuery()});
				}
			}
		}
		rs.close();
		ps = c.prepareStatement("UPDATE smart.gridded_query set query_def = ? where uuid = ?");
		for (Object[] x : updatedQueries){
			ps.setString(1, (String)x[1]);
			ps.setBytes(2, (byte[])x[0]);
			ps.execute();
		}
		ps.close();
		
		
		/*------------------- Summary Queries -----------------------*/
		sql = "SELECT uuid, query_def, ca_uuid from smart.summary_query";
		rs = c.createStatement().executeQuery(sql);
		updatedQueries = new ArrayList<Object[]>();
		while(rs.next()){
			byte[] uuid = rs.getBytes(1);
			String query = rs.getString(2);
			byte[] cauuid = rs.getBytes(3);
			
			if (query != null && query.length() > 0){
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				SumQueryDefinition gd = parser.SumQuery();
				
				IFilter rateFilter = gd.getRateFilter();
				IFilter valueFilter = gd.getValueFilter();
				
				boolean processed = false;
				for (Entry<KeyItem,String> it : keysUpdated.entrySet()){
					if (Arrays.equals(it.getKey().getCaUuid(), cauuid)){
						boolean p = processor.processFilter(rateFilter,it.getKey().getOriginalHkey(), it.getValue());
						processed = processed ||  p;
						p = processor.processFilter(valueFilter,it.getKey().getOriginalHkey(), it.getValue());
						processed = processed ||  p;
						
						for (IValueItem valueItem : gd.getValuePart().getValueItems()){
							p = processor.processValueItem(valueItem,it.getKey().getOriginalHkey(), it.getValue());
							processed = processed ||  p;
						}
						for (IGroupBy groupBy : gd.getColumnGroupByPart().getGroupBys()){
							p = processor.processGroupBy(groupBy,it.getKey().getOriginalHkey(), it.getValue());
							processed = processed ||  p;
						}
						for (IGroupBy groupBy : gd.getRowGroupByPart().getGroupBys()){
							p = processor.processGroupBy(groupBy,it.getKey().getOriginalHkey(), it.getValue());
							processed = processed ||  p;
						}
					}
				}
				
				if (processed){
					System.out.println("------------------");
					System.out.println(query);
					System.out.println(gd.asQuery());
					System.out.println("------------------");
					updatedQueries.add(new Object[]{uuid, gd.asQuery()});
				}
			}
		}
		rs.close();
		ps = c.prepareStatement("UPDATE smart.summary_query set query_def = ? where uuid = ?");
		for (Object[] x : updatedQueries){
			ps.setString(1, (String)x[1]);
			ps.setBytes(2, (byte[])x[0]);
			ps.execute();
		}
		ps.close();
	}
}
