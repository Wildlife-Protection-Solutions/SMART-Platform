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
package org.wcs.smart.i2.ui.views.query.dropitem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.util.SmartUtils;

public class DateDropItem extends DropItem {

	private DateTime dtime1;
	private DateTime dtime2;
	
	protected String text;
	protected String queryKey;
	
	private ComboViewer operators;

	private Date currentValue1;
	private Date currentValue2;
	private Operator currentOperator;

	public DateDropItem(String text, String queryKey) {
		this.text = text;
		this.queryKey = queryKey;
	}
	

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		String d1 = SmartUtils.getDate(dtime1).toString();
		String d2 = SmartUtils.getDate(dtime1).toString();
		return this.text + " " + getOperatorSelection().getLabel(Locale.getDefault()) + " " + d1 + " " + d2; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder querypart = new StringBuilder();
	
		querypart.append(this.queryKey);
		querypart.append( " "); //$NON-NLS-1$
		querypart.append(getOperatorSelection().getKey());
		querypart.append( " "); //$NON-NLS-1$
		querypart.append((new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(SmartUtils.getDate(dtime1)));
		querypart.append( " "); //$NON-NLS-1$
		querypart.append( Operator.AND.getKey() );
		querypart.append( " "); //$NON-NLS-1$
		querypart.append((new SimpleDateFormat(IQueryFilter.DATE_FORMAT_STR)).format(SmartUtils.getDate(dtime2)));
		
		return querypart.toString();
	}

	private Operator getOperatorSelection(){
		return (Operator) ((IStructuredSelection)operators.getSelection()).getFirstElement();
	}
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		Label lblAttribute = new Label(main, SWT.NONE);
		lblAttribute.setText(formatStringForLabel(text));
		
		
		operators = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		operators.setContentProvider(ArrayContentProvider.getInstance());
		operators.setLabelProvider(new OperatorLabelProvider());
		operators.setInput(Operator.DATE_OPS);
		
		operators.getCombo().addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (currentOperator != null
							&& currentOperator.equals(getOperatorSelection())) {
						// no change
					} else {
						currentOperator = getOperatorSelection();
						queryChanged();
					}
				}
			});
		
		if (currentOperator == null){
			currentOperator = Operator.DATE_OPS[0];
		}
		operators.setSelection(new StructuredSelection(currentOperator));
		FontData fd = (operators.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		Font smallerFont = new Font(Display.getCurrent(), fd);
		operators.getCombo().setFont(smallerFont);
		operators.getCombo().addListener(SWT.Dispose, e->smallerFont.dispose());
		
			
		dtime1 = new DateTime(main, SWT.DROP_DOWN | SWT.DATE | SWT.MEDIUM);
		dtime1.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				Date newValue = (new java.sql.Date(SmartUtils.getDate(dtime1).getTime()));
				if (!newValue.equals(currentValue1)){
					queryChanged();
					currentValue1 = newValue;
				}
			}});
		
		dtime2 = new DateTime(main, SWT.DROP_DOWN | SWT.DATE | SWT.MEDIUM);
		dtime2.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				Date newValue = (new java.sql.Date(SmartUtils.getDate(dtime2).getTime()));
				if (!newValue.equals(currentValue1)){
					queryChanged();
					currentValue2 = newValue;
				}
			}});
		
		
		initDrag(main);
		initDrag(lblAttribute);
		
		if (dtime1 != null && currentValue1 != null){
			SmartUtils.initDateDateTimeWidget(dtime1, currentValue1);	
		}
		if (dtime2 != null && currentValue2 != null){
			SmartUtils.initDateDateTimeWidget(dtime2, currentValue2);	
		}
		
	}

	public void setInitialValue(Operator op, Date d1, Date d2) {
		this.currentOperator = op;
		this.currentValue1 = d1;
		this.currentValue2 = d2;
	}
}
