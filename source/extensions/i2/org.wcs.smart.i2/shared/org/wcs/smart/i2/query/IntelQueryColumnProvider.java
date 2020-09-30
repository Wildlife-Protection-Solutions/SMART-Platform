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
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.IntelHibernateManager;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.query.observation.filter.DataModelFilter;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.RecordAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;
import org.wcs.smart.util.UuidUtils;

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
		return MessageFormat.format("{0} [{1}]", area.getName(), area.getType().name()); //$NON-NLS-1$
	}
	
	public static String generateName(IntelAttribute attribute, IntelEntityType type){
		if (type == null){
			return attribute.getName();
		}else{
			return MessageFormat.format("{0} ({1})", attribute.getName(), type.getName()); //$NON-NLS-1$
		}
	}
	
	public static String generateName(IntelRecordSourceAttribute attribute, IntelRecordSource type){
		if (type == null){
			if (attribute.getName() != null) return attribute.getName();
			if (attribute.getAttribute() != null) return attribute.getAttribute().getName();
			return attribute.getEntityType().getName();
			
		}else{
			if (attribute.getName() != null) return MessageFormat.format("{0} ({1})", attribute.getName(), type.getName()); //$NON-NLS-1$
			if (attribute.getAttribute() != null) return MessageFormat.format("{0} ({1})", attribute.getAttribute().getName(), type.getName()); //$NON-NLS-1$
			return MessageFormat.format("{0} ({1})", attribute.getEntityType().getName(), type.getName()); //$NON-NLS-1$
		}
	}
	
	public static String generateName(IntelEntity entity, Locale l){
		return MessageFormat.format("{0} ({1})", entity.getIdAttributeAsText(l), entity.getEntityType().getName() ); //$NON-NLS-1$
	}
	
	public static String generateName(Attribute attribute, Category category){
		if (attribute != null && category == null) return attribute.getName();
		if (category != null && attribute == null) return category.getFullCategoryName();
		if (category != null && attribute != null) return  MessageFormat.format("{0} ({1})", attribute.getName(), category.getFullCategoryName()); //$NON-NLS-1$
		return null;
	}
	
	public synchronized static IntelQueryColumnProvider getInstance(){
		if (instance == null){
			instance = new IntelQueryColumnProvider();
		}
		return instance;	
	}
	public List<IQueryColumn> getQueryColumns (AbstractIntelQuery query, IQueryItemProvider itemProvider, Locale l, Session session) throws Exception{
		if (query instanceof IntelRecordObservationQuery) {
			return getQueryColumns((IntelRecordObservationQuery)query, itemProvider, l, session);
		}else if (query instanceof IntelEntityRecordQuery) {
			return getQueryColumns((IntelEntityRecordQuery)query, itemProvider, l, session);
		}else if (query instanceof IntelRecordQuery) {
			return getQueryColumns((IntelRecordQuery)query, itemProvider, l, session);
		}
		throw new IllegalStateException("getQueryColumns is not support for query type " + query.getTypeKey()); //$NON-NLS-1$
	}
	
	@SuppressWarnings("unchecked")
	public List<IQueryColumn> getQueryColumns (IntelRecordQuery query, IQueryItemProvider itemProvider, Locale l, Session session) throws Exception{
		
		List<IQueryColumn> columns = new ArrayList<>();
		
		// Fixed query columns
		FixedQueryColumn.Column[] thiscolumns;
		if (query.getConservationArea().getIsCcaa()) {
			thiscolumns = new FixedQueryColumn.Column[]{
					FixedQueryColumn.Column.CA_ID,
					FixedQueryColumn.Column.CA_NAME,
					FixedQueryColumn.Column.RECORD_TITLE,
					FixedQueryColumn.Column.RECORD_STATUS,
					FixedQueryColumn.Column.RECORD_SOURCE,
					FixedQueryColumn.Column.RECORD_PROFILE,
				};
		}else {
			thiscolumns = new FixedQueryColumn.Column[]{
					FixedQueryColumn.Column.RECORD_PROFILE,
					FixedQueryColumn.Column.RECORD_TITLE,
					FixedQueryColumn.Column.RECORD_STATUS,
					FixedQueryColumn.Column.RECORD_SOURCE,
					FixedQueryColumn.Column.RECORD_DATE,
					
				};
		}
		
		for (FixedQueryColumn.Column c : thiscolumns){
			columns.add(new FixedQueryColumn(c, l));
		}
		
		Set<String> profiles = AbstractIntelQuery.convertFromProfileFilter(query.getProfileFilter());
		Set<UUID> uuids = new HashSet<>();
		
		uuids.addAll(session.createQuery("SELECT uuid FROM IntelProfile WHERE keyId IN (:keys) and conservationArea in (:cas)") //$NON-NLS-1$
		.setParameterList("keys", profiles) //$NON-NLS-1$
		.setParameterList("cas", itemProvider.getConservationAreas()).list()); //$NON-NLS-1$
		
		
		List<IntelRecordSource> items = itemProvider.getRecordSources(uuids, session);
		for (IntelRecordSource rs : items) {
			for (IntelRecordSourceAttribute ir : itemProvider.getRecordSourceAttributes(rs, session)) {
				columns.add(new IntelRecordAttributeQueryColumn(ir));
			}
		}
		
		return columns;
	}

	public List<IQueryColumn> getQueryColumns (IntelRecordObservationQuery query, IQueryItemProvider itemProvider, Locale l, Session session) throws Exception{
		
		List<IQueryColumn> columns = new ArrayList<>();
		
		// Fixed query columns
		FixedQueryColumn.Column[] thiscolumns;
		if (query.getConservationArea().getIsCcaa()) {
			thiscolumns = new FixedQueryColumn.Column[]{
					FixedQueryColumn.Column.CA_ID,
					FixedQueryColumn.Column.CA_NAME,
					FixedQueryColumn.Column.RECORD_TITLE,
					FixedQueryColumn.Column.RECORD_STATUS,
					FixedQueryColumn.Column.RECORD_SOURCE,
					FixedQueryColumn.Column.RECORD_PROFILE,
					FixedQueryColumn.Column.LOC_ID,
					FixedQueryColumn.Column.LOC_DATE,
					FixedQueryColumn.Column.LOC_TIME,
					FixedQueryColumn.Column.LOC_COMMENT,
					FixedQueryColumn.Column.LOC_GEOMTRY,
				};
		}else {
			thiscolumns = new FixedQueryColumn.Column[]{
					FixedQueryColumn.Column.RECORD_TITLE,
					FixedQueryColumn.Column.RECORD_STATUS,
					FixedQueryColumn.Column.RECORD_SOURCE,
					FixedQueryColumn.Column.RECORD_PROFILE,
					FixedQueryColumn.Column.LOC_ID,
					FixedQueryColumn.Column.LOC_DATE,
					FixedQueryColumn.Column.LOC_TIME,
					FixedQueryColumn.Column.LOC_COMMENT,
					FixedQueryColumn.Column.LOC_GEOMTRY,
				};
		}
		
		for (FixedQueryColumn.Column c : thiscolumns){
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
							column = generateColumn((EntityFilter)filter, l, session);
						}else if (filter instanceof EntityTypeFilter){
							column = generateColumn((EntityTypeFilter)filter, itemProvider, session);
						}else if (filter instanceof IntelAttributeFilter){
							IntelAttributeFilter afilter = (IntelAttributeFilter)filter;
							column = new FilterQueryColumn(generateColumnName(afilter, itemProvider, session, l), afilter.getUniqueColumnIdentifier(), afilter);
						}
						if (column != null && !columns.contains(column)){
							columns.add(column);
						}
					}
				});
			}
			
		}catch (Exception ex){
			throw new Exception("Error loading query columns.  Unable to parse query: " + ex.getMessage(), ex); //$NON-NLS-1$
		}
		

		//data model columns
		//categories
		Integer maxCategory = itemProvider.getMaxDmCategoryDepth(session);
		for (int i = 0; i < maxCategory; i ++){
			columns.add(new DataModelColumn(i, l));
		}
		
		//attributes - keep only active attributes
		List<?> q = session.createQuery("SELECT distinct id.attribute.keyId FROM CategoryAttribute a WHERE a.id.attribute.conservationArea in ( :cas ) and a.isActive = 'true'") //$NON-NLS-1$
				.setParameterList("cas", itemProvider.getConservationAreas()).list(); //$NON-NLS-1$
		Set<String> attributeKeys = new HashSet<>();
		q.forEach(e->attributeKeys.add((String)e));
			
		List<Attribute> attributes = itemProvider.getDmAttributes(session);
		for (Iterator<Attribute> iterator = attributes.iterator(); iterator.hasNext();) {
			Attribute attribute = (Attribute) iterator.next();
			if (!attributeKeys.contains(attribute.getKeyId())) iterator.remove();
		}

		attributes.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));
		for (Attribute attribute : attributes){
			columns.add(new DataModelColumn(attribute));
		}
		
		return columns;
	}
	
	public List<IQueryColumn> getQueryColumns (IntelEntityRecordQuery query, IQueryItemProvider itemProvider, Locale l, Session session) throws Exception{
		
		List<IQueryColumn> columns = new ArrayList<>();
		
		// Fixed query columns
		FixedQueryColumn.Column[] thiscolumns;
		if (query.getConservationArea().getIsCcaa()) {
			thiscolumns = new FixedQueryColumn.Column[]{
					FixedQueryColumn.Column.CA_ID,
					FixedQueryColumn.Column.CA_NAME,
					FixedQueryColumn.Column.ENTITY_ID,
					FixedQueryColumn.Column.ENTITY_TYPE,
					FixedQueryColumn.Column.ENTITY_PROFILE,
			};	
		}else {
			thiscolumns = new FixedQueryColumn.Column[]{
				FixedQueryColumn.Column.ENTITY_ID,
				FixedQueryColumn.Column.ENTITY_TYPE,
				FixedQueryColumn.Column.ENTITY_PROFILE,
			};
		}
		
		for (FixedQueryColumn.Column c : thiscolumns){
			columns.add(new FixedQueryColumn(c, l));
		}
		
		//Columns for various filter items in queries
		try{
			IQueryFilter queryFilter = IntelEntityRecordQuery.parseQuery(query.getQueryString()).getFilter();
			if (queryFilter != null){
				queryFilter.accept(new IFilterVisitor() {
					@Override
					public void visitElement(IQueryFilter filter) {
						IQueryColumn column = null;
						if (filter instanceof EntityFilter){
							column = generateColumn((EntityFilter)filter, l, session);
						}else if (filter instanceof EntityTypeFilter){
							column = generateColumn((EntityTypeFilter)filter, itemProvider, session);
						}else if (filter instanceof IntelAttributeFilter){
							IntelAttributeFilter afilter = (IntelAttributeFilter)filter;
							column = new FilterQueryColumn(generateColumnName(afilter, itemProvider, session, l), afilter.getUniqueColumnIdentifier(), afilter);
						}else if (filter instanceof SystemAttributeFilter) {
							SystemAttributeFilter afilter = (SystemAttributeFilter)filter;
							column = generateColumnName(afilter, itemProvider, session, l);
						}else if (filter instanceof RecordAttributeFilter) {
							RecordAttributeFilter afilter = (RecordAttributeFilter)filter;
							column = new FilterQueryColumn(generateColumnName(afilter, itemProvider, session, l), afilter.getUniqueColumnIdentifier(), afilter);
						}else if (filter instanceof DataModelFilter) {
							DataModelFilter afilter = (DataModelFilter)filter;
							column = generateColumnName(afilter, itemProvider, session, l);
						}
						
						if (column != null && !columns.contains(column)){
							columns.add(column);
						}
					}
				});
			}
			
		}catch (Exception ex){
			throw new Exception("Error loading query columns.  Unable to parse query: " + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
		//intel attribute columns
		List<IntelAttribute> attributes = session.createQuery("SELECT distinct ia.id.attribute FROM IntelEntityTypeAttribute ia WHERE ia.id.attribute.conservationArea in(:cas)", IntelAttribute.class) //$NON-NLS-1$
				.setParameter("cas", itemProvider.getConservationAreas()).list(); //$NON-NLS-1$
		
		List<String> keys = new ArrayList<>();
		HashMap<String, IntelAttribute.AttributeType> types = new HashMap<>();
		List<String> toremove= new ArrayList<>();
		attributes.forEach(e->{
			if (!keys.contains(e.getKeyId())) {
				keys.add(e.getKeyId());
			}
			IntelAttribute.AttributeType type = types.get(e.getKeyId());
			if (type != null && type != e.getType()) toremove.add(e.getKeyId());
			if (type == null) types.put(e.getKeyId(), e.getType());
		});
		keys.removeAll(toremove);
		
		keys.sort((a,b)->a.compareTo(b));
		for (String key : keys) {
			IntelAttribute ia = itemProvider.getAttribute(key, session);
			if (ia != null) columns.add(new IntelAttributeQueryColumn(ia));
		}	
		return columns;
	}

	private IQueryColumn generateColumn(EntityTypeFilter filter, IQueryItemProvider itemProvider, Session session){
		IntelEntityType entity = itemProvider.getEntityType(filter.getTypeKey(), session);
		String name = null;
		if (entity != null){
			name = entity.getName();
		}else{
			name= filter.getTypeKey();
		}
		return new FilterQueryColumn(name, filter.getUniqueColumnIdentifier(), filter);
	}
	
	private IQueryColumn generateColumn(EntityFilter filter, Locale l, Session session){
		IntelEntity entity = IntelHibernateManager.getEntity(filter.getEntityUuid(), session);
		String name = null;
		if (entity != null){
			name = generateName(entity, l);
		}else{
			name= filter.getEntityUuid().toString();
		}
		return new FilterQueryColumn(name,  filter.getUniqueColumnIdentifier(), filter);
	}
	
	private IQueryColumn generateColumnName(SystemAttributeFilter filter, IQueryItemProvider itemProvider, Session session, Locale l){
		if (filter.getAttribute() == SystemAttribute.RECORD_SOURCE) {
			IntelRecordSource src = itemProvider.getRecordSource(filter.getStringKey(), session);
			String name = null;
			if (src == null) {
				name = filter.getStringKey();
			}else {
				name = src.getName();
			}
			return new FilterQueryColumn(name, filter.getUniqueColumnIdentifier(), filter);
		}else if (filter.getAttribute() == SystemAttribute.RECORD_STATUS) {
			String name = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(IntelRecord.Status.valueOf(filter.getStringKey()), l);
			return new FilterQueryColumn(name, filter.getUniqueColumnIdentifier(), filter);
		}else if (filter.getAttribute() == SystemAttribute.RECORD_DATE) {
			String name = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(SystemAttribute.RECORD_DATE, l);
			return new FilterQueryColumn(name, filter.getUniqueColumnIdentifier(), filter);
		}
		return null;
	}
	
	private IQueryColumn generateColumnName(DataModelFilter filter, IQueryItemProvider itemProvider, Session session, Locale l){
		Category c = null;
		if (filter.getCategoryKey() != null) {
			c = itemProvider.getCategory(filter.getCategoryKey(), session);
		}
		Attribute a = null;
		if (filter.getAttributeKey() != null) {
			a = itemProvider.getDmAttribute(filter.getAttributeKey(), session);
		}
		
		StringBuilder sb = new StringBuilder();
		if (filter.getAttributeKey() == null) {
			sb.append(c == null ? filter.getCategoryKey() : c.getName());
		}else {
			sb.append( a==null ? filter.getAttributeKey() : a.getName());
		
			switch(filter.getAttributeType()) {
			case BOOLEAN:
				break;
			case DATE:
				sb.append(" ("); //$NON-NLS-1$
				sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(filter.getDateValues()[0]));
				sb.append(" - "); //$NON-NLS-1$
				sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(filter.getDateValues()[1]));
				sb.append(")"); //$NON-NLS-1$
				break;
			case LIST:
				if (filter.getKeyValue().equals(DataModelFilter.ANY_OPTION_KEY)) {
					sb.append(" (" ); //$NON-NLS-1$
					sb.append(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(ANY_ITEM, l));
					sb.append(")"); //$NON-NLS-1$
				}else {
					List<AttributeListItem> allItems = itemProvider.getDmAttributeListItem(a, session);
					boolean ok = false;
					for (AttributeListItem i : allItems) {
						if (i.getKeyId().equalsIgnoreCase(filter.getKeyValue())) {
							sb.append(" ("); //$NON-NLS-1$
							sb.append(i.getName());
							sb.append(") "); //$NON-NLS-1$
							ok = true;
						}
					}
					if (!ok) {
						sb.append(" ("); //$NON-NLS-1$
						sb.append(filter.getKeyValue());
						sb.append(")"); //$NON-NLS-1$
					}
				}
				break;
			case NUMERIC:
				sb.append( " ("); //$NON-NLS-1$
				sb.append(filter.getOperator().getLabel(l));
				sb.append(" "); //$NON-NLS-1$
				sb.append(filter.getNumberValue());
				sb.append(")"); //$NON-NLS-1$
				break;
			case TEXT:
				sb.append(" ("); //$NON-NLS-1$
				sb.append(filter.getStringValue());
				sb.append(")"); //$NON-NLS-1$
				break;
			case TREE:
				List<AttributeTreeNode> nodes = new ArrayList<>(itemProvider.getDmAttributeTreeNodes(a, session));
				boolean ok = false;
				while(!nodes.isEmpty()) {
					AttributeTreeNode n = nodes.remove(0);
					if (n.getHkey().equalsIgnoreCase(filter.getKeyValue())) {
						sb.append(" ("); //$NON-NLS-1$
						sb.append(n.getName() );
						sb.append(") "); //$NON-NLS-1$
						ok = true;
						break;
					}
					nodes.addAll(n.getChildren());
				}
				if (!ok) { 
					sb.append( " ("); //$NON-NLS-1$
					sb.append(filter.getKeyValue());
					sb.append(")"); //$NON-NLS-1$
				}
				break;
			default:
				break;
			
			}
			
			if (filter.getCategoryKey() != null) {
				sb.append(" ["); //$NON-NLS-1$
				sb.append(c == null ? filter.getCategoryKey() : c.getName() );
				sb.append("]"); //$NON-NLS-1$
			}
				
		}
		
		return new FilterQueryColumn(sb.toString(), filter.getUniqueColumnIdentifier(), filter);
	}
	
	
	private String generateColumnName(IntelAttributeFilter filter, IQueryItemProvider itemProvider, Session session, Locale l){
		
		StringBuilder sb = new StringBuilder();
		IntelAttribute attribute = itemProvider.getAttribute(filter.getAttributeKey(), session);
		IntelEntityType etype = null;
		if (filter.getEntityTypeKey() != null){
			etype = itemProvider.getEntityType(filter.getEntityTypeKey(), session);
		}
		
		if (attribute != null){
			sb.append(generateName(attribute, etype));
		}else{
			sb.append(filter.getAttributeKey());
			if (filter.getEntityTypeKey() != null){
				sb.append (" (" + filter.getEntityTypeKey() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		switch(filter.getAttributeType()){
			case BOOLEAN:
				break;
			case DATE:
				sb.append(": "); //$NON-NLS-1$
				sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(filter.getDateValues()[0]));
				sb.append(" " + filter.getOperator().getLabel(l) + " "); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(filter.getDateValues()[1]));
				break;
			case LIST:
				if (filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
					sb.append(": "); //$NON-NLS-1$
					sb.append(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(ANY_ITEM, l));
				}else{
					sb.append(": "); //$NON-NLS-1$
					List<IntelAttributeListItem> items = itemProvider.getAttributeListItems(attribute.getKeyId(), session);
					IntelAttributeListItem item = null;
					for (IntelAttributeListItem i : items) {
						if (i.getKeyId().equals(filter.getKeyValue())) {
							item = i;
							break;
						}
					}
					if (item != null){
						sb.append(item.getName());
					}else{
						sb.append(filter.getKeyValue());
					}
				}
				break;
			case EMPLOYEE:
				if (filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
					sb.append(": "); //$NON-NLS-1$
					sb.append(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(ANY_ITEM, l));
				}else{
					sb.append(": "); //$NON-NLS-1$
					Employee e = null;
					try {
						UUID uuid = UuidUtils.stringToUuid(filter.getKeyValue());
						e = session.get(Employee.class, uuid);
					}catch (Exception ex) {
						ex.printStackTrace();
					}
					
					if (e != null){
						sb.append(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(e, l));
					}else{
						sb.append(filter.getKeyValue());
					}
				}
				break;
			case NUMERIC:
				sb.append(": "); //$NON-NLS-1$
				sb.append(filter.getOperator().getLabel(l));
				sb.append(" "); //$NON-NLS-1$
				sb.append(filter.getNumberValue());
				break;
			case TEXT:
				sb.append(": "); //$NON-NLS-1$
				sb.append(filter.getOperator().getLabel(l));
				sb.append(" "); //$NON-NLS-1$
				sb.append(filter.getStringValue());
				break;
			case POSITION:
				throw new UnsupportedOperationException("position attributes not supported in queries"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	
	private String generateColumnName(RecordAttributeFilter filter, IQueryItemProvider itemProvider, Session session, Locale l){
		
		IntelRecordSource rsource = itemProvider.getRecordSource(filter.getRecordSourceKey(), session);
		
		IntelRecordSourceAttribute att = null;
		for (IntelRecordSourceAttribute a : rsource.getAttributes()) {
			if (a.getKeyId().equalsIgnoreCase(filter.getAttributeKey())) {
				att = a;
				break;
			}
		}
		if (att == null) {
			return "Attribute Not Found"; //$NON-NLS-1$
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(MessageFormat.format("{0} ({1})", IIntelligenceLabelProvider.getName(att), rsource.getName())); //$NON-NLS-1$

		if (att.getAttribute() != null) {
			
			
			switch(filter.getAttributeType()){
				case BOOLEAN:
					break;
				case DATE:
					sb.append(": "); //$NON-NLS-1$
					sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(filter.getDateValues()[0]));
					sb.append(" " + filter.getOperator().getLabel(l) + " "); //$NON-NLS-1$ //$NON-NLS-2$
					sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(filter.getDateValues()[1]));
					break;
				case LIST:
					if (filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
						sb.append(": "); //$NON-NLS-1$
						sb.append(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(ANY_ITEM, l));
					}else{
						sb.append(": "); //$NON-NLS-1$
						List<IntelAttributeListItem> items = itemProvider.getAttributeListItems(att.getAttribute().getKeyId(), session);
						IntelAttributeListItem item = null;
						for (IntelAttributeListItem i : items) {
							if (i.getKeyId().equals(filter.getKeyValue())) {
								item = i;
								break;
							}
						}
						if (item != null){
							sb.append(item.getName());
						}else{
							sb.append(filter.getKeyValue());
						}
					}
					break;
				case EMPLOYEE:
					if (filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
						sb.append(": "); //$NON-NLS-1$
						sb.append(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(ANY_ITEM, l));
					}else{
						sb.append(": "); //$NON-NLS-1$
						Employee e = null;
						try {
							UUID uuid = UuidUtils.stringToUuid(filter.getKeyValue());
							e = session.get(Employee.class, uuid);
						}catch (Exception ex) {
							ex.printStackTrace();
						}
						
						if (e != null){
							sb.append(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(e, l));
						}else{
							sb.append(filter.getKeyValue());
						}
					}
					break;
				case NUMERIC:
					sb.append(": "); //$NON-NLS-1$
					sb.append(filter.getOperator().getLabel(l));
					sb.append(" "); //$NON-NLS-1$
					sb.append(filter.getNumberValue());
					break;
				case TEXT:
					sb.append(": "); //$NON-NLS-1$
					sb.append(filter.getOperator().getLabel(l));
					sb.append(" "); //$NON-NLS-1$
					sb.append(filter.getStringValue());
					break;
				case POSITION:
					throw new UnsupportedOperationException("position attributes not supported in queries"); //$NON-NLS-1$
			}
			
		}else {
		
			if (filter.getKeyValue().equals(IQueryFilter.ANY_OPTION_KEY)){
				sb.append(": "); //$NON-NLS-1$
				sb.append(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(ANY_ITEM, l));
			}else{
				sb.append(": "); //$NON-NLS-1$
				UUID entityUuid = UuidUtils.stringToUuid( filter.getKeyValue() );
				
				IntelEntity entity = session.get(IntelEntity.class,  entityUuid);
				if (entity != null){
					sb.append(entity.getIdAttributeAsText());
				}else{
					sb.append(filter.getKeyValue());
				}
			}
		}
		return sb.toString();
	}
}
