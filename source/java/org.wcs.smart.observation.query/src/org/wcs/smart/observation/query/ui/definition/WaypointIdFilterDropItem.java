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
package org.wcs.smart.observation.query.ui.definition;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
/**
 * Drop item for filtering on waypoint source field.
 * @author Emily
 *
 */
public class WaypointIdFilterDropItem extends DropItem implements IFilterDropItem{

	private Text txtOption;
	private ComboViewer opViewer;
	
	private Font smallerFont;
	
	private String currentSelection = null;
	private Operator currentOperator = null;
	
	
	/**
	 * Creates waypoint source drop item
	 * 
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the category attribute to make up the drop item
	 */
	public WaypointIdFilterDropItem() {
		super();
	}

	
	/**
	 * @param data - an array of the {Operator, IWaypointSource} 
	 */
	public void initializeData(Object data){
		currentOperator = (Operator) ((Object[])data)[0];
		currentSelection = (String)((Object[])data)[1];
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		txtOption = null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return "wpn:id" + " " + opViewer.getCombo().getText() + " " + txtOption.getText(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder query = new StringBuilder("wpn:id"); //$NON-NLS-1$
		query.append(" "); //$NON-NLS-1$
		query.append(currentOperator.asSmartValue());
		query.append(" "); //$NON-NLS-1$
		query.append("\""); //$NON-NLS-1$
		query.append(txtOption.getText());
		query.append("\""); //$NON-NLS-1$
		return query.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.WaypointIdFilterDropItem_IdFieldName);
		
		/* -- operator viewer **/
		opViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		opViewer.setContentProvider(ArrayContentProvider.getInstance());
		opViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				return ((Operator)element).getGuiValue();
			}
		});
		opViewer.setInput(new Operator[]{Operator.STR_EQUALS, Operator.STR_CONTAINS});
		if (currentOperator == null){
			currentOperator = Operator.STR_EQUALS;
		}
		opViewer.setSelection(new StructuredSelection(currentOperator));
		
		/* -- list viewer **/
		txtOption = new Text(main, SWT.BORDER);
		txtOption.addListener(SWT.Modify, e->{
			if (txtOption.getText().equalsIgnoreCase(currentSelection)) return;
			
			currentSelection = txtOption.getText();
			queryChanged();	
		});
		txtOption.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtOption.getLayoutData()).widthHint = 150;
		if (currentSelection != null) {
			txtOption.setText(currentSelection);
		}
		opViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Operator newSelection = (Operator) ((IStructuredSelection)opViewer.getSelection()).getFirstElement();
				if (! (currentOperator != null && currentOperator.equals(newSelection))){
					queryChanged();	
				}			
				currentOperator = newSelection;
			}
		});
		
		initDrag(main);
		initDrag(l);
	}
}
