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
package org.wcs.smart.map.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.BoundsStrategy;
import org.locationtech.udig.project.ui.wizard.export.image.ExportMapToImageWizard;
import org.locationtech.udig.project.ui.wizard.export.image.GeotiffImageExportFormat;
import org.locationtech.udig.project.ui.wizard.export.image.ImageExportFormat;
import org.locationtech.udig.project.ui.wizard.export.image.PDFImageExportFormat;
import org.locationtech.udig.project.ui.wizard.export.image.WorldImageExportFormat;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
/**
 * Custom export map to image wizard page for gathering
 * map and export options.  Based on the udig ExportMapToImage wizard 
 * pages.
 * 
 * @author Emily
 *
 */
public class ExportMapWizardPage extends WizardPage {

	/*
	 * Keys for storing dialog last values
	 */
	private static final String SIZE_KEY = "ExportMapWizardSizeValue"; //$NON-NLS-1$
	private static final String CUSTOM_SIZE_KEY = "ExportMapWizardCustomSize"; //$NON-NLS-1$
	private static final String WIDTH_KEY = "ExportMapWizardPageWidth"; //$NON-NLS-1$
	private static final String HEIGHT_KEY = "ExportMapWizardPageHeight"; //$NON-NLS-1$
	private static final String EXPORT_OP_KEY = "ExportMapWizardPageOption"; //$NON-NLS-1$
	private static final String ASPECT_KEY = "ExportMapWizardPageAspect"; //$NON-NLS-1$
	private static final String FORMAT_KEY = "ExportMapWizardPageFormat"; //$NON-NLS-1$
	private static final String DIR_KEY = "ExportMapWizardPageDir"; //$NON-NLS-1$
	
	private Text destDir;
	private Text fileName;
	private ComboViewer cmbMap;
	private ComboViewer cmbFormat;
	private Composite formatOpComp;
	
	private ArrayList<ImageExportFormat> formats;
	private Shell temporaryParent;
	private Control lastFormat;
	
	private IMap defaultMap;
	private Button btnMaintainBounds;
	private Button btnMaintainScale;
	private Spinner opWidth;
	private Spinner opHeight;
	private Button btnOpAspect;
	
	private Scale opSize;
	private Button chCustomSize;
	private Label low;
	private Label high;
	
	/**
	 * Creates a new page
	 */
	protected ExportMapWizardPage() {
		super("ExportOps", Messages.ExportMapWizardPage_PageTitle, SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.WIZBAN_EXPORT_IMAGE)); //$NON-NLS-1$
		
