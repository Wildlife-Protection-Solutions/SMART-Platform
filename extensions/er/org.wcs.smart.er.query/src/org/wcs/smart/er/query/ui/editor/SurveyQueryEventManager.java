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
package org.wcs.smart.er.query.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.model.ISurveyQuery;

/**
 * Survey query event manager.
 * 
 * @author Emily
 *
 */
public class SurveyQueryEventManager {

	private static SurveyQueryEventManager instance;
	
	private List<QuerySurveyDesignChangeListener> listeners;
	
	private SurveyQueryEventManager(){
		listeners = new ArrayList<SurveyQueryEventManager.QuerySurveyDesignChangeListener>();
	}
	
	public static SurveyQueryEventManager getInstance(){
		if (instance == null){
			instance = new SurveyQueryEventManager();
		}
		return instance;
	}
	
	/**
	 * Adds a new listener
	 * @param listener
	 */
	public void addSurveyDesignChangeListener(QuerySurveyDesignChangeListener listener){
		listeners.add(listener);
		
	}
	
	/**
	 * Removes a new listener
	 * @param listener
	 */
	public void removeSurveyDesignChangeListener(QuerySurveyDesignChangeListener listener){
		listeners.remove(listener);
	}
	
	/**
	 * Fires change listener 
	 * @param newDesign new survey design
	 * @param query current query
	 */
	public void fireQuerySurveyDesignChange(ISurveyQuery query, SurveyDesign newDesign){
		for (QuerySurveyDesignChangeListener ll : listeners){
			ll.surveyDesignChange(query, newDesign);
		}
	}
	
	/**
	 * Survey design change listener.
	 *  
	 * @author Emily
	 *
	 */
	public interface QuerySurveyDesignChangeListener{
		/**
		 * The survey design associated with the query has
		 * been modified.
		 * 
		 * @param query new query that was modified
		 * @param newDesign the new survey design to be associated with the query.  The query may not be updated with the new survey design at this point
		 */
		public void surveyDesignChange(ISurveyQuery query, SurveyDesign newDesign);
	}
}
