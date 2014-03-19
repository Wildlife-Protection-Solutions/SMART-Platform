package org.wcs.smart.entity.query.parser.internal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.engine.DerbyEntityQueryEngine;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.ui.definition.EntityDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.AttributeDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeListDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;

public class EntityAttributeFilter implements IFilter {
	
	
	/**
	 * Creates a new boolean attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:b:<key>"
	 * @return
	 */
	public static EntityAttributeFilter createBooleanFilter(String attributeIdentifier){
		return new EntityAttributeFilter(attributeIdentifier);
	}
	
	/**
	 * Creates a new value attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:n:<key>"
	 * @param op the operator
	 * @param Double value the filter value
	 * @return
	 */
	public static EntityAttributeFilter createValueFilter(String attributeIdentifier, Operator op, Double value){
		return new EntityAttributeFilter(attributeIdentifier, op, value);
	}
	/**
	 * Creates a new text attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:s:<key>"
	 * @param op the string operator
	 * @param value the filter value
	 * @return
	 */
	public static EntityAttributeFilter createStringFilter(String attributeIdentifier, Operator op, String value){
		value = SmartUtils.stripQuotes(value);
		return new EntityAttributeFilter(attributeIdentifier,  op, value);
	}

	/**
	 * Creates a new date attribute filter
	 * 
	 * Date filters are of the form: <DATE> BETWEEN <DATE1> AND <DATE2>
	 * 
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:s:<key>"
	 * @param date1 the first date
	 * @param date2 the second date
	 * @return
	 */
	public static EntityAttributeFilter createDateFilter(String attributeIdentifier, String date1, String date2, Operator op){
		return new EntityAttributeFilter(attributeIdentifier, op, date1, date2);
	}
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:l:<key>"
	 * @param op the list operator
	 * @param attributeItemKey the list item key
	 * @return
	 */
	public static EntityAttributeFilter createListItemFilter(String attributeIdentifier, Operator op, String attributeItemKey){
		return new EntityAttributeFilter(attributeIdentifier,  op, attributeItemKey);
	}
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "entity:<key>:attribute:t:<hkey>"
	 * @param op the list operator
	 * @param attributeItemKey the tree item hkey
	 * @return
	 */
	public static EntityAttributeFilter createTreeItemFilter(String attributeIdentifier, Operator op, String attributeItemKey){
		return new EntityAttributeFilter(attributeIdentifier,  op, attributeItemKey);
	}
	
	private String entityKey;
	private String entityAttributeKey;
	private AttributeType attributeType;
	private String dmEntityTypeAttributeKey = null;
	private Operator op;
	private Object value1;
	private Object value2;
	
	
	/**
	 * Creates a new attribute filter with a given key and type.
	 * 
	 * @param attributeIdentifier
	 * @param type
	 */
	private EntityAttributeFilter(String attributeIdentifier){
		String[] bits = attributeIdentifier.split(":"); //$NON-NLS-1$
		
		this.entityKey = bits[1];
		this.attributeType = Attribute.decodeAttributeTypeKey(bits[3]);
		entityAttributeKey = bits[4];
	}
	
	/**
	 * Creates a new attribute filter 
	 * @param attributeIdentifier the attribute key of the form attribute:type:attributeKey
	 * @param op the filter operator
	 * @param value the filter value
	 */
	private EntityAttributeFilter(String attributeIdentifier, Operator op, Object value){
		this(attributeIdentifier);
		this.op = op;
		this.value1 = value;
	}
	
	/* for between operators */
	private EntityAttributeFilter(String attributeIdentifier, Operator op, Object value, Object value2){
		this(attributeIdentifier, op,  value);
		this.value2 = value2;
	}
	
	public Operator getOperator(){
		return this.op;
	}
	
