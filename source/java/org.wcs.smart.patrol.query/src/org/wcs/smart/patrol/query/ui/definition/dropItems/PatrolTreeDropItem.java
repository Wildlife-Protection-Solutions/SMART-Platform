/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.ui.definition.dropItems;

import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
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
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.datamodel.ITreeNode;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IPatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.query.QueryFilterConfigManager;
import org.wcs.smart.query.QueryFilterConfigManager.IConfigurationChangeListener;
import org.wcs.smart.query.common.model.QueryFilterConfiguration;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.ui.ca.datamodel.TreeDropDownViewer;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.IDefinitionPanel;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.TreeNodeLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Patrol drop item for a patrol option 
 * that contains a tree (custom patrol attribute).
 * 
 * @author Emily
 * @since 8.1.0
 */
public class PatrolTreeDropItem<T extends ITreeNode<?>> extends DropItem implements IFilterDropItem{

	private String keyPart;
	private String text;
	private IPatrolQueryOption option;
	private IPatrolOptionData data;
	
	private Label lblAttribute, lblitem;
	private ControlDecoration cd;
	private Button btnEdit;
	
	private Font smallerFont = null;
	
	private ListItem currentSelection = null;
	protected Object input = DialogConstants.LOADING_TEXT;
	protected TreeDropDownViewer treeviewer;

	private IConfigurationChangeListener queryConfChangeListener = new IConfigurationChangeListener() {
		@Override
		public void configurationChanged(QueryFilterConfiguration config) {
			loadItemsJobs.cancel();
			loadItemsJobs.schedule();
		}
	};

