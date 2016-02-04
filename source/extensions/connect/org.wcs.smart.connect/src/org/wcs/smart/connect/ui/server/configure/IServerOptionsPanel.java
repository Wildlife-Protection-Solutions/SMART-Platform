package org.wcs.smart.connect.ui.server.configure;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.connect.model.ConnectServer;

/**
 * Interface for allowing plugins to add to the SMART Connect configuration options.
 * This page provides the ui for updating these options.
 * @author Emily
 *
 */
public interface IServerOptionsPanel {
	
	public static final String EXTENSION_ID = "org.wcs.smart.connect.configure.optionpanel"; //$NON-NLS-1$
	
	/**
	 * The name of the panel
	 * @return
	 */
	public String getName();
	
	/**
	 * Initializes widgets on the panel with information from the
	 * connect server.
	 * 
	 * @param server
	 */
	public void initValues(ConnectServer server);
	
	/**
	 * Updates the connect server object with the items
	 * from the panel
	 * @param server
	 */
	public void updateServer(ConnectServer server);
	
	/**
	 * Function called after the server options are saved to the database.
	 * This allows for events to be fired, or jobs started as required.
	 */
	public void afterSave(ConnectServer server);
	
	/**
	 * 
	 * @return <code>true</code> if all fields are valid, false if an error exists
	 */
	public boolean isValid();
	
	/**
	 * Creates the option panel widgets
	 * @param parent
	 * @param isEditable
	 * @return
	 */
	public Composite createComposite(Composite parent, boolean isEditable);
	
	/**
	 * Adds a change listener that should be fired when an option on the panel
	 * has been modified.
	 * 
	 * @param listener
	 */
	public void addChangeListener(ModifyListener listener);
}
