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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.dataentry.CmAttributeOptionLabelProvider;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeOption.EnterOnceType;
import org.wcs.smart.dataentry.model.CmAttributeOption.VisibleWhen;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Info composite for {@link CmAttribute}
 *
 * @author elitvin
 * @since 2.0.0
 */
public abstract class CmAttributeInfoComposite extends AbstractInfoComposite {

	private Session session;
	private CmAttribute attribute;

	private Label lblAttribute;
	private Label lblEnterOnces;
	private ComboViewer enterOncesComboViewer;
	private ImageSelectionControl imageSelection;
	
	public CmAttributeInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, model);
		this.session = session;
		createControls();
	}
	
	/**
	 * attribute composites have no controls
	 */
	public boolean isButtonValid(ConfigurableModelEditorDefaultTab.ControlButton button){
		return false;
	}
	
	private void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite container = createContentContainer(this);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createDisplayNameControls(container);

		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Attribute);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		lblAttribute = new Label(container, SWT.WRAP);
		lblAttribute.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		((GridData)lblAttribute.getLayoutData()).widthHint = 100;
		lblAttribute.setText(""); //$NON-NLS-1$

		lblEnterOnces = new Label(container, SWT.NONE);
		lblEnterOnces.setText(Messages.CmAttributeInfoComposite_Option_EnableOnce);
		lblEnterOnces.setToolTipText(Messages.CmAttributeInfoComposite_EnableOnce_Tooltip);
		lblEnterOnces.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false));
		
		enterOncesComboViewer = createEnterOnceControl(container);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttribute attr = getSourceObject();
				if (attr != null) {
					if (lblAttribute != null) {
						String text = attr.getAttribute().findNameNull(language);
						if (text == null){
							text = attr.getAttribute().findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
						}
						text += " (" + attr.getNode().getCategory().getFullCategoryName(language) + ")";  //$NON-NLS-1$//$NON-NLS-2$
						lblAttribute.setText(text);
					}

					if (enterOncesComboViewer != null) {
						setEnterOncesIncluded(attr.getNode().isCollectMultipleObservations());
						CmAttributeOption op = attr.getCmAttributeOptions().get(CmAttributeOption.ID_ENTER_ONCES);
						if (op != null && op.getStringValue() != null){
							enterOncesComboViewer.setSelection(new StructuredSelection(EnterOnceType.valueOf(op.getStringValue())));
						}else{
							enterOncesComboViewer.setSelection(new StructuredSelection(EnterOnceType.NONE));
						}
					}
					CmAttributeInfoComposite.this.layout(true, true);
				}
			}
		});

		Label lImage = new Label(container, SWT.NONE);
		lImage.setText(Messages.CmAttributeInfoComposite_ImageLabel);
		lImage.setToolTipText(Messages.CmAttributeInfoComposite_ImageTooltip);
		lImage.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false));
		
		imageSelection = new ImageSelectionControl(container, new ImageSelectionControl.IImageContentProvider() {
				@Override
				public void setImageFile(Path file) {
					CmAttribute cmNode = getSourceObject();
					cmNode.setImageFile(file);
					if (cmNode.getUuid() != null) {
						//need this logic to correctly trigger intercepter
						session.evict(cmNode);
						session.saveOrUpdate(cmNode);
					}
					fireModelChanged();
				}
				
				@Override
				public boolean isCustom() {
					if (attribute == null) return true;
					return attribute.hasCustomImage();
				}
				
				@Override
				public boolean hasDataModel() {
					return true;
				}
				
				@Override
				public Path getImageFile() {
					CmAttribute cmNode = getSourceObject();
					if (cmNode == null) return null;
					if (isCustom()) {
						return cmNode.getImageFile();
					}
					
					if (cmNode.getAttribute() == null) return null;
					Icon i = cmNode.getAttribute().getIcon();
					if (i == null) return null;
					IconFile iconfile = i.getIconFile(getModel().getIconSet());
					if (iconfile == null) return null;
					return iconfile.getAttachmentFile();
				}
				@Override
				public ConfigurableModel getModel() {
					return CmAttributeInfoComposite.this.getModel();
				}
		});
		imageSelection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createTypeSpecificControls(container);

	}

	/**
	 * Create controls specifi to the attribute type 
	 * @param container
	 */
	protected abstract void createTypeSpecificControls(Composite container);

	protected ComboViewer createEnterOnceControl(Composite parent) {
		final ComboViewer enterOncesCombo = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		enterOncesCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		enterOncesCombo.setContentProvider(ArrayContentProvider.getInstance());
		enterOncesCombo.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof EnterOnceType){
					return CmAttributeOptionLabelProvider.INSTANCE.getGuiName(((EnterOnceType)element));
				}
				return ""; //$NON-NLS-1$
			}
		});
		enterOncesCombo.setInput(EnterOnceType.values());
		
		enterOncesCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)enterOncesCombo.getSelection()).getFirstElement();
				CmAttributeOption op = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_ENTER_ONCES);
				if (op == null) {
					op = CmAttributeOptionFactory.createEnterOnceOption(getSourceObject());
					getSourceObject().getCmAttributeOptions().put(op.getOptionId(),op);
				}
				boolean fire = true;
				if (x != null && x instanceof EnterOnceType) {
					fire = !x.toString().equals(op.getStringValue());
					op.setStringValue(x.toString());
				}else{
					getSourceObject().getCmAttributeOptions().remove(op.getOptionId());
					op.setStringValue(null);
				}
				if (fire){
					fireModelChanged();
				}
			}
		});
		return enterOncesCombo;
		
	}

	public void setEnterOncesIncluded(boolean isShow) {
		if (lblEnterOnces != null) {
			lblEnterOnces.setVisible(isShow);
			if (lblEnterOnces.getLayoutData() instanceof GridData) {
				GridData gd = (GridData) lblEnterOnces.getLayoutData();
				gd.exclude = !isShow;
			}
		}
		if (enterOncesComboViewer != null) {
			enterOncesComboViewer.getControl().setVisible(isShow);
			if (enterOncesComboViewer.getControl().getLayoutData() instanceof GridData) {
				GridData gd = (GridData) enterOncesComboViewer.getControl().getLayoutData();
				gd.exclude = !isShow;
			}
		}
	}
	
	protected ComboViewer createIsVisibleControl(Composite container) {
		
		final Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_IsVisible);
		label.setToolTipText(Messages.CmAttributeInfoComposite_enabledTooltip);
		
		ComboViewer cmbVisibleWhen = new ComboViewer(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbVisibleWhen.setContentProvider(ArrayContentProvider.getInstance());
		cmbVisibleWhen.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object x) {
				switch(((CmAttributeOption.VisibleWhen)x)) {
					case ALWAYS: return "Always";
					case CUSTOM: return "Custom";
					case NEVER: return "Never";
				}
				return "";
			}
		});
		cmbVisibleWhen.setInput(CmAttributeOption.VisibleWhen.values());
		cmbVisibleWhen.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_IS_VISIBLE);
				CmAttributeOption.VisibleWhen op = option.getVisibleWhen();
				if (op == null) op = CmAttributeOption.VisibleWhen.ALWAYS;
				cmbVisibleWhen.setSelection(new StructuredSelection(op));
			}
		});
		cmbVisibleWhen.addPostSelectionChangedListener(e->{
				Object x = cmbVisibleWhen.getStructuredSelection().getFirstElement();
				if (x == null) return ;
				if (!(x instanceof CmAttributeOption.VisibleWhen)) return;
				
				CmAttributeOption.VisibleWhen value = ((CmAttributeOption.VisibleWhen)x);
				if (value == VisibleWhen.ALWAYS || value == VisibleWhen.NEVER) {
					getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_IS_VISIBLE).setVisibleWhen(value, null);
				}else {
					
				}
				fireModelChanged();
		});
		return cmbVisibleWhen;
	}
	
	protected Button createBooleanControl(Composite parent, final String optionId, String text, String cbText, String tooltip) {
		final Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		label.setToolTipText(tooltip);
		
		final Button btnBool = new Button(parent, SWT.CHECK);
		btnBool.setText(cbText);
		btnBool.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().getCmAttributeOptions().get(optionId).setBooleanValue(btnBool.getSelection());
				fireModelChanged();
			}
		});
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(optionId);
				btnBool.setVisible(option != null);
				label.setVisible(option != null);
				if (option != null) {
					btnBool.setSelection(option.getBooleanValue());
				}
			}
		});
		return btnBool;
	}

	@Override
	public CmAttribute getSourceObject() {
		return attribute;
	}
	
	public void setSourceObject(CmAttribute attribute, Language language) {
		this.attribute = attribute;
		try {
			fireChanged = false;
			//load icon files as necessary
			if (attribute.getAttribute().getIcon() != null) loadFiles(attribute.getAttribute().getIcon(), session);
			if (attribute.getCurrentList() != null) {
				for (CmAttributeListItem li : attribute.getCurrentList()) {
					if (li.getListItem().getIcon() != null) loadFiles(li.getListItem().getIcon(), session);
				}
			}
			if (attribute.getCurrentTree() != null) {
				List<CmAttributeTreeNode> nodes = new ArrayList<>();
				nodes.addAll(attribute.getCurrentTree());
				while(!nodes.isEmpty()) {
					CmAttributeTreeNode node = nodes.remove(0);
					if (node.getDmTreeNode() != null && node.getDmTreeNode().getIcon() != null) loadFiles(node.getDmTreeNode().getIcon(), session);
					if (node.getChildren() != null) nodes.addAll(node.getChildren());
	
				}
			}
			imageSelection.updateImage();
			fireSourceObjectChanged(attribute, language);
		}finally {
			fireChanged = true;
		}
	}

}