	/*
	 * job for loading options
	 */
	private Job loadItemsJobs = new Job(Messages.PatrolTreeDropItem_loading){ 

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			try(Session s = HibernateManager.openSession()){
				List<? extends ITreeNode<?>> nodes = data.getValuesTree(s);
				input = nodes;
				if (currentSelection != null) {
					ITreeNode<?>[] current = new ITreeNode[1];
					for (ITreeNode<?> n : nodes) {
						n.accept(vx->{
							if (vx.getHkey().equals(currentSelection.getKey())){
								current[0] = vx;
								return false;
							}
							return true;
						});
						if (current[0] != null) break;
					}
					if (current[0] != null) currentSelection = new ListItem(null, current[0].getName(), current[0].getHkey());
					
				}
			}
			
			//update label
			Display.getDefault().asyncExec(() ->updateLabel());
			
			//update tree viewer
			if(treeviewer == null) return Status.OK_STATUS;
			
			Display d = treeviewer.getTreeViewer().getTree().getDisplay();
			if (d == null || !d.isDisposed()) return Status.OK_STATUS;
				
			d.asyncExec(() ->{
				if (treeviewer == null || 
					treeviewer.getTreeViewer().getControl().isDisposed()){
					return;
				}
				treeviewer.getTreeViewer().setInput(input);
				treeviewer.getTreeViewer().refresh();
			});			

			return Status.OK_STATUS;
		}};
		


	/**
	 * Creates a new patrol list drop item
	 *  
	 * @param parent parent
	 * @param target target item
	 * @param option patrol filter option
	 */
	public PatrolTreeDropItem(IPatrolQueryOption option) {
		this.keyPart = option.getKey();
		this.text = option.getGuiName(Locale.getDefault());
		this.option = option;
	}

	
	public void dispose(){
		QueryFilterConfigManager.getInstance().removeChangeListener(queryConfChangeListener);
		super.dispose();
		loadItemsJobs.cancel();
		if (smallerFont != null){
			smallerFont.dispose();
		}
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder(this.text);
		sb.append(" = "); //$NON-NLS-1$
		if (currentSelection != null) {	
			sb.append("\""); //$NON-NLS-1$
			sb.append(currentSelection.getName());
			sb.append("\""); //$NON-NLS-1$
		}
		return sb.toString();
	}

	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder(this.keyPart);
		sb.append(" equals "); //$NON-NLS-1$
		ListItem it = currentSelection;
		if (it != null){
			sb.append("\""); //$NON-NLS-1$
			if (option == PatrolQueryOption.PATROL_TYPE){
				sb.append(it.getKey().toUpperCase(Locale.ROOT));
			}else if (option.getType() == PatrolQueryOptionType.KEY){
				sb.append(it.getKey());
			}else{
				if (it.getUuid() != null){
					sb.append( UuidUtils.uuidToString(it.getUuid()));
				}else {
					sb.append(it.getKey());
				}
			}
			sb.append("\""); //$NON-NLS-1$
		}
		
		return sb.toString();
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
		main.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		lblAttribute = new Label(main, SWT.NONE);

		Composite t = new Composite(main, SWT.BORDER);
		gl = new GridLayout(2, false);
		gl.horizontalSpacing = 5;
		gl.verticalSpacing = 5;
		gl.marginLeft = 5;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		t.setLayout(gl);
		t.setBackground(Display.getDefault().getSystemColor( SWT.COLOR_WHITE ));
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblitem = new Label(t, SWT.NONE);
		lblitem.setBackground(Display.getDefault().getSystemColor( SWT.COLOR_WHITE ));
		FontData fd = (lblitem.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		
		smallerFont = new Font(Display.getCurrent(), fd);
		lblitem.setFont(smallerFont);
		lblitem.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)lblitem.getLayoutData()).horizontalIndent = 4;
		cd = new ControlDecoration(lblitem, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
		cd.setDescriptionText(Messages.PatrolTreeDropItem_novalue);
		cd.hide();
		
		btnEdit = new Button(t, SWT.DOWN | SWT.ARROW);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		fd = (btnEdit.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 2);
		smallerFont = new Font(Display.getCurrent(), fd);
		btnEdit.setFont(smallerFont);
		initDrag(main);
		initDrag(lblAttribute);
		initDrag(t);
		initDrag(lblitem);
		btnEdit.addListener(SWT.Selection,e->showTree());
		
		if (currentSelection != null){
			lblitem.setText( formatStringForLabel(currentSelection.getName()));
		}else{
			lblitem.setText(""); //$NON-NLS-1$
			cd.show();
		}
		
		lblAttribute.setText(formatStringForLabel(this.text + " = ")); //$NON-NLS-1$		
		loadItemsJobs.schedule();
	}

	/**
	 * This expects an array with two elements.  The first is
	 * the patrol option data providing the additional details, the
	 * second is optional and can be null or the current selection.  
	 * @param data a array {PatrolOptionData, ListItem}
	 */
	@Override
	public void initializeData(Object data) {
		Object[] values = (Object[]) data;
		this.data = (IPatrolOptionData) values[0];
		if (values.length < 2 || values[1] == null){
			this.currentSelection = this.data.getDefaultListItem();	
		}else{
			currentSelection = (ListItem)values[1];
		}
		//add configuration listener is data state depends on configuration
		if (this.data.isDependOnQueryConfiguration()) {
			QueryFilterConfigManager.getInstance().addChangeListener(queryConfChangeListener);
		}
	}
	
	private void updateLabel() {
		if (!lblitem.isDisposed()){
			if (currentSelection != null){
				lblitem.setText( formatStringForLabel(currentSelection.getName()));
				cd.hide();
			}else{
				lblitem.setText(""); //$NON-NLS-1$
				cd.show();
			}
		}
		getTargetPanel().redraw();
	}
	protected void showTree(){
		
		treeviewer = getTreeEditor();
		if (treeviewer == null) return;
		
		treeviewer.getTreeViewer().setLabelProvider(new TreeNodeLabelProvider(IconManager.Size.ICON));
		treeviewer.getTreeViewer().setInput(input);	
		
		treeviewer.positionAndShow(PatrolTreeDropItem.this.getWidget(), new ISelectionListener(){
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (selection != null && !selection.isEmpty()){
					ITreeNode<?> node = (ITreeNode<?>) ((IStructuredSelection) selection).getFirstElement();
					currentSelection = new ListItem(null, node.getName(), node.getHkey()); 
				}
				updateLabel();
				PatrolTreeDropItem.this.queryChanged();
			}});
	}
	
	protected TreeDropDownViewer getTreeEditor(){
		IDefinitionPanel pnl = getTargetPanel();
		return pnl.getTreeEditor();
	}
}
