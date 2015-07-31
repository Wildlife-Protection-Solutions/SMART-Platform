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
package org.wcs.smart.patrol;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.PatrolLabelProvider;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;


/**
 * SMART patrol feature plugin
 * @author Emily
 * @since 1.0.0
 */
public class SmartPatrolPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.patrol"; //$NON-NLS-1$

	// The shared instance
	private static SmartPatrolPlugIn plugin;
	

	/**
	 * Image descriptor key for air patrol icon
	 */
	public static final String AIR_PATROL_ICON = "org.wsc.smart.patrol.AIR_PATROL"; //$NON-NLS-1$

	/**
	 * Image descriptor key for marine patrol icon
	 */
	public static final String MARINE_PATROL_ICON = "org.wsc.smart.patrol.MARINE_PATROL"; //$NON-NLS-1$

	/**
	 * Image descriptor key for ground patrol icon
	 */
	public static final String GROUND_PATROL_ICON = "org.wsc.smart.patrol.GROUND_PATROL"; //$NON-NLS-1$

	/**
	 * Image descriptor key for patrol member
	 */
	public static final String PATROL_MEMBER_ICON = "org.wsc.smart.patrol.PATROL_MEMBER"; //$NON-NLS-1$

	/**
	 * Image descriptor key for patrol leader
	 */
	public static final String PATROL_LEADER_ICON = "org.wsc.smart.patrol.PATROL_LEADER"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for patrol pilot
	 */
	public static final String PATROL_PILOT_ICON = "org.wsc.smart.patrol.PATROL_PILOT"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for patrol icon
	 */
	public static final String PATROL_ICON = "org.wsc.smart.patrol.PATROL"; //$NON-NLS-1$
	
	
	/**
	 * Image descriptor key for patrol armed icon
	 */
	public static final String PATROL_ARMED_ICON = "org.wsc.smart.patrol.PATROL_ARMED"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for patrol armed icon
	 */
	public static final String PATROL_TEAM_ICON = "org.wsc.smart.patrol.PATROL_TEAM"; //$NON-NLS-1$
	
	/**
	 * Image descriptor key for patrol mandate icon
	 */
	public static final String PATROL_MANDATE_ICON = "org.wsc.smart.patrol.PATROL_MANDATE"; //$NON-NLS-1$

	/**
	 * Image descriptor key for patrol filetr icon
	 */
	public static final String PATROL_FILTER_ICON = "org.wsc.smart.patrol.PATROL_FILTER"; //$NON-NLS-1$
	
	/**
	 * The constructor
	 */
	public SmartPatrolPlugIn() {
	}

	@Override
    protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(PATROL_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol.png")); //$NON-NLS-1$
		reg.put(AIR_PATROL_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/airplane.png")); //$NON-NLS-1$
		reg.put(PATROL_ARMED_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol_armed.png")); //$NON-NLS-1$
		reg.put(PATROL_TEAM_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol_team.png")); //$NON-NLS-1$
		reg.put(MARINE_PATROL_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/boat.png")); //$NON-NLS-1$
		reg.put(GROUND_PATROL_ICON,imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol_ground.png")); //$NON-NLS-1$
		reg.put(PATROL_LEADER_ICON,imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol_leader.png")); //$NON-NLS-1$
		reg.put(PATROL_MEMBER_ICON,imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol_member.png")); //$NON-NLS-1$
		reg.put(PATROL_PILOT_ICON,imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol_pilot.png")); //$NON-NLS-1$
		reg.put(PATROL_MANDATE_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/obj16/patrol_mandate.png")); //$NON-NLS-1$			

		reg.put(PATROL_FILTER_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/etool16/patrol_filter.png")); //$NON-NLS-1$			
    }
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		//adds the delete handler
		ConservationAreaManager.getInstance().addDeleteHandler(new PatrolCaDeleteHandler(),PatrolCaDeleteHandler.EXECUTE_ORDER);
		SmartContext.INSTANCE.setClass(IPatrolLabelProvider.class, new PatrolLabelProvider());
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
	public static SmartPatrolPlugIn getDefault() {
		return plugin;
	}

	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
	}
	
	public static void logInfo(String message){
		getDefault().getLog().log(new Status(IStatus.OK, PLUGIN_ID, IStatus.INFO, message, null));
	}
	

	
	/**
	 * Displays an error message to the user and logs the
	 * message.
	 * 
	 * @param message  Error message to display
	 * @param t exception to log
	 */
	public static void displayLog(final String message, Throwable t){
		log(message, t);
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.SmartPatrolPlugIn_Error_DialogMessage1, message);
			}
			
		});
		
	}
	
	/**
	 * Displays an error message to the user, logs the error and
	 * exits the program with an error code of 1.
	 * 
	 * @param message the message to display to the user
	 * @param t optionally exception
	 */
	public static void displayLogExit(String message, Throwable t){
		log(message, t);
		MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.SmartPatrolPlugIn_Error_DialogMessage2, message);
		System.exit(1);
	}
	

	
}
