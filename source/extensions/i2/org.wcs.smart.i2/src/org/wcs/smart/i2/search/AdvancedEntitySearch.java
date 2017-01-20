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
package org.wcs.smart.i2.search;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.util.SharedUtils;

/**
 * Advanced entity search implementation
 * 
 * @author Emily
 *
 */
public class AdvancedEntitySearch implements IIntelEntitySearch{
	
	private int maxResultCnt = MAX_RESULT_CNT;
	
	private String searchString = null;
	
	public static final String ENTITYTYPE_KEY = "et";
	
	public static final String ATTRIBUTE_KEY = "a";
	
	
	public static AdvancedEntitySearch parse(String searchString){
		String[] bits = searchString.split(SEPARATOR);
		if (!bits[0].equals(Type.ADVANCED.key)) return null;
		
		int maxCnt = Integer.parseInt(bits[1]);
		String ss = bits[2];
		AdvancedEntitySearch search = new AdvancedEntitySearch();
		search.searchString = ss;
		search.maxResultCnt = maxCnt;
		return search;
	}
	
	
	public AdvancedEntitySearch(){
		
	}
	
	public void setSearchString(String searchString){
		this.searchString = searchString;
	}
	
	public String getSearchString(){
		return this.searchString;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public IntelSearchResult doSearch(Session session, IProgressMonitor monitor) {
		
		if (searchString == null || searchString.trim().isEmpty()){
			Long now = System.nanoTime();
			Long eCount = (Long)session.createCriteria(IntelEntity.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
			.setProjection(Projections.rowCount())
			.uniqueResult();
			
			List<IntelEntity> entities = session.createCriteria(IntelEntity.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
			.setMaxResults(maxResultCnt)
			.list();
			
			List<IntelSearchResultItem> items = new ArrayList<>();
			for (IntelEntity e : entities){
				items.add(new IntelSearchResultItem(e.getUuid(), null, 1, lazyLoadEntity(e, session)));
			}
			Long done = System.nanoTime();
			IntelSearchResult results = new IntelSearchResult(eCount, items, (done - now)); 
			return results;
		}else{
			Long now = System.nanoTime();
			Query q = null;
			try{
				q = parseQueryString(session);
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog("Error parsing search query.  Ensure query is valid and try again. " + ex.getMessage(), ex);
				return new IntelSearchResult(0, Collections.emptyList(), 0);
			}
			try{
				List<UUID> uuids = q.list();
				int totalCount = uuids.size();
				List<IntelSearchResultItem> items = new ArrayList<>();
				for (int i = 0; i < Math.min(totalCount, maxResultCnt); i ++){
					UUID uuid = uuids.get(i);
					items.add(new IntelSearchResultItem(uuid, null, 1, lazyLoadEntity((IntelEntity)session.get(IntelEntity.class, uuid), session)));
				}
				
				Long done = System.nanoTime();
				IntelSearchResult results = new IntelSearchResult(totalCount, items, (done - now));
				return results;
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog("Error executing search query: " + ex.getMessage(), ex);
				return new IntelSearchResult(0, Collections.emptyList(), 0);
			}
		}
	}

	@Override
	public String serialize() {
		StringBuilder sb = new StringBuilder();
		sb.append(Type.ADVANCED.key);
		sb.append(SEPARATOR);
		sb.append(maxResultCnt);
		sb.append(SEPARATOR);
		sb.append(searchString);
		
		return sb.toString();
	}

	
	private Query parseQueryString(Session session) throws Exception{
		String stokens[] = searchString.split("\\|");
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT DISTINCT ie.uuid ");
		sb.append(" FROM IntelEntity ie  JOIN ie.entityType t ");
		
		HashSet<String> usedKeys = new HashSet<String>();
		for (String t : stokens){
			if (t.startsWith(ATTRIBUTE_KEY + ":")){
				String[] qbits = t.split(" ");
				String[] bits = qbits[0].split(":");
				String attributeKey = bits[2].split("=")[0];
				if (!usedKeys.contains(attributeKey)){
					usedKeys.add(attributeKey);
					sb.append (" LEFT JOIN ie.attributes a_" + attributeKey + " LEFT JOIN a_" + attributeKey + ".id.attribute at_" + attributeKey + " with at_" + attributeKey + ".keyId = '" + attributeKey + "' ");
					if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.LIST.key)){
						sb.append(" LEFT JOIN a_" + attributeKey + ".attributeListItem li_" + attributeKey);
					}
				}
			}
		}
		
		
		int pcnt = 0;
		HashMap<String, Object> params = new HashMap<>();
		
		sb.append(" WHERE ");
		sb.append(" ie.conservationArea = :ca AND ( ");
		params.put("ca", SmartDB.getCurrentConservationArea());
		
		for (String t : stokens){
			t = t.trim();
			if (t.equalsIgnoreCase(Operator.AND.getKey())){
				sb.append(" AND ");
			}else if (t.equalsIgnoreCase(Operator.OR.getKey())){
				sb.append( " OR ");
			}else if (t.equalsIgnoreCase(Operator.NOT.getKey())){
				sb.append( " NOT ");
			}else if (t.equalsIgnoreCase(Operator.BRACKET_OPEN.getKey())){
				sb.append(" ( ");
			}else if (t.equalsIgnoreCase(Operator.BRACKET_CLOSE.getKey())){
				sb.append(" ) ");
			}else if (t.startsWith(ENTITYTYPE_KEY)){
				String[] bits = t.split(" ");
				String pname = "p" + (pcnt++);
				sb.append(" t.keyId = :" + pname);
				params.put(pname, bits[2]);
			}else if (t.startsWith(ATTRIBUTE_KEY + ":")){
				String[] qbits = t.split(" ");
				String[] bits = qbits[0].split(":");
				if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.BOOLEAN.key)){
					String attributeKey = bits[2];
					sb.append(" (  at_" + attributeKey + ".keyId = '" + attributeKey + "' AND a_" + attributeKey + ".numberValue  > 0.5 ) "); //at_" + attributeKey + ".keyId = '" + attributeKey + "' AND
				}else if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.TEXT.key)){
					String attributeKey = bits[2];
					sb.append(" ( at_" + attributeKey + ".keyId = '" + attributeKey + "' AND LOWER(a_" + attributeKey + ".stringValue) ");
					
					String value = SharedUtils.stripQuotes(qbits[2]).toLowerCase();
					if (qbits[1].equalsIgnoreCase(Operator.STR_EQUALS.getKey())){
						sb.append(" = ");
					}else if (qbits[1].equalsIgnoreCase(Operator.STR_CONTAINS.getKey())){
						sb.append(" like ");
						value = "%" + value + "%";
					}else if (qbits[1].equalsIgnoreCase(Operator.STR_NOTCONTAINS.getKey())){
						sb.append(" not like ");
						value = "%" + value + "%";
					}
					
					String pname = "p" + (pcnt++);
					sb.append(" :" + pname + " ");
					params.put(pname, value);
					
					sb.append(" ) ");
					
				}else if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.NUMERIC.key)){
					String attributeKey = bits[2];
					sb.append(" ( at_" + attributeKey + ".keyId = '" + attributeKey + "' AND a_" + attributeKey + ".numberValue  ");
					sb.append(qbits[1]); //operator
					String pname = "p" + (pcnt++);
					sb.append(" :" + pname + " ");
					params.put(pname, Double.parseDouble(qbits[2]));
					sb.append(" ) ");
					
					
				}else if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.DATE.key)){
					String attributeKey = bits[2];
					String pname1 = "p" + (pcnt++);
					String pname2 = "p" + (pcnt++);
					
					sb.append(" ( at_" + attributeKey + ".keyId = '" + attributeKey + "' AND a_" + attributeKey + ".stringValue is not null AND  cast(a_" + attributeKey + ".stringValue as date) ");
					
					if (qbits[1].equalsIgnoreCase(Operator.BETWEEN.getKey())){
						sb.append(" between ");
					}else if (qbits[1].equalsIgnoreCase(Operator.NOT_BETWEEN.getKey())){
						sb.append(" not between ");
					}else{
						throw new Exception(MessageFormat.format("Operator {0} not supported for date filter", qbits[2]));
					}
					sb.append(" :" + pname1 + " AND :" + pname2 + " ) ");
					Date d1 = (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).parse(qbits[2]);
					Date d2 = (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).parse(qbits[4]);
							
					params.put(pname1, d1);
					params.put(pname2, d2);
					
				}else if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.LIST.key)){
					String attributeKey = bits[2];
					String listKey = qbits[2];
					sb.append(" ( at_" + attributeKey + ".keyId = '" + attributeKey + "' AND li_" + attributeKey + ".keyId = ");
					
					String pname = "p" + (pcnt++);
					sb.append(" :" + pname + " ");
					params.put(pname, listKey);
					sb.append(" ) ");
				}else{
					throw new Exception(MessageFormat.format("Attribute type {0} not supported.", bits[1]));
				}
				
			}else{
				throw new Exception(MessageFormat.format("Unsupported search token: {0}.", t));
			}
		}
		sb.append(" ) ");
		
		//System.out.println(sb.toString());
		Query q = session.createQuery(sb.toString());
		for (Entry<String,Object> param : params.entrySet()){
			q.setParameter(param.getKey(), param.getValue());
		}
		return q;
		
	}
}
