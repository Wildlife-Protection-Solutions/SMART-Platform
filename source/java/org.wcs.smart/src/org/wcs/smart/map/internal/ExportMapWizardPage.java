package org.wcs.smart.map.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;

import net.refractions.udig.project.IMap;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.BoundsStrategy;
import net.refractions.udig.project.ui.wizard.export.image.ExportMapToImageWizard;
import net.refractions.udig.project.ui.wizard.export.image.GeotiffImageExportFormat;
import net.refractions.udig.project.ui.wizard.export.image.ImageExportFormat;
import net.refractions.udig.project.ui.wizard.export.image.PDFImageExportFormat;
import net.refractions.udig.project.ui.wizard.export.image.WorldImageExportFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class ExportMapWizardPage extends WizardPage {

	public static final String WIDTH_KEY = "ExportMapWizardPageWidth";
	public static final String HEIGHT_KEY = "ExportMapWizardPageHeight";
	public static final String EXPORT_OP_KEY = "ExportMapWizardPageOption";
	public static final String ASPECT_KEY = "ExportMapWizardPageAspect";
	public static final String FORMAT_KEY = "ExportMapWizardPageFormat";
	public static final String DIR_KEY = "ExportMapWizardPageDir";
	
	private Text destDir;
	private Text exportMap;
	private ComboViewer cmbFormat;
	private Composite formatOpComp;
	
	private ArrayList<ImageExportFormat> formats;
	private Shell temporaryParent;
	private Control lastFormat;
	
	private IMap map;
	private Button btnMaintainBounds;
	private Button btnMaintainScale;
	private Spinner opWidth;
	private Spinner opHeight;
	private Button btnOpAspect;
	
	protected ExportMapWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		
		
		setPageComplete(true);
	}

	public void setSelection(IStructuredSelection selection){
		if (selection.isEmpty()){
			map = null;
			IMap activeMap = ApplicationGIS.getActiveMap();
            if (activeMap != ApplicationGIS.NO_MAP) {
                map = activeMap;
            }
			
		}else{
			for (Iterator<Object> iterator = selection.iterator(); iterator.hasNext();) {
				Object x = iterator.next();
				if (x instanceof IMap){
					map = (IMap) x;
					break;
				}
			}
		}
	}
	
	public IMap getMap(){
		return this.map;
	}
	
	public File getOutputDir(){
		return new File(this.destDir.getText());
	}
	
	
	public int getWidth(double mapwidth, double mapheight) {
		if (getSelectedFormat().useStandardDimensionControls()) {
			return opWidth.getSelection();
		} else {
			return getSelectedFormat().getWidth(mapwidth, mapheight);
		}
	}

	public int getHeight(double mapwidth, double mapheight) {
		int height;
		if (getSelectedFormat().useStandardDimensionControls()) {
			if (btnOpAspect.getSelection()) {
				height = (int) (mapheight / (mapwidth / getWidth(mapwidth,
						mapheight)));
			} else {
				height = opHeight.getSelection();
			}
		} else {
			height = getSelectedFormat().getHeight(mapwidth, mapheight);
		}

		return height;

	}
	    
	public BoundsStrategy getBoundsStrategy(){
		if (btnMaintainBounds.getSelection()){
			return new BoundsStrategy(map.getViewportModel().getBounds());
			
		}else{
			return new BoundsStrategy(map.getViewportModel().getScaleDenominator());
		}
	}
	 /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl( Composite parent ) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        comp.setLayout(layout);
        
        createExportDirectory(comp);
        createMapInfo(comp);

        createFormat(comp);
        createExportOptions(comp);
        createSizeOptions(comp);
        
        
        formatOpComp = new Composite(comp, SWT.BORDER);
        formatOpComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
        formatOpComp.setLayout(new FillLayout());
        
        init();
        
        setControl(comp);
    }
    
    
    private void init(){
    	 String defaultSize = getWizard().getDialogSettings().get(WIDTH_KEY);
         if (defaultSize == null) {
             opWidth.setSelection(1024);
         } else {
        	 opWidth.setSelection(getWizard().getDialogSettings().getInt(WIDTH_KEY));
         }
         
         defaultSize = getWizard().getDialogSettings().get(HEIGHT_KEY);
         if (defaultSize == null) {
             opHeight.setSelection(1024);
         } else {
        	 opHeight.setSelection(getWizard().getDialogSettings().getInt(HEIGHT_KEY));
         }
         
         defaultSize = getWizard().getDialogSettings().get(ASPECT_KEY);
         if (defaultSize == null){
        	 btnOpAspect.setSelection(true);        	 
         }else{
        	 btnOpAspect.setSelection(getWizard().getDialogSettings().getBoolean(ASPECT_KEY));
         }
         opHeight.setEnabled(!btnOpAspect.getSelection());
         
         defaultSize = getWizard().getDialogSettings().get(EXPORT_OP_KEY);
         if (defaultSize == null){
        	 btnMaintainBounds.setSelection(true);
         }else{
        	 if (getWizard().getDialogSettings().getBoolean(EXPORT_OP_KEY)){
        		 btnMaintainBounds.setSelection(true);
        		 btnMaintainScale.setSelection(false);
        	 }else{
        		 btnMaintainBounds.setSelection(false);
        		 btnMaintainScale.setSelection(true);
        	 }
         }
         
         String preferedFormat = getWizard().getDialogSettings().get(FORMAT_KEY);
         if (preferedFormat == null) {
             preferedFormat = "png"; //$NON-NLS-1$
         }
         ImageExportFormat defaultformat = formats.get(0);
         for( ImageExportFormat format : formats ) {
             if (format.getExtension().equalsIgnoreCase(preferedFormat)) {
            	 defaultformat = format;
                 break;
             }
             if (format.getExtension().equalsIgnoreCase("png")) { //$NON-NLS-1$
            	 defaultformat = format;
             }
         }
         cmbFormat.setSelection(new StructuredSelection(defaultformat));

         if (getWizard().getDialogSettings().get(DIR_KEY) != null){
        	 destDir.setText(getWizard().getDialogSettings().get(DIR_KEY));
         }
    }
    
    public void saveLastSelection(){
    	if (getSelectedFormat() != null){
    		getWizard().getDialogSettings().put(FORMAT_KEY, getSelectedFormat().getExtension());
    	}
    	
    	getWizard().getDialogSettings().put(WIDTH_KEY, Integer.valueOf(opWidth.getText()));
    	if (btnOpAspect.getSelection()){
    		getWizard().getDialogSettings().put(ASPECT_KEY, true);
    	}else{
    		getWizard().getDialogSettings().put(ASPECT_KEY, false);
    		getWizard().getDialogSettings().put(HEIGHT_KEY, Integer.valueOf(opHeight.getText()));
    	}
    	
    	if (btnMaintainBounds.getSelection()){
    		getWizard().getDialogSettings().put(EXPORT_OP_KEY, true);
    	}else{
    		getWizard().getDialogSettings().put(EXPORT_OP_KEY, false);
    	}
    	
    	getWizard().getDialogSettings().put(DIR_KEY, destDir.getText());
    }
    
    
    private void createSizeOptions(Composite parent){
    	Label lbl = new Label(parent, SWT.NONE);
    	//lbl.setText("Image Size:");
    	
    	Composite a = new Composite(parent, SWT.NONE);
    	a.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    	a.setLayout(new GridLayout(5, false));
    	
    	opWidth = createSpinner(WIDTH_KEY, a);
    	opHeight = createSpinner(HEIGHT_KEY, a);
    	btnOpAspect = new Button(a, SWT.CHECK);
    	btnOpAspect.setText("Preserve Aspect Ratio");
    	btnOpAspect.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnOpAspect.getSelection()){
					opHeight.setEnabled(false);
				}else{
					opHeight.setEnabled(true);
				}
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
    }
    
    private Spinner createSpinner( String spinnerKey, Composite comp ) {
        Label label = new Label(comp, SWT.NONE);
        if (spinnerKey == WIDTH_KEY){
        	label.setText("Width:");
        }else if (spinnerKey == HEIGHT_KEY){
        	label.setText("Height:");
        }
        label.setLayoutData(new GridData());

        Spinner spinner = new Spinner(comp, SWT.BORDER);
        initSpinner(spinner, spinnerKey);
        return spinner;
    }

    private void initSpinner( Spinner spinner, String sizeKey ) {
        spinner.setDigits(0);
        spinner.setIncrement(1);
        spinner.setPageIncrement(100);
        spinner.setMinimum(10);
        spinner.setMaximum(20000);

       

        spinner.setLayoutData(new GridData());
    }
    
    private void createExportOptions(Composite comp){
    	Label lbl = new Label(comp, SWT.NONE);
    	lbl.setText("Export Options:");
    	Composite compa = new Composite(comp, SWT.NONE);
    	compa.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,2,1));
    	compa.setLayout(new GridLayout(2, false));
    	btnMaintainBounds = new Button(compa, SWT.RADIO);
    	btnMaintainBounds.setText("Preserve Bounds");
    	btnMaintainBounds.setSelection(true);
    	
    	btnMaintainScale = new Button(compa, SWT.RADIO);
    	btnMaintainScale.setText("Preserve Scale");
    	
    }
    private void createFormat(Composite comp) {
        Label scaleLabel = new Label(comp, SWT.NONE);
        scaleLabel.setText("Image Format:");
        scaleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
                false));
        cmbFormat = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        cmbFormat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        cmbFormat.setContentProvider(ArrayContentProvider.getInstance());
        cmbFormat.setLabelProvider(new LabelProvider(){
        	@Override
        	public String getText(Object element){
        		if (element instanceof ImageExportFormat){
        			return ((ImageExportFormat) element).getName();
        		}
        		return super.getText(element);
        	}
        });
        
        loadFormats();
        cmbFormat.setInput(formats);
        
        cmbFormat.getCombo().addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ImageExportFormat format = getSelectedFormat();
				if (format == null){
					return;
				}
				if (lastFormat != null){
					lastFormat.setParent(temporaryParent);
				}
				lastFormat = format.getControl();
				
				lastFormat.setParent(formatOpComp);
				formatOpComp.layout();
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
    }
    public ImageExportFormat getSelectedFormat(){
    	if (cmbFormat.getSelection().isEmpty()){
    		return null;
    	}
    	return (ImageExportFormat) ((IStructuredSelection)cmbFormat.getSelection()).getFirstElement();
    }
    
    private void createMapInfo(Composite comp) {
        Label scaleLabel = new Label(comp, SWT.NONE);
        scaleLabel.setText("Export Map");
        scaleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
                false));
        exportMap = new Text(comp, SWT.SINGLE|SWT.BORDER);
        exportMap.setEditable(false);
        exportMap.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        if (map != null){
        	exportMap.setText(map.getName());
        }else{
        	setErrorMessage("No map found to export");
        }
    }
    
    private void createExportDirectory(Composite comp) {
        Label scaleLabel = new Label(comp, SWT.NONE);
        scaleLabel.setText("Export Directory");
        scaleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
                false));
        
        destDir = new Text(comp, SWT.SINGLE | SWT.BORDER);
        destDir.setToolTipText("The location to export the map to");
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        destDir.setLayoutData(gridData);
        String previousLocation = getWizard().getDialogSettings().get(ExportMapToImageWizard.DIRECTORY_KEY);
        if (previousLocation != null) {
        	destDir.setText(previousLocation);
        } else {
        	destDir.setText(Platform.getLocation().toOSString());
        }
        
        Button browse = new Button(comp, SWT.PUSH);
        browse.setText("Browse");
        browse.setToolTipText("Select export location");
        browse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		browse.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog d = new DirectoryDialog(e.widget.getDisplay()
						.getActiveShell());
				String selection = d.open();
				if (selection != null) {
					destDir.setText(selection);
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
    }
    
    
    
    
    private void loadFormats() {

        formats = new ArrayList<ImageExportFormat>();
        formats.addAll(loadImageWriterSpis());
        formats.add(new GeotiffImageExportFormat());
        formats.add(new PDFImageExportFormat());

        Collections.sort(formats, new Comparator<ImageExportFormat>(){

            public int compare( ImageExportFormat format1, ImageExportFormat format2 ) {
                String name1 = format1.getName().toLowerCase();
                String name2 = format2.getName().toLowerCase();
                return name1.compareTo(name2);
            }

        });

        temporaryParent = new Shell();
        // this is to make sure that temporaryParent is also disposed
        cmbFormat.getControl().addDisposeListener(new DisposeListener(){
            public void widgetDisposed( DisposeEvent e ) {
                temporaryParent.dispose();
            }

        });

        for( ImageExportFormat format : formats ) {
            format.createControl(temporaryParent);
        }

    }

    private List<WorldImageExportFormat> loadImageWriterSpis() {
        IIORegistry defaultInstance = IIORegistry.getDefaultInstance();
        Iterator<ImageWriterSpi> writers = defaultInstance.getServiceProviders(
                ImageWriterSpi.class, false);
        List<WorldImageExportFormat> formats = new ArrayList<WorldImageExportFormat>();
        while( writers.hasNext() ) {
            ImageWriterSpi writer = writers.next();
            formats.add(new WorldImageExportFormat(writer.getFormatNames()[0], writer
                    .getFileSuffixes()[0]));
        }

        return formats;
    }
}
