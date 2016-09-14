package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.ui.Thumbnail;

public class AttachmentTable extends Composite implements Listener {

	private ThumbnailComposite thumb;
	private IntelEntity entity;
	private FormToolkit toolkit;
	
	private ScrolledForm infoSection;
	
	public AttachmentTable(Composite parent, FormToolkit toolkit){
		super(parent, SWT.BORDER);
		this.toolkit = toolkit;
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		infoSection = toolkit.createScrolledForm(this);
		infoSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		infoSection.addListener(SWT.Resize, this);

		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = gl.marginHeight = 0;
		gl.horizontalSpacing = 0;
		infoSection.getBody().setLayout(gl);
		
		addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (thumb != null){
					for (org.wcs.smart.i2.ui.editors.AttachmentTable.ThumbnailComposite.ThumbInfo t : thumb.thumbs){
						if (t.thumb != null) t.thumb.dispose();
					}
				}
			}
		});
	}
	
	@Override
	public void handleEvent(Event event) {
		if (event == null || event.type == SWT.Resize){
			if (thumb != null){
				int mainWidth = infoSection.getClientArea().width - infoSection.getVerticalBar().getSize().x;
				int width = infoSection.getSize().x - infoSection.getVerticalBar().getSize().x;	
				int cols = (int)Math.floor(width / 100.0);
				if (cols < 1) cols = 1;
				thumb.updateLayout(cols);
				((GridData)thumb.getLayoutData()).widthHint = mainWidth;
				thumb.layout(true);
				infoSection.getBody().layout(true);
				infoSection.reflow(true);
			}
		}
	
	}
	
	public void setEntity(IntelEntity entity){
		this.entity = entity;
		refresh();
	}
	
	public void refresh(){
		redraw.schedule();
	}
	
	
	private Job redraw = new Job("redraw thumbnails"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (thumb == null){
						thumb = new ThumbnailComposite(AttachmentTable.this.infoSection.getBody());
						infoSection.setContent(thumb);
						toolkit.adapt(thumb);
					}else{
						for (Control c : thumb.getChildren()){
							c.dispose();
						}
					}
				}
				
			});
			
			if (entity != null && entity.getEntityAttachments() != null && !entity.getEntityAttachments().isEmpty()){
				List<ISmartAttachment> thumbs = new ArrayList<ISmartAttachment>();
				for (IntelEntityAttachment a : entity.getEntityAttachments()){
					thumbs.add(a.getAttachment());
				}
			
				thumb.setFiles(thumbs);
				thumb.initThumbs();
				
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						thumb.createThumbs();
						handleEvent(null);
					}
					
				});
			}
			return Status.OK_STATUS;
		}
		
	};
	
	/*
	 * A composite for thumbnails
	 */
	private class ThumbnailComposite extends Composite{
		private List<ThumbInfo> thumbs;
		
		public ThumbnailComposite(Composite parent){
			super(parent, SWT.NONE);
			GridLayout gl = new GridLayout();
			gl.marginWidth = gl.marginHeight = 0;
			setLayout(gl);
			
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
		
		public void updateLayout(int numCols){
			GridLayout gl = new GridLayout(numCols, false);
			gl.marginWidth = gl.marginHeight = 0;
			setLayout(gl);
		}
		
		public void setFiles(List<ISmartAttachment> fileNames){
			if (thumbs == null){
				thumbs = new ArrayList<AttachmentTable.ThumbnailComposite.ThumbInfo>();
				for (ISmartAttachment a : fileNames){
					thumbs.add(new ThumbInfo(a));
				}
			}else{
				//let's merge file
				List<ThumbInfo> old = thumbs;
				thumbs = new ArrayList<AttachmentTable.ThumbnailComposite.ThumbInfo>();
				
				for (ISmartAttachment file : fileNames){
					ThumbInfo found = null;
					for (ThumbInfo o : old){
						if (o.file.equals(file)){
							found = o;
							break;
						}
					}
					if (found != null){
						old.remove(found);
						thumbs.add(found);
					}else{
						thumbs.add(new ThumbInfo(file));
					}
				}
				for (ThumbInfo i : old){
					if (i.thumb != null) i.thumb.dispose();
				}
			}
		}
		
		public void initThumbs(){
			if (thumbs == null) return;
			for (ThumbInfo info : thumbs){
				info.createThumb();
			}
		}
		
		public void createThumbs(){
			if (thumbs == null) return;
			for (ThumbInfo t : thumbs){
				Composite parent = toolkit.createComposite(this);
				t.thumb.createThumbnail(parent);
			}
		}
		
		private class ThumbInfo{
			ISmartAttachment file;
			Thumbnail thumb;
			
			public ThumbInfo(ISmartAttachment file){
				this.file = file;
			}
			
			public void createThumb(){
				if (thumb == null){
					thumb = new Thumbnail(file, false);
				}
			}
		}
	}
}
