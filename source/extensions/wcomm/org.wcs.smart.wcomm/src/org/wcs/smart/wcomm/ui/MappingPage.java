/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.wcomm.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.wcomm.Messages;
import org.wcs.smart.wcomm.WCommPlugIn;
import org.wcs.smart.wcomm.WcommMapping;
import org.wcs.smart.wcomm.WcommMapping.Field;

/**
 * Editor page for managing wcomm mapping
 * @author Emily
 *
 */
public class MappingPage  extends EditorPart {

	private FormToolkit toolkit;
	
	private ComboViewer cmbOcCat;
	private ComboViewer cmbHwcCat;
	private ComboViewer cmbWoCat;
	private ComboViewer cmbEmCat;
	private Composite attributeMappingItems, incidentMappingItems;
	private WCommImportEditor editor;
	
	private List<ComboViewer> cmbAttributes;
	private List<Category> categories;
	
	private Set<Attribute> listtreeattributes = new HashSet<>();
	
	private boolean isInit = false;
	private ScrolledForm form;
	
	private ILabelProvider catAttLblProvider = new LabelProvider() {
		public String getText(Object element) {
			if (element instanceof CategoryAttribute) return ((CategoryAttribute)element).getAttribute().getName();
			if (element instanceof Attribute) return ((Attribute)element).getName();
			return super.getText(element);
		}
	};
	
	private ILabelProvider categoryLabelProvider = new LabelProvider() {
		public String getText(Object element) {
			if (element instanceof Category) {
				StringBuilder sb = new StringBuilder();
				Category c = ((Category)element).getParent();
				while(c != null) {
					sb.append("   "); //$NON-NLS-1$
					c = c.getParent();
				}
				sb.append(((Category)element).getName());
				if (((Category)element).getParent() != null) {
					sb.append(" (" + ((Category)element).getParent().getFullCategoryName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return sb.toString();
			}
			return super.getText(element);
		}
	};
	
	
	private ISelectionChangedListener attListener = e->{
		WcommMapping.Field field = (Field) ((ComboViewer)e.getSource()).getControl().getData("FIELD"); //$NON-NLS-1$
		Attribute a = (Attribute) e.getStructuredSelection().getFirstElement();
		if (!isInit) editor.getMapping().setField(field, a.getKeyId());
		if (a.getType() == AttributeType.LIST || a.getType() == AttributeType.TREE) listtreeattributes.add(a);
	};
	
	
	public MappingPage(WCommImportEditor editor) {
		this.editor = editor;
		cmbAttributes = new ArrayList<>();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void createPartControl(Composite parent) {
		
		toolkit = new FormToolkit(parent.getDisplay());
		
		form = toolkit.createScrolledForm(parent);
		form.setExpandHorizontal(true);
		form.setExpandVertical(true);
		form.getBody().setLayout(new GridLayout());
		
		Composite main = toolkit.createComposite(form.getBody(), SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Composite btns = toolkit.createComposite(main);
		btns.setLayout(new GridLayout(3, false));
		((GridLayout)btns.getLayout()).marginHeight = 0;
		btns.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		WidgetElement.setCSSClass(btns, "SMARTFormHeader"); //$NON-NLS-1$

		
		Label l = toolkit.createLabel(btns, Messages.MappingPage_SectionTitle);
		l.setFont(form.getFont());
		l.setForeground(form.getForeground());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Button btnExport = toolkit.createButton(btns, DialogConstants.EXPORT_BUTTON_TEXT, SWT.PUSH);
		btnExport.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		btnExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		btnExport.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(btns.getShell(), SWT.SAVE);
			fd.setText(Messages.MappingPage_ExportDialog);
			fd.setFileName("WCoMM_Mapping.csv"); //$NON-NLS-1$
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			String f = fd.open();
			if (f == null) return;
			
			Path efile = Paths.get(f);
			try {
				Files.copy(WcommMapping.getPath(), efile, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e1) {
				WCommPlugIn.displayLog(e1.getMessage(), e1);
			}
			
		});
		
		
		Button btnImport = toolkit.createButton(btns, DialogConstants.IMPORT_BUTTON_TEXT, SWT.PUSH);
		btnImport.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnImport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		btnImport.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(btns.getShell(), SWT.SAVE);
			fd.setText(Messages.MappingPage_ImportDialog);
			fd.setFileName("WCoMM_Mapping.csv"); //$NON-NLS-1$
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			String f = fd.open();
			if (f == null) return;
			
			Path efile = Paths.get(f);
			try {
				Files.copy(efile, WcommMapping.getPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e1) {
				WCommPlugIn.displayLog(e1.getMessage(), e1);
			}
			
			//close and reopen editor
			IWorkbenchPage current = editor.getEditorSite().getWorkbenchWindow().getActivePage();
			current.closeEditor(editor, false);
			try {				
				current.openEditor(DataImportEditorInput.INSTANCE, WCommImportEditor.ID);
			} catch (PartInitException e1) {
				WCommPlugIn.displayLog(e1.getMessage(), e1);
			}	

		});
		
		
		createObservationsMapping(main);
		createElephantMortality(main);
		createOtherWildlifeCarcass(main);
		createHWC(main);

		createIncidentMappings(main);
		createAttributeMappings(main);
		
		Composite spacer = toolkit.createComposite(main);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)spacer.getLayoutData()).heightHint = 100;
		
		form.reflow(true);
		
		loadCategoriesJob.schedule();
	}
	
