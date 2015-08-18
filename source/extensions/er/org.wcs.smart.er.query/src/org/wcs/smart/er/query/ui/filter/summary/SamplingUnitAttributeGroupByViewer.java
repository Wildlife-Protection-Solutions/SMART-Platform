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
package org.wcs.smart.er.query.ui.filter.summary;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.query.filter.summary.SamplingUnitAttributeGroupBy;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Mission attribute group by.  Only applicable for list
 * mission attributes.
 * 
 * @author Emily
 *
 */
public class SamplingUnitAttributeGroupByViewer extends AbstractGroupByViewer<SamplingUnitAttributeGroupBy>{
	
	public SamplingUnitAttributeGroupByViewer(
			SamplingUnitAttributeGroupBy gb) {
		super(gb);
	}

	@Override
	public List<ListItem> getItems(Session session) {
		SamplingUnitAttribute su = null;
		try{
			su = getSamplingUnitAttribute(session);
		}catch (Exception ex){
			throw new RuntimeException(ex.getMessage());
		}
		String items[] = groupBy.getRawItems();
		ArrayList<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null && items.length > 0){
			for (String it : items){
				for (SamplingUnitAttributeListItem mli : su.getAttributeList()){
					if (mli.getKeyId().equals(it)){
						allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
					}
				}
			}
		}else{
			for (SamplingUnitAttributeListItem mli : su.getAttributeList()){
				allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
			}
		}
		return allItems;
	}
	
	private SamplingUnitAttribute getSamplingUnitAttribute(Session session) throws Exception{
		String attributeKey = groupBy.getKeyPart();
		List<?> items = session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.eq("keyId", attributeKey)) //$NON-NLS-1$
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.list();
		if (items.size() == 0){
			throw new Exception(MessageFormat.format(Messages.SamplingUnitAttributeGroupBy_KeyNotFound, new Object[]{attributeKey}));
		}else if (items.size() > 1){
			throw new Exception(MessageFormat.format(Messages.SamplingUnitAttributeGroupBy_InvalidKey, new Object[]{attributeKey}));
		}
		SamplingUnitAttribute ma = (SamplingUnitAttribute) items.get(0);
		return ma;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		SamplingUnitAttribute ma = null;
		try{
			ma = getSamplingUnitAttribute(session);
		}catch (Exception ex){
			return new ErrorDropItem(ex.getMessage());
		}
		String items[] = groupBy.getRawItems();
		DropItem di = SurveyDropItemFactory.INSTANCE.createSamplingUnitAttributeGroupByDropItem(ma);
		if (items != null && items.length > 0){
			di.initializeData(getItems(session));
		}else{
			di.initializeData(new ArrayList<ListItem>());
		}
		return di;
	}


}
