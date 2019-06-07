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
package org.wcs.smart.cybertracker.export.mbtile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.AbstractCtPackage.BaseMapKeys;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Contribution for map files
 * 
 * @author Emily
 *
 */
public class MapPackageContribution implements IPackageContribution{

	//package files
	public static final String MAPFILE = "map.mbtiles"; //$NON-NLS-1$
	
	@Override
	public IPackageUiContribution getUiController() {
		return new MapPackageUiContribution();
	}

	@Override
	public void createDetails(Composite parent, ICtPackage ctpackage, Session session) {
		if (!(ctpackage instanceof AbstractCtPackage)) return;
		AbstractCtPackage pp = (AbstractCtPackage)ctpackage;
		
		Label l = new Label(parent, SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setText(Messages.MapPackageContribution_BasemapFilesOp);
		((GridData)l.getLayoutData()).verticalIndent = 5;
		
		l = new Label(parent, SWT.NONE);
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		String bm = pp.getBasemapDef();
		if (bm == null) {
			l.setText(Messages.MapPackageContribution_NoneLabel);
		}else {
			JSONParser parser = new JSONParser();
			try {
				JSONObject obj = (JSONObject) parser.parse(bm);
				
				if (obj.containsKey(BaseMapKeys.FILE.jsonkey)) {
					l.setText((String)obj.get(BaseMapKeys.FILE.jsonkey));
				}else if (obj.containsKey(BaseMapKeys.BM.jsonkey)) {
					UUID uuid = UuidUtils.stringToUuid((String)obj.get(BaseMapKeys.BM.jsonkey));
					BasemapDefinition bmdef = session.get(BasemapDefinition.class, uuid);
					if (bmdef != null) {
						l.setText(bmdef.getName());
					}else {
						l.setText(Messages.MapPackageContribution_UnknownLabel);
					}
				}else {
					l.setText(Messages.MapPackageContribution_UnknownLabel);
				}
			}catch (Exception ex) {
				CyberTrackerPlugIn.log(ex.getMessage(), ex);
				l.setText(Messages.MapPackageContribution_UnknownLabel);
			}
		}
	}
	
	
	@Override
	public PackageContribution packageFiles(ICtPackage ctpackage, IProgressMonitor monitor) throws IOException {
		if (!(ctpackage instanceof AbstractCtPackage)) return new PackageContribution();
		AbstractCtPackage pp = (AbstractCtPackage)ctpackage;
		
		if (pp.getBasemapDef() == null) return new PackageContribution();
		
		JSONParser parser = new JSONParser();
		JSONObject obj = null;
		try {
			obj = (JSONObject) parser.parse(pp.getBasemapDef());
		}catch (Exception ex) {
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
			return new PackageContribution();
		
		}
	
		Path tempDir = Files.createTempDirectory("smartmap"); //$NON-NLS-1$
		Path mapFile = tempDir.resolve(MAPFILE);
		
		PackageContribution updates = new PackageContribution() {
			@Override
			public void cleanUp() throws IOException{
				FileUtils.deleteDirectory(tempDir.toFile());
			}
		};
		
		if (obj.containsKey(BaseMapKeys.BM.jsonkey)) {

			BasemapDefinition def = null;
			try(Session session = HibernateManager.openSession()){
				UUID bm = UuidUtils.stringToUuid((String)obj.get(BaseMapKeys.BM.jsonkey));
				def = session.get(BasemapDefinition.class, bm);
				
			}
			if (def == null) {
				CyberTrackerPlugIn.log(Messages.MapPackageContribution_BasemapNotFound, null);
				return new PackageContribution();
			}

			MbTileGenerator generator = new MbTileGenerator();
			updates.setProjectMetadata(CtJsonExportUtils.MAP_FILE_DIRECTORY_NAME, MAPFILE);
			int minZoom = ((Long)obj.get(BaseMapKeys.MINZOOM.jsonkey)).intValue();
			int maxZoom = ((Long)obj.get(BaseMapKeys.MINZOOM.jsonkey)).intValue();
			
			double xmin = (double) obj.get(BaseMapKeys.XMIN.jsonkey);
			double xmax = (double) obj.get(BaseMapKeys.XMAX.jsonkey);
			double ymin = (double) obj.get(BaseMapKeys.YMIN.jsonkey);
			double ymax = (double) obj.get(BaseMapKeys.YMAX.jsonkey);
			ReferencedEnvelope env = new ReferencedEnvelope(xmin, xmax, ymin, ymax, SmartDB.DATABASE_CRS);
			try {
				generator.generateMbTiles(mapFile, env, minZoom, maxZoom, def, monitor);
			}catch (Exception ex) {
				throw new IOException(ex);
			}
			if (monitor.isCanceled()) return null;
			updates.addFile(mapFile);
		}else if (obj.containsKey(BaseMapKeys.FILE.jsonkey)) {
			Path dir = ICyberTrackerConstants.getBasemapFileStore(ctpackage);
			if (Files.exists(dir)) {
				updates.addFile(dir);
				updates.setProjectMetadata(CtJsonExportUtils.MAP_FILE_DIRECTORY_NAME, dir.getFileName().toString());
			}
		}
		
		if (monitor.isCanceled()) return null;
		return updates;
		
	}


}
