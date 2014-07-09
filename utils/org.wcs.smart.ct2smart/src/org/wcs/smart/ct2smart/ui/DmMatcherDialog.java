package org.wcs.smart.ct2smart.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.ui.support.Ct2AttributeTypeLabelProvider;
import org.wcs.smart.ct2smart.ui.support.Ct2AttributeTypeTableEditor;
import org.wcs.smart.ct2smart.ui.support.SmartAttributeEditingSupport;
import org.wcs.smart.ct2smart.ui.support.SmartAttributeLabelProvider;
import org.wcs.smart.ct2smart.ui.support.SmartCategoryEditingSupport;
import org.wcs.smart.ct2smart.ui.support.SmartCategoryLabelProvider;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.LanguageType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class DmMatcherDialog extends Composite {

	private MatchSession session;
	
	private TableViewer viewer;
	private Combo langSelector;
	private DataModelLookup dmLookup;
	private MatchAttributeComposite infoComposite;
	private CategoryMapComposite categoryComposite;
	
	private List<ILanguageChangedListener> langListeners = new ArrayList<ILanguageChangedListener>();
	
	public DmMatcherDialog(Composite c, MatchSession session) {
		super(c, SWT.NONE);
		this.session = session;
		dmLookup = new DataModelLookup(session.getDataModel());

		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.setLayoutData(gridData);

		this.setSize(840, 640);

		//main composite and layout
		final Composite main = new Composite(this, SWT.NONE);
		GridLayout mlayout = new GridLayout(1, true);
		main.setLayout(mlayout);
		GridData mainGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		main.setLayoutData(mainGridData);

		//language selector
		final Composite langCmp = new Composite(main, SWT.NONE);
		langCmp.setLayout(new GridLayout(2, false));
		langCmp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true));

		Label languageLabel = new Label(langCmp, SWT.NONE);
		languageLabel .setText("Language:" );
		GridData langLabelData = new GridData(SWT.RIGHT,SWT.TOP, false, false);
		languageLabel.setLayoutData(langLabelData);

		List<LanguageType> languages = session.getDataModel().getLanguages().getLanguages();
		String[] langCodes = new String[languages.size()];
		for (int i=0; i < languages.size(); i++){
			langCodes[i] = languages.get(i).getCode();
		}
		
		langSelector =  new Combo (langCmp, SWT.READ_ONLY);
		langSelector.setItems(langCodes);
		langSelector.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		langSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				languageChanged();
			}
		});
		
		//attributes table
		final Composite left = new Composite(main, SWT.NONE);
		left.setLayout(new GridLayout(1, false));
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer = new TableViewer(left, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		// create the columns 
		createColumns();

		// make lines and header visible
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true); 

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(session.getCt2Smart().getCt2Attribute());

		// define layout for the viewer
		GridData tgridData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 0);
		//tgridData.widthHint = 510;
		tgridData.heightHint = 350;
		viewer.getControl().setLayoutData(tgridData);

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {		
				viewerSelectionChanged();
			}
		});

		infoComposite = new MatchAttributeComposite(left, dmLookup);
		addLanguageChangedListener(infoComposite);

		categoryComposite = new CategoryMapComposite(left, dmLookup, session.getCt2Smart());
		addLanguageChangedListener(categoryComposite);
		
		langSelector.select(0); //to select default language and fire all listeners
	}

	protected void languageChanged() {
		String langCode = langSelector.getItems()[langSelector.getSelectionIndex()];
		for (ILanguageChangedListener listener : langListeners) {
			listener.languageChanged(langCode);
		}
		viewer.refresh();
	}

	protected void addLanguageChangedListener(ILanguageChangedListener listener) {
		langListeners.add(listener);
	}
	
	protected void viewerSelectionChanged() {
		Object obj = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (obj instanceof Ct2Attribute) {
			Ct2Attribute a = (Ct2Attribute)obj;
			infoComposite.setInput(a);
			if (Ct2AttributeType.CATEGORY.equals(a.getType())) {
				categoryComposite.setInput(session.getCt2Smart());
			} else {
				//TODO: hide
			}
		}
	}

	private void createColumns() {

		TableViewerColumn col = createTableViewerColumn("CyberTracker Attribute", 200);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Ct2Attribute a = (Ct2Attribute) element;
				return a.getN();
			}
		});

		col = createTableViewerColumn("Type", 80);
		col.setLabelProvider(new Ct2AttributeTypeLabelProvider());
		col.setEditingSupport(new Ct2AttributeTypeTableEditor(viewer));

		col = createTableViewerColumn("SMART Attribute", 200);
		SmartAttributeLabelProvider attrLabelProvider = new SmartAttributeLabelProvider(dmLookup);
		col.setLabelProvider(attrLabelProvider);
		addLanguageChangedListener(attrLabelProvider);
		List<AttributeType> attributes = session.getDataModel().getAttributes().getAttributes();
		col.setEditingSupport(new SmartAttributeEditingSupport(viewer, attributes, attrLabelProvider));

		col = createTableViewerColumn("SMART Category", 200);
		SmartCategoryLabelProvider catLabelProvider = new SmartCategoryLabelProvider(dmLookup);
		col.setLabelProvider(catLabelProvider);
		addLanguageChangedListener(catLabelProvider);
		col.setEditingSupport(new SmartCategoryEditingSupport(viewer, dmLookup, catLabelProvider));
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

}
