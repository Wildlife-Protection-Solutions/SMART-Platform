/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.inout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.inout.AssetLocationCsvImporter;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Wizard page for mapping csv file fields to location attributes.
 * 
 * @author Emily
 *
 */
public class LocationMappingPage extends WizardPage{
	
	private static final String ATTRIBUTE2 = "ATTRIBUTE"; //$NON-NLS-1$
	private ComboViewer cmbLocationId;
	private ComboViewer cmbStationId;
	private ComboViewer cmbPositionX;
	private ComboViewer cmbPositionY;
	
	private List<ComboViewer> attributeMappings;
	
	private Composite fields;
	
	@Inject
	IEclipseContext context;
	
	private LabelProvider lblProvider = new LabelProvider() {
		public String getText(Object element) {
			if (element instanceof AssetType) return ((AssetType)element).getName();
			if (element instanceof HeaderIndex) return ((HeaderIndex) element).header;
			return super.getText(element);
		}
	};
	
	protected LocationMappingPage() {
		super("LOCATION_MAPPING"); //$NON-NLS-1$
	}
	
    @Override
	public IWizardPage getNextPage() {
        return null;
    }

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.LocationMappingPage_LocationIdlabel);
		
		cmbLocationId = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbLocationId.setContentProvider(ArrayContentProvider.getInstance());
		cmbLocationId.setLabelProvider(lblProvider);
		cmbLocationId.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbLocationId.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.LocationMappingPage_StationIdLabel);
		
		cmbStationId = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbStationId.setContentProvider(ArrayContentProvider.getInstance());
		cmbStationId.setLabelProvider(lblProvider);
		cmbStationId.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbStationId.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.LocationMappingPage_PositionLabel);
		
		Composite locComp = new Composite(main, SWT.NONE);
		locComp.setLayout(new GridLayout(2, true));
		locComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)locComp.getLayout()).marginWidth = 0;
		((GridLayout)locComp.getLayout()).marginHeight= 0;
		
		Composite xComp = new Composite(locComp, SWT.NONE);
		xComp.setLayout(new GridLayout(2, false));
		xComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)xComp.getLayout()).marginWidth = 0;
		((GridLayout)xComp.getLayout()).marginHeight= 0;
		
		l = new Label(xComp, SWT.NONE);
		l.setText(Messages.LocationMappingPage_XLabel);

		cmbPositionX = new ComboViewer(xComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbPositionX.setContentProvider(ArrayContentProvider.getInstance());
		cmbPositionX.setLabelProvider(lblProvider);
		cmbPositionX.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbPositionX.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		Composite yComp = new Composite(locComp, SWT.NONE);
		yComp.setLayout(new GridLayout(2, false));
		yComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)yComp.getLayout()).marginWidth = 0;
		((GridLayout)yComp.getLayout()).marginHeight= 0;
		l = new Label(yComp, SWT.NONE);
		l.setText(Messages.LocationMappingPage_YLabel);
		
		cmbPositionY = new ComboViewer(yComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbPositionY.setContentProvider(ArrayContentProvider.getInstance());
		cmbPositionY.setLabelProvider(lblProvider);
		cmbPositionY.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbPositionY.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		fields = new Composite(main, SWT.NONE);
		fields.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		fields.setLayout(new GridLayout(2, false));
		((GridLayout)fields.getLayout()).marginWidth = 0;
		((GridLayout)fields.getLayout()).marginHeight= 0;
		setControl(main);
	}

	public void updateMapping(Path fileName, char delimiter) {
		cmbLocationId.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbStationId.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbPositionX.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbPositionY.setInput(new String[] {DialogConstants.LOADING_TEXT});

		for (Control c : fields.getChildren()) c.dispose();
		
		if (fileName == null) return;
		
		List<HeaderIndex> headers = null;
		try(CSVReader csvreader = new CSVReader(Files.newBufferedReader(fileName), delimiter)){
			String[] fileHeaders = csvreader.readNext();
			headers = new ArrayList<>();
			for (int i = 0; i < fileHeaders.length; i ++) {
				headers.add(new HeaderIndex(fileHeaders[i],i));
			}
		} catch (IOException e) {
			AssetPlugIn.log(e.getMessage(), e);
		}
		
		if (headers == null) {
			cmbLocationId.setInput(new String[] {});
			cmbStationId.setInput(new String[] {});
			cmbPositionX.setInput(new String[] {});
			cmbPositionY.setInput(new String[] {});
			
			setErrorMessage(MessageFormat.format(Messages.LocationMappingPage_ReadError, fileName));
			return;
		}
		
		setErrorMessage(null);
		
		List<AssetAttribute> attributes = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			List<AssetStationLocationAttribute> stnAttributes = QueryFactory.buildQuery(session, AssetStationLocationAttribute.class, new Object[] {"attribute.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			for (AssetStationLocationAttribute a : stnAttributes) {
				attributes.add(a.getAttribute());
				a.getAttribute().getName();
				a.getAttribute().getAttributeList().forEach( l -> {l.getNames().size();l.getKeyId();});
				
			}
		}
		
		List<AssetAttribute> sortedattributes = new ArrayList<>();
		sortedattributes.addAll(attributes);
		sortedattributes.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		
		cmbStationId.setInput(headers);
		cmbLocationId.setInput(headers);
		cmbPositionX.setInput(headers);
		cmbPositionY.setInput(headers);
		
		
		
		attributeMappings = new ArrayList<>();
		
		List<HeaderIndex> options = new ArrayList<>();
		options.add(new HeaderIndex("", -1)); //$NON-NLS-1$
		options.addAll(headers);
		
		ScrolledComposite scroll = new ScrolledComposite(fields, SWT.V_SCROLL);
		Composite content = new Composite(scroll, SWT.NONE);
		scroll.setContent(content);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		
		content.setLayout(new GridLayout(2, false));
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)content.getLayout()).marginWidth = 0;
		((GridLayout)content.getLayout()).marginHeight= 0;
		for (AssetAttribute attribute : sortedattributes) {
			Label l = new Label(content, SWT.NONE);
			l.setText(attribute.getName());
			
			ComboViewer cmbViewer = new ComboViewer(content, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbViewer.setLabelProvider(lblProvider);
			cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
			cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbViewer.setInput(options);
			
			for (HeaderIndex x : headers) {
				if (x.header.trim().toLowerCase().equals(attribute.getName().trim().toLowerCase())) {
					cmbViewer.setSelection(new StructuredSelection(x));
				}
			}
			attributeMappings.add(cmbViewer);
			cmbViewer.setData(ATTRIBUTE2, attribute);
		}
		fields.layout(true);
		fields.getParent().layout(true);
		scroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		
		validate();
		
		setTitle(Messages.LocationMappingPage_Title);
		setMessage(Messages.LocationMappingPage_Message);
	}

	public Integer getStationIdMapping() {
		return ((HeaderIndex) cmbStationId.getStructuredSelection().getFirstElement()).index;
	}
	
	public Integer getLocationIdMapping() {
		return ((HeaderIndex) cmbLocationId.getStructuredSelection().getFirstElement()).index;
	}
	
	public Integer getLocationXMapping() {
		return ((HeaderIndex) cmbPositionX.getStructuredSelection().getFirstElement()).index;
	}
	public Integer getLocationYMapping() {
		return ((HeaderIndex) cmbPositionY.getStructuredSelection().getFirstElement()).index;
	}

	
	public HashMap<AssetAttribute, Integer> getAttributeMappings(){
		HashMap<AssetAttribute, Integer> mappings = new HashMap<>();
		for (ComboViewer cmb : attributeMappings) {
			HeaderIndex x = (HeaderIndex)cmb.getStructuredSelection().getFirstElement();
			if (x != null && x.index >= 0) {
				mappings.put((AssetAttribute)cmb.getData(ATTRIBUTE2), x.index);
			}
		}
		return mappings;
	}
	
	private void validate() {
		String error = null;
		
		Object x = cmbLocationId.getStructuredSelection().getFirstElement();
		if (x == null) error = Messages.LocationMappingPage_LocationRequired;
		
		x = cmbStationId.getStructuredSelection().getFirstElement();
		if (x == null) error = Messages.LocationMappingPage_StationRequired;
		
		
		x = cmbPositionX.getStructuredSelection().getFirstElement();
		if (x == null) error = Messages.LocationMappingPage_XRequired;
		
		x = cmbPositionY.getStructuredSelection().getFirstElement();
		if (x == null) error = Messages.LocationMappingPage_YRequired;
		
		setErrorMessage(error);
	}

	public boolean doFinish() {
		Path file = ((AssetDataImportWizard)getWizard()).filePage.getFile();
		char delimiter = ((AssetDataImportWizard)getWizard()).filePage.getDelimiter();
		boolean skipFirst = ((AssetDataImportWizard)getWizard()).filePage.skipFirst();
		String dateFormat = ((AssetDataImportWizard)getWizard()).filePage.getDateTimeFormat();
		Projection proj = ((AssetDataImportWizard)getWizard()).filePage.getProjection();
		
		if (!Files.exists(file)) {
			MessageDialog.openError(getContainer().getShell(), Messages.LocationMappingPage_ErrorMessage, MessageFormat.format(Messages.LocationMappingPage_FileNotFound, file.toString()));
			return false;
		}
		
		AssetLocationCsvImporter importer = new AssetLocationCsvImporter(file, delimiter,skipFirst, getLocationIdMapping(), getStationIdMapping(), getLocationXMapping(), getLocationYMapping(), getAttributeMappings(), dateFormat, proj);
		ContextInjectionFactory.inject(importer, context);
		try {
			return importer.processFile();
		}catch (Exception ex) {
			AssetPlugIn.displayLog(Messages.LocationMappingPage_ImportError +ex.getMessage(),  ex);
			return false;
		}
		
	}
	
	private class HeaderIndex{
		String header;
		int index;
		
		public HeaderIndex(String header, int index) {
			this.header = header;
			this.index = index;
		}
	}
}