	@Override
	public void dispose(){
		super.dispose();

		toolkit.dispose();
	}
	

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void setFocus() {
		
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	private GridLayout createLayout(int cols, boolean same) {
		GridLayout gl = new GridLayout(cols, same);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		return gl;
	}
	
	private void createObservationsMapping(Composite c) {
		Section part = toolkit.createSection(c, Section.TITLE_BAR);
		part.setLayout(createLayout(1, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		part.setText(Messages.MappingPage_WoSectionName);
		
		Composite m = toolkit.createComposite(part);
		m.setLayout(createLayout(2, false));
		m.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		part.setClient(m);
		
		toolkit.createLabel(m, Messages.MappingPage_wocategory);
		cmbWoCat = new ComboViewer(m, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbWoCat.setContentProvider(ArrayContentProvider.getInstance());
		cmbWoCat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbWoCat.setLabelProvider(categoryLabelProvider);
		cmbWoCat.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		cmbWoCat.getControl().setBackground(m.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		List<ComboViewer> attviewer = new ArrayList<>();

		toolkit.createLabel(m, Messages.MappingPage_wospecies);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.WO_SPECIES));
		
		toolkit.createLabel(m, Messages.MappingPage_wonumindiv);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.WO_NUMINDIV));
		
		toolkit.createLabel(m, Messages.MappingPage_wospoorsct);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.WO_SPOORSCT));
		
