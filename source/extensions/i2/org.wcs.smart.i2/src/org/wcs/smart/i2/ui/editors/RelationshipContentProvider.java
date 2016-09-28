/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelRelationshipGroup;

/**
 * Content provider for displaying relationship data in a tree by group.
 * 
 * @author Emily
 *
 */
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
