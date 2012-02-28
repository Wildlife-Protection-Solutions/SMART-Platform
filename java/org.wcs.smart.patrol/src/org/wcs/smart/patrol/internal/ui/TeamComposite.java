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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.Team;

/**
 * Patrol item composite for selecting patrol team.
 * @author Emily
 * @since 1.0.0
 */
public class TeamComposite extends PatrolItemComposite{

	private ComboViewer teamList;

	/*
	 * Team label provider
	 */
	private LabelProvider lblProvider = new LabelProvider(){
		public String getText(Object element) {
			if (element instanceof Team){
				return ((Team)element).getName();
			}
			return super.getText(element);
		}
	};
	
	/**
	 * 
	 */
	public TeamComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {

		Composite center = new Composite(parent, SWT.NONE);
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		center.setLayout(new GridLayout(2, false));
		
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Team: ");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		teamList = new ComboViewer(center, SWT.DROP_DOWN | SWT.READ_ONLY);
		teamList.setContentProvider(ArrayContentProvider.getInstance());
		teamList.setLabelProvider(lblProvider);
		teamList.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		teamList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		return center;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
		List<? extends Object> teams =  PatrolHibernateManager.getActiveTeams(p.getConservationArea(), session);		
		
		String none = "(None)";
		List<Object> stns = new ArrayList<Object>();
		stns.add(none);
		if (teams != null){
			stns.addAll(teams);
		}

		teamList.setInput(stns.toArray());
		if (p.getTeam() != null){
			teamList.setSelection(new StructuredSelection(p.getTeam()));
		}else{
			teamList.setSelection(new StructuredSelection(none));
		}

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public void updatePatrol(Patrol p) {
		Object team = (Object)((IStructuredSelection)teamList.getSelection()).getFirstElement();
		if (team != null && team instanceof Team){
			p.setTeam((Team)team);
		}else{
			p.setTeam(null);
		}
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Patrol Team";
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_TEAM;
	}
}

