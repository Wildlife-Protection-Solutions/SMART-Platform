/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 *
 */
package org.wcs.smart.udig.legend.style;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.locationtech.udig.legend.ui.LegendGraphic;
import org.locationtech.udig.mapgraphic.MapGraphic;
import org.locationtech.udig.project.IBlackboard;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.style.IStyleConfigurator;
import org.wcs.smart.internal.Messages;

/**
 * Style to identify in the layer should appear in the legend mapgraphic or not
 * 
 * @author Emily
 */
public final class LegendLayerStyleConfigurator extends IStyleConfigurator implements SelectionListener, ModifyListener {

	private Button btnVisible;
	private Button btnExcludeRoot;
	private Button btnHideRootImage;
	private Label lblHideRootImage;
	
	private CheckboxTableViewer layers;
	
	private Composite parent;
	private boolean created = false;
    /**
     * @see org.locationtech.udig.style.StyleConfigurator#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl( Composite parent ) {
        this.parent = parent;   
    }
    

	public void init() {
		// do nothing

	}

	public void refresh() {
		if (!created) {
			created = true;
			if (getLayer().canAdaptTo(LegendGraphic.class)) {
				parent.setLayout( new GridLayout( ));		
				Label xLabel = new Label(parent, SWT.RIGHT);
				xLabel.setText(Messages.LegendLayerStyleConfigurator_VisibleLayers);
				
				layers = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.V_SCROLL);
				layers.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				layers.setContentProvider(ArrayContentProvider.getInstance());
				layers.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						return ((ILayer) element).getName();
					}
				});
			} else {
				parent.setLayout( new GridLayout( 2, false ));
				
				Label xLabel = new Label(parent, SWT.RIGHT);
				xLabel.setText(Messages.LegendLayerStyleConfigurator_ShowInLegendImage);
				xLabel.setToolTipText(Messages.LegendLayerStyleConfigurator_ShowInLegendTooltip);

				btnVisible = new Button(parent, SWT.CHECK);
				btnVisible.addSelectionListener(this);
				btnVisible.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				
				Label label = new Label(parent, SWT.RIGHT);
				label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 2, 1));
				
				label = new Label(parent, SWT.RIGHT);
				label.setText(Messages.LegendLayerStyleConfigurator_HideHeaderLabel);
				label.setToolTipText(Messages.LegendLayerStyleConfigurator_HideHeaderTooltip);

				btnExcludeRoot = new Button(parent, SWT.CHECK);
				btnExcludeRoot.addSelectionListener(this);
				btnExcludeRoot.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				
				
				lblHideRootImage = new Label(parent, SWT.RIGHT);
				lblHideRootImage.setText(Messages.LegendLayerStyleConfigurator_HideHeaderImageLabel);
				lblHideRootImage.setToolTipText(Messages.LegendLayerStyleConfigurator_HideHeaderImageTooltip);

				btnHideRootImage = new Button(parent, SWT.CHECK);
				btnHideRootImage.addSelectionListener(this);
				btnHideRootImage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			}
		}
		
		if (getLayer().canAdaptTo(LegendGraphic.class)) {
			
			List<ILayer> configurelayers = new ArrayList<>();
			for (ILayer l : getLayer().getMap().getMapLayers()) {
				if (l.hasResource(MapGraphic.class)) continue;
				configurelayers.add(0,l);
			}
			layers.setInput( configurelayers );
			
			for (ILayer l : getLayer().getMap().getMapLayers()) {
			    LegendLayerStyle style = (LegendLayerStyle) l.getStyleBlackboard().get(LegendLayerStyleContent.ID);
			    if (style == null || style.isVisible) {
			    	layers.setChecked(l, true);
			    }
			}
			
			layers.addCheckStateListener(new ICheckStateListener() {
				
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					widgetSelected(null);
				}
			});
		}else {
		
		    IBlackboard blackboard = getStyleBlackboard();
		    LegendLayerStyle style = (LegendLayerStyle) blackboard.get(LegendLayerStyleContent.ID);
	
	        if (style == null) {
	            style = LegendLayerStyleContent.createDefaultStyle();
	            blackboard.put(LegendLayerStyleContent.ID, style);
	            ((StyleBlackboard) blackboard).setSelected(new String[]{LegendLayerStyleContent.ID});
	        }
	
	        btnVisible.setSelection(style.isVisible);
	        btnExcludeRoot.setSelection(style.excludeRoot);
	        btnHideRootImage.setSelection(style.hideRootImage);
	        
	        btnHideRootImage.setEnabled(!btnExcludeRoot.getSelection());
	        lblHideRootImage.setEnabled(!btnExcludeRoot.getSelection());
		}
    }
	
    public boolean canStyle( Layer layer ) {
    	return true;
    }

	@Override
	public void modifyText(ModifyEvent e) {
		widgetSelected(null);
		
	}

    /*
     * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetSelected( SelectionEvent e ) {
    	if (getLayer().canAdaptTo(LegendGraphic.class)) {
    		for (ILayer l : getLayer().getMap().getMapLayers()) {
    			
				if (l.hasResource(MapGraphic.class)) continue;

    			LegendLayerStyle style = (LegendLayerStyle) l.getStyleBlackboard().get(LegendLayerStyleContent.ID);
    			boolean isvisible = layers.getChecked(l);
    			
    			if (style == null) {
    				if (!isvisible) {
    					style = LegendLayerStyleContent.createDefaultStyle();
    					style.isVisible = false;
    					style.excludeRoot = false;
    					style.hideRootImage = false;
    					l.getStyleBlackboard().put(LegendLayerStyleContent.ID, style);
    				}
    			}else {
    				style.isVisible = isvisible;
					l.getStyleBlackboard().put(LegendLayerStyleContent.ID, style);
  			    }
    		}
    	}else {
	        IBlackboard blackboard = getStyleBlackboard();
	        LegendLayerStyle style = (LegendLayerStyle) blackboard.get(LegendLayerStyleContent.ID);
	
	        if (style == null) {
	            style = LegendLayerStyleContent.createDefaultStyle();
	            
	            blackboard.put(LegendLayerStyleContent.ID, style);
	            ((StyleBlackboard) getStyleBlackboard()).setSelected(new String[]{LegendLayerStyleContent.ID});
	        }
	        
	        style.isVisible = btnVisible.getSelection();
	        style.excludeRoot = btnExcludeRoot.getSelection();
	        style.hideRootImage = btnHideRootImage.getSelection();
	        
	        btnHideRootImage.setEnabled(!btnExcludeRoot.getSelection());
	        lblHideRootImage.setEnabled(!btnExcludeRoot.getSelection());
    	}
        
    }
    
    /*
     * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetDefaultSelected( SelectionEvent e ) {
        //do nothing
    }

   

}
