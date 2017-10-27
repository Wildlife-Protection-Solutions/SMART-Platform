package org.wcs.smart.asset.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A dialog to collect a comment
 * @author Emily
 *
 */
public class CommentDialog extends TitleAreaDialog {

	private Text txtComment;
	private String comment;
	
	private String title;
	private String message;
	
	public CommentDialog(Shell parentShell, String title, String message) {
		super(parentShell);
		this.title = title;
		this.message = message;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	public void okPressed() {
		this.comment = txtComment.getText();
		super.okPressed();
	}
	
	public String getComment() {
		return this.comment;
	}
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		txtComment = new Text(main, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setTitle(title);
		setMessage(message);
		getShell().setText(title);
		return parent;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
}
