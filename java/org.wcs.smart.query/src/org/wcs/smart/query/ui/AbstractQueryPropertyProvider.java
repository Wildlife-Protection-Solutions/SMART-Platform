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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;

/**
 * Query property provider.  To display additiona
 * query properties in the query properties window
 * @author egouge
 * @since 1.0.0
 */
public abstract class AbstractQueryPropertyProvider {

	private String name = null;
	private List<Listener> changeListeners = new ArrayList<Listener>();
	
	public AbstractQueryPropertyProvider(){
		
	}
	
	
	/**
	 * @param name set the property name
	 */
	public void setName(String name){
		this.name = name;
	}
	/**
	 * @return the property name
	 */
	public String getName(){
		return this.name;
	}
	
	public void addChangeListener(Listener listener){
		changeListeners.add(listener);
	}
	public void clearChangeListener(){
		changeListeners.clear();
	}
	
	public void fireChangeMade(){
		for(Listener listener : changeListeners){
			listener.handleEvent(null);
		}
	}
	/**
	 * @param query
	 * @return <code>true</code> if property is valid 
	 * for given query type
	 */
	public abstract boolean isValid(IQueryType query);
	
	/**
	 * @param query
	 * @return the query property for the given query
	 */
	public String getValue(Query query){
		return "";
	}
	
	public Composite createComposite(Composite parent, Query query){
		return null;
	}
	
	/**
	 * Called when save is selected on the dialog;
	 * allows query properties to be updated.
	 */
	public String save(Query query, Session session){
		return null;
	}
}
