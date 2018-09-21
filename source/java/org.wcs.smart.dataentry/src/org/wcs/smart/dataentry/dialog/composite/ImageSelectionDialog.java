package org.wcs.smart.dataentry.dialog.composite;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
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
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

public class ImageSelectionDialog extends TitleAreaDialog {


	private static final String PATH_KEY = "PATH"; //$NON-NLS-1$

	private static final String DIR_PREF_KEY = "org.wcs.smart.ui.internal.ca.properties.IconSelectionDialog.dir"; //$NON-NLS-1$
	private static final String EXT_PREF_KEY = "org.wcs.smart.ui.internal.ca.properties.IconSelectionDialog.ext"; //$NON-NLS-1$
	
	private static final int SIZE = 32;
	
	

	private Text txtFile;
	
	private Composite iconTable;
	private Path selectedFile;
	
	public ImageSelectionDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public Path getImageFile() {
		return null;
//		return this.imageFile;
	}
	
	@Override
	public void okPressed(){
		
		super.okPressed();
	}
	
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Button btnOpFile = new Button(main, SWT.RADIO);
		btnOpFile.setText("File:");
		btnOpFile.setSelection(true);
		
		txtFile = new Text(main, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnFile = new Button(main, SWT.NONE);
		btnFile.setText("...");
		btnFile.addListener(SWT.Selection, e->{
			Path p = selectFile();
			if (p != null) txtFile.setText(p.toString());
		});
		
		Button btnOpIcon = new Button(main, SWT.RADIO);
		btnOpIcon.setText("SMART Icon:");
		btnOpIcon.setSelection(false);
		
		iconTable = new Composite(main, SWT.BORDER);
		iconTable.setLayout(new GridLayout());
		iconTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		Label l = new Label(iconTable, SWT.NONE);
		l.setText(DialogConstants.LOADING_TEXT);
		
		
		Listener ll = e->{
			txtFile.setEnabled(btnOpFile.getSelection());
			btnFile.setEnabled(btnOpFile.getSelection());
			iconTable.setEnabled(!btnOpFile.getSelection());
		};
		
		btnOpFile.addListener(SWT.Selection, ll);
		btnOpIcon.addListener(SWT.Selection, ll);
		
		loadDataJob.schedule();
		
		setMessage("Select an image file or use one of the existing SMART icons");
		setTitle("Image Selection");
		getShell().setText("Image Selection");
		return parent;
	}
	
	
	
	private Path selectFile() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		fd.setFilterExtensions(new String[] {"*.svg","*.png","*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		fd.setFilterNames(new String[] {"SVG (*.svg)", "PNG (*.png)", "All Files (*.*)"});
		
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

	private void createIconTable(List<String> paths) {
		for (Control c : iconTable.getChildren()) c.dispose();
		
		int size = 50;
		
		ScrolledComposite scroll = new ScrolledComposite(iconTable, SWT.V_SCROLL | SWT.BORDER);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite core = new Composite(scroll, SWT.BORDER);
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setContent(core);
		
		
		core.setLayout(new RowLayout());
		((RowLayout)core.getLayout()).wrap = true;
		((RowLayout)core.getLayout()).marginWidth = 0;
		((RowLayout)core.getLayout()).marginHeight = 0;
		
		for (String path : paths) {
			Composite icon = new Composite(core, SWT.BORDER);
			icon.setLayoutData(new RowData(size, size));
			icon.setData("PATH", path);
			
			icon.addListener(SWT.Paint, e->{
				Object img = icon.getData("IMAGE");
				if (img == null) {
					try {
						img = SmartUtils.getImage(Paths.get(path), size);
						icon.setData("IMAGE", img);
					}catch (Exception ex) {
						
					}
				}
				if (img != null) {
					e.gc.drawImage((Image)img, 0, 0);
				}
				
			});
		}
		
		core.setSize(core.computeSize(iconTable.getClientArea().width, SWT.DEFAULT));
		//TODO: add this only once
		iconTable.addListener(SWT.Resize, e->{
			core.setSize(core.computeSize(iconTable.getClientArea().width, SWT.DEFAULT));	
		});
		iconTable.layout();
	}
	
	
	
	private Job loadDataJob = new Job("load icons") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<String> files = new ArrayList<>();
			
			
			try(Session session = HibernateManager.openSession()){
				List<IconFile> icons = QueryFactory.buildQuery(session, IconFile.class, 
						new Object[] {"iconSet.conservationArea", SmartDB.getCurrentConservationArea()}).list();
				
				for (IconFile file : icons) {
					if (file.isSystemIcon()) {
						files.add(file.getFilename());
					}else {
						file.computeFileLocation(session);
						files.add(file.getAttachmentFile().getAbsolutePath().toString());
					}
				}
			}

			//TODO all platform icons that are not ionfiles
			
			Display.getDefault().syncExec(()->{
				createIconTable(files);
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
