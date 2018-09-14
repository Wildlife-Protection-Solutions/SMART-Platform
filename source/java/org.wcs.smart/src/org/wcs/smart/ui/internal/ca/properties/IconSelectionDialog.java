package org.wcs.smart.ui.internal.ca.properties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

public class IconSelectionDialog extends TitleAreaDialog {

	private static final int SIZE = 32;
	
	private Composite stackPanel;
	
	private Composite iconLibraryPanel;
	private Composite importPanel;
	
	private TableViewer tblIcons;
	private Button btnLibrary;
	private Button btnImport;
	
	private Icon selectedIcon;
	private Text txtName;
	
	private HashMap<IconSet, Label> imports;
	private List<Icon> icons;
	
	public IconSelectionDialog(Shell parentShell) {
		super(parentShell);
	}


	public Icon getSelectedIcon() {
		return this.selectedIcon;
	}
	@Override
	public void okPressed(){
		this.selectedIcon = null;
		if (btnLibrary.getSelection()) {
			Object x = tblIcons.getStructuredSelection().getFirstElement();
			if (x instanceof Icon) {
				selectedIcon = (Icon)x;
			}
		}else if (btnImport.getSelection()) {
			
			String name = txtName.getText().trim();
			if (name.isEmpty()) name = "SMART Icon";
			
			Icon icon = new Icon();
			icon.setConservationArea(SmartDB.getCurrentConservationArea());
			icon.setKeyId(  DataModelManager.INSTANCE.generateKey(name, icons) );
			icon.setName(name);
			icon.updateName(SmartDB.getCurrentLanguage(), name);
			icon.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
			icon.setFiles(new ArrayList<>());
			for (Entry<IconSet, Label> item : imports.entrySet()) {
				if (item.getValue().getData("PATH") == null) continue;
				Path file = (Path)item.getValue().getData("PATH");
				
				IconFile f = new IconFile();
				f.setIcon(icon);
				f.setIconSet(item.getKey());
				f.setCopyFromLocation(file.toFile());
				f.setFilename(file.getFileName().toString());
				
				icon.getFiles().add(f);
			}
			
			//only create icon if at least one file is selected
			if (!icon.getFiles().isEmpty()) selectedIcon = icon;
			
		}
		super.okPressed();
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		List<IconSet> sets = new ArrayList<>();
		try(Session s = HibernateManager.openSession()){
			sets.addAll(QueryFactory.buildQuery(s, IconSet.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
			sets.forEach(set->{
				set.getName();
				set.getUuid().equals(null);
			});
		}
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Icon Source:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Composite btn = new Composite(main, SWT.NONE);
		btn.setLayout(new GridLayout());
		btn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)btn.getLayout()).marginWidth = 0;
		((GridLayout)btn.getLayout()).marginHeight = 0;
		
		btnLibrary = new Button(btn, SWT.RADIO);
		btnLibrary.setText("SMART Icon");
		btnLibrary.setSelection(true);
		
		btnImport = new Button(btn, SWT.RADIO);
		btnImport.setText("Import New Icon");
		
		l = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		stackPanel = new Composite(main, SWT.NONE);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		stackPanel.setLayout(new StackLayout());
		
		iconLibraryPanel = createLibraryComposite(stackPanel, sets);
		
		importPanel = createImportComposite(stackPanel, sets);
		
		((StackLayout)stackPanel.getLayout()).topControl = iconLibraryPanel;
		stackPanel.layout();
		
		
		btnLibrary.addListener(SWT.Selection, e->{
			((StackLayout)stackPanel.getLayout()).topControl = iconLibraryPanel;
			stackPanel.layout();	
		});
		
		btnImport.addListener(SWT.Selection, e->{
			((StackLayout)stackPanel.getLayout()).topControl = importPanel;
			stackPanel.layout();	
		});
		
		loadDataJob.schedule();
		
		setMessage("Select an new icon ");
		setTitle("SMART Icon");
		getShell().setText("SMART Icon");
		return parent;
	}
	
	private Composite createImportComposite(Composite parent, List<IconSet> sets) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(panel, SWT.NONE);
		l.setText("Icon Name:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtName = new Text(panel, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
		imports = new HashMap<>();
		for (IconSet s : sets) {
			l = new Label(panel, SWT.NONE);
			l.setText(s.getName() + ":");
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			Composite temp = new Composite(panel, SWT.NONE);
			temp.setLayout(new GridLayout(2, false));
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)temp.getLayout()).marginWidth = 0;
			((GridLayout)temp.getLayout()).marginHeight = 0;
			
