package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelRelationshipGroup;


public class RelationshipContentProvider implements ITreeContentProvider {

	private HashMap<IntelRelationshipGroup, List<IntelEntityRelationship>> data = new HashMap<IntelRelationshipGroup, List<IntelEntityRelationship>>();
	
	private static IntelRelationshipGroup NONEGROUP;
	
	static{
		NONEGROUP = new IntelRelationshipGroup();
		NONEGROUP.setName("None");
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof List){
			data = new HashMap<IntelRelationshipGroup, List<IntelEntityRelationship>>();
			List<?> items = (List<?>)newInput;
			for (Object x : items){
				if (x instanceof IntelEntityRelationship){
					IntelRelationshipGroup g = ((IntelEntityRelationship) x).getRelationshipType().getRelationshipGroup();
					if (g == null) g = NONEGROUP;
					
					List<IntelEntityRelationship> rels = data.get(g);
					if (rels == null){
						rels = new ArrayList<IntelEntityRelationship>();
						data.put(g, rels);
					}
					rels.add((IntelEntityRelationship) x);
				}
			}
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return data.keySet().toArray(new IntelRelationshipGroup[data.keySet().size()]);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IntelEntityRelationship) return null;
		if (parentElement instanceof IntelRelationshipGroup){
			List<IntelEntityRelationship> kids = data.get(parentElement);
			if (kids != null){
				return kids.toArray(new IntelEntityRelationship[kids.size()]);
			}
		};
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IntelRelationshipGroup) return null;
		if (element instanceof IntelEntityRelationship){
			IntelRelationshipGroup g = (((IntelEntityRelationship) element).getRelationshipType().getRelationshipGroup());
			if (g == null) return NONEGROUP;
			return g;
		};
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IntelEntityRelationship) return false;
		if (element instanceof IntelRelationshipGroup) return true;
		return false;
		
	}

}
