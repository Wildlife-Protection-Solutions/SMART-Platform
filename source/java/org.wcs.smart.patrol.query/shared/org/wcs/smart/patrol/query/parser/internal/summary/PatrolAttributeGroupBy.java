/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.parser.internal.summary;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;

/**
 * Patrol group for custom patrol attributes. Only supports
 * list patrol attributes
 * 
 * @author egouge
 * @since 1.0.0
 * 
 */
public class PatrolAttributeGroupBy implements IGroupBy {

	/**
	 * Creates a new patrol group by option
	 * @param key patrol group by key of the form
	 * "patrol:attribute:l:<key>:<listkey>,<listkey>,<listkey>"
	 * 
	 * Where <key> is the patrol attribute key and listkey
	 * is key for the individual list values
	 * 
	 * @return
	 */
	public static final PatrolAttributeGroupBy createGroupBy(String key){
		return new PatrolAttributeGroupBy(key);
	}

	private String[] items;
	private String attributeKey;
	private Attribute.AttributeType attributeType;
	
	/**
	 * Creates a new patrol group by option
	 * @param key patrol group by item key of the format "patrol:attribute:l:<key>:<item>:<item>..."
	 */
	public PatrolAttributeGroupBy(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		
		this.attributeKey = bits[3];
		this.attributeType = Attribute.decodeAttributeTypeKey(bits[2]);
		assert(attributeType == Attribute.AttributeType.LIST);
		
		if (bits.length > 4){
			items = new String[bits.length - 4];
			for (int i = 4; i < bits.length; i ++){
				items[i-4] = bits[i];
			}
		}else{
			items = null;
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		StringBuilder sb = new StringBuilder();
		sb.append("patrol:attribute:"); //$NON-NLS-1$
		sb.append(attributeType.typeKey);
		sb.append(":"); //$NON-NLS-1$
		sb.append(attributeKey);
		sb.append(":"); //$NON-NLS-1$
		return sb.toString();
	}
	

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				sb.append(items[i]);
				if (i < items.length - 1) {
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}
		
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		return GroupByType.KEY;
	}
	
	public String getAttributeKey() {
		return this.attributeKey;
	}
	
	/**
	 * 
	 * @return the patrol group by items
	 */
	public String[] getItems(){
		return this.items;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);
	}
}