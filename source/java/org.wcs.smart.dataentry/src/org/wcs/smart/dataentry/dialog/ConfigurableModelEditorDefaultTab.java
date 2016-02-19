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
package org.wcs.smart.dataentry.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.dialog.composite.AbstractInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.AbstractInfoComposite.IModelChangedListener;
import org.wcs.smart.dataentry.dialog.composite.BooleanAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.CmNodeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.CmRootNodeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.DateAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.ListAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.NumericAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.TextAttributeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.TreeAttributeInfoComposite;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;

/**
 * Tab with default content for configurable model.
 * If not additional tabs provided this will be the only content displayed (without tab itself).
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConfigurableModelEditorDefaultTab implements IConfigurableModelEditorTabContent {

	public static enum ControlButton {
		ADD_GROUP(Messages.AbstractInfoComposite_Button_AddGroup),
		ADD_CATEGORY(Messages.AbstractInfoComposite_Button_AddCategory), 
		DELETE(DialogConstants.DELETE_BUTTON_TEXT);
		
		public String name;
		
		ControlButton(String name){
			this.name = name;
		}
	};
	
	private ConfigurableModelEditDialog dialog;
	
	private LanguageViewer languageViewer;
	private TreeViewer modelTreeViewer;

	private Composite infoInnerPanel;
	private Composite emptyComposite;
	private CmRootNodeInfoComposite rootNodeComposite;
	private CmNodeInfoComposite groupNodeComposite;
	private CmNodeInfoComposite categoryNodeComposite;
	private Map<AttributeType, CmAttributeInfoComposite> attributeComposites;
	
	private HashMap<ControlButton, Button> controlButtons = new HashMap<ControlButton, Button>();
	ChangeTracker tracker = new ChangeTracker();
	
	public ConfigurableModelEditorDefaultTab(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void setDialog(ConfigurableModelEditDialog dialog) {
		this.dialog = dialog;
	}
	
	@Override
	public String getTabName() {
		return Messages.ConfigurableModelEditorDefaultTab_TabName;
	}

	@Override
	public Composite createTabContent(Composite parent) {
		ConfigurableModel model = dialog.getModel();
		SashForm container = new SashForm(parent, SWT.HORIZONTAL);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite innerLeft = new Composite(container, SWT.NONE);
		innerLeft.setLayout(new GridLayout());
		
		languageViewer = new LanguageViewer(innerLeft, SWT.DROP_DOWN | SWT.READ_ONLY, SmartDB.getCurrentConservationArea());
		languageViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		modelTreeViewer = new TreeViewer(innerLeft, SWT.V_SCROLL | SWT.H_SCROLL| SWT.BORDER);
		modelTreeViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider(true));
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)modelTreeViewer.getControl().getLayoutData()).widthHint = 100;
		((GridData)modelTreeViewer.getControl().getLayoutData()).heightHint = 100;
		modelTreeViewer.setInput(model);
		modelTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRightPanelState();
			}
		});
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		modelTreeViewer.addDragSupport(DND.DROP_MOVE, transferTypes , new ConfigurableModelTreeDragListener(modelTreeViewer));
		modelTreeViewer.addDropSupport(DND.DROP_MOVE, transferTypes, new ConfigurableModelTreeDropListener(modelTreeViewer) {
			@Override
			public boolean performDrop(Object data) {
				boolean ok = super.performDrop(data);
				if (ok) {
					dialog.notifyChangesMade();
					updateRightPanelState();
				}
				return ok;
			}
		});
		modelTreeViewer.expandToLevel(2);
		
		languageViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				((ConfigurableModelLabelProvider)modelTreeViewer.getLabelProvider()).setLanguage(languageViewer.getCurrentSelection());
				modelTreeViewer.refresh();
				updateRightPanelState();
			}
		});

		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));

		Composite buttonPanel = new Composite(rightPanel, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(3, false));
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		for (final ControlButton cbtn : ControlButton.values()){
			Button btn = new Button(buttonPanel, SWT.PUSH);
			btn.setText(cbtn.name);
			btn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Control currentPanel = ((StackLayout)infoInnerPanel.getLayout()).topControl;
					if (currentPanel instanceof AbstractInfoComposite){
						((AbstractInfoComposite)currentPanel).processButton(cbtn);
						
						//we we add or remove something make sure the current selection is expanded
						Object x = ((StructuredSelection)modelTreeViewer.getSelection()).getFirstElement();
						if (x != null) modelTreeViewer.setExpandedState(x, true);
					}
				}
			});
			btn.setEnabled(false);
			dialog.setButtonLayoutData(btn);
			controlButtons.put(cbtn,btn);
		}		
		
		infoInnerPanel = new Group(rightPanel, SWT.NONE);
		((Group)infoInnerPanel).setText(Messages.ConfigurableModelEditDialog_PropertiesLabel);
		
		StackLayout layout = new StackLayout();
		layout.marginHeight = 2;
		infoInnerPanel.setLayout(layout);
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		IModelChangedListener modelChangeListener = new IModelChangedListener() {
			@Override
			public void modelChanged() {
				dialog.notifyChangesMade();
				modelTreeViewer.refresh();
			}
		};

		//NOTE: session is already opened and should not be closed until dialog is closed
		
		rootNodeComposite = new CmRootNodeInfoComposite(infoInnerPanel, model, tracker);
		rootNodeComposite.addModelChangedListener(modelChangeListener);

		groupNodeComposite = new CmNodeInfoComposite(infoInnerPanel, model, tracker, true);
		groupNodeComposite.addModelChangedListener(modelChangeListener);
		
		categoryNodeComposite = new CmNodeInfoComposite(infoInnerPanel, model, tracker, false);
		categoryNodeComposite.addModelChangedListener(modelChangeListener);

		attributeComposites = new HashMap<AttributeType, CmAttributeInfoComposite>();
		CmAttributeInfoComposite attrComposite;

		attrComposite = new NumericAttributeInfoComposite(infoInnerPanel, model, tracker);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.NUMERIC, attrComposite);
		
		attrComposite = new TextAttributeInfoComposite(infoInnerPanel, model, tracker);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.TEXT, attrComposite);

		attrComposite = new ListAttributeInfoComposite(infoInnerPanel, model, tracker);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.LIST, attrComposite);

		attrComposite = new TreeAttributeInfoComposite(infoInnerPanel, model, tracker);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.TREE, attrComposite);

		attrComposite = new BooleanAttributeInfoComposite(infoInnerPanel, model, tracker);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.BOOLEAN, attrComposite);

		attrComposite = new DateAttributeInfoComposite(infoInnerPanel, model, tracker);
		attrComposite.addModelChangedListener(modelChangeListener);
		attributeComposites.put(AttributeType.DATE, attrComposite);
		
		container.setWeights(new int[]{40,60});
		
		//set  language
		((ConfigurableModelLabelProvider)modelTreeViewer.getLabelProvider()).setLanguage(languageViewer.getCurrentSelection());
		modelTreeViewer.refresh();
		updateRightPanelState();
		
		return container;
	}

	private void updateRightPanelState() {
		IStructuredSelection selection = (IStructuredSelection) modelTreeViewer.getSelection();
		Object obj = selection.getFirstElement();

		if (obj instanceof CmNode) {
			CmNode node = (CmNode) obj;
			CmNodeInfoComposite cmp = node.isGroup() ? groupNodeComposite : categoryNodeComposite;
			cmp.setSourceObject(node, languageViewer.getCurrentSelection());
			((StackLayout)infoInnerPanel.getLayout()).topControl = cmp;
			
		} else if (obj instanceof CmAttribute) {
			CmAttribute attr = (CmAttribute)obj;
			CmAttributeInfoComposite attrComposite = attributeComposites.get(attr.getAttribute().getType());
			if (attrComposite != null) {
				attrComposite.setSourceObject((CmAttribute)obj, languageViewer.getCurrentSelection());
				((StackLayout)infoInnerPanel.getLayout()).topControl = attrComposite;
			} else {
				((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
			}

		} else if (obj instanceof CmRootNode) {
			rootNodeComposite.setSourceObject((CmRootNode)obj, languageViewer.getCurrentSelection());
			((StackLayout)infoInnerPanel.getLayout()).topControl = rootNodeComposite;
			
		} else {
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
		}
		infoInnerPanel.layout();
		
		//update buttons
		Control control = ((StackLayout)infoInnerPanel.getLayout()).topControl;
		if (control instanceof AbstractInfoComposite){
			for (Iterator<Entry<ControlButton, Button>> iterator = controlButtons.entrySet().iterator(); iterator.hasNext();) {
				Entry<ControlButton, Button> type = (Entry<ControlButton, Button>) iterator.next();
				type.getValue().setEnabled( ((AbstractInfoComposite)control).isButtonValid(type.getKey()));
			}
		}else{
			for (Button btn : controlButtons.values()){
				btn.setEnabled(false);
			}
		}
	}

	@Override
	public void performSave(Session s) {
		/*
		 * it would be possible not to track changes
		 * and just call saveofupdate(dialog.getModel())
		 * however this is exceptionally slow, so 
		 * and apply only the changes.
		 */
		//if new we need to save
		if (dialog.getModel().getUuid() == null){
			s.save(dialog.getModel());
		}
		s.flush();
		//apply all changes
		tracker.applyUpdates(s);
		s.flush();
		tracker.updates.clear();
	}

	/**
	 * Change tracker for tracking changes to model items.  
	 * 
	 * @author Emily
	 *
	 */
	public class ChangeTracker{
		List<Object[]> updates = new ArrayList<Object[]>();
		
		public void deleteObject(Object x){
			updates.add(new Object[]{0, x});
		}
		public void saveOrUpdate(Object x){
			updates.add(new Object[]{1, x});
		}
		public void applyUpdates(Session session){
			for (Object[] up: updates){
				if ((Integer)up[0] == 0){
					session.delete(up[1]);
				}else if ((Integer)up[0] == 1){
					session.saveOrUpdate(up[1]);
				}
			}
		}
	}
}
