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
package org.wcs.smart.dataentry.dialog.composite;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.GeometrySource;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Info composite for {@link CmAttribute} of text type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class GeometryAttributeInfoComposite extends CmAttributeInfoComposite {

	private CheckboxTableViewer chDataCollectionOp;
	private Text txtGpsAutoSeconds;
	private Composite compAutoSettings; 
	private ControlDecoration cdAutoSec;
	private ControlDecoration cdCollectOp;

	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public GeometryAttributeInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, model, session);
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite#createTypeSpecificControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);

		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.GeometryAttributeInfoComposite_CollectionOptions);
		label.setToolTipText(Messages.GeometryAttributeInfoComposite_DataCollectionOptions);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
				
		chDataCollectionOp = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		chDataCollectionOp.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Attribute.GeometrySource)element).getLabel(Locale.getDefault());
			}
		});
		chDataCollectionOp.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(chDataCollectionOp));
		chDataCollectionOp.setContentProvider(ArrayContentProvider.getInstance());
		chDataCollectionOp.setInput(new Attribute.GeometrySource[] {Attribute.GeometrySource.MANUAL_DRAW, Attribute.GeometrySource.GPS_AUTO, Attribute.GeometrySource.GPS_MANUAL});
		chDataCollectionOp.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		chDataCollectionOp.setAllChecked(true);
		
		cdCollectOp = createControlDecoration(chDataCollectionOp.getControl());
		
		chDataCollectionOp.addCheckStateListener(e->{
			updateAutoSettings();
			validateCollection(false);
		});
		chDataCollectionOp.getControl().addListener(SWT.FocusOut, e->validateCollection(true));
		
		new Label(container, SWT.NONE);
		
		
		final boolean[] autoChange = {false}; //indicate if text was changed by user or by calling setter

		compAutoSettings = new Composite(container, SWT.NONE);
		compAutoSettings.setLayout(new GridLayout(3, false));
		((GridLayout)compAutoSettings.getLayout()).marginWidth = 0;
		((GridLayout)compAutoSettings.getLayout()).marginHeight = 0;
		compAutoSettings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(compAutoSettings, SWT.NONE);
		l.setText(MessageFormat.format(Messages.GeometryAttributeInfoComposite_GpsPointRecordTime, Attribute.GeometrySource.GPS_AUTO.getLabel(Locale.getDefault())));
		
		txtGpsAutoSeconds = new Text(compAutoSettings, SWT.BORDER);
		txtGpsAutoSeconds.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		((GridData)txtGpsAutoSeconds.getLayoutData()).widthHint = 50;
		txtGpsAutoSeconds.setText(""); //$NON-NLS-1$
		txtGpsAutoSeconds.addListener(SWT.Modify, e->{
			if (autoChange[0]) return;
			validateSeconds(false);
		});
		txtGpsAutoSeconds.addListener(SWT.FocusOut, e->{
			if (autoChange[0]) return;
			validateSeconds(true);
		});
		
		cdAutoSec = createControlDecoration(txtGpsAutoSeconds);

		l = new Label(compAutoSettings, SWT.NONE);
		l.setText(Messages.GeometryAttributeInfoComposite_secondunits);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getOrCreateSecondsOp();
				if (option == null) {
					txtGpsAutoSeconds.setText(""); //$NON-NLS-1$
				}else {
					autoChange[0] = true;
					txtGpsAutoSeconds.setText( String.valueOf(option.getDoubleValue().intValue()) );
					autoChange[0] = false;
				}
				
				option = getOrCreateCollectionOp();
				if (option == null) {
					chDataCollectionOp.setAllChecked(true);
				}else {
					chDataCollectionOp.setAllChecked(false);
					for (Object x : CmAttributeOptionFactory.parseGeometryCollectionOption(option)) {
						chDataCollectionOp.setChecked(x, true);
					}
				}
				validateSeconds(false);
				validateCollection(false);
				updateAutoSettings();		
			}
		});
		
	}
	
	private void validateCollection(boolean display) {
		
		cdCollectOp.hide();
		if (chDataCollectionOp.getCheckedElements().length == 0) {
			
			String message = Messages.GeometryAttributeInfoComposite_OneOptionRequired;
			if (display) {
				MessageDialog.openError(getShell(), DialogConstants.ERROR_STRING, message);
				//revert to previous selection
				CmAttributeOption op = getOrCreateCollectionOp();
				chDataCollectionOp.setAllChecked(false);
				for (Object x : CmAttributeOptionFactory.parseGeometryCollectionOption(op)) {
					chDataCollectionOp.setChecked(x, true);
				}
				updateAutoSettings();
			}else {
				cdCollectOp.setDescriptionText(message);
				cdCollectOp.show();
			}
			return;
		}
		
		if (getSourceObject() != null) {
			CmAttributeOption op = getOrCreateCollectionOp();
			if (op == null) return;
			List<Attribute.GeometrySource> selected = new ArrayList<>();
			for (Object x : chDataCollectionOp.getCheckedElements()) {
				selected.add((GeometrySource) x);
			}
			op.setStringValue(CmAttributeOptionFactory.encodeGeometryCollectionOption(selected));
			
			fireModelChanged();
		}
	}
	
	private void validateSeconds(boolean display) {
		
		cdAutoSec.hide();
		try {
			Integer sec = Integer.parseInt(txtGpsAutoSeconds.getText());
			if (sec <= 0) {
				throw new Exception();
			}
			
			Double value = sec.doubleValue();
			
			if (getSourceObject() != null) {
				CmAttributeOption op = getOrCreateSecondsOp();
				if (op == null) return;
				op.setDoubleValue(value);
			}
			
		}catch (Exception ex) {
			String message = Messages.GeometryAttributeInfoComposite_InvalidSeconds;
			if (display) {
				MessageDialog.openError(getShell(), DialogConstants.ERROR_STRING, message);
				txtGpsAutoSeconds.setText( String.valueOf(getOrCreateSecondsOp().getDoubleValue().intValue()) );
			}else {
				cdAutoSec.setDescriptionText(message);
				cdAutoSec.show();
			}
		}
		fireModelChanged();
	}
	
	
	private void updateAutoSettings() {
		Object[] selected = chDataCollectionOp.getCheckedElements();
		boolean isAuto = false;
		for (Object x : selected) {
			if (x == Attribute.GeometrySource.GPS_AUTO) {
				isAuto = true;
			}
		}
		for (Control kid : compAutoSettings.getChildren()) {
			kid.setEnabled(isAuto);
		}
	}
	

	private CmAttributeOption getOrCreateSecondsOp() {
		if (getSourceObject() == null) return null;
		CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_GEOM_COLLECTION_AUTO_GPS_SEC);
		if (op == null){
			op = CmAttributeOptionFactory.createGeomCollectionGpsAutoSecOption(getSourceObject());
			getSourceObject().getCmAttributeOptions().put(op.getOptionId(), op);
		}
		return op;
	}
	
	private CmAttributeOption getOrCreateCollectionOp() {
		if (getSourceObject() == null) return null;
		CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_GEOM_COLLECTION_OP);
		if (op == null){
			op = CmAttributeOptionFactory.createGeomCollectionOption(getSourceObject());
			getSourceObject().getCmAttributeOptions().put(op.getOptionId(), op);
		}
		return op;
	}
	
}
