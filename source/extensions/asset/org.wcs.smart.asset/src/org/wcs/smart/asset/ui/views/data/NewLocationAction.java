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
package org.wcs.smart.asset.ui.views.data;

import java.text.MessageFormat;
import java.util.Iterator;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.data.importer.ActionableWarning;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.data.importer.NewLocationWarning;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.StationLocationDialog;

/**
 * Action for creating a new station location from a
 * metadata mapping value.
 * 
 * @author Emily
 *
 */
public class NewLocationAction implements ImportAction {

	private NewLocationWarning warning;
	
	@Inject
	private IEclipseContext context;
	
	public NewLocationAction(NewLocationWarning warning) {
		this.warning = warning;
	}

	@Override
	public boolean preformAction(FileProcessor processor, FileProxy selectedItem) {
		
		AssetStation station = selectedItem.getStation();
		if (station == null || station.getUuid() == null) {
			//first we create a station
			StationAssetSelectionDialog stn = new StationAssetSelectionDialog(Display.getDefault().getActiveShell(), StationAssetSelectionDialog.Type.STATION);
			ContextInjectionFactory.inject(stn, context);
			if (stn.open() != Window.OK) return false;
			station = stn.getSelectedStation();
		}
		
		AssetStationLocation newLocation = new AssetStationLocation();
		newLocation.setStation(station);
		newLocation.setId(warning.getLocationId());
		
		StationLocationDialog dialog = new StationLocationDialog(Display.getDefault().getActiveShell(), newLocation);
		ContextInjectionFactory.inject(dialog, context);
		if (dialog.open() != Window.OK) return false;
		
		
		selectedItem.setStationLocation(newLocation);
		selectedItem.getWarnings().remove(warning);
		
		//lets see if we can find other items that also have this same warning
		for (FileProxy other : processor.getFiles()) {
			if (other.getStationLocation() != null && other.getStationLocation().getUuid() != null) continue;
			for (Iterator<ActionableWarning> iterator = other.getWarnings().iterator(); iterator.hasNext();) {
				ActionableWarning aw = iterator.next();
				if (aw instanceof NewLocationWarning && ((NewLocationWarning) aw).getLocationId().equals(warning.getLocationId())) {
					other.setStationLocation(newLocation);
					iterator.remove();
				}
			}
		}
		return true;
	}
	
	public String getMenuLabel() {
		return MessageFormat.format("Create Location {0}", warning.getLocationId());
	}
	
	@Override
	public Image getMenuImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON);
	}
}
