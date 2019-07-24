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
import org.eclipse.swt.widgets.Button;
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
 *
 */
public class TimePicker extends Composite {

	private static NumberFormat nf = new DecimalFormat("00");  //$NON-NLS-1$
	
	private static final String AM = "am"; //$NON-NLS-1$
	private static final String PM = "pm"; //$NON-NLS-1$
	
	private Composite cHour, cMin, cAmPm;
	private Text txtHour, txtMin, txtAmPm;
	private Button bHour, bMin, bAmPm;
	private Shell hourShell, minShell, apShell;
	
	private List<Listener> listeners;
	
	public TimePicker(Composite parent, int style) {
		super(parent, SWT.BORDER);
		listeners = new ArrayList<>();
		
		Listener out = e->{
			if (e.widget != hourShell) hideHour();
			if (e.widget != minShell) hideMinute();
			if (e.widget != apShell) hideAmpm();
		};
		parent.getDisplay().addFilter(SWT.FocusIn, out);
		
		addListener(SWT.Dispose, e->{
			if (hourShell != null) hourShell.dispose();
			if (minShell != null) minShell.dispose();
			if (apShell != null) apShell.dispose();
			parent.getDisplay().removeFilter(SWT.FocusIn, out);
		});
		
		setLayout(new GridLayout(4, false));
		((GridLayout)getLayout()).marginWidth = 2;
		((GridLayout)getLayout()).marginHeight = 2;
		((GridLayout)getLayout()).horizontalSpacing = 0;
		
		cHour = createPartComposite();
		
		txtHour = createText(cHour, "12", 1, 12); //$NON-NLS-1$
		txtHour.addListener(SWT.MouseDown, e->{showHour();});
		
		bHour = new Button(cHour, SWT.ARROW | SWT.DOWN);
		bHour.addListener(SWT.Selection, e->{showHour(); txtHour.setSelection(0, 2);});

		Label l = new Label(this, SWT.NONE);
		l.setText(":"); //$NON-NLS-1$
		l.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cMin = createPartComposite();
		
		txtMin = createText(cMin, "00", 0, 59); //$NON-NLS-1$
		txtMin.addListener(SWT.MouseDown, e->{showMinute();});
		bMin = new Button(cMin, SWT.ARROW | SWT.DOWN);
		bMin.addListener(SWT.Selection, e->{showMinute(); txtMin.setSelection(0, 2);});
		
		cAmPm = createPartComposite();

		txtAmPm = new Text(cAmPm, SWT.NONE);
		txtAmPm.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, false, false));
		txtAmPm.setText(PM);
		txtAmPm.addListener(SWT.MouseDown, e->{showAmPm();txtAmPm.setSelection(0, 2);});
		txtAmPm.addListener(SWT.KeyDown, e->{
			if (e.keyCode == SWT.ESC || e.keyCode == SWT.CR || e.keyCode == SWT.LF) hideAmpm();
			e.doit = false;
		});
		txtAmPm.addListener(SWT.Modify,e->fireListeners());
		bAmPm = new Button(cAmPm, SWT.ARROW | SWT.DOWN);
		bAmPm.addListener(SWT.Selection, e->{showAmPm(); txtAmPm.setSelection(0, 2);});
		
		setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		txtHour.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		bHour.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		txtMin.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		bMin.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		txtAmPm.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		bAmPm.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

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
	
	private Text createText(Composite parent, String initvalue, int min, int max ) {
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
			try {
				if (!txtItem.getText().isEmpty()) {
					int i = Integer.parseInt(txtItem.getText());
					if (i < min || i > max) throw new Exception();
				}
				
			}catch (Exception ex) {
				txtItem.setText((String)txtItem.getData());
				txtItem.setSelection(2, 2);
				e.doit = false;
				return;
			}
			fireListeners();
		});
		return txtItem;
	}


	private Composite createPartComposite() {
		Composite cMin = new Composite(this, SWT.NONE);
		cMin.setLayout(new GridLayout(2, false));
		((GridLayout)cMin.getLayout()).marginWidth = 0;
		((GridLayout)cMin.getLayout()).marginHeight = 0;
		((GridLayout)cMin.getLayout()).horizontalSpacing = 0;
		cMin.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		return cMin;
	}
	
	public void setTimeInSeconds(int seconds) {
		int hour = seconds / 3600;
		int min = (seconds - hour * 3600) / 60;
		//int sec = seconds - (hour * 3600) - (min * 60);
		
		if (hour > 12) {
			txtHour.setText(nf.format(hour - 12));
			txtAmPm.setText(PM);
		}else {
			txtHour.setText(nf.format(hour));
			txtAmPm.setText(AM);
		}
		txtMin.setText(nf.format(min));
	}
	
	/**
	 * The selected time in seconds - the last second (59) of the selected
	 * hour/minute is returned.
	 * 
	 * @return
	 */
	public int getTimeInSeconds() {
		
		String s = txtAmPm.getText();
		
		Integer hour = 1;
		if (!txtHour.getText().isEmpty()) {
			hour = Integer.valueOf( txtHour.getText() );
			if (hour < 1 || hour > 12) hour = 1;
		}
		if (s.equals(PM)) {
			hour += 12;
		}
			
		Integer min = 0;
		if (!txtMin.getText().isEmpty()) Integer.valueOf( txtMin.getText() );
		if (min < 0 || min > 59) min = 0;
	
		return hour * 3600 + min * 60 + 59;
	}
	
	private void hideHour() {
		if (hourShell == null) return;
		hourShell.setVisible(false);
	}
	
	private void showHour() {
		if (hourShell == null) {
			hourShell = new Shell(getShell(), SWT.MODELESS);
			
			hourShell.setLayout(new GridLayout(4, true));
			hourShell.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			((GridLayout)hourShell.getLayout()).marginWidth = 0;
			((GridLayout)hourShell.getLayout()).marginHeight = 0;
			
			Color sel = new Color(getShell().getDisplay(), SystemColor.textHighlight.getRed(), SystemColor.textHighlight.getGreen(), SystemColor.textHighlight.getBlue());
			hourShell.addDisposeListener(e->sel.dispose());
			for (int i = 1; i < 13; i ++) {
			
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

	private void hideAmpm() {
		if (apShell == null) return;
		apShell.setVisible(false);
	}
	
	private void showAmPm() {
		if (apShell == null) {
			apShell = new Shell(getShell(), SWT.MODELESS);
			
			apShell.setLayout(new GridLayout(2, true));
			apShell.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			((GridLayout)apShell.getLayout()).marginWidth = 0;
			((GridLayout)apShell.getLayout()).marginHeight = 0;
			
			Color sel = new Color(getShell().getDisplay(), SystemColor.textHighlight.getRed(), SystemColor.textHighlight.getGreen(), SystemColor.textHighlight.getBlue());
			apShell.addDisposeListener(e->sel.dispose());
			
			for (String s : new String[] {AM, PM}) {
				CLabel l = new CLabel(apShell, SWT.NONE);
				l.setText(s);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setMargins(4, 4, 4, 4);
					
				l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				l.addListener(SWT.MouseUp, e->{
					txtAmPm.setText(l.getText());
					hideAmpm();
				});
				l.addListener(SWT.MouseEnter, e->l.setBackground(sel));
				l.addListener(SWT.MouseExit, e->l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT)));
			}
			
			apShell.pack();
		}
		Point p = cAmPm.getParent().toDisplay(cAmPm.getLocation());
		p.y = p.y + cAmPm.getBounds().height + 2;
		p.x = p.x;
		
		apShell.setLocation(p);
		apShell.setVisible(true);
	}
}
