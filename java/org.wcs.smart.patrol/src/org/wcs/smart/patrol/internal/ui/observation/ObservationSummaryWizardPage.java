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
package org.wcs.smart.patrol.internal.ui.observation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Observation input wizard summary page. This page displays all the observations
 * collected to date to the user and lets them review and modify if required.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationSummaryWizardPage extends WizardPage implements IObservationWizardPage{

	public static final String PAGE_NAME = "Observation Summary Page";

	public Font boldFont = null;
	private ObservationWizardPage nextPage = null;
	
	protected ObservationSummaryWizardPage(Wizard wizard) {
		super(PAGE_NAME);
		wizard.addPage(this);
	}

	/**
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (boldFont != null){
			boldFont.dispose();
		}
	}
	
	@Override
	public void createControl(Composite parent) {
		
		ScrolledComposite scrolled = new ScrolledComposite(parent,  SWT.V_SCROLL | SWT.H_SCROLL);
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite main = new Composite(scrolled, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		HashMap<Category, List<WaypointObservation>> obs = ((ObservationWizard)getWizard()).getAllObservations();
		for (Iterator<Entry<Category, List<WaypointObservation>>> iterator = obs.entrySet().iterator(); iterator.hasNext();) {
			final Entry<Category, List<WaypointObservation>> ob =  iterator.next();
			
			final Composite entryComp = new Composite(main, SWT.BORDER);
			entryComp.setLayout(new GridLayout(1, false));
			entryComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Composite lblComp = new Composite(entryComp, SWT.NONE);
			lblComp.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
			lblComp.setLayout(new GridLayout(3, false));
			Label lbl = new Label(lblComp, SWT.NONE);
			
			if (boldFont == null){
				FontData boldFontData= lbl.getFont().getFontData()[0];
				boldFontData.setStyle(SWT.BOLD); 
				boldFont = new Font(Display.getCurrent(), boldFontData);
			}
			lbl.setFont(boldFont);
			lbl.setText(ob.getKey().getFullCategoryName().replaceAll("&", "&&"));
			lbl.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Link lnkDelete = new Link(lblComp, SWT.NONE);
			lnkDelete.setText("<a>Delete</a>");
			lnkDelete.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e){
					deleteCategory(ob.getKey(), entryComp);
				}
			});
			
			if (ob.getKey().hasAttributes()){
				Link lnkEdit = new Link(lblComp, SWT.NONE);
				lnkEdit.setText("<a>Edit</a>");
				//lnkEdit.setBounds(getShell().getClientArea().x, getShell().getClientArea().y,140,40);
				lnkEdit.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						editCategory(ob.getKey());	
					}
				});
			}else{
				lbl.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			}

//			List<WaypointObservation> wob = ob.getValue();
			//only  keep observations with data
			List<WaypointObservation> items = new ArrayList<WaypointObservation>();
			for (WaypointObservation wo :ob.getValue()){
				for (WaypointObservationAttribute att: wo.getAttributes()){
					if (att.hasValue()){
						items.add(wo);
						break;
					}
				}
			}
			
			//if (wob.size() > 1 || (wob.size() == 1 && (wob.get(0).getAttributes() != null && wob.get(0).getAttributes().size() > 0))){
			if (items.size() > 0){
				TableViewer viewer = AttributeTable.createAttributeTable(false, entryComp, ob.getKey(), null);
				viewer.setContentProvider(ArrayContentProvider.getInstance());
				
				viewer.setInput(items.toArray());
				GridData gd = new GridData(SWT.FILL, SWT.FILL,true, false);
				gd.heightHint = Math.min(viewer.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y, 50);
				viewer.getTable().setLayoutData(gd);
				
			}
		}
		
		super.setMessage("Review the observation data you have entered.  Use the edit button to edit observations.  Press 'Next' to enter another observation. 'Finish' if the observations are complete.");
		super.setPageComplete(true);
		((ObservationWizard)getWizard()).setCanFinish(true);
		
		scrolled.setContent(main);
		setControl(scrolled);
		scrolled.setMinSize(scrolled.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	
	/**
	 * Deletes the given cateory and all associated observations
	 * @param category
	 * @param comp
	 */
	private void deleteCategory(Category category, Composite comp){
		((ObservationWizard)getWizard()).getAllObservations().remove(category);
		Composite parent = comp.getParent();
		comp.dispose();
		parent.layout();
	}
	
	/**
	 * Opens the edit wizard page.
	 * 
	 * @param category
	 */
	private void editCategory(Category category){
		((ObservationWizard)getWizard()).setCurrentObservation(category);
		((ObservationWizard)getWizard()).getWizardDialog().showPage( new AttributeWizardPage((Wizard)getWizard()) );
	}
	
	/**
	 * Users cannot go back from this page.
	 * 
	 * @return null
	 * @see org.eclipse.jface.wizard.WizardPage#getPreviousPage()
	 */
	@Override
	public IWizardPage getPreviousPage(){
		return null;
	}

	/**
	 * The observation wizard page
	 */
	@Override
    public IWizardPage getNextPage() {
		if (nextPage == null){
			nextPage = new ObservationWizardPage((Wizard) getWizard());
		}
		return nextPage;
    }
	
	/**
	 * @return true
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeMoveNext()
	 */
	@Override
	public boolean beforeMoveNext(IWizardPage target) {
		return true;
	}

	
	
}
