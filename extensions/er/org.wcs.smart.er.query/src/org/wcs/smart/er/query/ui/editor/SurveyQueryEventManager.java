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
import org.wcs.smart.query.model.Query;

/**
 * Survey query event manager.
 * 
 * @author Emily
 *
 */
public class SurveyQueryEventManager {

	private static SurveyQueryEventManager instance;
	
	private List<SurveyDesignChangeListener> listeners;
	
	private SurveyQueryEventManager(){
		listeners = new ArrayList<SurveyQueryEventManager.SurveyDesignChangeListener>();
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
	public void addSurveyDesignChangeListener(SurveyDesignChangeListener listener){
		listeners.add(listener);
		
	}
	
	/**
	 * Removes a new listener
	 * @param listener
	 */
	public void removeSurveyDesignChangeListener(SurveyDesignChangeListener listener){
		listeners.remove(listener);
	}
	
	/**
	 * Fires change listener 
	 * @param newDesign new survey design
	 * @param query current query
	 */
	public void fireSurveyDesignChange(SurveyDesign newDesign, Query query){
		for (SurveyDesignChangeListener ll : listeners){
			ll.surveyDesignChange(newDesign, query);
		}
	}
	
	/**
	 * Survey design change listener.
	 *  
	 * @author Emily
	 *
	 */
	public interface SurveyDesignChangeListener{
		/**
		 * The survey design has changed.
		 * @param newDesign
		 * @param query
		 */
		public void surveyDesignChange(SurveyDesign newDesign, Query query);
	}
}
