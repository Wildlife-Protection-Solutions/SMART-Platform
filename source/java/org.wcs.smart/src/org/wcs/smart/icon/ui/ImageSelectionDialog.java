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
package org.wcs.smart.icon.ui;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for selecting an image from icon set, smart icon or a file
 *  
 * @author Emily
 *
 */
public class ImageSelectionDialog extends SmartStyledTitleDialog {

	private static final String NAMEKEY = "KEY"; //$NON-NLS-1$
	private static final String WIDGET_KEY = "WIDGET"; //$NON-NLS-1$
	private static final String IMAGE_KEY = "IMAGE"; //$NON-NLS-1$
	private static final String PATH_KEY = "PATH"; //$NON-NLS-1$
	private static final String MOUSEIN_KEY = "MOUSEIN"; //$NON-NLS-1$
	private static final String SELECTED_KEY = "SELECTED"; //$NON-NLS-1$
	
	private static final String DIR_PREF_KEY = "org.wcs.smart.ui.internal.ca.properties.IconSelectionDialog.dir"; //$NON-NLS-1$
	private static final String EXT_PREF_KEY = "org.wcs.smart.ui.internal.ca.properties.IconSelectionDialog.ext"; //$NON-NLS-1$
	private static final String OP_PREF_KEY = "org.wcs.smart.ui.internal.ca.properties.IconSelectionDialog.startOp"; //$NON-NLS-1$
	
	private static final int THUMB_SIZE = 50;
	
	private String selectedFile;
	
	private Text txtFile;
	private Composite iconTable;
	private Composite core;
	private ScrolledComposite scroll;
	private Color selectionColor, highlightColor;
	private Button btnOpFile, btnOpIcon;
	
	public ImageSelectionDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public String getImageFile() {
		return selectedFile;
	}
	
	@Override
	public void okPressed(){
		if (btnOpFile.getSelection()) {
			SmartPlugIn.getDefault().getPreferenceStore().setValue(OP_PREF_KEY, 0);
			if (txtFile.getText().trim().isEmpty()) {
				selectedFile = null;
			}else {
				selectedFile = txtFile.getText();
			}
		}else {
			SmartPlugIn.getDefault().getPreferenceStore().setValue(OP_PREF_KEY, 1);
			for (Control c : core.getChildren()) {
				if ((boolean)c.getData(SELECTED_KEY)) {
					URL url = (URL) c.getData(PATH_KEY);
					if (url.getProtocol().equalsIgnoreCase("file")) { //$NON-NLS-1$
						try {
							selectedFile = Paths.get(url.toURI()).toString();
						} catch (URISyntaxException ex) {
							SmartPlugIn.log(ex.getMessage(),  ex);
						}
					}else {
						try {
							Path temp = Files.createTempFile("smart", "." + SharedUtils.getFilenameExtension(url.toExternalForm())); //$NON-NLS-1$ //$NON-NLS-2$
							try(InputStream inputStream = url.openConnection().getInputStream()){
								Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
							}
							selectedFile = temp.toString();
							temp.toFile().deleteOnExit();
						}catch (Exception ex) {
							SmartPlugIn.log(ex.getMessage(),  ex);
						}
					}
					break;
				}
			}
		}
		super.okPressed();
	}
	
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		selectionColor = SmartUtils.getListSelectedColor(getShell().getDisplay());
		highlightColor = SmartUtils.getListHighlightColor(getShell().getDisplay());
		main.addListener(SWT.Dispose, e->{selectionColor.dispose(); highlightColor.dispose();});
		
		Composite top = new Composite(main, SWT.NONE);
		top.setLayout(new GridLayout(3, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(top, SWT.NONE);
		l.setText(Messages.ImageSelectionDialog_ImgSrcLabel);
		
		btnOpFile = new Button(top, SWT.RADIO);
		btnOpFile.setText(Messages.ImageSelectionDialog_CustomFileOp);
		btnOpFile.setSelection(true);
		btnOpFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnOpFile.getLayoutData()).horizontalIndent = 10;

		btnOpIcon = new Button(top, SWT.RADIO);
		btnOpIcon.setText(Messages.ImageSelectionDialog_SmartOp);
		btnOpIcon.setSelection(false);
		btnOpIcon.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnOpIcon.getLayoutData()).horizontalIndent = 10;
		
