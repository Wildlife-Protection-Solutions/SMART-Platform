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
package org.wcs.smart.asset.query;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.Session;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.IAssetQueryColumnProvider;
import org.wcs.smart.asset.query.model.observation.AssetQueryColumnProvider;
import org.wcs.smart.asset.query.ui.AssetQueryLabelProvider;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.query.common.engine.IQueryImageData;

/**
 * The activator class controls the plug-in life cycle
 */
public class AssetQueryPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.asset.query"; //$NON-NLS-1$

	//The current db version
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_1; //current
		
	// The shared instance
	private static AssetQueryPlugIn plugin;

	/**
	 * The waypoint query
	 */
	public static final String WAYPOINT_QUERY_ICON = "org.wcs.smart.query.waypointquery"; //$NON-NLS-1$

	/**
	 * The observation query
	 */
	public static final String OBSERVATION_QUERY_ICON = "org.wcs.smart.query.observationquery"; //$NON-NLS-1$

	/**
	 * The summary query
	 */
	public static final String SUMMARY_QUERY_ICON = "org.wcs.smart.query.summaryquery"; //$NON-NLS-1$

	/**
	 * The constructor
	 */
	public AssetQueryPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		SmartContext.INSTANCE.setClass(IQueryAssetLabelProvider.class, new AssetQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IAssetQueryColumnProvider.class, new AssetQueryColumnProvider());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static AssetQueryPlugIn getDefault() {
		return plugin;
	}

	@Override
	public void initializeImageRegistry(ImageRegistry reg) {
		reg.put(SUMMARY_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/summary_query.png"));//$NON-NLS-1$
		reg.put(WAYPOINT_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/waypoint_query.png"));//$NON-NLS-1$
		reg.put(OBSERVATION_QUERY_ICON,
				imageDescriptorFromPlugin(PLUGIN_ID,
						"images/icons/obj16/observation_query.png"));//$NON-NLS-1$
		super.initializeImageRegistry(reg);
	}

	/**
	 * Logs the given error to the error log.
	 * 
	 * @param message
	 *            message
	 * @param t
	 *            error
	 */
	public static void log(String message, Throwable t) {
		int status = t instanceof Exception || message != null ? IStatus.ERROR
				: IStatus.WARNING;
		getDefault().getLog().log(
				new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}

	/**
	 * Displays an error message to the user and logs the message.
	 * 
	 * @param message
	 *            Error message to display
	 * @param t
	 *            exception to log
	 */
	public static void displayLog(final String message, Throwable t) {
		log(message, t);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						Messages.QueryPlugIn_Error_DialogTitle, message);
			}
		});
	}

	public static List<AssetWaypoint> findWaypoint(Session session, IQueryImageData data) {
		ObservationAttachment a = session.get(ObservationAttachment.class, data.getAttachment().getUuid());
		Waypoint wp = null;
		if (a != null) {
			wp = a.getObservation().getWaypoint();
		}else {
			WaypointAttachment w = session.get(WaypointAttachment.class, data.getAttachment().getUuid());
			wp = w.getWaypoint();
		}
		if (wp != null) {
			return QueryFactory.buildQuery(session, AssetWaypoint.class, "waypoint", wp).list(); //$NON-NLS-1$
		}
		return null;
	}
	
	public static WaypointObservation findObservation(Session session, IQueryImageData data) {
		ObservationAttachment a = session.get(ObservationAttachment.class, data.getAttachment().getUuid());
		if (a != null) {
			return a.getObservation();
		}
		return null;
	}
	
	public static String asString(Set<String> strings) {
		if (strings.size() == 0) return "";
		if (strings.size() == 1) return strings.iterator().next();
		
		ArrayList<String> sorted = new ArrayList<>();
		sorted.addAll(strings);
		Collections.sort(sorted, (a,b)->Collator.getInstance().compare(a, b));
		StringBuilder s = new StringBuilder();
		s.append(sorted.get(0));
		for (int i = 1; i < sorted.size(); i ++) {
			s.append(", ");
			s.append(sorted.get(i));
		}
		return s.toString();
	}

}