		setMessage(Messages.ExportMapWizardPage_PageDescription);
		setPageComplete(true);
	}

	/**
	 * Sets the current map selection.  If no map found
	 * the current active map is used.
	 * @param selection
	 */
	public void setSelection(IStructuredSelection selection) {
		defaultMap = null;
		for (@SuppressWarnings("unchecked")
		Iterator<Object> iterator = selection.iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof IMap) {
				defaultMap = (IMap) x;
				break;
			}
		}
		
		if (defaultMap == null) {
			Collection<? extends IMap> visibleMaps = ApplicationGIS.getVisibleMaps();
			IMap activeMap = ApplicationGIS.getActiveMap();
			if (activeMap != ApplicationGIS.NO_MAP && visibleMaps.contains(activeMap)) {
				defaultMap = activeMap;
			}else{
				if (!visibleMaps.isEmpty()){
					defaultMap = visibleMaps.iterator().next();
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @return the selected map
	 */
	public IMap getMap(){
		return (IMap) ((IStructuredSelection)this.cmbMap.getSelection()).getFirstElement();
	}
	
	/**
	 * 
	 * @return the selected filename
	 */
	public String getFileName(){
		return fileName.getText();
	}
	
	/**
	 * 
	 * @return the export directory
	 */
	public File getOutputDir(){
		return new File(this.destDir.getText());
	}
	
	/**
	 * Computes the width of the output map based
	 * on the format settings and the current map
	 * settings. 
	 * 
	 * @return output image width
	 */
	public int getWidth() {
		if (getSelectedFormat().useStandardDimensionControls()) {
			if (!(Boolean)chCustomSize.getSelection()){
				return getWidth(opSize.getSelection());
			}
			return opWidth.getSelection();
		} else {
			return getSelectedFormat().getWidth(getMap().getViewportModel().getWidth(), getMap().getViewportModel().getHeight());
		}
	}

	/**
	 * Predefined quality values for opSize
	 * @param quality
	 * @return
	 */
	private int getWidth(int quality){
		switch(quality){
		case 1:
			return 512;
		case 2:
			return 750;
		case 3:
			return 1024;
		case 4:
			return 1250;
		case 5:
			return 1500;
		case 6:
			return 1750;
		case 7:
			return 2048;
		case 8:
			return 2500;
		case 9:
			return 3072;
		case 10:
			return 4096;
		default:
			return 1024;
		}
	}
	/**
	 * Computes the height of the output image based
	 *  on the format settings, scale options
	 *  and current map.
	 *  
	 * @return output image height
	 */
	public int getHeight() {
		IMap map = getMap();
		double mapwidth = map.getViewportModel().getWidth();
		double mapheight = map.getViewportModel().getHeight();
		int height;
		if (getSelectedFormat().useStandardDimensionControls()) {
			if (!(Boolean)chCustomSize.getSelection()){
				height = (int) (mapheight / (mapwidth / getWidth()));
			}else{
				if (btnOpAspect.getSelection()) {
					height = (int) (mapheight / (mapwidth / getWidth()));
				} else {
					height = opHeight.getSelection();
				}
			}
		} else {
			height = getSelectedFormat().getHeight(mapwidth, mapheight);
		}
		return height;
	}
	    
	/**
	 * Bounds strategy to use when exporting map.  Either
	 * preserves bounds or scale.
	 * 
	 * @return
	 */
	public BoundsStrategy getBoundsStrategy(){
		IMap map = getMap();
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
        
        createMapInfo(comp);
        createExportDirectory(comp);
        createFileName(comp);

        createFormat(comp);
        createExportOptions(comp);
        createSizeOptions(comp);
        
        
        formatOpComp = new Composite(comp, SWT.NONE);
        formatOpComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
        formatOpComp.setLayout(new FillLayout());
        
        init();
        validate();
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
         
         
         cmbMap.setInput(ApplicationGIS.getOpenMaps());
         if (defaultMap != null){
        	 cmbMap.setSelection(new StructuredSelection(defaultMap));
         }
         if (getWizard().getDialogSettings().get(CUSTOM_SIZE_KEY) != null){
        	 chCustomSize.setSelection(getWizard().getDialogSettings().getBoolean(CUSTOM_SIZE_KEY));
         }
         if (getWizard().getDialogSettings().get(SIZE_KEY) != null){
        	 opSize.setSelection(getWizard().getDialogSettings().getInt(SIZE_KEY));
         }
         updateCustomSizeEnabled();
    }
    
    private void validate(){
    	String error = null;
    	if (fileName.getText().trim().isEmpty()){
    		error = Messages.ExportMapWizardPage_InvalidFileName;
    	}
    	if (destDir.getText().trim().isEmpty()){
    		error = Messages.ExportMapWizardPage_InvalidDestinationFolder;
    	}
    	if (cmbMap.getSelection().isEmpty()){
    		error = Messages.ExportMapWizardPage_NoMapFoundErrorMsg;
    	}
    	setErrorMessage(error);
    }
    /**
     * Saves selections to dialog settings store.
     */
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
    	getWizard().getDialogSettings().put(CUSTOM_SIZE_KEY, chCustomSize.getSelection());
    	getWizard().getDialogSettings().put(SIZE_KEY, opSize.getSelection());
    	
    }
    
    private void createFileName(Composite parent){
    	Label lbl = new Label(parent, SWT.NONE);
    	lbl.setText(Messages.ExportMapWizardPage_Filename);
    	
    	fileName = new Text(parent, SWT.BORDER);
    	fileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    	fileName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});
    }
    
    
    
    private void createSizeOptions(Composite parent){
    	new Label(parent, SWT.NONE);
    	
    	Composite outer = new Composite(parent, SWT.NONE);
    	outer.setLayout(new GridLayout(2, false));
    	
    	Label l = new Label(outer, SWT.NONE);
    	l.setText("Image Quality:");
    	l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    	
    	opSize = new Scale(outer, SWT.HORIZONTAL);
    	opSize.setMinimum(1);
    	opSize.setMaximum(10);
    	opSize.setIncrement(1);
    	opSize.setPageIncrement(1);
    	opSize.setSelection(5);
    	opSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	((GridData)opSize.getLayoutData()).heightHint = 23;
    	
    	opSize.addListener(SWT.MouseUp, (event)->{
    		float percent = ((float)event.x) / ((float)opSize.getSize().x);
    		float pos = (opSize.getMaximum() - opSize.getMinimum()) * percent;
    		int value = Math.round(pos+opSize.getMinimum()) ;
    		opSize.setSelection(value);
    		event.doit = false;
    	});
    	new Label(outer, SWT.NONE); //spacer
    	
    	Composite b= new Composite(outer, SWT.NONE);
    	b.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	b.setLayout(new GridLayout(2, false));
    	((GridLayout)b.getLayout()).marginWidth = 0;
    	((GridLayout)b.getLayout()).marginHeight = 0;
    	
    	low = new Label(b, SWT.NONE);
    	low.setText("Low/Small");
    	low.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    	((GridData)low.getLayoutData()).horizontalIndent = 6;
    	
    	high = new Label(b, SWT.NONE);
    	high.setText("High/Large");
    	high.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));		
    	((GridData)high.getLayoutData()).horizontalIndent = 10;
    	
    	new Label(outer, SWT.NONE); //spacer
    	
    	chCustomSize = new Button(outer, SWT.CHECK);
    	chCustomSize.setText("Custom");
    	chCustomSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	((GridData)chCustomSize.getLayoutData()).verticalIndent = 3;
    	new Label(outer, SWT.NONE); //spacer
    	
    	Composite a = new Composite(outer, SWT.NONE);
    	a.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    	a.setLayout(new GridLayout(5, false));
    	((GridLayout)a.getLayout()).marginWidth = 0;
    	opWidth = createSpinner(WIDTH_KEY, a);
    	opHeight = createSpinner(HEIGHT_KEY, a);
    	btnOpAspect = new Button(a, SWT.CHECK);
    	btnOpAspect.setText(Messages.ExportMapWizardPage_AspectRatioLabel);
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
			}
		});
    	
    	chCustomSize.addListener(SWT.Selection, (event) -> {
    		updateCustomSizeEnabled();
    	});
    	updateCustomSizeEnabled();
    }
    
    private void updateCustomSizeEnabled(){
    	boolean enabled = chCustomSize.getSelection();
    	opSize.setEnabled(!enabled);
		opWidth.setEnabled(enabled);
		btnOpAspect.setEnabled(enabled);
		low.setEnabled(!enabled);
		high.setEnabled(!enabled);
		((Label)opWidth.getData()).setEnabled(enabled);
		((Label)opHeight.getData()).setEnabled(enabled);
		if (!enabled){
			opHeight.setEnabled(false);
		}else{
			opHeight.setEnabled(!btnOpAspect.getSelection());
		}
    }
    
    private Spinner createSpinner( String spinnerKey, Composite comp ) {
        Label label = new Label(comp, SWT.NONE);
        if (spinnerKey == WIDTH_KEY){
        	label.setText(Messages.ExportMapWizardPage_WidthLabel);
        }else if (spinnerKey == HEIGHT_KEY){
        	label.setText(Messages.ExportMapWizardPage_HeightLabel);
        }
        label.setLayoutData(new GridData());

        Spinner spinner = new Spinner(comp, SWT.BORDER);
        spinner.setData(label);
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
    	lbl.setText(Messages.ExportMapWizardPage_ExportOptionsLabel);
    	Composite compa = new Composite(comp, SWT.NONE);
    	compa.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,2,1));
    	compa.setLayout(new GridLayout(2, false));
    	btnMaintainBounds = new Button(compa, SWT.RADIO);
    	btnMaintainBounds.setText(Messages.ExportMapWizardPage_BoundsOpLabel);
    	btnMaintainBounds.setSelection(true);
    	
    	btnMaintainScale = new Button(compa, SWT.RADIO);
    	btnMaintainScale.setText(Messages.ExportMapWizardPage_ScaleOpLabel);
    	
    }
    private void createFormat(Composite comp) {
        Label scaleLabel = new Label(comp, SWT.NONE);
        scaleLabel.setText(Messages.ExportMapWizardPage_ImgFormatLabel);
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
        cmbFormat.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
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
        scaleLabel.setText(Messages.ExportMapWizardPage_ExportMapLabel);
        scaleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
                false));
        cmbMap = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        cmbMap.setLabelProvider(new LabelProvider(){
        	@Override
        	public String getText(Object element){
        		if (element instanceof IMap){
        			return ((IMap) element).getName();
        		}
        		return super.getText(element);
        	}
        });
        cmbMap.setContentProvider(ArrayContentProvider.getInstance());
        cmbMap.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        cmbMap.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fileName.setText(getMap().getName());
				validate();
			}
		});
    }
    
    private void createExportDirectory(Composite comp) {
        Label scaleLabel = new Label(comp, SWT.NONE);
        scaleLabel.setText(Messages.ExportMapWizardPage_ExportdirLabel);
        scaleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
                false));
        
        destDir = new Text(comp, SWT.SINGLE | SWT.BORDER);
        destDir.setToolTipText(Messages.ExportMapWizardPage_ExportDirToolTip);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        destDir.setLayoutData(gridData);
        String previousLocation = getWizard().getDialogSettings().get(ExportMapToImageWizard.DIRECTORY_KEY);
        if (previousLocation != null) {
        	destDir.setText(previousLocation);
        } else {
        	destDir.setText(Platform.getLocation().toOSString());
        }
        destDir.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
				
			}
		});
        
        Button browse = new Button(comp, SWT.PUSH);
        browse.setText(Messages.ExportMapWizardPage_BrowseButton);
        browse.setToolTipText(Messages.ExportMapWizardPage_BrowseButtonTooltip);
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
        Set<String> items = new HashSet<String>();
        
        while( writers.hasNext() ) {
            ImageWriterSpi writer = writers.next();
            String key = writer.getFormatNames()[0] + "_" + writer.getFileSuffixes()[0] + "_";
            if (!items.contains(key)){
            	formats.add(new WorldImageExportFormat(writer.getFormatNames()[0], writer
                    .getFileSuffixes()[0]));
            	items.add(key);
            }
        }

        return formats;
    }
}
