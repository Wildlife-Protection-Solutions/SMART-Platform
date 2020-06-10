package org.wcs.smart.smartcollect;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartContext;
import org.wcs.smart.smartcollect.model.ISmartCollectLabelProvider;

/**
 * The activator class controls the plug-in life cycle
 */

/*
 * 
CREATE TABLE smart.smartcollect_waypoint(wp_uuid char(16) for bit data not null,  source varchar(32000), primary key(wp_uuid));
ALTER TABLE smart.smartcollect_waypoint ADD CONSTRAINT smartcollect_wp_uuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

create table smart.smartcollect_package(uuid char(16) for bit data not null, 
name varchar(512), 
ca_uuid char(16) for bit data not null, 
cm_uuid char(16) for bit data, 
ctprofile_uuid char(16) for bit data,
basemapdef varchar(32672), primary key (uuid))

ALTER TABLE SMART.smartcollect_package ADD CONSTRAINT ct_community_package_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE SMART.smartcollect_package ADD CONSTRAINT ct_community_package_cm_uuid_fk FOREIGN KEY (CM_UUID) REFERENCES smart.configurable_model(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE SMART.smartcollect_package ADD CONSTRAINT ct_community_package_ctprofile_uuid_fk FOREIGN KEY (ctprofile_uuid) REFERENCES smart.ct_properties_profile(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;

GRANT ALL PRIVILEGES ON smart.smartcollect_package to data_entry;
GRANT ALL PRIVILEGES ON smart.smartcollect_package to manager;
GRANT ALL PRIVILEGES ON smart.smartcollect_package to analyst;
	
GRANT ALL PRIVILEGES ON smart.smartcollect_waypoint to data_entry;
GRANT ALL PRIVILEGES ON smart.smartcollect_waypoint to manager;
GRANT ALL PRIVILEGES ON smart.smartcollect_waypoint to analyst;
 */
public class SmartCollectPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.smartcollect"; //$NON-NLS-1$

	public static final String SMARTCOLLECT_ICON = "smartcollecticon";
	public static final String SMARTCOLLECT32_ICON = "smartcollecticon32";
	
	// The shared instance
	private static SmartCollectPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public SmartCollectPlugIn() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		SmartContext.INSTANCE.setClass(ISmartCollectLabelProvider.class, new SmartCollectLabelProvider());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SmartCollectPlugIn getDefault() {
		return plugin;
	}
	
	@Override
    protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(SMARTCOLLECT_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/community.16.png")); //$NON-NLS-1$
		reg.put(SMARTCOLLECT32_ICON, imageDescriptorFromPlugin(PLUGIN_ID, "images/icons/community.32.png")); //$NON-NLS-1$
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message);
			}
		});
		
	}
}
