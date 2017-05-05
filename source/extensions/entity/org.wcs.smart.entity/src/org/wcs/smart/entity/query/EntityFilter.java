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
package org.wcs.smart.entity.query;

import java.util.List;

import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

/**
 * Filter for filtering individual entities.
 * 
 * @author Emily
 *
 */
public class EntityFilter implements IFilter {

	/*
	 * Filter options
	 */
	public enum EntityFilterType{
		ALL(Messages.EntityFilter_AllEntitiesLabel),
		ALLACTIVE(Messages.EntityFilter_ActiveOnlyEntitiesLabel),
		CUSTOM(Messages.EntityFilter_CustomEntityLabel);
		
		private String guiName;
		private EntityFilterType(String guiName){
			this.guiName = guiName;
		}
		public String getGuiName(){
			return this.guiName;
		}
	};
	
	private EntityFilterType type;
	private List<Entity> customEntities;
	
	/**
	 * Creates a new entity filter
	 * @param type type of filter
	 */
	public EntityFilter(EntityFilterType type){
		this.type = type;
		customEntities = null;
	}
	
	/**
	 * Creates a new custom filter with the provided custom entity filter
	 * list.
	 * @param type
	 * @param entities
	 */
	public EntityFilter(EntityFilterType type, List<Entity> entities){
		this(type);
		this.customEntities = entities;
	}
	
	/**
	 * 
	 * @return filter type
	 */
	public EntityFilterType getType(){
		return this.type;
	}
	
	/**
	 * 
	 * @return list of custom entities to filter on 
	 */
	public List<Entity> getEntities(){
		return this.customEntities;
	}
		
	@Override
	public String asString() {
		return type.getGuiName();
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

}