		Composite stack = new Composite(main, SWT.NONE);
		stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stack.setLayout(new StackLayout());
		
		Composite fileComp = new Composite(stack, SWT.NONE);
		fileComp.setLayout(new GridLayout(3, false));
		
		l = new Label(fileComp, SWT.NONE);
		l.setText(Messages.ImageSelectionDialog_FileName);
		txtFile = new Text(fileComp, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnFile = new Button(fileComp, SWT.NONE);
		btnFile.setText("..."); //$NON-NLS-1$
	
		new Label(fileComp, SWT.NONE);
		
		Label fileImagePreview = new Label(fileComp, SWT.NONE);
		fileImagePreview.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		fileImagePreview.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		((GridData)fileImagePreview.getLayoutData()).widthHint = 100;
		((GridData)fileImagePreview.getLayoutData()).heightHint = 100;
		fileImagePreview.addListener(SWT.Dispose, e->{if (fileImagePreview.getImage() != null) fileImagePreview.dispose();});
		btnFile.addListener(SWT.Selection, e->{
			Path p = selectFile();
			if (p == null) return;
			if (p != null) txtFile.setText(p.toString());
			
			if (fileImagePreview.getImage() != null) fileImagePreview.getImage().dispose();
			Image img = SmartUtils.getImage(p, 100);
			fileImagePreview.setImage(img);
			
		});
		
		
		iconTable = new Composite(stack, SWT.NONE);
		iconTable.setLayout(new GridLayout());
		iconTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		((GridLayout)iconTable.getLayout()).marginWidth = 0;
		((GridLayout)iconTable.getLayout()).marginHeight = 0;
		
		iconTable.addListener(SWT.Resize, e->{
			if (core == null || core.isDisposed()) return;
			core.setVisible(false);
			core.setSize(core.computeSize(iconTable.getClientArea().width - scroll.getVerticalBar().getSize().x - 10, SWT.DEFAULT));
			core.setVisible(true);
		});
				
		l = new Label(iconTable, SWT.NONE);
		l.setText(DialogConstants.LOADING_TEXT);

		int op = SmartPlugIn.getDefault().getPreferenceStore().getInt(OP_PREF_KEY);
		if (op == 0) {
			btnOpFile.setSelection(true);
			btnOpIcon.setSelection(false);
			((StackLayout)stack.getLayout()).topControl = fileComp;
		}else if (op == 1) {
			btnOpIcon.setSelection(true);
			btnOpFile.setSelection(false);
			((StackLayout)stack.getLayout()).topControl = iconTable;
		}
		
		Listener ll = e->{
			if (btnOpFile.getSelection()) {
				((StackLayout)stack.getLayout()).topControl = fileComp;
			}else {
				((StackLayout)stack.getLayout()).topControl = iconTable;
			}
			stack.layout();
		};
		btnOpFile.addListener(SWT.Selection, ll);
		btnOpIcon.addListener(SWT.Selection, ll);
				
		loadDataJob.schedule();
		
		setMessage(Messages.ImageSelectionDialog_message);
		setTitle(Messages.ImageSelectionDialog_title);
		getShell().setText(Messages.ImageSelectionDialog_title);
		return parent;
	}
	
	
	private Path selectFile() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		
		fd.setFilterExtensions(new String[] {
				"*.bmp;*.jpg;*.jpeg;*.png;*.svg", //$NON-NLS-1$
				"*.bmp", //$NON-NLS-1$
				"*.jpg;*.jpeg", //$NON-NLS-1$
				"*.png;*.svg", //$NON-NLS-1$
				"*.png", //$NON-NLS-1$
				"*.svg", //$NON-NLS-1$
				
		});
		fd.setFilterNames(new String[] {
				Messages.ImageSelectionControl_AllImages1,
				Messages.ImageSelectionControl_BitmapFiles,
				Messages.ImageSelectionControl_JpegFiles,
				Messages.ImageSelectionControl_pngsvg,
				Messages.ImageSelectionControl_png,
				Messages.ImageSelectionControl_svg
		});
				
