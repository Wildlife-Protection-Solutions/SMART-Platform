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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StringType;
import org.hibernate.type.UUIDBinaryType;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.util.UuidUtils;

import info.debatty.java.stringsimilarity.Levenshtein;


public enum SearchManager {
	
	INSTANCE;
	
	private static final Levenshtein LEVENSHTEIN_DISTANCE = new Levenshtein();
	private static final DoubleMetaphone DOUBLE_METAPHONE = new DoubleMetaphone();
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+"); //$NON-NLS-1$
		
	public List<IntelSearchResultItem> fuzzySearch(String searchFor, List<String> typeKeys, Collection<ConservationArea> conservationAreas, int maxResults, Session session){
		
		searchFor = searchFor.trim();
		
		List<String> metas = new ArrayList<String>();
		SPLIT_PATTERN.splitAsStream(searchFor).forEach(w -> {
			String s = DOUBLE_METAPHONE.doubleMetaphone(w,false);
			if (!metas.contains(s)) metas.add(s);
			s = DOUBLE_METAPHONE.doubleMetaphone(w,true);
			if (!metas.contains(s)) metas.add(s);
		});
		

		List<UUID> types = null;
		if (typeKeys != null && !typeKeys.isEmpty()){
			 CriteriaBuilder cb = session.getCriteriaBuilder();
			 CriteriaQuery<IntelEntityType> c = cb.createQuery(IntelEntityType.class);
			 Root<IntelEntityType> from = c.from(IntelEntityType.class);
			 c.where(cb.and(
					 from.get("conservationArea").in(conservationAreas), //$NON-NLS-1$
					 from.get("keyId").in(typeKeys) //$NON-NLS-1$
					 ));
			 List<IntelEntityType> stypes = session.createQuery(c).getResultList();
			 types = stypes.stream().map(iet->iet.getUuid()).collect(Collectors.toList());
		}
				
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT a.string_value, a.entity_uuid "); //$NON-NLS-1$
		sql.append (" FROM "); //$NON-NLS-1$
		sql.append (" smart.i_entity_attribute_value a "); //$NON-NLS-1$
		sql.append (" JOIN smart.i_entity b "); //$NON-NLS-1$
		sql.append(" ON a.entity_uuid = b.uuid and b.ca_uuid in (:cas) "); //$NON-NLS-1$
		sql.append(" AND a.string_value is not null "); //$NON-NLS-1$
		
		if (types != null ){
			sql.append(" AND b.entity_type_uuid in (:types) "); //$NON-NLS-1$
		}
		
		if (!metas.isEmpty()){
			sql.append(" AND ( "); //$NON-NLS-1$
			for (int i = 0; i < metas.size(); i ++){
				sql.append(" smart.metaphoneContains(:m"+i+",a.metaphone) OR "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			sql.deleteCharAt(sql.length() - 1);
			sql.deleteCharAt(sql.length() - 1);
			sql.deleteCharAt(sql.length() - 1);
			sql.deleteCharAt(sql.length() - 1);
			
			sql.append(" OR "); //$NON-NLS-1$
			sql.append(" LOWER(a.string_value) like :ml"); //$NON-NLS-1$
			sql.append(" ) "); //$NON-NLS-1$
		}else{
			sql.append(" AND "); //$NON-NLS-1$
			sql.append(" LOWER(a.string_value) like :ml"); //$NON-NLS-1$
		}

		// create the query and parameters
		NativeQuery<?> q = session.createNativeQuery(sql.toString());
		//these two lines are required to get this to work
		//on Connect with the postgresql libraries
		q.addScalar("string_value", StringType.INSTANCE); //$NON-NLS-1$
		q.addScalar("entity_uuid", SmartContext.INSTANCE.getClass(UUIDBinaryType.class)); //$NON-NLS-1$
		q.setParameterList("cas", conservationAreas); //$NON-NLS-1$
		if (types != null){
			q.setParameterList("types", types); //$NON-NLS-1$
		}
		for (int i = 0; i < metas.size(); i ++){
			q.setParameter("m" + i, metas.get(i)); //$NON-NLS-1$
		}
		q.setParameter("ml", "%" + searchFor.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		q.setMaxResults(maxResults);
		List<?> items = q.list();
		
		ParsedString searchString = new ParsedString(searchFor);
		//search through each row and compute a rating value
		Map<UUID, IntelSearchResultItem> results = new HashMap<>();
		for (Object rr : items){
			Object[] row = (Object[])rr;
			String fullstring = (String)row[0];
			UUID uuid = null;
			if (row[1] instanceof UUID) {
				uuid = (UUID)row[1];
			}else if(row[1] instanceof byte[]) {
				uuid = UuidUtils.byteToUUID((byte[])row[1]);
			}
			Double value = 0.0;
			if (searchFor.equalsIgnoreCase(fullstring)){
				value = 1.0;
			}else{
				value = getRating(searchString,fullstring) * 0.999;
			}
			
			IntelSearchResultItem r = results.get(uuid);
			if (r == null ){
				results.put(uuid, new IntelSearchResultItem(uuid, value, fullstring));
			}else{
				if (value > r.getRating()){
					results.put(uuid, new IntelSearchResultItem(uuid, value, fullstring));
				}
			}
		}
		
		//sort
		ArrayList<IntelSearchResultItem> sorted = new ArrayList<IntelSearchResultItem>(results.values());
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
					
					if (m1.equals(m3) || m1.equals(m4) || m2.equals(m3) || m2.equals(m4)){
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
		
		if (value == 0){
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < words.getNumWords(); i ++){
				sb.append(words.getWord(i));
				sb.append(" "); //$NON-NLS-1$
			}
			String searchFor = sb.toString();
			value = 1 - (LEVENSHTEIN_DISTANCE.distance(searchFor, searchIn) / Math.max(searchFor.length(), searchIn.length()));
		}
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
