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
package org.wcs.smart.er.query.ui;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.QueryDefPanel;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Query proxy wrapper for survey queries that include
 * access to survey design object.
 * 
 * @author Emily
 *
 */
public class SurveyQueryProxyWrapper extends QueryProxy{

	private QueryProxy wrapper;
	private volatile SurveyDesign surveydesign;
	private String sdKey = null;
	
	public SurveyQueryProxyWrapper(QueryProxy wrap, String surveyDesignKey) {
		super(wrap.getQuery());
		this.wrapper = wrap;
		this.sdKey = surveyDesignKey;
	}

	@Override
	public void setDropItems(String panelId, Collection<DropItem> dropItems ){
		wrapper.setDropItems(panelId, dropItems);
	}
	
	@Override
	public  Collection<DropItem> getDropItems(String panelId){
		return wrapper.getDropItems(panelId);
	}
	
	@Override
	public Query getQuery(){
		return wrapper.getQuery();
	}
	
	@Override
	public IQueryType getQueryType(){
		return wrapper.getQueryType();
	}
	
	@Override
	public void dispose(){
		wrapper.dispose();
		surveydesign = null;
	}

	@Override
	public QueryDefPanel getQueryDefinitionPanel(){
		return wrapper.getQueryDefinitionPanel();
	}
	
	@Override
	public void setQueryDefinitionPanel(QueryDefPanel panel){
		wrapper.setQueryDefinitionPanel(panel);
	}
	
	@Override
	public boolean isValid(){
		return wrapper.isValid();
	}
	
	@Override
	public String getErrorMessage(){
		return wrapper.getErrorMessage();
	}

	@Override
	public void setValid(String valid){
		wrapper.setValid(valid);
	}
	
	public void setSurveyDesign(String key){
		this.surveydesign = null;
		this.sdKey = key;
	}
	public SurveyDesign getSurveyDesign(){
		if (sdKey == null) return null;
		
		if (surveydesign == null){
			synchronized (this) {
				if (surveydesign == null){
			
					Job j = new Job(Messages.SurveyObservationQuery_loadingDesignJobName){

					@Override
					protected IStatus run(IProgressMonitor monitor) {
                 	   Session s = HibernateManager.openSession();
                 	   try{
                 		   List<?> results = s.createCriteria(SurveyDesign.class)
                 				   .add(Restrictions.eq("keyId", sdKey)) //$NON-NLS-1$
                 				   .add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
                 		   if (results.size() > 0){
                 			   surveydesign = (SurveyDesign) results.get(0);
                 		   }
                 	   }finally{
                 		   s.close();
                 	   }
                 	   return Status.OK_STATUS;
					}};
					j.setSystem(true);
					j.schedule();
					try {
						j.join();
					} catch (InterruptedException e) {
						ERQueryPlugIn.log(e.getMessage(), e);
					}
				}
			}
		}
		return surveydesign;
	}
}
