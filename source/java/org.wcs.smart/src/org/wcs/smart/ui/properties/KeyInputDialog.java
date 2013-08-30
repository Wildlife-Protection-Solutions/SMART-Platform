package org.wcs.smart.ui.properties;

import java.util.Collection;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.internal.Messages;

public class KeyInputDialog extends InputDialog{

	public KeyInputDialog(Shell parentShell, final String initialValue, final Collection<? extends NamedKeyItem> siblings) {

		super(	parentShell,
		 Messages.NameKeyComposite_ChangeKey_Dialog_Title,
			Messages.NameKeyComposite_ChangeKey_Dialog_Message,
			initialValue, new IInputValidator() {

				@Override
				public String isValid(String newText) {
					if (initialValue != null && initialValue.equals(newText)){
						//same key
						return ""; //$NON-NLS-1$
					}
					String error = NamedKeyItem.validateKey(newText, siblings);
					return error;

				}
			});
			

	}
	
	@Override
	public int open(){
		MessageDialog.openInformation(getShell(), "Edit Key", "Modifying keys will affect queries and reports used in cross conservation area analysis.  Some of your queries/reports may become invalid.");
		return super.open();
	}

}
