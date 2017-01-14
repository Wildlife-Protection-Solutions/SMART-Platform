package org.wcs.smart.i2.ui.dialogs.query;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.i2.query.export.CsvQueryExporter;
import org.wcs.smart.i2.query.export.IQueryExporter;
import org.wcs.smart.i2.query.export.ShpQueryExporter;


public class QueryFormatPage extends WizardPage {

	private static final String LAST_EXPORT_FORMAT = "org.wcs.smart.i2.query.export.format"; //$NON-NLS-1$
	
	private TableViewer outputOptions;
	
	/**
	 * @param pageName
	 */
	protected QueryFormatPage() {
		super("Export_Format");
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		
		main.setLayout(new GridLayout(1, false));
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Format:");
		
		outputOptions = new TableViewer(main, SWT.BORDER | SWT.SINGLE );
		outputOptions.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outputOptions.setContentProvider(ArrayContentProvider.getInstance());
		outputOptions.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof IQueryExporter){
					IQueryExporter exp = (IQueryExporter) element;
					String name = exp.getName(Locale.getDefault());
					String ext = exp.getExtension();
					if (ext != null){
						name = name  + " (*." + ext + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return name;					
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		});
		List<IQueryExporter> exports = new ArrayList<>();
		exports.add(new CsvQueryExporter());
		exports.add(new ShpQueryExporter());
		
		Collections.sort(exports, new Comparator<IQueryExporter>() {
			@Override
			public int compare(IQueryExporter o1, IQueryExporter o2) {
				return Collator.getInstance().compare(o1.getName(Locale.getDefault()), o2.getName(Locale.getDefault()));
			}
		});
		outputOptions.setInput(exports);
		
		outputOptions.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(!outputOptions.getSelection().isEmpty());
			}
		});
		outputOptions.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (!outputOptions.getSelection().isEmpty()){
					setPageComplete(true);
					((ExportQueryWizard)getWizard()).getContainer().showPage( getNextPage() );
				}
				
			}
		});
		
		String id = getWizard().getDialogSettings().get(LAST_EXPORT_FORMAT);
		IQueryExporter defaultExport = null;
		for (IQueryExporter export : exports){
			if (export.getClass().getName().equals(id)){
				defaultExport = export;
			}
		}
		if (defaultExport != null){
			outputOptions.setSelection(new StructuredSelection(defaultExport));
		}
		
		setTitle(MessageFormat.format("Export Query: {0}", ((ExportQueryWizard)getWizard()).getQuery().getName())); //$NON-NLS-1$
		setMessage("Exports the current query results to file");
		setControl(main);
		setPageComplete(!outputOptions.getSelection().isEmpty());
	}

	/**
	 * @return the exporter for the selected export option
	 */
	public IQueryExporter getQueryExporter(){
		 return (IQueryExporter) ((IStructuredSelection)outputOptions.getSelection()).getFirstElement();
	}
	
	public void performFinish(){
		try{
			IQueryExporter f = getQueryExporter();
			getWizard().getDialogSettings().put(LAST_EXPORT_FORMAT, f.getClass().getName());
		}catch (Exception ex){
			//eatme
		}
	}

}

