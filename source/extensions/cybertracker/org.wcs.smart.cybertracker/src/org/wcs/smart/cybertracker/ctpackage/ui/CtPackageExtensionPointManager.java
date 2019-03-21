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
package org.wcs.smart.cybertracker.ctpackage.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Tools managing cybertracker packages
 * 
 * @author Emily
 *
 */
public enum CtPackageExtensionPointManager {
	
	INSTANCE;
	
	public static final String EXT_ID = "org.wcs.smart.cybertracker.ctpackage"; //$NON-NLS-1$
	
	private HashMap<String, ICtPackageManager> managers = null;
	
	public Path createPackage(ICtPackage ctpackage) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat(ICtPackage.PACKAGE_DATE_FORMAT);
		
		Path root = ICyberTrackerConstants.getCyberTrackerPackageFolder(ctpackage.getConservationArea());
		if (!Files.exists(root)) {
			Files.createDirectories(root);
		}
		
		String idpart = UuidUtils.uuidToString(ctpackage.getUuid());
		
		//delete existing files
		try(Stream<Path> files = Files.walk(root)){
			List<Path> toDelete = new ArrayList<>();
			files.forEach(file->{
				if (file.getFileName().toString().startsWith(idpart)) {
					toDelete.add(file);
				}
			});
			for (Path d : toDelete) Files.delete(d);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(idpart);
		sb.append("."); //$NON-NLS-1$
		sb.append(UuidUtils.uuidToString(UUID.randomUUID()));
		sb.append("."); //$NON-NLS-1$
		sb.append(sdf.format(new Date()));
		sb.append(".zip"); //$NON-NLS-1$
		String fname = sb.toString();
		
		Path output = ICyberTrackerConstants.getCyberTrackerPackageFolder(SmartDB.getCurrentConservationArea()).resolve(fname);
		
		findManager(ctpackage).buildPackage(ctpackage, output);
		
		return output;
	}
	
	public Collection<ICtPackageManager> getPackageManagers(){
		if (managers == null) {
			readPackageExtensions();
		}
		return managers.values();
	}
	
	public ICtPackageManager findManager(ICtPackage ctPackage) {
		if (managers == null) {
			readPackageExtensions();
		}
		return managers.get(ctPackage.getTypeIdentifier());
	}
	
	private void readPackageExtensions() {
		HashMap<String, ICtPackageManager> mgs = new HashMap<>();
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_ID);
			try {
				for (IConfigurationElement e : config) {
					if (e.getName().equalsIgnoreCase(ICtPackageManager.EXT_NAME_ID)) {
						ICtPackageManager ext = (ICtPackageManager) e.createExecutableExtension("class"); //$NON-NLS-1$
						mgs.put(ext.getTypeIdentifier(), ext);
					}
				}
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Error", "Unable to load package action extension points:" + ex.getMessage(), ex);
			}
		}
		this.managers = mgs;
	}
	public List<ICtExportAction> getPackageActions() {
		List<ICtExportAction> actions = new ArrayList<>();
		actions.add(new ExportPackageToFileAction());
		
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_ID);
			try {
				for (IConfigurationElement e : config) {
					if (e.getName().equalsIgnoreCase(ICtExportAction.EXT_NAME)) {
						ICtExportAction ext = (ICtExportAction) e.createExecutableExtension("class"); //$NON-NLS-1$
						actions.add(ext);
					}
				}
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Error", "Unable to load package action extension points:" + ex.getMessage(), ex);
			}
		}
		return actions;

	}
	
	public List<ICtPackagePropertyProvider> getPackageProperties() {
		List<ICtPackagePropertyProvider> props = new ArrayList<>();
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_ID);
			try {
				for (IConfigurationElement e : config) {
					if (e.getName().equalsIgnoreCase(ICtPackagePropertyProvider.EXT_NAME)) {
						ICtPackagePropertyProvider ext = (ICtPackagePropertyProvider) e.createExecutableExtension("class"); //$NON-NLS-1$
						props.add(ext);
					}
				}
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Error", "Unable to load package action property points:" + ex.getMessage(), ex);
			}
		}
		return props;

	}
}
