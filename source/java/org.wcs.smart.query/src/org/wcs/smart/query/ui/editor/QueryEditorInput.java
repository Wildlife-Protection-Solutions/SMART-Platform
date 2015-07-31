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
package org.wcs.smart.query.ui.editor;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;

/**
 * A query input for query editors. 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryEditorInput implements IEditorInput {
	
	private UUID uuid = null;
	private String queryName = null;
	private String id = null;
	private boolean isShared;
	private IQueryType type;
	
	/**
	 * Creates a new query input.
	 * 
	 * @param type the query type
	 */
	public QueryEditorInput(IQueryType type){
		this.type = type;
	}
	
	/**
	 * Creates a new query input based on the query.
	 * 
	 * @param query the query
	 */
	public QueryEditorInput(Query query){
		this(query.getUuid(), query.getName(), query.getId(), query
				.getIsShared(), 
				QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey()));
	}
	
	/**
	 * Creates a new query input
	 * @param uuid the query uuid
	 * @param queryName the query name
	 * @param id the query id
	 * @param isShared if the query is shared or not
	 * @param type the type of query
	 */
	public QueryEditorInput(UUID uuid, 
			String queryName, 
			String id, 
			boolean isShared,
			IQueryType type){
		this.uuid = uuid;
		this.queryName = queryName;
		this.id = id;
		this.isShared = isShared;
		this.type = type;
	}
	
	/**
	 * @return the query type
	 */
	public IQueryType getType(){
		return this.type;
	}

	/**
	 * @return <code>true</code> if the query is shared <code>false</code> otherwise
	 */
	public boolean isShared(){
		return this.isShared;
	}
	/**
	 * @return the query id
	 */
	public String getId(){
		return this.id;
	}
	/**
	 * Sets the id;
	 * @param newId
	 * @return
	 */
	public void setId(String newId){
		this.id = newId;
	}
	
	/**
	 * Sets the uuid associated with the input
	 * 
	 * @param uuid
	 */
	public void setUuid(UUID uuid){
		this.uuid = uuid;
	}
	/**
	 * Updates the query name;
	 * @param name
	 */
	public void setQueryName(String name){
		this.queryName = name;
	}
	/**
	 * @return the query name
	 */
	public String getQueryName(){
		return this.queryName;
	}
	
	/**
	 * @return the query uuid or null if not set
	 */
	public UUID getUuid(){
		return this.uuid;
	}
	
	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		return false;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return QueryPlugIn.getDefault().getImageRegistry().getDescriptor(QueryPlugIn.QUERY_ICON);
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		if (queryName == null){
			return Messages.QueryInput_NullQueryName;
		}else{
			return queryName;
		}
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		if (queryName == null){
			return Messages.QueryInput_EditorNameNoQuery;
		}else{
			return MessageFormat.format(Messages.QueryInput_EditorNameQuery, new Object[]{queryName});
		}
	}
	
	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	/**
	 * Two inputs are equals if they have the same uuid;
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryEditorInput other = (QueryEditorInput) obj;
		
		if (uuid == null && other.uuid == null) return false;
		if (uuid == null) return false;
		if (!uuid.equals(other.uuid))
			return false;
		return true;
	}


}