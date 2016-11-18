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

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.UuidItem;
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
	private Cache searchCache;
	
	public List<IntelEntitySearchResult> fuzzySearch(String searchFor, List<String> typeKeys, Session session){
		
		Map<UUID, IntelEntitySearchResult> results = new HashMap<>();
		
		
		if (searchCache == null){
			System.gc();
			System.out.println("before allocated memory: " + NumberFormat.getInstance().format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000));
			//TODO: add listeners to update when entity is modified
			searchCache = new Cache(new CacheConfiguration("basic fuzzy searching", 1000)
					.overflowToDisk(true)
					.eternal(true));
			searchManager.addCache(searchCache);
			
			StringBuilder hql = new StringBuilder();
			hql.append("SELECT v.stringValue, v.id.entity.uuid, et.keyId ");
			hql.append(" FROM IntelEntityAttributeValue v JOIN v.id.entity e join e.entityType et JOIN et.attributes ta ");
			hql.append(" WHERE v.stringValue != null AND et.conservationArea IN (:ca) ");
			hql.append(" AND v.id.attribute = ta.id.attribute ");
			hql.append(" AND (ta.inBasicSearch = 'true' OR et.idAttribute = v.id.attribute )");
			
			System.out.println("before query");
			Query q = session.createQuery(hql.toString());
			q.setParameterList("ca", SmartDB.getConservationAreaConfiguration().getConservationAreas());
			
			List<Object[]> toSearch = q.list();
			for (Object[] x : toSearch){
				SearchItem i = new SearchItem((UUID)x[1], (String)x[0], (String)x[2]);
				searchCache.put(new Element(i, null));
			}
			System.gc();
			System.out.println("after allocated memory: " + NumberFormat.getInstance().format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000));
		}
		Set<String> keys = null;
		if (typeKeys != null && !typeKeys.isEmpty()){
			keys = new HashSet<>();
			keys.addAll(typeKeys);
		}
		
		System.out.println("start search: " + searchCache.getSize());
		
		
		List<String> words = SPLIT_PATTERN.splitAsStream(searchFor).distinct().collect(Collectors.toList());
		String[][] meta = new String[words.size()][3];
		int i = 0;
		for (String w : words){
			meta[i][0] = w;
			meta[i][1] = DOUBLE_METAPHONE.doubleMetaphone(w, true);
			meta[i][2] = DOUBLE_METAPHONE.doubleMetaphone(w, false);
			i++;
		}
		HashMap<String,Double> ratings = new HashMap<String,Double>();
		for (Object item : searchCache.getKeys()){

			SearchItem si = (SearchItem) item;
			
			if (keys == null || keys.contains(si.type)){
				
				Double value = null;
				if (searchFor.equalsIgnoreCase(si.text)){
					value = 1.0;
				}else{
					value = getRating(meta, si.text, si.metas, ratings) * 0.99;
				}
				if (value != null && value > 0){
					IntelEntitySearchResult existing = results.get(si.uuid);
					if (existing == null) {
						results.put(si.uuid, new IntelEntitySearchResult(si.uuid, si.text, value));
					} else if (existing.getRating() < value) {
						existing.setResult(si.uuid, si.text, value);
					}
				}
			}
		}
		System.out.println("done search: " + results.size());
		
		ArrayList<IntelEntitySearchResult> sorted = new ArrayList<IntelEntitySearchResult>(
				results.values());
		sorted.sort((a, b) -> -1 * Double.compare(a.getRating(), b.getRating()));
		System.out.println("done sort: " + results.size());
