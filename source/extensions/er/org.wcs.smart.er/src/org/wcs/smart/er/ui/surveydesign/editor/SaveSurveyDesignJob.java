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
package org.wcs.smart.er.ui.surveydesign.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job is used to save survey design object
 * 
 * @author elitvin
 */
public class SaveSurveyDesignJob extends Job {

	private SurveyDesign design;
	
    public SaveSurveyDesignJob(SurveyDesign design) {
        super(Messages.SaveSurveyDesignJob_Title);
        this.design = design;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			
			//we need to merge mission properties; for some reason the cascade doesn't work in
			//this case; probably a many to many problem;
			SurveyDesign db = (SurveyDesign) session.load(SurveyDesign.class, design.getUuid());
			List<MissionProperty> toRemove = new ArrayList<MissionProperty>();
			for (MissionProperty mp : db.getMissionProperties()){
				if (!design.getMissionProperties().contains(mp)){
					toRemove.add(mp);
					
					//remove values associated with the mission property
					String sqlquery = "SELECT mpv FROM MissionPropertyValue mpv join mpv.id.mission m join m.survey s WHERE mpv.id.missionAttribute = :attribute and s.surveyDesign = :sd"; //$NON-NLS-1$
					Query query = session.createQuery(sqlquery);
					query.setParameter("attribute", mp.getAttribute()); //$NON-NLS-1$
					query.setParameter("sd", db); //$NON-NLS-1$
					@SuppressWarnings("unchecked")
					List<MissionPropertyValue> toDelete = query.list();
					for (MissionPropertyValue v: toDelete){
						session.delete(v);
					}
				}
			}
			for (MissionProperty mp : toRemove){
				session.delete(mp);
			}
			db.getMissionProperties().removeAll(toRemove);
			session.flush();
			session.clear();
			
			// save original design
			session.saveOrUpdate(design);
			session.getTransaction().commit();
			
        	return Status.OK_STATUS;
		} catch (Exception ex){
			try{
				session.getTransaction().rollback();
			}catch (Exception ex2){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex2);
			}
			EcologicalRecordsPlugIn.displayLog(Messages.SaveSurveyDesignJob_Error + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		} finally {
			session.close();
		}
        //no need to use other status as hibernate manager will report error in case something is wrong
        return Status.CANCEL_STATUS;
    }
}	
