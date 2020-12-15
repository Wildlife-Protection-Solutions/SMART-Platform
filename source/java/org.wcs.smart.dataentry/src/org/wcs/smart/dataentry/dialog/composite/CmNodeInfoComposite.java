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

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.dataentry.CmDefaultListsUtil;
import org.wcs.smart.dataentry.CmDefaultTreesUtil;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ControlButton;
import org.wcs.smart.dataentry.dialog.composite.ImageSelectionControl.IImageContentProvider;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;

/**
 * Info composite for {@link CmNode}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class CmNodeInfoComposite extends AbstractInfoComposite {
	
	private Session session;
	private CmNode node;

	private Label lblCategory;
	private Button btnPhoto;
	private Button btnPhotoRequired;
	private Button btnCollectMultiple;
	private Button btnSingleGpsPoint;
	private ImageSelectionControl imageControl;
	
	private boolean isGroup;
	private List<CmAttributeConfig> deletedConfigs;
	
	public CmNodeInfoComposite(Composite parent, ConfigurableModel model, Session session, boolean isGroup, List<CmAttributeConfig> deletedConfigs) {
		super(parent, model);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		this.setLayout(layout);
		this.session = session;
		this.deletedConfigs = deletedConfigs;
		this.isGroup = isGroup;
		
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (isGroup)
			createGroupControls();
		else
			createCategoryControls();
	}
	
	public boolean isButtonValid(ConfigurableModelEditorDefaultTab.ControlButton button){
		if (isGroup){
			//groups enable all buttons
			return true;
		}else{
			//categories can only be deleted
			if (button == ControlButton.DELETE){
				return true;
			}
		}
		
		return false;
	}
	
	private void createGroupControls() {
		Composite container = createContentContainer(this);
		createDisplayNameControls(container);
		createDisplayModeControls(container);
		
		addImageRow(container);
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				imageControl.updateImage();
			}
		});
		
	}
	
	private void addImageRow(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmNodeInfoComposite_Image);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		imageControl = new ImageSelectionControl(container, new IImageContentProvider() {
			@Override
			public boolean isCustom() {
				if (getSourceObject() == null) return true;
				return getSourceObject().hasCustomImage();
			}
			
			@Override
			public boolean hasDataModel() {
				if (getSourceObject() == null) return false;
				if (getSourceObject().getCategory() != null) return true;
				return false;
			}
			
			@Override
			public Path getImageFile() {
				if (getSourceObject().hasCustomImage()) {
					return getSourceObject().getImageFile();
				}
				
				CmNode cmNode = getSourceObject();
				if (cmNode.getCategory() == null) return null;
				Icon i = cmNode.getCategory().getIcon();
				if (i == null) return null;
				IconFile iconfile = i.getIconFile(getModel().getIconSet());
				if (iconfile == null) return null;
				return iconfile.getAttachmentFile();
				
			}
			
			@Override
			public void setImageFile(Path file) {
				CmNode cmNode = getSourceObject();
				cmNode.setImageFile(file);
				if (cmNode.getUuid() != null) {
					//need this logic to correctly trigger intercepter
					session.evict(cmNode);
					session.saveOrUpdate(cmNode);
				}
				fireModelChanged();
			}
			@Override
			public ConfigurableModel getModel() {
				return CmNodeInfoComposite.this.getModel();
			}
		});
		imageControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}
	
	private void createCategoryControls() {
		Composite container = createContentContainer(this);
		createDisplayNameControls(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmNodeInfoComposite_Category);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lblCategory = new Label(container, SWT.WRAP);
		lblCategory.setText(""); //$NON-NLS-1$
		lblCategory.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		((GridData)lblCategory.getLayoutData()).widthHint = 100;
		
		label = new Label(container, SWT.NONE);
		label.setText(Messages.CmNodeInfoComposite_OptionsSection);
		
		btnPhoto = new Button(container, SWT.CHECK);
		btnPhoto.setText(Messages.CmNodeInfoComposite_AttachmentsAllowed);
		btnPhoto.setToolTipText(Messages.CmNodeInfoComposite_AttachmentsAllowedTooltip);
		btnPhoto.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().setPhotoAllowed(btnPhoto.getSelection());
				btnPhotoRequired.setEnabled(isPhotoRequiredEnabled(getSourceObject()));
				fireModelChanged();
			}
		});

		
		label = new Label(container, SWT.NONE);
		
		btnPhotoRequired = new Button(container, SWT.CHECK);
		btnPhotoRequired.setText(Messages.CmNodeInfoComposite_PhotoRequired);
		btnPhotoRequired.setToolTipText(Messages.CmNodeInfoComposite_PhotoRequiredTooltip);
		btnPhotoRequired.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)btnPhotoRequired.getLayoutData()).horizontalIndent = 15;
		btnPhotoRequired.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().setPhotoRequired(btnPhotoRequired.getSelection());
				fireModelChanged();
			}
		});
		
		label = new Label(container, SWT.NONE);
		label.setText(Messages.CmNodeInfoComposite_ClassicOptionsLbl);
		label.setToolTipText(Messages.CmNodeInfoComposite_ClassicOptionsTooltip);
		
		btnCollectMultiple = new Button(container, SWT.CHECK);
		btnCollectMultiple.setText(Messages.CmNodeInfoComposite_CollectMultiplObservations);
		btnCollectMultiple.setToolTipText(Messages.CmNodeInfoComposite_CollectMultiplObservationsTooltip);
		btnCollectMultiple.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isMulti = btnCollectMultiple.getSelection();
				getSourceObject().setCollectMultipleObservations(isMulti);
				
				if (isMulti){
					//ensure non of the attributes are multi-select
					//for multiple observation categories we do not support multi-select lists or numeric lists
					for (CmAttribute kid : getSourceObject().getCmAttributes()){
						CmAttributeOption op = kid.getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT);
						if (op != null) op.setBooleanValue(false);

						op = kid.getCmAttributeOptions().get(CmAttributeOption.ID_NUMERIC);
						if (op != null) op.setBooleanValue(false);
						
					}
				}
				btnSingleGpsPoint.setEnabled(isUseSingleGpsPointEnabled(getSourceObject()));
				fireModelChanged();
			}
		});

		
		label = new Label(container, SWT.NONE);

		btnSingleGpsPoint = new Button(container, SWT.CHECK);
		btnSingleGpsPoint.setText(Messages.CmNodeInfoComposite_RecordSingleGpsPoint);
		btnSingleGpsPoint.setToolTipText(Messages.CmNodeInfoComposite_RecordSingleGpsPointTooltip);
		btnSingleGpsPoint.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)btnSingleGpsPoint.getLayoutData()).horizontalIndent = 15;
		btnSingleGpsPoint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().setUseSingleGpsPoint(btnSingleGpsPoint.getSelection());
				fireModelChanged();
			}
		});
		
		
		label = new Label(container, SWT.NONE);
		label.setText(Messages.EditListDialog_DisplayMode);
		
		DisplayModeComboViewer modeViewer = new DisplayModeComboViewer(container);
		modeViewer.addDisplayModeChangeListener(e->{
			node.setDisplayMode(modeViewer.getSelectedDisplayMode());
			fireModelChanged();
		});
		modeViewer.addCascadeListener(new CascadeDisplayModeListener(modeViewer, this));
		modeViewer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		addImageRow(container);

		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmNode n = getSourceObject();
				boolean isGroup = n.isGroup();
				if (!isGroup) {
					if (lblCategory != null)
						lblCategory.setText(n.getCategory().getFullCategoryName(language));
					if (btnPhoto != null) {
						btnPhoto.setSelection(getPhotoAllowedValue(n));
						btnPhoto.setEnabled(isPhotoAllowedEnabled(n));
					}
					if (btnPhotoRequired != null) {
						btnPhotoRequired.setSelection(getPhotoRequiredValue(n));
						btnPhotoRequired.setEnabled(isPhotoRequiredEnabled(n));
					}
					if (btnCollectMultiple != null)
						btnCollectMultiple.setSelection(n.isCollectMultipleObservations());
					if (btnSingleGpsPoint != null) {
						btnSingleGpsPoint.setSelection(getUseSingleGpsPointValue(n));
						btnSingleGpsPoint.setEnabled(isUseSingleGpsPointEnabled(n));
					}
					modeViewer.setSelection(new StructuredSelection(node.getDisplayMode() != null ? node.getDisplayMode() : DisplayMode.DEFAULT_DISPLAY_MODE));
					imageControl.updateImage();
					CmNodeInfoComposite.this.layout(true, true);
				}
			}
		});
	}

	@Override
	protected void handleDeleteNode() {
		
		final CmNode node = getSourceObject();
		
		if (!MessageDialog.openConfirm(getShell(), Messages.CmNodeInfoComposite_DeleteDialogTitle, 
				MessageFormat.format(Messages.CmNodeInfoComposite_DeleteConfirmation, new Object[]{node.getName()}))){
			return;
		}
		
		CmNode parentNode = node.getParent();
		node.setParent(null);
		if (parentNode == null) {
			//this is the root node
			getModel().getNodes().remove(node);
			//re-order nodes
			int i = 0;
			for (CmNode n : getModel().getNodes()){
				n.setNodeOrder(i++);
			}
		} else {
			//not a root node
			parentNode.getChildren().remove(node);
			
			//re-order nodes
			int i = 0;
			for (CmNode n : parentNode.getChildren()){
				n.setNodeOrder(i++);
			}
		}
		
		//remove default tree mapping if present
		Set<Attribute> existingTrees = CmDefaultTreesUtil.getPresentedTreeAttributes(getModel());
		for (Attribute a : CmDefaultTreesUtil.getPresentedTreeAttributes(node)) {
			if (!existingTrees.contains(a)) {
				//attribute is not present in CM anymore -> remove all related configurations
				getModel().getDefaultConfigs().remove(a);
				removeRelatedConfigs(a, node);
				
				
			}
		}
		//remove default list mapping if present
		Set<Attribute> existingLists = CmDefaultListsUtil.getPresentedListAttributes(getModel());
		for (Attribute a : CmDefaultListsUtil.getPresentedListAttributes(node)) {
			if (!existingLists.contains(a)) {
				//attribute is not present in CM anymore -> remove all related configurations
				getModel().getDefaultConfigs().remove(a);
				removeRelatedConfigs(a, node);
			}
		}
		fireModelChanged();
	}

	private void removeRelatedConfigs(Attribute a, CmNode node) {
		List<CmAttributeConfig> configs = DataentryHibernateManager.getCmAttributeConfigs(this.session, getModel(), a);
		for (CmAttributeConfig cfg : configs) {
			if (node.getCmAttributes().isEmpty()) continue;
			//for hibernate
			node.getCmAttributes().forEach(cma->{
				if (cfg.equals(cma.getConfig())) {
					cma.setConfig(null);
				}
			});
			
			this.session.delete(cfg);
			this.deletedConfigs.add(cfg);

			//for hibernate
			if (cfg.getList() != null)cfg.getList().forEach(f->f.setConfig(null));
			if (cfg.getTree() != null)cfg.getTree().forEach(f->f.setConfig(null));
		}
		
	}
	
	private boolean isPhotoAllowedEnabled(CmNode node) {
		return !node.getModel().isPhotoFirst();
	}

	private boolean getPhotoAllowedValue(CmNode node) {
		return node.isPhotoAllowed() || node.getModel().isPhotoFirst();
	}

	private boolean isPhotoRequiredEnabled(CmNode node) {
		return node.isPhotoAllowed() && !node.getModel().isPhotoFirst();
	}

	private boolean getPhotoRequiredValue(CmNode node) {
		return node.isPhotoRequired() && !node.getModel().isPhotoFirst();
	}

	private boolean isUseSingleGpsPointEnabled(CmNode node) {
		return node.isCollectMultipleObservations() && !node.getModel().isInstantGps() && !node.getModel().isPhotoFirst();
	}

	private boolean getUseSingleGpsPointValue(CmNode node) {
		return node.isUseSingleGpsPoint() || node.getModel().isInstantGps() || node.getModel().isPhotoFirst();
	}
	
	@Override
	public CmNode getSourceObject() {
		return node;
	}

	public void setSourceObject(CmNode node, Language currentLanguage) {
		this.node = node;
		try {
			fireChanged = false;
			if (node.getCategory() != null) {
				if (node.getCategory().getIcon() != null) {
					loadFiles(node.getCategory().getIcon(), session);
				}
			}
		
			fireSourceObjectChanged(node, currentLanguage);
		}finally {
			fireChanged = true;
		}
	}
}
