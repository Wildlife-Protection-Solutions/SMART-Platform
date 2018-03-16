package org.wcs.smart.event.ui.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.event.model.EAction;

/**
 * User interface for collection parameters associated 
 * with an action.
 * 
 * @author Emily
 *
 */
public interface IActionParameterCollector {

	public Composite createComposite(Composite parent);
	
	public void initParameters(EAction action);
	
	public void updateParameters(EAction action);
	
	public String validate();
	
	public void addModifyListener(Listener listener);
}
