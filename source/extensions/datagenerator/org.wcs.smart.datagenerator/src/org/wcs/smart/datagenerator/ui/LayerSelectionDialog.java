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
package org.wcs.smart.datagenerator.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.datagenerator.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Dialog for selecting a layer for computing the new spatial location
 * 
 * @author Emily
 *
 */
public class LayerSelectionDialog extends TitleAreaDialog {

	
	private Button opSmart;
	private Button opShapefile;
	private Button btnSelect;
	
	private Text txtShape;
	private ComboViewer cmbLayers;
	
	private DataGeneratorView view;
	
	private Envelope envelope;
	private Path shapefile;
	
	private HashMap<Area.AreaType, Envelope> areaToEnvelope = new HashMap<>();

	
	public LayerSelectionDialog(Shell parentShell, DataGeneratorView view) {
		super(parentShell);
		this.view = view;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
	}
	
	public Envelope getBounds() {
		return this.envelope;
	}
	
	public Path getShapefile() {
		return this.shapefile;
	}
	public void okPressed() {
		this.shapefile = null;
		this.envelope = null;
		
		if (opSmart.getSelection()) {
			Object item = cmbLayers.getStructuredSelection().getFirstElement();
			if (item instanceof Area.AreaType) {
				envelope = areaToEnvelope.get(item);
			}else {
				MessageDialog.openError(getShell(), Messages.LayerSelectionDialog_NotSelected, Messages.LayerSelectionDialog_SelectionRequire);
				return;
			}
		}else if (opShapefile.getSelection()) {
			this.shapefile = Paths.get(txtShape.getText());
		}
		super.okPressed();
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
	
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		c.setLayout(new GridLayout(3, false));
		
		opSmart = new Button(c, SWT.RADIO);
		opSmart.setText(Messages.LayerSelectionDialog_SmartLayer);
		
		cmbLayers = new ComboViewer(c, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbLayers.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbLayers.setContentProvider(ArrayContentProvider.getInstance());
		cmbLayers.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof Area.AreaType) {
					return ((Area.AreaType) element).getGuiName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		cmbLayers.setInput(DialogConstants.LOADING_TEXT);
		
		opShapefile = new Button(c, SWT.RADIO);
		opShapefile.setText(Messages.LayerSelectionDialog_ShpFile);
		
		txtShape = new Text(c, SWT.BORDER);
		txtShape.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnSelect = new Button(c, SWT.NONE);
		btnSelect.setText("..."); //$NON-NLS-1$
		btnSelect.addListener(SWT.Selection, e->{
			Path p = view.loadShapefile();
			if (p == null) return;
			txtShape.setText(p.toString());
		});
		
		opSmart.addListener(SWT.Selection, e->updateEnabled());
		opShapefile.addListener(SWT.Selection, e->updateEnabled());
			
		opSmart.setSelection(true);
		updateEnabled();
		
		setTitle(Messages.LayerSelectionDialog_Title);
		getShell().setText(Messages.LayerSelectionDialog_Title2);
		setMessage(Messages.LayerSelectionDialog_Message);
		
		loadAreas.schedule();
		return parent;
	}
	
	private void updateEnabled() {
		btnSelect.setEnabled(opShapefile.getSelection());
		txtShape.setEnabled(opShapefile.getSelection());
		cmbLayers.getControl().setEnabled(opSmart.getSelection());
	}
	
	private Job loadAreas = new Job("load areas") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			HashMap<Area.AreaType, Envelope> area = new HashMap<>();
			
			try(Session session = HibernateManager.openSession()){
				@SuppressWarnings("unchecked")
				List<Area> types = session.createQuery("FROM Area WHERE conservationArea = :ca") //$NON-NLS-1$
					.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.list();
				for (Area a : types) {
					Envelope e = area.get(a.getType());
					if (e == null) {
						e = new Envelope(a.getGeometry().getEnvelopeInternal());
						area.put(a.getType(), e);
					}else {
						e.expandToInclude(a.getGeometry().getEnvelopeInternal());
					}
				}
			}
			
			Display.getDefault().syncExec(()->{
				areaToEnvelope = area;
				List<Area.AreaType> items = new ArrayList<>(area.keySet());
				cmbLayers.setInput(items);
			});
			return Status.OK_STATUS;
		}
		
	};
}
