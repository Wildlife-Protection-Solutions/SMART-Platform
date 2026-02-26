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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
 * 
 * users picks the date/time as the local value but the filters
 * returns convert this to utc
 * @since 8.1.2
 */
public class UTCDateTimeDropItem extends DropItem {
	
	private DateTime date1;
	private DateTime date2;
	private DateTime time1;
	private DateTime time2;
	
	protected String text;
	protected String queryKey;
	
	private ComboViewer operators;

	private LocalDateTime currentDate1;
	private LocalDateTime currentDate2;
	
	private Operator currentOperator;

	private boolean canEdit;
	
	public UTCDateTimeDropItem(String text, String queryKey, boolean canEdit) {
		this.text = text;
		this.queryKey = queryKey;
		this.canEdit = canEdit;
		
		this.currentDate1 = LocalDate.now().atStartOfDay();
		this.currentDate2 = LocalDate.now().atTime(LocalTime.MAX);
	}
	

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
	
		LocalDateTime utcdt = toDateTime(1).atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
		LocalDateTime utcdt2 = toDateTime(2).atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

		
		String d1 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(utcdt);
		String d2 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(utcdt2);
		
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
		
		LocalDateTime utcdt = toDateTime(1).atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
		
		querypart.append((DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(utcdt));
				
		querypart.append( " "); //$NON-NLS-1$
		querypart.append( Operator.AND.getKey() );
		querypart.append( " "); //$NON-NLS-1$
		
		LocalDateTime utcdt2 = toDateTime(2).atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
		
		querypart.append((DateTimeFormatter.ofPattern(IQueryFilter.DATETIME_FORMAT_STR)).format(utcdt2));
		
		return querypart.toString();
	}

	private LocalDateTime toDateTime(int widget) {
		if (widget == 1) {
			LocalDate d1 = SmartUtils.toDate(date1);
			LocalTime t1 = SmartUtils.toTime(time1);
			return d1.atTime(t1);
		}else if (widget == 2) {
			LocalDate d1 = SmartUtils.toDate(date2);
			LocalTime t1 = SmartUtils.toTime(time2);
			return d1.atTime(t1);
		}
		return LocalDateTime.now();
		
		
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
		GridLayout layout = new GridLayout(7, false);
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
		
			
		date1 = new DateTime(main, SWT.DROP_DOWN | SWT.MEDIUM | SWT.DATE );
		date1.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				LocalDateTime newValue = toDateTime(1);
				if (currentDate1 == null || !newValue.isEqual(currentDate1)){
					queryChanged();
					currentDate1 = newValue;			
				}
			}});
		
		
		time1 = new DateTime(main, SWT.DROP_DOWN | SWT.MEDIUM | SWT.TIME );
		time1.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				LocalDateTime newValue = toDateTime(1);
				if (currentDate1 == null || !newValue.isEqual(currentDate1)){
					queryChanged();
					currentDate1 = newValue;			
				}
			}});
		Label l  = new Label(main, SWT.NONE);
		l.setText(Operator.AND.getLabel(Locale.getDefault()));
		
		date2 = new DateTime(main, SWT.DROP_DOWN | SWT.MEDIUM | SWT.DATE);
		date2.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				LocalDateTime newValue = toDateTime(2);
				if (currentDate2 == null || !newValue.isEqual(currentDate1)){
					queryChanged();
					currentDate2 = newValue;			
				}
			}});
		
		time2 = new DateTime(main, SWT.DROP_DOWN | SWT.MEDIUM | SWT.TIME);
		time2.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				LocalDateTime newValue = toDateTime(2);
				if (currentDate2 == null || !newValue.isEqual(currentDate1)){
					queryChanged();
					currentDate2 = newValue;			
				}
			}});
		
		operators.getControl().setEnabled(canEdit);
		date1.setEnabled(canEdit);
		date2.setEnabled(canEdit);
		time1.setEnabled(canEdit);
		time2.setEnabled(canEdit);
		
		initDrag(main);
		initDrag(lblAttribute);
		
		
		SmartUtils.initDateTimeWidget(date1, currentDate1.toLocalDate());
		SmartUtils.initDateTimeWidget(time1, currentDate1.toLocalTime());
		
		
		SmartUtils.initDateTimeWidget(date2, currentDate2.toLocalDate());
		SmartUtils.initDateTimeWidget(time2, currentDate2.toLocalTime());
	}

	public void setInitialValue(Operator op, LocalDateTime d1, LocalDateTime d2) {
		
		//convert d1, d2 provided in utc
		this.currentDate1 = d1.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneOffset.systemDefault()).toLocalDateTime();
		this.currentDate2= d2.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneOffset.systemDefault()).toLocalDateTime();

		
		this.currentOperator = op;
		//this.currentDate1 = d1;
		//this.currentDate2 = d2;
	}
	
}
