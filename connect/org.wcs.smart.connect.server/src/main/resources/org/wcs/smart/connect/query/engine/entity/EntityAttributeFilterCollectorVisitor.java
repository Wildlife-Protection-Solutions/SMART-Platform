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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.Connection;
import java.util.Set;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.query.common.engine.visitors.AttributeFilterCollectorVisitor;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;

/**
 * Attribute Filter collector. This visitor collects
 * attribute filters.
 * 
 * @author Emily
 *
 */
public class EntityAttributeFilterCollectorVisitor extends AttributeFilterCollectorVisitor{

	private Connection c;
	private AbstractQueryEngine engine;
	private ConservationAreaFilter caFilter;
	
	public EntityAttributeFilterCollectorVisitor(Connection c, ConservationAreaFilter caFilter, AbstractQueryEngine engine){
		super();
		this.c = c;
		this.engine = engine;
		this.caFilter = caFilter;
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
			AttributeInfo in = new AttributeInfo(engine.getEntityDmAttributeKey(f.getEntityKey(), caFilter, c), AttributeType.LIST);
			//we need to add the dm attribute associated with the entity type
			if (!filters.contains(in)){
				filters.add(in);
			}
		}
	}
	
}

