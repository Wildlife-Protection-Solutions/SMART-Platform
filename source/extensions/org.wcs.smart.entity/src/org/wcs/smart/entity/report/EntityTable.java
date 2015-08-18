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
package org.wcs.smart.entity.report;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.SightingQueryColumn;
import org.wcs.smart.entity.ui.EntityLabelProvider;

/**
 * Entity type table for BIRT reporting 
 * 
 * @author Emily
 *
 */
public class EntityTable extends SmartBirtTable {

	public static final String ENTITYKEY_PREFIX = "ENTITY"; //$NON-NLS-1$
	private EntityType et;
	
	public EntityTable(EntityType et) {
		super(MessageFormat.format(Messages.EntityTable_LongName, new Object[]{et.getName()}),
				et.getName(),
				ENTITYKEY_PREFIX + ":" + et.getType().name() + ":" + et.getKeyId()); //$NON-NLS-1$ //$NON-NLS-2$
		this.et = et;
	}

	@Override
	public String[] getColumnNames() {
		int add = 2;
		if (et.getType() == EntityType.Type.FIXED){
			add = 4;
		}
		String[] cols = new String[et.getAttributes().size() + add];
		
		cols[0] = SightingQueryColumn.FixedColumns.ENTITY_ID.getKey();
		cols[1] = SightingQueryColumn.FixedColumns.ENTITY_STATUS.getKey();
		if (et.getType() == EntityType.Type.FIXED){
			cols[2] = "entity:x"; //$NON-NLS-1$
			cols[3] = "entity:y"; //$NON-NLS-1$
		}
		
		int i = 0;
		for (EntityAttribute ea : et.getAttributes()){
			cols[i+add] = ea.getKeyId();
			i++;
		}
		return cols;
	}

	@Override
	public String[] getColumnLabels() {
		int add = 2;
		if (et.getType() == EntityType.Type.FIXED){
			add = 4;
		}
		String[] cols = new String[et.getAttributes().size() + add];
		
		cols[0] = EntityLabelProvider.ID_FIELD_NAME;
		cols[1] = EntityLabelProvider.STATUS_FIELD_NAME;
		if (et.getType() == EntityType.Type.FIXED){
			cols[2] = EntityLabelProvider.X_FIELD_NAME;
			cols[3] = EntityLabelProvider.Y_FIELD_NAME;
		}
		
		int i = 0;
		for (EntityAttribute ea : et.getAttributes()){
			cols[i+add]=ea.getName();
			i++;
		}
		return cols;
	}

	@Override
	public int[] getColumnTypes() {
		int add = 2;
		if (et.getType() == EntityType.Type.FIXED){
			add = 4;
		}
		int[] cols = new int[et.getAttributes().size() + add];
		
		cols[0] = java.sql.Types.VARCHAR;
		cols[1] = java.sql.Types.VARCHAR;
		if (et.getType() == EntityType.Type.FIXED){
			cols[2] = java.sql.Types.DOUBLE;
			cols[3] = java.sql.Types.DOUBLE;
		}
		
		int i = 0;
		for (EntityAttribute ea : et.getAttributes()){
			AttributeType et = ea.getDmAttribute().getType();
			if (et == AttributeType.BOOLEAN){
				cols[i+add]=java.sql.Types.VARCHAR;
			}else if (et == AttributeType.DATE){
				cols[i+add]=java.sql.Types.DATE;
			}else if (et == AttributeType.LIST ||
					et == AttributeType.TREE||
					et == AttributeType.TEXT){
				cols[i+add]=java.sql.Types.VARCHAR;
			}
			i++;
		}
		return cols;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getValues (Collection<ConservationArea> cas, Session session) {
		Query q = session.createQuery("FROM Entity e WHERE e.entityType.keyId = :keyid and e.entityType.conservationArea in (:cas)"); //$NON-NLS-1$
		q.setParameter("keyid", et.getKeyId()); //$NON-NLS-1$
		q.setParameterList("cas", cas); //$NON-NLS-1$
		List<Object> x = q.list();
		return x;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index) {
		Entity e = (Entity)object;
		if (index == 0){
			return e.getId();
		}else if (index == 1){
			return e.getStatus().getGuiName(Locale.getDefault());
		}
		if (et.getType() == EntityType.Type.FIXED){
			if (index == 2){
				return e.getX();
			}else if (index == 3){
				return e.getY();
			}
			index = index - 2;
		}
		EntityAttributeValue eav = e.findAttribute(et.getAttributes().get(index-2).getKeyId());
		
		if (eav == null){
			return null;
		}
		if (eav.getEntityAttribute().getDmAttribute().getType() ==  AttributeType.LIST ||
				eav.getEntityAttribute().getDmAttribute().getType() ==  AttributeType.TREE ||
				eav.getEntityAttribute().getDmAttribute().getType() == AttributeType.BOOLEAN){
			return eav.getValueAsString(Locale.getDefault());
		}
		return eav.getValue();
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#openQuery()
	 */
	@Override
	public void openQuery() {		
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#closeQuery()
	 */
	@Override
	public void closeQuery() {
	}

}
