/*
 * Copyright (C) 2024 Wildlife Conservation Society
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


import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

/**
 * @since 8.1.0
 */
public class DateTimeDropItem extends DropItem {

	public enum Type{DATE, TIME};
	
	private DateTime dtime1;
	private DateTime dtime2;
	
	protected String text;
	protected String queryKey;
	
	private ComboViewer operators;

	private LocalDate currentDate1;
	private LocalDate currentDate2;
	private LocalTime currentTime1;
	private LocalTime currentTime2;
	private Operator currentOperator;

	private boolean canEdit;
	private Type type;
	
	public DateTimeDropItem(Type type, String text, String queryKey, boolean canEdit) {
		this.type = type;
		this.text = text;
		this.queryKey = queryKey;
		this.canEdit = canEdit;
	}
	

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		String d1 = ""; //$NON-NLS-1$
		String d2 = ""; //$NON-NLS-1$
	
		if (type == Type.DATE) {
			d1 = DateTimeFormatter.ISO_LOCAL_DATE.format(SmartUtils.toDate(dtime1));
			d2 = DateTimeFormatter.ISO_LOCAL_DATE.format(SmartUtils.toDate(dtime2));
		}else {
			d1 = DateTimeFormatter.ISO_LOCAL_TIME.format(SmartUtils.toTime(dtime1));
			d2 = DateTimeFormatter.ISO_LOCAL_TIME.format(SmartUtils.toTime(dtime2));
		}
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
		if (type == Type.DATE) {
			querypart.append((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(SmartUtils.toDate(dtime1)));
		}else if (type == Type.TIME) {
			querypart.append((DateTimeFormatter.ofPattern(IQueryFilter.TIME_FORMAT_STR)).format(SmartUtils.toTime(dtime1)));
		}
		querypart.append( " "); //$NON-NLS-1$
		querypart.append( Operator.AND.getKey() );
		querypart.append( " "); //$NON-NLS-1$
		if (type == Type.DATE) {
			querypart.append((DateTimeFormatter.ofPattern(IQueryFilter.DATE_FORMAT_STR)).format(SmartUtils.toDate(dtime2)));
		}else if (type == Type.TIME) {
			querypart.append((DateTimeFormatter.ofPattern(IQueryFilter.TIME_FORMAT_STR)).format(SmartUtils.toTime(dtime2)));
		}
		
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
		
			
		int style = type == Type.DATE ? SWT.DATE : SWT.TIME;
		dtime1 = new DateTime(main, SWT.DROP_DOWN | style | SWT.MEDIUM);
		dtime1.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (type == Type.DATE) {
					LocalDate newValue = SmartUtils.toDate(dtime1);
					if (!newValue.isEqual(currentDate1)){
						queryChanged();
						currentDate1 = newValue;
					}
				}else if (type == Type.TIME) {
					LocalTime newValue = SmartUtils.toTime(dtime1);
					if (!newValue.equals(currentTime1)){
						queryChanged();
						currentTime1 = newValue;
					}
				}
			}});
		
		dtime2 = new DateTime(main, SWT.DROP_DOWN | style | SWT.MEDIUM);
		dtime2.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (type == Type.DATE) {
					LocalDate newValue = SmartUtils.toDate(dtime2);
					if (!newValue.isEqual(currentDate2)){
						queryChanged();
						currentDate2 = newValue;
					}
				}else if (type == Type.TIME) {
					LocalTime newValue = SmartUtils.toTime(dtime2);
					if (!newValue.equals(currentTime2)){
						queryChanged();
						currentTime2 = newValue;
					}
				}
			}});
		
		operators.getControl().setEnabled(canEdit);
		dtime1.setEnabled(canEdit);
		dtime2.setEnabled(canEdit);
		
		initDrag(main);
		initDrag(lblAttribute);
		
		if (type == Type.DATE) {
			if (dtime1 != null && currentDate1 != null){
				SmartUtils.initDateTimeWidget(dtime1, currentDate1);	
			}
			if (dtime2 != null && currentDate2 != null){
				SmartUtils.initDateTimeWidget(dtime2, currentDate2);	
			}
		}else if (type == Type.TIME) {
			if (dtime1 != null && currentTime1 != null){
				SmartUtils.initDateTimeWidget(dtime1, currentTime1);	
			}
			if (dtime2 != null && currentTime2 != null){
				SmartUtils.initDateTimeWidget(dtime2, currentTime2);	
			}	
		}
		
	}

	public void setInitialValue(Operator op, LocalDate d1, LocalDate d2) {
		this.currentOperator = op;
		this.currentDate1 = d1;
		this.currentDate2 = d2;
	}
	
	public void setInitialValue(Operator op, LocalTime t1, LocalTime t2) {
		this.currentOperator = op;
		this.currentTime1 = t1;
		this.currentTime2 = t2;
	}
}