		cmbWoCat.addSelectionChangedListener(e->{
			Category cat = (Category) cmbWoCat.getStructuredSelection().getFirstElement();
			Attribute none = new Attribute();
			none.setName(Messages.MappingPage_NoneOp);
			List<Attribute> atts = new ArrayList<>();
			atts.add(none);
			cat.getAllAttribute(atts, null);
			
			for (ComboViewer cv: attviewer) {
				cv.setInput(atts);
				cv.setSelection(new StructuredSelection(none));
			}
			
			if (!isInit) editor.getMapping().setField(Field.WO_CATEGORY, cat.getHkey());
		});

	}
	
	private void createElephantMortality(Composite c) {
		
		Section part = toolkit.createSection(c, Section.TITLE_BAR);
		part.setLayout(createLayout(1, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		part.setText(Messages.MappingPage_ElephantMortSection);
		
		Composite m = toolkit.createComposite(part);
		m.setLayout(createLayout(2, false));
		m.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		part.setClient(m);
		
		toolkit.createLabel(m, Messages.MappingPage_emcategory);
		cmbEmCat = new ComboViewer(m, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbEmCat.setContentProvider(ArrayContentProvider.getInstance());
		cmbEmCat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbEmCat.setLabelProvider(categoryLabelProvider);
		cmbEmCat.getControl().setBackground(m.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		cmbEmCat.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		
		List<ComboViewer> attviewer = new ArrayList<>();
		
		toolkit.createLabel(m, Messages.MappingPage_emspecies);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.EM_SEPCIES));
		 
		toolkit.createLabel(m, Messages.MappingPage_emcage);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.EM_CARCASSAGE));
		
		toolkit.createLabel(m, Messages.MappingPage_emsex);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.EM_SEX));
		
		toolkit.createLabel(m, Messages.MappingPage_emage);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.EM_AGE));
		
		toolkit.createLabel(m, Messages.MappingPage_emcauseofdeath);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.EM_CAUSEDEATH));
		
		toolkit.createLabel(m, Messages.MappingPage_emmeansofdeath);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.EM_MEANSDEATH));
		
		toolkit.createLabel(m, Messages.MappingPage_emleftrusk);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.EM_LEFTTUSK));
		
		toolkit.createLabel(m, Messages.MappingPage_emrighttusk);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.EM_RIGHTTUSK));
		
		cmbEmCat.addSelectionChangedListener(e->{
			Category cat = (Category) cmbEmCat.getStructuredSelection().getFirstElement();
			Attribute none = new Attribute();
			none.setName(Messages.MappingPage_NoneOp);
			List<Attribute> atts = new ArrayList<>();
			atts.add(none);
			cat.getAllAttribute(atts, null);
			
			for (ComboViewer cv: attviewer) {
				cv.setInput(atts);
				cv.setSelection(new StructuredSelection(none));
			}
			
			if (!isInit) editor.getMapping().setField(Field.EM_CATEGORY, cat.getHkey());
		});

	}
	
	
	private void createOtherWildlifeCarcass(Composite c) {
		Section part = toolkit.createSection(c, Section.TITLE_BAR);
		part.setLayout(createLayout(1, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		part.setText(Messages.MappingPage_OtherCarcassSection);
		
		Composite m = toolkit.createComposite(part);
		m.setLayout(createLayout(2, false));
		m.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		part.setClient(m);
		
		toolkit.createLabel(m, Messages.MappingPage_occatfield);
		cmbOcCat = new ComboViewer(m, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbOcCat.setContentProvider(ArrayContentProvider.getInstance());
		cmbOcCat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbOcCat.setLabelProvider(categoryLabelProvider);
		cmbOcCat.getControl().setBackground(m.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbOcCat.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		
		List<ComboViewer> attviewer = new ArrayList<>();
		
		toolkit.createLabel(m, Messages.MappingPage_ocspeciesfield);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.OC_SEPCIES));

		toolkit.createLabel(m, Messages.MappingPage_numfield);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.OC_NUMBER));
		
		toolkit.createLabel(m, Messages.MappingPage_cagefield);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.OC_CARCAASSAGE));
		
		toolkit.createLabel(m, Messages.MappingPage_sexfield);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.OC_SEX));
		
		toolkit.createLabel(m, Messages.MappingPage_ageclassfield);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.OC_AGE));
		
		toolkit.createLabel(m, Messages.MappingPage_CauseofDeathfield);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.OC_CAUSEDEATH));
		
		toolkit.createLabel(m, Messages.MappingPage_MeansofDeathField);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.OC_MEANSDEATH));
		
		
		cmbOcCat.addSelectionChangedListener(e->{
			Category cat = (Category) cmbOcCat.getStructuredSelection().getFirstElement();
			Attribute none = new Attribute();
			none.setName(Messages.MappingPage_NoneOp);
			List<Attribute> atts = new ArrayList<>();
			atts.add(none);
			cat.getAllAttribute(atts, null);
			
			for (ComboViewer cv: attviewer) {
				cv.setInput(atts);
				cv.setSelection(new StructuredSelection(none));
			}
			
			if (!isInit) editor.getMapping().setField(Field.OC_CATEGORY, cat.getHkey());
		});
		
	}
	
	private void createHWC(Composite c) {
		
		Section part = toolkit.createSection(c, Section.TITLE_BAR);
		part.setLayout(createLayout(1, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		part.setText(Messages.MappingPage_HWCSection);
		
		Composite m = toolkit.createComposite(part);
		m.setLayout(createLayout(2, false));
		m.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		part.setClient(m);
		
		toolkit.createLabel(m, Messages.MappingPage_CategoryLbl);
		cmbHwcCat = new ComboViewer(m, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbHwcCat.setContentProvider(ArrayContentProvider.getInstance());
		cmbHwcCat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbHwcCat.setLabelProvider(categoryLabelProvider);
		cmbHwcCat.getControl().setBackground(m.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbHwcCat.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		
		List<ComboViewer> attviewer = new ArrayList<>();
		
		toolkit.createLabel(m, Messages.MappingPage_WSpeciesField);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.HWC_SPECIES));
		
		toolkit.createLabel(m, Messages.MappingPage_LivestockField);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.HWC_LIVESTOCK));
		
		toolkit.createLabel(m, Messages.MappingPage_Timefield);
		attviewer.add(createAttributeComboViewer(m, WcommMapping.Field.HWC_TIME));
		
		cmbHwcCat.addSelectionChangedListener(e->{
			Category cat = (Category) cmbHwcCat.getStructuredSelection().getFirstElement();
			Attribute none = new Attribute();
			none.setName(Messages.MappingPage_NoneOp);
			List<Attribute> atts = new ArrayList<>();
			atts.add(none);
			cat.getAllAttribute(atts, null);
			
			for (ComboViewer cv: attviewer) {
				cv.setInput(atts);
				cv.setSelection(new StructuredSelection(none));
			}
			if (!isInit) editor.getMapping().setField(Field.HWC_CATEGORY, cat.getHkey());
		});
	}

	
	private void createIncidentMappings(Composite c) {
		Section part = toolkit.createSection(c, Section.TITLE_BAR);
		part.setLayout(createLayout(1, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		part.setText(Messages.MappingPage_IncidentSection);
		
		Composite m = toolkit.createComposite(part);
		m.setLayout(createLayout(1, false));
		m.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		part.setClient(m);
		
		incidentMappingItems = toolkit.createComposite(m);
		incidentMappingItems.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		incidentMappingItems.setLayout(createLayout(5, false));
		
		Button btnAdd = toolkit.createButton(m, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.addListener(SWT.Selection, e->{
			WcommMapping.IncidentMapping mm = editor.getMapping().addIncidentMapping();
			addIncidentMappingItem(mm);
		});		
	}
	
	private void addIncidentMappingItem(WcommMapping.IncidentMapping mm) {
		
		ComboViewer cmbCategory = new ComboViewer(incidentMappingItems, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbCategory.setContentProvider(ArrayContentProvider.getInstance());
		cmbCategory.setLabelProvider(categoryLabelProvider);
		cmbCategory.setInput(categories);
		cmbCategory.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbCategory.getControl().getLayoutData()).widthHint = 200;
		cmbCategory.getControl().setBackground(cmbCategory.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbCategory.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		
		ComboViewer cmbAttribute = new ComboViewer(incidentMappingItems, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbAttribute.setContentProvider(ArrayContentProvider.getInstance());
		cmbAttribute.setLabelProvider(catAttLblProvider);
		cmbAttribute.setInput(new ArrayList<>());
		cmbAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbAttribute.getControl().getLayoutData()).widthHint = 200;
		cmbAttribute.getControl().setBackground(cmbAttribute.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbAttribute.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		
		ComboViewer cmbItem = new ComboViewer(incidentMappingItems, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbItem.setContentProvider(ArrayContentProvider.getInstance());
		cmbItem.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbItem.getControl().getLayoutData()).widthHint = 200;
		cmbItem.getControl().setBackground(cmbItem.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbItem.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		cmbItem.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AttributeListItem) return ((AttributeListItem)element).getName();
				if (element instanceof AttributeTreeNode) {
					StringBuilder sb = new StringBuilder();
					AttributeTreeNode c = ((AttributeTreeNode)element).getParent();
					while(c != null) {
						sb.append("   "); //$NON-NLS-1$
						c = c.getParent();
					}
					sb.append(((AttributeTreeNode)element).getName());
					if (((AttributeTreeNode)element).getParent() != null) {
						sb.append(" (" + ((AttributeTreeNode)element).getParent().getFullCategoryName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		});		
		
		cmbCategory.addSelectionChangedListener(evt->{
			Category c = (Category) cmbCategory.getStructuredSelection().getFirstElement();
			List<Attribute> all = new ArrayList<>();
			c.getAllAttribute(all, null);
			List<Object> lists = new ArrayList<>();
			lists.add(""); //$NON-NLS-1$
			for (Attribute a : all) if (a.getType() == Attribute.AttributeType.LIST) lists.add(a);
			cmbAttribute.setInput(lists);
			mm.category = c.getHkey();
			try {
				editor.getMapping().save();
			} catch (IOException e) {
				WCommPlugIn.displayLog(e.getMessage(), e);
			}
		});
		
		cmbAttribute.addSelectionChangedListener(ev->{
			Object oo = ev.getStructuredSelection().getFirstElement();
			if (!(oo instanceof Attribute)) {
				if (!isInit) {
					mm.attribute = ""; //$NON-NLS-1$
					mm.item = ""; //$NON-NLS-1$
					try {
						editor.getMapping().save();
					} catch (IOException e) {
						WCommPlugIn.displayLog(e.getMessage(), e);
					}	
				}
				return;
			}
			Attribute x = (Attribute)oo;
			if (!isInit) {
				mm.attribute = x.getKeyId();
				try {
					editor.getMapping().save();
				} catch (IOException e) {
					WCommPlugIn.displayLog(e.getMessage(), e);
				}
			}
			List<Object> kids = new ArrayList<>();
			kids.add(""); //$NON-NLS-1$
			try(Session session = HibernateManager.openSession()){
				Attribute t = session.get(Attribute.class, x.getUuid());
				if (t.getType() == Attribute.AttributeType.LIST) {
					kids.addAll(t.getAttributeList());
				}else {
					Stack<AttributeTreeNode> allitems = new Stack<>();
					allitems.addAll(t.getTree());
					allitems.sort((a,b)->-1*Integer.compare(a.getNodeOrder(), b.getNodeOrder()));
					while(!allitems.isEmpty()) {
						AttributeTreeNode tn = allitems.pop();
						tn.getFullCategoryName();
						tn.getName();
						kids.add(tn);
						
						List<AttributeTreeNode> temp = new ArrayList<>();
						temp.addAll(tn.getChildren());
						temp.sort((a,b)->-1*Integer.compare(a.getNodeOrder(), b.getNodeOrder()));
						allitems.addAll(temp);
					}
				}
			}
			Display.getDefault().syncExec(()->{
				cmbItem.setInput(kids);
				if (mm.item != null) {
					for (Object kd : kids) {
						if (kd instanceof AttributeListItem && ((AttributeListItem)kd).getKeyId().equals(mm.item)) {
							cmbItem.setSelection(new StructuredSelection(kd));
							break;
						}
						if (kd instanceof AttributeTreeNode && ((AttributeTreeNode)kd).getHkey().equals(mm.item)) {
							cmbItem.setSelection(new StructuredSelection(kd));
							break;
						}
					}
				}
			});
		});
		if (mm.category != null) {
			for (Category c : categories) {
				if (c.getHkey().equals(mm.category)) {
					cmbCategory.setSelection(new StructuredSelection(c));
					break;
				}
			}
		}
		if (mm.attribute != null) {
			for (Object a: ((List<?>)cmbAttribute.getInput())) {
				if (a instanceof Attribute && ((Attribute)a).getKeyId().equals(mm.attribute)) {
					cmbAttribute.setSelection(new StructuredSelection(a));
					break;
				}
			}
		}
		
		cmbItem.addSelectionChangedListener(evt->{
			if (isInit) return;
			Object x = evt.getStructuredSelection().getFirstElement();
			if (x instanceof AttributeListItem) {
				mm.item = ((AttributeListItem)x).getKeyId();
			}else if (x instanceof AttributeTreeNode) {
				mm.item = ((AttributeTreeNode)x).getHkey();
			}else {
				mm.item = ""; //$NON-NLS-1$
			}
			try {
				editor.getMapping().save();
			} catch (IOException e1) {
				WCommPlugIn.displayError(e1.getMessage(), e1);
			}
		});
		Text txtValue = new Text(incidentMappingItems, SWT.BORDER);
		txtValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (mm.value != null) txtValue.setText(mm.value);
		txtValue.addListener(SWT.Modify, evt->{
			if (isInit) return;
			mm.value = txtValue.getText();
			try {
				editor.getMapping().save();
			} catch (IOException e1) {
				WCommPlugIn.displayError(e1.getMessage(), e1);
			}
		});
		Button btnDelete = toolkit.createButton(incidentMappingItems, DialogConstants.DELETE_BUTTON_TEXT, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.addListener(SWT.Selection, evt->{
			cmbCategory.getControl().dispose();
			cmbAttribute.getControl().dispose();
			txtValue.dispose();
			cmbItem.getControl().dispose();
			btnDelete.dispose();
			incidentMappingItems.getParent().layout(true);
			form.reflow(true);
			
			
			editor.getMapping().removeIncidentMapping(mm);
			try {
				editor.getMapping().save();
			} catch (IOException e1) {
				WCommPlugIn.displayError(e1.getMessage(), e1);
			}
		});
		
		incidentMappingItems.getParent().layout(true);
		
		form.reflow(true);
	}

	private void createAttributeMappings(Composite c) {
		Section part = toolkit.createSection(c, Section.TITLE_BAR);
		part.setLayout(createLayout(1, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		part.setText(Messages.MappingPage_AttributeSection);
		
		Composite m = toolkit.createComposite(part);
		m.setLayout(createLayout(1, false));
		m.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		part.setClient(m);
		
		attributeMappingItems = toolkit.createComposite(m);
		attributeMappingItems.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		attributeMappingItems.setLayout(createLayout(4, false));
		
		Button btnAdd = toolkit.createButton(m, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.addListener(SWT.Selection, e->{
			WcommMapping.AttributeMapping mm = editor.getMapping().addAttributeMapping();
			addAttributeMappingItem(mm);
		});
		
	}
	
	private void addAttributeMappingItem(WcommMapping.AttributeMapping mm) {
		
		
		ComboViewer cmbAttribute = new ComboViewer(attributeMappingItems, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbAttribute.setContentProvider(ArrayContentProvider.getInstance());
		cmbAttribute.setLabelProvider(catAttLblProvider);
		cmbAttribute.setInput(listtreeattributes);
		cmbAttribute.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbAttribute.getControl().getLayoutData()).widthHint = 200;
		cmbAttribute.getControl().setBackground(cmbAttribute.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbAttribute.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		
		ComboViewer cmbItem = new ComboViewer(attributeMappingItems, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbItem.setContentProvider(ArrayContentProvider.getInstance());
		cmbItem.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbItem.getControl().getLayoutData()).widthHint = 200;
		cmbItem.getControl().setBackground(cmbItem.getControl().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbItem.getControl().addListener(SWT.MouseWheel, e->e.doit = false);
		cmbItem.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AttributeListItem) return ((AttributeListItem)element).getName();
				if (element instanceof AttributeTreeNode) {
					StringBuilder sb = new StringBuilder();
					AttributeTreeNode c = ((AttributeTreeNode)element).getParent();
					while(c != null) {
						sb.append("   "); //$NON-NLS-1$
						c = c.getParent();
					}
					sb.append(((AttributeTreeNode)element).getName());
					if (((AttributeTreeNode)element).getParent() != null) {
						sb.append(" (" + ((AttributeTreeNode)element).getParent().getFullCategoryName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		});		
		cmbAttribute.addSelectionChangedListener(ev->{
			Attribute x = (Attribute) ev.getStructuredSelection().getFirstElement();
			if (!isInit) {
				mm.attribute = x.getKeyId();
				try {
					editor.getMapping().save();
				} catch (IOException e1) {
					WCommPlugIn.displayError(e1.getMessage(), e1);
				}
			}
			List<Object> kids = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				Attribute t = session.get(Attribute.class, x.getUuid());
				if (t.getType() == Attribute.AttributeType.LIST) {
					kids.addAll(t.getAttributeList());
				}else {
					Stack<AttributeTreeNode> allitems = new Stack<>();
					allitems.addAll(t.getTree());
					allitems.sort((a,b)->-1*Integer.compare(a.getNodeOrder(), b.getNodeOrder()));
					while(!allitems.isEmpty()) {
						AttributeTreeNode tn = allitems.pop();
						tn.getFullCategoryName();
						tn.getName();
						kids.add(tn);
						
						List<AttributeTreeNode> temp = new ArrayList<>();
						temp.addAll(tn.getChildren());
						temp.sort((a,b)->-1*Integer.compare(a.getNodeOrder(), b.getNodeOrder()));
						allitems.addAll(temp);
					}
				}
			}
			Display.getDefault().syncExec(()->{
				cmbItem.setInput(kids);
				if (mm.item != null) {
					for (Object kd : kids) {
						if (kd instanceof AttributeListItem && ((AttributeListItem)kd).getKeyId().equals(mm.item)) {
							cmbItem.setSelection(new StructuredSelection(kd));
							break;
						}
						if (kd instanceof AttributeTreeNode && ((AttributeTreeNode)kd).getHkey().equals(mm.item)) {
							cmbItem.setSelection(new StructuredSelection(kd));
							break;
						}
					}
				}
			});
		});
		if (mm.attribute != null) {
			for (Attribute a: listtreeattributes) {
				if (a.getKeyId().equals(mm.attribute)) {
					cmbAttribute.setSelection(new StructuredSelection(a));
					break;
				}
			}
		}
		cmbItem.addSelectionChangedListener(evt->{
			if (isInit) return;
			Object x = evt.getStructuredSelection().getFirstElement();
			if (x instanceof AttributeListItem) {
				mm.item = ((AttributeListItem)x).getKeyId();
			}else if (x instanceof AttributeTreeNode) {
				mm.item = ((AttributeTreeNode)x).getHkey();
			}
			try {
				editor.getMapping().save();
			} catch (IOException e1) {
				WCommPlugIn.displayError(e1.getMessage(), e1);
			}
		});
		Text txtValue = new Text(attributeMappingItems, SWT.BORDER);
		txtValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (mm.value != null) txtValue.setText(mm.value);
		txtValue.addListener(SWT.Modify, evt->{
			if (isInit) return;
			mm.value = txtValue.getText();
			try {
				editor.getMapping().save();
			} catch (IOException e1) {
				WCommPlugIn.displayError(e1.getMessage(), e1);
			}
		});
		Button btnDelete = toolkit.createButton(attributeMappingItems, DialogConstants.DELETE_BUTTON_TEXT, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.addListener(SWT.Selection, evt->{
			cmbAttribute.getControl().dispose();
			txtValue.dispose();
			cmbItem.getControl().dispose();
			btnDelete.dispose();
			attributeMappingItems.getParent().layout(true);
			form.reflow(true);
			
			editor.getMapping().removeAttributeMapping(mm);
			try {
				editor.getMapping().save();
			} catch (IOException e1) {
				WCommPlugIn.displayError(e1.getMessage(), e1);
			}
		});
		
		attributeMappingItems.getParent().layout(true);
		
		form.reflow(true);
	}
	
	private ComboViewer createAttributeComboViewer(Composite c, WcommMapping.Field field) {
		ComboViewer cmb = new ComboViewer(c, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmb.getControl().setBackground(c.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmb.setContentProvider(ArrayContentProvider.getInstance());
		cmb.setLabelProvider(catAttLblProvider);
		cmb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)cmb.getControl().getLayoutData()).widthHint = 400;
		cmb.getControl().setData("FIELD", field); //$NON-NLS-1$
		cmb.addSelectionChangedListener(attListener);
		cmb.getControl().addListener(SWT.MouseWheel, e->e.doit = false);		
		cmbAttributes.add(cmb);
		return cmb;
	}
	
	
	
	private Job loadCategoriesJob = new Job(Messages.MappingPage_jobname) {

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			categories = new ArrayList<>();
			Stack<Category> items = new Stack<>();
			
			HashMap<String,Category> cmapping = new HashMap<>();
			HashMap<String,Attribute> amapping = new HashMap<>();
			try(Session session = HibernateManager.openSession()){
				items.addAll(QueryFactory.buildQuery(session, Category.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"parent", null}).list()); //$NON-NLS-1$
				items.sort((a,b)->-1*Integer.compare(a.getCategoryOrder(), b.getCategoryOrder()));
				while(!items.isEmpty()) {
					Category c = items.pop();
					c.getFullCategoryName();
					c.getName();
					c.getAttributes().forEach(ca->{amapping.put(ca.getAttribute().getKeyId(), ca.getAttribute()); ca.getAttribute().getName();});
					cmapping.put(c.getHkey(), c);
					
					categories.add(c);
					
					List<Category> temp = new ArrayList<>();
					temp.addAll(c.getChildren());
					temp.sort((a,b)->-1*Integer.compare(a.getCategoryOrder(), b.getCategoryOrder()));
					items.addAll(temp);
				}
			}
			
			WcommMapping mapping = editor.getMapping();
			
			Display.getDefault().syncExec(()->{
				try {
					isInit = true;
					
					cmbEmCat.setInput(categories);
					cmbWoCat.setInput(categories);
					cmbOcCat.setInput(categories);
					cmbHwcCat.setInput(categories);
					
					if (mapping.getValue(Field.EM_CATEGORY) != null) cmbEmCat.setSelection(new StructuredSelection(cmapping.get(mapping.getValue(Field.EM_CATEGORY))));
					if (mapping.getValue(Field.OC_CATEGORY) != null) cmbOcCat.setSelection(new StructuredSelection(cmapping.get(mapping.getValue(Field.OC_CATEGORY))));
					if (mapping.getValue(Field.WO_CATEGORY) != null) cmbWoCat.setSelection(new StructuredSelection(cmapping.get(mapping.getValue(Field.WO_CATEGORY))));
					if (mapping.getValue(Field.HWC_CATEGORY) != null) cmbHwcCat.setSelection(new StructuredSelection(cmapping.get(mapping.getValue(Field.HWC_CATEGORY))));
					
					for (ComboViewer cm : cmbAttributes) {
						Field field = (Field) cm.getControl().getData("FIELD"); //$NON-NLS-1$
						String data = mapping.getValue(field);
						if (cm.getInput() == null) continue;
						for (Attribute aa : ((List<Attribute>)cm.getInput())) {
							if (aa.getKeyId() == null && data == null) {
								cm.setSelection(new StructuredSelection(aa));
								break;
							}else if (aa.getKeyId() != null && aa.getKeyId().equals(data)){
								cm.setSelection(new StructuredSelection(aa));
								break;
							}
						}
					}
					for (WcommMapping.AttributeMapping m : mapping.getAttributeMapping()) {
						addAttributeMappingItem(m);
					}
					for (WcommMapping.IncidentMapping m : mapping.getIncidentMapping()) {
						addIncidentMappingItem(m);
					}
				}finally {
					isInit = false;
				}
				
				
			});
			return Status.OK_STATUS;
		}
		
	};
}
