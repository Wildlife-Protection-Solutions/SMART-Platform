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
package org.wcs.smart.udig.style;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.geotools.filter.text.ecql.ECQL;
import org.locationtech.udig.style.advanced.common.FiltersComposite;
import org.locationtech.udig.style.advanced.common.IStyleChangesListener;
import org.locationtech.udig.style.advanced.common.styleattributeclasses.PointSymbolizerWrapper;
import org.locationtech.udig.style.advanced.common.styleattributeclasses.RuleWrapper;
import org.locationtech.udig.style.advanced.points.PointPropertiesEditor;
import org.locationtech.udig.style.advanced.points.widgets.IPointSymbolizerComposite;
import org.locationtech.udig.style.advanced.points.widgets.PointGeneralParametersComposite;
import org.opengis.filter.Filter;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.properties.IconSelectionDialog;
import org.wcs.smart.ui.internal.ca.properties.IconSelectionDialog.Type;
import org.wcs.smart.util.SmartUtils;

/**
 * Addition to the uDig point styling interface to support
 * SMART Icon sets.
 * 
 * @author Emily
 *
 */
public class SmartIconSymbolizer implements IPointSymbolizerComposite, IStyleChangesListener {

	private static final String IMAGEKEY = "IMAGE"; //$NON-NLS-1$
	private static final int PREVIEWSIZE = 64;
	
	private Composite composite;
	
	private FiltersComposite fontFiltersComposite ;
	private PointGeneralParametersComposite generalParametersComposite;
	
	private IconFile selectedFile = null;
	private RuleWrapper ruleWrapper;
	private PointPropertiesEditor editor;
	
	private Canvas imagePreview;
	
	public SmartIconSymbolizer() {
	}

	@Override
	public String getName() {
		return Messages.SmartIconSymbolizer_SmartIconPointSymbolizer;
	}

	@Override
	public Composite getComposite() {
		return composite;
	}

