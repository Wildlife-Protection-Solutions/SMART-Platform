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
package org.wcs.smart.patrol.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.ScreenOption;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Patrol Type/Transport screens configuration.
 *  
 * @author elitvin
 * @since 2.0.0
 */
public class TypeTransportScreenOptionComposite extends ScreenOptionComposite {

	private ScreenOption typeOption;
	private ScreenOption transportOption;
	
	private List<PatrolType> patrolTypes;

	private ComboViewer typeViewer;
	private ComboViewer transportViewer;

	private TransportOptionGroup transportGroup;
	
	public TypeTransportScreenOptionComposite(Composite parent, ScreenOption typeOption, ScreenOption transportOption, List<PatrolType> patrolTypes) {
		super(parent);
		this.typeOption = typeOption;
		this.transportOption = transportOption;
		this.patrolTypes = patrolTypes;
		
		new TypeOptionGroup(this, this.typeOption);
		
		transportGroup = new TransportOptionGroup(this, this.transportOption);
		
		if (typeOption.isVisible()) {
			transportGroup.setEnabled(false);
			transportGroup.getBtnDisplayPage().setSelection(true);
			transportViewer.getControl().setEnabled(false);
		}
	}

	private PatrolType.Type getPatrolType(ScreenOption option) {
 		String val = option.getStringValue();
 		return val == null ? null : PatrolType.Type.valueOf(val);
	}
	
	private List<PatrolTransportType> getTransportTypes() {
 		PatrolType.Type type = getPatrolType(typeOption);
 		if (type == null || patrolTypes.isEmpty())
 			return new ArrayList<PatrolTransportType>();

 		for (PatrolType item : patrolTypes) {
 			if (item.getType() == type) {
 				return item.getTransportTypes();
 			}
 		}

		return new ArrayList<PatrolTransportType>();
	}
	
	private class TypeOptionGroup extends ScreenOptionGroup {

		public TypeOptionGroup(Composite parent, ScreenOption option) {
			super(parent, option);
		}

		@Override
		protected void createDefaultControl(Group group) {
			typeViewer = new ComboViewer(group, SWT.READ_ONLY);
			typeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			typeViewer.getControl().setEnabled(!typeOption.isVisible());
			typeViewer.setContentProvider(ArrayContentProvider.getInstance());
			typeViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof PatrolType) {
						PatrolType i = (PatrolType) element;
						return LabelConstants.getLabel(i);
					}
					return super.getText(element);
				}
			});
	 		typeViewer.setInput(patrolTypes);
	 		PatrolType.Type type = getPatrolType(typeOption);
	 		if (type == null && !patrolTypes.isEmpty()) {
	 			//try to GROUND as default if possible
	 			for (PatrolType item : patrolTypes) {
	 				if (PatrolType.Type.GROUND.equals(item.getType())) {
	 					type = PatrolType.Type.GROUND;
	 					break;
	 				}
	 			}
	 			//if no ground present use first available as default
	 			if (type == null) {
	 				type = patrolTypes.get(0).getType();
	 			}
	 			typeOption.setStringValue(type.name());
	 		}
	 		if (type != null) {
	 			for (PatrolType item : patrolTypes) {
	 				if (item.getType() == type) {
	 					typeViewer.setSelection(new StructuredSelection(item));
	 					break;
	 				}
	 			}
	 		}
	 		typeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
	 			@Override
	 			public void selectionChanged(SelectionChangedEvent event) {
	 				IStructuredSelection selection = (IStructuredSelection) typeViewer.getSelection();
	 				Object obj = selection.getFirstElement();
	 				if (obj instanceof PatrolType) {
	 					PatrolType pt = (PatrolType) obj;
	 	 				typeOption.setStringValue(pt.getType().name());

	 	 				List<PatrolTransportType> tTypes = getTransportTypes();
	 	 		 		transportViewer.setInput(tTypes);
	 	 		 		UUID uuid = tTypes.isEmpty() ? null : tTypes.get(0).getUuid();
	 		 			transportOption.setUuidValue(uuid);
	 	 		 		if (uuid != null) {
	 	 		 			for (NamedItem item : tTypes) {
	 	 		 				if (item.getUuid().equals(uuid)) {
	 	 		 					transportViewer.setSelection(new StructuredSelection(item));
	 	 		 					break;
	 	 		 				}
	 	 		 			}
	 	 		 		}
	 	 				fireScreenOptionListeners();
	 				}
	 			}
	 		});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean display = getBtnDisplayPage().getSelection();
			typeOption.setVisible(display);
			
			typeViewer.getControl().setEnabled(!display);
			
			transportGroup.setEnabled(!display);
			transportViewer.getControl().setEnabled(!display && !transportOption.isVisible());
			transportGroup.getBtnDisplayPage().setSelection(display || transportOption.isVisible()); //always true if type is visible

			TypeTransportScreenOptionComposite.this.layout(true, true);
			fireScreenOptionListeners();
		}
	}

	private class TransportOptionGroup extends ScreenOptionGroup {

		public TransportOptionGroup(Composite parent, ScreenOption option) {
			super(parent, option);
		}

		@Override
		protected void createDefaultControl(Group group) {
			transportViewer = new ComboViewer(group, SWT.READ_ONLY);
			transportViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			transportViewer.getControl().setEnabled(!transportOption.isVisible());
			transportViewer.setContentProvider(ArrayContentProvider.getInstance());
			transportViewer.setLabelProvider(new NamedItemLabelProvider());
			List<PatrolTransportType> tTypes = getTransportTypes();
	 		transportViewer.setInput(tTypes);
	 		UUID uuid = transportOption.getUuidValue();
	 		if (uuid == null && !tTypes.isEmpty()) {
	 			uuid = tTypes.get(0).getUuid();
	 			transportOption.setUuidValue(uuid);
	 		}
	 		if (uuid != null) {
	 			for (NamedItem item : tTypes) {
	 				if (item.getUuid().equals(uuid)) {
	 					transportViewer.setSelection(new StructuredSelection(item));
	 					break;
	 				}
	 			}
	 		}
			transportViewer.addSelectionChangedListener(new ISelectionChangedListener() {
	 			@Override
	 			public void selectionChanged(SelectionChangedEvent event) {
	 				IStructuredSelection selection = (IStructuredSelection) transportViewer.getSelection();
	 				Object obj = selection.getFirstElement();
	 				if (obj instanceof UuidItem) {
	 					UuidItem i = (UuidItem) obj;
	 					transportOption.setUuidValue(i.getUuid());
	 	 				fireScreenOptionListeners();
	 				}
	 			}
	 		});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean visible = getBtnDisplayPage().getSelection();
			transportOption.setVisible(visible);
			transportViewer.getControl().setEnabled(!visible);
			fireScreenOptionListeners();
		}
		
	}
	
}
