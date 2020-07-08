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
package org.wcs.smart.dataentry.dialog.composite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.icon.ui.ImageSelectionDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Control that allows to select a single image for provided object.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ImageSelectionControl extends Composite {
	
	private enum Type {
		DATAMODEL(Messages.ImageSelectionControl_DefaultIcon), 
		CUSTOM(Messages.ImageSelectionControl_CustomIcon);
		public String guiName;
		
		Type(String type){
			this.guiName = type;
		}
	};
	
	private IImageContentProvider contentProvider;
	private Canvas canvas;
	private Label lblWarnText;
	private Composite warningArea;
	
	private boolean fireEvents = true;
	private Type lastSelection = null;
	private ComboViewer cmbType;
	
	public ImageSelectionControl(Composite parent, IImageContentProvider contentProvider) {
		super(parent, SWT.NONE);
		this.contentProvider = contentProvider;
		initControls();
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		super.setEnabled(isEnabled);
		
		List<Control> kids = new ArrayList<>();
		for (Control c : this.getChildren()) kids.add(c);
		
		while(kids.size() > 0) {
			Control c = kids.remove(0);
			if (c instanceof Button) {
				if (!isEnabled) {
					c.setEnabled(false);
				}else {
					//only enable
					c.setEnabled(contentProvider.isCustom());
				}
				
				continue;
			}
			c.setEnabled(isEnabled);
			if (c instanceof Composite) {
				for (Control kk : ((Composite)c).getChildren()) {
					kids.add(kk);
				}
			}
		}
	}
	
	public void updateImage() {
		canvas.redraw();
		warningArea.setVisible(false);
		lblWarnText.setText(""); //$NON-NLS-1$

		if (!contentProvider.hasDataModel()) {
			lastSelection = Type.CUSTOM;
			cmbType.setSelection(new StructuredSelection(Type.CUSTOM));
			cmbType.getControl().setEnabled(false);
		}else {
			cmbType.getControl().setEnabled(true);
			if (contentProvider.isCustom()) {
				lastSelection = Type.CUSTOM;
				cmbType.setSelection(new StructuredSelection(Type.CUSTOM));
			}else {
				lastSelection = Type.DATAMODEL;
				cmbType.setSelection(new StructuredSelection(Type.DATAMODEL));
				
				//if model has no icon set warn the user
				if (contentProvider.getModel().getIconSet() == null) {
					lblWarnText.setText(Messages.ImageSelectionControl_noiconsetmsg);
					lblWarnText.setToolTipText(Messages.ImageSelectionControl_noiconsettooltip);
					warningArea.setVisible(true);
				}
			}
		}
		
		Path file = contentProvider.getImageFile();
		if (file != null && Files.exists(file) && !Files.isDirectory(file)) {
			
			if (file.getFileName().toString().endsWith(".svg") || file.getFileName().toString().endsWith(".png")) { //$NON-NLS-1$ //$NON-NLS-2$
				lblWarnText.setText(Messages.ImageSelectionControl_BetaCtFormat);
				warningArea.setVisible(true);
			}
		}
		warningArea.getParent().getParent().layout(true);
		
		canvas.setToolTipText(file == null ? "" : file.getFileName().toString()); //$NON-NLS-1$
	}
	
	private void initControls() {
		GridLayout gd = new GridLayout(3, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);

		
		cmbType = new ComboViewer(this, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((Type)element).guiName;
			}
		});
		cmbType.setInput(Type.values());
		
		int size = 64;
		canvas = new Canvas(this, SWT.BORDER);
		GridData canvasLayoutData = new GridData(size, size);
		canvasLayoutData.verticalAlignment = SWT.TOP;
		
		canvas.setLayoutData(canvasLayoutData);
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				
				Path file = contentProvider.getImageFile();
				if (file != null && Files.exists(file) && !Files.isDirectory(file)) {
					
					Image image = SmartUtils.getImage(file, size);
					try {
						e.gc.drawImage(image, 0, 0, size, size, 0, 0, size, size);
					}finally {
						image.dispose();
					}
				}
			}
		});
		
		Composite btnCmp = new Composite(this, SWT.NONE);
		GridLayout bgd = new GridLayout(1, false);
		bgd.marginBottom=0;
		bgd.marginHeight = 0;
		bgd.marginLeft = 0;
		bgd.marginRight = 0;
		bgd.marginTop = 0;
		bgd.marginWidth = 0;
		btnCmp.setLayout(bgd);
		btnCmp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		
		Button buttonLoad = new Button(btnCmp, SWT.PUSH);
		buttonLoad.setBackground(btnCmp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		buttonLoad.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		buttonLoad.setText(Messages.ImageSelectionControl_Button_Load);
		buttonLoad.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String imageFile = selectImage();
				if (imageFile == null) return;
				contentProvider.setImageFile(Paths.get(imageFile));
				updateImage();
			}
		});

		Button buttonClear = new Button(btnCmp, SWT.PUSH);
		buttonClear.setBackground(btnCmp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		buttonClear.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		buttonClear.setText(Messages.ImageSelectionControl_ResetIconBtn);
		buttonClear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				contentProvider.setImageFile(IImageAssociatedObject.NULL_FILE); //we cannot use null, as null indicates that we need to load a value
				updateImage();
			}
		});
		
		if (contentProvider.isCustom()) {
			cmbType.setSelection(new StructuredSelection(Type.CUSTOM));
			buttonLoad.setEnabled(true);
			buttonClear.setEnabled(true);
		}else {
			cmbType.setSelection(new StructuredSelection(Type.DATAMODEL));
			buttonLoad.setEnabled(false);
			buttonClear.setEnabled(false);
		}
		buttonLoad.setEnabled(false);
		
		cmbType.addSelectionChangedListener(e->{
			if (!fireEvents) return;
			try {
				fireEvents = false;
				Type type = (Type) cmbType.getStructuredSelection().getFirstElement();
				if (type == Type.DATAMODEL) {
					contentProvider.setImageFile(null);
					updateImage();
				}else if (type == Type.CUSTOM && lastSelection != Type.CUSTOM) {
					String imageFile = selectImage();
					if (imageFile == null) return;
					contentProvider.setImageFile(Paths.get(imageFile));
					updateImage();
				}
				lastSelection = type;
				buttonLoad.setEnabled(type == Type.CUSTOM);
				buttonClear.setEnabled(type == Type.CUSTOM);
			}finally {
				fireEvents = true;
			}
			
		});
		
		
		
		warningArea = new Composite(this, SWT.NONE);
		warningArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		warningArea.setLayout(new GridLayout(2, false));
		
		Label lblWarnImg = new Label(warningArea, SWT.NONE);
		lblWarnImg.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		lblWarnImg.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		lblWarnText = new Label(warningArea, SWT.WRAP);
		lblWarnText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblWarnText.getLayoutData()).widthHint = size;
		lblWarnText.setText(""); //$NON-NLS-1$
		
		warningArea.setVisible(false);
	}
	

	private String selectImage() {
		ImageSelectionDialog dialog = new ImageSelectionDialog(getShell());
		if (dialog.open() != Window.OK) return null;
		String imageFile = dialog.getImageFile();
		return imageFile;
	}
	
	
	/**
	 * Interface should be implemented by any object that want to use this control.
	 * It provides data to display and is called when new image was selected.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	public static interface IImageContentProvider {
		
		/**
		 * If the node references a data model node
		 */
		public boolean hasDataModel();
		
		/**
		 * If the image is a custom image or the data model image
		 */
		public boolean isCustom();
		
		/**
		 * @return an image that is supposed to be displayed.
		 */
		public Path getImageFile();
		
		/**
		 * Called when new image was selected.
		 * @param file
		 */
		public void setImageFile(Path file);
		
		/**
		 * The configurable model being modified
		 * 
		 * @return
		 */
		public ConfigurableModel getModel();
	}
}
