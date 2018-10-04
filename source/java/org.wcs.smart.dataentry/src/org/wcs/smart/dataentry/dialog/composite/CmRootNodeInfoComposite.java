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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ControlButton;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Info composite for {@link CmRootNode}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class CmRootNodeInfoComposite extends AbstractInfoComposite {

	private static final String NONE = Messages.CmRootNodeInfoComposite_NoneOption;
	private CmRootNode rootNode;

	private Button btnInstantGps;
	private Button btnPhotoFirst;
	private ComboViewer cmbIconSet;
	
	public CmRootNodeInfoComposite(Composite parent, ConfigurableModel model) {
		super(parent, model);
		createControls();
	}
	
	private void createControls() {
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		this.setLayout(layout);		
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite container = createContentContainer(this);
		createDisplayNameControls(container);
		createDisplayModeControls(container);

		Label l = new Label(container, SWT.NONE);
		l.setText(Messages.CmRootNodeInfoComposite_IconSetOption);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		cmbIconSet = new ComboViewer(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbIconSet.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbIconSet.setContentProvider(ArrayContentProvider.getInstance());
		cmbIconSet.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IconSet) return ((IconSet)element).getName();
				return super.getText(element);
			}
		});
		
		List<Object> sets = new ArrayList<>();
		sets.add(NONE);
		try(Session session = HibernateManager.openSession()){
			List<IconSet> items = QueryFactory.buildQuery(session, IconSet.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			items.forEach(e->e.getName());
			sets.addAll(items);
		}
		cmbIconSet.setInput(sets);
		cmbIconSet.setSelection(new StructuredSelection(NONE));
		cmbIconSet.addSelectionChangedListener(e->{
			Object x = cmbIconSet.getStructuredSelection().getFirstElement();
			if (x instanceof IconSet) {
				getSourceObject().getModel().setIconSet((IconSet)x);
			}else {
				getSourceObject().getModel().setIconSet(null);
			}
            fireModelChanged();
		});
		
        Label lblInstantGps = new Label(container, SWT.NONE);
        lblInstantGps.setText(Messages.CmRootNodeInfoComposite_InstantGps);
        lblInstantGps.setToolTipText(Messages.CmRootNodeInfoComposite_InstantGpsTooltip);
        btnInstantGps = new Button(container, SWT.CHECK);
        btnInstantGps.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getSourceObject().getModel().setInstantGps(btnInstantGps.getSelection());
                fireModelChanged();
            }
        });

        Label lblPhotoFirst = new Label(container, SWT.NONE);
        lblPhotoFirst.setText(Messages.CmRootNodeInfoComposite_PhotoFirst);
        lblPhotoFirst.setToolTipText(Messages.CmRootNodeInfoComposite_PhotoFirstTooltip);
        btnPhotoFirst = new Button(container, SWT.CHECK);
        btnPhotoFirst.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getSourceObject().getModel().setPhotoFirst(btnPhotoFirst.getSelection());
                fireModelChanged();
            }
        });
        
        addSourceObjectChangedListener(new ISourceObjectChangedListener() {
            @Override
            public void sourceObjectChanged(Object newObject, Language language) {
                ConfigurableModel cm = getSourceObject().getModel();
                if (btnInstantGps != null) {
                	btnInstantGps.setSelection(cm.isInstantGps());
                }
                if (btnPhotoFirst != null) {
                	btnPhotoFirst.setSelection(cm.isPhotoFirst());
                }
                if (cmbIconSet != null) {
                	if (cm.getIconSet() != null) {
                		cmbIconSet.setSelection(new StructuredSelection(cm.getIconSet()));
                	}else {
                		cmbIconSet.setSelection(new StructuredSelection(NONE));
                	}
                }
            }
        });

	}

	public boolean isButtonValid(ConfigurableModelEditorDefaultTab.ControlButton button){
		if (button == ControlButton.DELETE){
			return false;
		}
		return true;
	}
	
	@Override
	public CmRootNode getSourceObject() {
		return rootNode;
	}
	
	public void setSourceObject(CmRootNode rootNode, Language language) {
		this.rootNode = rootNode;
		fireSourceObjectChanged(rootNode, language);
	}
	
}
