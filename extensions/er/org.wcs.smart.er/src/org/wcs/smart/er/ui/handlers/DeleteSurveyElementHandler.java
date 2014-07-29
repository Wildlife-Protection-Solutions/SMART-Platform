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
package org.wcs.smart.er.ui.handlers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.SurveyListTreeNode;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Handler for deleting survey elements.
 * @author Emily
 *
 */
public class DeleteSurveyElementHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		int designCnt = 0;
		int surveyCnt = 0;
		
		
		ISelection a = HandlerUtil.getCurrentSelection(event);
		if (!(a instanceof StructuredSelection)){
			return null;
		}
		
		final List<SurveyListTreeNode> nodes = new ArrayList<SurveyListTreeNode>();
		
		StructuredSelection selection = (StructuredSelection)a;
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object delete = (Object) iterator.next();
			if (delete instanceof SurveyListTreeNode){
				SurveyListTreeNode.Type type = (((SurveyListTreeNode)delete).getType());
				nodes.add((SurveyListTreeNode)delete);
				if (type == Type.SURVEY){
					surveyCnt++;
				}else if (type == Type.SURVEY_DESIGN){
					designCnt++;
				}
			}
			
		}
		if (nodes.size() == 0) return null;
		
		StringBuilder warn = new StringBuilder();
		warn.append("Are you sure you want to delete the {0} selected elements?");
		if (designCnt > 0 || surveyCnt > 0){
			warn.append(" All data associated with the selected survey designs and/or missions will also be deleted.");
		}
		warn.append("\n\n");
		warn.append("This action cannot be undone. Are you sure you want to continue?");
		
		if (!MessageDialog.openQuestion(HandlerUtil.getActiveShell(event), "Delete", MessageFormat.format(warn.toString(), new Object[]{nodes.size()}))){
			return null;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					
					monitor.beginTask("Deleting selected items.", nodes.size());
					Session s = HibernateManager.openSession();
					try{
						for (SurveyListTreeNode node : nodes){
							monitor.subTask(MessageFormat.format("Deleting {0}", new Object[]{node.getLabel()}));
							deleteItem(node,s);
							monitor.worked(1);
						}
					}finally{
						s.close();
					}
				}
			});
		} catch (Exception e) {
			EcologicalRecordsPlugIn.displayLog("Error deleting selected items." + "\n\n" + e.getMessage(), e);
		}
		
		
		return null;
	}
	
	private void deleteItem(SurveyListTreeNode element, Session session){
		if (element.getType() == Type.SURVEY_DESIGN){
			deleteSurveyDesign(element.getUuid(), session);
		}else if (element.getType() == Type.SURVEY){
			deleteSurvey(element.getUuid(), session);
		}else if (element.getType() == Type.MISSION){
			deleteMission(element.getUuid(), session);
			
		}
	}
	
	
	/**
	 * Delete the survey design.  Includes deleteing all
	 * surveys, missions and waypoints associated with the 
	 * survey design (and filestore folder).
	 * 
	 * 
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static boolean deleteSurveyDesign(byte[] uuid, Session session) {
		String id = SmartUtils.encodeHex(uuid);
		session.beginTransaction();
		try{
			SurveyDesign design = (SurveyDesign) session.load(SurveyDesign.class, uuid);
			if (design == null){
				session.getTransaction().rollback();
				return false;
			}
		
			if (DeleteManager.canDelete(design, session)){
				List<File> dirsToDelete = new ArrayList<File>();
				
				List<Survey> surveys = session.createCriteria(Survey.class).add(Restrictions.eq("surveyDesign", design)).list();
				for (Survey survey : surveys){
				
				
					//delete all waypoints
					if (survey.getMissions() != null){
						for (Mission m : survey.getMissions()){
							dirsToDelete.add(m.getFilestoreLocation());
							if (m.getWaypoints() != null){
								for (SurveyWaypoint sw : m.getWaypoints()){
									session.delete(sw.getWaypoint());
								}
							}
						}
					}
					session.delete(survey);
				}
				session.delete(design);
			
				session.getTransaction().commit();

				
				for (File f : dirsToDelete){
					if (f.exists()){
						try{
							FileUtils.forceDelete(f);
						}catch(Exception ex){
							EcologicalRecordsPlugIn.displayLog(
									"Could not delete the filestore associated with the mission.  This directory should be deleted manually." + f.getAbsolutePath(), ex);
						}
					}
				}
				
				try{
					SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_DELETED, design);
				}catch (Exception ex){
					EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);
				}
				
				return true;
			}else{
				session.getTransaction().rollback();
			}
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(
					MessageFormat.format("Error occurred deleting survey {0}", new Object[]{id}) + "\n\n" + ex.getMessage(), ex);
			session.getTransaction().rollback();
		}
		return false;
	}
	
	/**
	 * Deletes a survey and associated missions.
	 * <p>Deletes all waypoints, then the survey and associated missions. Once
	 * complete all filestore directories associated with the delete missions
	 * are removed.
	 * </p>
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static boolean deleteSurvey(byte[] uuid, Session session) {
		String id = SmartUtils.encodeHex(uuid);
		session.beginTransaction();
		try{
			Survey survey = (Survey) session.load(Survey.class, uuid);
			if (survey == null){
				session.getTransaction().rollback();
				return false;
			}
		
			if (DeleteManager.canDelete(survey, session)){
				List<File> dirsToDelete = new ArrayList<File>();
				//delete all waypoints
				if (survey.getMissions() != null){
					for (Mission m : survey.getMissions()){
						dirsToDelete.add(m.getFilestoreLocation());
						if (m.getWaypoints() != null){
							for (SurveyWaypoint sw : m.getWaypoints()){
								session.delete(sw.getWaypoint());
							}
						}
					}
				}
				//delete surveys
				session.delete(survey);
				session.getTransaction().commit();

				//delete filestore dir
				for (File f : dirsToDelete){
					if (f.exists()){
						try{
							FileUtils.forceDelete(f);
						}catch(Exception ex){
							EcologicalRecordsPlugIn.displayLog(
									"Could not delete the filestore associated with the mission.  This directory should be deleted manually." + f.getAbsolutePath(), ex);
						}
					}
				}
				
				//fire events
				try{
					SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DELETED, survey);
				}catch (Exception ex){
					EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);
				}
				return true;
			}else{
				session.getTransaction().rollback();
			}
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(
					MessageFormat.format("Error occurred deleting survey {0}", new Object[]{id}) + "\n\n" + ex.getMessage(), ex);
			session.getTransaction().rollback();
		}
		return false;
	}
	
	
	
	/**
	 * Deletes a mission.
	 * <p>Loads the mission from the database, deletes 
	 * all waypoints associated with the mission,
	 * deletes the mission, then removes the filestore
	 * folder.</p>
	 * 
	 * @param uuid
	 * @param session
	 * @return
	 */
	public static boolean deleteMission(byte[] uuid, Session session){
		String id = SmartUtils.encodeHex(uuid);
		session.beginTransaction();
		try{
			Mission mission = (Mission) session.load(Mission.class, uuid);
			if (mission == null){
				session.getTransaction().rollback();
				return false;
			}
			id = mission.getId();

			File fileStore = mission.getFilestoreLocation();
			
			if (DeleteManager.canDelete(mission, session)){
				
				//waypoint delete not cascaded so we need to delete
				//explicitly
				if (mission.getWaypoints() != null){
					for (SurveyWaypoint w : mission.getWaypoints()){
						session.delete(w.getWaypoint());
					}
				}
				//delete mission
				session.delete(mission);
				session.getTransaction().commit();
				
				//delete filestore
				try{
					if (fileStore.exists()){
						FileUtils.forceDelete(fileStore);
					}
				}catch(Exception ex){
					EcologicalRecordsPlugIn.displayLog(
							"Could not delete the filestore associated with the mission.  This directory should be deleted manually." + fileStore.getAbsolutePath(), ex);
				}
				
				//fire events
				try{
					SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_DELETED, mission);
				}catch (Exception ex){
					EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);
				}
				return true;
				
			}else{
				session.getTransaction().rollback();
			}
		}catch (Exception ex){
			EcologicalRecordsPlugIn.displayLog(
					MessageFormat.format("Error occurred deleting mission {0}", new Object[]{id}) + "\n\n" + ex.getMessage(), ex);
			session.getTransaction().rollback();
		}
		return false;
		
		
	}

}
