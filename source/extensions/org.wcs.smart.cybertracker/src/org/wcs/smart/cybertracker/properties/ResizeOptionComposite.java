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
package org.wcs.smart.cybertracker.properties;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.ImageSizeOption;

/**
 * Composite for collection image resizing options
 * 
 * @author Emily
 *
 */
public class ResizeOptionComposite extends Composite {

	private ComboViewer cmbSize;
	private Text txtWidth, txtHeight;
	private Label[] labels;
	private Label sizeLabel;
	private Label infoLabel;
	
	private String getLabel(ImageSizeOption op){
		switch(op){
		case KP300: return "640 x 480"; //$NON-NLS-1$
		case MP1: return "1280 x 960"; //$NON-NLS-1$
		case MP2: return "1600 x 1200"; //$NON-NLS-1$
		case MP3: return "2048 x 1536"; //$NON-NLS-1$
		case MP5: return "2560 x 1920"; //$NON-NLS-1$
		case MP6: return "2816 x 2112"; //$NON-NLS-1$
		case MP8: return "3264 x 2468"; //$NON-NLS-1$
		case MP12: return "4200 x 2800"; //$NON-NLS-1$
		case CUSTOM: return Messages.ResizeOptionComposite_CustomLabel;
		
		}
		return ""; //$NON-NLS-1$
	}
	
	public ResizeOptionComposite(Composite parent){
		super(parent, SWT.NONE);
		createControl();
	}
	
	protected void createControl(){
		setLayout(new GridLayout());
		
		Composite sizeComp = new Composite(this, SWT.NONE);
		sizeComp.setLayout(new GridLayout(2, false));
		sizeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)sizeComp.getLayoutData()).horizontalIndent = 20;
		((GridLayout)sizeComp.getLayout()).marginHeight = 0;
		
		infoLabel = new Label(sizeComp, SWT.NONE);
		infoLabel.setText(Messages.ResizeOptionComposite_infoLabel);
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		sizeLabel = new Label(sizeComp, SWT.NONE);
		sizeLabel.setText(Messages.ResizeOptionComposite_size);
		
		
		cmbSize = new ComboViewer(sizeComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbSize.setContentProvider(ArrayContentProvider.getInstance());
		cmbSize.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof ImageSizeOption){
					return getLabel( ((ImageSizeOption) element) );
				}
				return super.getText(element);
			}
		});
		cmbSize.setInput(ImageSizeOption.values());
		cmbSize.setSelection(new StructuredSelection(ImageSizeOption.MP1));
		cmbSize.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateControls();
				notifyListeners(SWT.Modify, new Event());
			}
		});
		cmbSize.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite customSize = new Composite(this, SWT.NONE);
		customSize.setLayout(new GridLayout(6, false));
		customSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)customSize.getLayoutData()).horizontalIndent = 20;
		((GridLayout)customSize.getLayout()).marginHeight = 0;
		
		labels = new Label[4];
		labels[0] = new Label(customSize, SWT.NONE);
		labels[0] .setText(Messages.ResizeOptionComposite_widthLabel);
		
		txtWidth = new Text(customSize, SWT.BORDER);
		txtWidth.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtWidth.addListener(SWT.Modify, e-> notifyListeners(SWT.Modify, e));
		
		
		labels[1] = new Label(customSize, SWT.NONE);
		labels[1].setText("px"); //$NON-NLS-1$
		
		labels[2] = new Label(customSize, SWT.NONE);
		labels[2].setText(Messages.ResizeOptionComposite_heightLabel);
		
		txtHeight = new Text(customSize, SWT.BORDER);
		txtHeight.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtHeight.addListener(SWT.Modify, e-> notifyListeners(SWT.Modify, e));
		
		labels[3] = new Label(customSize, SWT.NONE);
		labels[3].setText("px"); //$NON-NLS-1$
		
		updateControls();
	}

	public void setEnabled(boolean enabled){
		if (!enabled){
			cmbSize.getControl().setEnabled(false);
			txtWidth.setEnabled(false);
			txtHeight.setEnabled(false);
			sizeLabel.setEnabled(false);
			infoLabel.setEnabled(false);
			for (Label l : labels) l.setEnabled(false);
		}else{
			cmbSize.getControl().setEnabled(true);
			sizeLabel.setEnabled(true);
			infoLabel.setEnabled(true);
			updateControls();
		}
	}
	
	private void updateControls(){
		Object x = ((IStructuredSelection)cmbSize.getSelection()).getFirstElement();
		boolean custom = x != null && x.equals(CyberTrackerPropertiesOption.ImageSizeOption.CUSTOM);
		txtWidth.setEnabled(custom);
		txtHeight.setEnabled(custom);
		for (Label l : labels) l.setEnabled(custom);
	}
	
	public ImageSizeOption getResizeOption(){
		ImageSizeOption selectedSizeOp = (ImageSizeOption) ((IStructuredSelection)cmbSize.getSelection()).getFirstElement();
		return selectedSizeOp;
	}
	
	public void setResizeOption(ImageSizeOption option){
		cmbSize.setSelection(new StructuredSelection(option));
	}
	
	public String getWidth(){
		return txtWidth.getText();
	}
	
	public String getHeight(){
		return txtHeight.getText();
	}
	
	public void setWidth(String width){
		txtWidth.setText(width);
	}
	
	public void setHeight(String height){
		txtHeight.setText(height);
	}
}
