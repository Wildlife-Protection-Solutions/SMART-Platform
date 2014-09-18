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
package org.wcs.smart.query.model.summary;


import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.filter.date.DayDateGroupBy;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.filter.date.MonthDateGroupBy;
import org.wcs.smart.query.model.filter.date.YearDateGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;

/**
 * Date Group By option.
 * @author egouge
 * @since 1.0.0
 */
public class DateGroupBy implements IGroupBy {

	
	private static IDateGroupBy[] GROUPBYS = {
		DayDateGroupBy.INSTANCE,
		MonthDateGroupBy.INSTANCE,
		YearDateGroupBy.INSTANCE
	};
	
	public static final DateGroupBy createGroupBy(String key){
		return new DateGroupBy(key);
	}
	
	private IDateGroupBy op;
	
	/**
	 * Creates a new date group by part.
	 * @param key the date group by key
	 */
	public DateGroupBy(String key){
		String opPart = key.split(":")[1];		 //$NON-NLS-1$
		for (IDateGroupBy pg : GROUPBYS){
			if (pg.getKey().equals(opPart)){
				this.op = pg;
			}
		}
		if (this.op == null){
			throw new IllegalStateException(MessageFormat.format(
					Messages.DateGroupBy_GroupByNoSupported, new Object[]{opPart}));
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		return "date:" + op.getKey(); //$NON-NLS-1$
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		return getKeyPart();
	}


	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		return op.getType();
	}
	
	/**
	 * @return the patrol group by option
	 */
	public IDateGroupBy getOption(){
		return this.op;
	}

	
	private IDateFilter df;
	/**
	 * Sets the date filter associated with the query using this
	 * option.  This is used to know how many block
	 * to break the query into.
	 * 
	 * @see getItems(org.hibernate.Session)
	 * 
	 * @param df
	 */
	public void setDateFilter(IDateFilter df){
		this.df = df;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		return op.getItems(session, df);
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) {
		return BasicDropItemFactory.INSTANCE.createDateGroupByDropItem(op);
	}
	
	public void visit(IGroupByVisitor visitor){
		visitor.visit(this);
	}

}
