package org.wcs.smart.plan.ui.tree;

import net.refractions.udig.project.ui.internal.ISharedImages;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

public class FakeLabelProvider extends LabelProvider {
	  @Override
	  public String getText(Object element) {
	    if (element instanceof FakeCategory) {
	      FakeCategory category = (FakeCategory) element;
	      return category.getName();
	    }
	    return ((FakeItem) element).getSummary();
	  }

	  @Override
	  public Image getImage(Object element) {
		  return null;
	} 
}