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
package org.wcs.smart.entity.query.engine;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.entity.query.parser.internal.EntityTypeFilter;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

/**
 * Attribute Filter collector. This visitor collects
 * attribute filters.
 * 
 * @author Emily
 *
 */
public class AttributeFilterCollectorVisitor implements IFilterVisitor{

	private HashSet<AttributeInfo> filters = new HashSet<AttributeInfo>();

	private Connection c;
	private DerbyEntityQueryEngine engine;
	public AttributeFilterCollectorVisitor(Connection c, DerbyEntityQueryEngine engine){
		this.c = c;
		this.engine = engine;
	}
	
	
	@Override
	public void visit(IFilter filter) {
		if (filter instanceof AttributeFilter){
			AttributeFilter f = (AttributeFilter) filter;
			AttributeInfo in = new AttributeInfo(f.getAttributeKey(),f.getAttributeType());
			if (!filters.contains(in)){
				filters.add(in);
			}
			
		}else if (filter instanceof EntityAttributeFilter){
			EntityAttributeFilter f = (EntityAttributeFilter) filter;
			AttributeInfo in = new AttributeInfo(engine.getEntityDmAttributeKey(f.getEntityKey(), c), AttributeType.LIST);
			//we need to add the dm attribute associated with the entity type
			if (!filters.contains(in)){
				filters.add(in);
			}
		}
	}
	
	/**
	 * 
	 * @return list of attribute filters found
	 */
	public Set<AttributeInfo> getAttributeInfo(){
		return this.filters;
	}
}

