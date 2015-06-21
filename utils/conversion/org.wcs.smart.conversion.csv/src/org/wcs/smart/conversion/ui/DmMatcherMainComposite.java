package org.wcs.smart.conversion.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.conversion.csv.handler.ProcessingActionHandler;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.conversion.tool.CleanMappingTool;
import org.wcs.smart.conversion.tool.MatchSession;
import org.wcs.smart.conversion.ui.support.Ct2AttributeTypeLabelProvider;
import org.wcs.smart.conversion.ui.support.Ct2AttributeTypeTableEditor;
import org.wcs.smart.conversion.ui.support.SmartAttributeEditingSupport;
import org.wcs.smart.conversion.ui.support.SmartAttributeLabelProvider;
import org.wcs.smart.conversion.ui.support.SmartCategoryEditingSupport;
import org.wcs.smart.conversion.ui.support.SmartCategoryLabelProvider;
import org.wcs.smart.conversion.util.Ct2AttributeTypeUtil;
import org.wcs.smart.conversion.util.FileUtil;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.LanguageType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class DmMatcherMainComposite extends Composite {

	private MatchSession session;
	
	private TableViewer viewer;
	private Combo langSelector;
	private DataModelLookup dmLookup;
	
	private Composite innerPanel;
	private MatchAttributeComposite infoComposite;
	private CategoryMapComposite categoryComposite;
	
	private List<ILanguageChangedListener> langListeners = new ArrayList<ILanguageChangedListener>();
	
	private ProcessingActionHandler handler;
	
	public DmMatcherMainComposite(Composite c, MatchSession session) {
		super(c, SWT.NONE);
		this.session = session;
		dmLookup = new DataModelLookup(session.getDataModel());
		handler = new ProcessingActionHandler(getShell()) {
			@Override
			protected MatchSession createMatchSession() throws JAXBException, IOException {
				return DmMatcherMainComposite.this.session;
			}
		};

		GridLayout layout = new GridLayout(1, false);
		this.setLayout(layout);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 1200;
		this.setLayoutData(gridData);

		this.setSize(840, 640);

		//main composite and layout
		final Composite main = new Composite(this, SWT.NONE);
		GridLayout mlayout = new GridLayout(1, true);
		main.setLayout(mlayout);
		GridData mainGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		main.setLayoutData(mainGridData);

		Composite top = new Composite(main, SWT.NONE);
		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom = 0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		top.setLayout(gd);
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite buttonsCmp = new Composite(top, SWT.NONE);
		buttonsCmp.setLayout(new GridLayout(5, false));
		buttonsCmp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		
		Button btnSave = new Button(buttonsCmp, SWT.PUSH);
		btnSave.setText("Save mapping");
		btnSave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performSave();
			}
		});
		
		Button btnValidate = new Button(buttonsCmp, SWT.PUSH);
		btnValidate.setText("Validate mapping");
		btnValidate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handler.validateMapping();
			}
		});

		Button btnMeta = new Button(buttonsCmp, SWT.PUSH);
		btnMeta.setText("Generate metadata");
		btnMeta.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handler.generateMeta();
			}
		});

		Button btnGenPatrol = new Button(buttonsCmp, SWT.PUSH);
		btnGenPatrol.setText("Generate patrols");
		btnGenPatrol.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handler.generatePatrols();
			}
		});

		Button btnGenMission = new Button(buttonsCmp, SWT.PUSH);
		btnGenMission.setText("Generate missions");
		btnGenMission.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handler.generateMissions();
			}
		});
		
		
		//language selector
		final Composite langCmp = new Composite(top, SWT.NONE);
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
		
		langSelector =  new Combo(langCmp, SWT.READ_ONLY);
		langSelector.setItems(langCodes);
		langSelector.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		langSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				languageChanged();
			}
		});
		
		//attributes table
		viewer = new TableViewer(main, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		createColumns();

		// make lines and header visible
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true); 

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(session.getSmartMapping().getMappedAttribute());

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

		innerPanel = new Composite(main, SWT.NONE);
		innerPanel.setLayout(new StackLayout());
		innerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		infoComposite = new MatchAttributeComposite(innerPanel, dmLookup, session.getConnection());
		addLanguageChangedListener(infoComposite);

		categoryComposite = new CategoryMapComposite(innerPanel, dmLookup, session.getSmartMapping());
		addLanguageChangedListener(categoryComposite);
		
		langSelector.select(0); //to select default language and fire all listeners
	}

	protected void performSave() {
		FileDialog dlg = new FileDialog(getShell(), SWT.SAVE);
		dlg.setFilterNames(new String[] {"XML file"});
		dlg.setFilterExtensions(new String[] {"*.xml"}); //$NON-NLS-1$
		String fn = dlg.open();
		if (fn != null) {
			if (!fn.endsWith(".xml")) { //$NON-NLS-1$
				fn += ".xml"; //$NON-NLS-1$
			}
			try {
				SmartMapping smartMapping = session.getSmartMapping();
				CleanMappingTool cleanTool = new CleanMappingTool();
				cleanTool.clean(smartMapping);
				FileUtil.write(new File(fn), smartMapping);
			} catch (Exception e) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Error occured. Failed to save mapping. See console for details.");
				e.printStackTrace();
			}
		}
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
		if (obj instanceof MappedAttribute) {
			MappedAttribute a = (MappedAttribute)obj;
			StackLayout stackLayout = ((StackLayout)innerPanel.getLayout());
			if (MappedAttributeType.CATEGORY.equals(a.getType())) {
				categoryComposite.setInput(session.getSmartMapping());
				stackLayout.topControl = categoryComposite;
			} else {
				infoComposite.setInput(a);
				stackLayout.topControl = infoComposite;
			}
			innerPanel.layout();
		}
	}

	private void createColumns() {
		TableViewerColumn col = createTableViewerColumn("CyberTracker Attribute", 220);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				MappedAttribute a = (MappedAttribute) element;
				return Ct2AttributeTypeUtil.getN(a);
			}
		});

		col = createTableViewerColumn("Type", 100);
		col.setLabelProvider(new Ct2AttributeTypeLabelProvider());
		col.setEditingSupport(new Ct2AttributeTypeTableEditor(viewer));

		col = createTableViewerColumn("SMART Attribute", 220);
		SmartAttributeLabelProvider attrLabelProvider = new SmartAttributeLabelProvider(dmLookup);
		col.setLabelProvider(attrLabelProvider);
		addLanguageChangedListener(attrLabelProvider);
		List<AttributeType> attributes = session.getDataModel().getAttributes().getAttributes();
		col.setEditingSupport(new SmartAttributeEditingSupport(viewer, attributes, attrLabelProvider));

		col = createTableViewerColumn("SMART Category", 220);
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
