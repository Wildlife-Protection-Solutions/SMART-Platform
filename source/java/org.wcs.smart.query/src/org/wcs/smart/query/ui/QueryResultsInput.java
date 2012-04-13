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
package org.wcs.smart.query.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.query.QueryPlugIn;

/**
 * Editor input for query editor.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryResultsInput implements IEditorInput {

	private byte[] uuid = null;
	private String queryName = null;
	
	public QueryResultsInput(){
	}
	
	public QueryResultsInput(byte[] uuid, String queryName){
		this.uuid = uuid;
		this.queryName = queryName;
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
	public byte[] getUuid(){
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
		return JFaceResources.getImageRegistry().getDescriptor(QueryPlugIn.QUERY_ICON);
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		if (queryName == null){
			return "Query";
		}else{
			return "Query : " + queryName;
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
			return "Smart Query Editor";
		}else{
			return "Smart Query Editor - " + queryName;
		}
	}

}