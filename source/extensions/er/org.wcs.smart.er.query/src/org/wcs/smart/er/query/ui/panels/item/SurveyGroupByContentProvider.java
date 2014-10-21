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

import java.util.ArrayList;
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
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
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
 * Content provider for survey group by options.
 * @author Emily
 *
 */
public class SurveyGroupByContentProvider implements ITreeContentProvider{

	private SurveyDesign design;
	
	private List<MissionAttribute> listMissionAttributes = null;
	private List<SamplingUnitAttribute> listSamplingUnitAttributes = null;
	
	private boolean triedMission = false;
	private boolean triedSamplingUnits = false;
	
	private Viewer viewer;
	
	private IItemTreeNode rootNode;
	
	@SuppressWarnings("unchecked")
	private Job loadMissionPropertiesJob = new Job(Messages.SurveyItemContentProvider_loadMissionJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				if (design != null){
					SurveyDesign lDesign = (SurveyDesign) s.load(SurveyDesign.class, design.getUuid());
					for (MissionProperty p : lDesign.getMissionProperties()){
						p.getAttribute().getName();
					}
				}else{
					listMissionAttributes = s.createCriteria(MissionAttribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.add(Restrictions.eq("type", Attribute.AttributeType.LIST)) //$NON-NLS-1$
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
	private Job loadSamplingUnitPropertiesJob = new Job(Messages.SurveyGroupByContentProvider_suJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				if (design != null){
					listSamplingUnitAttributes = new ArrayList<SamplingUnitAttribute>();
					SurveyDesign lDesign = (SurveyDesign) s.load(SurveyDesign.class, design.getUuid());
					for (SurveyDesignSamplingUnitAttribute p : lDesign.getSamplingUnitAttributes()){
						p.getSamplingUnitAttribute().getName();
						if (p.getSamplingUnitAttribute().getType() == AttributeType.LIST){
							listSamplingUnitAttributes.add(p.getSamplingUnitAttribute());
						}
					}
				}else{
					listSamplingUnitAttributes = s.createCriteria(SamplingUnitAttribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.add(Restrictions.eq("type", Attribute.AttributeType.LIST)) //$NON-NLS-1$
							.list();
				}
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					((TreeViewer)viewer).refresh(new WrappedTreeNode((IItemTreeNode) getParent(Node.SAMPLING_UNITS_ATT), Node.SAMPLING_UNITS_ATT));
					
				}});	
			return Status.OK_STATUS;
		}
	};
	
	public enum Node{
		SURVEY_ID(Messages.SurveyGroupByContentProvider_SurveyIdNode),
		MISSION_ID(Messages.SurveyGroupByContentProvider_MissionIdNode),
		MISSION_PROP(Messages.SurveyGroupByContentProvider_MissionPropertiesNode),
		SAMPLING_UNITS(Messages.SurveyGroupByContentProvider_SamplingUnitNode),
		SAMPLING_UNITS_ATT(Messages.SurveyGroupByContentProvider_SamplingUnitAttributesLabel),
		OBSERVER(Messages.SurveyGroupByContentProvider_ObserverNode);
		
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
		triedSamplingUnits = false;
		listMissionAttributes = null;
		listSamplingUnitAttributes = null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (design != null){
			return Node.values();
		}else{
			return new Node[]{Node.SURVEY_ID, Node.MISSION_ID, Node.MISSION_PROP, Node.OBSERVER};
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement == Node.MISSION_ID){
			return null;
		}else if (parentElement == Node.SURVEY_ID){
			return null;
		}else if (parentElement == Node.MISSION_PROP){
			if (design != null){
				try{
					List<MissionProperty> numerics = new ArrayList<MissionProperty>();
					for (MissionProperty mp : design.getMissionProperties()){
						if (mp.getAttribute().getType() == AttributeType.LIST){
							numerics.add(mp);
						}
					}
					return numerics.toArray();
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
				if (listMissionAttributes != null){
					return listMissionAttributes.toArray();
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
		}else if (parentElement == Node.SAMPLING_UNITS_ATT){
			if (listSamplingUnitAttributes != null){
				return listSamplingUnitAttributes.toArray();
			}else{
				if (triedSamplingUnits){
					return new Object[]{Messages.SurveyItemContentProvider_ErrorLabel};
				}
				triedSamplingUnits = true;
				loadSamplingUnitPropertiesJob.schedule();
				return new Object[]{Messages.SurveyItemContentProvider_LoadingLabel};
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
		}else if (element instanceof SamplingUnitAttribute){
			return Node.SAMPLING_UNITS_ATT;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element == Node.MISSION_PROP){
			return true;
		}else if (element == Node.SAMPLING_UNITS_ATT){
			return true;
		}
		return false;
	}

}