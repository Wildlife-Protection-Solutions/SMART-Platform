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
package org.wcs.smart.intelligence.query;

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.patrol.query.ext.IExtensionFilter;
import org.wcs.smart.patrol.query.ext.IExtensionFilterViewer;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SharedUtils;

/**
 * Intelligence contribution for the Patrol section of a "Query Filter" view.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligencePatrolQueryFilterViewer implements IExtensionFilterViewer {

	private IntelligencePatrolQueryOption intelligenceOption = new IntelligencePatrolQueryOption();
	
	/**
	 * @see org.wcs.smart.patrol.query.ext.IExtensionFilterViewer#asSql(java.util.HashMap, org.wcs.smart.query.model.filter.IFilter)
	 */
	@Override
	public String asSql(IQueryEngine engine, Session session, IFilter filter ){
		if (!(filter instanceof IntelligencePatrolQueryFilter)){
			return null;
		}
		
		IntelligencePatrolQueryFilter qFilter = (IntelligencePatrolQueryFilter)filter;
		String prefix = engine.tablePrefix(intelligenceOption.getPatrolAttributeClass());
		String v = SharedUtils.stripQuotes((String)qFilter.getValue());
		//if v is empty this means that this is "Any Plan" case
		String intelPart = !qFilter.isAnyIntelligence() ? " AND p2i.intelligence_uuid = x'" + v + "'" : "";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String sql = "EXISTS (SELECT * FROM smart.patrol_intelligence p2i WHERE p2i.patrol_uuid = " + prefix + ".uuid" + intelPart + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return sql;
	}

	@Override
	public String getName() {
		return intelligenceOption.getGuiName(Locale.getDefault());
	}

	@Override
	public DropItem asDropItem() {
		DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolFilterDropItem(intelligenceOption);
		it.initializeData(new Object[]{new PatrolIntelligencePatrolData()});
		return it;
	}

	@Override
	public Image getImage() {
		return IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INTELLIGENCE_ICON);
	}

	@Override
	public Class<? extends IExtensionFilter> getFilterClass() {
		return IntelligencePatrolQueryFilter.class;
	}

	@Override
	public DropItem[] getDropItems(IFilter filter, Session session) {
		if (!(filter instanceof IntelligencePatrolQueryFilter)){
			return null;
		}
		IntelligencePatrolQueryFilter f = (IntelligencePatrolQueryFilter)filter;
		
		DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolFilterDropItem(intelligenceOption);
		
		String id = SharedUtils.stripQuotes((String)f.getValue());
		ListItem listItem;
		try {
			listItem = f.isAnyIntelligence() ? PatrolIntelligencePatrolData.ANY_INTELLIGENCE_ITEM
					: IntelligenceHibernateManager.getIntelligence(session, id);

			it.initializeData(new Object[]{new PatrolIntelligencePatrolData(), listItem});
		} catch (Exception e) {
			IntelligencePlugIn.log(e.getMessage(), e);
			return new DropItem[]{new ErrorDropItem(Messages.IntelligencePatrolQueryFilterViewer_ParseError)};
		}
		return new DropItem[]{it};
	}


}
