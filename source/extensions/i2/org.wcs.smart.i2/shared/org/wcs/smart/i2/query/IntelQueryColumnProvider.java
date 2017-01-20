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
package org.wcs.smart.i2.query;

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.IntelHibernateManager;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;

/**
 * Manages columns for intelligence queries.
 * 
 * @author Emily
 *
 */
public class IntelQueryColumnProvider {

	private static IntelQueryColumnProvider instance;
	
	public static Object ANY_ITEM = new Object(); 
			
	public static String generateName(Area area){
		return MessageFormat.format("{0} [{1}]", area.getName(), area.getType().name());
	}
	
	public static String generateName(IntelAttribute attribute, IntelEntityType type){
		if (type == null){
			return attribute.getName();
		}else{
			return MessageFormat.format("{0} ({1})", attribute.getName(), type.getName());
		}
	}
	
	public static String generateName(IntelEntity entity){
		return MessageFormat.format("{0} ({1})", entity.getIdAttributeAsText(), entity.getEntityType().getName() );
	}
	
	public static String generateName(Attribute attribute, Category category){
		if (attribute != null && category == null) return attribute.getName();
		if (category != null && attribute == null) return category.getFullCategoryName();
		if (category != null && attribute != null) return  MessageFormat.format("{0} ({1})", attribute.getName(), category.getFullCategoryName());
		return null;
	}
	
	public synchronized static IntelQueryColumnProvider getInstance(){
		if (instance == null){
			instance = new IntelQueryColumnProvider();
		}
		return instance;	
	}
	
	@SuppressWarnings("unchecked")
	public List<IQueryColumn> getQueryColumns (IntelRecordObservationQuery query, Locale l, Session session) throws Exception{
		
		List<IQueryColumn> columns = new ArrayList<>();
		
		// Fixed query columns
		for (FixedQueryColumn.Column c : FixedQueryColumn.Column.values()){
			columns.add(new FixedQueryColumn(c, l));
		}
		
		//Columns for various filter items in queries
		try{
			IQueryFilter queryFilter = IntelRecordObservationQuery.parseQuery(query.getQueryString()).getFilter();
			if (queryFilter != null){
				queryFilter.accept(new IFilterVisitor() {
					@Override
					public void visitElement(IQueryFilter filter) {
						IQueryColumn column = null;
						if (filter instanceof EntityFilter){
							column = generateColumn((EntityFilter)filter, session);
						}else if (filter instanceof EntityTypeFilter){
							column = generateColumn((EntityTypeFilter)filter, session);
						}else if (filter instanceof IntelAttributeFilter){
							IntelAttributeFilter afilter = (IntelAttributeFilter)filter;
							column = new FilterQueryColumn(generateColumnName(afilter, session, l), afilter.getUniqueColumnIdentifier(), afilter);
						}
						if (column != null && !columns.contains(column)){
							columns.add(column);
						}
					}
				});
			}
			
		}catch (Exception ex){
			throw new Exception("Error loading query columns.  Unable to parse query: " + ex.getMessage(), ex);
		}
		

		//data model columns
		//categories
		SQLQuery sq = session.createSQLQuery("SELECT max(smart.hkeylength(hkey)) from smart.dm_category WHERE ca_uuid = :ca");
		sq.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid());
		Integer maxCategory = (Integer) sq.uniqueResult();
		
		for (int i = 0; i < maxCategory; i ++){
			columns.add(new DataModelColumn(i));
		}
		
		//attributes
		//TODO: test this
		Query q = session.createSQLQuery("SELECT distinct id.attribute FROM CatetoryAttribute a WHERE a.id.attribute.conservationArea = :ca and a.isActive ");
		List<Attribute> attributes = q.list();
//		
//		List<CategoryAttribute> attributes = session.createCriteria(CategoryAttribute.class)
//				.add(Restrictions.eq("id.attribute.conservationArea", SmartDB.getCurrentConservationArea()))
//				.add(Restrictions.eq("isActive", true))
//				.list();
		attributes.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));
		for (Attribute attribute : attributes){
			columns.add(new DataModelColumn(attribute));
		}
		
		return columns;
	}
	
	private IQueryColumn generateColumn(EntityTypeFilter filter, Session session){
		IntelEntityType entity = IntelHibernateManager.getEntityType(filter.getTypeKey(), session);
		String name = null;
		if (entity != null){
			name = entity.getName();
		}else{
			name= filter.getTypeKey();
		}
		return new FilterQueryColumn(name, filter.getUniqueColumnIdentifier(), filter);
	}
	
	private IQueryColumn generateColumn(EntityFilter filter, Session session){
		IntelEntity entity = IntelHibernateManager.getEntity(filter.getEntityUuid(), session);
		String name = null;
		if (entity != null){
			name = generateName(entity);
		}else{
			name= filter.getEntityUuid().toString();
		}
		return new FilterQueryColumn(name,  filter.getUniqueColumnIdentifier(), filter);
	}
	
	private String generateColumnName(IntelAttributeFilter filter, Session session, Locale l){
		
		StringBuilder sb = new StringBuilder();
		
		IntelAttribute attribute = IntelHibernateManager.getAttribute(filter.getAttributeKey(), session);
		IntelEntityType etype = null;
		if (filter.getEntityTypeKey() != null){
			etype = IntelHibernateManager.getEntityType(filter.getEntityTypeKey(), session);
		}
		
		if (attribute != null){
			sb.append(generateName(attribute, etype));
		}else{
			sb.append(filter.getAttributeKey());
			if (filter.getEntityTypeKey() != null){
				sb.append (" (" + filter.getEntityTypeKey() + ")");
			}
		}
		
		switch(filter.getAttributeType()){
			case BOOLEAN:
				break;
			case DATE:
				sb.append(": ");
				sb.append(DateFormat.getDateInstance().format(filter.getDateValues()[0]));
				sb.append(" " + filter.getOperator().getLabel(l) + " ");
				sb.append(DateFormat.getDateInstance().format(filter.getDateValues()[1]));
				break;
			case LIST:
				if (filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
					sb.append(": ");
					sb.append(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(ANY_ITEM, l));
				}else{
					sb.append(": ");
					IntelAttributeListItem i = IntelHibernateManager.getAttributeListItem(attribute, filter.getKeyValue(), session);
					if (i != null){
						sb.append(i.getName());
					}else{
						sb.append(filter.getKeyValue());
					}
				}
				break;
			case NUMERIC:
				sb.append(": ");
				sb.append(filter.getOperator().getLabel(l));
				sb.append(" ");
				sb.append(filter.getNumberValue());
				break;
			case TEXT:
				sb.append(": ");
				sb.append(filter.getOperator().getLabel(l));
				sb.append(" ");
				sb.append(filter.getStringValue());
				break;
		}
		return sb.toString();
	}

}
