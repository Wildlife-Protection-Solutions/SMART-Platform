package org.wcs.smart.i2.ui.dialogs;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelPermission;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.EmployeeSelectDialog;
import org.wcs.smart.ui.SectionHeader;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;

public class ProfileDialog extends SmartStyledDialog {

	private static final String COLOR_KEY = "COLOR"; //$NON-NLS-1$

	
	private IntelProfile config;
	private List<IntelProfile> others;
	
	private SectionHeader sections;
	private Composite stackPanel;
	private ScrolledComposite scrollComp;
	
	private Composite configComp;
	private Composite permissionComp;
	private Composite entityTypeComp;
	private Composite recordSourceComp;
	
	private Composite header; 
	private Label headerLbl;
	
	private boolean isDirty = false;
	
	private Text txtName;
	private Text txtKey;
	private Label btnColor;
	private CheckboxTableViewer tblEntityTypes;
	private CheckboxTableViewer tblRecordSources;
	
	private List<IntelPermission> permissions = new ArrayList<>();
	private Composite permissionTable;
	
	private Listener generateKeyListener = e->{
		String newKey = DataModelManager.INSTANCE.generateKey(txtName.getText(), others);
		txtKey.setText(newKey);
	};
	
	public ProfileDialog(Shell parent, IntelProfile config, List<IntelProfile> others) {
		super(parent);
		this.config = config;
		this.others = others;
	}
	
