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

import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;

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
		return "sgb:missionproperty:l:" + attributeKey + ":"; //$NON-NLS-1$ //$NON-NLS-2$
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
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);		
	}
	
}
