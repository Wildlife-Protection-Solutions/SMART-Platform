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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.WaypointObservation;
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
	private ComboViewer employeeViewer;
	
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
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)parent.getLayout()).marginHeight = 0;
		
		if (getWizardLocal().getObservationOptions().getTrackObserver()){
			Composite observerComp = new Composite(parent, SWT.NONE);
			observerComp.setLayout(new GridLayout(2, false));
			((GridLayout)observerComp.getLayout()).marginHeight = 0;
		
			Label l = new Label(observerComp, SWT.NONE);
			l.setText(Messages.ObservationSummaryWizardPage_ObserverLabel);
			l.setFont(boldFont);
		
		
			employeeViewer = new ComboViewer(
				new Combo(observerComp,SWT.FLAT | SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY));
			employeeViewer.setContentProvider(ArrayContentProvider.getInstance());
			employeeViewer.getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			employeeViewer.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof Employee){
						return ((Employee)element).getFullLabel();
					}
					return super.getText(element);
				}
			});
			employeeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			
			List<Object> objects = new ArrayList<Object>();
			objects.add(""); //$NON-NLS-1$
			if (getWizardLocal().getObservers() != null){
				objects.addAll( getWizardLocal().getObservers() );
			}
			employeeViewer.setInput(objects);
			Employee em = null;
			if (getWizardLocal().getWaypoint().getObservations() == null){
				getWizardLocal().getWaypoint().setObservations(new ArrayList<WaypointObservation>());
			}
			for (WaypointObservation wp : getWizardLocal().getWaypoint().getObservations()){
				if (wp.getObserver() != null){
					em = wp.getObserver();
				}
			}
			if (em != null){
				employeeViewer.setSelection(new StructuredSelection(em));
			}
		}
		
		
		
		
		ScrolledComposite scrolled = new ScrolledComposite(parent,  SWT.V_SCROLL | SWT.H_SCROLL );
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite main = new Composite(scrolled, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		HashMap<Category, List<WaypointObservation>> obs = getWizardLocal().getAllObservations();
		
		ArrayList<Category> sortedCategories = new ArrayList<Category>();
		sortedCategories.addAll(obs.keySet());
		Collections.sort(sortedCategories, new Comparator<Category>() {
			@Override
			public int compare(Category o1, Category o2) {
				return Collator.getInstance().compare(o1.getFullCategoryName(), o2.getFullCategoryName());
			}
		});
		
		for (final Category c : sortedCategories){
			List<WaypointObservation> observations = obs.get(c);
			
			final Composite entryComp = new Composite(main, SWT.BORDER);
			entryComp.setLayout(new GridLayout(1, false));
			entryComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Composite lblComp = new Composite(entryComp, SWT.NONE);
			lblComp.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
			lblComp.setLayout(new GridLayout(3, false));
			entryComp.pack();
		
			Label lbl = new Label(lblComp, SWT.WRAP);
			if (boldFont == null){
				FontData boldFontData= lbl.getFont().getFontData()[0];
				boldFontData.setStyle(SWT.BOLD); 
				boldFont = new Font(Display.getCurrent(), boldFontData);
			}
			lbl.setFont(boldFont);
			lbl.setText(SmartUtils.formatStringForLabel(c.getFullCategoryName()));
			lbl.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)lbl.getLayoutData()).widthHint = 400;
			
			Link lnkDelete = new Link(lblComp, SWT.NONE);
			lnkDelete.setText("<a>" + DialogConstants.DELETE_BUTTON_TEXT + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			lnkDelete.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e){
					deleteCategory(c, entryComp);
				}
			});
			
			Link lnkEdit  = new Link(lblComp, SWT.NONE);
			lnkEdit.setText("<a>" + DialogConstants.EDIT_BUTTON_TEXT + "</a>");  //$NON-NLS-1$//$NON-NLS-2$
			
			final List<WaypointObservation> items = new ArrayList<WaypointObservation>();
			items.addAll(observations);
		
			final TableViewer viewer = AttributeTable.createAttributeTable(
					entryComp, c);
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.setInput(items.toArray());
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.heightHint = Math.min(
					viewer.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).y,
					viewer.getTable().getItemHeight() * 4);
			gd.widthHint = 300;
			viewer.getTable().setLayoutData(gd);
			AttributeTable.resizeColumns(viewer);
			viewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					WaypointObservation wob = null;
					if (!(((IStructuredSelection) viewer.getSelection())
							.isEmpty())) {
						wob = (WaypointObservation) ((IStructuredSelection) viewer
								.getSelection()).getFirstElement();
					} else if (items.size() == 1) {
						wob = items.get(0);
					}
					editCategory(c, wob);
				}
			});
			if (lnkEdit != null) {
				lnkEdit.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						WaypointObservation wob = null;
						if (!(((IStructuredSelection) viewer.getSelection())
								.isEmpty())) {
							wob = (WaypointObservation) ((IStructuredSelection) viewer
									.getSelection()).getFirstElement();
						} else if (items.size() == 1) {
							wob = items.get(0);
						}
						editCategory(c, wob);
					}
				});
			}

		}
		
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
	
	/**
	 * Deletes the given cateory and all associated observations
	 * @param category
	 * @param comp
	 */
	private void deleteCategory(Category category, Composite comp){
		getWizardLocal().removeObservations(category);
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
		
		getWizardLocal().setCategoriesToProcess(cats);
		
		AttributeWizardPage wizardPage = new AttributeWizardPage((Wizard)getWizard(), 0);
		getWizardLocal().getContainer().showPage( wizardPage );
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
		//update observer
		if (employeeViewer != null){
			for (WaypointObservation wo : getWizardLocal().getWaypoint().getObservations()){
				wo.setObserver(getObserver());
			}
		}
		
		if (target instanceof ObservationWizardPage){
			getWizardLocal().setCategoriesToProcess(new ArrayList<Category>());
		}
		return true;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.IObservationWizardPage#beforeShow()
	 */
	@Override
	public void beforeShow(){
		getWizardLocal().setObservations();
	}
	
	private ObservationWizard getWizardLocal(){
		return (ObservationWizard)getWizard();
	}
}
