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
package org.wcs.smart.plan.ui.targets;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTargetPoint;
import org.wcs.smart.plan.ui.newPlanWizard.ITargetPage;
import org.wcs.smart.ui.map.location.ILocationPointsChangeListener;
import org.wcs.smart.ui.map.location.LocationSelectComposite;

/**
 * Page for collecting spatial plan target properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class SpatialPlanTargetPropertyPage implements ITargetPage, ILocationPointsChangeListener {

	private static final int UPPER_PART_HEIGHT_HINT = 100;
	private static final int CONTENT_MIN_HEIGHT = 325;

	private TargetPropertyDialog parentWindow;

	private Text targetName;
    private ControlDecoration nameDecoration;
	private Text txtDistanceToComplete;
    private ControlDecoration distancDecoration;
 	private Text targetDesc;
	private LocationSelectComposite<SpatialPlanTargetPoint> locationSelect;
	
	private boolean isInit = false;	
	private Listener changeListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (!isInit){
				parentWindow.setDirty();
			}
		}
	};
	
	public SpatialPlanTargetPropertyPage(TargetPropertyDialog targetPropertyPage) {
		this.parentWindow = targetPropertyPage;
	}

	@Override
	public Composite createComponent(Composite parent, int style) {
		
		ScrolledComposite scrollCmp = new ScrolledComposite(parent, SWT.V_SCROLL);
		
		final Composite main = new Composite(scrollCmp, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        main.setLayoutData(layoutData);

		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		GridData centerLayout = new GridData(SWT.FILL, SWT.FILL, true, true);
		centerLayout.grabExcessVerticalSpace = false;
		centerLayout.heightHint = UPPER_PART_HEIGHT_HINT;
		center.setLayoutData(centerLayout);
	
		Label nameLabel = new Label(center, SWT.NONE);
		nameLabel.setText(Messages.SpatialPlanTargetPropertyPage_Name_Label);
		nameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		targetName = new Text(center, SWT.BORDER );
		targetName.setTextLimit(PlanTarget.MAX_NAME_LENGTH);
		targetName.setLayoutData( createGridDataWithIndent());
		targetName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isTargetNameValid()) {
					nameDecoration.hide();
				} else {
					nameDecoration.show();
				}
			}
		});
		targetName.addListener(SWT.Modify, changeListener);

		nameDecoration = createDecoration(targetName);
		nameDecoration.setDescriptionText(Messages.SpatialPlanTargetPropertyPage_Name_Required_Error);

		Label distanceLabel = new Label(center, SWT.NONE);
		distanceLabel.setText(Messages.SpatialPlanTargetPropertyPage_DistanceForCompletion_Label);
		distanceLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		txtDistanceToComplete = new Text(center, SWT.BORDER);
		//init with default value BEFORE adding any listeners
		txtDistanceToComplete.setText(String.valueOf(getDefaultDistanceToComplete()));
		txtDistanceToComplete.setTextLimit(32);
		txtDistanceToComplete.setLayoutData(createGridDataWithIndent());
		txtDistanceToComplete.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isDistanceToCompleteValid()) {
					distancDecoration.hide();
				} else {
					distancDecoration.show();
				}
			}
		});
		txtDistanceToComplete.addListener(SWT.Modify, changeListener);
		
		distancDecoration = createDecoration(txtDistanceToComplete);
		distancDecoration.setDescriptionText(Messages.SpatialPlanTargetPropertyPage_InvalidDistanceForCompletion_Error);
		if (isDistanceToCompleteValid()) {
			distancDecoration.hide();
		} else {
			distancDecoration.show();
		}
		
		Label descrLabel = new Label(center, SWT.NONE);
		descrLabel.setText(Messages.SpatialPlanTargetPropertyPage_Description_Label);
		descrLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		targetDesc= new Text(center, SWT.BORDER  | SWT.WRAP | SWT.V_SCROLL);
		targetDesc.setTextLimit(AdministrativePlanTarget.MAX_DESC_LENGTH);
		targetDesc.setLayoutData(createGridDataWithIndent());
		((GridData)targetDesc.getLayoutData()).widthHint = 100;
		((GridData)targetDesc.getLayoutData()).grabExcessVerticalSpace = true;
		targetDesc.addListener(SWT.Modify, changeListener);
		
        //location selection
        locationSelect = new LocationSelectComposite<SpatialPlanTargetPoint>(main, SWT.NONE, getLayerStyle()) {
			@Override
			protected SpatialPlanTargetPoint createNewPoint() {
				return new SpatialPlanTargetPoint();
			}
        };
        locationSelect.addLocationPointsChangeListener(this);
        locationSelect.getDecoration().setDescriptionText(Messages.SpatialPlanTargetPropertyPage_Description_Required_Error);
        locationSelect.getDecoration().show();
        locationSelect.addLocationPointsChangeListener(new ILocationPointsChangeListener() {
			@Override
			public void locationPointsChanged() {
				changeListener.handleEvent(null);
			}
		});
        
        
        //the first time this composite is displayed it needs to refresh the map bounds
        PaintListener firstPaint = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (!main.isDisposed()){
					locationSelect.getMap().sendCommandASync(new ZoomExtentCommand());
				}
				main.removePaintListener(this);
			}
		};
        main.addPaintListener(firstPaint);
        
        scrollCmp.setContent(main);
		scrollCmp.setExpandVertical(true);
		scrollCmp.setExpandHorizontal(true);
		scrollCmp.setMinHeight(CONTENT_MIN_HEIGHT);
		return scrollCmp;
	}

	private ControlDecoration createDecoration(Control control) {
		ControlDecoration decoration = new ControlDecoration(control, SWT.LEFT);
		decoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		decoration.setShowHover(true);
		return decoration;
	}
	
	private GridData createGridDataWithIndent(){
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 8;
		return gd;
	}
	
	@Override
	public String getPageName() {
		return Messages.SpatialPlanTargetPropertyPage_PageName;
	}
	
	@Override
	public PlanTarget createTarget() {
		return new SpatialPlanTarget();
	}

	@Override
	public void initPage(PlanTarget pt) {
		this.isInit = true;
		try{
			if (!(pt instanceof SpatialPlanTarget)){
				return;
			}
			SpatialPlanTarget ptSpatial = (SpatialPlanTarget) pt;
			targetName.setText(ptSpatial.getName());
			txtDistanceToComplete.setText(String.valueOf(ptSpatial.getDistanceForCompletion()));
			targetDesc.setText(ptSpatial.getDescription()==null ? "" : ptSpatial.getDescription()); //$NON-NLS-1$
			locationSelect.setPoints(ptSpatial.getPoints());
		}finally{
			this.isInit=false;
		}
	}
	
	@Override
	public void updateTarget(PlanTarget pt) {
		if (!(pt instanceof SpatialPlanTarget)){
			return;
		}
		SpatialPlanTarget ptSpatial = (SpatialPlanTarget) pt;
		ptSpatial.setName(targetName.getText());
		if (targetDesc.getText().trim().length() == 0){
			ptSpatial.setDescription(null);
		}else{
			ptSpatial.setDescription(targetDesc.getText());
		}
		ptSpatial.setDistanceForCompletion(Integer.valueOf(txtDistanceToComplete.getText()));

		//create a copy of points array (we aren't allowed to modify original list as this will effect gui)
		List<SpatialPlanTargetPoint> points = new ArrayList<SpatialPlanTargetPoint>(locationSelect.getPoints());
		//Update the points
		for (Iterator<SpatialPlanTargetPoint> iterator = ptSpatial.getPoints().iterator(); iterator.hasNext();) {
			SpatialPlanTargetPoint ptPoint = iterator.next();
			if (!points.remove(ptPoint)){
				iterator.remove();
			}
		}
		
		//add remaining; these should all be new points
		for (Iterator<SpatialPlanTargetPoint> iterator = points.iterator(); iterator.hasNext();) {
			SpatialPlanTargetPoint ptPoint = iterator.next();
			ptPoint.setPlanTarget(ptSpatial);
			ptSpatial.getPoints().add(ptPoint);
		}
	}


	private boolean isTargetNameValid() {
    	return targetName != null && 
    			targetName.getText() != null && 
    			!targetName.getText().isEmpty() && 
    			targetName.getText().length() <= PlanTarget.MAX_NAME_LENGTH;
	}

	private boolean isDistanceToCompleteValid() {
		try {
			Integer.parseInt(txtDistanceToComplete.getText());
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	private boolean isLocationPointsValid() {
    	return !locationSelect.getPoints().isEmpty();
	}
	
	@Override
	public boolean validate() {
		return isTargetNameValid() && isDistanceToCompleteValid() && isLocationPointsValid();
	}

	private int getDefaultDistanceToComplete() {
		int propValue = SmartPlanPlugIn.getDefault().getPreferenceStore().getInt(SmartPlanPlugIn.SYSPROP_PLAN_DISTANCE_TO_COMPLETE);
		return propValue;
	}
	
	@Override
	public void locationPointsChanged() {
		if (isLocationPointsValid()) {
			locationSelect.getDecoration().hide();
		} else {
			locationSelect.getDecoration().show();
		}
	}

	private Style getLayerStyle() {
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		StyleBuilder sb = new StyleBuilder();
		
		//not selected
		Mark mark = sb.createMark("circle",  new Color(0, 64, 128), new Color(0, 64, 128), 1); //$NON-NLS-1$ 
		Graphic graph2 = sb.createGraphic(null, mark, null, 1, 10, 0);
        PointSymbolizer notselected = sb.createPointSymbolizer(graph2);
        
        Rule rnotselected = sb.createRule(notselected);
        rnotselected.setFilter(ff.equals(ff.property("selected"), ff.literal(Boolean.FALSE))); //$NON-NLS-1$
        rnotselected.setName("Not Selected"); //$NON-NLS-1$
        
        //selected
        mark = sb.createMark("circle",  new Color(255, 255, 0), new Color(255, 255, 0), 1); //$NON-NLS-1$
		graph2 = sb.createGraphic(null, mark, null, 1, 10, 0);
        PointSymbolizer selected = sb.createPointSymbolizer(graph2);
        
        Rule rselected = sb.createRule(selected);
        rselected.setFilter(ff.equals(ff.property("selected"), ff.literal(Boolean.TRUE))); //$NON-NLS-1$
        rselected.setName("Selected"); //$NON-NLS-1$
        
        FeatureTypeStyle fs = sb.createFeatureTypeStyle(null, rselected, rnotselected);
        Style s = sb.createStyle();
        s.featureTypeStyles().add(fs);
        return s;
	}

}
