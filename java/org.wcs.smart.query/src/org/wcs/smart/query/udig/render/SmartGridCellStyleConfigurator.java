/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.wcs.smart.query.udig.render;

import java.awt.Color;

import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.style.IStyleConfigurator;
import net.refractions.udig.ui.ColorEditor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.ConstantExpression;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.query.common.model.udig.RasterService;
import org.wcs.smart.query.internal.Messages;

/**
 * A style configurator that allows 
 * grid cell border styles.
 * 
 * @author Emily
 * @since 1.1.0
 */
public class SmartGridCellStyleConfigurator extends IStyleConfigurator implements SelectionListener {

	public static final String STYLE_ID = "org.wcs.smart.query.grid.style.border"; //$NON-NLS-1$
	
	private Button btnCheck;
	
	private Composite cStyle;
	private ColorEditor btnColor;
	private Spinner cmbSize;
	
	private Style style;
	private FilterFactory ff = CommonFactoryFinder.getFilterFactory();
	private StyleFactory sf = CommonFactoryFinder.getStyleFactory();
	
    @Override
    public boolean canStyle( Layer layer ) {
    	return layer.canAdaptTo(RasterService.class);
    }

    @Override
    public void createControl( Composite parent ) {
        GridLayout gridLayout = new GridLayout(1, false);
        parent.setLayout(gridLayout);

        btnCheck = new Button(parent, SWT.CHECK);
        btnCheck.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        btnCheck.setText(Messages.SmartGridCellStyleConfigurator_CellBorderLabel);
        
        cStyle = new Composite(parent, SWT.NONE);
        cStyle.setLayout(new GridLayout(2, false));
        cStyle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        ((GridData)cStyle.getLayoutData()).horizontalIndent = 10;
        
        Label l = new Label(cStyle, SWT.NONE);
        l.setText(Messages.SmartGridCellStyleConfigurator_ColorLabel);
        
        btnColor = new ColorEditor(cStyle);
        btnColor.getButton().setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
        btnColor.setColor(Color.BLACK);
        
        l = new Label(cStyle, SWT.NONE);
        l.setText(Messages.SmartGridCellStyleConfigurator_SizeLabel);
        
        cmbSize = new Spinner(cStyle, SWT.BORDER);
        cmbSize.setMinimum(0);
        cmbSize.setMaximum(100);
        cmbSize.setSelection(1);
        
        btnCheck.addSelectionListener(this);
        btnColor.getButton().addSelectionListener(this);
        cmbSize.addSelectionListener(this);
        
        cStyle.setEnabled(false);
        for (Control c : cStyle.getChildren()){
			c.setEnabled(cStyle.getEnabled());
		}
    }
    
	@Override
	public void preApply() {
	}
	
	
	private LineSymbolizer gridSymbolizer;
	private Rule rule;
	
    @Override
    protected void refresh() {
    	gridSymbolizer = null;
    	rule = null;
    	
        style = (Style) getStyleBlackboard().get(STYLE_ID);
        if (style == null){
        	style = sf.createStyle();
        	FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        	style.featureTypeStyles().add(fts);
        	fts.rules().add(sf.createRule());
        }
        
        for (FeatureTypeStyle fs : style.featureTypeStyles()){
        	for (Rule r : fs.rules()){
        		for (Symbolizer s : r.symbolizers()){
        			if (s instanceof LineSymbolizer){
        				gridSymbolizer = (LineSymbolizer) s;
        				rule = r;
        			}
        		}
        	}
        }
        if (gridSymbolizer == null){
        	btnCheck.setSelection(false);
        }else{
        	btnCheck.setSelection(true);
        	
        	btnColor.setColor(SLD.lineColor(gridSymbolizer));
        	cmbSize.setSelection(SLD.lineWidth(gridSymbolizer));
        }
        updateEnabledState();
    }
    
    
    @Override
	public void widgetSelected(SelectionEvent e) {
		if (btnCheck.getSelection()){
			if (gridSymbolizer == null){
				gridSymbolizer = sf.createLineSymbolizer();
				gridSymbolizer.setStroke(sf.createStroke(ConstantExpression.color(Color.BLACK), ff.literal(1)));
				if (rule == null){
					rule = CommonFactoryFinder.getStyleFactory().createRule();
					style.featureTypeStyles().get(0).rules().add(rule);
				}
				rule.symbolizers().add(gridSymbolizer);
			}
			gridSymbolizer.getStroke().setWidth(ff.literal(cmbSize.getSelection()));
			gridSymbolizer.getStroke().setColor(ConstantExpression.color(btnColor.getColor()));
			
			getStyleBlackboard().put(STYLE_ID, style);	
		}else{			
			//remove style
			getStyleBlackboard().remove(STYLE_ID);
		}
		
		updateEnabledState();
		
	}

    private void updateEnabledState(){
    	boolean enabled = btnCheck.getSelection();
    	cStyle.setEnabled(enabled);
    	for (Control c : cStyle.getChildren()){
			c.setEnabled(enabled);
		}
    }
    
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}

}
