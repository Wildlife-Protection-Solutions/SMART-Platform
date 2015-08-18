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
package org.wcs.smart.query.ui.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;

/**
 * Conservation area filter panel.
 * @author Emily
 *
 */
public class ConservationAreaFilterPanel implements IDefinitionPanel, SelectionListener{
	
	/**
	 * Panel Id
	 */
	public static final String ID = "org.wcs.smart.query.conservationAreaPanel"; //$NON-NLS-1$
	
	public static final String PANEL_TITLE = Messages.ConservationAreaFilterPanel_CaFilterPanelTitle;
	
	private SelectionListener selectionListener;
	
	private Button btnIncludeAll, btnFilter;
	
	private Composite main;
	
	private Composite caWarning;
	private Composite content;
	private ScrolledComposite scroll ;
	
	private Label lblWarn;
	
	private Composite caList = null;
	private Color yellow = null;
	private List<UUID> missingFilterUuids = new ArrayList<UUID>();
	
	private QueryProxy currentQuery;
	
	@Override
	public void dispose(){
		main.dispose();
	}
	
	public ConservationAreaFilterPanel(){
	}
	
	public Composite createComposite(Composite parent){
		main = new Composite(parent, SWT.NONE);
		main.setLayout(new FillLayout());
		((FillLayout)main.getLayout()).marginHeight = 0;
		((FillLayout)main.getLayout()).marginWidth = 0;
		
		scroll = new ScrolledComposite(main, SWT.NONE | SWT.H_SCROLL | SWT.V_SCROLL);
		scroll.setAlwaysShowScrollBars(false);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		
		content = new Composite(scroll, SWT.NONE);
		content.setLayout(new GridLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		caWarning = new Composite(content, SWT.NONE | SWT.BORDER);
		caWarning.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		yellow = new Color(Display.getDefault(), new RGB(255, 255, 212));
		main.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (yellow != null){
					yellow.dispose();
				}
			}
		});
		caWarning.setLayout(new GridLayout(2, false));
		caWarning.setBackground(yellow);
		
		Label lblWarnImage = new Label(caWarning, SWT.NONE);
		lblWarnImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		lblWarnImage.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		lblWarnImage.setBackground(yellow);
		
		lblWarn = new Label(caWarning, SWT.WRAP);
		lblWarn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblWarn.setBackground(yellow);
		
		btnIncludeAll = new Button(content, SWT.RADIO);
		btnIncludeAll.setText(Messages.ConservationAreaFilterPanel_IncludeAllCaOp);
		btnIncludeAll.addSelectionListener(this);
		
		btnFilter = new Button(content, SWT.RADIO);
		btnFilter.setText(Messages.ConservationAreaFilterPanel_FilterCaOp);
		btnFilter.addSelectionListener(this);
		
		caList = new Composite(content, SWT.CHECK | SWT.NONE );
		GridLayout gl = new GridLayout();
		gl.marginLeft = 15;
		caList.setLayout(gl);
		caList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		content.pack();
		scroll.setContent(content);
		scroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return main;
	}
	
	
	
	private void radioSelected(){
		for(Control c : caList.getChildren()){
			c.setEnabled(btnFilter.getSelection());
		}
		
		missingFilterUuids.clear();
		if (caWarning.isVisible()){
			caWarning.setVisible(false);
			lblWarn.setText(""); //$NON-NLS-1$
			((GridData)lblWarn.getLayoutData()).widthHint = SWT.DEFAULT;
			((GridData)caWarning.getLayoutData()).heightHint = 0;
			
			scroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			main.getParent().getParent().getParent().layout(true, true);
		}
	}
	
	/**
	 * Sets the selection listener.  Only one selection listener
	 * can be registered at a time. 
	 * 
	 * @param selectionListener
	 */
	public void setSelectionListener(SelectionListener selectionListener){
		this.selectionListener = selectionListener;
	}
	

	/**
	 * Initializes the panel with the information from the query.
	 * 
	 * @param query
	 */
	@Override
	public void initItems(QueryProxy query){
		this.currentQuery = query;
		ConservationAreaFilter temp = ConservationAreaFilter.parseFilter(query.getQuery().getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
		temp.refreshMissingList(SmartDB.getConservationAreaConfiguration().getConservationAreas());
		missingFilterUuids.clear();
		
		for (Control c : caList.getChildren()){
			c.dispose();
		}
		
		for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
			Button btnCa = new Button(caList, SWT.CHECK);
			btnCa.setText(ca.getNameLabel());
			btnCa.setData(ca);
			btnCa.setSelection(false);
		
			for (int i = 0; i < temp.getConservationAreaFilterIds().size(); i ++){
				if (temp.getConservationAreaFilterIds().get(i).equals(ca.getUuid())){
					btnCa.setSelection(true);
				}
			}
			btnCa.addSelectionListener(this);
		}

		if (temp.getMissingCas() != null){
			missingFilterUuids.addAll(temp.getMissingCas());
		}
		
		if (missingFilterUuids.size() > 0){
			caWarning.setVisible(true);
			int w = content.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			lblWarn.setText(Messages.ConservationAreaFilterPanel_CaFilterError);
			((GridData)lblWarn.getLayoutData()).widthHint = w;
			((GridData)caWarning.getLayoutData()).heightHint = SWT.DEFAULT;
		}else{
			caWarning.setVisible(false);
			lblWarn.setText(""); //$NON-NLS-1$
			((GridData)lblWarn.getLayoutData()).widthHint = SWT.DEFAULT;
			((GridData)caWarning.getLayoutData()).heightHint = 0;
		}

		boolean sel = temp.includeAll();
		btnIncludeAll.setSelection(sel);
		btnFilter.setSelection(!sel);
		
		for(Control c : caList.getChildren()){
			c.setEnabled(btnFilter.getSelection());
		}
		
		scroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		main.getParent().getParent().getParent().layout(true, true);		
	}
	
	/**
	 * 
	 * @return the selected conservation areas as filters 
	 */
	public ConservationAreaFilter getCaFilter(){
		
		if (validate() != null) return null;
		
		ConservationAreaFilter filter = new ConservationAreaFilter();
		if (btnIncludeAll.getSelection()){
			filter.setIncludeAll(true);
		}else{
			filter.setIncludeAll(false);
			for (Control c : caList.getChildren()){
				if (c instanceof Button && c.getData() instanceof ConservationArea && ((Button)c).getSelection()){
					filter.addConservationArea((ConservationArea)c.getData());
				}
			}
			/* add back missing uuid */
			filter.setMissingConservationAreas(missingFilterUuids);
		}
		return filter;
	}
	
	/**
	 * 
	 * @return <code>null</code> if filter is valid otherwise error message
	 */
	@Override
	public String validate(){
		if (btnIncludeAll.getSelection()){
			return null;
		}
		for (Control c : caList.getChildren()){
			if (c instanceof Button && c.getData() instanceof ConservationArea && ((Button)c).getSelection()){
				//at least one ca is selected which is all we care about
				return null;
			}
		}
		return Messages.ConservationAreaFilterPanel_Error_NoCas;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		radioSelected();	//update ui
		if (selectionListener != null){
			selectionListener.widgetSelected(e);
		}
		//fire the query changed event
		if (currentQuery != null){
			fireQueryChangedListeners();
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getGuiName() {
		return PANEL_TITLE;
	}

	@Override
	public void addItem(DropItem item) {
		//do nothing does not support drag and drop
	}

	@Override
	public void removeItem(DropItem item) {
		//do nothing does not support drag and drop
	}


	@Override
	public String getQueryPart() {
		return getCaFilter().asString();
	}

	@Override
	public void saveItems(QueryProxy q) {
		
	}


	@Override
	public void clear() {
		//do nothing
	}

	@Override
	public void finishDrag(DropItem item) {
		//do nothing
	}

	@Override
	public void fireQueryChangedListeners() {
		QueryEventManager.getInstance().fireQueryDefinitionModified(currentQuery.getQuery());
	}

	@Override
	public Composite getDropTargetComposite() {
		return main;
	}

	@Override
	public void redraw() {
		
	}
}
