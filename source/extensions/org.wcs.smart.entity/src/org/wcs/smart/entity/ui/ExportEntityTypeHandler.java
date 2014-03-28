package org.wcs.smart.entity.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import net.refractions.udig.catalog.URLUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.editor.EntityTypeEditorInput;
import org.wcs.smart.entity.xml.EntityTypeToXmlConverter;
import org.wcs.smart.entity.xml.EntityTypeXmlManager;
import org.wcs.smart.hibernate.HibernateManager;

public class ExportEntityTypeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		
		IEditorInput input = HandlerUtil.getActiveEditor(event).getEditorInput();
		if (input == null || !(input instanceof EntityTypeEditorInput)){
			return null;
		}	
		
		Session s = HibernateManager.openSession();
		try{
			final EntityType et = (EntityType) s.load(EntityType.class, ((EntityTypeEditorInput)input).getUuid());
			ExportEntityTypeDialog dialog = new ExportEntityTypeDialog(HandlerUtil.getActiveShell(event), et);
			if (dialog.open() != Window.OK){
				return null;
			}
			
			final File exportFile = dialog.getFile();
			
			final ProgressMonitorDialog pmd = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));
			try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(MessageFormat.format(Messages.ExportEntityTypeHandler_ExportProgress, new Object[]{et.getName()}), 100);
					try{
						FileOutputStream fout = new FileOutputStream(exportFile);
						try{
							EntityTypeXmlManager.writeDataModel(EntityTypeToXmlConverter.toXml(et, new SubProgressMonitor(monitor, 100)),
								fout);
							monitor.worked(100);
						}finally{
							fout.close();
						}
						pmd.getShell().getDisplay().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openInformation(pmd.getShell(), Messages.ExportEntityTypeHandler_DialogTitle, 
										MessageFormat.format(Messages.ExportEntityTypeHandler_ExportComplete, new Object[]{exportFile.getAbsolutePath()}));		
							}});
						
					}catch (Exception ex){
						EntityPlugIn.displayLog(Messages.ExportEntityTypeHandler_ExportError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
					}
					monitor.done();
				}
			});
			}catch (Exception ex){
				EntityPlugIn.log(ex.getMessage(), ex);
			}
		}finally{
			s.close();
		}
		
		
		
		return null;
	}
	
	class ExportEntityTypeDialog extends TitleAreaDialog{

		private static final String LAST_DIR_KEY = "LAST_EXPORT_DIR"; //$NON-NLS-1$
		
		private Text txtFile;
		private EntityType et;
		private File file;
		
		public ExportEntityTypeDialog(Shell parentShell, EntityType et) {
			super(parentShell);
			this.et = et;
		}
		
		public File getFile(){
			return this.file;
		}
		
		protected void okPressed() {
			file = new File(txtFile.getText());
//			if (!file.isFile()){
//				MessageDialog.openError(getShell(), "Error", "Not a valid file.");
//				return;
//			}
			if (!file.getParentFile().exists()){
				if (!MessageDialog.openConfirm(getShell(), Messages.ExportEntityTypeHandler_WarningDialogTitle, 
						MessageFormat.format(Messages.ExportEntityTypeHandler_DirectoryNotExistant, new Object[]{file.getParentFile().toString()}))){
					return;
				}
			}
			if (file.exists()){
				if (!MessageDialog.openConfirm(getShell(), Messages.ExportEntityTypeHandler_WarningDialogTitle, 
						MessageFormat.format(Messages.ExportEntityTypeHandler_FileExists, new Object[]{file.toString()}))){
					return;
				}
			}
			EntityPlugIn.getDefault().getDialogSettings().put(LAST_DIR_KEY, file.getParent().toString());
			
			super.okPressed();
		}
		
		protected Control createDialogArea(Composite parent) {
			
			setTitle(Messages.ExportEntityTypeHandler_DialogTitleA);
			setMessage(MessageFormat.format(Messages.ExportEntityTypeHandler_DialogMessage, et.getName()));
			getShell().setText(Messages.ExportEntityTypeHandler_DialogTitleB);
			
			Composite p = (Composite) super.createDialogArea(parent);
			
			Composite contents = new Composite(p, SWT.NONE);
			contents.setLayout(new GridLayout(3, false));
			contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Label l = new Label(contents, SWT.NONE);
			l.setText(Messages.ExportEntityTypeHandler_FileLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			txtFile = new Text(contents, SWT.BORDER);
			txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			String initFile = URLUtils.cleanFilename(et.getName()) + ".xml"; //$NON-NLS-1$
			String location = EntityPlugIn.getDefault().getDialogSettings().get(LAST_DIR_KEY);
			if (location == null){
				location = System.getProperty("user.home"); //$NON-NLS-1$
			}
			File init = new File(location, initFile);

			txtFile.setText(init.getAbsolutePath());
			txtFile.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					getButton(IDialogConstants.OK_ID).setEnabled(txtFile.getText().length() > 0);
				}
			});
			
			Button btnBrowse = new Button(contents, SWT.NONE);
			btnBrowse.setText(Messages.ExportEntityTypeHandler_BrowseLabel);
			btnBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
					
					String ext = "xml"; //$NON-NLS-1$
					String name= Messages.ExportEntityTypeHandler_XmlLabel;
					
					String[] extensions = new String[]{"*." + ext, "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
					String[] names = new String[]{name + " (*." + ext + ")", Messages.ExportEntityTypeHandler_AllFiles}; //$NON-NLS-1$ //$NON-NLS-2$
					
					fd.setFilterExtensions(extensions);
					fd.setFilterNames(names);
					
					fd.setFilterPath(txtFile.getText());
					fd.setFileName(txtFile.getText());
					
					String f = fd.open();
					if (f != null) {
						txtFile.setText(f);
					}
				}
			});
			
			
			return p;
		}
		
	}
}