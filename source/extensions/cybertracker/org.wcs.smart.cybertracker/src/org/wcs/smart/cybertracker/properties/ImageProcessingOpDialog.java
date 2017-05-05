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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.ImageResizeOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.ImageSizeOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption.OptionID;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Properties dialog for collection image processing options.
 * 
 * @author Emily
 *
 */
public class ImageProcessingOpDialog extends TitleAreaDialog implements Listener{
	
	private Button btnDoResize;
	private Button btnPrompt;
	private Button btnAuto;
	private ResizeOptionComposite resizeComp;
	private Text txtMaxSize;
	private Label lblMaxSize;
	
	public ImageProcessingOpDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		// create OK and Cancel buttons by default
		Button btnSave = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CLOSE_LABEL, false);
		btnSave.setEnabled(false);
	}
		
	@Override
	public void handleEvent(Event event) {
		if (!btnDoResize.getSelection()){
			btnPrompt.setEnabled(false);
			btnAuto.setEnabled(false);
			resizeComp.setEnabled(false);
			txtMaxSize.setEnabled(false);
			lblMaxSize.setEnabled(false);
		}else{
			btnPrompt.setEnabled(true);
			btnAuto.setEnabled(true);
			txtMaxSize.setEnabled(true);
			lblMaxSize.setEnabled(true);
			if (btnPrompt.getSelection()){
				resizeComp.setEnabled(false);
			}else{
				resizeComp.setEnabled(true);
			}
		}
		modified(true);
	}
	
	public Control createDialogArea(Composite parent){
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnDoResize = new Button(main, SWT.CHECK);
		btnDoResize.setText(Messages.ImageProcessingOpDialog_doresizeoption);
		btnDoResize.addListener(SWT.Selection, this);
		
		int indent = 20;
		
		Composite size = new Composite(main, SWT.NONE);
		size.setLayout(new GridLayout(2, false));
		size.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)size.getLayout()).marginWidth = 0;
		((GridLayout)size.getLayout()).marginHeight = 0;
		((GridData)size.getLayoutData()).horizontalIndent = indent;
		lblMaxSize = new Label(size, SWT.NONE);
		lblMaxSize.setText(Messages.ImageProcessingOpDialog_maxsizeoption);
		lblMaxSize.setToolTipText(Messages.ImageProcessingOpDialog_maxsizetooltip);
		txtMaxSize = new Text(size, SWT.BORDER);
		txtMaxSize.setText("0"); //$NON-NLS-1$
		txtMaxSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtMaxSize.getLayoutData()).widthHint = 50;
		txtMaxSize.addListener(SWT.Modify, e->modified(true));
		
		btnPrompt = new Button(main, SWT.RADIO);
		btnPrompt.setText(Messages.ImageProcessingOpDialog_promptOption);
		btnPrompt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)btnPrompt.getLayoutData()).horizontalIndent = indent;
		btnPrompt.addListener(SWT.Selection, this);
		btnPrompt.setToolTipText(Messages.ImageProcessingOpDialog_promptTooltip);
		
		btnAuto = new Button(main, SWT.RADIO);
		btnAuto.setText(Messages.ImageProcessingOpDialog_autoOption);
		btnAuto.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAuto.setToolTipText(Messages.ImageProcessingOpDialog_autoTooltip);
		((GridData)btnAuto.getLayoutData()).horizontalIndent = indent;
		btnAuto.addListener(SWT.Selection, this);
		btnAuto.setSelection(true);
		
		resizeComp = new ResizeOptionComposite(main);
		resizeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		resizeComp.addListener(SWT.Modify, e->modified(true));
		
		loadSettings();
		
		setTitle(Messages.ImageProcessingOpDialog_Title);
		setMessage(Messages.ImageProcessingOpDialog_Message);
		getShell().setText(Messages.ImageProcessingOpDialog_Title);
		return parent;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

	private void modified(boolean isModified){
		Button btn = getButton(IDialogConstants.OK_ID);
		String error = validate();
		setErrorMessage(error);
		
		if (error != null){
			btn.setEnabled(false);
		}else{
			if (btn != null) btn.setEnabled(isModified);
		}
	}

	private String validate(){
		if (btnDoResize.getSelection()){	
			try{
				double maxsize = Double.parseDouble(txtMaxSize.getText());
				if (maxsize < 0) throw new Exception();
			}catch (Exception ex){
				return Messages.ImageProcessingOpDialog_InvalidMaxSize; 
			}
			if (btnAuto.getSelection()){
				ImageSizeOption selectedSizeOp = resizeComp.getResizeOption();
				if (selectedSizeOp == null){
					return Messages.ImageProcessingOpDialog_SizeRequired;
				}
				if (selectedSizeOp.equals(ImageSizeOption.CUSTOM)){
					try{
						int width = Integer.parseInt(resizeComp.getWidth());
						if (width <= 0) throw new Exception();
					}catch (Exception ex){
						return Messages.ImageProcessingOpDialog_InvalidWidth;
					}
					try{
						int height = Integer.parseInt(resizeComp.getHeight());
						if (height <= 0) throw new Exception();
					}catch (Exception ex){
						return Messages.ImageProcessingOpDialog_InvalidHeight;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public void okPressed(){
		//valid
		String error = validate();
		if (error != null){
			MessageDialog.openError(getShell(), Messages.ImageProcessingOpDialog_ErrorTitle, error);
			return;
		}
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			CyberTrackerPropertiesOption resizeOp = (CyberTrackerPropertiesOption)s.createCriteria(CyberTrackerPropertiesOption.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("optionId", OptionID.RESIZE_IMAGE)) //$NON-NLS-1$
				.uniqueResult();
			CyberTrackerPropertiesOption sizeOp = (CyberTrackerPropertiesOption)s.createCriteria(CyberTrackerPropertiesOption.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.add(Restrictions.eq("optionId", OptionID.IMAGE_SIZE)) //$NON-NLS-1$
					.uniqueResult();
			
			CyberTrackerPropertiesOption maxSize = (CyberTrackerPropertiesOption)s.createCriteria(CyberTrackerPropertiesOption.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.add(Restrictions.eq("optionId", OptionID.MAX_IMAGE_SIZE)) //$NON-NLS-1$
					.uniqueResult();
			
			if (resizeOp == null){
				resizeOp = new CyberTrackerPropertiesOption();
				resizeOp.setOptionId(OptionID.RESIZE_IMAGE);
				resizeOp.setConservationArea(SmartDB.getCurrentConservationArea());
			}
			if (!btnDoResize.getSelection()){
				resizeOp.setStringValue(CyberTrackerPropertiesOption.ImageResizeOption.NONE.name());
				if (sizeOp != null){
					s.delete(sizeOp);
				}
			}else{
				if (maxSize == null){
					maxSize = new CyberTrackerPropertiesOption();
					maxSize.setOptionId(OptionID.MAX_IMAGE_SIZE);
					maxSize.setConservationArea(SmartDB.getCurrentConservationArea());
				}
				maxSize.setDoubleValue(Double.parseDouble(txtMaxSize.getText()));
				s.saveOrUpdate(maxSize);
				
				if (btnPrompt.getSelection()){
					resizeOp.setStringValue(ImageResizeOption.MANUAL.name());
					if (sizeOp != null) s.delete(sizeOp);
				}else{
					resizeOp.setStringValue(ImageResizeOption.AUTO.name());
					if (sizeOp == null){
						sizeOp = new CyberTrackerPropertiesOption();
						sizeOp.setOptionId(OptionID.IMAGE_SIZE);
						sizeOp.setConservationArea(SmartDB.getCurrentConservationArea());
					}
					ImageSizeOption selectedSizeOp = resizeComp.getResizeOption();
					if(selectedSizeOp.equals(ImageSizeOption.CUSTOM)){
						sizeOp.setStringValue(selectedSizeOp.name() + CyberTrackerPropertiesOption.PROP_SEP + resizeComp.getWidth() + CyberTrackerPropertiesOption.PROP_SEP + resizeComp.getHeight());
					}else{
						sizeOp.setStringValue(selectedSizeOp.name());
					}
					s.saveOrUpdate(sizeOp);
				}
			}
			s.saveOrUpdate(resizeOp);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			CyberTrackerPlugIn.displayError(Messages.ImageProcessingOpDialog_ErrorTitle, Messages.ImageProcessingOpDialog_SaveError + ex.getMessage(), ex);
		}finally{
			s.close();
		}
		modified(false);
	}
	
	
	private void loadSettings(){
		btnDoResize.setSelection(false);
		handleEvent(null);
		btnDoResize.setEnabled(false);
		
		Job j = new Job("loading settings"){ //$NON-NLS-1$

			@SuppressWarnings("unchecked")
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<CyberTrackerPropertiesOption> options = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					options.addAll(s.createCriteria(CyberTrackerPropertiesOption.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.add(Restrictions.in("optionId", new OptionID[]{OptionID.RESIZE_IMAGE, OptionID.IMAGE_SIZE, OptionID.MAX_IMAGE_SIZE})) //$NON-NLS-1$
					.list());
					
				}finally{
					s.close();
				}

				Display.getDefault().syncExec(()->{
					
					for (CyberTrackerPropertiesOption o : options){
						if (o.getOptionId().equals(OptionID.RESIZE_IMAGE)){
							String value = o.getStringValue();
							for (CyberTrackerPropertiesOption.ImageResizeOption r : ImageResizeOption.values()){
								if (r.name().equalsIgnoreCase(value)){
									if (r.equals(ImageResizeOption.AUTO) ){
										btnPrompt.setSelection(false);
										btnAuto.setSelection(true);
										btnDoResize.setSelection(true);
									}else if (r.equals(ImageResizeOption.MANUAL)){
										btnPrompt.setSelection(true);
										btnAuto.setSelection(false);
										btnDoResize.setSelection(true);
									}else if (r.equals(ImageResizeOption.NONE)){
										btnPrompt.setSelection(false);
										btnAuto.setSelection(true);
										btnDoResize.setSelection(false);
									}
									break;
								}
							}
						}else if (o.getOptionId().equals(OptionID.MAX_IMAGE_SIZE)){
							txtMaxSize.setText(String.valueOf(o.getDoubleValue()));
						}
					}
					btnDoResize.setEnabled(true);
					handleEvent(null);
					
					if (btnAuto.isEnabled() && btnAuto.getSelection()){
						//find size
						for (CyberTrackerPropertiesOption o : options){
							if (o.getOptionId().equals(OptionID.IMAGE_SIZE)){
								if (o.getStringValue().startsWith(ImageSizeOption.CUSTOM.name())){
									//custom
									resizeComp.setResizeOption(ImageSizeOption.CUSTOM);
									String[] parts = o.getStringValue().split(CyberTrackerPropertiesOption.PROP_SEP);
									try{
										resizeComp.setWidth(String.valueOf(Integer.parseInt(parts[1])));
									}catch (Exception ex){
										
									}
									try{
										resizeComp.setHeight(String.valueOf(Integer.parseInt(parts[2])));
									}catch (Exception ex){
										
									}
									
								}else{
									for (ImageSizeOption so : ImageSizeOption.values()){
										if (o.getStringValue().equalsIgnoreCase(so.name())){
											resizeComp.setResizeOption(so);
											break;
										}
									}
								}
								break;
							}
						}
					}
					
					
					modified(false);
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}

}
