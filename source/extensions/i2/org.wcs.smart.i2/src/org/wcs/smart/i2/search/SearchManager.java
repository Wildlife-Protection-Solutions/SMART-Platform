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
import info.debatty.java.stringsimilarity.NGram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;


public enum SearchManager {
	
	INSTANCE;
	
	
	private static final Levenshtein LEVENSHTEIN_DISTANCE = new Levenshtein();
	private static final DoubleMetaphone DOUBLE_METAPHONE = new DoubleMetaphone();
	private static final int GRAM_SIZE = 3;
	private static final NGram GRAM = new NGram(GRAM_SIZE);
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");
	
	private CacheManager searchManager = CacheManager.getInstance();
	private Cache metaphoneSearchCache;
	private Cache wordToEntityCache;
	
	public List<IntelEntitySearchResult> fuzzySearch(String searchFor, List<String> typeKeys, Session session){
		
		Map<UUID, IntelEntitySearchResult> results = new HashMap<>();
		
//		
//		if (metaphoneSearchCache == null){
//			System.gc();
//			System.out.println("before allocated memory: " + NumberFormat.getInstance().format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000));
//			
//			//TODO: add listeners to update when entity is modified
//			metaphoneSearchCache = new Cache(new CacheConfiguration("basic fuzzy searching", 1000)
//					.overflowToDisk(true)
//					.eternal(true));
//			wordToEntityCache = new Cache(new CacheConfiguration("basic fuzzy searching word", 1000)
//				.overflowToDisk(true)
//				.eternal(true));
//			searchManager.addCache(metaphoneSearchCache);
//			searchManager.addCache(wordToEntityCache);
//			
		
		List<String> metas = new ArrayList<String>();
		SPLIT_PATTERN.splitAsStream(searchFor).forEach(w -> {
			String s = DOUBLE_METAPHONE.doubleMetaphone(w,false);
			if (!metas.contains(s)) metas.add(s);
			s = DOUBLE_METAPHONE.doubleMetaphone(w,true);
			if (!metas.contains(s)) metas.add(s);
		});
		
		
//		StringBuilder hql = new StringBuilder();
//		hql.append("SELECT v.stringValue, v.id.entity.uuid, et.keyId ");
//		hql.append(" FROM IntelEntityAttributeValue v JOIN v.id.entity e join e.entityType et JOIN et.attributes ta ");
//		hql.append(" WHERE v.stringValue != null AND et.conservationArea IN (:ca) ");
//		hql.append(" AND v.id.attribute = ta.id.attribute ");
//		hql.append(" AND (ta.inBasicSearch = 'true' OR et.idAttribute = v.id.attribute )");
//		hql.append(" AND ( ");
//		for (int i = 0; i < metas.size(); i ++){
//			hql.append(" function('smart.metaphoneContains', :m" + i + ", v.metaphone ) OR ");
//		}
//		if (!metas.isEmpty()){
//			hql.deleteCharAt(hql.length() - 1);
//			hql.deleteCharAt(hql.length() - 1);
//			hql.deleteCharAt(hql.length() - 1);
//			hql.deleteCharAt(hql.length() - 1);
//		}
//		hql.append( ") ");
		
		
//		SELECT count(*)
//		FROM
//		 (select * From smart.I_ENTITY_ATTRIBUTE_VALUE where string_value is not null and (smart.metaphoneContains('RFR',metaphone) OR smart.metaphoneContains('PL',metaphone)) ) a 
//		 join smart.i_entity b on a.entity_uuid = b.uuid  and b.ca_uuid in (x'633559a3622543979177e2ddf2e53399')
//		 join smart.i_entity_type c on b.entity_type_uuid = c.uuid  
//		join smart.i_entity_type_attribute d on a.attribute_uuid = d.attribute_uuid and c.uuid = d.entity_type_uuid
//		WHERE  (d.in_basic_search = 'true' OR c.id_attribute_uuid = a.attribute_uuid) 
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT a.string_value, a.entity_uuid ");
		sql.append (" FROM ");
		sql.append (" (SELECT a.* FROM smart.i_entity_attribute_value a ");
		
		if (typeKeys != null && !typeKeys.isEmpty()){
			sql.append(" JOIN smart.i_entity b on a.entity_uuid = b.uuid join smart.i_entity_type c on c.uuid = b.entity_type_uuid and c.keyId in (:types1) ");
		}
		sql.append(" WHERE a.string_value is not null ");
		
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
		sql.append(") a");
//		sql.append(" FROM smart.i_entity_attribute_value a ");
		sql.append( " join smart.i_entity b on a.entity_uuid = b.uuid and b.ca_uuid in (:ca) ");
		sql.append(" join smart.i_entity_type c on b.entity_type_uuid = c.uuid ");
		sql.append(" join smart.i_entity_type_attribute d on a.attribute_uuid = d.attribute_uuid and c.uuid = d.entity_type_uuid");
		sql.append(" WHERE  ");
		sql.append(" (d.in_basic_search = 'true' OR c.id_attribute_uuid = a.attribute_uuid) ");
		
		
		
		
		System.out.println("running query:" + sql.toString());
		Query q = session.createSQLQuery(sql.toString());
		q.setParameterList("ca", SmartDB.getConservationAreaConfiguration().getConservationAreas());
		if (typeKeys != null && !typeKeys.isEmpty()){
//			q.setParameterList("types", typeKeys);
			q.setParameterList("types1", typeKeys);
		}
		for (int i = 0; i < metas.size(); i ++){
			q.setParameter("m" + i, metas.get(i));
		}
//		ScrollableResults sresults = q.scroll();
		List<Object[]> items = q.list();
		System.out.println("processing results");
		
		int cnt = 0;
		ParsedString searchString = new ParsedString(searchFor);
//		while(sresults.next()){
//			Object[] row = sresults.get();
		for (Object[] row : items){
			String fullstring = (String)row[0];
			UUID uuid = UuidUtils.byteToUUID((byte[])row[1]);
	
				
			Double value = 0.0;
			if (searchFor.equalsIgnoreCase(fullstring)){
				value = 1.0;
			}else{
				value = getRating(searchString,fullstring) * 0.999;
			}
			
			IntelEntitySearchResult r = results.get(uuid);
			if (r == null ){
				results.put(uuid, new IntelEntitySearchResult(uuid, fullstring, value));
			}else{
				if (value > r.getRating()){
					r.setResult(uuid, fullstring, value);
				}
			}
		}
		
		System.out.println("done search: " + results.size());
		
		ArrayList<IntelEntitySearchResult> sorted = new ArrayList<IntelEntitySearchResult>(
				results.values());
		sorted.sort((a, b) -> -1 * Double.compare(a.getRating(), b.getRating()));
		System.out.println("done sort: " + results.size());
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
//		Double value1 = null;
//		for(String s1 : SPLIT_PATTERN.split(searchString)){
//			for (String s2 : SPLIT_PATTERN.split(searchIn)){
//				String d1 = SearchManager.DOUBLE_METAPHONE.doubleMetaphone(s1, true);
//				String d2 = SearchManager.DOUBLE_METAPHONE.doubleMetaphone(s1, false);
//				
//				String in1 = SearchManager.DOUBLE_METAPHONE.doubleMetaphone(s2, true);
//				String in2 = SearchManager.DOUBLE_METAPHONE.doubleMetaphone(s2, false);
//				
//				
//				if (d1.equals(in1) || d1.equals(in2) || d2.equals(in1) || d2.equals(in2)){
//					Double v = 1 - (LEVENSHTEIN_DISTANCE.distance(s1.toLowerCase(), s2.toLowerCase()) / Math.max(s1.length(), s2.length()));
//					if (value1 == null || v > value1){
//						value1 = v;
//					}
//				}
//			}
//		}
//		
//		if (value1 == null){
//			//do some ngram matching
//			for(String s1 : SPLIT_PATTERN.split(searchString)){
//				Collection<String> grams = createGrams(s1);
//				//search and rate grams
//				for (String gram : grams){
//					for (String s2 : SPLIT_PATTERN.split(searchIn)){
//						if (s2.contains(gram)){
//							Double v = 1 - GRAM.distance(gram, s2);
//							if (value1 == null || v > value1){
//								value1 = v;
//							}
//						}	
//					}
//				}
//			}
//			if (value1 == null) return null;
//		}
//		
//		//rate so only equals are ever given 1
//		value1 = value1*0.99;
//		return value1;
	}
	
//	private Collection<String> createGrams(String string) {
//		if (string.length() < GRAM_SIZE) return Collections.emptyList();
//		string = string.toLowerCase();
//		Set<String> grams = new HashSet<>();
//		for (int i = 0; i <= string.length() - GRAM_SIZE; i++) {
//			String gram = string.substring(i, i + GRAM_SIZE);
//			grams.add(gram);
//		}
//
//		grams.add("  " + string.charAt(0));
//		grams.add(" " + string.substring(0, 1));
//		grams.add(string.substring(string.length() - 2) + " ");
//
//		return grams;
//	}
	
//	private class SearchItem implements Serializable{
//		public UUID uuid;
//		public String text;
//		
//		public String type;
//		public String[][] metas;
//		
//		public SearchItem(UUID uuid, String text, String type){
//			this.uuid = uuid;
//			this.text = text;
//			this.type = type;
//			
//			//todo make words unique??
//			String[] words = SPLIT_PATTERN.split(text);
//			metas = new String[words.length][3];
//			int i = 0;
//			for (String w : words){
//				metas[i][0] = w;
//				metas[i][1] = DOUBLE_METAPHONE.doubleMetaphone(w, true);
//				metas[i][2] = DOUBLE_METAPHONE.doubleMetaphone(w, false);
//				i++;
//			}
//		}
//	}
//	
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