		String start = SmartPlugIn.getDefault().getPreferenceStore().getString(DIR_PREF_KEY);
		if (start != null) {
			fd.setFilterPath(start);
		}
		int index = SmartPlugIn.getDefault().getPreferenceStore().getInt(EXT_PREF_KEY);
		fd.setFilterIndex(index);

		String file = fd.open();
		if (file == null) return null;
		Path p = Paths.get(file);
		try {
			SmartPlugIn.getDefault().getPreferenceStore().putValue(DIR_PREF_KEY, p.getParent().toString());
			SmartPlugIn.getDefault().getPreferenceStore().putValue(EXT_PREF_KEY, String.valueOf(fd.getFilterIndex()));
		}catch (Exception ex) {}
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

	private void createIconTable(SortedMap<String, Set<String>> paths) {
		for (Control c : iconTable.getChildren()) c.dispose();
		
		FilterComposite txtFilter = new FilterComposite(iconTable, SWT.NONE);
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				String txtFilterText = txtFilter.getPatternFilter();
				
				Set<String> items =  paths.keySet();
				if (txtFilterText != null && !txtFilterText.trim().isBlank()) {
					items = new HashSet<>();
					txtFilterText = txtFilterText.trim().toLowerCase();
					for (String key : paths.keySet()) {
						if (key.toLowerCase().contains(txtFilterText)) {
							items.add(key);
						}
					}
				}

				core.setVisible(false);
				for (Control kid : core.getChildren()) {
					if (items.contains(kid.getData(NAMEKEY))){
						kid.setVisible(true);
						((RowData)kid.getLayoutData()).exclude = false;
					}else {
						kid.setVisible(false);
						((RowData)kid.getLayoutData()).exclude = true;
					}
				}
				core.setVisible(true);
				core.setSize(core.computeSize(iconTable.getClientArea().width, SWT.DEFAULT));
				iconTable.layout();		
			}
		});
		
		scroll = new ScrolledComposite(iconTable, SWT.V_SCROLL | SWT.BORDER);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		core = new Composite(scroll, SWT.NONE);
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		core.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		scroll.setContent(core);
		
		
		core.setLayout(new RowLayout());
		((RowLayout)core.getLayout()).wrap = true;
		((RowLayout)core.getLayout()).marginWidth = 0;
		((RowLayout)core.getLayout()).marginHeight = 0;
	
		for (String name : paths.keySet()) {
			Set<String> icons = paths.get(name);
			
			for (String path : icons) {
				Composite outer = new Composite(core, SWT.NONE);
				outer.setLayout(new GridLayout());
				outer.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
				((GridLayout)outer.getLayout()).marginWidth = 4;
				((GridLayout)outer.getLayout()).marginHeight = 4;
				outer.setData(NAMEKEY, name);
				outer.setLayoutData(new RowData());
				((RowData)outer.getLayoutData()).exclude = false;
				
				Label l = new Label(outer, SWT.NONE);
				l.setText(name);
				l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
				l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				
				Composite icon = new Composite(outer, SWT.NONE);
				icon.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
				((GridData)icon.getLayoutData()).widthHint = THUMB_SIZE;
				((GridData)icon.getLayoutData()).heightHint = THUMB_SIZE;
				try {
					icon.setData(PATH_KEY, new URL(path));
					outer.setData(PATH_KEY, new URL(path));
				}catch (Exception ex) {
					ex.printStackTrace();
				}
				icon.addListener(SWT.Paint, e->{
					Widget w = e.widget;
					Object img = w.getData(IMAGE_KEY);
					if (img == null) {
						URL url = (URL) w.getData(PATH_KEY);
						if (url != null) {
							img = SmartUtils.getImage(url, THUMB_SIZE - 4);
							w.setData(IMAGE_KEY, img);
						}
					}
					if (img != null) {
						e.gc.drawImage((Image)img, 2, 2);
					}
				});
				outer.setData(SELECTED_KEY,  false);
				outer.setData(MOUSEIN_KEY,  false);
				outer.addListener(SWT.Paint, itemListener);
				outer.addListener(SWT.MouseEnter, itemListener);
				outer.addListener(SWT.MouseExit, itemListener);
				outer.addListener(SWT.MouseUp, itemListener);
				outer.addListener(SWT.MouseDoubleClick, itemListener);
				outer.setData(WIDGET_KEY, outer);
				l.addListener(SWT.MouseEnter, itemListener);
				l.addListener(SWT.MouseExit, itemListener);
				l.addListener(SWT.MouseUp, itemListener);
				l.addListener(SWT.MouseDoubleClick, itemListener);
				l.setData(WIDGET_KEY, outer);
				
				icon.addListener(SWT.MouseEnter, itemListener);
				icon.addListener(SWT.MouseExit, itemListener);
				icon.addListener(SWT.MouseUp, itemListener);
				icon.addListener(SWT.MouseDoubleClick, itemListener);
				
				icon.setData(WIDGET_KEY, outer);
				
				
				
			}
			
		}
		
		core.setSize(core.computeSize(iconTable.getClientArea().width, SWT.DEFAULT));
		iconTable.layout();
	}
	
	private Job loadDataJob = new Job("load icons") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			SortedMap<String, Set<String>> files = new TreeMap<>();
			
			for (String[] s : IconUtils.SMART_ICON_MAPPING) {
				Set<String> items = files.get(s[1]);
				if (items == null) {
					items = new HashSet<>();
					files.put(s[1], items);
				}
				items.add(s[2]);
				items.add(s[2]);
				items.add(s[3]);
			}
			
			try(Session session = HibernateManager.openSession()){
				session.doWork(new Work() {
					@Override
					public void execute(Connection c) throws SQLException {
						c.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
						session.beginTransaction();
						List<IconFile> icons = QueryFactory.buildQuery(session, IconFile.class, 
								new Object[] {"iconSet.conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
								.list(); 
						
						for (IconFile file : icons) {
							String name = file.getIcon().getName();
							Set<String> items = files.get(name);
							if (items == null) {
								items = new HashSet<>();
								files.put(name, items);
							}
							
							if (file.isSystemIcon()) {
								items.add(file.getFilename());
							}else {
								file.computeFileLocation(session);
								try {
									items.add(file.getAttachmentFile().normalize().toAbsolutePath().toUri().toURL().toString()); 
								} catch (MalformedURLException e) {
									e.printStackTrace();
								}
							}
						}
						session.getTransaction().commit();
					}
				});
			}
			
			Display.getDefault().syncExec(()->{
				createIconTable(files);
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	private Listener itemListener = new Listener() {

		@Override
		public void handleEvent(Event event) {
			Widget w = event.widget;
			w = (Widget) w.getData(WIDGET_KEY);
			if (w == null) return;
			if (event.type == SWT.Paint) {
				int width = ((Composite)w).getBounds().width;
				int height = ((Composite)w).getBounds().height;
				if ((boolean)event.widget.getData(SELECTED_KEY)) {
					event.gc.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
					event.gc.setLineWidth(2);
					event.gc.drawRectangle(1, 1, width - 2, height - 2);
				}else if ((boolean)event.widget.getData(MOUSEIN_KEY)) {
					event.gc.setForeground(selectionColor);
					event.gc.setLineWidth(2);
					event.gc.drawRectangle(1, 1, width - 2, height - 2);
				}else {
					event.gc.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY));
					event.gc.setLineWidth(1);
					event.gc.drawRectangle(0, 0, width - 1, height - 1);
				}
				
			}else if (event.type == SWT.MouseEnter) {
				w.setData(MOUSEIN_KEY,  true);
				((Composite)w).setCursor(getShell().getDisplay().getSystemCursor(SWT.CURSOR_HAND));
				((Composite)w).redraw();
			}else if (event.type == SWT.MouseExit) {
				w.setData(MOUSEIN_KEY,  false);
				((Composite)w).setCursor(null);
				((Composite)w).redraw();
			}else if (event.type == SWT.MouseUp) {
				for (Control c : core.getChildren()) {
					if ((boolean)c.getData(SELECTED_KEY)) {
						c.setData(SELECTED_KEY,  false);
						c.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
						c.redraw();
					}
				}
				w.setData(SELECTED_KEY,  true);
				((Composite)w).setBackground(highlightColor);
				((Composite)w).redraw();	
			}else if (event.type == SWT.MouseDoubleClick) {
				w.setData(SELECTED_KEY,  true);
				okPressed();
			}
		}
		
	};
}
