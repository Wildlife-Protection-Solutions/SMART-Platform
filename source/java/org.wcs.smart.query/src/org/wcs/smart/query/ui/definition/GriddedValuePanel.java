package org.wcs.smart.query.ui.definition;

import java.util.ArrayList;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.ListDropTargetPanel;

public class GriddedValuePanel {

	private Text txtGridSize;
	private ListDropTargetPanel lstValues;
	
	public void clear() {
		lstValues.clear();		
	}

	public String getQueryString() {
		return  lstValues.getQueryString();
	}

	public void init(GriddedQuery query) {
		lstValues.addElements(query.getValueDropItems());
	}

	public void saveDropItems(GriddedQuery query) {
		ArrayList<DropItem> items = new ArrayList<DropItem>();
		items.addAll(lstValues.getItems());
		query.setValueDropItems(items);		
	}

	public void addItem(DropItem item) {
		lstValues.addElement(item);
	}

	public Composite createComposite(Composite parent, QueryDefView parentView) {
		SashForm main = new SashForm(parent, SWT.HORIZONTAL );
		
		Composite left = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		left.setLayout(gl);
		
		Label lbl = new Label(left, SWT.NONE);
		lbl.setText("Grid Size (in meters):");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		txtGridSize = new Text(left, SWT.BORDER);
		txtGridSize.setTextLimit(6);
		txtGridSize.setText("100");
		
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		data.widthHint = 20;
		txtGridSize.setLayoutData(data);
		
		
		
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
		lblValues.setText("Values");
		lblValues.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblValues.setToolTipText("Add values to compute here from the 'Value Options' section of the Query Filters tree.");
		
		lstValues = new ListDropTargetPanel(parentView, false);
		Composite comp = lstValues.createComposite(right);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return main;
	}


	
}
