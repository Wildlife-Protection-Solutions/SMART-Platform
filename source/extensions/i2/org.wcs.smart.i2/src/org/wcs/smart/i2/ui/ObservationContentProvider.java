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
package org.wcs.smart.i2.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelRecord;

/**
 * Content provider for displaying relationship data in a tree by group.
 * 
 * @author Emily
 *
 */
public class ObservationContentProvider implements ITreeContentProvider {

	private List<IntelObservation> observations;
	
	public ObservationContentProvider(){
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.observations = null;
		if (newInput instanceof IntelLocation){
			IntelLocation location = (IntelLocation) newInput;
			observations = new ArrayList<>();
			observations.addAll(location.getObservations());
		}else if (newInput instanceof IntelRecord) {
			observations = new ArrayList<>();
			IntelRecord rr = (IntelRecord)newInput;
			if (rr.getLocations() == null) rr.setLocations(new ArrayList<>());
			for (IntelLocation ll : rr.getLocations()) {
				observations.addAll(ll.getObservations());
			}
		}
		
		if (observations != null) {
			observations.sort((a,b)-> a.getCategory().getFullCategoryName().compareTo(b.getCategory().getFullCategoryName()));
		}
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		if (observations == null) return new Object[0];
		if (observations.size() == 0) return new Object[0];
		return observations.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IntelObservationAttribute) return null;
		
		if (parentElement instanceof IntelObservation){
			if (((IntelObservation)parentElement).getObservationAttributes() == null) return null;
			List<IntelObservationAttribute> a = new ArrayList<>(((IntelObservation)parentElement).getObservationAttributes());
			a.sort((x,y)->Collator.getInstance().compare(x.getAttribute().getName(), y.getAttribute().getName()));
			return a.toArray();
		};
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IntelObservationAttribute) return ((IntelObservationAttribute) element).getObservation();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IntelObservation) return true;
		if (element instanceof IntelObservationAttribute) return false;
		return false;
		
	}

}
