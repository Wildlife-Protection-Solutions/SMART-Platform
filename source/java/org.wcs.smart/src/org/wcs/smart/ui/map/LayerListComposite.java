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
package org.wcs.smart.ui.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.refractions.udig.internal.ui.IDropTargetProvider;
import net.refractions.udig.project.internal.ContextModel;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectPackage;
import net.refractions.udig.project.ui.AdapterFactoryLabelProviderDecorator;
import net.refractions.udig.project.ui.internal.ProjectExplorer;
import net.refractions.udig.project.ui.internal.ViewerLayerSorter;
import net.refractions.udig.ui.UDIGDragDropUtilities;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * A viewer simple composite that lists all the layers on 
 * a map and allows them to be turned on and off.  Intended to be used
 * for only a single map (call setMap only once).
 * @author Emily
 *
 */
@SuppressWarnings("deprecation")
public class LayerListComposite extends Composite implements IDropTargetProvider {

	private CheckboxTreeViewer viewer;
	private Map currentMap;

	 private Adapter checkboxContextListener = new AdapterImpl(){
		 
		 private boolean requiresRefresh = false;
	        public void notifyChanged( final Notification msg ) {

	            if (msg.getNotifier() instanceof ContextModel) {
	                if (msg.getFeatureID(ContextModel.class) == ProjectPackage.CONTEXT_MODEL__LAYERS) {
	                    switch( msg.getEventType() ) {
	                    case Notification.ADD: {
	                        Layer layer = (Layer) msg.getNewValue();
	                        updateCheckbox(layer);
	                        requiresRefresh = true;
	                        break;
	                    }
	                    case Notification.ADD_MANY: {
	                        updateCheckboxes();
	                        requiresRefresh = true;
	                        break;
	                    }
	                    case Notification.SET: {
	                        Layer layer = (Layer) msg.getNewValue();
	                        updateCheckbox(layer);
	                        requiresRefresh = true;
	                        break;
	                    }
	                    case Notification.MOVE:{
	                    	requiresRefresh = true;
	                    	break;
	                    }
	                    }
	                }
	            } else if (msg.getNotifier() instanceof Layer) {
	                Layer layer = (Layer) msg.getNotifier();
	                if (msg.getFeatureID(Layer.class) == ProjectPackage.LAYER__VISIBLE){
	                    if (msg.getNewBooleanValue() != msg.getOldBooleanValue()) {
	                    	updateCheckbox(layer);
	                    }
	                }else if ( requiresRefresh ){
						updateCheckboxes();
						requiresRefresh = false;
	                }
	            }
	        }
	    };
	    
	public LayerListComposite(Composite parent) {
		super(parent, SWT.NONE);
		createContents();
	}
	
	/**
	 * Sets the map assocated with the layer list.  This should
	 * only be called once.
	 * 
	 * @param map
	 */
	public void setMap(Map map){
		this.currentMap = map;
		
		viewer.setInput(currentMap.getLayersInternal());
		updateCheckboxes();
		
		currentMap.addDeepAdapter(checkboxContextListener);
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				currentMap.removeDeepAdapter(checkboxContextListener);
			}
		});
	}

	private void createContents() {
		setLayout(new GridLayout());
		viewer = new CheckboxTreeViewer(this, SWT.MULTI);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer.setContentProvider(new ITreeContentProvider() {
			private Collection<Object> objects; 
			@SuppressWarnings("unchecked")
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput instanceof Collection){
					objects = (Collection<Object>) newInput;
				}
			}
			
			@Override
			public void dispose() {
			}
			
			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
			
			@Override
			public Object getParent(Object element) {
				return null;
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return objects.toArray();
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}
		});
		
		LabelProvider labelProvider = new AdapterFactoryLabelProviderDecorator(
				ProjectExplorer.getProjectExplorer().getAdapterFactory(),
				viewer);
		viewer.setLabelProvider(labelProvider);
		/*
		 * In dispose() method we need to remove this listener manually!
		 */

		viewer.setSorter(new ViewerLayerSorter());

		// sets the layer visibility to match the check box setting.
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (((Layer) event.getElement()).isVisible() != event
						.getChecked())
					((Layer) event.getElement()).setVisible(event.getChecked());
			}
		});

		UDIGDragDropUtilities.addDragDropSupport(viewer, this);
	}


	/**
	 * Updates a single checkbox
	 * @param layer
	 */
    private void updateCheckbox( final Layer layer ) {
    	if (viewer != null && !viewer.getControl().isDisposed()) {
			if (Display.getCurrent() != null){
				
                viewer.refresh();
			}else{
				viewer.getControl().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						viewer.setChecked(layer, layer.isVisible());
						viewer.refresh();		
					}});
			}
    	}
    }
    
    /**
     * Updates all checkboxes
     */
	private void updateCheckboxes() {

		List<Layer> layers = currentMap.getLayersInternal();
		final List<Layer> checkedLayers = new ArrayList<Layer>();
		for (Layer layer : layers) {
			if (layer.isVisible()) {
				checkedLayers.add(layer);
			}
		}
		if (viewer != null && !viewer.getControl().isDisposed()) {
			if (Display.getCurrent() != null){
				viewer.setCheckedElements(checkedLayers.toArray());
				viewer.refresh();
			}else{
				viewer.getControl().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (!viewer.getControl().isDisposed()){
							viewer.setCheckedElements(checkedLayers.toArray());
							viewer.refresh();
						}
					}});
			}
		}

	}

	@Override
	public Object getTarget(DropTargetEvent event) {

		if (currentMap == null) return this;
		if (currentMap.getMapLayers().isEmpty()) return this;
		return currentMap.getMapLayers().get(currentMap.getMapLayers().size() - 1);
		
	}
}