	@Override
	public void cancelPressed() {
		if (isDirty) {
			MessageDialog md = new MessageDialog(getShell(), "Save Changes?", null, 
					"There are unsaved changes.  Would you like to save the changes before closing?", 
					MessageDialog.QUESTION_WITH_CANCEL, 0,
					new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL});
			int r = md.open();
			if (r == 0) {
				//yes
				okPressed();
			}else if (r == 1) {
				//no
			}else if (r == 2) {
				//cancel
				return;
			}
		}
		super.cancelPressed();
	}
	
	public void okPressed() {
		if (txtName.getText().length() > DmObject.MAX_NAME_LENGTH) {
			MessageDialog.openError(getShell(), "Name Error",  MessageFormat.format("Name must be fewer than {0} characters", DmObject.MAX_NAME_LENGTH));
			return;
		}
		config.setName(txtName.getText());
		config.updateName(SmartDB.getCurrentLanguage(), txtName.getText());
		config.setKeyId(txtKey.getText());
		Color c = ((Color)btnColor.getData(COLOR_KEY));
		config.setColorObj( c == null ? null : (new java.awt.Color (c.getRed(), c.getGreen(), c.getBlue())) );
		
		if (config.getKeyId().isBlank()) {
			config.setKeyId(DataModelManager.INSTANCE.generateKey(config.getName(), others));
			txtKey.setText(config.getKeyId());
		}
		
		Set<IntelEntityType> types = new HashSet<>();
		for (Object x : tblEntityTypes.getCheckedElements()) {
			if (x instanceof IntelEntityType) types.add((IntelEntityType)x);				
		}
		Set<IntelEntityType> d = new HashSet<>();
		for (IntelEntityType x : config.getEntityTypes()) {
			if (!types.contains(x))  d.add(x);
			if (types.contains(x)) types.remove(x);
		}
		for (IntelEntityType dd : d) {
			config.getEntityTypes().remove(dd);
			dd.getProfiles().remove(config);
		}
		for (IntelEntityType dd : types) {
			config.getEntityTypes().add(dd);
			dd.getProfiles().add(config);
		}
		types.addAll(d);
		
		
		Set<IntelRecordSource> rtypes = new HashSet<>();
		for (Object x : tblRecordSources.getCheckedElements()) {
			if (x instanceof IntelRecordSource) rtypes.add((IntelRecordSource)x);				
		}
		Set<IntelRecordSource> rs = new HashSet<>();
		for (IntelRecordSource x : config.getRecordSources()) {
			if (!rtypes.contains(x))  rs.add(x);
			if (rtypes.contains(x)) types.remove(x);
		}
		for (IntelRecordSource dd : rs) {
			config.getEntityTypes().remove(dd);
			dd.getProfiles().remove(config);
		}
		for (IntelRecordSource dd : rtypes) {
			config.getRecordSources().add(dd);
			dd.getProfiles().add(config);
		}
		rtypes.addAll(rs);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(config);
				for (IntelEntityType t : types)  session.saveOrUpdate(t);	
				for (IntelRecordSource t : rtypes)  session.saveOrUpdate(t);			

				session.getTransaction().commit();
			}catch (Exception ex) {
				if (session.getTransaction().isActive()) {
					try {
						session.getTransaction().rollback();
					}catch (Exception ex2) {
						Intelligence2PlugIn.log(ex2.getMessage(), ex2);
					}
				}
				Intelligence2PlugIn.displayLog("Could not save changes to profile." + "\n\n" + ex.getMessage(), ex);
				return;
			}
		}
		setDirty(false);
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.x < 400) p.x = 400;
		if (p.y < 400) p.y = 400;
		return p;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).verticalSpacing = 0;
		
		header = new Composite(parent, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT ));
		WidgetElement.setCSSClass(header, "customcolor"); //$NON-NLS-1$

		
		Composite temp = new Composite(header, SWT.NONE);
		temp.setLayout(new GridLayout());
		temp.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT ));
		
		headerLbl = new Label(temp, SWT.NONE);
		
		headerLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		headerLbl.setFont(JFaceResources.getBannerFont());
		headerLbl.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT ));

		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sections = new SectionHeader(part, SWT.NONE, 
				new String[] {"Configuration", "Entity Types", "Record Source", "Permissions"},
				new Listener[] {
						e->selectConfigPage(),
						e->selectEntityTypePage(),
						e->selectRecordSourcePage(),
						e->selectPermissionPage()
				});
		sections.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		
		scrollComp = new ScrolledComposite(part, SWT.V_SCROLL | SWT.H_SCROLL );
		scrollComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// always show the focus control
		scrollComp.setShowFocusedControl(true);
		scrollComp.setExpandHorizontal(true);
		scrollComp.setExpandVertical(true);
		
		stackPanel = new Composite(scrollComp, SWT.NONE);

		StackLayout layout = new StackLayout();
		layout.marginHeight = 2;
		stackPanel.setLayout(layout);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		configComp = new Composite(stackPanel, SWT.NONE);
		entityTypeComp = new Composite(stackPanel, SWT.NONE);
		recordSourceComp = new Composite(stackPanel, SWT.NONE);
		permissionComp = new Composite(stackPanel, SWT.NONE);
		
		createEntityTypeComp();
		createRecordSourceComp();
		createConfigComp();
		createPermissionComp();
		
		scrollComp.setContent(stackPanel);
		scrollComp.setMinSize(stackPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		sections.selectPanel(0);
		
		initData.schedule();
		return parent;
	}

	

	
	private void selectConfigPage() {
		((StackLayout)stackPanel.getLayout()).topControl = configComp;
		stackPanel.layout(true);
	}
	private void selectPermissionPage() {
		((StackLayout)stackPanel.getLayout()).topControl = permissionComp;
		stackPanel.layout(true);
	}
	private void selectEntityTypePage() {
		((StackLayout)stackPanel.getLayout()).topControl = entityTypeComp;
		stackPanel.layout(true);
	}
	private void selectRecordSourcePage() {
		((StackLayout)stackPanel.getLayout()).topControl = recordSourceComp;
		stackPanel.layout(true);
	}
	
	private void createEntityTypeComp() {
		entityTypeComp.setLayout(new GridLayout());
		
		tblEntityTypes = CheckboxTableViewer.newCheckList(entityTypeComp, SWT.BORDER | SWT.FULL_SELECTION);
		tblEntityTypes.setContentProvider(ArrayContentProvider.getInstance());
		tblEntityTypes.setLabelProvider(new EntityTypeLabelProvider());
		tblEntityTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblEntityTypes.addSelectionChangedListener(e->modified());
		tblEntityTypes.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(tblEntityTypes));
	}
		
	private void createRecordSourceComp() {
		recordSourceComp.setLayout(new GridLayout());
		
		tblRecordSources = CheckboxTableViewer.newCheckList(recordSourceComp, SWT.BORDER  | SWT.FULL_SELECTION);
		tblRecordSources.setContentProvider(ArrayContentProvider.getInstance());
		tblRecordSources.setLabelProvider(new RecordSourceLabelProvider());
		tblRecordSources.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblRecordSources.addSelectionChangedListener(e->modified());
		tblRecordSources.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(tblRecordSources));
	}
	
	private void createConfigComp() {
		configComp.setLayout(new GridLayout(3, false));
		
		Label l = new Label(configComp, SWT.NONE);
		l.setText("Name:");
		
		txtName = new Text(configComp, SWT.BORDER);
		txtName.addListener(SWT.Modify, e->modified());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btn = createButton(configComp, "Translate", SmartPlugIn.EDIT_ICON);
		btn.addListener(SWT.Selection, e->{
			config.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), txtName.getText());
			TranslateSimpleListItemDialog d = new TranslateSimpleListItemDialog(getShell(), config);
			if (d.open() == TranslateSimpleListItemDialog.OK){
				txtName.setText(config.findName(SmartDB.getCurrentLanguage()));
				modified();
			}
		});
		
		l = new Label(configComp, SWT.NONE);
		l.setText("Key:");
		
		txtKey = new Text(configComp, SWT.BORDER);
		txtKey.setEnabled(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		btn = createButton(configComp, DialogConstants.EDIT_BUTTON_TEXT, SmartPlugIn.EDIT_ICON);
		btn.addListener(SWT.Selection, e->{
			if (!MessageDialog.openConfirm(
							getShell(),
							"Set Key",
							"Keys should not be changed unless you understand the implications.  The keys of the data model effect reporting across multiple conservation areas.  Are you sure you want to continue?")) {
				return;
			}
			
			KeyInputDialog id = new KeyInputDialog(getShell(), txtKey.getText(), others);
			int ret = id.openNoWarning();
			if (ret != Window.CANCEL) {
				txtKey.setText(id.getValue());
				txtName.removeListener(SWT.KeyUp,generateKeyListener);
				modified();
			}

		});

		l = new Label(configComp, SWT.NONE);
		l.setText("Color:");
		
		Composite ctemp = new Composite(configComp, SWT.NONE);
		ctemp.setLayout(new GridLayout(2, false));
		((GridLayout)ctemp.getLayout()).marginWidth = 0;
		((GridLayout)ctemp.getLayout()).marginHeight = 0;
		ctemp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		btnColor = new Label(ctemp, SWT.NONE);
		btnColor.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		((GridData)btnColor.getLayoutData()).widthHint = 30;
		btnColor.addListener(SWT.Dispose, e->disposeColor(btnColor));		
		btnColor.addListener(SWT.Paint, e->{
			if (btnColor.getData(COLOR_KEY) != null) e.gc.drawRectangle(0, 0, btnColor.getBounds().width-1, btnColor.getBounds().height-1);
		});
		WidgetElement.setCSSClass(btnColor, "customcolor"); //$NON-NLS-1$
		
		Button btnSetTrackColor = new Button(ctemp, SWT.PUSH);
		btnSetTrackColor.setText("..."); //$NON-NLS-1$
		btnSetTrackColor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Listener changeColorTrack = e->{
			ColorDialog cd = new ColorDialog(getShell());
			cd.setRGB(btnColor.getBackground().getRGB());
			cd.setText("Profile Color");
			RGB rgb = cd.open();
			if (rgb == null) return;
			
			disposeColor(btnColor);
			Color newColor = new Color(getShell().getDisplay(), rgb);
			btnColor.setData(COLOR_KEY, newColor);
			btnColor.setBackground(newColor);
			header.setBackground(newColor);
			modified();
		};
		
		btnSetTrackColor.addListener(SWT.Selection, changeColorTrack);
		btnColor.addListener(SWT.MouseDoubleClick, changeColorTrack);
	}
	

	
	private void createPermissionComp() {
		permissionComp.setLayout(new GridLayout());
		
		Composite outer = new Composite(permissionComp, SWT.NONE);
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight= 0;
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnAdd = new Button(outer, SWT.PUSH);
		btnAdd.setText("Add Employees");
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.addListener(SWT.Selection, e->{
			
			EmployeeSelectDialog dialog = new EmployeeSelectDialog(getShell());
			if (dialog.open() != Window.OK) return;
			for (Employee emp : dialog.getSelectedEmployees()) {
				
				boolean add = true;
				for (IntelPermission p : permissions) {
					if (p.getEmployee().equals(emp)) {
						add = false;
						break;
					}
				}
				if (!add) continue;
				
				IntelPermission ip = new IntelPermission();
				ip.setProfile(config);
				ip.setEntityType(emp);
				ip.setPermission(IntelPermission.READ_ONLY);
				
				permissions.add(ip);
			}
			updatePermissionList();
		});
		
		permissionTable = new Composite(permissionComp, SWT.NONE);
		permissionTable.setLayout(new GridLayout(  13, false ));
		((GridLayout)permissionTable.getLayout()).marginWidth = 0;
		((GridLayout)permissionTable.getLayout()).marginHeight = 0;
		((GridLayout)permissionTable.getLayout()).horizontalSpacing = 0;
		((GridLayout)permissionTable.getLayout()).verticalSpacing = 0;
		permissionTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		permissionTable.addListener(SWT.Resize, e->layout());
		
		Composite headComp = new Composite(permissionTable, SWT.NONE);
		headComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		String headers1[] = new String[] {"Employee", "Admin", "ReadOnly", "Entity", "Record", "Query"};
		Control[] groupheader = new Control[headers1.length];
		for (int i = 0; i < headers1.length; i ++) {
			Composite c = new Composite(headComp, SWT.BORDER);
			c.setLayout(new GridLayout());
			Label l = new Label(c, SWT.NONE);
			l.setText(headers1[i]);
			l.setToolTipText(headers1[i]);
			l.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
			groupheader[i] = c;
		}

		String headers[] = new String[] {"", "", "", "Create", "Delete", "View", "Edit", "Create", "Delete", "View", "Edit", "Edit Status", "All"};
		Control[] ass = new Control[] {groupheader[0], groupheader[1], groupheader[2], groupheader[3], null, null, null, groupheader[4], null, null, null, null, groupheader[5]};
		Integer[] rowspan = new Integer[] {1,1,1,4,null, null, null, 5,null, null, null, null, 1};
		for (int i = 0; i < headers.length; i ++) {
			Composite c = new Composite(headComp, SWT.BORDER);
			c.setLayout(new GridLayout());
			Label l = new Label(c, SWT.NONE);
			l.setText(headers[i]);
			l.setToolTipText(headers[i]);
			l.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
			columnSize.put(i,new Object[] {c,ass[i], rowspan[i]});
		}
		
		updatePermissionList();
	}
	
	private HashMap<Integer,Object[]> columnSize = new HashMap<>();
	private ScrolledComposite userscroll;
	private Composite permComp;
	
	private void updatePermissionList() {
		if (userscroll != null) userscroll.dispose();
		
		userscroll = new ScrolledComposite(permissionTable, SWT.V_SCROLL );
		userscroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 13, 1));
		userscroll.setAlwaysShowScrollBars(true);
		
		permComp = new Composite(userscroll, SWT.NONE);
		permComp.setLayout(new GridLayout(13, false));
		((GridLayout)permComp.getLayout()).marginWidth = 0;
		((GridLayout)permComp.getLayout()).marginHeight = 0;
		((GridLayout)permComp.getLayout()).horizontalSpacing = 0;
		((GridLayout)permComp.getLayout()).verticalSpacing = 0;
		
		List<Employee> emps = new ArrayList<>(permissions.stream().map(p->p.getEmployee()).collect(Collectors.toSet()));
		emps.sort((a,b)->Collator.getInstance().compare( SmartLabelProvider.getShortLabel(a), SmartLabelProvider.getShortLabel(b)));
		
		List<Row> allRows = new ArrayList<>();
		for (Employee e : emps) {
			Row r = new Row(e, allRows);
			r.createRow(permComp);
			allRows.add(r);
		}
		
		SmartUiUtils.makeTransparent(permissionTable);
		permissionTable.layout(true);

		userscroll.setContent(permComp);
		permComp.setSize(permissionTable.getBounds().width-15, permComp.computeSize(permissionTable.getBounds().width-15, SWT.DEFAULT).y);
		
		layout();
	}
	
	private void layout() {
		if(permComp == null) return;
		if (permComp.isDisposed()) return;
		permComp.setSize(permissionTable.getBounds().width-15, permComp.computeSize(permissionTable.getBounds().width-15, SWT.DEFAULT).y);
		
		if (permComp.getChildren().length == 0) return;
		int xs = 0;
		for (int i = 0; i < 13; i ++) {
			Object[] its  = columnSize.get(i);
			
			int x = permComp.getChildren()[i].getBounds().width;
			
			Control single = (Control) its[0];
			Control multi = (Control) its[1];
			Integer rspan = (Integer) its[2];
			
			Rectangle r = single.getBounds();
			r.width = x;
			r.x = xs;
			r.height = permComp.getChildren()[i].getBounds().height;
			r.y = r.height;
			
			
			single.setBounds(r);
			
			if (multi != null) {
				Rectangle r2 = multi.getBounds();
				r2.width = x * rspan;
				r2.x = xs;
				r2.y = 0;
				r2.height = permComp.getChildren()[i].getBounds().height;

				multi.setBounds(r2);
			}
			xs+= r.width;
			
		}
		((Control)columnSize.get(0)[0]).getParent().layout();
	}
	
	private void modified() {
		setDirty(true);
	}
	
	private void setDirty(boolean dirty) {
		this.isDirty = dirty;
		getButton(IDialogConstants.OK_ID).setEnabled(isDirty);
	}
	
	private Button createButton(Composite parent, String text, String icon) {
		Button btn = new Button(parent, SWT.PUSH);
		if (icon != null) btn.setImage(SmartPlugIn.getDefault().getImageRegistry().get(icon));
		btn.setText(text);
		btn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btn.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		return btn;
	}
	
	private void disposeColor(Label label) {
		Color c = (Color) label.getData(COLOR_KEY);
		if (c != null) {
			c.dispose();
		}
		label.setData(COLOR_KEY, null);
	}
	
	private Job initData = new Job("load profile") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelEntityType> types = new ArrayList<>();
			List<IntelRecordSource> rtypes = new ArrayList<>();
			
			permissions = new ArrayList<>();
			
			try(Session s =  HibernateManager.openSession()){
				if (config.getUuid() != null) {
					config = s.get(IntelProfile.class, config.getUuid());
					config.getEntityTypes().forEach(e->e.getName());
					config.getRecordSources().forEach(r->r.getName());
					config.getNames().size();
					
				}
				types.addAll( EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea()) );
				types.forEach(t->t.getProfiles().size());
				
				rtypes.addAll( RecordManager.INSTANCE.getSources( s ) );
				rtypes.forEach(t->t.getProfiles().size());
				
				permissions.addAll(QueryFactory.buildQuery(s, IntelPermission.class, 
						new Object[] {"id.employee.conservationArea", SmartDB.getCurrentConservationArea()}).list());
			}
			
			
			if (config.getEntityTypes() == null) config.setEntityTypes(new HashSet<>());
			if (config.getRecordSources() == null) config.setRecordSources(new HashSet<>());
			
			getShell().getDisplay().syncExec(()->{
				headerLbl.setText(config.getName());
				txtName.setText(config.getName() == null ? "" : config.getName());
				txtKey.setText(config.getKeyId() == null ? "" : config.getKeyId());

				
				tblRecordSources.setInput(rtypes);
				for (IntelRecordSource t : config.getRecordSources()) {
					tblRecordSources.setChecked(t, true);
				}
				
				tblEntityTypes.setInput(types);
				for (IntelEntityType t : config.getEntityTypes()) {
					tblEntityTypes.setChecked(t, true);
				}
				if (config.getKeyId() == null) {
					txtName.addListener(SWT.KeyUp,generateKeyListener);
					config.setKeyId(DataModelManager.INSTANCE.generateKey(txtName.getText(), others));
				}
				if (config.getColor() != null) {
					java.awt.Color c = config.getColorObj();
					Color newColor = new Color(getShell().getDisplay(), c.getRed(), c.getGreen(), c.getBlue());
					btnColor.setData(COLOR_KEY, newColor);
					btnColor.setBackground(newColor);
					header.setBackground(newColor);
				}
				updatePermissionList();
				header.layout(true);
				setDirty(false);
				getShell().setText(config.getName());
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private class Row{
		
		private boolean selected = false;
		private Set<Composite> controls;
		private Employee employee;
		
		private List<Row> allRows;
		
		public Row(Employee e, List<Row> allRows) {
			this.employee = e;
			this.allRows = allRows;
			controls = new HashSet<>();
		}
		
		public void createRow(Composite parent) {
			
			Composite t = new Composite(parent, SWT.BORDER);
			t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			t.setLayout(new GridLayout());
			controls.add(t);
			
			Label ll = new Label(t, SWT.NONE);
			ll.setText(SmartLabelProvider.getShortLabel(employee));
			ll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

			
			Composite btnAdmin = createCheckBox(parent);
			controls.add(btnAdmin);
			Composite btnReadOnly = createCheckBox(parent);
			controls.add(btnReadOnly);
			Composite btnEntityCreate = createCheckBox(parent);
			controls.add(btnEntityCreate);
			Composite btnEntityDelete = createCheckBox(parent);
			controls.add(btnEntityDelete);
			Composite btnEntityView = createCheckBox(parent);
			controls.add(btnEntityView);
			Composite btnEntityEdit = createCheckBox(parent);
			controls.add(btnEntityEdit);
			Composite btnRecordCreate = createCheckBox(parent);
			controls.add(btnRecordCreate);
			Composite btnRecordDelete = createCheckBox(parent);
			controls.add(btnRecordDelete);
			Composite btnRecordView = createCheckBox(parent);
			controls.add(btnRecordView);
			Composite btnRecordEdit = createCheckBox(parent);
			controls.add(btnRecordEdit);
			Composite btnRecordEditStatus = createCheckBox(parent);
			controls.add(btnRecordEditStatus);
			Composite btnRecordQuery = createCheckBox(parent);
			controls.add(btnRecordQuery);
					
			Button ba = (Button) btnAdmin.getChildren()[0];
			
			ba.addListener(SWT.Selection,e->{
				for (Composite cc : controls) {
					if (cc.getChildren()[0] instanceof Button) {
						cc.getChildren()[0].setEnabled(!ba.getSelection());
					}
					ba.setEnabled(true);
				}
			});
			
			Button ro = (Button) btnReadOnly.getChildren()[0];
			ro.addListener(SWT.Selection,e->{
				for (Composite cc : controls) {
					if (cc.getChildren()[0] instanceof Button) {
						cc.getChildren()[0].setEnabled(!ro.getSelection());
					}
					ro.setEnabled(true);
				}
			});
			
			Listener l = evt->{
				for (Row r : allRows) {
					if (r == Row.this) continue;
					r.setSelected(false);
				}
				setSelected(true);
				parent.layout(true);
			};
			t.addListener(SWT.MouseUp, l);
			ll.addListener(SWT.MouseUp, l);
		}
		
		public void setSelected(boolean isSelected) {
			if (this.selected == isSelected) return;
			this.selected = isSelected;
			if (isSelected) {
				for (Control c : controls) c.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
			}else {
				for (Control c : controls) c.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			}
		}
	}
	
	private Composite createCheckBox(Composite parent) {
		Composite t = new Composite(parent, SWT.BORDER);
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		t.setLayout(new GridLayout());
		Button btnAdmin = new Button(t, SWT.CHECK);
		btnAdmin.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		return t;
	}
}
