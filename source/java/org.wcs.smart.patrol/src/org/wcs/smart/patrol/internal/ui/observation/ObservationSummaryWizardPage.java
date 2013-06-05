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
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard summary page that displays a summary of all
 * the observations entered at a given waypoint.
 * 
 * @author egouge
 *
 */
public class ObservationSummaryWizardPage  extends WizardPage implements IObservationWizardPage{

	public static final String PAGE_NAME = Messages.ObservationSummaryWizardPage_PageName;

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
			lbl.setText(SmartUtils.formatStringForLabel(ob.getKey().getFullCategoryName()));
			lbl.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Link lnkDelete = new Link(lblComp, SWT.NONE);
			lnkDelete.setText("<a>" + DialogConstants.DELETE_BUTTON_TEXT + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			lnkDelete.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e){
					deleteCategory(ob.getKey(), entryComp);
				}
			});
			
			Link lnkEdit = null;
			if (ob.getKey().hasAttributes()){
				lnkEdit = new Link(lblComp, SWT.NONE);
				lnkEdit.setText("<a>" + DialogConstants.EDIT_BUTTON_TEXT + "</a>");  //$NON-NLS-1$//$NON-NLS-2$
			}else{
				lbl.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			}

			final List<WaypointObservation> items = new ArrayList<WaypointObservation>();
			for (WaypointObservation wo :ob.getValue()){
				if (wo.getAttributes() != null){
					for (WaypointObservationAttribute att: wo.getAttributes()){
						if (att.hasValue()){
							items.add(wo);
							break;
						}
					}
				}
			}
			
			if (items.size() > 0){
				final TableViewer viewer = AttributeTable.createAttributeTable(entryComp, ob.getKey());
				viewer.setContentProvider(ArrayContentProvider.getInstance());			
				viewer.setInput(items.toArray());
				GridData gd = new GridData(SWT.FILL, SWT.FILL,true, true);
				gd.heightHint = Math.min(viewer.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y, viewer.getTable().getItemHeight()*4);
				gd.widthHint = 300;
				viewer.getTable().setLayoutData(gd);
				AttributeTable.resizeColumns(viewer);
				if (lnkEdit != null){
					lnkEdit.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							WaypointObservation wob = null;
							if (!(((IStructuredSelection)viewer.getSelection()).isEmpty())){
								wob = (WaypointObservation) ((IStructuredSelection)viewer.getSelection()).getFirstElement();
							}else if (  items.size() == 1){
								wob = items.get(0);
							}
							editCategory(ob.getKey(), wob);	
						}
					});
				}
				
			}else{
				if (lnkEdit != null){
					lnkEdit.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							editCategory(ob.getKey(), null);	
						}
					});
				}
			}
		}
		
		setTitle(Messages.ObservationSummaryWizardPage_PageTitle);
		super.setMessage(Messages.ObservationSummaryWizardPage_PageMessage);
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
		((ObservationWizard)getWizard()).removeObservations(category);
		Composite parent = comp.getParent();
		comp.dispose();
		parent.layout();
	}
	
	/**
	 * Opens the edit wizard page.
	 * 
	 * @param category
	 */
	private void editCategory(Category category, WaypointObservation wo){
		List<Category> cats = new ArrayList<Category>();
		cats.add(category);
		ObservationWizard wizard = (ObservationWizard) getWizard();
		wizard.setCategoriesToProcess(cats);
		
		AttributeWizardPage wizardPage = new AttributeWizardPage((Wizard)getWizard(), 0);
		wizard.getContainer().showPage( wizardPage );
		if (wo != null){
			//we have a particular observation to edit
			wizardPage.editObservation(wo);
		}
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
		if (target instanceof ObservationWizardPage){
			((ObservationWizard)getWizard()).setCategoriesToProcess(new ArrayList<Category>());
		}
		return true;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeShow()
	 */
	@Override
	public void beforeShow(){
		((ObservationWizard)getWizard()).setObservations();
	}
	
	
}
