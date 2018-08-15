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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Advanced entity search implementation
 * 
 * @author Emily
 *
 */
public class AdvancedEntitySearch implements IIntelEntitySearch{
	
	public enum Error{
		PARSE_ERROR,
		RUN_ERROR,
		ATTRIBUTE_TYPE_NOT_SUPPORTED,
		TOKEN_NOT_SUPPORTED
	}
	
	private int maxResultCnt = MAX_RESULT_CNT;
	
	private String searchString = null;
	
	public static final String ENTITYTYPE_KEY = "et"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_KEY = "a"; //$NON-NLS-1$
	
	private Collection<ConservationArea> cas;
	
	public static AdvancedEntitySearch parse(String searchString, ConservationArea ca){
		return parse(searchString, Collections.singleton(ca));
	}
	
	public static AdvancedEntitySearch parse(String searchString, Collection<ConservationArea> cas){
		String[] bits = searchString.split(IntelEntitySearch.SEPARATOR);
		if (!bits[0].equals(IntelEntitySearch.Type.ADVANCED.key)) return null;
		
		//old form included max search result
		//new form does not include this
		String ss = ""; //$NON-NLS-1$
		if (bits.length == 3) {
			ss = bits[2];
		}else if (bits.length == 2) {
			ss = bits[1];
		}
		
		AdvancedEntitySearch search = new AdvancedEntitySearch(cas);
		search.searchString = ss;
		return search;
	}
	
	
	public AdvancedEntitySearch(Collection<ConservationArea> cas){
		this.cas = cas;
	}
	
	public AdvancedEntitySearch(ConservationArea ca){
		this(Collections.singleton(ca));
	}
	
	public void setSearchString(String searchString){
		this.searchString = searchString;
	}
	
	public String getSearchString(){
		return this.searchString;
	}
	
	/**
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility 
	 * to call done() on the given monitor
	 */
	@Override
	public IntelSearchResult doSearch(Session session, Locale locale, IProgressMonitor monitor) throws Exception {
		
		if (searchString == null || searchString.trim().isEmpty()){
			Long now = System.nanoTime();
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<IntelEntity> c = cb.createQuery(IntelEntity.class);
			Root<IntelEntity> from = c.from(IntelEntity.class);
			c.where(from.get("conservationArea").in(cas)); //$NON-NLS-1$
			Query<IntelEntity> q = session.createQuery(c);
			List<IntelSearchResultItem> items = new ArrayList<>(maxResultCnt);
			try(ScrollableResults scroll = q.scroll()){
				while(scroll.next()) {
					IntelEntity entity = (IntelEntity) scroll.get()[0];
					items.add(new IntelSearchResultItem(entity.getUuid(), 1));		
				}
			}
			
			Long done = System.nanoTime();
			IntelSearchResult results = new IntelSearchResult(items, (done - now)); 
			return results;
		}else{
			Long now = System.nanoTime();
			List<?> uuids = null;
			try{
				uuids = runQueryString(session, locale);
			}catch (Exception ex){
				throw new Exception(MessageFormat.format(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(Error.PARSE_ERROR, locale), ex.getMessage())  ,ex);
			}
			try{
				int totalCount = uuids.size();
				List<IntelSearchResultItem> items = new ArrayList<>(Math.min(totalCount, maxResultCnt));
				for (int i = 0; i < uuids.size(); i ++){
					UUID uuid = (UUID) uuids.get(i);
					items.add(new IntelSearchResultItem(uuid, 1));
				}
				
				Long done = System.nanoTime();
				IntelSearchResult results = new IntelSearchResult(items, (done - now));
				return results;
			}catch (Exception ex){
				throw new Exception(MessageFormat.format(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(Error.RUN_ERROR, locale), ex.getMessage()), ex);
			}
		}
	}

