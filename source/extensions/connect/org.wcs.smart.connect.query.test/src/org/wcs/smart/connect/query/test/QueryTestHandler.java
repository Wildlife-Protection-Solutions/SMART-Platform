package org.wcs.smart.connect.query.test;

import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.util.UuidUtils;

public class QueryTestHandler extends AbstractHandler {

	private QueryTester qt;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		InputDialog id = new InputDialog(Display.getDefault().getActiveShell(),
				"Query UUID", "Query UUID TO Test, null for all", "", null);
		if (id.open() != InputDialog.OK){
			return null;
		}
		
		String uuid = id.getValue();
		if (uuid.trim().isEmpty()){
			uuid = null;
		}
		UUID uuid1 = null;
		try{
			uuid1 = UuidUtils.stringToUuid(uuid);
		}catch (Exception ex){
			ex.printStackTrace();
		}
		final UUID uuid2 = uuid1;
		
		final OutputDialog out = new OutputDialog(Display.getDefault().getActiveShell());
		
		Job j = new Job("testing queries"){
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				qt = new QueryTester(new QueryTester.IStatusRecorder() {
					@Override
					public void updateStatus(String newInfo) {
						out.appendText(newInfo);
						if (out.cancel){
							qt.cancel();
						}
					}
				});
				if (uuid2 == null){
					qt.testAllQueries();
				}else{
					qt.testQuery(uuid2);
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		out.open();
		
		
		return null;
	}

	
	class OutputDialog extends Dialog{

		private Text txt;
		private String alltext;
		public boolean cancel;
		
		protected OutputDialog(Shell parentShell) {
			super(parentShell);
			cancel = false;
			alltext = "";
		}
		
		protected void cancelPressed() {
			cancel = true;
			super.cancelPressed();
		}
		public void appendText(String text){
			alltext += "\n" + text;
			Display.getDefault().asyncExec(new Runnable(){

				@Override
				public void run() {
					if (txt != null && !txt.isDisposed()){
						txt.setText(alltext);
						txt.setSelection(txt.getText().length());
					}		
				}});
			
		}
		
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
			composite.setLayout(new GridLayout());
			txt = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
			txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)txt.getLayoutData()).widthHint = 300;
			((GridData)txt.getLayoutData()).heightHint = 300;
			
			return composite;
		}
		
		@Override
		public boolean isResizable(){
			return true;
		}
	}
}
