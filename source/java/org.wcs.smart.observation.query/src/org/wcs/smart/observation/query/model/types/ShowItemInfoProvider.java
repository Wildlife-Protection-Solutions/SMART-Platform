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
package org.wcs.smart.observation.query.model.types;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSourceUiProvider;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;
import org.wcs.smart.query.model.IQueryResultInfoProvider;

/**
 * Abstract observation provider that requires a waypoint source
 * and waypoint uuid to determine what item to show.
 * 
 * @author Emily
 *
 */
public class ShowItemInfoProvider implements IQueryResultInfoProvider {

	@Override
	public String getName() {
		return GOTO_SOURCE_STR;
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON);
	}
	
	/**
	 * Users must override.  Call showItem to display
	 * a given waypoint.
	 */
	@Override
	public void doWork(IResultItem resultItem) {
		if (resultItem instanceof WaypointQueryResultItem) {
			UUID waypointUuid = ((WaypointQueryResultItem) resultItem).getWaypointUuid();
			String waypointSourceKey = ((WaypointQueryResultItem) resultItem).getSourceId();
			showItem(waypointUuid, waypointSourceKey);
			return;
		}
		
		if (resultItem instanceof IAttachmentResultItem) {
			try(Session s = HibernateManager.openSession()){
				ObservationAttachment a = s.get(ObservationAttachment.class, ((IAttachmentResultItem) resultItem).getAttachment().getUuid());
				Waypoint wp = null;
				if (a != null) {
					wp = a.getObservation().getWaypoint();
				}else {
					WaypointAttachment w = s.get(WaypointAttachment.class, ((IAttachmentResultItem) resultItem).getAttachment().getUuid());
					wp = w.getWaypoint();
				}
				if (wp != null) {
					showItem(wp.getUuid(), wp.getSourceId());
					return;
				}
			}
		}
		
		MessageDialog
				.openError(
						Display.getDefault().getActiveShell(),
						ERROR_STR,
						MessageFormat.format(OP_NOT_SUPPORTED_STR,resultItem.getClass().getName()));
		

	}
	
	protected void showItem(UUID waypointUuid, String waypointSourceKey){
		if (waypointUuid == null){
			displayError(Messages.AbstractObservationInfoProvider_WaypointNull);
			return ;
		}
		if (waypointSourceKey == null){
			displayError(Messages.AbstractObservationInfoProvider_SourceNull);
			return ;
		}
		
		IWaypointSourceUiProvider provider = WaypointSourceEngine.INSTANCE.findUiProvider(waypointSourceKey);
		if (provider == null){
			displayError(
					MessageFormat.format(Messages.AbstractObservationInfoProvider_ProviderNotFound,
									waypointSourceKey));
		}else{
			provider.findAndShow(waypointUuid);
		}

	}

	private void displayError(String message){
		MessageDialog
		.openError(
				Display.getDefault().getActiveShell(),
				ERROR_STR,message);
	}

	@Override
	public boolean supportsCcaa() {
		return false;
	}
	
}
