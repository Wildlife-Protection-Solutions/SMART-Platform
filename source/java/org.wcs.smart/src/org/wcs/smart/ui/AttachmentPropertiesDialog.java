/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;

import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.properties.XMPPropertyInfo;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.xmp.XmpDirectory;

/**
 * Attachment properties dialog;
 * 
 * @author Emily
 *
 */
public class AttachmentPropertiesDialog {

	private static final String DETAILS_KEY = "SMART Details"; //$NON-NLS-1$
	
	private ISmartAttachment attachment;
	private Shell parent;
	private HashMap<String, List<Entry>> properties;
	
	private ITreeContentProvider provider = new ITreeContentProvider() {
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
		@Override
		public void dispose() {
		}
		
		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof String) return true;
			if (element instanceof Entry) return false;
			return false;
		}
		
		@Override
		public Object getParent(Object element) {
			return null;
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
			List<String> sortedKey = new ArrayList<String>(properties.keySet());
			sortedKey.sort((a,b) ->{
				if (a.equals(DETAILS_KEY)) return -1;
				if (b.equals(DETAILS_KEY)) return 1;
				return Collator.getInstance().compare(a, b);
			});

			return sortedKey.toArray();
		}
		
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String){
				return properties.get((String)parentElement).toArray();
			}
			return null;
		}
	};
	
	public AttachmentPropertiesDialog(Shell parent, ISmartAttachment attachment){
		this.parent = new Shell(parent, SWT.RESIZE|SWT.TITLE|SWT.CLOSE|SWT.APPLICATION_MODAL);
		this.attachment =attachment;
		createDialogArea();
		this.parent.setText(MessageFormat.format(Messages.AttachmentPropertiesDialog_DialogTitle, attachment.getFilename()));
		this.parent.setSize(400,  500);
	}
	
	public void open(){
		int x = parent.getParent().getLocation().x;
		int y = parent.getParent().getLocation().y;
		x = x + ((parent.getParent().getBounds().width / 2) - 200);
		y = y + ((parent.getParent().getBounds().height / 2) - 250);
		parent.setLocation(x,y);
		parent.open();
	}
	
	@SuppressWarnings("unchecked")
	protected void createDialogArea() {
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).marginWidth = 0;
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		((GridLayout)composite.getLayout()).marginHeight = 0;
		((GridLayout)composite.getLayout()).marginWidth = 0;
		composite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Tree detailsTree = new Tree(composite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		TreeViewer treeViewer = new TreeViewer(detailsTree);
		
		treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.setContentProvider(provider);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.getTree().setLinesVisible(true);

		TreeViewerColumn colType = new TreeViewerColumn(treeViewer, SWT.DEFAULT);
		colType.getColumn().setWidth(150);
		colType.getColumn().setText(Messages.AttachmentPropertiesDialog_PropertyColumnName);
		colType.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof String) return element.toString();
				if (element instanceof Entry) return ((Entry) element).getKey().toString();
				return super.getText(element);
			}
		});
		
		TreeViewerColumn colValue = new TreeViewerColumn(treeViewer, SWT.DEFAULT);
		colValue.getColumn().setWidth(200);
		colValue.getColumn().setText(Messages.AttachmentPropertiesDialog_ValueColumnName);
		colValue.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof String) return ""; //$NON-NLS-1$
				if (element instanceof Entry){
					if (((Entry)element).getValue() == null) return ""; //$NON-NLS-1$
					return ((Entry) element).getValue().toString();
				}
				return super.getText(element);
			}
		});
		
		
		properties = new HashMap<String, List<Entry>>();
		
		try(Session s = HibernateManager.openSession()){
			if (attachment.getUuid() != null){
				attachment = (ISmartAttachment) s.get(Hibernate.getClass(attachment), attachment.getUuid());
			}
			List<Entry> details = new ArrayList<AttachmentPropertiesDialog.Entry>();
			details.add(new Entry(Messages.AttachmentPropertiesDialog_FileNameLabel, attachment.getFilename()));
			details.addAll(findAdditionalDetails(attachment));
			
			try {
				attachment.computeFileLocation(s);
				details.add(new Entry(Messages.AttachmentPropertiesDialog_PathSection, attachment.getAttachmentFile().getAbsolutePath()));
			} catch (Exception e) {
				SmartPlugIn.log(e.getMessage(), e);
				details.add(new Entry(Messages.AttachmentPropertiesDialog_PathSection, Messages.AttachmentPropertiesDialog_Error + e.getMessage()));
			}
			properties.put(DETAILS_KEY, details);
		}
		
		
		try{
			Path imageFile = EncryptUtils.decryptAttachment(attachment);
			try {
				Metadata metadata = ImageMetadataReader.readMetadata(imageFile.toFile());
				
				List<Directory> dirs = new ArrayList<>();
				for (Directory directory : metadata.getDirectories()) dirs.add(directory);
				dirs.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
				
				for (Directory directory : dirs) {
					String name = directory.getName();
					List<Entry> details = properties.get(name);
					if (details == null){
						details = new ArrayList<AttachmentPropertiesDialog.Entry>();
						properties.put(name, details);
					}
					if (directory.getClass().equals(XmpDirectory.class)) {
						XMPMeta meta = ((XmpDirectory)directory).getXMPMeta();
						try {
							XMPIterator i = meta.iterator();
							while(i.hasNext()) {
								XMPPropertyInfo info = (XMPPropertyInfo)i.next();
								if (info.getPath() != null && !info.getPath().isEmpty())
									details.add(new Entry(info.getPath(), info.getValue()));
							}
						}catch (Exception ex) {
							SmartPlugIn.log(ex.getMessage(),  ex);
						}
					}else {
						for (Tag g : directory.getTags()){
							details.add(new Entry(g.getTagName(), g.getDescription()));
						}
					}
				}
			}finally {
				if (imageFile != null) {
					try {
						Files.delete(imageFile);
					}catch (Exception ex) {
						//eat me
					}
				}
			}
		}catch (Exception ex){
			//eat this for whatever reason we cannot read image metadata 
			ex.printStackTrace();
		}
		
		treeViewer.setInput(properties);
		treeViewer.expandAll();
		
		Composite btnComposite = new Composite(composite, SWT.NONE);
		btnComposite.setLayout(new GridLayout());
		btnComposite.setBackground(composite.getBackground());
		btnComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Button btnOk = new Button(btnComposite, SWT.PUSH);
		btnOk.setText(IDialogConstants.OK_LABEL);
		btnOk.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				parent.dispose();
			}
		});
		((GridData)btnOk.getLayoutData()).widthHint = 80;
		
	}

	/**
	 * Must not return null;
	 * 
	 * @param attachment
	 * @return
	 */
	protected List<Entry> findAdditionalDetails(ISmartAttachment attachment) {
		return Collections.emptyList();
	}
	
	public class Entry{
		String key;
		String value;
		
		public Entry(String key, String value){
			this.key = key;
			this.value = value;
		}
		public String getKey(){
			return this.key;
		}
		public String getValue(){
			return this.value;
		}
	}
}
