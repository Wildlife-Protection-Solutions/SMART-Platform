package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;

public class CaListWizardPage extends WizardPage {

	private CheckboxTableViewer tblConservationAreas;
	
	protected CaListWizardPage() {
		super("CALISTPAGE");
	}

	public List<ConservationArea> getConservationAreas() {
		List<ConservationArea> items = new ArrayList<>();
		for (Object c : tblConservationAreas.getCheckedElements()) {
			if (c instanceof ConservationArea) {
				items.add((ConservationArea) c);
			}
		}
		return items;
	}
	
	@Override
	public boolean isPageComplete() {
		return tblConservationAreas.getCheckedElements().length != 0;
	}
	
	@Override
	public void createControl(Composite parent) {
	
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout());
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		tblConservationAreas = CheckboxTableViewer.newCheckList(temp,  SWT.MULTI);
		tblConservationAreas.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblConservationAreas.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(tblConservationAreas));
		tblConservationAreas.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof ConservationArea){
					return ((ConservationArea)element).getNameLabel();
				}
				return super.getText(element);
			}
		});
		tblConservationAreas.setContentProvider(ArrayContentProvider.getInstance());
		tblConservationAreas.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				getContainer().updateButtons();	
			}
		});
		setControl(temp);
		
		setTitle("Conservation Areas");
		setMessage("Select the Conservation Area to import data from.  Only Conservation Areas that exist in the SMART 6 backup and current SMART version are listed.");
	}

	public void setConservationArea(List<ConservationArea> cas) {
		tblConservationAreas.setInput(cas);
	}
}
