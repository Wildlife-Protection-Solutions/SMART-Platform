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
package org.wcs.smart.er.query.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Mission attribute filter.
 * 
 * @author Emily
 *
 */
public class MissionAttributeFilter implements IFilter {

	public static final String QUERY_KEY = "s:missionproperty"; //$NON-NLS-1$
	
	/**
	 * Creates a filter.
	 * 
	 * @return
	 */
	public static MissionAttributeFilter createFilter(String key, Operator op, Object value){
		String[] bits = key.split(":"); //$NON-NLS-1$
		return new MissionAttributeFilter(bits[2], bits[3], op, value);
	}

	
	private Operator op;
	private Object value;
	
	private String typeKey;
	private String key;
	
	public MissionAttributeFilter(String typeKey, String key, Operator op, Object value){
		this.typeKey = typeKey;
		this.key = key;
		this.op = op;
		this.value = value;
	}
	
	@Override
	public String asString() {
		String fullIdentifier = QUERY_KEY + ":" + typeKey + ":" + key ; //$NON-NLS-1$ //$NON-NLS-2$
		if (typeKey.equals(AttributeType.NUMERIC.typeKey)){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((Double)value).toString();  //$NON-NLS-1$  //$NON-NLS-2$
		}else if (typeKey.equals(AttributeType.TEXT.typeKey)){
			return fullIdentifier + " " + op.asSmartValue() + " \"" + ((String)value) + "\"";  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}else if (typeKey.equals(AttributeType.LIST.typeKey)){
			return fullIdentifier + " " + op.asSmartValue() + " " + ((String)value);  //$NON-NLS-1$  //$NON-NLS-2$  
		}
		return null;
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		try{
			MissionAttribute ma = (MissionAttribute) session.createCriteria(MissionAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("keyId", key)).list().get(0); //$NON-NLS-1$
			DropItem di = SurveyDropItemFactory.INSTANCE.createMissionAttributeDropItem(ma);
			
			if (typeKey.equals(AttributeType.NUMERIC.typeKey) ||
					typeKey.equals(AttributeType.TEXT.typeKey)){
				di.initializeData(new Object[]{op.asSmartValue(), value.toString()}); 
			}else if (typeKey.equals(AttributeType.LIST.typeKey)){
				
				boolean ok = false;
				for (MissionAttributeListItem item : ma.getAttributeList()){
					if (item.getKeyId().equals(value)){
						ok = true;
						di.initializeData(new ListItem(item.getUuid(), item.getName(), item.getKeyId()));
					}
				}
				if (!ok){
					return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionAttributeFilter_ListItemNotFoundError, new Object[]{key}))};		
				}
			}
			return new DropItem[]{di};
		}catch (Exception ex){
			ERQueryPlugIn.log(ex.getMessage(), ex);
			return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionAttributeFilter_AttributeNotFoundError, new Object[]{key}))};
		}
	}

}
