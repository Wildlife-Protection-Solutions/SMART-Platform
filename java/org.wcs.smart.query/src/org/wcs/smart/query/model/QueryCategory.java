package org.wcs.smart.query.model;

import java.util.ArrayList;
import java.util.List;

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
/**
 * A grouping of queries into categories.
 * 
 * @author Emily
 *
 */
public class QueryCategory {

	private String name;
	private String id;
	private String htmlDescription;
	private List<IQueryType> qtypes;
	
	/**
	 * Creates a new query category
	 * 
	 * @param id  category id
	 * @param name category name
	 * @param htmlDescription description string
	 */
	public QueryCategory(String id, String name, String htmlDescription) {
		super();
		this.name = name;
		this.id = id;
		this.htmlDescription = htmlDescription;
		qtypes = new ArrayList<IQueryType>();
	}

	/**
	 * Adds a query type to the query category
	 * 
	 * @param type
	 */
	public void addQueryType(IQueryType type){
		qtypes.add(type);
	}
	
	/**
	 * 
	 * @return all query types that have been registered with the category
	 */
	public List<IQueryType> getTypes(){
		return this.qtypes;
	}
	
	/**
	 * Query category name
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Query category id
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Query category html description string
	 * @return
	 */
	public String getHtmlDescription() {
		return htmlDescription;
	}
	
}
