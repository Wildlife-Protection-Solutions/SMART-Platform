package org.wcs.smart.er.query.ui.filter;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;
import org.wcs.smart.query.common.ui.itempanel.WrappedTreeNode;

public class SurveyItemContentProvider implements ITreeContentProvider{

	private SurveyDesign design;
	
	private List<MissionAttribute> allMissionAttributes = null;
	private List<Survey> allSurveys = null;
	
	private boolean triedMission = false;
	private boolean triedSurveys = false;
	
	private Viewer viewer;
	
	private IItemTreeNode rootNode;
	
	public enum Node{
		SURVEY_MISSION("All Surveys and Missions"),
		SURVEY_ID("Survey ID"),
		MISSION_ID("Mission ID"),
		MISSION_PROP("Mission Properties");
		
		public String guiName;
		
		private Node(String guiName){
			this.guiName = guiName;
		}
				
	};
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		if (newInput instanceof Object[]){
			this.design = (SurveyDesign) ((Object[])newInput)[0];
			this.rootNode = (IItemTreeNode)((Object[])newInput)[1];
		}else{
			this.design = null;
		}
		triedMission = false;
		triedSurveys = false;
		allMissionAttributes = null;
		allSurveys = null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (this.design != null){
			return Node.values();
		}else{
			return new Object[]{Node.SURVEY_ID, Node.MISSION_ID, Node.MISSION_PROP};
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement == Node.SURVEY_MISSION){
			if (design != null){
				if (allSurveys != null){
					return allSurveys.toArray();
				}else{
					if (triedSurveys){
						return new Object[]{"Error"};
					}else{
						triedSurveys = true;
						loadSurveyJob.schedule();
						return new Object[]{"Loading..."};
					}
				}
			}
		}else if (parentElement == Node.MISSION_ID){
			return null;
		}else if (parentElement == Node.MISSION_PROP){
			//TODO: this is going to have to be done in a job or something
			if (design != null){
				try{
					return design.getMissionProperties().toArray();
				}catch (Exception ex){
					if (triedMission){
						ERQueryPlugIn.log(ex.getMessage(), ex);
						return new Object[]{"Error"};
						
					}
					triedMission = true;
					loadMissionPropertiesJob.schedule();
					return new Object[]{"Loading..."};
				}
			}else{
				if (allMissionAttributes != null){
					return allMissionAttributes.toArray();
				}else{
					if (triedMission){
						return new Object[]{"Error"};
					}
					triedMission = true;
					loadMissionPropertiesJob.schedule();
					return new Object[]{"Loading..."};
				}
				
				//get all mission attributes in the system
			}
		}else if (parentElement instanceof Survey){
			return ((Survey) parentElement).getMissions().toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof Node){
			return rootNode;
		}else if (element instanceof MissionProperty){
			return Node.MISSION_PROP;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element == Node.SURVEY_MISSION ||
				element == Node.MISSION_PROP ||
				element instanceof Survey){
			return true;
		}
		return false;
	}

	Job loadMissionPropertiesJob = new Job("load properties"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				if (design != null){
					s.update(design);
					for (MissionProperty p : design.getMissionProperties()){
						p.getAttribute().getName();
					}
				}else{
					allMissionAttributes = s.createCriteria(MissionAttribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
							.list();
					
				}
				
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					((TreeViewer)viewer).refresh(new WrappedTreeNode((IItemTreeNode) getParent(Node.MISSION_PROP), Node.MISSION_PROP));
					
				}});
			
			return Status.OK_STATUS;
		}
		
	};
	
	Job loadSurveyJob = new Job("load surveys"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				if (design != null){
					allSurveys = s.createCriteria(Survey.class).add(Restrictions.eq("surveyDesign", design)).list();
					for (Survey survey : allSurveys){
						for (Mission m : survey.getMissions()){
							m.getId();
							
						}
					}
				}else{
					allSurveys = null;
				}
				
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					((TreeViewer)viewer).refresh(new WrappedTreeNode((IItemTreeNode) getParent(Node.SURVEY_MISSION), Node.SURVEY_MISSION));
					
				}});
			
			return Status.OK_STATUS;
		}
	};

	
}
