/*
 * Copyright (C) 2020 Wildlife Conservation Society
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.ui.AssetTypeLabelProvider;
import org.wcs.smart.asset.ui.AttributeLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Wizard page for collection model export options
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class ModelExportOptionsWizardPage extends WizardPage{
	
	private Font boldFont;

	private CheckboxTableViewer lstAttributes;
	private CheckboxTableViewer lstAssetTypes;
	private CheckboxTableViewer lstMetadataMappings;
	
	private Button btnIncludeModuleSettings, btnIncludeStationAttributes, btnIncludeLocationAttributes;
	
	
	
	protected ModelExportOptionsWizardPage() {
		super("MODEL_OPTIONS"); //$NON-NLS-1$
	}

	@Override
	public IWizardPage getNextPage() {
		return null;
	}
	
    @Override
	public boolean isPageComplete() {
       return true;
    }
    
    public List<AssetAttribute> getSelectedAttributes(){
    	return getSelected(AssetAttribute.class, lstAttributes);
    }
    
    public List<AssetType> getSelectedAssetTypes(){
    	return getSelected(AssetType.class, lstAssetTypes);
    }
    
    public List<AssetMetadataMapping> getSelectedMetadata(){
    	return getSelected(AssetMetadataMapping.class, lstMetadataMappings);
    }
    
    public boolean getIncludeModuleSettings() {
    	return this.btnIncludeModuleSettings.getSelection();
    }
    
    public boolean getIncludeStationAttributes() {
    	return this.btnIncludeStationAttributes.getSelection();
    }
    public boolean getIncludeLocationAttributes() {
    	return this.btnIncludeLocationAttributes.getSelection();
    }
	@SuppressWarnings("unchecked")
	private <T> List<T> getSelected(Class<T> c, CheckboxTableViewer viewer) {
		List<T> items = new ArrayList<>();
		for (Object x : viewer.getCheckedElements())
			if (x.getClass().equals(c))
				items.add((T) x);
		return items;
	}

	
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		parent.addListener(SWT.Dispose, e -> boldFont.dispose());

		Composite itemsSection = new Composite(outer, SWT.NONE);
		itemsSection.setLayout(new GridLayout());
		itemsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// asset attributes
		Composite attributeSection = new Composite(itemsSection, SWT.NONE);
		attributeSection.setLayout(new GridLayout());
		((GridLayout) attributeSection.getLayout()).marginWidth = 0;
		((GridLayout) attributeSection.getLayout()).marginHeight = 0;
		attributeSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(attributeSection, Messages.ExportModelToXmlDialog_AttributesLabel);
		lstAttributes = createTableViewer(attributeSection, new AttributeLabelProvider(), null);
				
		// asset types
		Composite assetTypeSection = new Composite(itemsSection, SWT.NONE);
		assetTypeSection.setLayout(new GridLayout());
		((GridLayout) assetTypeSection.getLayout()).marginWidth = 0;
		((GridLayout) assetTypeSection.getLayout()).marginHeight = 0;
		assetTypeSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(assetTypeSection, Messages.ExportModelToXmlDialog_AssetTypesLabel);
		lstAssetTypes = createTableViewer(assetTypeSection, new AssetTypeLabelProvider(), null);

		// metadata mappings
		Composite metadataSection = new Composite(itemsSection, SWT.NONE);
		metadataSection.setLayout(new GridLayout());
		((GridLayout) metadataSection.getLayout()).marginWidth = 0;
		((GridLayout) metadataSection.getLayout()).marginHeight = 0;
		metadataSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(metadataSection, Messages.ExportModelToXmlDialog_MetadataMappingsLabel);
		lstMetadataMappings = createTableViewer(metadataSection, new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMetadataMapping) {
					AssetMetadataMapping mm = (AssetMetadataMapping)element;
					StringBuilder sb = new StringBuilder();
					sb.append(mm.getMetadataType().name());
					sb.append(" ("); //$NON-NLS-1$
					sb.append(mm.getMetadataKey());
					sb.append(" ) "); //$NON-NLS-1$
					if (mm.getMappedAssetProperty() != null) sb.append(mm.getMappedAssetProperty().name());
					if (mm.getMappedCategory() != null) {
						sb.append(mm.getMappedCategory().getName());
						sb.append(" "); //$NON-NLS-1$
					}
					if (mm.getMappedAttribute() != null) {
						sb.append(mm.getMappedAttribute().getName());
						sb.append(" "); //$NON-NLS-1$
					}
					if (mm.getMappedListItem() != null) {
						sb.append(mm.getMappedListItem().getName());
						sb.append(" "); //$NON-NLS-1$
					}
					if (mm.getMappedTreeNode() != null) {
						sb.append(mm.getMappedTreeNode().getName());
						sb.append(" "); //$NON-NLS-1$
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		}, null);

		//configuration section
		Composite configSection = new Composite(itemsSection, SWT.NONE);
		configSection.setLayout(new GridLayout());
		((GridLayout) configSection.getLayout()).marginWidth = 0;
		((GridLayout) configSection.getLayout()).marginHeight = 0;
		configSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(configSection, Messages.ExportModelToXmlDialog_ConfigurationLabel);
		
		btnIncludeModuleSettings= new Button(configSection, SWT.CHECK);
		btnIncludeModuleSettings.setText(Messages.ExportModelToXmlDialog_IncludeSettiongsOp);
		btnIncludeModuleSettings.setToolTipText(Messages.ExportModelToXmlDialog_settingstooltip);
		btnIncludeModuleSettings.setSelection(true);
		
		btnIncludeStationAttributes= new Button(configSection, SWT.CHECK);
		btnIncludeStationAttributes.setText(Messages.ExportModelToXmlDialog_IncludeStationsOption);
		btnIncludeStationAttributes.setToolTipText(Messages.ExportModelToXmlDialog_IncludeStationsTooltips);
		btnIncludeStationAttributes.setSelection(true);
		
		btnIncludeLocationAttributes= new Button(configSection, SWT.CHECK);
		btnIncludeLocationAttributes.setText(Messages.ExportModelToXmlDialog_IncludeLocations);
		btnIncludeLocationAttributes.setToolTipText(Messages.ExportModelToXmlDialog_IncludeLocationsTooltip);
		btnIncludeLocationAttributes.setSelection(true);
		
		// load data
		initializeLists.schedule();

		setTitle(Messages.ModelExportOptionsWizardPage_Title);
		setMessage(Messages.ModelExportOptionsWizardPage_Message);
		
		setControl(outer);
	}
	
	private CheckboxTableViewer createTableViewer(Composite parent, ILabelProvider lblProvider,
			ICheckStateListener listener) {

		Composite action = new Composite(parent, SWT.NONE);
		action.setLayout(new GridLayout(2, false));
		((GridLayout) action.getLayout()).marginWidth = 0;
		((GridLayout) action.getLayout()).marginHeight = 0;

		Link selectAll = new Link(action, SWT.NONE);
		selectAll.setText("<a>" + Messages.ExportModelToXmlDialog_AllOption + "</a>");  //$NON-NLS-1$ //$NON-NLS-2$

		Link selectNone = new Link(action, SWT.NONE);
		selectNone.setText("<a>" + Messages.ExportModelToXmlDialog_NoneOption + "</a>");  //$NON-NLS-1$ //$NON-NLS-2$
		selectNone.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData) viewer.getControl().getLayoutData()).heightHint = 50;
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(lblProvider);
		viewer.setInput(new String[] { DialogConstants.LOADING_TEXT });

		selectAll.addListener(SWT.Selection, e -> {
			viewer.setAllChecked(true);
			if (listener != null)
				listener.checkStateChanged(null);
		});
		selectNone.addListener(SWT.Selection, e -> {
			viewer.setAllChecked(false);
			if (listener != null)
				listener.checkStateChanged(null);
		});

		return viewer;
	}

	private void createHeader(Composite parent, String text) {
		Composite header = new Composite(parent, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setLayout(new GridLayout(2, false));
		((GridLayout) header.getLayout()).marginWidth = 0;
		((GridLayout) header.getLayout()).marginHeight = 0;

		Label l = new Label(header, SWT.NONE);
		l.setText(text);
		l.setFont(boldFont);

		Label sep = new Label(header, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}
	
	private Job initializeLists = new Job(Messages.ExportModelToXmlDialog_initJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<AssetAttribute> attributes;
			List<AssetType> assetTypes;
			List<AssetMetadataMapping> mappings;
		
			try (Session s = HibernateManager.openSession()) {
				attributes = QueryFactory
						.buildQuery(s, AssetAttribute.class, "conservationArea", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list();
				attributes.forEach(e -> e.getName());
				
				assetTypes = QueryFactory
						.buildQuery(s, AssetType.class, "conservationArea", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list();
				assetTypes.forEach(e -> e.getName());

				mappings = QueryFactory
						.buildQuery(s, AssetMetadataMapping.class, "conservationArea", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list();
				mappings.forEach(e -> {
					e.getMetadataKey();
					if (e.getMappedAttribute() != null) e.getMappedAttribute().getName();
					if (e.getMappedCategory() != null) e.getMappedCategory().getName();
					if (e.getMappedListItem() != null) e.getMappedListItem().getName();
					if (e.getMappedTreeNode() != null) e.getMappedTreeNode().getName();
				});
			}

			Display.getDefault().syncExec(() -> {
				lstAttributes.setInput(attributes);
				lstAssetTypes.setInput(assetTypes);
				lstMetadataMappings.setInput(mappings);

				lstAttributes.setAllChecked(true);
				lstAssetTypes.setAllChecked(true);
				lstMetadataMappings.setAllChecked(true);
			});

			return Status.OK_STATUS;
		}

	};
}
