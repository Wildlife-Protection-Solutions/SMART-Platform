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
