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
package org.wcs.smart.patrol.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public abstract class PatrolItemComposite {
	
	private List<IPatrolItemChangeListener> listeners = new ArrayList<IPatrolItemChangeListener>();
	public abstract Composite createComponent(Composite parent, int style);
	
	public abstract void setValues(Patrol p, Session session) ;

	public abstract void updatePatrol(Patrol p);
	
	public abstract String getTitle();
	
	public void addChangeListener(IPatrolItemChangeListener listener){
		this.listeners.add(listener);
		
	}
	public void removeChangeListener(IPatrolItemChangeListener listener){
		this.listeners.remove(listener);
	}
	
	public void fireChangeListeners(){
		for(IPatrolItemChangeListener listener : listeners){
			listener.itemChanged();
		}
	}
	
	
	
}


