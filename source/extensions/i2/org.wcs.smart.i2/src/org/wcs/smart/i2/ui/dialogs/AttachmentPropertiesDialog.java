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
package org.wcs.smart.i2.ui.dialogs;

import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
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
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.ui.SmartLabelProvider;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * Attachment properties dialog;
 * 
 * @author Emily
 *
 */
public class AttachmentPropertiesDialog {

	private static final String DETAILS_KEY = "SMART Details";
	
	private IntelAttachment attachment;
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
	
	public AttachmentPropertiesDialog(Shell parent, IntelAttachment attachment){
		this.parent = new Shell(parent, SWT.RESIZE|SWT.TITLE|SWT.CLOSE|SWT.APPLICATION_MODAL);
		this.attachment =attachment;
		createDialogArea();
		this.parent.setText("Properties: " + attachment.getFilename());
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
		colType.getColumn().setText("Property");
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
		colValue.getColumn().setText("Value");
		colValue.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof String) return "";
				if (element instanceof Entry){
					if (((Entry)element).getValue() == null) return "";
					return ((Entry) element).getValue().toString();
				}
				return super.getText(element);
			}
		});
		
		
		properties = new HashMap<String, List<Entry>>();
		
		Session s = HibernateManager.openSession();
		try{
			if (attachment.getUuid() != null){
				attachment = (IntelAttachment) s.get(IntelAttachment.class, attachment.getUuid());
			}
			List<Entry> details = new ArrayList<AttachmentPropertiesDialog.Entry>();
			details.add(new Entry("Name", attachment.getFilename()));
			details.add(new Entry("Created By", SmartLabelProvider.getShortLabel(attachment.getCreatedBy())));
			details.add(new Entry("Created On", DateFormat.getDateInstance().format(attachment.getDateCreated())));
			try {
				attachment.computeFileLocation(s);
				details.add(new Entry("Path", attachment.getAttachmentFile().getAbsolutePath()));
			} catch (Exception e) {
				Intelligence2PlugIn.log(e.getMessage(), e);
				details.add(new Entry("Path", "ERROR: " +e.getMessage()));
			}
			properties.put(DETAILS_KEY, details);
		}finally{
			s.close();
		}
		
		
		try{
			Metadata metadata = ImageMetadataReader.readMetadata(attachment.getAttachmentFile());
			
			for (Directory directory : metadata.getDirectories()) {
				String name = directory.getName();
				List<Entry> details = properties.get(name);
				if (details == null){
					details = new ArrayList<AttachmentPropertiesDialog.Entry>();
					properties.put(name, details);
				}
				
				for (Tag g : directory.getTags()){
					details.add(new Entry(g.getTagName(), g.getDescription()));
				}
			}
		}catch (Exception ex){
			//eat this for whatever reason we cannot read image metadata 
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

	private class Entry{
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
