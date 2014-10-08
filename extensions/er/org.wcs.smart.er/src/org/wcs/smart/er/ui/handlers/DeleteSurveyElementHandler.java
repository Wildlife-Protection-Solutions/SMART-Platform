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
import org.wcs.smart.er.SurveyPermissionManager;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.SurveyListTreeNode;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.util.SmartUtils;

/**
 * Handler for deleting survey elements.
 * @author Emily
 *
 */
public class DeleteSurveyElementHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection a = HandlerUtil.getCurrentSelection(event);
		if (!(a instanceof StructuredSelection)){
			return null;
		}
		
		final List<SurveyListTreeNode> nodes = new ArrayList<SurveyListTreeNode>();
		final List<SurveyDesignEditorInput> designs = new ArrayList<SurveyDesignEditorInput>();
		StructuredSelection selection = (StructuredSelection)a;
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object delete = (Object) iterator.next();
			if (delete instanceof SurveyListTreeNode){
				nodes.add((SurveyListTreeNode)delete);
			}else if (delete instanceof SurveyDesignEditorInput){
				designs.add((SurveyDesignEditorInput) delete);
			}
			
		}

		if (nodes.size() > 0){
		
			StringBuilder warn = new StringBuilder();
			warn.append(Messages.DeleteSurveyElementHandler_ConfirmDelete1);
			warn.append(Messages.DeleteSurveyElementHandler_ConfirmDelete2);
			warn.append("\n\n"); //$NON-NLS-1$
			warn.append(Messages.DeleteSurveyElementHandler_ConfirmDelete3);
		
			if (!MessageDialog.openQuestion(HandlerUtil.getActiveShell(event), Messages.DeleteSurveyElementHandler_DeleteDialogTitle, MessageFormat.format(warn.toString(), new Object[]{nodes.size()}))){
				return null;
			}
		
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));
			try {
				pmd.run(true, false, new IRunnableWithProgress() {
				
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					
						monitor.beginTask(Messages.DeleteSurveyElementHandler_ProgressTaskName, nodes.size());
						Session s = HibernateManager.openSession();
						try{
							for (SurveyListTreeNode node : nodes){
								monitor.subTask(MessageFormat.format(Messages.DeleteSurveyElementHandler_ProgressItem, new Object[]{node.getLabel()}));
								deleteItem(node,s);
								monitor.worked(1);
							}
						}finally{
							s.close();
						}
					}
				});
			} catch (Exception e) {
				EcologicalRecordsPlugIn.displayLog(Messages.DeleteSurveyElementHandler_DeleteError + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
			}
		}
		if (designs.size() > 0){

			StringBuilder warn = new StringBuilder();
			warn.append(Messages.DeleteSurveyElementHandler_Confirm4);
			warn.append(Messages.DeleteSurveyElementHandler_Confirm5);
			warn.append("\n\n"); //$NON-NLS-1$
			warn.append(Messages.DeleteSurveyElementHandler_Confirm6);
		
			if (!MessageDialog.openQuestion(HandlerUtil.getActiveShell(event), Messages.DeleteSurveyElementHandler_DeleteDialogTitle, MessageFormat.format(warn.toString(), new Object[]{designs.size()}))){
				return null;
			}
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));
			try {
				pmd.run(true, false, new IRunnableWithProgress() {
				
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					
						monitor.beginTask(Messages.DeleteSurveyElementHandler_ProgressTaskName, nodes.size());
						Session s = HibernateManager.openSession();
						try{
							for (SurveyDesignEditorInput design : designs){
								monitor.subTask(MessageFormat.format(Messages.DeleteSurveyElementHandler_ProgressItem, new Object[]{design.getName()}));
								deleteSurveyDesign(design.getUuid(), s);
								monitor.worked(1);
							}
						}finally{
							s.close();
						}
					}
				});
			} catch (Exception e) {
				EcologicalRecordsPlugIn.displayLog(Messages.DeleteSurveyElementHandler_DeleteError + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
			}
		}
		
		
		return null;
	}
	
	private void deleteItem(SurveyListTreeNode element, Session session){
		if (element.getType() == Type.SURVEY){
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
	@SuppressWarnings("unchecked")
	public static boolean deleteSurveyDesign(byte[] uuid, Session session) {
		String id = SmartUtils.encodeHex(uuid);
		session.beginTransaction();
		try{
			
			String error = SurveyPermissionManager.INSTANCE.canDeleteSurveyDesign();
			if (error != null){
				throw new Exception(error);
			}
			SurveyDesign design = (SurveyDesign) session.load(SurveyDesign.class, uuid);
			if (design == null){
				session.getTransaction().rollback();
				return false;
			}
		
			if (DeleteManager.canDelete(design, session)){
				List<File> dirsToDelete = new ArrayList<File>();
				
				List<Survey> surveys = session.createCriteria(Survey.class)
						.add(Restrictions.eq("surveyDesign", design)).list(); //$NON-NLS-1$
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
				//delete sampling unit
				List<SamplingUnit> units = session.createCriteria(SamplingUnit.class)
						.add(Restrictions.eq("surveyDesign", design)).list(); //$NON-NLS-1$
				for (SamplingUnit unit:  units){
					session.delete(unit);
				}
				
				session.delete(design);
				session.getTransaction().commit();
				
				for (File f : dirsToDelete){
					if (f.exists()){
						try{
							FileUtils.forceDelete(f);
						}catch(Exception ex){
							EcologicalRecordsPlugIn.displayLog(
									Messages.DeleteSurveyElementHandler_FsSurveyDesignDeleteError + f.getAbsolutePath(), ex);
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
					MessageFormat.format(Messages.DeleteSurveyElementHandler_SurveyDesignDeleteError, new Object[]{id}) + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
			id = survey.getId();
			
			ObservationOptions ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session);
			String error = SurveyPermissionManager.INSTANCE.canEditSurvey(survey, ops);
			if (error != null){
				throw new Exception(error);
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
									Messages.DeleteSurveyElementHandler_FsSurveyDeleteError + f.getAbsolutePath(), ex);
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
					MessageFormat.format(Messages.DeleteSurveyElementHandler_SurveyDeleteError, new Object[]{id}) + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
			
			ObservationOptions ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session);
			String error = SurveyPermissionManager.INSTANCE.canEditMission(mission, ops);
			if (error != null){
				throw new Exception(error);
			}
			
			File fileStore = mission.getFilestoreLocation();
			if (DeleteManager.canDelete(mission, session)){
				
				//waypoint delete not cascaded so we need to delete
				//explicitly
				if (mission.getWaypoints() != null){
					for (SurveyWaypoint w : mission.getWaypoints()){
						session.delete(w.getWaypoint());
						session.delete(w);
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
							Messages.DeleteSurveyElementHandler_FsMissionDeleteError + fileStore.getAbsolutePath(), ex);
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
					MessageFormat.format(Messages.DeleteSurveyElementHandler_MissionDeleteError, new Object[]{id}) + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			session.getTransaction().rollback();
		}
		return false;
		
		
	}

}
