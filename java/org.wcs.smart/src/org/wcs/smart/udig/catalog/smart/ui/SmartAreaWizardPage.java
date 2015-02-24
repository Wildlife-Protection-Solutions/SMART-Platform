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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.ui.AbstractUDIGImportPage;
import org.locationtech.udig.catalog.ui.UDIGConnectionPage;
import org.locationtech.udig.catalog.ui.workflow.Listener;
import org.locationtech.udig.catalog.ui.workflow.State;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
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
		super(Messages.SmartAreaWizardPage_SmartLayerConnectionPage_Title);
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		setControl(main);
		
		if (SmartDB.isMultipleAnalysis()){
			main.setLayout(new GridLayout());
			Label lbl = new Label(main, SWT.NONE);
			lbl.setText(Messages.SmartAreaWizardPage_CrossCaErrorMessage); 
			
		}else{
			IRunnableWithProgress runnable = new IRunnableWithProgress(){
            	public void run( IProgressMonitor monitor ) throws InvocationTargetException,
                    	InterruptedException {
                	getWizard().getWorkflow().next();
            	}
        	};
        	try {
            	getContainer().run(true, false, runnable);
        	} catch (Exception e) {
            	throw (RuntimeException) new RuntimeException( ).initCause( e );
        	}
		}
	}
	
	@Override
	public boolean isPageComplete() {
		if (SmartDB.isMultipleAnalysis()){
			return false;
		}
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
        	SmartPlugIn.displayLog(Messages.SmartAreaWizardPage_Error_LoadingSmartResources, ex);
        }
        return Collections.emptySet();
    }
    
    @Override
    public void setState( State state ) {
        super.setState(state);
        state.getWorkflow().addListener(this);
    }
	/* (non-Javadoc)
	 * @see org.locationtech.udig.catalog.ui.workflow.Listener#forward(org.locationtech.udig.catalog.ui.workflow.State, org.locationtech.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void forward(State current, State prev) {
        if( current == getState() ){
            current.getWorkflow().next();
        }
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.catalog.ui.workflow.Listener#backward(org.locationtech.udig.catalog.ui.workflow.State, org.locationtech.udig.catalog.ui.workflow.State)
	 */
	@Override
    public void backward( State current, State next ) {
        if( current == getState() ){
            current.getWorkflow().previous();
            current.getWorkflow().removeListener(this);
        }
    }

	/* (non-Javadoc)
	 * @see org.locationtech.udig.catalog.ui.workflow.Listener#statePassed(org.locationtech.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void statePassed(State state) {
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.catalog.ui.workflow.Listener#stateFailed(org.locationtech.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void stateFailed(State state) {
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.catalog.ui.workflow.Listener#started(org.locationtech.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void started(State first) {
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.catalog.ui.workflow.Listener#finished(org.locationtech.udig.catalog.ui.workflow.State)
	 */
	@Override
	public void finished(State last) {
		
	}
}
