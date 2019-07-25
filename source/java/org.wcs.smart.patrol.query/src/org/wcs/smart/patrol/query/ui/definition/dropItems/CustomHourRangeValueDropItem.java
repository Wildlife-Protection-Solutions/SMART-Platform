/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.ui.definition.dropItems;

import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.ui.TimePicker;

/**
 * Extension of a patrol value drop item that supports the collection
 * of a time range.  
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class CustomHourRangeValueDropItem extends PatrolValueDropItem{

	private TimePicker sTime, eTime;
	private int[] initValues = null;
	
	public CustomHourRangeValueDropItem(PatrolValueOption item) {
		super(item);
	}

	@Override
	public String getValueQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("patrol:sum:"); //$NON-NLS-1$
		sb.append(item.getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		sb.append(sTime.getTimeInSeconds());
		sb.append(":"); //$NON-NLS-1$
		sb.append(eTime.getTimeInSeconds());
		
		return sb.toString();
	}
	
	@Override
	protected void createValueComposite(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(3, false));
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		((GridLayout)part.getLayout()).horizontalSpacing = 0;
		
		Label lbl = new Label(part, SWT.NONE);
		lbl.setText( formatStringForLabel(item.getGuiName(Locale.getDefault())));
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		lbl.setToolTipText("Computes the number of times a patrol\ninterests the selected time range.");
		initDrag(lbl);
		
		sTime = new TimePicker(part, SWT.NONE);
		sTime.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		
		lbl = new Label(part, SWT.NONE);
		lbl.setText( Messages.CustomHourRangeValueDropItem_tolabel );
		initDrag(lbl);
		
		eTime = new TimePicker(part, SWT.NONE);
		eTime.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		
		if (initValues != null) {
			sTime.setTimeInSeconds(initValues[0]);
			eTime.setTimeInSeconds(initValues[1]);
		}else {
			sTime.setTimeInSeconds(20*3600);
			eTime.setTimeInSeconds(6*3600);
		}

		sTime.addListener(e->queryChanged());
		eTime.addListener(e->queryChanged());
		
		initDrag(part);
		
	}
	
	@Override
	protected void queryChanged() {
		initValues = new int[] {sTime.getTimeInSeconds(), eTime.getTimeInSeconds()};
		super.queryChanged();
	}
	
	@Override
	protected void updateUi(){
		super.updateUi();
		
		main.layout();
		main.redraw();
	}
	
	/**
	 * @param data expects a list of numbers {starttime, endtime}
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#initializeValueData(java.lang.Object)
	 */
	@Override
	protected void initializeValueData(Object data) {
		if (data instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<Integer> items = (List<Integer>)data;
			
			if (sTime == null) {
				initValues = new int[] {items.get(0), items.get(1)};
			}else {
				sTime.setTimeInSeconds(items.get(0));
				eTime.setTimeInSeconds(items.get(1));
			}
		}
	}
}
