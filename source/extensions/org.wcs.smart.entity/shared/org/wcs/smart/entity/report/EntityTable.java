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
import java.util.List;
import java.util.Locale;

import org.hibernate.Query;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.entity.IEntityLabelProvider;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Entity type table for BIRT reporting 
 * 
 * @author Emily
 *
 */
public class EntityTable extends SmartBirtTable {

	public static final String GEOMETRY_COL_KEY = "geometry"; //$NON-NLS-1$
	
	public static final String ENTITYKEY_PREFIX = "ENTITY"; //$NON-NLS-1$
	
	private static final GeometryFactory gf = new GeometryFactory();
	
	private EntityType et;
	
	public EntityTable(EntityType et) {
		super(ENTITYKEY_PREFIX + ":" + et.getType().name() + ":" + et.getKeyId()); //$NON-NLS-1$ //$NON-NLS-2$
		this.et = et;
	}

	@Override
	public String[] getColumnNames(SmartConnection connection) {
		int size = et.getAttributes().size() + 2;
		int add = 2;
		if (et.getType() == EntityType.Type.FIXED){
			add = 4;
			size += 3;
		}
		String[] cols = new String[size];
		
		cols[0] = "entity:id"; //$NON-NLS-1$
		cols[1] = "entity:status"; //$NON-NLS-1$
		if (et.getType() == EntityType.Type.FIXED){
			cols[2] = "entity:x"; //$NON-NLS-1$
			cols[3] = "entity:y"; //$NON-NLS-1$
		}
		
		int i = 0;
		for (EntityAttribute ea : et.getAttributes()){
			cols[i+add] = ea.getKeyId();
			i++;
		}
		if (et.getType() == EntityType.Type.FIXED){
			cols[i+add] = GEOMETRY_COL_KEY;
		}
		return cols;
	}

	@Override
	public String[] getColumnLabels(SmartConnection connection) {
		int add = 2;
		int size = et.getAttributes().size() + 2;
		if (et.getType() == EntityType.Type.FIXED){
			add = 4;
			size += 3;
		}
		
		String[] cols = new String[size];
		
		IEntityLabelProvider lblProvider = SmartContext.INSTANCE.getClass(IEntityLabelProvider.class);
		
		cols[0] = lblProvider.getLabel(IEntityLabelProvider.ID_FIELD_KEY, connection.getCurrentLocale());
		cols[1] = lblProvider.getLabel(IEntityLabelProvider.STATUS_FIELD_KEY, connection.getCurrentLocale());
		if (et.getType() == EntityType.Type.FIXED){
			cols[2] = lblProvider.getLabel(IEntityLabelProvider.X_FIELD_KEY, connection.getCurrentLocale());
			cols[3] = lblProvider.getLabel(IEntityLabelProvider.Y_FIELD_KEY, connection.getCurrentLocale());
		}
		
		int i = 0;
		
		for (EntityAttribute ea : et.getAttributes()){
			cols[i+add]=ea.getName();
			i++;
		}
		if (et.getType() == EntityType.Type.FIXED){
			cols[i+add] = GEOMETRY_COL_KEY;
		}
		return cols;
	}

	@Override
	public int[] getColumnTypes(SmartConnection connection) {
		int size = et.getAttributes().size() + 2;
		int add = 2;
		if (et.getType() == EntityType.Type.FIXED){
			add = 4;
			size += 3;
		}
		int[] cols = new int[size];
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
		if (et.getType() == EntityType.Type.FIXED){
			cols[i+add] = java.sql.Types.JAVA_OBJECT;
		}
		return cols;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getValues (SmartConnection connection) {
		Query q = connection.getSession().createQuery("FROM Entity e WHERE e.entityType.keyId = :keyid and e.entityType.conservationArea in (:cas)"); //$NON-NLS-1$
		q.setParameter("keyid", et.getKeyId()); //$NON-NLS-1$
		q.setParameterList("cas", connection.getConservationAreas()); //$NON-NLS-1$
		List<Object> x = q.list();
		return x;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index, SmartConnection connection) {
		Entity e = (Entity)object;
		if (index == 0){
			return e.getId();
		}else if (index == 1){
			return e.getStatus().getGuiName(connection.getCurrentLocale());
		}
		if (et.getType() == EntityType.Type.FIXED){
			if (index == 2){
				return e.getX();
			}else if (index == 3){
				return e.getY();
			}else if ((index-4) == et.getAttributes().size() ){
				if (e.getX() == null || e.getY() == null) return null;
				return gf.createPoint(new Coordinate(e.getX(), e.getY()));
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
			return eav.getValueAsString(connection.getCurrentLocale());
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

	

	@Override
	public String getTableFullName(Locale l) {
		return MessageFormat.format(
				SmartContext.INSTANCE.getClass(IEntityLabelProvider.class).getLabel(IEntityLabelProvider.ENTITY_TYPE_REPORT_KEY, l),et.getName()); 						
	}

	@Override
	public String getTableShortName(Locale l) {
		return et.getName();
	}
}