	@Override
	public String serialize() {
		StringBuilder sb = new StringBuilder();
		sb.append(IntelEntitySearch.Type.ADVANCED.key);
		sb.append(IntelEntitySearch.SEPARATOR);
		sb.append(searchString);
		
		return sb.toString();
	}

	
	private List<UUID> runQueryString(Session session, Locale locale) throws Exception{
		String stokens[] = searchString.split("\\|"); //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		sb.append("DROP TABLE qt_temp");  //$NON-NLS-1$
		try {
			session.createNativeQuery(sb.toString()).executeUpdate();
		}catch (Exception ex) {}
		
		sb = new StringBuilder();
		sb.append(" CREATE TABLE qt_temp (entity_uuid char(16) for bit data, entity_type_key varchar(128) )"); //$NON-NLS-1$
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		
		sb = new StringBuilder();
		sb.append("INSERT INTO qt_temp (entity_uuid, entity_type_key ) ");  //$NON-NLS-1$
		sb.append(" SELECT DISTINCT ie.uuid, t.keyid "); //$NON-NLS-1$
		sb.append(" FROM smart.i_entity ie join smart.i_entity_type t on ie.entity_type_uuid = t.uuid  WHERE ie.ca_uuid in ( :cauuids ) "); //$NON-NLS-1$
		
		List<byte[]> uuids = this.cas.stream().map(e->UuidUtils.uuidToByte(e.getUuid())).collect(Collectors.toList()); 
		session.createNativeQuery(sb.toString())
			.setParameterList("cauuids", uuids)	 //$NON-NLS-1$
			.executeUpdate();

		StringBuilder where = new StringBuilder();
		
		int cnt = 0;
		for (String t : stokens){
			
			if (t.startsWith(ENTITYTYPE_KEY + " ")){ //$NON-NLS-1$
				String[] bits = t.split(" "); //$NON-NLS-1$
				String entityTypeKey = bits[2];
				
				String columnName = "e_" + entityTypeKey + cnt; //$NON-NLS-1$
				cnt++;
				
				sb = new StringBuilder();
				sb.append("ALTER TABLE qt_temp ADD COLUMN ");  //$NON-NLS-1$
				sb.append(columnName);
				sb.append(" boolean default false");  //$NON-NLS-1$
					
				session.createNativeQuery(sb.toString()).executeUpdate();
					
				sb = new StringBuilder();
				sb.append("UPDATE qt_temp SET " );  //$NON-NLS-1$
				sb.append(columnName);
				sb.append(" = true WHERE entity_uuid IN (");  //$NON-NLS-1$
				sb.append(" SELECT t.entity_uuid FROM qt_temp t WHERE t.entity_type_key = :key)");  //$NON-NLS-1$
				
				session.createNativeQuery(sb.toString())
					.setParameter("key", entityTypeKey)  //$NON-NLS-1$
					.executeUpdate();
				
				where.append(" " + columnName +" ");  //$NON-NLS-1$//$NON-NLS-2$
			}else  if (t.startsWith(ATTRIBUTE_KEY + ":")){ //$NON-NLS-1$
				String[] qbits = t.split(" "); //$NON-NLS-1$
				String[] bits = qbits[0].split(":"); //$NON-NLS-1$
				String attributeKey = bits[2].split("=")[0]; //$NON-NLS-1$

				String columnName = "a_" + attributeKey + cnt; //$NON-NLS-1$
				cnt++;
					
				sb = new StringBuilder();
				sb.append("ALTER TABLE qt_temp ADD COLUMN ");  //$NON-NLS-1$
				sb.append(columnName);
				sb.append(" boolean DEFAULT FALSE");  //$NON-NLS-1$
					 
				session.createNativeQuery(sb.toString()).executeUpdate();
					
				sb = new StringBuilder();
				sb.append("UPDATE qt_temp SET " );  //$NON-NLS-1$
				sb.append(columnName);
				sb.append(" = true WHERE entity_uuid IN (");  //$NON-NLS-1$
					
				String atype = bits[1];
			
				where.append(" " + columnName +" ");  //$NON-NLS-1$//$NON-NLS-2$
				
				if (atype.equalsIgnoreCase(IntelAttribute.AttributeType.BOOLEAN.key)){
					sb.append("SELECT t.entity_uuid");  //$NON-NLS-1$
					sb.append(" FROM qt_temp t join smart.i_entity_attribute_value v on t.entity_uuid = v.entity_uuid ");  //$NON-NLS-1$
					sb.append(" join smart.i_attribute a on a.uuid = v.attribute_uuid and a.keyId = :attributeKey ");  //$NON-NLS-1$
					sb.append(" WHERE v.double_value > 0.5 ");  //$NON-NLS-1$
					sb.append(")"); //$NON-NLS-1$
						
					session.createNativeQuery(sb.toString())
						.setParameter("attributeKey", attributeKey)  //$NON-NLS-1$
						.executeUpdate();
						
				}else if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.TEXT.key)){
					sb.append("SELECT t.entity_uuid"); //$NON-NLS-1$
					sb.append(" FROM qt_temp t join smart.i_entity_attribute_value v on t.entity_uuid = v.entity_uuid ");  //$NON-NLS-1$
					sb.append(" join smart.i_attribute a on a.uuid = v.attribute_uuid and a.keyId = :attributeKey ");  //$NON-NLS-1$
					sb.append(" WHERE LOWER(v.string_value)  ");  //$NON-NLS-1$
						
					int startIndex = t.indexOf(qbits[2], qbits[0].length());
					String strValue = t.substring(startIndex).trim();
					String value = SharedUtils.stripQuotes(strValue).toLowerCase();
					if (qbits[1].equalsIgnoreCase(Operator.STR_EQUALS.getKey())){
						sb.append(" = "); //$NON-NLS-1$
					}else if (qbits[1].equalsIgnoreCase(Operator.STR_CONTAINS.getKey())){
						sb.append(" like "); //$NON-NLS-1$
						value = "%" + value + "%"; //$NON-NLS-1$ //$NON-NLS-2$
					}else if (qbits[1].equalsIgnoreCase(Operator.STR_NOTCONTAINS.getKey())){
						sb.append(" not like "); //$NON-NLS-1$
						value = "%" + value + "%"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					sb.append(" :value "); //$NON-NLS-1$
					sb.append(")");  //$NON-NLS-1$
						
					session.createNativeQuery(sb.toString())
						.setParameter("attributeKey", attributeKey)  //$NON-NLS-1$
						.setParameter("value", value)  //$NON-NLS-1$
						.executeUpdate();
						
				}else if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.NUMERIC.key)){
					sb.append("SELECT t.entity_uuid"); //$NON-NLS-1$
					sb.append(" FROM qt_temp t join smart.i_entity_attribute_value v on t.entity_uuid = v.entity_uuid "); //$NON-NLS-1$
					sb.append(" join smart.i_attribute a on a.uuid = v.attribute_uuid and a.keyId = :attributeKey "); //$NON-NLS-1$
					sb.append(" WHERE v.double_value ");  //$NON-NLS-1$
					sb.append(qbits[1]);
					sb.append(" :value ");  //$NON-NLS-1$
					sb.append(")");  //$NON-NLS-1$
						
					session.createNativeQuery(sb.toString())
						.setParameter("attributeKey", attributeKey)  //$NON-NLS-1$
						.setParameter("value", Double.parseDouble(qbits[2]))  //$NON-NLS-1$
						.executeUpdate(); 
			
				}else if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.DATE.key)){
					sb.append("SELECT t.entity_uuid"); //$NON-NLS-1$
					sb.append(" FROM qt_temp t join smart.i_entity_attribute_value v on t.entity_uuid = v.entity_uuid "); //$NON-NLS-1$
					sb.append(" join smart.i_attribute a on a.uuid = v.attribute_uuid and a.keyId = :attributeKey "); //$NON-NLS-1$
					sb.append(" WHERE v.string_value is not null and cast(v.string_value as date )"); //$NON-NLS-1$
						
					if (qbits[1].equalsIgnoreCase(Operator.BETWEEN.getKey())){
						sb.append(" between "); //$NON-NLS-1$
					}else if (qbits[1].equalsIgnoreCase(Operator.NOT_BETWEEN.getKey())){
						sb.append(" not between "); //$NON-NLS-1$
					}else{
						throw new Exception(MessageFormat.format("Operator {0} not supported for date filter", qbits[2])); //$NON-NLS-1$
					}
						
					sb.append(" :date1 and :date2 "); //$NON-NLS-1$ 
					sb.append(")"); //$NON-NLS-1$
						
					Date d1 = (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).parse(qbits[2]);
					Date d2 = (new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).parse(qbits[4]);
						
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(d2.getTime());
					cal.set(Calendar.SECOND, 59);
					cal.set(Calendar.MINUTE, 59);
					cal.set(Calendar.HOUR_OF_DAY, 23);
						
					session.createNativeQuery(sb.toString())
						.setParameter("date1", d1) //$NON-NLS-1$
						.setParameter("date2", cal.getTime()) //$NON-NLS-1$
						.setParameter("attributeKey", attributeKey) //$NON-NLS-1$
						.executeUpdate();
				}else if (bits[1].equalsIgnoreCase(IntelAttribute.AttributeType.LIST.key)){
						
					sb.append("SELECT t.entity_uuid"); //$NON-NLS-1$
					sb.append(" FROM qt_temp t join smart.i_entity_attribute_value v on t.entity_uuid = v.entity_uuid ");  //$NON-NLS-1$
					sb.append(" join smart.i_attribute a on a.uuid = v.attribute_uuid and a.keyId = :attributeKey ");  //$NON-NLS-1$
					sb.append(" join smart.i_attribute_list_item li on li.uuid = v.list_item_uuid ");  //$NON-NLS-1$
					sb.append(" WHERE v.li.keyid = :keyId");  //$NON-NLS-1$
					sb.append(" ) "); //$NON-NLS-1$

					String listKey = qbits[2];
					session.createNativeQuery(sb.toString())
						.setParameter("attributeKey", attributeKey) //$NON-NLS-1$
						.setParameter("keyId", listKey) //$NON-NLS-1$
						.executeUpdate();
				}else{
					throw new Exception(MessageFormat.format(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(Error.ATTRIBUTE_TYPE_NOT_SUPPORTED, locale), bits[1]));
				}
				
			}else if (t.equalsIgnoreCase(Operator.AND.getKey())){
				where.append(" AND "); //$NON-NLS-1$
			}else if (t.equalsIgnoreCase(Operator.OR.getKey())){
				where.append( " OR "); //$NON-NLS-1$
			}else if (t.equalsIgnoreCase(Operator.NOT.getKey())){
				where.append( " NOT "); //$NON-NLS-1$
			}else if (t.equalsIgnoreCase(Operator.BRACKET_OPEN.getKey())){
				where.append(" ( "); //$NON-NLS-1$
			}else if (t.equalsIgnoreCase(Operator.BRACKET_CLOSE.getKey())){
				where.append(" ) "); //$NON-NLS-1$
			}
		}
		
		sb = new StringBuilder();
		sb.append(" SELECT entity_uuid FROM qt_temp "); //$NON-NLS-1$
		sb.append(" WHERE "); //$NON-NLS-1$
		sb.append(where);

		
		//query results
		List<?> items = session.createNativeQuery(sb.toString()).list();
		List<UUID> entities = new ArrayList<>();
		for (Object x : items) {
			byte[] bb = (byte[])x;
			UUID eUuid = UuidUtils.byteToUUID(bb);
			entities.add(eUuid);
		}
		
		try {
			//drop results table
			session.createNativeQuery("DROP TABLE qt_temp").executeUpdate(); //$NON-NLS-1$
		}catch (Exception ex) {}
		
		return entities;
		
	}
}
