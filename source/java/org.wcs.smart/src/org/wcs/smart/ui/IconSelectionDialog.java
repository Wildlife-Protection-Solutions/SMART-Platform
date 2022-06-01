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
package org.wcs.smart.ui;

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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconManager;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.icon.ui.ImageSelectionDialog;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.properties.IconCellHighlighter;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for selecting a SMART icon.  Can either select an 
 * existing icon or add a new one.
 * 
 * @author Emily
 *
 */
public class IconSelectionDialog extends SmartStyledTitleDialog {

	private static final String PATH_KEY = "PATH"; //$NON-NLS-1$

	private static final int SIZE = 32;
	
	/**
	 * The type of dialog.  SELECT allows uses to select
	 * an icon from the existing set (or add a new one), NEW allows users to create
	 * a new icon, EDIT allows users to edit an existing icon
	 * @author Emily
	 *
	 */
	public static enum Type {
		SELECT,
		NEW,
		EDIT,
		SINGLE_SELECT
	}
	private Composite stackPanel;
	
	
	private Composite iconCaPanel;
	private Composite importPanel;
	
	private TableViewer tblCaIcons;
		
	private Button btnIcon;
	private Button btnImport;
	
	private Icon selectedIcon;
	private Text txtName, txtKey;
	
	private HashMap<IconSet, Label> imports;
	private List<Icon> caIcons;
	private List<Icon> systemIcons;
	
	private Type type;
	
	private List<IconSet> activeSets;
	
//	private IconFile selectedFile = null;
	private IconSet currentSet = null;
	private TableViewerFocusCellManager focusManager ;
	
	private String txtFilterText = null;
	
