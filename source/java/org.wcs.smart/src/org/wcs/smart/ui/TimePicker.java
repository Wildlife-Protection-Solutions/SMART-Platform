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
package org.wcs.smart.ui;

import java.awt.SystemColor;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Custom widget for picking a time.  Allows users to select hours and minutes but
 * not seconds.  Value returned represents the second of the day.  The last
 * second (59) of the minute is returned.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class TimePicker extends Composite {

	private static NumberFormat nf = new DecimalFormat("00");  //$NON-NLS-1$
	
	private Composite cHour, cMin;
	private Text txtHour, txtMin;
	private Shell hourShell, minShell; 
	
	private LastValue lasthour = new LastValue();
	private LastValue lastmin = new LastValue();
	
	private List<Listener> listeners;
	
	public TimePicker(Composite parent, int style) {
		super(parent, SWT.BORDER);
		listeners = new ArrayList<>();
		
		Listener out = e->{
			if (e.widget != hourShell) hideHour();
			if (e.widget != minShell) hideMinute();
		};
		parent.getDisplay().addFilter(SWT.FocusIn, out);
		
		addListener(SWT.Dispose, e->{
			if (hourShell != null) hourShell.dispose();
			if (minShell != null) minShell.dispose();
			parent.getDisplay().removeFilter(SWT.FocusIn, out);
		});
		
		setLayout(new GridLayout(4, false));
		((GridLayout)getLayout()).marginWidth = 2;
		((GridLayout)getLayout()).marginHeight = 2;
		((GridLayout)getLayout()).horizontalSpacing = 0;
		
		cHour = createPartComposite();
		
		txtHour = createText(cHour, "12", 0, 23, lasthour); //$NON-NLS-1$
		txtHour.addListener(SWT.MouseDown, e->{showHour();});

		Label l = new Label(this, SWT.NONE);
		l.setText(":"); //$NON-NLS-1$
		l.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cMin = createPartComposite();
		
		txtMin = createText(cMin, "00", 0, 59, lastmin); //$NON-NLS-1$
		txtMin.addListener(SWT.MouseDown, e->{showMinute();});
		
		setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		txtHour.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		txtMin.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

	}
	
	public void addListener(Listener l ) {
		this.listeners.add(l);
	}
	
	private void fireListeners() {
		Event evt = new Event();
		evt.widget = this;
		for (Listener l : listeners) {
			l.handleEvent(evt);
		}
	}
	
	private Text createText(Composite parent, String initvalue, int min, int max, LastValue lastValue ) {
		Text txtItem = new Text(parent, SWT.NONE);
		txtItem.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, false, false));
		txtItem.setText(initvalue);
		txtItem.addListener(SWT.KeyDown, e->{
			if (e.keyCode == SWT.ESC || e.keyCode == SWT.CR || e.keyCode == SWT.LF) hideHour();
			if (!((e.keyCode >= '0' && e.keyCode <= '9') || e.keyCode == SWT.DEL || e.keyCode == SWT.BS )) {
				e.doit = false;
				return;
			}
			txtItem.setData(txtItem.getText());
		});
		txtItem.addListener(SWT.FocusOut, e->{
			int i = 1;
			if (!txtItem.getText().isEmpty()) {
				i = Integer.parseInt(txtItem.getText());	
			}
			txtItem.setText(nf.format(i));
		});
		txtItem.addListener(SWT.Modify, e->{
			
			int value = 0;
			try {
				if (!txtItem.getText().isEmpty()) {
					value = Integer.parseInt(txtItem.getText());
					if (value < min || value > max) throw new Exception();
				}
			}catch (Exception ex) {
				txtItem.setText(nf.format(value));
			}
			
			if (value != lastValue.value) fireListeners();
			lastValue.value = value;
		});
		return txtItem;
	}


	private Composite createPartComposite() {
		Composite cMin = new Composite(this, SWT.NONE);
		cMin.setLayout(new GridLayout(1, false));
		((GridLayout)cMin.getLayout()).marginWidth = 0;
		((GridLayout)cMin.getLayout()).marginHeight = 0;
		((GridLayout)cMin.getLayout()).horizontalSpacing = 0;
		cMin.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cMin.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		return cMin;
	}
	
	public void setTimeInSeconds(int seconds) {
		int hour = seconds / 3600;
		int min = (seconds - hour * 3600) / 60;

		lasthour.value = hour;
		lastmin.value = min;
		
		txtHour.setText(nf.format(hour));
		txtMin.setText(nf.format(min));
	}
	
	/**
	 * The selected time in seconds - the last second (59) of the selected
	 * hour/minute is returned.
	 * 
	 * @return
	 */
	public int getTimeInSeconds() {
		Integer hour = 0;
		if (!txtHour.getText().isEmpty()) {
			hour = Integer.valueOf( txtHour.getText() );
			if (hour < 0 || hour > 23) hour = 0;
		}

		Integer min = 0;
		if (!txtMin.getText().isEmpty()) {
			min = Integer.valueOf( txtMin.getText() );
			if (min < 0 || min > 59) min = 0;
		}
		
		return hour * 3600 + min * 60 + 59;
	}
	
	private void hideHour() {
		if (hourShell == null) return;
		hourShell.setVisible(false);
	}
	
	private void showHour() {
		if (hourShell == null) {
			hourShell = new Shell(getShell(), SWT.MODELESS);
			
			hourShell.setLayout(new GridLayout(6, true));
			hourShell.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			((GridLayout)hourShell.getLayout()).marginWidth = 0;
			((GridLayout)hourShell.getLayout()).marginHeight = 0;
			
			Color sel = new Color(getShell().getDisplay(), SystemColor.textHighlight.getRed(), SystemColor.textHighlight.getGreen(), SystemColor.textHighlight.getBlue());
			hourShell.addDisposeListener(e->sel.dispose());
			for (int i = 0; i < 24; i ++) {
			
				String v = nf.format(i);
				
				CLabel l = new CLabel(hourShell, SWT.NONE);
				l.setText(v);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setMargins(4, 4, 4, 4);
				
				l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				l.addListener(SWT.MouseUp, e->{
					txtHour.setText(l.getText());
					hideHour();
				});
				l.addListener(SWT.MouseEnter, e->l.setBackground(sel));
				l.addListener(SWT.MouseExit, e->l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT)));
			}
			hourShell.pack();
		}
		Point p = cHour.getParent().toDisplay(cHour.getLocation());
		p.y = p.y + cHour.getBounds().height + 2;
		p.x = p.x;
		
		hourShell.setLocation(p);
		hourShell.setVisible(true);
	}
	
	private void hideMinute() {
		if (minShell == null) return;
		minShell.setVisible(false);
	}
	
	private void showMinute() {
		if (minShell == null) {
			minShell = new Shell(getShell(), SWT.MODELESS);
			
			minShell.setLayout(new GridLayout(4, true));
			minShell.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			((GridLayout)minShell.getLayout()).marginWidth = 0;
			((GridLayout)minShell.getLayout()).marginHeight = 0;
			
			Color sel = new Color(getShell().getDisplay(), SystemColor.textHighlight.getRed(), SystemColor.textHighlight.getGreen(), SystemColor.textHighlight.getBlue());
			minShell.addDisposeListener(e->sel.dispose());
			for (int i = 0; i < 60; i+=5) {
			
				String v = nf.format(i);
				
				CLabel l = new CLabel(minShell, SWT.NONE);
				l.setText(v);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setMargins(4, 4, 4, 4);
				
				l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				l.addListener(SWT.MouseUp, e->{
					txtMin.setText(l.getText());
					hideMinute();
				});
				l.addListener(SWT.MouseEnter, e->l.setBackground(sel));
				l.addListener(SWT.MouseExit, e->l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT)));
			}
			minShell.pack();
		}
		Point p = cMin.getParent().toDisplay(cMin.getLocation());
		p.y = p.y + cMin.getBounds().height + 2;
		p.x = p.x;
		
		minShell.setLocation(p);
		minShell.setVisible(true);
	}

	private class LastValue{
		int value = 0;
	}
}
