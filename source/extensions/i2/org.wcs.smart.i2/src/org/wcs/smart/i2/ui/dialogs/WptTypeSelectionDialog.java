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
package org.wcs.smart.i2.ui.dialogs;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.gpx.xml.WptType;

/**
 * Dialog for selecting waypoints from a gpx file.
 * 
 * @author Emily
 *
 */
public class WptTypeSelectionDialog extends TitleAreaDialog{

	private CheckboxTableViewer lstViewer;
	
	private List<WptType> waypoints;
	private String message;
	
	
	public WptTypeSelectionDialog(Shell parentShell, List<WptType> waypoints, String message) {
		super(parentShell);
		
		this.waypoints = waypoints;
		this.message = message;	
	}
	
	public List<WptType> getWaypoints(){
		return this.waypoints;
	}
	
	@Override
	public void cancelPressed(){
		waypoints = null;
		super.cancelPressed();
	}
	
	@Override
	public void okPressed(){
		waypoints = new ArrayList<WptType>();
		for (Object wptType : lstViewer.getCheckedElements()){
			if (wptType instanceof WptType){
				waypoints.add((WptType) wptType);
			}
		}
		super.okPressed();
	}
	
	@Override
	public Point getInitialSize(){
		Point pnt = super.getInitialSize();
		pnt.y = 500;
		return pnt;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstViewer = CheckboxTableViewer.newCheckList(main, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.setInput(waypoints);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof WptType) {
					WptType wp = (WptType) element;
					StringBuilder value = new StringBuilder( );
					value.append(wp.getName());
					if (wp.getCmt() != null && !wp.getCmt().toLowerCase().equals("null")){ //$NON-NLS-1$
						value.append (" (" + wp.getCmt() + ") "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					value.append( " [");
					value.append(DateFormat.getInstance().format(GPSDataImport.findWaypointDate(wp)));
					value.append( " ]");
					return value.toString();
				}
				return super.getText(element);
			}
		});
		lstViewer.getTable().addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.SPACE){
					boolean value = lstViewer.getChecked(   ((IStructuredSelection)lstViewer.getSelection()).getFirstElement() );
					for (Iterator<?> iterator = ((IStructuredSelection)lstViewer.getSelection()).iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						lstViewer.setChecked(tp, !value);
					}
					e.doit = false;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		
		Composite bottom = new Composite(main, SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		
		Link lnkSelectAll = new Link(bottom, SWT.NONE);
		lnkSelectAll.setText("<a>" + "Select All" + "</a>"); 
		lnkSelectAll.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				lstViewer.setAllChecked(true);
			}
		});
		
		lnkSelectAll = new Link(bottom, SWT.NONE);
		lnkSelectAll.setText("<a>" + "Select None" + "</a>"); 
		lnkSelectAll.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				lstViewer.setAllChecked(false);
			}
		});
		
		super.setMessage(message);
		super.setTitle("Import Waypoints");
		getShell().setText("Import Waypoints");
		
		return parent;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}
