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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.wcs.smart.asset.data.inout.AssetCsvImporter;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Wizard page for mapping csv file fields to asset attributes.
 * 
 * @author Emily
 *
 */
public class AssetMappingPage extends WizardPage{

	private static final String SINGLE_TYPE_OP = "-- SINGLE TYPE --";
	private ComboViewer cmbAssetType;
	private ComboViewer cmbAssetId;
	
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
	
	protected AssetMappingPage() {
		super("ASSET_MAPPING");
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Asset Type*:");
		
		cmbAssetType = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbAssetType.setContentProvider(ArrayContentProvider.getInstance());
		cmbAssetType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbAssetType.setLabelProvider(lblProvider);
		cmbAssetType.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		l = new Label(main, SWT.NONE);
		l.setText("Asset ID*:");
		
		cmbAssetId = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbAssetId.setContentProvider(ArrayContentProvider.getInstance());
		cmbAssetId.setLabelProvider(lblProvider);
		cmbAssetId.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbAssetId.addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		
		fields = new Composite(main, SWT.NONE);
		fields.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		fields.setLayout(new GridLayout(2, false));
		((GridLayout)fields.getLayout()).marginWidth = 0;
		((GridLayout)fields.getLayout()).marginHeight = 0;
		setControl(main);
	}

	public void updateMapping(Path fileName, char delimiter) {
		cmbAssetType.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbAssetId.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
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
			cmbAssetType.setInput(new String[] {});
			cmbAssetId.setInput(new String[] {});
			setErrorMessage(MessageFormat.format("Unable to read file: {0}", fileName));
			return;
		}
		
		setErrorMessage(null);
		
		List<AssetType> types = new ArrayList<>();
		Set<AssetAttribute> attributes = new HashSet<>();
		try(Session session = HibernateManager.openSession()){
			types = QueryFactory.buildQuery(session, AssetType.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list();
			for (AssetType type : types) {
				type.getAssetAttributes().forEach(a->attributes.add(a.getAttribute()));
			}
		}
		
		List<AssetAttribute> sortedattributes = new ArrayList<>();
		sortedattributes.addAll(attributes);
		sortedattributes.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		
		cmbAssetId.setInput(headers);
		for (HeaderIndex x : headers) {
			if (x.header.trim().toLowerCase().equals("asset id".trim().toLowerCase())) {
				cmbAssetId.setSelection(new StructuredSelection(x));
			}
		}
		
		List<Object> typeOptions = new ArrayList<>();
		typeOptions.addAll(headers);
		typeOptions.add(SINGLE_TYPE_OP);
		typeOptions.addAll(types);
		cmbAssetType.setInput(typeOptions);
		
		attributeMappings = new ArrayList<>();
		
		List<HeaderIndex> options = new ArrayList<>();
		options.add(new HeaderIndex("", -1));
		options.addAll(headers);
		
		ScrolledComposite scroll = new ScrolledComposite(fields, SWT.V_SCROLL);
		Composite content = new Composite(scroll, SWT.NONE);
		scroll.setContent(content);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		
		content.setLayout(new GridLayout(2, false));
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)content.getLayout()).marginWidth = 0;
		((GridLayout)content.getLayout()).marginHeight = 0;
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
			cmbViewer.setData("ATTRIBUTE", attribute);
		}
		fields.layout(true);
		fields.getParent().layout(true);
		scroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		
		validate();
		
		setTitle("Import Assets From CSV");
		setMessage("Map asset attributes to csv file");
	}

	public Integer getAssetIdMapping() {
		return ((HeaderIndex) cmbAssetId.getStructuredSelection().getFirstElement()).index;
	}
	
	public Object getAssetTypeMapping() {
		Object x = cmbAssetType.getStructuredSelection().getFirstElement();
		if (x instanceof HeaderIndex) return ((HeaderIndex) x).index;
		if (x instanceof AssetType) return x;
		return null;
	}
	
	public HashMap<AssetAttribute, Integer> getAttributeMappings(){
		HashMap<AssetAttribute, Integer> mappings = new HashMap<>();
		for (ComboViewer cmb : attributeMappings) {
			HeaderIndex x = (HeaderIndex)cmb.getStructuredSelection().getFirstElement();
			if (x != null && x.index >= 0) {
				mappings.put((AssetAttribute)cmb.getData("ATTRIBUTE"), x.index);
			}
		}
		return mappings;
	}
	
	private void validate() {
		String error = null;
		
		Object x = cmbAssetId.getStructuredSelection().getFirstElement();
		if (x == null) error = "A mapping for asset id field is required.";
		
		x = cmbAssetType.getStructuredSelection().getFirstElement();
		if (x == null || x == SINGLE_TYPE_OP) error = "A mapping for asset type field is required.";
		
		setErrorMessage(error);
	}

	public boolean doFinish() {
		Path file = ((AssetDataImportWizard)getWizard()).filePage.getFile();
		char delimiter = ((AssetDataImportWizard)getWizard()).filePage.getDelimiter();
		boolean skipFirst = ((AssetDataImportWizard)getWizard()).filePage.skipFirst();
		String dateFormat = ((AssetDataImportWizard)getWizard()).filePage.getDateTimeFormat();
		
		if (!Files.exists(file)) {
			MessageDialog.openError(getContainer().getShell(), "Error", MessageFormat.format("This file {0} does not exist.", file.toString()));
			return false;
		}
		
		AssetCsvImporter importer = new AssetCsvImporter(file, delimiter,skipFirst, getAssetIdMapping(), getAssetTypeMapping(), getAttributeMappings(), dateFormat);
		ContextInjectionFactory.inject(importer, context);
		try {
			return importer.processFile();
		}catch (Exception ex) {
			AssetPlugIn.displayLog("Unable to import asset data from file: " +ex.getMessage(),  ex);
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
