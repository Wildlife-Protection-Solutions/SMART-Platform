package org.wcs.smart.p2;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.p2.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	
	private static BundleContext context;
	private ScopedPreferenceStore preferenceStore;

	private IPropertyChangeListener preferenceListener;

	
	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the singleton plugin instance
	 * 
	 * @return the instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public Activator() {
		// constructor
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
		Activator.context = bundleContext;
		PreferenceInitializer.migratePreferences();
		getPreferenceStore().addPropertyChangeListener(getPreferenceListener());
	}

	private IPropertyChangeListener getPreferenceListener() {
		if (preferenceListener == null) {
			preferenceListener = new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					updateWithPreferences(getPolicy());
				}
			};
		}
		return preferenceListener;
	}

	public ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}

	Policy getPolicy() {
		return getProvisioningUI().getPolicy();
	}

	public IProvisioningAgent getProvisioningAgent() {
		return getProvisioningUI().getSession().getProvisioningAgent();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		getPreferenceStore().removePropertyChangeListener(preferenceListener);
		super.stop(bundleContext);
	}

	public static IStatus getNoSelfProfileStatus() {
		return new Status(IStatus.WARNING, PLUGIN_ID, "Could not locate the running profile instance. The eclipse.p2.data.area and eclipse.p2.profile properties may not be set correctly in this application's config.ini file.");
	}

	void updateWithPreferences(Policy policy) {
		policy.setShowLatestVersionsOnly(getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
	}

	/*
	 * Overridden to use a profile scoped preference store.
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#getPreferenceStore()
	 */
	public IPreferenceStore getPreferenceStore() {
		// Create the preference store lazily.
		if (preferenceStore == null) {
			final IAgentLocation agentLocation = getAgentLocation();
			if (agentLocation == null)
				return super.getPreferenceStore();
			preferenceStore = new ScopedPreferenceStore(new ProfileScope(agentLocation, IProfileRegistry.SELF), PLUGIN_ID);
		}
		return preferenceStore;
	}

	private IAgentLocation getAgentLocation() {
		ServiceReference<?> ref = getContext().getServiceReference(IAgentLocation.SERVICE_NAME);
		if (ref == null)
			return null;
		IAgentLocation location = (IAgentLocation) getContext().getService(ref);
		getContext().ungetService(ref);
		return location;
	}

	public void savePreferences() {
		if (preferenceStore != null)
			try {
				preferenceStore.save();
			} catch (IOException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Error saving update preferences", e), StatusManager.LOG | StatusManager.SHOW);
			}
	}
}