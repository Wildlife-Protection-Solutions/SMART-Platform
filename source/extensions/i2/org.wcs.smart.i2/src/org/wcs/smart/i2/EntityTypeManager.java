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
package org.wcs.smart.i2;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.search.AdvancedEntitySearch;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItemFactory;

import com.ibm.icu.text.MessageFormat;

/**
 * Tools for managing entity types
 * 
 * @author Emily
 *
 */
public enum EntityTypeManager {
	
	INSTANCE;
	
	private EntityTypeManager(){
		
	}

	
	/**
	 * Loads all entity types and sorts by name
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<IntelEntityType> getEntityTypes(Session session, ConservationArea ca){
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<IntelEntityType> c = cb.createQuery(IntelEntityType.class);
		Root<IntelEntityType> root = c.from(IntelEntityType.class);
		c.where(cb.equal(root.get("conservationArea"), ca)); //$NON-NLS-1$
		
		List<IntelEntityType> types = session.createQuery(c).getResultList();
		types.sort((IntelEntityType a, IntelEntityType b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		return types;
	}
	
	/**
	 * returns a set of entity types that are viewable by the current user
	 * and associated with an active profile
	 * 
	 * @param session
	 * @return
	 */
	public List<IntelEntityType> getViewableEntityTypesActiveProfiles(Session session){
		if (ProfilesManager.INSTANCE.getActiveProfiles().isEmpty()) return Collections.emptyList();
		
		List<IntelProfile> profiles = new ArrayList<>(ProfilesManager.INSTANCE.getActiveProfiles());
		profiles = profiles.stream().filter(e->IntelSecurityManager.INSTANCE.canViewEntities(e)).collect(Collectors.toList());
		if (profiles.isEmpty()) return Collections.emptyList();
		
		@SuppressWarnings("deprecation")
		List<IntelEntityType> types = (session.createQuery("SELECT r FROM IntelEntityType r join r.profiles p join p.id.profile c WHERE c IN (:profiles)", IntelEntityType.class) //$NON-NLS-1$
				.setParameter("profiles", profiles) //$NON-NLS-1$
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
				.list());
		
		types.sort((IntelEntityType a, IntelEntityType b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		return types;
	}
	
	
	/**
	 * validates if the given entity type can be deleted or not
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void canDelete(IntelEntityType type, Session session) throws Exception{
		if (!DeleteManager.canDelete(type, session)){
			throw new Exception(Messages.EntityTypeManager_DeleteError);
		}
	}
	
	/**
	 * Deletes an entity type an all associated data (relationships, entities, record links etc)
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void deleteEntityType(IntelEntityType type, Session session) throws Exception{
		
		//update relationships references to null
		Query<?> q = session.createQuery("UPDATE IntelRelationshipType SET sourceEntityType = null where sourceEntityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("UPDATE IntelRelationshipType SET targetEntityType = null where targetEntityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all entity attribute values
		q = session.createQuery("delete from IntelEntityAttributeValue ieav where ieav.id.entity in (FROM IntelEntity WHERE entityType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all relationship attribute values
		q = session.createQuery("delete from IntelEntityRelationshipAttributeValue ii where ii.id.relationship in (FROM IntelEntityRelationship r WHERE  r.sourceEntity in (FROM IntelEntity WHERE entityType = :type) or r.targetEntity in (FROM IntelEntity WHERE entityType = :type2))"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.setParameter("type2", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all relationships
		q = session.createQuery("delete from IntelEntityRelationship ii where ii.sourceEntity in (FROM IntelEntity WHERE entityType = :type) or ii.targetEntity in (FROM IntelEntity WHERE entityType = :type2)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.setParameter("type2", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all entity attachments
		q = session.createQuery("delete from IntelEntityAttachment ii where ii.id.entity in (FROM IntelEntity WHERE entityType = :type) "); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all entity records
		q = session.createQuery("delete from IntelEntityRecord ii where ii.id.entity in (FROM IntelEntity WHERE entityType = :type) "); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all locations
		q = session.createQuery("delete from IntelEntityLocation ii where ii.id.entity in (FROM IntelEntity WHERE entityType = :type) "); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all links to working sets
		q = session.createQuery("delete from IntelWorkingSetEntity ii where ii.id.entity in (FROM IntelEntity WHERE entityType = :type) "); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all entity 		
		q = session.createQuery("delete from IntelEntity WHERE entityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();

		//delete all entity type attribute 		
		q = session.createQuery("delete from IntelEntityTypeAttribute ii WHERE ii.id.entityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();

		//delete all attribute groups 		
		q = session.createQuery("delete from IntelEntityTypeAttributeGroup WHERE entityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all record source attribute values
		q = session.createQuery("delete from IntelRecordAttributeValueList ii where ii.id.value in ( From IntelRecordAttributeValue ii where ii.attribute IN (FROM IntelRecordSourceAttribute ii where ii.entityType = :type ))"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from IntelRecordAttributeValue ii where ii.attribute in ( FROM IntelRecordSourceAttribute ii where ii.entityType = :type )"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all record source attributes
		q = session.createQuery("delete from IntelRecordSourceAttribute ii where ii.entityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		session.delete(type);
	}
	
	
	/**
	 * Validates the data model active filter string
	 * 
	 * @param filterString filter string
	 * @param entityTypeAttributes valid attributes from the entity type
	 * @return
	 */
	public String validateDmAttributeFilter(String filterString, List<IntelEntityTypeAttribute> entityTypeAttributes) {
		
		if (filterString == null || filterString.isEmpty()) return null;
		
		String[] parts = filterString.split("\\" + DropItemFactory.PART_SEPARATOR); //$NON-NLS-1$
		
		
		boolean isnot = false;
		Operator lastOp = null;
		boolean first = true;
		for (String x : parts) {
			if (x.equals(Operator.NOT.getKey())) {
				isnot = true;
			}else if (x.equals(Operator.AND.getKey()) || x.equals(Operator.OR.getKey())) {
				if (lastOp != null || isnot || first) return Messages.EntityTypeManager_InvalidFilterMsg;
				lastOp = Operator.OR;
			}else  {
				if ((first && lastOp != null) || (!first && lastOp == null)) return Messages.EntityTypeManager_InvalidFilterMsg;
				
				first = false;
				String[] ex = x.split(" "); //$NON-NLS-1$
				String[] bits = ex[0].split(DropItemFactory.ITEM_SEPARATOR);
				
				if (!bits[0].equals(AdvancedEntitySearch.ATTRIBUTE_KEY)) return Messages.EntityTypeManager_InvalidFilterMsg;
				
				AttributeType type = AttributeType.parse(bits[1]);
				
				String akey = bits[2];
				IntelEntityTypeAttribute ia = null;
				for (IntelEntityTypeAttribute av : entityTypeAttributes) {
					if (av.getAttribute().getKeyId().equalsIgnoreCase(akey)) {
						ia = av;
						break;
					}
				}
				
				if (ia == null) return MessageFormat.format(Messages.EntityTypeManager_AttributeNotFound, akey);
				
				if (type == AttributeType.BOOLEAN) {
				}else if (type == AttributeType.DATE) {
					if (ex.length != 5) return Messages.EntityTypeManager_InvalidDateFilter;
					Operator op = Operator.parse(ex[1]);
					if (op != Operator.BETWEEN && op != Operator.NOT_BETWEEN) return Messages.EntityTypeManager_InvalidDateOperator;

					try {
						(new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).parse(ex[2]);
						(new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).parse(ex[4]);
					}catch (Exception ex2) {
						return Messages.EntityTypeManager_DateParseError;
					}
				}else if (type == AttributeType.LIST) {
					if (ex.length != 3) return Messages.EntityTypeManager_InvalidListFilter;
					boolean found = false;
					for (IntelAttributeListItem li : ia.getAttribute().getAttributeList()) {
						if (li.getKeyId().equalsIgnoreCase(ex[2])) {
							found = true;
							break;
						}
					}
					if (!found) return MessageFormat.format(Messages.EntityTypeManager_ListItemNotFound, ex[2], ia.getAttribute().getName());
					
				}else if (type == AttributeType.NUMERIC) {
					if (ex.length != 3) return Messages.EntityTypeManager_InvalidNumericFilter;
					
					if (ex[2].isEmpty()) return Messages.EntityTypeManager_InvalidNumberValueFilter;
					try {
						Double.parseDouble(ex[2]);
					}catch (Exception e) {
						return Messages.EntityTypeManager_NumericParseError;
					}
					
					Operator op = Operator.parse(ex[1]);
					if (op == Operator.EQUALS) {
					}else if (op == Operator.LESSTHAN) {
					}else if (op == Operator.LESSTHANEQUALS) {
					}else if (op == Operator.GREATERTHAN) {
					}else if (op == Operator.GREATERTHANEQUALS) {
					}else if (op == Operator.NOTEQUALS) {
					}else {
						return Messages.EntityTypeManager_InvalidNumberOp;
					}
					
					
				}else if (type == AttributeType.TEXT) {
					if (ex.length != 3) return Messages.EntityTypeManager_InvalidTextFilter;
					Operator op = Operator.parse(ex[1]);
					if (op == Operator.STR_EQUALS) {
					}else if (op == Operator.STR_CONTAINS) {
					}else if (op == Operator.STR_NOTCONTAINS) {
					}else {
						return Messages.EntityTypeManager_InvalidTextFilterOp;
					}					
				}
				if (lastOp != null && (lastOp != Operator.AND && lastOp != Operator.OR)) return Messages.EntityTypeManager_InvalidFilterMsg;
				lastOp = null;
				isnot = false;
				first = false;
			}
		}
		if (isnot) return Messages.EntityTypeManager_InvalidFilterMsg;
		
		return null;
		
	}
}
