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
package org.wcs.smart.incident.ui.newwizard;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.incident.IIncidentProvider;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Incident ID composite
 * 
 * @author Emily
 *
 */
public class IdComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.id"; //$NON-NLS-1$
	
	private Text txtId;
	
	private ComboViewer cmbType;
	
	@Override
	public String validate() {
		if (txtId.getText().strip().isEmpty()){
			return Messages.IdComposite_IdRequired;
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(2, false));
		
		if (IncidentManager.getInstance().getIncidentProviders().size() > 1) {
			Label l = new Label(item, SWT.NONE);
			l.setText(Messages.IdComposite_WaypointSourceField);
			
			cmbType = new ComboViewer(item, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbType.setContentProvider(ArrayContentProvider.getInstance());
			cmbType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbType.setInput(IncidentManager.getInstance().getIncidentProviders());
			cmbType.setSelection(new StructuredSelection(IncidentManager.getInstance().getIncidentProviders().iterator().next()));
			cmbType.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((IIncidentProvider)element).getName();
				}
			});
		}
		
		Label l = new Label(item, SWT.NONE);
		l.setText(Messages.IdComposite_Label);
		
		txtId = new Text(item, SWT.BORDER);
		txtId.setTextLimit(Waypoint.ID_MAX_LENGTH);
		txtId.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		

		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		incident.setId(txtId.getText().strip());
		if (cmbType == null) {
			incident.setSourceId(IndepedentIncidentSource.KEY);
		}else {
			IIncidentProvider p = (IIncidentProvider) cmbType.getStructuredSelection().getFirstElement();
			incident.setSourceId(p.getWaypointSourceKey());
		}
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		txtId.setText(incident.getId());
		
		
		cmbType.setSelection(new StructuredSelection(IncidentManager.getInstance().getIncidentProvider(incident.getSourceId())));
		cmbType.getControl().setEnabled(incident.getUuid() == null);
	}
	
	@Override
	public String getName() {
		return Messages.IdComposite_Name;
	}

	@Override
	public String getDescription() {
		return Messages.IdComposite_Description1;
	}

}
