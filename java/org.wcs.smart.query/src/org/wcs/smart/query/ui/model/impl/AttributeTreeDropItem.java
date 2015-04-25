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
package org.wcs.smart.query.ui.model.impl;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.TreeDropDownViewer;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;

/**
 * Attribute tree drop item
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTreeDropItem extends DropItem implements IFilterDropItem{

	private static AttributeTreeLabelProvider lProvider = null;
	private final static AttributeTreeContentProvider cProvider = new AttributeTreeContentProvider(true, false);
	
	protected String text;
	protected String key;
	protected Label lblAttribute;
	protected Label lblitem;

	private Font smallerFont;
	private Font smallerFont2;
	private Button btnEdit = null;
	
	private Attribute attribute = null;
	private List<AttributeTreeNode> roots = null;
	protected AttributeTreeNode currentSelection = null;
	private Object input = Collections.singletonList(Messages.AttributeTreeDropItem_LoadingText);
	private TreeDropDownViewer treeviewer;
	/*
	 * Job to load the attribute list options
	 */
	private Job loadItemsJobs = new Job(Messages.AttributeTreeDropItem_LoadingListItemJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				roots = QueryDataModelManager.getInstance().getActiveAttributeTreeNodes(attribute, s);
			}catch(Exception ex){
				QueryPlugIn.log("Could not initialize attribute tree items", ex); //$NON-NLS-1$
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			
			input = roots;
			if(treeviewer == null) return Status.OK_STATUS;
			Display d = treeviewer.getTreeViewer().getTree().getDisplay();
			if (d != null && !d.isDisposed()){
				d.asyncExec(new Runnable(){
					@Override
					public void run() {
						if (treeviewer == null || 
								treeviewer.getTreeViewer().getControl().isDisposed()){
							return;
						}
						treeviewer.getTreeViewer().setInput(roots);
						treeviewer.getTreeViewer().refresh();
					}
					
				});
			}
			return Status.OK_STATUS;
		}
	};
		
	/**
	 * Creates a new attribute list drop item
	 * 
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the category attribute to make up the drop item
	 */
	public AttributeTreeDropItem(CategoryAttribute att) {
		//super(parent, panel);
		this.key = "category:" + att.getCategory().getHkey() + ":attribute:" + att.getAttribute().getType().typeKey + ":" + att.getAttribute().getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.text = att.getAttribute().getName() + " (" + att.getCategory().getFullCategoryName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		this.attribute = att.getAttribute();
	}
	
	/**
	 * Creates a new attribute list drop item
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the attribute to make up the drop item
	 */
	public AttributeTreeDropItem(Attribute att) {
		//super(parent, panel);
		this.key = "attribute:" + att.getType().typeKey + ":" + att.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$
		this.text = att.getName() ;
		this.attribute = att;
	}

	
	/**
	 * @param data - the AttributeTreeNode
	 */
	@Override
	public void initializeData(Object data){
		currentSelection = (AttributeTreeNode) data;
	}

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		if (smallerFont2 != null){
			smallerFont2.dispose();
		}
		
		lblAttribute = null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return this.text + " = " + (currentSelection == null ? "" : currentSelection.getHkey()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder query = new StringBuilder(this.key);
		query.append(" = "); //$NON-NLS-1$

		if (currentSelection != null){
			query.append(currentSelection.getHkey());
		}
		return query.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);

		Composite t = new Composite(main, SWT.BORDER);
		gl = new GridLayout(2, false);
		gl.horizontalSpacing = 5;
		gl.verticalSpacing = 5;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		t.setLayout(gl);
		t.setBackground(Display.getDefault().getSystemColor( SWT.COLOR_WHITE ));
		
		lblitem = new Label(t, SWT.NONE);
		lblitem.setBackground(Display.getDefault().getSystemColor( SWT.COLOR_WHITE ));
		FontData fd = (lblitem.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont2 = new Font(Display.getCurrent(), fd);
		lblitem.setFont(smallerFont2);
		
		btnEdit = new Button(t, SWT.DOWN | SWT.ARROW);
		//btnEdit.setText("..."); //$NON-NLS-1$
		fd = (btnEdit.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 2);
		smallerFont = new Font(Display.getCurrent(), fd);
		btnEdit.setFont(smallerFont);
		initDrag(main);
		initDrag(lblAttribute);
		initDrag(t);
		initDrag(lblitem);
		
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showTree();
			}
		});
		
		if (currentSelection != null){
			lblitem.setText( formatStringForLabel(currentSelection.getName()));
		}else{
			lblitem.setText(""); //$NON-NLS-1$
		}
		
		lblAttribute.setText(formatStringForLabel(this.text + " = ")); //$NON-NLS-1$
		loadAttributes();
	}
	
	protected void loadAttributes(){
		loadItemsJobs.schedule();
	}
	
	protected void showTree(){
		
		treeviewer = getTreeEditor();
		if (treeviewer == null){
			 return;
		}
		
		treeviewer.getTreeViewer().setContentProvider(cProvider);
		if (lProvider == null){
			lProvider = new AttributeTreeLabelProvider();
		}
		treeviewer.getTreeViewer().setLabelProvider(lProvider);
		treeviewer.getTreeViewer().setInput(input);	
		
		treeviewer.positionAndShow(AttributeTreeDropItem.this.getWidget(), new ISelectionListener(){
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (selection != null && !selection.isEmpty()){
					currentSelection = (AttributeTreeNode) ((IStructuredSelection) selection).getFirstElement();
				}
				if (!lblitem.isDisposed()){
					if (currentSelection != null){
						lblitem.setText( formatStringForLabel(currentSelection.getName()));
					}else{
						lblitem.setText(""); //$NON-NLS-1$
					}
				}
				getTargetPanel().redraw();
				AttributeTreeDropItem.this.queryChanged();
			}});
	}

	protected TreeDropDownViewer getTreeEditor(){
		IDefinitionPanel pnl = getTargetPanel();
		if (pnl instanceof BasicFilterDefintionPanel){
			return ((BasicFilterDefintionPanel)pnl).getTreeEditor();
		}
		return null;
	}

}