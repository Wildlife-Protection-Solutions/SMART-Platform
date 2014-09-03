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
package org.wcs.smart.er.query.ui.panels.item;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;
import org.wcs.smart.query.common.ui.itempanel.WrappedTreeNode;

/**
 * Content provider for the survey filter items tree node.
 * 
 * @author Emily
 *
 */
public class FilterContentProvider implements ITreeContentProvider{

	private SurveyDesign design;
	
	private List<MissionAttribute> allMissionAttributes = null;
	private List<Survey> allSurveys = null;
	private List<Object> sunits = null;
	private List<SamplingUnitAttribute> unitAttributes = null;
	
	private boolean triedMission = false;
	private boolean triedSurveys = false;
	private boolean triedUnits = false;
	private boolean triedUnitAttributes = false;
	
	private Viewer viewer;
	
	private IItemTreeNode rootNode;
	
	@SuppressWarnings("unchecked")
	private Job loadMissionPropertiesJob = new Job(Messages.SurveyItemContentProvider_loadMissionJobName){
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
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
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
	@SuppressWarnings("unchecked")
	private Job loadSurveyJob = new Job(Messages.SurveyItemContentProvider_LoadSurveyJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				if (design != null){
					allSurveys = s.createCriteria(Survey.class)
							.add(Restrictions.eq("surveyDesign", design)) //$NON-NLS-1$
							.addOrder(Order.desc("startDate")) //$NON-NLS-1$
							.addOrder(Order.asc("id")) //$NON-NLS-1$
							.list(); 
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
	
	private Job loadSamplingUnits = new Job(Messages.FilterContentProvider_LoadSuJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Session s = HibernateManager.openSession();
			try{
				if (design != null){
					sunits = SurveyHibernateManager.getInstance().getSamplingUnits(design, s);
					for (Object x : sunits){
						if (x instanceof SamplingUnit){
							((SamplingUnit) x).getId();
							((SamplingUnit) x).getType();
						}else if (x instanceof MissionTrack){
							((MissionTrack) x).getId();
						}
					}
				}else{
					sunits = null;
				}
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					((TreeViewer)viewer).refresh(new WrappedTreeNode((IItemTreeNode) getParent(Node.SAMPLING_UNITS), Node.SAMPLING_UNITS));
					
				}});
			return Status.OK_STATUS;
		}
	};
	
	private Job loadSamplingUnitAttributes = new Job("Loading Sampling Unit Attributes"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Session s = HibernateManager.openSession();
			try{
				if (design != null){
					List<SurveyDesignSamplingUnitAttribute> attributes = s.createCriteria(SurveyDesignSamplingUnitAttribute.class)
							.add(Restrictions.eq("id.surveyDesign", design))
							.list();
					
					unitAttributes = new ArrayList<SamplingUnitAttribute>();
					for (SurveyDesignSamplingUnitAttribute a : attributes){
						unitAttributes.add(a.getSamplingUnitAttribute());			
					}
				}else{					
					unitAttributes = s.createCriteria(SamplingUnitAttribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
							.list();
				}
				
				for (SamplingUnitAttribute a : unitAttributes){
					a.getName();
					a.getType();
				}
			}finally{
				s.close();
			}
			
			Collections.sort(unitAttributes, new Comparator<SamplingUnitAttribute>() {
				@Override
				public int compare(SamplingUnitAttribute arg0,
						SamplingUnitAttribute arg1) {
					return Collator.getInstance().compare(arg0.getName(), arg1.getName());
				}
			});
			
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					((TreeViewer)viewer).refresh(new WrappedTreeNode((IItemTreeNode) getParent(Node.SAMPLING_UNIT_ATTRIBUTE), Node.SAMPLING_UNIT_ATTRIBUTE));
					
				}});
			return Status.OK_STATUS;
		}
	};
	
	public enum Node{
		SURVEY_ID(Messages.SurveyItemContentProvider_SurveyIdLabel),
		MISSION_ID(Messages.SurveyItemContentProvider_MissionIDLabel),
		SURVEY_MISSION(Messages.SurveyItemContentProvider_AllMissionsAndSurveysLabel),
		MISSION_PROP(Messages.SurveyItemContentProvider_MissionPropertiesLabel),
		SAMPLING_UNITS(Messages.FilterContentProvider_SuLabel),
		SAMPLING_UNIT_ATTRIBUTE("Sampling Unit Attributes"),
		OBSERVER("Observer");
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
		triedUnits = false;
		triedUnitAttributes = false;
		
		allMissionAttributes = null;
		allSurveys = null;
		sunits = null;
		unitAttributes = null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (this.design != null){
			return Node.values();
		}else{
			return new Object[]{Node.SURVEY_ID, Node.MISSION_ID, Node.MISSION_PROP, Node.SAMPLING_UNIT_ATTRIBUTE, Node.OBSERVER};
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
						return new Object[]{Messages.SurveyItemContentProvider_ErrorLabel};
					}else{
						triedSurveys = true;
						loadSurveyJob.schedule();
						return new Object[]{Messages.SurveyItemContentProvider_LoadingLabel};
					}
				}
			}
		}else if (parentElement == Node.SAMPLING_UNITS){
			if (sunits != null){
				return sunits.toArray();
			}
			if(triedUnits){
				return new Object[]{Messages.SurveyItemContentProvider_ErrorLabel};
			}else{
				triedUnits = true;
				loadSamplingUnits.schedule();
				return new Object[]{Messages.SurveyItemContentProvider_LoadingLabel};
			}
		}else if (parentElement == Node.SAMPLING_UNIT_ATTRIBUTE){
			if (unitAttributes != null){
				return unitAttributes.toArray();
			}
			if(triedUnitAttributes){
				return new Object[]{Messages.SurveyItemContentProvider_ErrorLabel};
			}else{
				triedUnitAttributes = true;
				loadSamplingUnitAttributes.schedule();
				return new Object[]{Messages.SurveyItemContentProvider_LoadingLabel};
			}
		}else if (parentElement == Node.MISSION_ID){
			return null;
		}else if (parentElement == Node.MISSION_PROP){
			if (design != null){
				try{
					return design.getMissionProperties().toArray();
				}catch (Exception ex){
					if (triedMission){
						ERQueryPlugIn.log(ex.getMessage(), ex);
						return new Object[]{Messages.SurveyItemContentProvider_ErrorLabel};
						
					}
					triedMission = true;
					loadMissionPropertiesJob.schedule();
					return new Object[]{Messages.SurveyItemContentProvider_LoadingLabel};
				}
			}else{
				if (allMissionAttributes != null){
					return allMissionAttributes.toArray();
				}else{
					if (triedMission){
						return new Object[]{Messages.SurveyItemContentProvider_ErrorLabel};
					}
					triedMission = true;
					loadMissionPropertiesJob.schedule();
					return new Object[]{Messages.SurveyItemContentProvider_LoadingLabel};
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
				element == Node.SAMPLING_UNITS ||
				element == Node.SAMPLING_UNIT_ATTRIBUTE || 
				element instanceof Survey){
			return true;
		}
		return false;
	}

}
