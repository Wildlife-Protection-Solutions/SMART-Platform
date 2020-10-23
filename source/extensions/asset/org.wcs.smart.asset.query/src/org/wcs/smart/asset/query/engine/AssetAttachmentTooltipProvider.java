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
package org.wcs.smart.asset.query.engine;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
/**
 * Tooltip provider for asset attachments.
 * 
 * @author Emily
 *
 */
public class AssetAttachmentTooltipProvider extends Job {

	private IAttachmentResultItem data;
	private Composite details;
	
	public AssetAttachmentTooltipProvider(IAttachmentResultItem data, Composite details) {
		super("loading waypoint details"); //$NON-NLS-1$
		setSystem(true);
		this.data = data;
		this.details = details;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Set<String> assets = new HashSet<>();
		Set<String> stations = new HashSet<>();
		Set<String> locations = new HashSet<>();
		
		WaypointObservation o = null;
		Waypoint wp = null;
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			
			ObservationAttachment oba = s.get(ObservationAttachment.class, data.getAttachment().getUuid());
			if (oba != null) {
				o = oba.getObservation();
				wp = o.getWaypoint();
				if (o.getAttributes() != null) {
					for (WaypointObservationAttribute a : o.getAttributes()) {
						a.getAttribute().getName();
						a.getAttributeValueAsString(Locale.getDefault());
					}
				}
				o.getCategory().getFullCategoryName();
			}else {
				WaypointAttachment obw = s.get(WaypointAttachment.class,  data.getAttachment().getUuid());
				wp = obw.getWaypoint();
				for (WaypointObservation wo : wp.getAllObservations()) {
					if (wo.getAttributes() != null) {
						for (WaypointObservationAttribute a : wo.getAttributes()) {
							a.getAttribute().getName();
							a.getAttributeValueAsString(Locale.getDefault());
						}
					}
					wo.getCategory().getFullCategoryName();
				}
			}
			
			List<AssetWaypoint> pw = QueryFactory.buildQuery(s, AssetWaypoint.class, new Object[] {"waypoint", wp}).list(); //$NON-NLS-1$
			for (AssetWaypoint as : pw) {
				assets.add(as.getAssetDeployment().getAsset().getId());
				stations.add(as.getAssetDeployment().getStationLocation().getStation().getId());
				locations.add(as.getAssetDeployment().getStationLocation().getId());
			}
			
			s.getTransaction().rollback();
		}
		
		WaypointObservation fo = o;
		Waypoint fwp = wp;
		
		Display.getDefault().syncExec(()->{
			if (details == null || details.isDisposed()) return;
			ScrolledComposite scroll = new ScrolledComposite(details, SWT.V_SCROLL | SWT.H_SCROLL);
			scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			scroll.setBackground(details.getBackground());
			Composite main = new Composite(scroll, SWT.NONE);
			scroll.setContent(main);
			main.setLayout(new GridLayout(2, false));
			
			main.setBackground(details.getBackground());
			((GridLayout)main.getLayout()).marginWidth = 0;
			((GridLayout)main.getLayout()).marginHeight = 0;
			
			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			
			Label l = new Label(main, SWT.NONE);
			l.setText(Messages.AssetAttachmentTooltipProvider_StationLabel);
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(AssetQueryPlugIn.asString(stations));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(Messages.AssetAttachmentTooltipProvider_LocationsLabel);
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(AssetQueryPlugIn.asString(locations));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(Messages.AssetAttachmentTooltipProvider_AssetsLabel);
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(AssetQueryPlugIn.asString(assets));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(Messages.AssetAttachmentTooltipProvider_WpIDLabel);
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(fwp.getId());
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(Messages.AssetAttachmentTooltipProvider_DateTimeLabel);
			l.setBackground(details.getBackground());
			
			l = new Label(main, SWT.NONE);
			l.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(fwp.getDateTime()));
			l.setBackground(details.getBackground());
			
			if (fo != null) {
				l = new Label(main, SWT.NONE);
				l.setText(Messages.AssetAttachmentTooltipProvider_ObservationLabel);
				l.setBackground(details.getBackground());
				
				l = new Label(main, SWT.NONE);
				l.setText(fo.getCategory().getFullCategoryName());
				l.setBackground(details.getBackground());
				
				if (fo.getAttributes() != null) {
					new Label(main, SWT.NONE);
					Composite other = new Composite(main, SWT.NONE);
					other.setLayout(new GridLayout(2, false));
					other.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					other.setBackground(details.getBackground());
					((GridLayout)other.getLayout()).marginWidth = 0;
					((GridLayout)other.getLayout()).marginHeight = 0;
					
					l = new Label(other, SWT.SEPARATOR | SWT.HORIZONTAL);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
					
					for (WaypointObservationAttribute a : fo.getAttributes()) {
						l = new Label(other, SWT.NONE);
						l.setText(a.getAttribute().getName()+":"); //$NON-NLS-1$
						l.setBackground(details.getBackground());
						
						l = new Label(other, SWT.NONE);
						l.setText(a.getAttributeValueAsString(Locale.getDefault()));
						l.setBackground(details.getBackground());
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}
				}
			}else if (!fwp.getAllObservations().isEmpty()) {
				
				l = new Label(main, SWT.NONE);
				l.setText(Messages.AssetAttachmentTooltipProvider_ObservationsLabel);
				l.setBackground(details.getBackground());
				l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

				Composite other = new Composite(main, SWT.NONE);
				other.setLayout(new GridLayout());
				other.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				other.setBackground(details.getBackground());
				((GridLayout)other.getLayout()).marginWidth = 0;
				((GridLayout)other.getLayout()).marginHeight = 0;
				
				for (WaypointObservationGroup g : fwp.getObservationGroups()) {
					Composite c = new Composite(other, SWT.NONE);
					c.setLayout(new GridLayout(2, false));
					c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					c.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					((GridLayout)c.getLayout()).marginWidth = 0;
					((GridLayout)c.getLayout()).marginHeight = 0;
					
					
					if (fwp.getObservationGroups().size() > 1) {
						Composite header = SmartUiUtils.createHeaderLabel(c, Messages.AssetAttachmentTooltipProvider_ObsGroupLabel);
						header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
					}
					
					for (int i = 0; i < g.getObservations().size(); i ++) {
						WaypointObservation wo = g.getObservations().get(i);
						
						l = new Label(c, SWT.NONE);
						l.setText(wo.getCategory().getFullCategoryName());
						l.setBackground(details.getBackground());
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
						
						if (wo.getAttributes() != null) {
							for (WaypointObservationAttribute a : wo.getAttributes()) {
								l = new Label(c, SWT.NONE);
								l.setText(a.getAttribute().getName()+":"); //$NON-NLS-1$
								l.setBackground(details.getBackground());
								
								l = new Label(c, SWT.NONE);
								l.setText(a.getAttributeValueAsString(Locale.getDefault()));
								l.setBackground(details.getBackground());
								l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
							}
						}
						l = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
					}
				}
			}
			
			details.layout(true, true);
			scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			main.layout();					
		});

		return Status.OK_STATUS;
	}

}
