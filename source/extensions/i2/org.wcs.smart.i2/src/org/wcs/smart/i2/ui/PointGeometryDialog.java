/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Dialog for inputting text and converting it
 * to a set of points
 * 
 * @author Emily
 *
 */
public class PointGeometryDialog extends SmartStyledTitleDialog {

	private Text txtGeometry;
	private ComboViewer cmbProjection;
	
	private List<Coordinate> points;
	private Projection prj;
	
	public PointGeometryDialog(Shell parentShell) {
		super(parentShell);
		points = new ArrayList<>();
	}

	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		Label l = new Label(parent, SWT.NONE);
		l.setText(Messages.PointGeometryDialog_ProjectionLabel);
		
		cmbProjection = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cmbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbProjection.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof Projection) return ((Projection)element).getName();
				return super.getText(element);
			}
		});
		cmbProjection.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		
		txtGeometry = new Text(parent, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		
		txtGeometry.addListener(SWT.Modify, e-> validate());
		txtGeometry.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		loadProjections();
		
		setTitle(Messages.PointGeometryDialog_Title);
		setMessage(Messages.PointGeometryDialog_Message);
		getShell().setText(Messages.PointGeometryDialog_Title);
		return parent;
	}
	
	private void loadProjections() {
		Job load = new Job("load projections") { //$NON-NLS-1$
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Projection> items = null;
				try(Session session = HibernateManager.openSession()){
					items = QueryFactory.buildQuery(session, Projection.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
					items.forEach(e->e.getName());
				}
				final List<Projection> fitems = items;
				Display.getDefault().syncExec(()->{
					if (cmbProjection.getControl().isDisposed()) return;
					cmbProjection.setInput(fitems);
					cmbProjection.setSelection(new StructuredSelection(fitems.get(0)));
				});
				return Status.OK_STATUS;
			}
		};
		load.setSystem(true);
		load.schedule();
	}
	public void okPressed() {
		if (!validate())  return;
		super.okPressed();
	}
	private boolean validate(){
		points.clear();
		
		String text = txtGeometry.getText();
		String message = null;
		String[] lines = text.split("\\n"); //$NON-NLS-1$
		int linecnt = 1;
		
		if (!(cmbProjection.getStructuredSelection().getFirstElement() instanceof Projection)) {
			message = Messages.PointGeometryDialog_ProjectionRequired;
		}else {
			prj = (Projection) cmbProjection.getStructuredSelection().getFirstElement();
			try {
				prj.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(prj.getDefinition()));
			}catch (Exception ex) {
				message = MessageFormat.format(Messages.PointGeometryDialog_ParseError, ex.getMessage());
			}
		}
		if (message == null) {
			for (String line : lines) {
				if (line.trim().isBlank()) continue;
				
				//try parsing into two coordinates on space and comman
				String[] parts = line.split(","); //$NON-NLS-1$
				if (parts.length != 2) {
					parts = line.split(" "); //$NON-NLS-1$
				}
				if (parts.length != 2) {
					message = MessageFormat.format(Messages.PointGeometryDialog_ParseError1, linecnt);
					break;
				}
				Double x = null;
				Double y = null;
				try {
					x = Double.parseDouble(parts[0]);
				}catch (Exception ex) {
					message = MessageFormat.format(Messages.PointGeometryDialog_ParseErrorX, linecnt);
					break;
				}
				
				try {
					y = Double.parseDouble(parts[1]);
				}catch (Exception ex) {
					message = MessageFormat.format(Messages.PointGeometryDialog_ParseErrorY, linecnt);
					break;
				}
				//reproject to smart db
				try {
					Coordinate c = ReprojectUtils.reproject(x, y, prj.getParsedCoordinateReferenceSystem(), SmartDB.DATABASE_CRS);
					if (c.x > 180 || c.x < -180 || c.y > 90 || c.y < -90) {
						message = MessageFormat.format(Messages.PointGeometryDialog_ParseErrorInvalidNumbers, linecnt);
					}else {
						points.add(c);
					}
				} catch (Exception e) {
					e.printStackTrace();
					message = MessageFormat.format(Messages.PointGeometryDialog_ParseErrorReprojectErr,  linecnt);
				}
				linecnt++;
			}
		}
		
		
		setErrorMessage(message);
		getButton(IDialogConstants.OK_ID).setEnabled(message == null);
		return message == null;
	}
	
	public List<Coordinate> getCoordinates(){
		return this.points;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}