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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;

/**
 * Single conservation area query item provider.  Works for a single
 * conservation area.
 * 
 * @author Emily
 *
 */
public class CaQueryItemProvider implements IQueryItemProvider {

	private ConservationArea ca;
	private ConservationArea queryCa;
	
	public CaQueryItemProvider(ConservationArea ca, ConservationArea queryCa) {
		this.ca = ca;
		this.queryCa = queryCa;
	}
	
	public ConservationArea getQueryConservationArea() {
		return this.queryCa;
	}
	
	public Collection<ConservationArea> getConservationAreas(){
		return Collections.singletonList(ca);
	}
	
	public List<IntelEntityTypeAttribute> getEntityTypeAttributes(IntelEntityType entityType, Session session){
		return entityType.getAttributes();
	}

	public List<Employee> getEmployees(Session session){
		return QueryFactory.buildQuery(session, Employee.class, 
				new Object[] {"conservationArea", getCa()}).list(); //$NON-NLS-1$
	}
	
	public List<IntelEntityType> getEntityTypes(Session session){
		return QueryFactory.buildQuery(session, IntelEntityType.class,
				new Object[] {"conservationArea", getCa()}).list(); //$NON-NLS-1$
	}
	
	public List<Area> getAreas(Area.AreaType areaType, Session session){
		return  QueryFactory.buildQuery(session, Area.class, 
				new Object[] {"conservationArea", getCa()},  //$NON-NLS-1$
				new Object[] {"type", areaType}).list(); //$NON-NLS-1$
	}
	
	public IntelAttribute getAttribute(String attributeKey, Session session) {
		return QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", getCa()}, //$NON-NLS-1$
				new Object[] {"keyId", attributeKey}).uniqueResult(); //$NON-NLS-1$
				
	}
	
	public IntelEntityType getEntityType(String entityTypeKey, Session session) {
		IntelEntityType type = QueryFactory.buildQuery(session, IntelEntityType.class, 
				new Object[] {"conservationArea", getCa()},  //$NON-NLS-1$
				new Object[] {"keyId", entityTypeKey}).uniqueResult(); //$NON-NLS-1$
		type.getName();
		return type;
	}
	
	public List<IntelAttributeListItem> getAttributeListItems(String attributeKey, Session session){
		IntelAttribute a = getAttribute(attributeKey, session);
		return a.getAttributeList();
	}
	
	private ConservationArea getCa() {
		return ca;
	}
	
	
	
	public List<IntelEntity> getEntities(String entityTypeKey, Session session){
		return session.createQuery("SELECT i FROM IntelEntity i join i.entityType t WHERE i.conservationArea = :ca and t.keyId = :entityType", IntelEntity.class) //$NON-NLS-1$
			.setParameter("entityType", entityTypeKey) //$NON-NLS-1$
			.setParameter("ca",  getCa()) //$NON-NLS-1$
			.list();
	}
	
	/**
	 * Get all intelligence attributes
	 * @param session
	 * @return
	 */
	public List<IntelAttribute> getAttributes(Session session){
		return QueryFactory.buildQuery(session, IntelAttribute.class, 
				"conservationArea", getCa()).getResultList(); //$NON-NLS-1$
	}
	
	public List<Category> getRootCategories(Session session){
		return QueryFactory.buildQuery(session, Category.class,
				new Object[] {"conservationArea", getCa()},
				new Object[] {"parent", null}).list();
	}
	public Category getCategory(String categoryHkey, Session session){
		return QueryFactory.buildQuery(session, Category.class,
				new Object[] {"conservationArea", getCa()},
				new Object[] {"hkey", categoryHkey}).uniqueResult();
	}
	
	public List<Attribute> getDmAttributes(Session session){
		return QueryFactory.buildQuery(session, Attribute.class,
				new Object[] {"conservationArea", getCa()})
				.list();
	}
	
	public Attribute getDmAttribute(String attributeKey, Session session) {
		return QueryFactory.buildQuery(session, Attribute.class,
				new Object[] {"conservationArea", getCa()},
				new Object[] {"keyId", attributeKey}).uniqueResult();
	}
	
	public List<Category> getChildren(Category category, Session session){
		Category c = session.get(Category.class, category.getUuid());
		return c.getChildren();
	}
}
