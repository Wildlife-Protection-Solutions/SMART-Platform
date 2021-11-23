/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.patrol.internal.ui.editor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.wcs.smart.SmartPlugIn;

/**
 * Header for the presentation page that
 * moves between summaries and data.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class PresentationHeader extends Composite{

	public static final String TAB_BAR_CLASS = "SMARTTabBar"; //$NON-NLS-1$

	private static final String SUMMARY_PAGE = "Summary";
	
	private List<Object> dates;
	private Object currentDate;
	private Label dateLabel;
	
	private Label back, next;
	private ToolItem tiPick, tiZoomToggle;
	private Listener dateModified;
	
	/**
	 * Creates a new section header
	 * @param parent parent widget
	 * @param style style
	 * @param dateModified - event to fire when new date is selected
	 */
	public PresentationHeader(Composite parent, int style, Listener dateModified) {
		super(parent, style);
		
		this.dateModified = dateModified;
		
		WidgetElement.setCSSClass(this, TAB_BAR_CLASS);
		WidgetElement.applyStyles(this, true);
		
		createComponent();
	}

	public boolean getAutoZoomOption() {
		return this.tiZoomToggle.getSelection();
	}
	private void fireModified() {
		dateModified.handleEvent(new Event());
	}
	
	private void createComponent() {
		
		setLayout(new GridLayout(2, false));
		((GridLayout)getLayout()).marginWidth = 5;
		((GridLayout)getLayout()).marginHeight = 3;
		
		Composite main = new Composite(this, SWT.NONE);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		main.setLayout(new GridLayout(3, false));
		
		back = new Label(main, SWT.NONE);
		back.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
		back.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_BACKWARD));
		back.addListener(SWT.MouseUp, e->{
			int index = dates.indexOf(currentDate) - 1;
			if (index < 0) index = 0;
			setCurrentDate(dates.get(index));
			fireModified();
			
		});
		back.addListener(SWT.MouseEnter, e->getShell().setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND)));
		back.addListener(SWT.MouseExit, e->getShell().setCursor(null));
		
		dateLabel = new Label(main, SWT.NONE);
		dateLabel.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(LocalDate.now()));
		dateLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));

		next = new Label(main, SWT.NONE);
		next.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.BROWSER_FORWARD));
		next.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
		next.addListener(SWT.MouseUp, e->{
			int index = dates.indexOf(currentDate) + 1;
			if (index >= dates.size() ) index = dates.size() - 1;
			setCurrentDate(dates.get(index));
			fireModified();
		});
		next.addListener(SWT.MouseEnter, e->getShell().setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND)));
		next.addListener(SWT.MouseExit, e->getShell().setCursor(null));
				
		ToolBar tb = new ToolBar(this, SWT.FLAT);
		
		tiZoomToggle = new ToolItem(tb, SWT.CHECK);
		tiZoomToggle.setToolTipText("automatically zoom to waypoints");
		tiZoomToggle.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ZOOM_IMAGE));
		tiZoomToggle.setSelection(true);
		
		tiPick = new ToolItem(tb, SWT.PUSH);
		tiPick.setToolTipText("pick specific page");
		tiPick.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_LIST_ICON));
		tiPick.addListener(SWT.Selection, e->{
			showDateSelection(tb);
		});
		
	}
	
		
	public void setDateRange(List<LocalDate> dates) {
		this.dates = new ArrayList<>(dates);
		this.dates.add(0, SUMMARY_PAGE);
	}
	
	public void setCurrentDate(Object date) {
		if (date != null && date.equals(this.currentDate)) return;
		
		if (date == null) {
			date = SUMMARY_PAGE;
		}
		this.currentDate = date;
		if (date == SUMMARY_PAGE) {
			dateLabel.setText(SUMMARY_PAGE);
		} else {
			dateLabel.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format((LocalDate)date));
		}
		back.setEnabled(!currentDate.equals(dates.get(0)));
		next.setEnabled(!currentDate.equals(dates.get(dates.size() - 1)));
		
		tiPick.setEnabled(dates.size() > 1);
		layout(true);
		fireModified();
	}
	
	public LocalDate getCurrentDate() {
		if (this.currentDate == SUMMARY_PAGE) return null;
		return (LocalDate)this.currentDate;
	}
	
	
	private void showDateSelection(Control root) {
		
		Shell shell = new Shell(Display.getCurrent(), SWT.NO_TRIM | SWT.ON_TOP );
		shell.setLayout(new GridLayout());
		((GridLayout)shell.getLayout()).marginWidth = 0;
		((GridLayout)shell.getLayout()).marginHeight = 0;
		
		Composite c = new Composite(shell, SWT.BORDER);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		((GridLayout)c.getLayout()).marginHeight = 1;
		((GridLayout)c.getLayout()).marginWidth = 1;
		((GridLayout)c.getLayout()).verticalSpacing = 1;
		
		ListViewer lstDates = new ListViewer(c, SWT.V_SCROLL);
		lstDates.setContentProvider(ArrayContentProvider.getInstance());
		lstDates.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstDates.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element == SUMMARY_PAGE) return SUMMARY_PAGE;
				return DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format((LocalDate)element);
			}
		});
		lstDates.setInput(dates);
		lstDates.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object ld =lstDates.getStructuredSelection().getFirstElement();
				if (ld == null) return;
				setCurrentDate(ld);
				shell.dispose();
				fireModified();
			}
		});
		((GridData)lstDates.getControl().getLayoutData()).heightHint = 150;
		shell.pack();
		shell.layout(true);
		
		Point p2 = root.getParent().toDisplay(root.getLocation());
		shell.setLocation(p2.x-shell.getSize().x + root.getSize().x, p2.y + root.getSize().y);
		shell.open();
		shell.addListener(SWT.Deactivate, evt->{shell.dispose();});
	}
}
