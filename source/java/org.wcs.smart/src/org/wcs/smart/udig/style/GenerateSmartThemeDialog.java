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
package org.wcs.smart.udig.style;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.PropertyType;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.legend.style.DataModelStyleGenerator;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for generating data model theme styles
 * 
 * @author Emily
 *
 */
//TODO: support ccaa with data model merging
public class GenerateSmartThemeDialog extends SmartStyledDialog {

	//observation category label
	private static final String OBS_CAT_LABEL = Messages.GenerateSmartThemeDialog_ObsCategoryLabel;
	
	//ui elements
	private ComboViewer cmbDataModel;
	private ComboViewer cmbFAttribute;
	private ComboViewer cmbIconSet;
	private Combo cmbIconSize;
	private Button btnIncludeData, btnIncludeAll ;
	
	//input layer
	private Layer layer;
	
	//generated style
	private Style style;
	
	protected GenerateSmartThemeDialog(Shell parent, Layer layer) {
		super(parent);
		this.layer = layer;
	}

	public Style getStyle() {
		return style;
	}
	
	@Override
	public void cancelPressed() {
		style = null;
		super.cancelPressed();
	}
		
	@Override
	public void okPressed() {
		style = null;
		Object selection = cmbFAttribute.getStructuredSelection().getFirstElement();
		if (selection == null || !(selection instanceof AttributeDescriptor)) {
			MessageDialog.openWarning(getShell(), Messages.GenerateSmartThemeDialog_ErrorTitle, Messages.GenerateSmartThemeDialog_AttributeRequired);
			return;
		}
		AttributeDescriptor ad = (AttributeDescriptor) selection;
		
		selection = cmbIconSet.getStructuredSelection().getFirstElement();
		if (selection == null || !(selection instanceof IconSet)) {
			MessageDialog.openWarning(getShell(), Messages.GenerateSmartThemeDialog_ErrorTitle, Messages.GenerateSmartThemeDialog_IconSetRequired);
			return;
		}
		IconSet iset = (IconSet)selection;
		
		selection = cmbDataModel.getStructuredSelection().getFirstElement();
		if (selection == null || !(selection instanceof Attribute || selection instanceof Integer)) {
			MessageDialog.openWarning(getShell(), Messages.GenerateSmartThemeDialog_ErrorTitle, Messages.GenerateSmartThemeDialog_DmElementRequired);
			return;
		}
		Object dmItem = selection;
		
		Integer iconSize = 16;
		try {
			iconSize = Integer.parseInt(cmbIconSize.getText());
			if (iconSize < 0) throw new Exception();
		}catch (Exception ex) {
			MessageDialog.openWarning(getShell(), Messages.GenerateSmartThemeDialog_ErrorTitle, Messages.GenerateSmartThemeDialog_ValidSizeRequired);
			return;
		}
		
		SimpleFeatureSource source = null;
		try {
			source = layer.getGeoResource().resolve(SimpleFeatureSource.class, new NullProgressMonitor());
		}catch (Exception ex) {
			MessageDialog.openWarning(getShell(), Messages.GenerateSmartThemeDialog_ErrorTitle, Messages.GenerateSmartThemeDialog_FeatureSourceError);
			return;
		}
		DataModelStyleGenerator generator = new DataModelStyleGenerator(source, iset, btnIncludeAll.getSelection(), iconSize, ((Map)layer.getMap()).getColourScheme());
		
		if (dmItem instanceof Attribute) {
			try(Session session = HibernateManager.openSession()){
				style = generator.generateThemesAttribute(ad, (Attribute)dmItem, session);
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
				MessageDialog.openWarning(getShell(), Messages.GenerateSmartThemeDialog_ErrorTitle, MessageFormat.format(Messages.GenerateSmartThemeDialog_AttributeThemeError, ex.getMessage()));
				return;
			}
		}else if (dmItem instanceof Integer) {
			try(Session session = HibernateManager.openSession()){
				style = generator.generateThemesCategory(ad, (Integer)dmItem, session);				
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
				MessageDialog.openWarning(getShell(), Messages.GenerateSmartThemeDialog_ErrorTitle, MessageFormat.format(Messages.GenerateSmartThemeDialog_CategoryThemeError, ex.getMessage()));
				return;
			}
		}
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.GenerateSmartThemeDialog_FeatureAttributeLabel);
		