//		return sorted.subList(0,Math.min(20, sorted.size()));
		return sorted;
		
	}
	


	private Double getRating(String[][] words, String searchIn, String[][] metas, HashMap<String, Double>ratings){
		
		Double value1 = 0.0;
		for(String[] s1 : words){
			
			Double wordValue = null;
			String d1 = s1[1];
			String d2 = s1[2];
			
			for(String[] s2 : metas){
				
				String key = s1[0] + "_" + s2[0];
				if (ratings.containsKey(key)){
					Double v = ratings.get(key);
					if (v!= null && (wordValue == null || v > wordValue)){
						wordValue = v;
					}
				}else{
					String d3 = s2[1];
					String d4 = s2[2];
					if (d1.equals(d3) || d2.equals(d3)||d1.equals(d4) || d2.equals(d4)){
						Double v = 1 - (LEVENSHTEIN_DISTANCE.distance(s1[0].toLowerCase(), s2[0].toLowerCase()) / Math.max(s1[0].length(), s2[0].length()));
						if (wordValue == null || v > wordValue){
							wordValue = v;
						}
						ratings.put(key, v);
					}else{
						ratings.put(key, null);
					}
				}
			}
			if (wordValue != null){
				value1 += wordValue;
			}
		}
		return value1 / words.length;
	}
	
	public Double getRating(String searchString, String searchIn){
		if (searchString.equalsIgnoreCase(searchIn)){
			return 1.0;
		}
		Double value1 = null;
		for(String s1 : SPLIT_PATTERN.split(searchString)){
			for (String s2 : SPLIT_PATTERN.split(searchIn)){
				String d1 = SearchManager.DOUBLE_METAPHONE.doubleMetaphone(s1, true);
				String d2 = SearchManager.DOUBLE_METAPHONE.doubleMetaphone(s1, false);
				
				String in1 = SearchManager.DOUBLE_METAPHONE.doubleMetaphone(s2, true);
				String in2 = SearchManager.DOUBLE_METAPHONE.doubleMetaphone(s2, false);
				
				
				if (d1.equals(in1) || d1.equals(in2) || d2.equals(in1) || d2.equals(in2)){
					Double v = 1 - (LEVENSHTEIN_DISTANCE.distance(s1.toLowerCase(), s2.toLowerCase()) / Math.max(s1.length(), s2.length()));
					if (value1 == null || v > value1){
						value1 = v;
					}
				}
			}
		}
		
		if (value1 == null){
			//do some ngram matching
			for(String s1 : SPLIT_PATTERN.split(searchString)){
				Collection<String> grams = createGrams(s1);
				//search and rate grams
				for (String gram : grams){
					for (String s2 : SPLIT_PATTERN.split(searchIn)){
						if (s2.contains(gram)){
							Double v = 1 - GRAM.distance(gram, s2);
							if (value1 == null || v > value1){
								value1 = v;
							}
						}	
					}
				}
			}
			if (value1 == null) return null;
		}
		
		//rate so only equals are ever given 1
		value1 = value1*0.99;
		return value1;
	}
	
	private Collection<String> createGrams(String string) {
		if (string.length() < GRAM_SIZE) return Collections.emptyList();
		string = string.toLowerCase();
		Set<String> grams = new HashSet<>();
		for (int i = 0; i <= string.length() - GRAM_SIZE; i++) {
			String gram = string.substring(i, i + GRAM_SIZE);
			grams.add(gram);
		}

		grams.add("  " + string.charAt(0));
		grams.add(" " + string.substring(0, 1));
		grams.add(string.substring(string.length() - 2) + " ");

		return grams;
	}
	
	private class SearchItem implements Serializable{
		public UUID uuid;
		public String text;
		
		public String type;
		public String[][] metas;
		
		public SearchItem(UUID uuid, String text, String type){
			this.uuid = uuid;
			this.text = text;
			this.type = type;
			
			//todo make words unique??
			String[] words = SPLIT_PATTERN.split(text);
			metas = new String[words.length][3];
			int i = 0;
			for (String w : words){
				metas[i][0] = w;
				metas[i][1] = DOUBLE_METAPHONE.doubleMetaphone(w, true);
				metas[i][2] = DOUBLE_METAPHONE.doubleMetaphone(w, false);
				i++;
			}
		}
	}
}
