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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.ListDropTargetPanel;

/**
 * Query definition panel for gridded queries.
 * Collects the grid information and the 
 * value to compute.
 * 
 * @author Jeff
 *
 */
public class GriddedValuePanel {

	private Text txtGridSize;
	private ListDropTargetPanel lstValues;
	
	/**
	 * Clears values
	 */
	public void clear() {
		lstValues.clear();		
	}

	/**
	 * @return the value part of the gridded query
	 */
	public String getQueryString() {
		return  lstValues.getQueryString();
	}

	/**
	 * Initializes the panel with the query values
	 * @param query
	 */
	public void init(GriddedQuery query) {
		lstValues.addElements(query.getValueDropItems());
		txtGridSize.setText(Double.toString(query.getGridSize()));
	}

	/**
	 * Saves drop items to query
	 * @param query
	 */
	public void saveDropItems(GriddedQuery query) {
		ArrayList<DropItem> items = new ArrayList<DropItem>();
		items.addAll(lstValues.getItems());
		query.setValueDropItems(items);		
	}

	/**
	 * Adds drop item to values
	 * @param item
	 */
	public void addItem(DropItem item) {
		lstValues.addElement(item);
	}

	public Composite createComposite(Composite parent, final QueryDefView parentView) {
		SashForm main = new SashForm(parent, SWT.HORIZONTAL );
		
		Composite left = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		left.setLayout(gl);
		
		Composite leftInner = new Composite(left, SWT.NONE);
		leftInner.setLayout(new GridLayout(2, false));
		leftInner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblGridDef = new Label(leftInner, SWT.NONE);
		lblGridDef.setImage(JFaceResources.getImageRegistry().get(QueryPlugIn.GRID_ICON));
		
		Label lblDef = new Label(leftInner, SWT.NONE);
		lblDef.setText("Grid Definition");
		lblDef.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblDef.setToolTipText("Define the grid here.");
		
		Composite leftMain = new Composite(left, SWT.NONE);
		leftMain.setLayout(new GridLayout(2, false));
		leftMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		leftMain.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		Label lbl = new Label(leftMain, SWT.NONE);
		lbl.setText("Grid Size (in meters):");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		txtGridSize = new Text(leftMain, SWT.BORDER);
		txtGridSize.setTextLimit(6);
		txtGridSize.setText("1");
		
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		data.widthHint = 20;
		txtGridSize.setLayoutData(data);
		txtGridSize.addListener(SWT.Modify, new Listener(){

			@Override
			public void handleEvent(Event event) {
				parentView.fireQueryModifiedListeners();
			}
		});
		
		
		Composite right = new Composite(main, SWT.BORDER);
		GridLayout rgl = new GridLayout(2, false);
		rgl = new GridLayout(1, false);
		rgl.marginWidth = 0;
		rgl.marginHeight = 0;
		rgl.verticalSpacing = 0;
		rgl.horizontalSpacing = 0;
		right.setLayout(rgl);
		
		Composite rightInner = new Composite(right, SWT.NONE);
		gl = new GridLayout(2, false);
		rightInner.setLayout(gl);
		
		Label lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setImage(JFaceResources.getImageRegistry().get(QueryPlugIn.VALUE_ICON));
		
		lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setText("Grid Value");
		lblValues.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblValues.setToolTipText("Add values to compute here from the 'Value Options' section of the Query Filters tree.");
		
		lstValues = new ListDropTargetPanel(parentView, false);
		Composite comp = lstValues.createComposite(right);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return main;
	}

	/**
	 * 
	 * @return the grid size
	 */
	protected double getGridSize(){
		return Double.parseDouble( txtGridSize.getText() );
	}
	
}