		cmbFAttribute = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbFAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbFAttribute.setContentProvider(ArrayContentProvider.getInstance());
		cmbFAttribute.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((AttributeDescriptor)element).getLocalName();
			}
		});
		cmbFAttribute.addSelectionChangedListener(e->findDm());
		
		List<AttributeDescriptor> dd = layer.getSchema().getAttributeDescriptors().stream()
				.filter(e->(e.getType() instanceof PropertyType) && ((PropertyType) e.getType()).getBinding().equals(String.class))
				.collect(Collectors.toList());
		cmbFAttribute.setInput(dd);
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.GenerateSmartThemeDialog_DmElementLabel);
		
		cmbDataModel = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbDataModel.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbDataModel.setContentProvider(ArrayContentProvider.getInstance());
		cmbDataModel.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbDataModel.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Attribute) return ((Attribute)element).getName();
				if (element instanceof Integer) return MessageFormat.format( OBS_CAT_LABEL + " {0}", (Integer)element); //$NON-NLS-1$
				return element.toString();
			}
		});
		
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.GenerateSmartThemeDialog_IconSetLabel);
		
		cmbIconSet = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbIconSet.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbIconSet.setContentProvider(ArrayContentProvider.getInstance());
		cmbIconSet.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbIconSet.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IconSet) return ((IconSet)element).getName();
				return super.getText(element);
			}
		});
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.GenerateSmartThemeDialog_IconSizeLabel);
		
		cmbIconSize = new Combo(main, SWT.DROP_DOWN);
		cmbIconSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbIconSize.setItems(new String[] {"8", "16", "24", "32"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		cmbIconSize.setText("16"); //$NON-NLS-1$
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.GenerateSmartThemeDialog_MissionValuesOp);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Composite mtemp = new Composite(main, SWT.NONE);
		mtemp.setLayout(new GridLayout());
		((GridLayout)mtemp.getLayout()).marginWidth = 0;
		
		btnIncludeAll = new Button(mtemp, SWT.RADIO);
		btnIncludeAll.setText(Messages.GenerateSmartThemeDialog_IncludeAllOp);
		btnIncludeAll.setSelection(true);
		
		btnIncludeData = new Button(mtemp, SWT.RADIO);
		btnIncludeData.setText(Messages.GenerateSmartThemeDialog_IncludeDataOp);
		
		loadDm.schedule();
		
		getShell().setText(Messages.GenerateSmartThemeDialog_Title);
		return composite;
	}

	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	/*
	 * attempt to match the category to the data model element
	 */
	@SuppressWarnings("unchecked")
	private void findDm() {
		String value = ((AttributeDescriptor)cmbFAttribute.getStructuredSelection().getFirstElement()).getLocalName();
		value = value.replaceAll("[^\\p{L}\\p{N}0-9]", "").toLowerCase(Locale.ROOT); //$NON-NLS-1$ //$NON-NLS-2$
		
		List<Object> dmItems = (List<Object>) cmbDataModel.getInput();
		
		Object found = null;
		for (Object x : dmItems) {
			
			if (x instanceof Attribute) {
				Attribute a = (Attribute)x;
				
				String part = a.getName().replaceAll("[^\\p{L}\\p{N}0-9]", "").toLowerCase(Locale.ROOT); //$NON-NLS-1$ //$NON-NLS-2$
				if (part.equals(value)) {
					found = x;break;
				}
				
			}else if (x instanceof Integer) {
				String part = OBS_CAT_LABEL.replaceAll("[^\\p{L}\\p{N}0-9]", "") + ((Integer)x); //$NON-NLS-1$ //$NON-NLS-2$
				part = part.toLowerCase();
				if (part.equals(value)) {
					found = x;break;
				}
			}
		}
		if (found != null) {
			cmbDataModel.setSelection(new StructuredSelection(found));
		}
	}
	
	private Job loadDm = new Job("loading data model elements") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IconSet> iset = new ArrayList<>();
			List<Object> dmItems = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				//TODO: Support CrossConservationAnalysis tools for theme generation
				//max category depth
				String query = "SELECT cast(max(smart.hkeyLength(hkey)) as int) FROM Category WHERE conservationArea = :ca "; //$NON-NLS-1$
				Query<?> q = session.createQuery(query)
						.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$

				Integer maxInt = (Integer) q.uniqueResult();
				if (maxInt != null) {
					for (int i = 0; i <= maxInt; i ++) {
						dmItems.add(i);
					}
				}				
				
				//load all list and tree attributes and category levels
				List<Attribute.AttributeType> types = new ArrayList<>();
				types.add(Attribute.AttributeType.LIST);
				types.add(Attribute.AttributeType.TREE);
				dmItems.addAll(session.createQuery("FROM Attribute WHERE conservationArea = :ca AND type IN (:types)", Attribute.class) //$NON-NLS-1$
					.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.setParameterList("types", types) //$NON-NLS-1$
					.list());
				
				iset.addAll(session.createQuery("FROM IconSet WHERE conservationArea =: ca", IconSet.class) //$NON-NLS-1$
						.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list());				
			}
			
			Display.getDefault().syncExec(()->{
				cmbIconSet.setInput(iset);
				IconSet s = null;
				for (IconSet is : iset) {
					if (is.getIsDefault()) {
						s = is;
						break;
					}
				}
				if (s == null && !iset.isEmpty()) s = iset.get(0);
				if (!iset.isEmpty()) cmbIconSet.setSelection(new StructuredSelection(s));
				
				cmbDataModel.setInput(dmItems);
				
			});
			return Status.OK_STATUS;
		}
		
	};
	

}
