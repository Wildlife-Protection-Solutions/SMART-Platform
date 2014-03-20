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
package org.wcs.smart.entity.query.ui.itempanel;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.internal.Messages;

/**
 * Entity Type filter tree item content provider 
 * 
 * @author Emily
 *
 */
public class EntityTypeFilterContentProvider implements ITreeContentProvider{

	private List<EntityType> types = null;
	private Viewer viewer = null;
	
	@Override
	public void dispose() {
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		this.types = null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (types == null){
			loadTypes();
			return new String[]{Messages.EntityTypeFilterContentProvider_LoadingLabel};
		}
		return types.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof EntityType){
			Object[] x = new Object[((EntityType) parentElement).getAttributes().size() + 1];
			x[0] = ((EntityType) parentElement).getDmAttribute();
			for (int i = 0; i < ((EntityType) parentElement).getAttributes().size() ; i ++){
				x[i+1]=((EntityType) parentElement).getAttributes().get(i);
			}
			return x;
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof EntityAttribute){
			return ((EntityAttribute) element).getEntityType();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof EntityType){
			return true;
		}
		return false;
	}
	
	private void loadTypes(){
		Job j = new Job(Messages.EntityTypeFilterContentProvider_LoadJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				types = EntityHibernateManager.getActiveEntityTypes();
				
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						//TODO: figure out how to only refresh this node which is a wrapped object
						//viewer.refresh(at);
						viewer.refresh();
					}
				});
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}
	
	public static final LabelProvider lblProvider = new LabelProvider(){
		@Override
		public String getText(Object element){
			if (element instanceof EntityType){
				return ((EntityType) element).getName();
			}else if (element instanceof EntityAttribute){
				return ((EntityAttribute) element).getName();
			}else if (element instanceof Attribute){
				return ((Attribute) element).getName();
			}
			return super.getText(element);
		}
		
		@Override
		public Image getImage(Object element){
			if (element instanceof EntityType){
				return EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.ENTITY_TYPE_ICON);
			}else if (element instanceof EntityAttribute){
				return ((EntityAttribute) element).getDmAttribute().getType().getImage();
			}else if (element instanceof Attribute){
				return ((Attribute) element).getType().getImage();
			}
			return super.getImage(element);
		}
		
	};

}


