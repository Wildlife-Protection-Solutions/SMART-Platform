package org.wcs.smart.er.query.ui.filter;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class SurveyItemContentProvider implements ITreeContentProvider{

	

	public enum Node{
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return Node.values();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}

}
