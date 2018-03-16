package org.wcs.smart.event;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EventPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.event"; //$NON-NLS-1$

	// The shared instance
	private static EventPlugIn plugin;
	
	public static final String ICON_ACTION_TYPE = "org.wcs.smart.event.icon.actiontype";
	public static final String ICON_ACTION = "org.wcs.smart.event.icon.action";
	public static final String ICON_ACTION_EVENT = "org.wcs.smart.event.icon.actionevent";
	public static final String ICON_FILTER = "org.wcs.smart.event.icon.filter";
	public static final String ICON_DELETE_MINI = "org.wcs.smart.event.icon.deletemini";
	
	/**
	 * The constructor
	 */
	public EventPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
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
	public static EventPlugIn getDefault() {
		return plugin;
	}
	
	public static void log(String message, Throwable t){
		int status = t instanceof Exception || message != null ? IStatus.ERROR : IStatus.WARNING;
        getDefault().getLog().log(new Status(status, PLUGIN_ID, IStatus.OK, message, t));
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message); //$NON-NLS-1$
			}
			
		});
		
	}	
	
	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		
		reg.put(ICON_ACTION_TYPE, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/action_type.png"));
		reg.put(ICON_ACTION, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/action.png"));
		reg.put(ICON_ACTION_EVENT, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/action_event.png"));
		reg.put(ICON_FILTER, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/event_filter.png"));
		reg.put(ICON_DELETE_MINI, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/delete_mini.png"));
	}
}
