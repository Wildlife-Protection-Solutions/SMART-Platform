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
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.style.IStyleConfigurator;
import org.locationtech.udig.ui.ColorEditor;
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

	
	private enum LineStyle{
		LINE_SOLID(Messages.SmartGridCellStyleConfigurator_linestyle_solid, null),
		LINE_DASH(Messages.SmartGridCellStyleConfigurator_linestyle_dash, new float[]{7, 2}),
		LINE_DASHDOT(Messages.SmartGridCellStyleConfigurator_linestyle_dashdot, new float[]{7, 2, 2, 2}),
		LINE_DASHDOTDOT(Messages.SmartGridCellStyleConfigurator_linestyle_dashdotdot, new float[]{7, 2, 2, 2, 2, 2}),
		LINE_DOT(Messages.SmartGridCellStyleConfigurator_linestyle_dot, new float[]{2, 2});
		
		String localName;
		float[] dashArray;
		
		LineStyle (String name, float[] dashArray){
			this.localName = name;
			this.dashArray = dashArray;
		}
	}
    
	private static final String[] LINE_STYLES = new String[] { 
		LineStyle.LINE_SOLID.localName,
		LineStyle.LINE_DASH.localName,
		LineStyle.LINE_DASHDOT.localName,
		LineStyle.LINE_DASHDOTDOT.localName,
		LineStyle.LINE_DOT.localName};
	
	private Button btnCheck;
	
	private Composite cStyle;
	private ColorEditor btnColor;
	private Spinner cmbSize;
	private Combo cmbLineStyle;
	
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
    
        l = new Label(cStyle, SWT.NONE);
        l.setText(Messages.SmartGridCellStyleConfigurator_LineStyleLabel);
        l.setToolTipText(Messages.SmartGridCellStyleConfigurator_LineStyleTooltip);
        cmbLineStyle = new Combo(cStyle, SWT.DROP_DOWN);
        cmbLineStyle.setItems(LINE_STYLES);
        cmbLineStyle.select(0); 
   	
        btnCheck.addSelectionListener(this);
        
        cStyle.setEnabled(false);
        for (Control c : cStyle.getChildren()){
			c.setEnabled(cStyle.getEnabled());
		}
    }
    
	@Override
	public void preApply() {
		updateStyle();
	}
	
	
	private LineSymbolizer gridSymbolizer;
	private Rule rule;
	
    @Override
    protected void refresh() {
    	gridSymbolizer = null;
    	rule = null;
    	
        style = (Style) getStyleBlackboard().get(SmartGridCellStyleContent.STYLE_ID);
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
        	
        	String text = getLineStyle(SLD.lineDash(gridSymbolizer));
        	boolean found = false;
        	for (int i = 0; i < cmbLineStyle.getItemCount(); i++){
        		if (cmbLineStyle.getItem(i).equalsIgnoreCase(text)){
        			found = true;
        		}
        	}
        	if (!found) cmbLineStyle.add(text);
        	cmbLineStyle.setText(text);
        }
        updateEnabledState();
    }
    
    private void updateStyle(){
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
			gridSymbolizer.getStroke().setDashArray(getDashArray());
			
			getStyleBlackboard().put(SmartGridCellStyleContent.STYLE_ID, style);	
		}else{			
			//remove style
			getStyleBlackboard().remove(SmartGridCellStyleContent.STYLE_ID);
		}
		
		updateEnabledState();
    }
    
    @Override
	public void widgetSelected(SelectionEvent e) {
		updateStyle();
		
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

	private float[] getDashArray(){
		String text = cmbLineStyle.getText();
		for (LineStyle ls: LineStyle.values()){
			if (ls.localName.equals(text)){
				return ls.dashArray;
			}
		}
		//try to parse an array of floats
		String bits[] = text.split(","); //$NON-NLS-1$
		float[] dash = new float[bits.length];
		try{
			for (int i = 0; i < bits.length; i ++){
				dash[i] = Float.valueOf(bits[i]);
			}
			return dash;
		}catch (Exception ex){
			//TODO:
			ex.printStackTrace();
		}
		return LineStyle.LINE_SOLID.dashArray;
	}
	
	private String getLineStyle(float[] dashArray){
		if (dashArray == null) return LineStyle.LINE_SOLID.localName;
		for (LineStyle ls : LineStyle.values()){
			if (ls.dashArray != null && ls.dashArray.length != dashArray.length) continue;
			if (Arrays.equals(dashArray, ls.dashArray)){
				return ls.localName;
			};
		}
		if (dashArray.length > 0){
			StringBuilder sb = new StringBuilder();
			for (float f : dashArray){
				sb.append(f);
				sb.append(","); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
		
		return LineStyle.LINE_SOLID.localName;
	}
}