	private ViewerFilter iconFilter = new ViewerFilter() {
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (txtFilterText == null || txtFilterText.isEmpty()) return true;
			
			if (element instanceof Icon) {
				return ((Icon)element).getName().toLowerCase().contains(txtFilterText);
			}
			return false;
		}
	};
	
	public IconSelectionDialog(Shell parentShell, Type type, List<IconSet> activeSets) {
		super(parentShell);
		this.type = type;
		this.activeSets = activeSets;
	}
	
	public IconSelectionDialog(Shell parentShell, Type type) {
		this(parentShell, type, null);
	}

	public IconSelectionDialog(Shell parentShell, Icon toUpdate, List<IconSet> activeSets) {
		this(parentShell, Type.EDIT, activeSets);
		selectedIcon = toUpdate;
	}
	
	public IconSelectionDialog(Shell parentShell, Icon toUpdate) {
		this(parentShell, toUpdate, null);
	}

	public IconFile getSelectedIconFile() {
		if (currentSet != null && selectedIcon != null) return this.selectedIcon.getIconFile(currentSet);
		return null;
	}
	public Icon getSelectedIcon() {
		return this.selectedIcon;
	}
	@Override
	public void okPressed(){
		if (type == Type.SINGLE_SELECT) {
			int index = focusManager.getFocusCell().getColumnIndex() - 2;
			if (index < 0) index = 0;
			if (!activeSets.isEmpty()) currentSet = activeSets.get(index);
			
			//Object x = tblIcons.getStructuredSelection().getFirstElement();
			Object x = tblCaIcons.getStructuredSelection().getFirstElement();
			if (x instanceof Icon) {
				selectedIcon = (Icon)x;
			}
			
		} else if (type == Type.SELECT) {
			this.selectedIcon = null;
			if (btnIcon.getSelection()) {
				Object x = tblCaIcons.getStructuredSelection().getFirstElement();
				if (x instanceof Icon) {
					selectedIcon = (Icon)x;
				}
			}else if (btnImport.getSelection()) {
				Icon icon = createIcon();
				//only create icon if at least one file is selected
				if (icon.getFiles().isEmpty()) {
					MessageDialog.openError(getShell(), Messages.IconSelectionDialog_DialogTitle, Messages.IconSelectionDialog_IconRequiredMsg);
					return;
				}
				selectedIcon = icon;
			}
		}else if (type == Type.NEW) {
			this.selectedIcon = null;
			Icon icon = createIcon();
			//only create icon if at least one file is selected
			if (icon.getFiles().isEmpty()) {
				MessageDialog.openError(getShell(), Messages.IconSelectionDialog_DialogTitle, Messages.IconSelectionDialog_IconRequiredMsg);
				return;
			}
			selectedIcon = icon;
		}else if (type == Type.EDIT) {
			selectedIcon.setName(txtName.getText());
			selectedIcon.updateName(SmartDB.getCurrentLanguage(), txtName.getText());
			
			for (Entry<IconSet, Label> item : imports.entrySet()) {
				if (item.getValue().getData(PATH_KEY) == null) continue;
				Path file = (Path)item.getValue().getData(PATH_KEY);
				
				IconFile f = selectedIcon.getIconFile(item.getKey());
				if (f != null) {
					if (getIconFile(f).equals(file)) continue;
					
					selectedIcon.getFiles().remove(f);
				}
					
				f = new IconFile();
				f.setIcon(selectedIcon);
				f.setIconSet(item.getKey());
				f.setCopyFromLocation(file);
				f.setFilename(file.getFileName().toString());
				f.setCopyFromLocation(file);
				f.setFilename(file.getFileName().toString());
				
				selectedIcon.getFiles().add(f);
			}
		}
		super.okPressed();
	}
	
	private Path getIconFile(IconFile f) {
		if (f.getCopyFromLocation() != null) return f.getCopyFromLocation();
		return f.getAttachmentFile();
	}
	
	private Icon createIcon() {
		String name = txtName.getText().trim();
		if (name.isEmpty()) name = Messages.IconSelectionDialog_DefaultName;
		
		Icon icon = new Icon();
		icon.setConservationArea(SmartDB.getCurrentConservationArea());
		icon.setKeyId(  DataModelManager.INSTANCE.generateKey(name, caIcons) );
		icon.setName(name);
		icon.updateName(SmartDB.getCurrentLanguage(), name);
		icon.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		icon.setFiles(new ArrayList<>());
		for (Entry<IconSet, Label> item : imports.entrySet()) {
			if (item.getValue().getData(PATH_KEY) == null) continue;
			Path file = (Path)item.getValue().getData(PATH_KEY);
			
			IconFile f = new IconFile();
			f.setIcon(icon);
			f.setIconSet(item.getKey());
			f.setCopyFromLocation(file);
			f.setFilename(file.getFileName().toString());
			
			icon.getFiles().add(f);
		}
		return icon;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		if (activeSets == null) {
			activeSets = new ArrayList<>();
			try(Session s = HibernateManager.openSession()){
				activeSets.addAll(QueryFactory.buildQuery(s, IconSet.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				activeSets.forEach(set->{
					set.getName();
					set.getUuid().equals(null);
				});
			}
			if (!activeSets.isEmpty()) currentSet = activeSets.get(0);
		}
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (type == Type.SELECT) {
			Label l = new Label(main, SWT.NONE);
			l.setText(Messages.IconSelectionDialog_SourceOp);
			l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			Composite btn = new Composite(main, SWT.NONE);
			btn.setLayout(new GridLayout(3, false));
			btn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)btn.getLayout()).marginWidth = 0;
			((GridLayout)btn.getLayout()).marginHeight = 0;
			
			btnIcon = new Button(btn, SWT.RADIO);
			btnIcon.setText(Messages.IconSelectionDialog_SmartSrc);
			btnIcon.setSelection(true);
			
			btnImport = new Button(btn, SWT.RADIO);
			btnImport.setText(Messages.IconSelectionDialog_NewSrc);
			
			l = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			btnIcon.addListener(SWT.Selection, e->{
				((StackLayout)stackPanel.getLayout()).topControl = iconCaPanel;
				stackPanel.layout();	
			});
			
			btnImport.addListener(SWT.Selection, e->{
				((StackLayout)stackPanel.getLayout()).topControl = importPanel;
				stackPanel.layout();	
			});
		}
		
		stackPanel = new Composite(main, SWT.NONE);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		stackPanel.setLayout(new StackLayout());
		((StackLayout)stackPanel.getLayout()).marginWidth = 0;
		((StackLayout)stackPanel.getLayout()).marginHeight = 0;
		
		if (type == Type.SELECT || type == Type.SINGLE_SELECT) {
			iconCaPanel = createLibraryComposite(stackPanel, activeSets);
			loadDataJob.schedule();
		}
		importPanel = createImportComposite(stackPanel, activeSets);
		
		if (type == Type.SELECT || type == Type.SINGLE_SELECT) {
			((StackLayout)stackPanel.getLayout()).topControl = iconCaPanel;
		}else {
			((StackLayout)stackPanel.getLayout()).topControl = importPanel;
		}
		stackPanel.layout();
		
		
		
		setMessage(Messages.IconSelectionDialog_DialogMsg);
		setTitle(Messages.IconSelectionDialog_DialogTitle2);
		getShell().setText(Messages.IconSelectionDialog_DialogTitle2);
		return parent;
	}
	
	private Composite createImportComposite(Composite parent, List<IconSet> sets) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight= 0;
		
		Label l = new Label(panel, SWT.NONE);
		l.setText(Messages.IconSelectionDialog_NameLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtName = new Text(panel, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (selectedIcon != null) {
			txtName.setText(selectedIcon.getName());
		}
		
		l = new Label(panel, SWT.NONE);
		l.setText(Messages.IconSelectionDialog_LeyLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtKey = new Text(panel, SWT.BORDER);
		txtKey.setEnabled(false);
		txtKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (selectedIcon != null) {
			txtKey.setText(selectedIcon.getKeyId());
		}else {
			txtKey.setText(Messages.IconSelectionDialog_SystemGenerated);
		}
		
		imports = new HashMap<>();
		for (IconSet s : sets) {
			l = new Label(panel, SWT.NONE);
			l.setText(s.getName() + ":"); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			Composite temp = new Composite(panel, SWT.NONE);
			temp.setLayout(new GridLayout(2, false));
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)temp.getLayout()).marginWidth = 0;
			((GridLayout)temp.getLayout()).marginHeight = 0;
			
			Label imageLabel = new Label(temp, SWT.NONE);
			imports.put(s, imageLabel);
			imageLabel.setText(Messages.IconSelectionDialog_15);
			imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			((GridData)imageLabel.getLayoutData()).heightHint = SIZE;
			((GridData)imageLabel.getLayoutData()).widthHint = SIZE;
			imageLabel.addListener(SWT.Dispose, e->{
				if (imageLabel.getImage() != null) imageLabel.getImage().dispose();
			});
			Button btnImport = new Button(temp, SWT.NONE);
			btnImport.setText("..."); //$NON-NLS-1$
			btnImport.addListener(SWT.Selection, e->{
				Path p = selectFile();
				if (p == null) return;
				updateName(p);
				imageLabel.setData(PATH_KEY, p);
				if (imageLabel.getImage() != null) {
					//dispose of existing image
					Image i = imageLabel.getImage();
					imageLabel.setImage(null);
					i.dispose();
				}
				Image i = null;
				try {
					i = SmartUtils.getImage(p, SIZE);
					imageLabel.setText(""); //$NON-NLS-1$
					imageLabel.setImage(i);
				} catch (Exception e1) {
					imageLabel.setText(p.getFileName().toString());
					i = null;
				}
				
				//update others
				for (Label iLabel : imports.values()) {
					if (iLabel == imageLabel) continue;
					if (iLabel.getData(PATH_KEY) != null || iLabel.getImage() != null) continue;
					iLabel.setData(PATH_KEY, p);
					if (i == null ) {
						iLabel.setText(p.getFileName().toString());
					}else {
						iLabel.setImage(new Image(getShell().getDisplay(), i.getImageData()));
					}
				}
			});
			
			if (selectedIcon != null) {
				IconFile f = selectedIcon.getIconFile(s);
				if (f != null) {
					Path file = getIconFile(f);
					imageLabel.setData(PATH_KEY, file);
					try {
						Image i = SmartUtils.getImage(file, SIZE);
						imageLabel.setText(""); //$NON-NLS-1$
						imageLabel.setImage(i);
					} catch (Exception e1) {
						imageLabel.setText(Messages.IconSelectionDialog_IconError);
						imageLabel.setToolTipText(e1.getMessage());
					}
				}
			}
		}
		
		return panel;
	}
	
	private FilterComposite createFilterText(Composite parent) {
		FilterComposite txtFilter = new FilterComposite(parent, SWT.NONE);
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return txtFilter;
				
	}
	
	private Composite createLibraryComposite(Composite parent, List<IconSet> sets) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight= 0;
		
		FilterComposite txtFilter = createFilterText(panel);
		
		TableViewer tblIcons = new TableViewer(panel,  SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.VIRTUAL);
		tblIcons.setFilters(new ViewerFilter[] {iconFilter});
		if (type == Type.SINGLE_SELECT) {
			IconCellHighlighter cellHighlighter = new IconCellHighlighter(tblIcons);
			focusManager = new TableViewerFocusCellManager(tblIcons, cellHighlighter);
			ColumnViewerEditorActivationStrategy ss = new ColumnViewerEditorActivationStrategy(tblIcons);
			TableViewerEditor.create(tblIcons, focusManager, ss, TableViewerEditor.KEYBOARD_ACTIVATION);			
		}		
		
		txtFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				txtFilterText = txtFilter.getPatternFilter();
				if (txtFilterText != null) txtFilterText = txtFilterText.toLowerCase().trim();
				tblIcons.refresh();	
			}
		});
		
		tblIcons.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblIcons.setContentProvider(ArrayContentProvider.getInstance());
		
		tblIcons.getTable().setHeaderVisible(true);
		tblIcons.getTable().setLinesVisible(false);
		tblIcons.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		tblIcons.getTable().addListener(SWT.KeyDown, e->{
			//scroll to first item that start with key e.character
			Object in = tblIcons.getInput();
			if (!(in instanceof List)) return;
			List<?> c = (List<?>) in;
			e.doit = false;
			
			Object selection = tblIcons.getStructuredSelection().getFirstElement();
			
			int startIndex = 0;
			if (selection != null) {
				int a = c.indexOf(selection);
				if (a >= 0) startIndex = ( a + 1 ) % c.size();
			}
			
			char toFind = Character.toLowerCase(e.character);
			int index = startIndex;
			while(true) {
				Object item =  c.get(index);
				if (item instanceof Icon) {
					char cc = Character.toLowerCase( ((Icon) item).getName().charAt(0) );
					if (toFind == cc) {
						tblIcons.getTable().setSelection(index);
						tblIcons.getTable().showSelection();
						return;
					}
				}
				
				index = (index + 1) % c.size();	
				if (index == startIndex) break;
			}
		});
		
		
		TableViewerColumn emptycolumn = new TableViewerColumn(tblIcons, SWT.NONE);
		emptycolumn.getColumn().setText(""); //$NON-NLS-1$
		emptycolumn.getColumn().setWidth(0);
		emptycolumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}
		});
		
		
		TableViewerColumn colName = new TableViewerColumn(tblIcons, SWT.NONE);
		colName.getColumn().setText(Messages.IconSelectionDialog_NameColumn);
		colName.getColumn().setWidth(150);
		colName.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Icon) return ((Icon)element).getName();
				return super.getText(element);
			}
			
			@Override
			public Color getBackground(Object element) {
				if (element instanceof Icon) return null;
				return tblIcons.getControl().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
			}
		});
		
		
		for (IconSet s : sets) {
			TableViewerColumn colIcon = new TableViewerColumn(tblIcons, SWT.DEFAULT);
			
			colIcon.getColumn().setText(s.getName());
			colIcon.setLabelProvider(new ColumnLabelProvider() {
				private HashMap<Icon,Image> images = new HashMap<>();
				@Override
				public String getText(Object element) {
					return null;
				}
				@Override
				public void dispose() {
					super.dispose();
					images.values().forEach(e->e.dispose());
				}
				
				@Override
				public Color getBackground(Object element) {
					if (element instanceof Icon) return null;
					return tblIcons.getControl().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
				}
				@Override
				public Image getImage(Object element) {
					if (element instanceof Icon) {
						if (images.containsKey(element))  return images.get(element);
						IconFile ff = ((Icon)element).getIconFile(s);
						try {
							Image img = SmartUtils.getImage(ff.getAttachmentFile(), SIZE);
							images.put((Icon)element, img);
							return img;
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
		
		TableViewerColumn colIsCa = new TableViewerColumn(tblIcons, SWT.NONE);
		colIsCa.getColumn().setText("Conservation Area Icon");
		colIsCa.getColumn().setToolTipText("Yes if the icon is configured for the Conservation Area, no if the icon is from the SMART System Library");
		colIsCa.getColumn().setWidth(50);
		colIsCa.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof Icon) {
					if (caIcons.contains(element)) return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
					return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
				}
				return "";
			}
			
			@Override
			public Color getBackground(Object element) {
				if (element instanceof Icon) return null;
				return tblIcons.getControl().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
			}
		});
		
		tblCaIcons = tblIcons;
		
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
		ImageSelectionDialog dialog = new ImageSelectionDialog(getShell());
		if (dialog.open() != Window.OK) return null;
		return Paths.get(dialog.getImageFile());
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

	private Job loadDataJob = new Job("load icons") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try(Session session = HibernateManager.openSession()){
				caIcons = IconManager.INSTANCE.getIcons(session, SmartDB.getCurrentConservationArea());
				systemIcons = IconManager.INSTANCE.getSystemIcons(session, SmartDB.getCurrentConservationArea());
			}
			Collections.sort(caIcons);
			Collections.sort(systemIcons);
			
			List<Object> allIcons = new ArrayList<>();
			allIcons.add("Conservation Area Icons");
			allIcons.addAll(caIcons);
			allIcons.add("SMART Library Icons");
			allIcons.addAll(systemIcons);
			
			Display.getDefault().syncExec(()->{
				if (tblCaIcons.getControl().isDisposed()) return;
				
				tblCaIcons.setItemCount(allIcons.size());
				tblCaIcons.setInput(allIcons);
				
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
