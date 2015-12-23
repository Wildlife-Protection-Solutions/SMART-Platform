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
package org.wcs.smart.entity.query.exportimport;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeGroupBy;
import org.wcs.smart.entity.query.parser.internal.EntityTypeFilter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.filter.QueryDefinitionValidator;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;

/**
 * Tools for patrol query validation.
 * 
 * @author Emily
 *
 */
public class EntityQueryValidator extends QueryDefinitionValidator {

	private ConservationArea conservationArea;

	/**
	 * @param session database session
	 * 
	 */
	public EntityQueryValidator( Session session ){
		this(SmartDB.getCurrentConservationArea(), session);
	}

	/**
	 * Creates an entity query validator for a given conservation area
	 * @param session database session
	 * 
	 */
	public EntityQueryValidator( ConservationArea ca, Session session ){
		super(session, QueryDataModelManager.getManager(ca), ca);
		this.conservationArea = ca;
	}
	
	/**
	 * Validates a filter item against the database.
	 * 
	 * @param filter the filter to validate
	 * 
	 * @throws Exception if filter cannot be validated
	 */
	@Override
	public List<String> validate(IFilter filter) throws Exception{
		List<String> warnings = super.validate(filter);
		
		FilterValidatorVisitor vv = new FilterValidatorVisitor();
		filter.accept(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		return warnings;
	}
		
	
	/**
	 * Validates a group by item.
	 * @param item
	 * @return
	 * @throws Exception
	 */
	public List<String> validate(IGroupBy item) throws Exception{
		List<String> warnings = super.validate(item);
		
		GroupByValidatorVisitor vv = new GroupByValidatorVisitor();
		item.visit(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		return warnings;
	}
	
	
	/**
	 * Validates value items
	 */
	@Override
	public List<String> validate(IValueItem item) throws Exception{
		return super.validate(item);
	}
	
	
	
	
	class FilterValidatorVisitor implements IFilterVisitor{
		private Exception ex;
		
		@Override
		public void visit(IFilter filter) {
			if (ex != null) return;
			try{
				if (filter instanceof EntityTypeFilter){
					validateEntity(((EntityTypeFilter) filter).getEntityTypeKey());
				}else if (filter instanceof EntityAttributeFilter){
					EntityAttributeFilter item = (EntityAttributeFilter)filter;
					validateEntity(item.getEntityKey());
					EntityAttribute ea = EntityHibernateManager.getInstance(conservationArea).getEntityAttribute(((EntityAttributeFilter) filter).getEntityKey(), ((EntityAttributeFilter) filter).getEntityAttributeKey(), session);
					if (ea == null){
						throw new Exception(MessageFormat.format(Messages.EntityQueryValidator_EntityAttributeNotFound, new Object[]{item.getEntityAttributeKey(), item.getEntityKey()}));
					}
					
					if (ea.getDmAttribute().getType() == AttributeType.LIST) {
						validateAttributeListItem((String) item.getValue(), ea.getDmAttribute().getKeyId());
					} else if (ea.getDmAttribute().getType() == AttributeType.TREE) {
						validateAttributeTreeNode((String) item.getValue(), ea.getDmAttribute().getKeyId());
					}					
				}
			}catch(Exception ex){
				this.ex = ex;
			}
		}
	}
	
	
	class GroupByValidatorVisitor implements IGroupByVisitor{
		private Exception ex;
		
		@Override
		public void visit(IGroupBy filter) {
			if (ex != null)
				return;
			try {
				if (filter instanceof EntityAttributeGroupBy) {
					EntityAttributeGroupBy gb = (EntityAttributeGroupBy) filter;
					validateEntity(gb.getEntityKey());
					
					EntityAttribute ea = EntityHibernateManager.getInstance(conservationArea).getEntityAttribute(((EntityAttributeGroupBy) filter).getEntityKey(), ((EntityAttributeGroupBy) filter).getEntityAttributeKey(), session);
					if (ea == null){
						throw new Exception(MessageFormat.format(Messages.EntityQueryValidator_EntityAttributeNotFound, new Object[]{gb.getEntityAttributeKey(), gb.getEntityKey()}));
					}
					
					if (gb.getFilterKeys() != null){
						if (ea.getDmAttribute().getType() == AttributeType.LIST) {
							for (String key : gb.getFilterKeys()){
								validateAttributeListItem(key, ea.getDmAttribute().getKeyId());
							}
						}else if (ea.getDmAttribute().getType() == AttributeType.TREE){
							for (String key : gb.getFilterKeys()){
								validateAttributeTreeNode(key, ea.getDmAttribute().getKeyId());
							}
						}
					}
				}
			} catch (Exception ex) {
				this.ex = ex;
			}
		}
	}
	
	/**
	 * Validates that a category with the given hkey exists
	 * @param hkey
	 * @param session
	 * @throws Exception
	 */
	public void validateEntity(String entityTypeKey) throws Exception{
		EntityType et = EntityHibernateManager.getInstance(conservationArea).getEntityType(entityTypeKey, session);
		if (et == null){
			throw new Exception (MessageFormat.format(Messages.EntityQueryValidator_EntityTypeNotFound, new Object[]{entityTypeKey}));
		}
	}
}
