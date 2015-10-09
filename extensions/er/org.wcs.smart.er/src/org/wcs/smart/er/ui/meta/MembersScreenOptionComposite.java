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
package org.wcs.smart.er.ui.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.dataentry.meta.ScreenOptionComposite;
import org.wcs.smart.dataentry.meta.ScreenOptionGroup;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.dataentry.model.ScreenOptionUuid;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.ui.EmployeeLabelProvider;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Patrol Members/Leader screens configuration.
 * 
 * @author elitvin
 * @since 3.3.0
 */
public class MembersScreenOptionComposite extends ScreenOptionComposite {

	private ScreenOption membersOption;
	private ScreenOption leaderOption;

	private List<Employee> members;

	private CheckboxTableViewer membersViewer;
	
	private EmployeeDropOptionGroup leaderGroup;
	private LabelProvider employeeLblProvider = new EmployeeLabelProvider(){
		@Override
		public String getText(Object element) {
			if (element instanceof Employee) {
				return SmartLabelProvider.getShortLabel((Employee) element);
			}
			return super.getText(element);
		}
	};
	
	/**
	 * @param parent
	 */
	public MembersScreenOptionComposite(Composite parent, Map<MissionScreenOptionMeta, ScreenOption> options, List<Employee> members) {
		super(parent);
		this.members = members;
		
		membersOption = options.get(MissionScreenOptionMeta.MEMBERS);
		leaderOption = options.get(MissionScreenOptionMeta.LEADER);

		new MemberOptionGroup(this, membersOption, SurveyMetaLabelUtil.getLabel(MissionScreenOptionMeta.MEMBERS));
		leaderGroup = new EmployeeDropOptionGroup(this, leaderOption, SurveyMetaLabelUtil.getLabel(MissionScreenOptionMeta.LEADER));

		if (membersOption.isVisible()) {
			leaderGroup.setEnabled(false);
			leaderGroup.getBtnDisplayPage().setSelection(true);
			leaderGroup.getViewer().getControl().setEnabled(false);
		}
	}

	private void updateEmployeeDropOptionGroup(EmployeeDropOptionGroup optionGroup) {
		Object[] checkedElements = membersViewer.getCheckedElements();
		optionGroup.getViewer().setInput(checkedElements);
		UUID uuid = optionGroup.getModel().getUuidValue();
		for (Object object : checkedElements) {
			Employee item = (Employee) object;
			if (item.getUuid().equals(uuid)) {
				optionGroup.getViewer().setSelection(new StructuredSelection(item));
				return; //current option is allowed
			}
		}
		//if we are here than selected leader or pilot was unchecked in members
		if (checkedElements.length > 0) {
			Employee item = (Employee) checkedElements[0];
			optionGroup.getViewer().setSelection(new StructuredSelection(item));
			optionGroup.getModel().setUuidValue(item.getUuid());
		} else {
			optionGroup.getModel().setUuidValue(null);
		}
	}
	
	@Override
	public String validate() {
		if (!membersOption.isVisible()) {
			if (membersOption.getUuidList().isEmpty())
				return Messages.MembersScreenOptionComposite_MemberRequired;
			if (!leaderOption.isVisible()) {
				if (leaderOption.getUuidValue() == null)
					return Messages.MembersScreenOptionComposite_LeaderRequired;
			}
		}
		return null;
	}
	
	private class MemberOptionGroup extends ScreenOptionGroup {

		public MemberOptionGroup(Composite parent, ScreenOption option, String title) {
			super(parent, option, title);
		}

		@Override
		protected void createDefaultControl(Group group) {
			new Label(group, SWT.NONE);
	        Table table = new Table(group, SWT.CHECK | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
			membersViewer = new CheckboxTableViewer(table);
//			membersViewer = new CheckboxTableViewer(group, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
//			gd.widthHint = 150;
			gd.heightHint = 120;
			membersViewer.getControl().setLayoutData(gd);
			membersViewer.getControl().setEnabled(!membersOption.isVisible());
			membersViewer.setContentProvider(ArrayContentProvider.getInstance());
			membersViewer.setLabelProvider(employeeLblProvider);
			membersViewer.setInput(members);
			
			//set current value
			List<Employee> pickedMembers = new ArrayList<Employee>();
			for (ScreenOptionUuid sou : membersOption.getUuidList()) {
				UUID uuid = sou.getUuidValue();
				for (Employee e : members) {
					if (uuid.equals(e.getUuid()))
						pickedMembers.add(e);
				}
			}
			membersViewer.setCheckedElements(pickedMembers.toArray());
			
			membersViewer.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
	 				List<ScreenOptionUuid> uuids = membersOption.getUuidList();
	 				uuids.clear();
	 				for (Object element : membersViewer.getCheckedElements()) {
	 					Employee e = (Employee) element;
	 					ScreenOptionUuid sou = new ScreenOptionUuid();
	 					sou.setScreenOption(membersOption);
	 					sou.setUuidValue(e.getUuid());
						uuids.add(sou);
					}
	 				updateEmployeeDropOptionGroup(leaderGroup);
	 				fireScreenOptionListeners();
				}

			});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean display = getBtnDisplayPage().getSelection();
			membersOption.setVisible(display);
			membersViewer.getControl().setEnabled(!display);
			
			leaderGroup.setEnabled(!display);
			leaderGroup.getViewer().getControl().setEnabled(!display && !leaderOption.isVisible());
			leaderGroup.getBtnDisplayPage().setSelection(display || leaderOption.isVisible()); //always true if members is visible
			
			fireScreenOptionListeners();
		}
	}

	private class EmployeeDropOptionGroup extends ScreenOptionGroup {

		private ComboViewer viewer;
		
		public EmployeeDropOptionGroup(Composite parent, ScreenOption option, String title) {
			super(parent, option, title);
		}

		@Override
		protected void createDefaultControl(Group group) {
			viewer = new ComboViewer(group, SWT.READ_ONLY);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			viewer.getControl().setEnabled(!getModel().isVisible());
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.setLabelProvider(employeeLblProvider);
			Object[] pickedMembers = membersViewer.getCheckedElements();
			viewer.setInput(pickedMembers);
			
	 		UUID uuid = getModel().getUuidValue();
	 		if (uuid == null && pickedMembers.length > 0) {
	 			uuid = ((Employee) pickedMembers[0]).getUuid();
	 			getModel().setUuidValue(uuid);
	 		}
	 		if (uuid != null) {
	 			for (Object obj : pickedMembers) {
	 				Employee item = (Employee) obj;
	 				if (item.getUuid().equals(uuid)) {
	 					viewer.setSelection(new StructuredSelection(item));
	 					break;
	 				}
	 			}
	 		}
			
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
	 			@Override
	 			public void selectionChanged(SelectionChangedEvent event) {
	 				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
	 				Object obj = selection.getFirstElement();
	 				if (obj instanceof Employee) {
	 					Employee i = (Employee) obj;
	 					getModel().setUuidValue(i.getUuid());
	 	 				fireScreenOptionListeners();
	 				}
	 			}
	 		});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean visible = getBtnDisplayPage().getSelection();
			getModel().setVisible(visible);
			viewer.getControl().setEnabled(!visible);
			fireScreenOptionListeners();
		}
		
		protected Viewer getViewer() {
			return viewer;
		}
		
		@Override
		public void setDefaultEnabled(boolean enabled){
			super.setDefaultEnabled(enabled);
			viewer.getControl().setEnabled(enabled);
		}
	}
	
}