	public String getEntityDmAttributeKey(Connection c, DerbyEntityQueryEngine engine){
		if (dmEntityTypeAttributeKey != null){
			return dmEntityTypeAttributeKey;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT " + engine.tablePrefix(Attribute.class) + ".keyid"); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(EntityType.class));
		sql.append(" join "); //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(Attribute.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityType.class) + ".dm_attribute_uuid = "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(Attribute.class) + ".uuid "); //$NON-NLS-1$
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityType.class));
		sql.append(".keyid = '" + entityKey + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" AND "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityType.class));
		if (SmartDB.isMultipleAnalysis()){
			sql.append(".ca_uuid = x'" + SmartUtils.encodeHex(SmartDB.getConservationAreaConfiguration().getMainConservationArea().getUuid()) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			sql.append(".ca_uuid = x'" + SmartUtils.encodeHex(SmartDB.getCurrentConservationArea().getUuid()) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		try{
			QueryPlugIn.logSql(sql.toString());
			ResultSet rs = c.createStatement().executeQuery(sql.toString());
			if (rs.next()){
				dmEntityTypeAttributeKey = rs.getString(1);
			}else{
				throw new RuntimeException(MessageFormat.format(Messages.EntityAttributeFilter_NoAttributeFound, new Object[]{entityKey}));
			}
			rs.close();
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
		
		return dmEntityTypeAttributeKey;
		
	}
	/**
	 * @return the unique entity attribute key
	 */
	public String getEntityAttributeKey(){
		return this.entityAttributeKey;
	}
	
	/**
	 * 
	 * @return the unique entity key
	 */
	public String getEntityKey(){
		return this.entityKey;
	}
	/**
	 * @return the type of attribute represented by the filter
	 */
	public AttributeType getAttributeType(){
		return this.attributeType;
	}
	/**
	 * @return the attribute filter value; the type depends on the attribute type
	 */
	public Object getValue(){
		return this.value1;
	}
	/**
	 * @return the second attribute filter value; the type depends on the attribute type
	 */
	public Object getValue2(){
		return this.value2;
	}
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		String key = "entity:" + entityKey + ":attribute:" + attributeType.typeKey + ":" + entityAttributeKey; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		if (attributeType == AttributeType.BOOLEAN){
			return key ;
		}else if (attributeType == AttributeType.NUMERIC){
			return key + " " + op.asSmartValue() + " " + ((Double)value1).toString();  //$NON-NLS-1$  //$NON-NLS-2$
		}else if (attributeType == AttributeType.TEXT){
			return key + " " + op.asSmartValue() + " \"" + ((String)value1) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}else if (attributeType == AttributeType.TREE || attributeType == AttributeType.LIST){
			return key + " " + op.asSmartValue() + " " + ((String)value1);  //$NON-NLS-1$  //$NON-NLS-2$  
		}else if (attributeType == AttributeType.DATE){
			return key + " " + op.asSmartValue() + " " + (String)value1 + " " + Operator.AND.asSmartValue() + " " + ((String)value2); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$ 
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * 
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 * 
	 * @return {@link AttributeDropItem} or {@link AttributeListDropItem} 
	 * or {@link AttributeTreeDropItem} depending on 
	 * attribute type.
	 */
	public DropItem[] getDropItems(Session session) throws Exception{
		try{
			EntityAttribute ea = getEntityAttribute(session);
			DropItem it = EntityDropItemFactory.INSTANCE.createEntityAttributeDropItem(ea);
			initDropItem(it, session);
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
	}
	
	/**
	 * Initializes the drop item with the 
	 * values from this filter
	 * 
	 * 
	 * @param it the drop item to initialize
	 * @param session database session
	 */
	public void initDropItem(DropItem it,  Session session){
		if (attributeType == AttributeType.TEXT || 
				attributeType == AttributeType.NUMERIC){
			it.initializeData(new String[]{op.getGuiValue(), String.valueOf(value1)});
		}else if (attributeType == AttributeType.LIST){
			ListItem li = null;
			if (AttributeFilter.ANY_OPTION.getKey().equals((String)value1)){
				li = AttributeFilter.ANY_OPTION;
			}else{
				try{
					EntityAttribute ea = getEntityAttribute(session);
					AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session, ea.getDmAttribute().getKeyId(), (String)value1);
					if (ali == null){
						throw new IllegalStateException(MessageFormat.format(Messages.EntityAttributeFilter_ListItemNotFound, new Object[]{(String)value1, ea.getDmAttribute().getKeyId()}));
					}
					li = new ListItem(ali.getUuid(), ali.getName(), ali.getKeyId());
				}catch (Exception ex){
					throw new IllegalStateException(ex.getMessage());
				}
			}
			it.initializeData(li);
		}else if (attributeType == AttributeType.TREE){
			try{
				EntityAttribute ea = getEntityAttribute(session);
				AttributeTreeNode ali = QueryDataModelManager.getInstance().getAttributeTreeNode(session, ea.getDmAttribute().getKeyId(), (String)value1);
				if (ali == null){
					throw new IllegalStateException(MessageFormat.format(Messages.EntityAttributeFilter_TreeNodeNotFound, new Object[]{(String)value1, ea.getDmAttribute().getKeyId()}));
				}
				it.initializeData(ali);
			}catch (Exception ex){
				throw new IllegalStateException(ex.getMessage());
			}
			
		}else if (attributeType == AttributeType.DATE){
			it.initializeData(new String[]{(String)value1, (String)value2, op.getGuiValue()});
		}
	}
	
	/**
	 * Loads the attribute from the database
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public EntityAttribute getEntityAttribute(Session session) throws Exception{
		
		Query q = session.createQuery("From EntityAttribute where entityType.conservationArea.uuid = :ca and entityType.keyId = :entitykey and keyId = :key"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid()); //$NON-NLS-1$
		q.setParameter("key", entityAttributeKey); //$NON-NLS-1$
		q.setParameter("entitykey", entityKey); //$NON-NLS-1$
		q.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<EntityAttribute> results = q.list();
		if (results.size() != 1 ){
			throw new Exception(MessageFormat.format(Messages.EntityAttributeFilter_Invalidtype, new Object[]{entityAttributeKey, entityKey}));
		}else{
			return results.get(0);
		}
	}

	public void accept(IFilterVisitor visitor){
		visitor.visit(this);
	}
}
