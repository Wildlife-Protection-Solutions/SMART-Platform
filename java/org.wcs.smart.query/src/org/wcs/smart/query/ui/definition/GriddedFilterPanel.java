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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.FilterDropTargetPanel;

/**
 * Filter Panel for gridded query
 * 
 * @author Emily
 *
 */
public class GriddedFilterPanel {

	private FilterDropTargetPanel valueFilter = null;
	private FilterDropTargetPanel rateFilter = null;
	
	private QueryDefView view;
	private FilterDropTargetPanel currentTarget = null;
	
	private SashForm main ;
	private Composite right;
	private Composite left;
	private Font smallerFont = null;
	
	public GriddedFilterPanel(QueryDefView view){
		this.view = view;
	}
	
	public void dispose(){
		if (valueFilter != null){
			valueFilter.dispose();
		}
		if (rateFilter != null){
			rateFilter.dispose();
		}
		if (smallerFont != null){
			smallerFont.dispose();
		}
	}
	/**
	 * Creates the drop target composite
	 * @param parent
	 * @return
	 */
	public Composite createComposite(Composite parent) {
		 
		main = new SashForm(parent, SWT.HORIZONTAL );

		/* left panel - value filter */
		left = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 2;
		gl.marginHeight = 2;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		left.setLayout(gl);
		left.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (currentTarget == valueFilter && right.isVisible()){
					e.gc.setLineWidth(2);
					e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
					e.gc.drawRectangle(1,1,left.getBounds().width - 6, left.getBounds().height - 6);
				}
			}
		});
		
		
		Composite leftInner = new Composite(left, SWT.NONE);
		gl = new GridLayout(2, false);
		leftInner.setLayout(gl);
		leftInner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblValueFilter = new Label(leftInner, SWT.NONE);
		lblValueFilter.setText(Messages.GriddedFilterPanel_ValueFilterLabel);
		lblValueFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblValueFilter.setToolTipText(Messages.GriddedFilterPanel_ValueFilterTooltip);
		lblValueFilter.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				setCurrent(valueFilter);
			}
			
		});
		
		Link lblClear = new Link(leftInner, SWT.NONE);
		lblClear.setText("<a>" + Messages.GriddedFilterPanel_ClearLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		FontData fd = lblClear.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight() - 2);
		smallerFont = new Font(lblClear.getDisplay(), fd);
		lblClear.setFont(smallerFont);
		lblClear.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		lblClear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				valueFilter.clear();
				valueFilter.validate();
			}
		});
		
		Label lblSep2 = new Label(leftInner, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSep2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		/* right panel value filter */
		right = new Composite(main, SWT.BORDER);
		gl = new GridLayout(1, false);
		gl.marginWidth = 2;
		gl.marginHeight = 2;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		right.setLayout(gl);
		right.addPaintListener(new PaintListener() {			
			@Override
			public void paintControl(PaintEvent e) {
				if (currentTarget == rateFilter && right.isVisible()){
					e.gc.setLineWidth(2);
					e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
					e.gc.drawRectangle(1,1,right.getBounds().width - 6, right.getBounds().height - 6);
				}
			}
		});

		Composite rightInner = new Composite(right, SWT.NONE);
		gl = new GridLayout(3, false);
		rightInner.setLayout(gl);
		rightInner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblRateFilter = new Label(rightInner, SWT.NONE);
		lblRateFilter.setText(Messages.GriddedFilterPanel_RateFilterLabel);
		lblRateFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		lblRateFilter.setToolTipText(Messages.GriddedFilterPanel_RateFilterTooltip);
		lblRateFilter.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				setCurrent(rateFilter);
			}
			
		});
		
		Link lblClear2 = new Link(rightInner, SWT.NONE);
		lblClear2.setText("<a>" + Messages.GriddedFilterPanel_ClearLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		lblClear2.setFont(smallerFont);
		lblClear2.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		lblClear2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				rateFilter.clear();
				rateFilter.validate();
			}
		});
		
		Link lblCopy = new Link(rightInner, SWT.NONE);
		lblCopy.setText("<a>" + Messages.GriddedFilterPanel_CopyLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		lblCopy.setFont(smallerFont);
		lblCopy.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		lblCopy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyValueFilterToRateFilter();
				
			}
		});
		
		Label lblSep = new Label(rightInner, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

		//filter panels
		
		
		valueFilter = new FilterDropTargetPanel(view);		
		valueFilter.createComposite(left);
		
		rateFilter = new FilterDropTargetPanel(view);
		rateFilter.createComposite(right);
		
		valueFilter.addDropTargetPanel(rateFilter);
		rateFilter.addDropTargetPanel(valueFilter);
		
		setCurrent(valueFilter);
		
		return main;
	}
	
	public void updateFilterPanel(boolean needsRateFilter){	
		right.setData(needsRateFilter);
		right.setVisible(needsRateFilter);			
		if (!needsRateFilter && currentTarget == rateFilter){
			currentTarget = valueFilter;
		}
		main.layout();
		left.redraw();
		right.redraw();
		
	}
	private void copyValueFilterToRateFilter(){
		String queryString = valueFilter.getQueryString();
		if (queryString == null || queryString.trim().isEmpty()){
			return;
		}
		Session session = null;
		try{
			InputStream is = new ByteArrayInputStream(queryString.getBytes());
			Parser parser = new Parser(is);
			IFilter filterPart = parser.ExpressionPart();
			is.close();
		
			//---- generate drop items for value filter
			session = HibernateManager.openSession();
			session.beginTransaction();
			List<DropItem> copies = new ArrayList<DropItem>();
			if (filterPart != null){
				DropItem[] filterItems = filterPart.getDropItems(session);
				for (int i = 0; i < filterItems.length; i ++){
					copies.add(filterItems[i]);
				}
			}
			session.getTransaction().rollback();
			rateFilter.addElements(copies);
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.GriddedFilterPanel_CopyError, ex);
			if (session != null && session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
		}finally{
			if (session != null){
				session.close();
			}
		}
	}

	private void setCurrent(FilterDropTargetPanel target){
		this.currentTarget = target;
		left.redraw();
		right.redraw();
	}
	
	public void clear(){
		valueFilter.clear();
		rateFilter.clear();
		
	}
	
	public String getQueryString(){
		StringBuilder queryText = new StringBuilder(valueFilter.getQueryString());
		queryText.append("|"); //$NON-NLS-1$
		if ((Boolean)right.getData()){
			queryText.append(rateFilter.getQueryString());
		}
		return queryText.toString();
	}
	
	public void addValueFilterElements(List<DropItem> items){
		valueFilter.addElements(items);
	}
	public void addRateFilterElements(List<DropItem> items){
		rateFilter.addElements(items);
	}
	public void addElement(DropItem item){
		if (currentTarget != null){
			currentTarget.addElement(item);
		}
	}
	public List<DropItem> getValueItems(){
		return valueFilter.getItems();
	}
	public List<DropItem> getRateItems(){
		return rateFilter.getItems();
	}

}
