/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.model;

import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.query.model.Query;

@Entity
@Table(name="smart.paws_classification")
public class PawsClassification extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	public enum Type{
		DATAMODEL,QUERY
	}
	
	private String classification;
	private PawsConfiguration config;
	private String cachedLabel;
	
	private Type type;
	
	//for data model classes
	private String categoryKey;
	private String attributeKey;
	private String listItemKey;
	private String treeNodeKey;
	
	//for query classes
	private String querytype;
	private UUID queryuuid;
	private Query cachedQuery;
	
	@OneToOne(cascade= CascadeType.ALL, orphanRemoval=true)
	@JoinColumn(name="config_uuid", referencedColumnName="uuid")
	public PawsConfiguration getConfiguration() {
		return this.config;
	}
	
	public void setConfiguration(PawsConfiguration config) {
		this.config = config;
	}
	
	@Column(name="class_type")
	@Enumerated(EnumType.STRING)
	public Type getType() {
		return this.type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	@Column(name="classification")
	public String getClassification() {
		return this.classification;
	}
	
	public void setClassification(String classification) {
		this.classification = classification;
	}
	

	
	@Column(name="category_hkey")
	public String getCategoryHkey() {
		return this.categoryKey;
	}
	
	public void setCategoryHkey(String categoryKey) {
		this.categoryKey = categoryKey;
		setType(Type.DATAMODEL);
	}
	
	@Column(name="attribute_key")
	public String getAttributeKey() {
		return this.attributeKey;
	}
	
	public void setAttributeKey(String attributeKey) {
		this.attributeKey = attributeKey;
	}
	
	@Column(name="list_key")
	public String getAttributeListItemKey() {
		return this.listItemKey;
	}
	
	public void setAttributeListItemKey(String listItemKey) {
		this.listItemKey = listItemKey;
	}
	
	@Column(name="tree_hkey")
	public String getAttributeTreeNodeHkey() {
		return this.treeNodeKey;
	}
	
	public void setAttributeTreeNodeHkey(String treeNodeKey) {
		this.treeNodeKey = treeNodeKey;
	}
	
	
	@Column(name="query_type")
	public String getQueryType() {
		return this.querytype;
	}
	
	public void setQueryType(String querytype) {
		this.querytype = querytype;
		setType(Type.QUERY);
	}
	
	@Column(name="query_uuid")
	public UUID getQueryUuid() {
		return this.queryuuid;
	}
	
	public void setQueryUuid(UUID queryuuid) {
		this.queryuuid = queryuuid;
		setType(Type.QUERY);
	}
	
	@Transient
	public Query getCachedQuery() {
		return this.cachedQuery;
	}
	@Transient
	public void setCachedQuery(Query cachedQuery) {
		this.cachedQuery = cachedQuery;
	}

	@Transient
	public void cacheLabel(String label) {
		this.cachedLabel = label;
	}
	@Transient
	public String getCachedLabel() {
		return this.cachedLabel;
	}
	@Transient
	public static String createLabel(Category c, Attribute a, DmObject listortree){
		if (listortree == null) return c.getFullCategoryName();
		
		StringBuilder sb = new StringBuilder();
		sb.append(listortree.getName());
		sb.append( " (" + a.getName() + ") "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(c.getFullCategoryName());
		return sb.toString();
	}	
}
