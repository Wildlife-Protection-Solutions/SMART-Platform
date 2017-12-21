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
package org.wcs.smart.asset.ui.views.asset;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetHistoryRecord;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.ui.AttributeFieldEditor;
import org.wcs.smart.asset.ui.CommentDialog;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Asset attribute page for the asset editor.
 * 
 * @author Emily
 *
 */
public class AssetPropertyPage {

	private AssetEditor parentEditor;
	
	private List<AttributeFieldEditor> fieldEditors;
	private Composite attributePanel;
	private FormToolkit toolkit;
	private Label lblRetiredState;
	private Hyperlink changeRetiredState;
	
	public AssetPropertyPage(AssetEditor editor) {
		this.parentEditor = editor;
	}
	
	private void retireAsset() {
		if (parentEditor.getAsset() == null) return;
		
		Asset asset = parentEditor.getAsset();
		String action;
		String msg;
		if (!asset.getIsRetired()) {
			action = "Asset Retired - ";
			msg = "Enter a comment related to the retirement";
		}else {
			action = "Asset Unretired - ";
			msg = "Enter a comment related to unretirement of asset";
		}
		CommentDialog dialog = new CommentDialog(parentEditor.getSite().getShell(), "Asset History Comment", msg);
		
		if (dialog.open() != CommentDialog.OK) return;
		
		AssetHistoryRecord historyRecord = new AssetHistoryRecord();
		historyRecord.setAsset(asset);
		historyRecord.setComment(action + dialog.getComment());
		historyRecord.setDate(new Date());
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				asset.setIsRetired(!asset.getIsRetired());
				session.saveOrUpdate(historyRecord);
				session.saveOrUpdate(asset);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.log("Unable to update asset state.  Please close editor and try again. " + ex.getMessage(),ex);
				return;
			}
			parentEditor.activeHistoryRecords.add(historyRecord);
			parentEditor.refreshHistoryRecords();
		}
		parentEditor.refreshStatus();
	}
	
	public void createControl(Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		Composite toppanel = toolkit.createComposite(panel, SWT.BORDER);
		toppanel.setLayout(new GridLayout(3, false));
		toppanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = toolkit.createLabel(toppanel, "State: ");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblRetiredState = toolkit.createLabel(toppanel, "");
		lblRetiredState.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		changeRetiredState = toolkit.createHyperlink(toppanel, "", SWT.NONE);
		changeRetiredState.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		changeRetiredState.addHyperlinkListener(new HyperlinkAdapter() {			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				retireAsset();
			}
		});
		
		Composite attributeComp = toolkit.createComposite(panel, SWT.BORDER);
		attributeComp.setLayout(new GridLayout());
		attributeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		l = toolkit.createLabel(attributeComp, "Attributes");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.setFont(boldFont);
		l.addListener(SWT.Dispose,  e-> boldFont.dispose());
		
		attributePanel = toolkit.createComposite(attributeComp);
		attributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributePanel.setLayout(new GridLayout());
		((GridLayout)attributePanel.getLayout()).marginWidth = 0;
		((GridLayout)attributePanel.getLayout()).marginHeight = 0;
		
		initializeAttributes(parentEditor.getAsset());
	}
	
	public void initializeAttributes(Asset asset) {
		
		for (Control c : attributePanel.getChildren()) c.dispose();
		fieldEditors = new ArrayList<>();
		
		if (lblRetiredState != null) {
			lblRetiredState.setText(asset.getStatus().getGuiName(Locale.getDefault()));
		}
		if (changeRetiredState != null) {
			if (asset.getIsRetired()) {
				changeRetiredState.setText("unretire asset");
			}else {
				changeRetiredState.setText("retire asset");
			}
			changeRetiredState.getParent().layout(true);
		}
		
		ScrolledComposite scroll = new ScrolledComposite(attributePanel, SWT.V_SCROLL);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(scroll);
		
		Composite attributes = toolkit.createComposite(scroll);
		attributes.setLayout(new GridLayout(2, false));
		scroll.setContent(attributes);
		
		List<AssetAttribute> assetAttributes = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			List<AssetTypeAttribute> aa = session.createQuery("FROM AssetTypeAttribute WHERE id.assetType = :type", AssetTypeAttribute.class)
						.setParameter("type", asset.getAssetType())
						.list();
			for (AssetTypeAttribute a : aa) {
				a.getAttribute().getName();
				if (a.getAttribute().getAttributeList() != null) a.getAttribute().getAttributeList().forEach(e->e.getName());
				assetAttributes.add(a.getAttribute());
			}
		}
		
		for (AssetAttribute attribute : assetAttributes) {
			AttributeFieldEditor editor = new AttributeFieldEditor(attributes, attribute);
			editor.adapt(toolkit);
			fieldEditors.add(editor);
			if (editor.getTextAttributeControl() != null) {
				editor.getTextAttributeControl().addListener(SWT.Resize, e-> scroll.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
			}
			
			for (AssetAttributeValue v : asset.getAttributeValues()) {
				if (v.getAttribute().equals(attribute)) editor.initControl(v);
			}
			
			editor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!editor.isValid()) return;
					AssetAttributeValue toUpdate = null;
					for (AssetAttributeValue v : asset.getAttributeValues()) {
						if (v.getAttribute().equals(editor.getAttribute())) {
							toUpdate = v;
							break;
						}
					}
					boolean isNew = false;
					if (toUpdate == null) {
						isNew = true;
						toUpdate = new AssetAttributeValue();
						toUpdate.setAsset(asset);
						toUpdate.setAttribute(editor.getAttribute());
					}
					if (editor.updateValue(toUpdate)) {
						if (isNew) asset.getAttributeValues().add(toUpdate);
					}else {
						if (!isNew) asset.getAttributeValues().remove(toUpdate);
					}
					parentEditor.setDirty(true);
					
				}
			});
		}
		scroll.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scroll.getParent().layout(true);
	}
}
