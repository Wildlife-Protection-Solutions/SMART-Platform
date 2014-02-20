package org.wcs.smart.query.ui.newwizard;

import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

public abstract class ListHelpWizardPage extends WizardPage {

	private TableViewer options = null;
	protected Browser helpPage = null;

	protected ListHelpWizardPage(String pageName) {
		super(pageName);
	}

	public void setOptions (List<?> data, LabelProvider lblProvider){
		if (options != null){
			options.setInput(data);
			options.setLabelProvider(lblProvider);
		}
	}
	@Override
	public void createControl(Composite parent) {
		SashForm main = new SashForm(parent, SWT.HORIZONTAL);

		Composite tv = new Composite(main, SWT.NONE);
		tv.setLayout(new GridLayout());
		
		options = new TableViewer(tv, SWT.BORDER);
		options.setContentProvider(ArrayContentProvider.getInstance());
		options.getTable().setLinesVisible(false);

		TableColumn singleColumn = new TableColumn(options.getTable(), SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableColumnLayout.setColumnData(singleColumn, new ColumnWeightData(100));
		tv.setLayout(tableColumnLayout);
	
		Composite hp = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		hp.setLayout(gl);
		helpPage = new Browser(hp, SWT.NONE);
		helpPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		options.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateHelpPage();
			}
		});
		
		options.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (getWizard().canFinish()){
					//finish
				}else if (isPageComplete()){
					//getWizard().getContainer()
					//TODO MOVE NEXT
				}
				
			}
		});
		main.setWeights(new int[]{40,60});
		setTitle("New Query Wizard");
		super.setControl(main);
	}

	public Object getSelection(){
		return ((StructuredSelection)options.getSelection()).getFirstElement();
	}
	public abstract void updateHelpPage();
}
