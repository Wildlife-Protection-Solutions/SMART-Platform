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


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;

public class ShowCaPropertyPage extends AbstractHandler {


	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if (SmartDB.getCurrentConservationArea() != null &&
				SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.ADMIN){
			
			
			//TODO: check this works on logout
			final StructuredSelection selection = new StructuredSelection(SmartDB.getCurrentConservationArea());
			ISelectionProvider provider = new ISelectionProvider() {
				
				@Override
				public void setSelection(ISelection selection) {
				}
				
				@Override
				public void removeSelectionChangedListener(
						ISelectionChangedListener listener) {					
				}
				
				@Override
				public ISelection getSelection() {
					return selection;
				}
				
				@Override
				public void addSelectionChangedListener(ISelectionChangedListener listener) {}
			};
			
			PropertyDialogAction action = new PropertyDialogAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), provider);
			action.run();
		}else{
			//TODO
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Error", "You do not have permissions to modify the conservation area.");
		}
		return null;
	}

}
