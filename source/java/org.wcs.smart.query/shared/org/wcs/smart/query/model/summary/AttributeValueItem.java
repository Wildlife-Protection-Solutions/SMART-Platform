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

import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.query.model.filter.IValueVisitor;

/**
 * Creates a new value item that represents
 * a numeric attribute or a 
 * numeric attribute and it's associated category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AttributeValueItem implements IValueItem {
	
	/**
	 * Creates a new attribute value item of the form
	 * < ATTRIBUTE_VALUE_KEY : "attribute:n:" < AGG > ":" < ATTRIBUTE_KEY >
	 * 
	 * @param key
	 * @return
	 */
	public static AttributeValueItem createAttributeItem(String key){
		return new AttributeValueItem(key, false);
	}
	
	/**
	 * Creates a new attribute value item with category filter of the
	 * form:
	 * |    < SUM_CAT_ATT_VALUE_KEY : "category:" < DM_KEY > ":attribute:n:" < AGG > ":" < DM_KEY > >
	 * 
	 * @param key
	 * @return
	 */
	public static AttributeValueItem createCategoryAttributeItem(String key){
		return new AttributeValueItem(key, true);
	}
	
	private String key;
	protected String categoryKey = null;
	protected String attributeKey = null;;
	protected String aggregationKey = null;
//	private Aggregation aggregation = null;
	
	protected String itemKey = null;
	protected IValueItem.ValueType valueType;
	protected AttributeType attributeType;
	
	/**
	 * Creates a new value item from the given key.
	 * @param key key
	 * @param includeCategory if the key includes a category
	 */
	public AttributeValueItem(String key, boolean includeCategory){
		this.key = key;
		String[] bits = key.split(":"); //$NON-NLS-1$
		
		String attTypeKey = null;
		if (includeCategory){
			attTypeKey = bits[3];
		}else{
			attTypeKey = bits[1];
		}
		this.attributeType = Attribute.decodeAttributeTypeKey(attTypeKey);
		if(attributeType != Attribute.AttributeType.NUMERIC && 
		   attributeType != Attribute.AttributeType.LIST &&
		   attributeType != Attribute.AttributeType.TREE){ 
			throw new IllegalStateException("Non numeric attribute type not valid."); //$NON-NLS-1$
		}
		if (attributeType == AttributeType.NUMERIC){
			//numeric are of the format
			//< SUM_ATTRIBUTE_VALUE_KEY : "attribute:n:" < AGG > ":" < DM_KEY > 
			//< SUM_CAT_ATT_VALUE_KEY : "category:" < DM_KEY > ":" < SUM_ATTRIBUTE_VALUE_KEY >
			if (includeCategory){
				this.categoryKey = bits[1];
				this.attributeKey = bits[5];
				this.aggregationKey = bits[4];
			}else{
				this.attributeKey = bits[3];
				this.aggregationKey = bits[2];
			}
		}else if (attributeType == AttributeType.LIST || 
				attributeType == AttributeType.TREE ){
				//< SUM_ATTRIBUTE_VALUE_LISTTREE_KEY : "attribute:" ("t" | "l") ":sum:" ("obs" | "wp") ":" < DM_KEY > >
				//< SUM_CAT_ATT_VALUE_LISTTREE_KEY : "category:" < DM_KEY > ":" < SUM_ATTRIBUTE_VALUE_LISTTREE_KEY >
				String valueTypeKey = ""; //$NON-NLS-1$
				if (includeCategory){
					this.categoryKey = bits[1];
					this.attributeKey = bits[6];
					this.aggregationKey = bits[4];
					valueTypeKey = bits[5];
					
				}else{
					this.attributeKey = bits[4];
					this.aggregationKey = bits[2];
					valueTypeKey = bits[3];
				}
				int index = attributeKey.indexOf('.');
				if (index <= 0){
					throw new IllegalStateException("Could not parse attribute list or tree information from attribute value item.");	 //$NON-NLS-1$
				}
				String temp = attributeKey;
				attributeKey = temp.substring(0, index);
				itemKey = temp.substring(index + 1);
				
				this.valueType = ValueType.OBSERVATION;
				for (ValueType vt : ValueType.values()){
					if (vt.key.equals(valueTypeKey)){
						this.valueType = vt;
						break;
					}
				}
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	@Override
	public String asString(){
		return this.key;
	}

	/**
	 * @return the attribute key that makes up the value item
	 */
	public String getAttributeKey(){
		return this.attributeKey;
	}
	
	public ValueType getValueType(){
		return this.valueType;
	}
	
	/**
	 * 
	 * @return the attribute type
	 */
	public AttributeType getAttributeType(){
		return this.attributeType;
	}
	/**
	 * @return the category key that makes up the item or
	 * null if no category for this item
	 * 
	 */
	public String getCategoryKey(){
		return this.categoryKey;
	}
	
	/**
	 * 
	 * @return attribute item key for list and tree
	 * attributes.
	 */
	public String getItemKey(){
		return this.itemKey;
	}
		
//	/**
//	 * @return attribute aggregation 
//	 */
//	public Aggregation getAggregation() {
//		if (aggregation == null) {
//			List<Aggregation> aggs = DataModel.getAggregations();
//			for (Aggregation agg : aggs) {
//				if (agg.getName().equals(aggregationKey)) {
//					aggregation = agg;
//					break;
//				}
//			}
//		}
//		return this.aggregation;
//	}
	
	/**
	 * The key associated with the aggregation
	 * @return
	 */
	public String getAggregationKey(){
		return this.aggregationKey;
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	
	public void accept(IValueVisitor visitor){
		visitor.visit(this);
	}

}
