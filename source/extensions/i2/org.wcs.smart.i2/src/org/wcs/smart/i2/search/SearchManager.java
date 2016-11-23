/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.search;

import info.debatty.java.stringsimilarity.Levenshtein;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.util.UuidUtils;


public enum SearchManager {
	
	INSTANCE;
	
	private static final Levenshtein LEVENSHTEIN_DISTANCE = new Levenshtein();
	private static final DoubleMetaphone DOUBLE_METAPHONE = new DoubleMetaphone();
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");
	
	@SuppressWarnings("unchecked")
	public List<IntelSearchResultItem> fuzzySearch(String searchFor, List<String> typeKeys, Session session){
		
		Map<UUID, IntelSearchResultItem> results = new HashMap<>();

		List<String> metas = new ArrayList<String>();
		SPLIT_PATTERN.splitAsStream(searchFor).forEach(w -> {
			String s = DOUBLE_METAPHONE.doubleMetaphone(w,false);
			if (!metas.contains(s)) metas.add(s);
			s = DOUBLE_METAPHONE.doubleMetaphone(w,true);
			if (!metas.contains(s)) metas.add(s);
		});
		

		List<byte[]> types = null;
		if (typeKeys != null && !typeKeys.isEmpty()){
			List<IntelEntityType> stypes = session.createCriteria(IntelEntityType.class)
			.add(Restrictions.in("conservationArea", SmartDB.getConservationAreaConfiguration().getConservationAreas()))
			.add(Restrictions.in("keyId", typeKeys))
			.list();
			types = stypes.stream()
					.map(iet -> UuidUtils.uuidToByte(iet.getUuid()))
					.collect(Collectors.toList());
		}
				
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT a.string_value, a.entity_uuid ");
		sql.append (" FROM ");
		sql.append (" smart.i_entity_attribute_value a ");
		sql.append (" JOIN smart.i_entity b ");
		sql.append(" ON a.entity_uuid = b.uuid and b.ca_uuid in (:cas) ");
		sql.append(" AND a.string_value is not null ");
		
		if (types != null ){
			sql.append(" AND b.entity_type_uuid in (:types) ");
		}
		
		if (!metas.isEmpty()){
			sql.append(" AND ( ");
			for (int i = 0; i < metas.size(); i ++){
				sql.append(" smart.metaphoneContains(:m"+i+",a.metaphone) OR ");
			}
			sql.deleteCharAt(sql.length() - 1);
			sql.deleteCharAt(sql.length() - 1);
			sql.deleteCharAt(sql.length() - 1);
			sql.deleteCharAt(sql.length() - 1);
			sql.append(" ) ");
		}

		// create the query and parameters
		Query q = session.createSQLQuery(sql.toString());
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas());
		if (types != null){
			q.setParameterList("types", types);
		}
		for (int i = 0; i < metas.size(); i ++){
			q.setParameter("m" + i, metas.get(i));
		}
		List<Object[]> items = q.list();
		
		ParsedString searchString = new ParsedString(searchFor);
		//search through each row and compute a rating value
		for (Object[] row : items){
			String fullstring = (String)row[0];
			UUID uuid = UuidUtils.byteToUUID((byte[])row[1]);
			Double value = 0.0;
			if (searchFor.equalsIgnoreCase(fullstring)){
				value = 1.0;
			}else{
				value = getRating(searchString,fullstring) * 0.999;
			}
			
			IntelSearchResultItem r = results.get(uuid);
			if (r == null ){
				results.put(uuid, new IntelSearchResultItem(uuid, fullstring, value));
			}else{
				if (value > r.getRating()){
					r.setResult(uuid, fullstring, value);
				}
			}
		}
		
		//sort
		ArrayList<IntelSearchResultItem> sorted = new ArrayList<IntelSearchResultItem>(
				results.values());
		sorted.sort((a, b) -> -1 * Double.compare(a.getRating(), b.getRating()));
		return sorted;
	}
	


	private Double getRating(ParsedString words, String searchIn){
		
		ParsedString searchParse = new ParsedString(searchIn);
		
		Double value = 0.0;
		for (int i = 0; i < words.getNumWords(); i ++){
			
			String m1 = words.getMetas(i)[0];
			String m2 = words.getMetas(i)[1];
			
			Double bestValue = null;
			for (int j = 0; j < searchParse.getNumWords(); j ++){
				if (words.getWord(i).equalsIgnoreCase(searchParse.getWord(j))){
					bestValue = 1.0;
				}else{
					String m3 = searchParse.getMetas(j)[0];
					String m4 = searchParse.getMetas(j)[1];
					
					if (m1.equals(m3) || m1.equals(m4) | m2.equals(m3) || m2.equals(m4)){
						Double v = 1 - (LEVENSHTEIN_DISTANCE.distance(words.getWord(i), searchParse.getWord(j)) / Math.max(words.getWord(i).length(), searchParse.getWord(j).length()));
						if (bestValue == null || v > bestValue){
							bestValue = v;
						}
					}
				}					
			}
			if (bestValue != null){
				value += bestValue;
			}
		}
		value= (value / words.getNumWords());
		
		return value;
	}
	
	public Double getRating(String searchString, String searchIn){
		if (searchString.equalsIgnoreCase(searchIn)){
			return 1.0;
		}
		return getRating(new ParsedString(searchString), searchIn) * 0.999;
	}
	
	private class ParsedString{
		private String[] words;
		private String[][] metas;
		
		public ParsedString(String fullString){
			words = SPLIT_PATTERN.split(fullString);
			metas = new String[words.length][2];
			for (int i = 0; i < words.length; i ++){
				metas[i][0] = DOUBLE_METAPHONE.doubleMetaphone(words[i], false);
				metas[i][1] = DOUBLE_METAPHONE.doubleMetaphone(words[i], true);
			}
		}
		
		public String getWord( int i ){
			return words[i];
		}
		public int getNumWords(){
			return words.length;
		}
		public String[] getMetas(int i){
			return metas[i];
		}
	}
}
