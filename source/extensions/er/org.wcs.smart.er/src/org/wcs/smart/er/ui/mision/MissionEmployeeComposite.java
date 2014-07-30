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
package org.wcs.smart.er.ui.mision;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.common.control.MultipleSelectComposite.IListChanged;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.ui.EmployeeLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Mission members composite.
 * 
 * @author Emily
 *
 */
public class MissionEmployeeComposite extends MissionComposite {

	private MultipleSelectComposite<Employee> composite;
	
	public MissionEmployeeComposite(){
	}
	
	
	@Override
	public Control createControl(Composite parent) {

		Composite c = new Composite(parent, SWT.NONE);
		
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		composite = new MultipleSelectComposite<Employee>(c, SWT.NONE);
		composite.setLabelProvider(EmployeeLabelProvider.getInstance());
		composite.setLabelAllText("All Employees:");
		composite.setLabelSelectedText("Mission Members:");
		
		composite.addSelectionChangedListener(new IListChanged<Employee>() {

			@Override
			public void listChanged(List<Employee> items) {
				fireChangeListeners();
			}
		});
		return c;
	}

	
	@Override
	public void init(Mission mission, Session session) {List<Employee> all = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session);
		List<Employee> selected = new ArrayList<Employee>();
		
		if (mission.getMembers() != null){
			for (MissionMember mm : mission.getMembers()){
				selected.add(mm.getMember());
				all.remove(mm.getMember());
			}
		}
		
		composite.setItemsData(all, selected);
	}

	@Override
	public void updateDesign(Mission mission) {
		if (mission.getMembers() == null){
			mission.setMembers(new ArrayList<MissionMember>());
		}
		
		// remove existing members
		for (MissionMember mm: mission.getMembers()){
			mm.setMission(null);
		}
		mission.getMembers().clear();
		
		//add new members
		for(Employee e : composite.getSelectedItemsAsList()){
			MissionMember mm = new MissionMember();
			mm.setMember(e);
			mm.setMission(mission);
			mission.getMembers().add(mm);
		}

	}

	@Override
	public boolean isValid() {
		if (composite.getSelectedItemsAsList().size()> 0){
			return true;
		}
		return false;
	}

	@Override
	public String getTitle() {
		return "Mission Members";
	}

	@Override
	public String getDescription() {
		return "Members involved in the mission.";
	}

}
