package org.wcs.smart.asset;

import org.apache.log4j.Logger;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class AutoCompletionTextField {
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setLayout(new GridLayout(1, false));

      ComboViewer text = new ComboViewer(shell, SWT.DROP_DOWN);

      String[] proposals = new String[] {"abc", "bcd", "cde", "abc2", "bcd2", "cde2"};
      String autoActivationCharacters = "";
      SimpleContentProposalProvider simpleContentProposalProvider = new     SimpleContentProposalProvider(proposals);

      for (int i = 0; i < proposals.length; i++) {

         if (proposals[i].length() == 0)
            continue;

         char c = proposals[i].charAt(0);

         if (autoActivationCharacters.indexOf(c) == -1) {
            autoActivationCharacters += c;
         }
      }

      KeyStroke keyStroke = null;
      simpleContentProposalProvider.setFiltering(true);
      try {
         keyStroke = KeyStroke.getInstance("Ctrl+Space");
      } catch (ParseException e) {
         Logger.getLogger("").error("KeyStroke Parse Exception");
      }
      ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(text.getCombo(), new ComboContentAdapter(),
     simpleContentProposalProvider, keyStroke, autoActivationCharacters.toCharArray());

          proposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
      proposalAdapter.setLabelProvider(new ILabelProvider() {

         @Override
         public void removeListener(ILabelProviderListener listener) {}

         @Override
         public boolean isLabelProperty(Object element, String property) {
            return false;
         }

         @Override
         public void dispose() {}

         @Override
         public void addListener(ILabelProviderListener listener) {}

         @Override
         public String getText(Object element) {
            return ((ContentProposal)element).getLabel();
         }

         @Override
         public Image getImage(Object element) {
        	 return null;
         }
      });
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch()) {
            display.sleep();
         }
      }
      display.dispose();
   }
}