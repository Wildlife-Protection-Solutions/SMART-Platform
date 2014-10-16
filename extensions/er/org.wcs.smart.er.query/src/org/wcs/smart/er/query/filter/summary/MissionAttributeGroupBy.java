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
package org.wcs.smart.er.query.filter.summary;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;
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
public class MissionAttributeGroupBy implements IGroupBy{
	
	/**
	 * s:missionproperty:l:" < DM_KEY > ":" ( < DM_KEY > )? (":" < DM_KEY > )* ) 
	 * @param key
	 * @return
	 */
	public static MissionAttributeGroupBy createGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		String attributeKey = bits[3];
		
		if (bits.length > 4){
			String items[] = new String[bits.length - 4];
			for (int i = 4; i < bits.length; i ++){
				items[i-4] = bits[i];
			}
			return new MissionAttributeGroupBy(attributeKey, items);
		}else{
			return new MissionAttributeGroupBy(attributeKey, null);
		}
	}
	
	private String attributeKey; 
	private String[] items;
	
	private MissionAttributeGroupBy(String attributeKey, String[] items){
		this.attributeKey = attributeKey;
		this.items = items;
	}
	
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		if (items != null){
			for (String it : items){
				sb.append(it);
				sb.append(":"); //$NON-NLS-1$
			}
			if (items.length > 0){
				sb.deleteCharAt(sb.length() - 1);
			}
		}
		return sb.toString();
	}
	
	public String getAttributeKey(){
		return this.attributeKey;
	}

	@Override
	public String getKeyPart() {
		return "sgb:missionproperty:l:"; //$NON-NLS-1$
	}
	/**
	 * 
	 * @return the raw group by items
	 */
	public String[] getRawItems(){
		return this.items;
	}

	@Override
	public GroupByType getType() {
		return GroupByType.KEY;
	}

	@Override
	public List<ListItem> getItems(Session session) {
		MissionAttribute ma = null;
		try{
			ma = getMissionAttribute(session);
		}catch (Exception ex){
			throw new RuntimeException(ex.getMessage());
		}
		ArrayList<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null && items.length > 0){
			for (String it : items){
				for (MissionAttributeListItem mli : ma.getAttributeList()){
					if (mli.getKeyId().equals(it)){
						allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
					}
				}
			}
		}else{
			for (MissionAttributeListItem mli : ma.getAttributeList()){
				allItems.add(new ListItem(null, mli.getName(), mli.getKeyId()));
			}
		}
		return allItems;
	}
	
	private MissionAttribute getMissionAttribute(Session session) throws Exception{
		List<?> items = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("keyId", attributeKey)) //$NON-NLS-1$
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.list();
		if (items.size() == 0){
			throw new Exception(MessageFormat.format(Messages.MissionAttributeGroupBy_AttributeNotFound, new Object[]{attributeKey}));
		}else if (items.size() > 1){
			throw new Exception(MessageFormat.format(Messages.MissionAttributeGroupBy_InvalidKey, new Object[]{attributeKey}));
		}
		MissionAttribute ma = (MissionAttribute) items.get(0);
		return ma;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		MissionAttribute ma = null;
		try{
			ma = getMissionAttribute(session);
		}catch (Exception ex){
			return new ErrorDropItem(ex.getMessage());
		}
		DropItem di = SurveyDropItemFactory.INSTANCE.createMissionAttributeGroupByDropItem(ma);
		if (items != null && items.length > 0){
			di.initializeData(getItems(session));
		}else{
			di.initializeData(new ArrayList<ListItem>());
		}
		return di;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);		
	}



}