			Label imageLabel = new Label(temp, SWT.NONE);
			imports.put(s, imageLabel);
			imageLabel.setText("");
			imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)imageLabel.getLayoutData()).heightHint = SIZE;
			((GridData)imageLabel.getLayoutData()).widthHint = SIZE;
			imageLabel.addListener(SWT.Dispose, e->{
				if (imageLabel.getImage() != null) imageLabel.getImage().dispose();
			});
			Button btnImport = new Button(temp, SWT.NONE);
			btnImport.setText("...");
			btnImport.addListener(SWT.Selection, e->{
				Path p = selectFile();
				if (p == null) return;
				updateName(p);
				imageLabel.setData("PATH", p);
				if (imageLabel.getImage() != null) {
					//dispose of existing image
					Image i = imageLabel.getImage();
					imageLabel.setImage(null);
					i.dispose();
				}
				Image i = null;
				try {
					i = SmartUtils.readSvg(getShell().getDisplay(), p, SIZE);
					imageLabel.setText("");
					imageLabel.setImage(i);
				} catch (Exception e1) {
					imageLabel.setText(p.getFileName().toString());
					i = null;
				}
				
				//update others
				for (Label iLabel : imports.values()) {
					if (iLabel == imageLabel) continue;
					if (iLabel.getData("PATH") != null || iLabel.getImage() != null) continue;
					iLabel.setData("PATH", p);
					if (i == null ) {
						iLabel.setText(p.getFileName().toString());
					}else {
						iLabel.setImage(new Image(getShell().getDisplay(), i.getImageData()));
					}
				}
			});
		}
		
		return panel;
	}
	
	
	private Composite createLibraryComposite(Composite parent, List<IconSet> sets) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		
		tblIcons = new TableViewer(panel, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.VIRTUAL);
		tblIcons.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblIcons.setContentProvider(new ILazyContentProvider() {
			
			List<?> data;
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput instanceof List) {
					data = (List<?>) newInput;
				}else {
					data = Collections.singletonList(newInput);
				}
			}

			@Override
			public void updateElement(int index) {
				tblIcons.replace(data.get(index), index);
			}
		});
		
		
		tblIcons.getTable().setHeaderVisible(true);
		tblIcons.getTable().setLinesVisible(false);
		tblIcons.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		
		TableViewerColumn emptycolumn = new TableViewerColumn(tblIcons, SWT.NONE);
		emptycolumn.getColumn().setText("");
		emptycolumn.getColumn().setWidth(0);
		emptycolumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}
		});
		
		
		TableViewerColumn colName = new TableViewerColumn(tblIcons, SWT.NONE);
		colName.getColumn().setText("Name");
		colName.getColumn().setWidth(150);
		colName.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Icon) return ((Icon)element).getName();
				return super.getText(element);
			}
		});
		
		
		for (IconSet s : sets) {
			TableViewerColumn colIcon = new TableViewerColumn(tblIcons, SWT.NONE);
			colIcon.getColumn().setText(s.getName());
			colIcon.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					return null;
				}
				public Image getImage(Object element) {
					if (element instanceof Icon) {
						IconFile ff = ((Icon)element).getIconFile(s);
						try {
							return SmartUtils.readSvg(getShell().getDisplay(), ff.getAttachmentFile().toPath(), SIZE);
						}catch (Throwable t) {
							
						}
					}
					return null;
				}
			});
			colIcon.getColumn().pack();
			if (colIcon.getColumn().getWidth() < SIZE) { colIcon.getColumn().setWidth(SIZE); }
		}
		tblIcons.setItemCount(1);
		tblIcons.setUseHashlookup(true);
		tblIcons.setInput(DialogConstants.LOADING_TEXT);

		return panel;
	}
	
	private void updateName(Path p) {
		if (p == null) return;
		if (txtName.getText().trim().isEmpty()) {
			String t = p.getFileName().toString();
			t = t.substring(0, t.lastIndexOf('.'));
			txtName.setText(t);
		}
	}
	
	private Path selectFile() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		fd.setFilterExtensions(new String[] {"*.svg","*.png","*.*"});
		fd.setFilterNames(new String[] {"SVG (*.svg)", "PNG (*.png)", "All Files (*.*)"});
		
		//TODO: save previous path?
		String file = fd.open();
		if (file == null) return null;
		Path p = Paths.get(file);
		if (!Files.exists(p)) return null;
		
		
		return p;
	}
	
	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		p.y = 500;
		return p;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

	private Job loadDataJob = new Job("load icons") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Icon> thisicons = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				thisicons.addAll(QueryFactory.buildQuery(session, Icon.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				for (Icon ii : thisicons) {
					ii.getFiles().forEach(ff->{
						ff.computeFileLocation(session);
						session.get(IconSet.class, ff.getIconSet().getUuid());
					});
				}
			}
			icons = thisicons;
			Display.getDefault().syncExec(()->{
				tblIcons.setItemCount(thisicons.size());
				tblIcons.setInput(thisicons);
				tblIcons.getTable().getColumn(1).pack();
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
