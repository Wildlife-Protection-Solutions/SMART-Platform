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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Incident distance/direction composite.
 * @author Emily
 *
 */
public class DistanceDirectionComposite extends AbstractIncidentComposite {

	public static final String ID = "incident.distancedirection"; //$NON-NLS-1$
	
	private Text txtDirection;
	private Text txtDistance;
	
	@Override
	public String validate() {
		if (!txtDirection.getText().trim().isEmpty()){
			try{
				float dir = Float.parseFloat(txtDirection.getText());
				if (dir < 0 || dir >= 360) throw new Exception();
			}catch (Exception ex){
				return Messages.DistanceDirectionComposite_DirectionNumberRequired1;
			}
		}
		if (!txtDistance.getText().trim().isEmpty()){
			try{
				float dis = Float.parseFloat(txtDistance.getText());
				if (dis < 0) throw new Exception();
			}catch (Exception ex){
				return Messages.DistanceDirectionComposite_DistanceNumberRequired1;
			}
		}
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout(2, false));
		
		Label l = new Label(item, SWT.NONE);
		l.setText(Messages.DistanceDirectionComposite_DistanceLbl);
		l.setToolTipText(Messages.DistanceDirectionComposite_DistanceTooltip);
		
		txtDistance = new Text(item, SWT.BORDER);
		txtDistance.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtDistance.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(item, SWT.NONE);
		l.setText(Messages.DistanceDirectionComposite_BearingLbl);
		l.setToolTipText(Messages.DistanceDirectionComposite_BearingTooltip);
		
		txtDirection = new Text(item, SWT.BORDER);
		txtDirection.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireChange(event);	
			}
		});
		txtDirection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		return item;
	}

	@Override
	public void updateIncident(Waypoint incident) {
		if (!txtDirection.getText().trim().isEmpty()){
			incident.setDirection(Float.parseFloat(txtDirection.getText().trim()));
		}else{
			incident.setDirection(null);
		}
		if (!txtDistance.getText().trim().isEmpty()){
			incident.setDistance(Float.parseFloat(txtDistance.getText().trim()));
		}else{
			incident.setDistance(null);
		}
	}

	@Override
	public void initFields(Waypoint incident, Session session) {
		if (incident.getDirection() == null){
			txtDirection.setText(""); //$NON-NLS-1$
		}else{
			txtDirection.setText(String.valueOf(incident.getDirection()));
		}
		if (incident.getDistance() == null){
			txtDistance.setText(""); //$NON-NLS-1$
		}else{
			txtDistance.setText(String.valueOf(incident.getDistance()));
		}
	}
	
	@Override
	public String getName() {
		return Messages.DistanceDirectionComposite_Name;
	}

	@Override
	public String getDescription() {
		return Messages.DistanceDirectionComposite_Description;
	}

}
