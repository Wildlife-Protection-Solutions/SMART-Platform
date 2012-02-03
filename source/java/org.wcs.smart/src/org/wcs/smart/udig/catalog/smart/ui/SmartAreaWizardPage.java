package org.wcs.smart.udig.catalog.smart.ui;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.catalog.ui.AbstractUDIGImportPage;
import net.refractions.udig.catalog.ui.UDIGConnectionPage;
import net.refractions.udig.catalog.ui.workflow.Listener;
import net.refractions.udig.catalog.ui.workflow.State;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;

/**
 * Connection page for connecting to smart area dataset.
 * 
 * This page simple skips to the next page in the wizard.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartAreaWizardPage extends AbstractUDIGImportPage implements
		UDIGConnectionPage, Listener {

	public SmartAreaWizardPage() {
		super("Smart Area Layer Connection Page");
	}

	@Override
	public void createControl(Composite parent) {
		setControl(new Composite(parent, SWT.NONE));
        IRunnableWithProgress runnable = new IRunnableWithProgress(){

            public void run( IProgressMonitor monitor ) throws InvocationTargetException,
                    InterruptedException {
                getWizard().getWorkflow().next();
            }
            
        };
        try {
            getContainer().run(true, false, runnable);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) new RuntimeException( ).initCause( e );
        } catch (InterruptedException e) {
            throw (RuntimeException) new RuntimeException( ).initCause( e );
        }
	}
	
	@Override
	public boolean isPageComplete() {
		return true;
	}
	
	 /** 
     * Gather up connection parameters from the user interface
     * @return connection parameters from the user interface
     */
    @Override
    public Map<String, Serializable> getParams() {
    	HashMap<String, Serializable> map = new HashMap<String, Serializable>();
    	map.put(SmartServiceExtension.CA_UUID_KEY, SmartDB.getCurrentConservationArea().getUuid());
    	return map;
    }
    
    @Override
    public Collection<URL> getResourceIDs() {
        Set<URL> ids = new HashSet<URL>();
        Map<String, Serializable> params = getParams();
        if (params == null){
            return Collections.emptySet();
        }
        
        try{
            Collection<IService> services = getServices();
            for( IService iService : services ) {
               List<? extends IGeoResource> resources = iService.resources(null);
               for( IGeoResource iGeoResource : resources ) {
                   ids.add(iGeoResource.getIdentifier());
               }
            }
            return ids;
           
        }catch (Exception ex){
        	SmartPlugIn.displayLog(getShell(), "Error loading smart resources.", ex);
        }
        return Collections.emptySet();
    }
    
    @Override
    public void setState( State state ) {
        super.setState(state);
        state.getWorkflow().addListener(this);
    }
	/* (non-Javadoc)
	 * @see net.refractions.udig.catalog.ui.workflow.Listener#forward(net.refractions.udig.catalog.ui.workflow.State, net.refractions.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void forward(State current, State prev) {
        if( current == getState() ){
            current.getWorkflow().next();
        }
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.catalog.ui.workflow.Listener#backward(net.refractions.udig.catalog.ui.workflow.State, net.refractions.udig.catalog.ui.workflow.State)
	 */
	@Override
    public void backward( State current, State next ) {
        if( current == getState() ){
            current.getWorkflow().previous();
            current.getWorkflow().removeListener(this);
        }
    }

	/* (non-Javadoc)
	 * @see net.refractions.udig.catalog.ui.workflow.Listener#statePassed(net.refractions.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void statePassed(State state) {
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.catalog.ui.workflow.Listener#stateFailed(net.refractions.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void stateFailed(State state) {
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.catalog.ui.workflow.Listener#started(net.refractions.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void started(State first) {
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.catalog.ui.workflow.Listener#finished(net.refractions.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void finished(State last) {
		
	}
}
