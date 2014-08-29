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
package org.wcs.smart.er.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Deferred content provider for the survey list tree node.
 * 
 * @author Emily
 *
 */
public class SurveyListTreeNodeContentProvider implements
		IDeferredWorkbenchAdapter {

	public static final SurveyListTreeNodeContentProvider INSTANCE 
		= new SurveyListTreeNodeContentProvider();
	
	private SurveyListTreeNodeContentProvider(){
		
	}
	
	
	@Override
	public Object[] getChildren(Object o) {
		return new Object[]{o};
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	@Override
	public String getLabel(Object o) {
		return null;
	}

	@Override
	public Object getParent(Object o) {
		if (o instanceof SurveyListTreeNode){
			return ((SurveyListTreeNode) o).getParent();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void fetchDeferredChildren(Object object,
			IElementCollector collector, IProgressMonitor monitor) {

		if (!(object instanceof SurveyListTreeNode)) return;
		
		SurveyListTreeNode node = (SurveyListTreeNode)object;
		if (node.getType() == Type.MISSION){
			return ;
		}
		
		Session s = HibernateManager.openSession();
		try {
			s.beginTransaction();
			if (node.getType() == Type.SURVEY){
				//get missions
				//get surveys
				Query q = s.createQuery("FRom Mission where survey.uuid = :uuid ORDER BY startDate desc"); //$NON-NLS-1$
				q.setParameter("uuid", node.getUuid()); //$NON-NLS-1$
				List<Mission> kids = q.list();
				List<SurveyListTreeNode> allKids = new ArrayList<SurveyListTreeNode>();
				for (Mission survey : kids){
					SurveyListTreeNode kid = new SurveyListTreeNode(survey.getId(), survey.getUuid(), Type.MISSION, node);
					allKids.add(kid);
				}
				collector.add(allKids.toArray(), monitor);
			}
			s.getTransaction().commit();
		}finally{
			s.close();
		}
		
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public ISchedulingRule getRule(Object object) {
		return null;
	}

}
