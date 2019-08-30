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
package org.wcs.smart.cybertracker.navigation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.NavigationLayer;
import org.wcs.smart.cybertracker.navigation.ui.CtNavigationExportDialog;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Manager for exporting navigation layers to files.
 * 
 * @author Emily
 *
 */
public enum ExportNavigationManager {
	
	INSTANCE;
	
	private static final String FILENAME_DATE_FORMAT = "yyyyMMddHHmmss"; //$NON-NLS-1$
	public static final String EXT_ID = "org.wcs.smart.cybertracker.navigation"; //$NON-NLS-1$
	
	public String getExportFileName(NavigationLayer layer) {
		String datetime = DateTimeFormatter.ofPattern(FILENAME_DATE_FORMAT).format(LocalDateTime.now());
		String fname = URLUtils.cleanFilename(layer.getName()) + "." + datetime + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
		return fname;
	}
	
	public void exportNavigationLayer(NavigationLayer layer, Path exportFile) throws IOException{
		String fname = SharedUtils.getFilenameWithoutExtension(exportFile.getFileName().toString());
		String json = new String(layer.getTargets(), StandardCharsets.UTF_8);
		ZipUtil.writeToZip(exportFile, fname + ".json", json); //$NON-NLS-1$
	}

	public List<Path> exportNavigationLayers(List<NavigationLayer> items, Path exportDir) {
		String datetime = DateTimeFormatter.ofPattern(FILENAME_DATE_FORMAT).format(LocalDateTime.now());
		List<Path> paths = new ArrayList<>();
		for (NavigationLayer l : items) {
			try {
				String fname = URLUtils.cleanFilename(l.getName()) + "." + datetime + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
				Path filename = exportDir.resolve(fname);
				exportNavigationLayer(l, filename);
				paths.add(filename);
			}catch(IOException ex) {
				CyberTrackerPlugIn.displayError(Messages.ExportNavigationManager_ErrorTitle, Messages.ExportNavigationManager_ExportErrorMsg + ex.getMessage(), ex);
			}
		}
		return paths;
	}
	
	public boolean doExport(List<NavigationLayer> toExport, IEclipseContext context) throws IOException {
		List<INavigationExportAction> actions = getNavigationActions();
		List<INavigationExportAction> doActions = new ArrayList<>(actions);
		
		if (actions.size() > 1) {
			CtNavigationExportDialog dialog = new CtNavigationExportDialog(context.get(Shell.class), actions);
			if (dialog.open() != Window.OK) return false;
			doActions = dialog.getSelectedActions();
		}
		for (INavigationExportAction a : doActions) {
			a.doAction(toExport, context);
		}
		return true;
	}

	public List<INavigationExportAction> getNavigationActions() {
		List<INavigationExportAction> actions = new ArrayList<>();
		actions.add(new ExportNavigationToDeviceAction());
		actions.add(new ExportNavigationToFileAction());
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_ID);
			try {
				for (IConfigurationElement e : config) {
					if (e.getName().equalsIgnoreCase(INavigationExportAction.EXT_NAME)) {
						INavigationExportAction ext = (INavigationExportAction) e.createExecutableExtension("class"); //$NON-NLS-1$
						actions.add(ext);
					}
				}
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.ExportNavigationManager_ErrorTitle, Messages.ExportNavigationManager_LoadActionsError + ex.getMessage(), ex);
			}
		}
		return actions;

	}
	
	public List<INavigationLayerProperty> getPackageProperties() {
		List<INavigationLayerProperty> props = new ArrayList<>();
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_ID);
			try {
				for (IConfigurationElement e : config) {
					if (e.getName().equalsIgnoreCase(INavigationLayerProperty.EXT_NAME)) {
						INavigationLayerProperty ext = (INavigationLayerProperty) e.createExecutableExtension("class"); //$NON-NLS-1$
						props.add(ext);
					}
				}
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.ExportNavigationManager_ErrorTitle, Messages.ExportNavigationManager_LoadPropertiesError + ex.getMessage(), ex);
			}
		}
		return props;

	}
}

