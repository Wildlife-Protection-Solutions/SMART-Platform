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
package org.wcs.smart.observation.ui.input;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.ui.SmartLabelProvider;

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
	private ComboViewer employeeViewer;
	private ScrolledComposite scrolled ;
	
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
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).marginWidth = 0;
		
		if (boldFont == null){
			FontData boldFontData= parent.getFont().getFontData()[0];
			boldFontData.setStyle(SWT.BOLD); 
			boldFont = new Font(parent.getDisplay(), boldFontData);
		}
		
		if (getWizardLocal().getTrackObserver()){
			Composite observerComp = new Composite(parent, SWT.NONE);
			observerComp.setLayout(new GridLayout());
			((GridLayout)observerComp.getLayout()).marginHeight = 0;
			((GridLayout)observerComp.getLayout()).marginWidth = 0;
			observerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Label l = new Label(observerComp, SWT.NONE);
			l.setText(Messages.ObservationSummaryWizardPage_ObserverLabel);
			l.setFont(boldFont);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
		
			employeeViewer = new ComboViewer(
				new Combo(observerComp,SWT.FLAT | SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY));
			employeeViewer.setContentProvider(ArrayContentProvider.getInstance());
			employeeViewer.getControl().setBackground(observerComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			employeeViewer.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof Employee){
						return SmartLabelProvider.getFullLabel((Employee)element);
					}
					return super.getText(element);
				}
			});
			employeeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)employeeViewer.getControl().getLayoutData()).widthHint = 100;
			
			List<Object> objects = new ArrayList<Object>();
			objects.add(""); //$NON-NLS-1$
			if (getWizardLocal().getObservers() != null){
				objects.addAll( getWizardLocal().getObservers() );
			}
			employeeViewer.setInput(objects);
			Employee selection = getWizardLocal().getObserver();
			if (selection != null){
				//ensure it is an option; even if not in the list
				if (!objects.contains(selection)){
					objects.add(selection);
					employeeViewer.setInput(objects);
				}
				
				employeeViewer.setSelection(new StructuredSelection(selection));
			}
		}
		
		scrolled = new ScrolledComposite(parent,  SWT.V_SCROLL | SWT.H_SCROLL );
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite main = new Composite(scrolled, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		new ObservationItemList(main, getWizardLocal());
		
		setTitle(Messages.ObservationSummaryWizardPage_PageTitle);
		super.setMessage(Messages.ObservationSummaryWizardPage_PageMessage);
		super.setPageComplete(true);
		getWizardLocal().setCanFinish(true);
		
		scrolled.setContent(main);
		setControl(parent);

		
		int width = 400;
		int height = main.computeSize(width, SWT.DEFAULT).y;
		scrolled.setMinSize(width, height);

		
		//for scrollbar and wrapping labels
		final Composite mParent = main;
		final ScrolledComposite mScrolled = scrolled;
		final Composite fparent = parent;
		parent.addListener(SWT.Resize, new Listener(){
			int width = -1;
			@Override
			public void handleEvent(Event event) {		
				int newWidth = mParent.getSize().x;
			    if (newWidth != width) {
			        mScrolled.setMinHeight(mParent.computeSize(newWidth, SWT.DEFAULT).y);
			        width = newWidth;
			    }
			}});

		parent.addListener(SWT.Paint, new Listener() {
			@Override
			public void handleEvent(Event event) {
				int newWidth = mParent.getSize().x;
				mScrolled.setMinHeight(mParent.computeSize(newWidth,SWT.DEFAULT).y);
				fparent.removeListener(SWT.Paint, this);
			}
		});
		parent.layout(true);
	}
	
	

	/**
	 * 
	 * @return the selected observer
	 */
	public Employee getObserver(){
		if (employeeViewer != null && employeeViewer.getSelection() != null){
			Object x = ((IStructuredSelection)employeeViewer.getSelection()).getFirstElement();
			if ( x instanceof Employee){
				return (Employee) x;
			}
		}
		return null;
	}
	
//	/**
//	 * Deletes the given cateory and all associated observations
//	 * @param category
//	 * @param comp
//	 */
//	private void deleteCategory(Category category, Composite comp, ScrolledComposite scrolled){
//		getWizardLocal().removeObservations(category);
//		Composite parent = comp.getParent();
//		comp.dispose();
//		parent.layout();
//		scrolled.layout(true);
//	}
//	
//	private void deleteObservation(WaypointObservation obs, TableViewer viewer,  Composite comp, ScrolledComposite scrolled){
//		getWizardLocal().removeObservation(obs);
//		if (getWizardLocal().getWaypointObservation(obs.getCategory()).isEmpty()) {
//			deleteCategory(obs.getCategory(), comp, scrolled);
//		}else {
//			viewer.refresh();
//		}
//	}
	

	
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
		//update observer
		if (employeeViewer != null){
			getWizardLocal().setObserver(getObserver());
		}
		
		if (target instanceof ObservationWizardPage){
			getWizardLocal().setCategoriesToProcess(new ArrayList<>());
		}
		return true;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeShow()
	 */
	@Override
	public void beforeShow(){
	}
	
	private ObservationWizard getWizardLocal(){
		return (ObservationWizard)getWizard();
	}
}