	@Override
	public boolean canStyle(PointSymbolizerWrapper pointWrapper) {
		try {
			String graphics = pointWrapper.getExternalGraphicPath();
			if (graphics.contains("smart") || graphics.startsWith(".\\data\\filestore")) { //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}
		} catch (MalformedURLException e) {
			SmartPlugIn.log(e.getMessage(),e);
		}
		return false;
	}	
	
	private void updateImagePreview() {
		
		Image img = (Image) imagePreview.getData(IMAGEKEY);
		
		if ((selectedFile == null || selectedFile.getFilename().isEmpty())) {
			if (img != null) img.dispose();
			imagePreview.setData(IMAGEKEY, null);
			return;
		}
		
		if (img != null) img.dispose();
		String fname = selectedFile.getFilename();
		try {
			
			if (selectedFile.getUuid() != null && !selectedFile.isSystemIcon()) {
				fname = "file:" + selectedFile.getAttachmentFile().toString(); //$NON-NLS-1$
			}else if (selectedFile.getUuid() == null && !selectedFile.isSystemIcon()) {
				fname = "file:" + fname; //$NON-NLS-1$
			}
			img = SmartUtils.getImage(new URL(fname), PREVIEWSIZE);
			imagePreview.setData(IMAGEKEY, img);
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		
		
		PointSymbolizerWrapper pointSymbolizerWrapper = SmartIconSymbolizer.this.ruleWrapper.getGeometrySymbolizersWrapper().adapt( PointSymbolizerWrapper.class);
        try {
        	pointSymbolizerWrapper.setExternalGraphicPath(new URL(fname));
            editor.refreshTreeViewer(SmartIconSymbolizer.this.ruleWrapper);
            editor.refreshPreviewCanvasOnStyle();
        }catch (Exception ex) {
        	SmartPlugIn.log(ex.getMessage(),ex);
        }
		imagePreview.redraw();
	}
	
	
	@Override
	public void createComposite(Composite parent, PointPropertiesEditor editor, RuleWrapper ruleWrapper, String[] numericAttributesArrays) {
		this.ruleWrapper = ruleWrapper;
		this.editor = editor;
		
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		((GridLayout)composite.getLayout()).marginHeight = 0;
		((GridLayout)composite.getLayout()).marginWidth = 0;
		
		
		
		if (SmartDB.isMultipleAnalysis()) {
			Label l = new Label(composite, SWT.NONE);
			l.setText(Messages.SmartIconSymbolizer_NotSupported);
			return;
		}
		
		Composite iconbit = new Composite(composite, SWT.NONE);
		iconbit.setLayout(new GridLayout(2, false));
		((GridLayout)iconbit.getLayout()).marginHeight = 0;
		((GridLayout)iconbit.getLayout()).marginWidth = 0;
		iconbit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		imagePreview = new Canvas(iconbit, SWT.BORDER);
		imagePreview.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)imagePreview.getLayoutData()).widthHint = PREVIEWSIZE;
		((GridData)imagePreview.getLayoutData()).heightHint = PREVIEWSIZE;
		imagePreview.addListener(SWT.Paint, e->{
			Image img = (Image) imagePreview.getData(IMAGEKEY);
			if (img == null) return;
			e.gc.drawImage(img, 0, 0);
		});
		
		
		Button btnSelect = new Button(iconbit, SWT.PUSH);
		btnSelect.setText(Messages.SmartIconSymbolizer_SelectIconButton);
		btnSelect.setBackground(iconbit.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnSelect.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		btnSelect.addListener(SWT.Selection, e->{
			IconSelectionDialog dialog = new IconSelectionDialog(composite.getShell(), Type.SINGLE_SELECT);
			if (dialog.open() != Window.OK) return;
			
			IconFile iconfile = dialog.getSelectedIconFile();
			if (iconfile == null) return;
			selectedFile = iconfile;
			updateImagePreview();
	        
		});
		
		
		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		generalParametersComposite = new PointGeneralParametersComposite(tabFolder, numericAttributesArrays);
	    generalParametersComposite.init(ruleWrapper);
		generalParametersComposite.addListener(this);
		Composite generalParametersInternalComposite = generalParametersComposite.getComposite();

		TabItem tabItem2 = new TabItem(tabFolder, SWT.NULL);
		tabItem2.setText(Messages.SmartIconSymbolizer_GeneralSection);
		tabItem2.setControl(generalParametersInternalComposite);

		// Filter GROUP
		fontFiltersComposite = new FiltersComposite(tabFolder);
		fontFiltersComposite.init(ruleWrapper);
		fontFiltersComposite.addListener(this);
		Composite filtersInternalComposite = fontFiltersComposite.getComposite();

		TabItem tabItem6 = new TabItem(tabFolder, SWT.NULL);
		tabItem6.setText(Messages.SmartIconSymbolizer_FilterSection);
		tabItem6.setControl(filtersInternalComposite);
	}


	@Override
	public void update(RuleWrapper ruleWrapper) {
		this.ruleWrapper = ruleWrapper;
		
		if (SmartDB.isMultipleAnalysis()) return;
		
		generalParametersComposite.update(ruleWrapper);
		fontFiltersComposite.update(ruleWrapper);
		
		PointSymbolizerWrapper pointSymbolizerWrapper = ruleWrapper.getGeometrySymbolizersWrapper().adapt(PointSymbolizerWrapper.class);
		
		try {
			IconFile temp = new IconFile();
			
			temp.setFilename(pointSymbolizerWrapper.getExternalGraphicPath());
			selectedFile = temp;
			updateImagePreview();
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}
	}

	
	
	public void onStyleChanged( Object source, String[] values, boolean fromField, STYLEEVENTTYPE styleEventType ) {
		if (SmartDB.isMultipleAnalysis()) return;
		
		String value = values[0];

        PointSymbolizerWrapper pointSymbolizerWrapper = ruleWrapper.getGeometrySymbolizersWrapper().adapt(PointSymbolizerWrapper.class);

        switch( styleEventType ) {
        // GENERAL PARAMETERS
        case TITLE:
            ruleWrapper.setTitle(value);
            break;
        case NAME:
            ruleWrapper.setName(value);
            break;
        case SIZE:
            pointSymbolizerWrapper.setSize(value, fromField);
            break;
        case ROTATION:
            pointSymbolizerWrapper.setRotation(value, fromField);
            break;
        case OFFSET:
            pointSymbolizerWrapper.setOffset(value);
            break;
        case MAXSCALE:
            ruleWrapper.setMaxScale(value);
            break;
        case MINSCALE:
            ruleWrapper.setMinScale(value);
            break;
        case FILTER: {
            if (value.length() > 0) {
                try {
                    Filter filter = ECQL.toFilter(value);
                    ruleWrapper.getRule().setFilter(filter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else {
            	ruleWrapper.getRule().setFilter(null);
            }
            break;
        }
        default:
            break;
        }

        editor.refreshTreeViewer(ruleWrapper);
        editor.refreshPreviewCanvasOnStyle();

    }
	
}
