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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTargetPoint;
import org.wcs.smart.plan.ui.newPlanWizard.ITargetPage;
import org.wcs.smart.ui.map.location.ILocationPointsChangeListener;
import org.wcs.smart.ui.map.location.ISmartPoint;
import org.wcs.smart.ui.map.location.LocationSelectComposite;

/**
 * Page for collecting spatial plan target properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class SpatialPlanTargetPropertyPage implements ITargetPage, ILocationPointsChangeListener {

	private static final int CONTENT_MIN_HEIGHT = 325;

	private TargetPropertyPage parentWindow;

	private Text targetName;
    private ControlDecoration nameDecoration;
	private Text targetDesc;
	private LocationSelectComposite<SpatialPlanTargetPoint> locationSelect;
	
	public SpatialPlanTargetPropertyPage(TargetPropertyPage targetPropertyPage) {
		this.parentWindow = targetPropertyPage;
	}

	@Override
	public Composite createComponent(Composite parent, int style) {
		ScrolledComposite scrollCmp = new ScrolledComposite(parent, SWT.V_SCROLL);
		
		Composite main = new Composite(scrollCmp, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        main.setLayoutData(layoutData);

		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		Label nameLabel = new Label(center, SWT.NONE);
		nameLabel.setText("Target Name:");
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
				applyCurrentState();
			}
		});

		nameDecoration = new ControlDecoration(targetName, SWT.LEFT);
		nameDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		nameDecoration.setShowHover(true);
		nameDecoration.setDescriptionText("Target Name is required.");
		
		Label descrLabel = new Label(center, SWT.NONE);
		descrLabel.setText("Target Description:");
		descrLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		targetDesc= new Text(center, SWT.BORDER  | SWT.WRAP | SWT.V_SCROLL);
		targetDesc.setTextLimit(AdministrativePlanTarget.MAX_DESC_LENGTH);
		targetDesc.setLayoutData(createGridDataWithIndent());
		((GridData)targetDesc.getLayoutData()).widthHint = 100;
		((GridData)targetDesc.getLayoutData()).grabExcessVerticalSpace = true;

        //location selection
        locationSelect = new LocationSelectComposite<SpatialPlanTargetPoint>(main, SWT.NONE) {
			@Override
			protected ISmartPoint createNewPoint() {
				return new SpatialPlanTargetPoint();
			}
        };
        locationSelect.addLocationPointsChangeListener(this);
        locationSelect.getDecoration().setDescriptionText("At least one point is required.");
        locationSelect.getDecoration().show();

        scrollCmp.setContent(main);
		scrollCmp.setExpandVertical(true);
		scrollCmp.setExpandHorizontal(true);
		scrollCmp.setMinHeight(CONTENT_MIN_HEIGHT);
		return scrollCmp;
	}

	private GridData createGridDataWithIndent(){
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 8;
		return gd;
	}
	
	@Override
	public String getPageName() {
		return "Spatial";
	}
	
	@Override
	public PlanTarget createTarget() {
		return new SpatialPlanTarget();
	}

	@Override
	public void initPage(PlanTarget pt) {
		SpatialPlanTarget ptSpatial = (SpatialPlanTarget) pt;
		targetName.setText(ptSpatial.getName());
		targetDesc.setText(ptSpatial.getDescription());
		locationSelect.setPoints(ptSpatial.getPoints());
	}
	
	@Override
	public void updateTarget(PlanTarget pt) {
		SpatialPlanTarget ptSpatial = (SpatialPlanTarget) pt;
		ptSpatial.setName(targetName.getText());
		ptSpatial.setDescription(targetDesc.getText());

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

	private void applyCurrentState() {
		boolean isValid = validate();
		parentWindow.enableOK(isValid);
	}

	private boolean isTargetNameValid() {
    	return targetName != null && targetName.getText() != null && !targetName.getText().isEmpty();
	}

	private boolean isLocationPointsValid() {
    	return !locationSelect.getPoints().isEmpty();
	}
	
	@Override
	public boolean validate() {
		return isTargetNameValid() && isLocationPointsValid();
	}

	@Override
	public void locationPointsChanged() {
		if (isLocationPointsValid()) {
			locationSelect.getDecoration().hide();
		} else {
			locationSelect.getDecoration().show();
		}
		applyCurrentState();
	}

}
