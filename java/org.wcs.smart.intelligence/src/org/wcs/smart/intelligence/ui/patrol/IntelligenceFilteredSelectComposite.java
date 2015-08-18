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
package org.wcs.smart.intelligence.ui.patrol;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.IIntelligenceFilteringView;
import org.wcs.smart.intelligence.ui.IntelligenceFilterDialog;
import org.wcs.smart.intelligence.ui.IntelligenceViewFilter;

/**
 * Composite to select intelligence.  This composite consists of two list:
 * a list of intelligence records to select from and a list of selected records.
 * Users move items from one list to the other.
 * 
 * It also adds filtering functionality that allows to filter list of items to select from.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceFilteredSelectComposite extends MultipleSelectComposite<Intelligence> implements IIntelligenceFilteringView {

	private IntelligenceViewFilter filter = new IntelligenceViewFilter();

	boolean stopEventPropogation;
	
	/**
	 * List of intelligences to select from
	 */
	private List<Intelligence> availableList;
	
	
	public IntelligenceFilteredSelectComposite(Composite parent, int style) {
		super(parent, style);

		setLabelProvider(new IntelligenceLabelProvider());
		setLabelAllText(Messages.PatrolMotivationComposite_Selector_All_Label);
		setLabelSelectedText(Messages.PatrolMotivationComposite_Selector_Selected_Label);
		
		updateContent();
	}

	@Override
	protected void contributeToFromLabelSection(Composite parent) {
		Button btnFilter = new Button(parent, SWT.PUSH);
		Image image = IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INTELLIGENCE_FILTER_ICON);		
		btnFilter.setImage(image);
		btnFilter.setToolTipText(Messages.IntelligenceFilteredSelectComposite_Filter_Tooltip);
		btnFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnFilter.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IntelligenceFilterDialog dailog = new IntelligenceFilterDialog(getShell(), IntelligenceFilteredSelectComposite.this);
				dailog.open();
			}

		});
	}

	@Override
	protected void fireChangeListeners() {
		//we don't want to fire listeners after filtering operation 
		if (!isStopEventPropogation()) {
			super.fireChangeListeners();
		}
		setStopEventPropogation(false);
	}
	
	@Override
	public IntelligenceViewFilter getFilter() {
		return filter;
	}
	
	@Override
	public void updateContent() {
		availableList = loadIntelligences();
		//we don't want to fire change listeners after filtering operation 
		setStopEventPropogation(true);
		setItemsData(availableList, getSelectedItems());		
	}

	public void setSelectedIntelligences(List<Intelligence> intelligences) {
		if (availableList == null) {
			availableList = loadIntelligences();
		}
		setItemsData(availableList, intelligences);		
	}
	
    private List<Intelligence> loadIntelligences() {
    	Session s = HibernateManager.openSession();
    	try {
    		Query query = filter.buildQuery(s);
    		List<?> results = query.list();
    		List<Intelligence> intelligences = new ArrayList<Intelligence>();
    		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
    			Object[] data = (Object[]) iterator.next();
    			Intelligence i = new Intelligence();
    			i.setUuid((UUID)data[0]);
    			i.setName((String)data[1]);
    			i.setReceivedDate((Date)data[2]);
    			intelligences.add(i);
    		}
    		return intelligences;
    	} finally {
    		s.close();
    	}
    }

	private boolean isStopEventPropogation() {
		return stopEventPropogation;
	}

	private void setStopEventPropogation(boolean stopEventPropogation) {
		this.stopEventPropogation = stopEventPropogation;
	}

}
