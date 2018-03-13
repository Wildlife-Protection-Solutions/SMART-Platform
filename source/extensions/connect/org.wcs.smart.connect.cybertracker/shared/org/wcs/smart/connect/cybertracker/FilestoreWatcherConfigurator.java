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
package org.wcs.smart.connect.cybertracker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.changetracking.IFileStoreWatcher;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Filestore watcher configurator that will provide the ctx archive directories
 * to be ignored if supported by the settings for each conservation area.
 * 
 * @author Emily
 *
 */
public class FilestoreWatcherConfigurator implements org.wcs.smart.changetracking.IReplicationEventHandler {

	/**
	 * Option value should be true if we want to ignore the ctx directory.  False
	 * to continue tracking it.  Default value of true will be assumed if
	 * no value found.
	 */
	public static final String EXCLUDE_CTX_DIR_OPTION = "IGNORE_CTX_DIR"; //$NON-NLS-1$
	

	@Override
	public void replicationEnabled(IFileStoreWatcher watcher, Session session) {
		List<CyberTrackerPropertiesOption> options = QueryFactory.buildQuery(session,  CyberTrackerPropertiesOption.class, 
				new Object[] {"optionId", EXCLUDE_CTX_DIR_OPTION}).list(); //$NON-NLS-1$
		HashMap<ConservationArea,CyberTrackerPropertiesOption> optionmap = new HashMap<>();
		for (CyberTrackerPropertiesOption op : options) {
			optionmap.put(op.getConservationArea(), op);
		}
		
		List<Path> toIgnore = new ArrayList<>();
		
		List<ConservationArea> allcas = QueryFactory.buildQuery(session, ConservationArea.class).list();
		for (ConservationArea ca : allcas) {
			Path ctctxpath = ICyberTrackerConstants.getStorageFolder(ca).toPath();
			if (!optionmap.containsKey(ca)) {
				toIgnore.add(ctctxpath);
			}else {
				CyberTrackerPropertiesOption oo = optionmap.get(ca);
				if (oo.getBooleanValue() == null || oo.getBooleanValue()) toIgnore.add(ctctxpath);
			}
		}
		
		for (Path p : toIgnore) watcher.addIgnorePath(p);
	}

	@Override
	public void replicationDisabled(IFileStoreWatcher watcher, Session session) {
		
	}

}
