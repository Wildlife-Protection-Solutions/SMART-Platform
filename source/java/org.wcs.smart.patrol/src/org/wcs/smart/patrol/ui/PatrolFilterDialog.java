package org.wcs.smart.patrol.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolType;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;

public class PatrolFilterDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	private Table table;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public PatrolFilterDialog(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		
		Session session = HibernateManager.openSession();
		
		shell = new Shell(getParent(), getStyle());
		shell.setSize(450, 300);
		shell.setText(getText());
		shell.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		
		
		ExpandableComposite compStart = new ExpandableComposite(composite, SWT.NONE);
		compStart.setText("Start Date");
		compStart.setExpanded(true);
		compStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ExpandableComposite patrolType = new ExpandableComposite(composite, SWT.NONE);
		patrolType.setText("Patrol Type");
		patrolType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		CheckboxTableViewer checkboxTableViewer = CheckboxTableViewer.newCheckList(patrolType, SWT.BORDER | SWT.FULL_SELECTION);
		table = checkboxTableViewer.getTable();
		compStart.setClient(table);
		
		
		checkboxTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		checkboxTableViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolType){
					return ((PatrolType) element).getType().getGuiName();
				}
				return super.getText(element);
			}
		});
		checkboxTableViewer.setInput(PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session).toArray());
		
		ExpandableComposite patrolId = new ExpandableComposite(composite, SWT.NONE);
		patrolId.setText("Patrol Id");
		patrolId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		

	}
}
