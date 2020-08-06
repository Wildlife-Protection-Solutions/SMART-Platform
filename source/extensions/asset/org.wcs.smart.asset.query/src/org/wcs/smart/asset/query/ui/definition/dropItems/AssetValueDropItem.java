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
package org.wcs.smart.asset.query.ui.definition.dropItems;

import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFormatOption;
import org.wcs.smart.asset.query.model.AssetValueOption;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Asset value drop item
 * @author egouge
 * @since 1.0.0
 */
public class AssetValueDropItem extends AbstractValueDropItem{

	
	private AssetValueOption item;
	private AssetFormatOption format = AssetFormatOption.DAYS_HOUR;
	
	private String guiLabel;
	/**
	 * Creates a new drop item
	 * @param item
	 */
	public AssetValueDropItem(AssetValueOption item, AssetFormatOption format){
		super(false);
		this.item = item;
		if (format != null) this.format = format;
		this.guiLabel = item.getGuiName(Locale.getDefault());
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueText()
	 */
	@Override
	public String getValueText() {
		return this.guiLabel;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#getValueQueryPart()
	 */
	@Override
	public String getValueQueryPart() {
		return ("asset:sum:" + item.getKeyPart() + ":" + format.getKey()); //$NON-NLS-1$ //$NON-NLS-2$
	}



	@Override
	protected void createValueComposite(Composite parent) {
		
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		Label l = new Label(temp, SWT.NONE);
		l.setText( formatStringForLabel( guiLabel ) );
		
		Link link = new Link(temp,  SWT.NONE);
		link.setText("(<a>" + this.format.getGuiName(Locale.getDefault()) + "</a>)");  //$NON-NLS-1$ //$NON-NLS-2$
		
		initDrag(temp);
		
		link.addListener(SWT.Selection, e->{
			FormatSelectionDialog dialog = new FormatSelectionDialog(parent.getShell());
			if (dialog.open() != Window.OK) return;
			this.format = dialog.getSelection();
			link.setText("(<a>" + this.format.getGuiName(Locale.getDefault()) + "</a>)"); //$NON-NLS-1$ //$NON-NLS-2$
			getTargetPanel().redraw();
			super.queryChanged();
		});
	}

	/**
	 * Does nothing
	 * @see org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem#initializeValueData(java.lang.Object)
	 */
	@Override
	protected void initializeValueData(Object data) {
	}

	
	class FormatSelectionDialog extends SmartStyledTitleDialog{

		private ComboViewer viewer;
		private AssetFormatOption selection;
		
		/**
		 * Creates new dialog
		 * @param parent
		 */
		public FormatSelectionDialog(Shell parent) {
			super(parent);
		}
		
		/**
		 * 
		 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
		 */
		@Override
		protected void buttonPressed(int buttonId) {
			if(buttonId == IDialogConstants.OK_ID){
				if (viewer.getSelection().isEmpty()){
					MessageDialog.openError(getShell(), Messages.AssetValueDropItem_ErrorTitle, Messages.AssetValueDropItem_SelectionRequired);
					return;
				}
				selection = (AssetFormatOption) ((IStructuredSelection)viewer.getSelection()).getFirstElement();
			}
			
			super.buttonPressed(buttonId);
		}
		
		/**
		 * @return the patrol value option selected for the encounter rate
		 * 
		 */
		public AssetFormatOption getSelection(){
			return this.selection;
		}
		
		
		@Override
		protected Control createDialogArea(Composite parent) {
			parent = (Composite)super.createDialogArea(parent);
			
			main = new Composite(parent, SWT.NONE);
			GridLayout gl = new GridLayout(2, false);
			gl.marginHeight = gl.marginWidth = 20;
			main.setLayout(gl);
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			
			Label lbl2 = new Label(main, SWT.WRAP);
			lbl2.setText("Results Format"); //$NON-NLS-1$
			
			viewer = new ComboViewer(main, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			viewer.setLabelProvider(new LabelProvider(){
				public String getText(Object element) {
					if (element instanceof AssetFormatOption){
						return ((AssetFormatOption) element).getGuiName(Locale.getDefault());
					}
					return super.getText(element);
				}
			});
					
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.setInput(AssetFormatOption.values());
			viewer.setSelection(new StructuredSelection(AssetValueDropItem.this.format));

			setMessage(Messages.AssetValueDropItem_FormatMessage);
			setTitle(Messages.AssetValueDropItem_FormatTitle);
			getShell().setText(Messages.AssetValueDropItem_FormatTitle);
			return main;
			
		}
		
		@Override
		public boolean isResizable(){
			return true;
		}

	}
}
