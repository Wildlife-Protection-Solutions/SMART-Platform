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
package org.wcs.smart.cybertracker.survey;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.osgi.framework.BundleContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.ICaDeleteHandler;

/**
 * The activator class controls the plug-in life cycle
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyCyberTrackerPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.cybertracker.survey"; //$NON-NLS-1$

	// The shared instance
	private static SurveyCyberTrackerPlugIn plugin;
	
	public static final String DB_VERSION_2 = "2.0"; //$NON-NLS-1$
	public static final String DB_VERSION_1 = "1.0"; //$NON-NLS-1$
	public static final String DB_VERSION = DB_VERSION_2;
	
	/**
	 * The constructor
	 */
	public SurveyCyberTrackerPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		ICaDeleteHandler deleteHandler = new ICaDeleteHandler() {
			@Override
			public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor) throws Exception {
				Query<?> q = session.createQuery("delete from SurveyCtPackage where conservationArea = :ca"); //$NON-NLS-1$
				q.setParameter("ca", ca); //$NON-NLS-1$
				q.executeUpdate();
			}
		};
		ConservationAreaManager.getInstance().addDeleteHandler(deleteHandler, 1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
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
	public static SurveyCyberTrackerPlugIn getDefault() {
		return plugin;
	}

}
