/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.util.SmartUtils;

import javafx.application.Platform;
import javafx.embed.swt.FXCanvas;
//import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;

/**
 * Image gallery similar to AttachmentTable that uses Javafx to display
 * images.  Seems to be much faster and less resource intensive 
 * for large number of images. 
 * 
 * @author Emily
 *
 */
public class ImageGallery extends Composite{

    
	private List<? extends ISmartAttachment> attachments;
	private FormToolkit toolkit;
	
	private List<ThumbInfo> thumbs;
	
	private ContextMenu thumbMenu;
	
	private String mouseOverBackgroundColor = null;
	private String mouseOverBorderColor = null;
	
	private String backgroundColor = null;
	private String borderColor = null;
	
	private String selectionBackgroundColor = null;
	private String selectionBorderColor = null;
	
	private int thumbSize;
	private int marginSize;
	
	private FXCanvas fxCanvas;
	private TilePane tile ;
	
	private AtomicBoolean needsRefresh = new AtomicBoolean(false);
	private AtomicBoolean isLoading = new AtomicBoolean(false);
	
	private Job loadImages = new Job(Messages.ImageGallery_jobname) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			isLoading.set(true);

			thumbs = new ArrayList<>();
			boolean[] ok = new boolean[] { true };
			List<ISmartAttachment> local = new ArrayList<>(attachments);
		
			List<Node> toAdd = new ArrayList<>();
			for (final ISmartAttachment attachment : local) {
				int index = thumbs.size();
				ThumbInfo i = new ThumbInfo(attachment, index);
				thumbs.add(i);
				toAdd.add(i.imageNode);
				
				//add to ui in batches of 20
				if (toAdd.size() == 20) {
					final List<Node> adding = new ArrayList<>(toAdd);
					toAdd.clear();
					Platform.runLater(() -> {
						if (fxCanvas.isDisposed()) ok[0] = false;
						for (Node n : adding) {
							tile.getChildren().add(n);
						}
						tile.layout();
						
					});
				}
			}
			
			final List<Node> adding = new ArrayList<>(toAdd);
			toAdd.clear();
			Platform.runLater(() -> {
				try {
					if (fxCanvas.isDisposed()) ok[0] = false;
					for (Node n : adding) {
						tile.getChildren().add(n);
					}
					tile.layout();
				}finally {
					//this assumes this platform runs things in order
					isLoading.set(false);
				}
				
			});

			if (needsRefresh.get()) {
				needsRefresh.set(false);
				Display.getDefault().syncExec(() -> refresh());
			}
			return Status.OK_STATUS;
		}
	};
	public ImageGallery(Composite parent, FormToolkit toolkit, IMenuCreator thumbsMenu, List<? extends ISmartAttachment> files, int thumbSize){
		this(parent, toolkit, thumbsMenu, SWT.NONE, files, thumbSize,0);
	}
	
	public ImageGallery(Composite parent, FormToolkit toolkit, IMenuCreator thumbsMenu, List<? extends ISmartAttachment> files, int thumbSize, int marginSize){
		this(parent, toolkit, thumbsMenu, SWT.NONE, files, thumbSize, marginSize);
	}
	
	public ImageGallery(Composite parent, FormToolkit toolkit, IMenuCreator thumbsMenu, int style, List<? extends ISmartAttachment> files, int thumbSize, int marginSize){
		super(parent, SWT.NONE);
				
		this.thumbSize = thumbSize;
		this.toolkit = toolkit;
		this.marginSize = marginSize;
		toolkit.adapt(this);
		
		setLayout(new FillLayout());
		((FillLayout)getLayout()).marginWidth = 0;
		((FillLayout)getLayout()).marginHeight = 0;
		
		org.eclipse.swt.graphics.Color color = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		
		borderColor = toHex(toolkit.getColors().getBorderColor());
		backgroundColor = toHex(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		mouseOverBorderColor = toHex(SmartUtils.blend(new RGB(0, 0, 0), color.getRGB(), 40));
		mouseOverBackgroundColor = toHex(SmartUtils.blend(new RGB(255, 255, 255), color.getRGB(), 90));
		
		selectionBorderColor = toHex(color);
		selectionBackgroundColor = toHex(SmartUtils.blend(new RGB(255, 255, 255), color.getRGB(), 75));
		
		//ensure we manually dispose of images
		//as we reuse them if we can
		
		this.attachments = files;
		
		createThumbnails();
		
		if (thumbsMenu != null) thumbMenu = thumbsMenu.createMenu();
		
		addListener(SWT.MouseUp, e->{
			clearSelection();
			fireSelectionEvents(e);
		});
	}

	protected String toHex(RGB rgb) {
		int code = ((255 & 0xFF) << 24) | 
				((rgb.red & 0xFF) << 16) | 
				((rgb.green & 0xFF) << 8) |
				((rgb.blue & 0xFF) << 0);

		String hex = Integer.toHexString(code & 0xffffff);
		while (hex.length() < 6) {
			hex = "0" + hex; //$NON-NLS-1$
		}
		hex = "#" + hex; //$NON-NLS-1$
		return hex;
	}

	protected String toHex(org.eclipse.swt.graphics.Color c) {
		return toHex(c.getRGB());
	}
	
	public void setThumbnailSize(int newSize) {
		this.thumbSize = newSize;
		for (Control c : getChildren()) c.dispose();
		createThumbnails();
		layout(true);
	}
	
	public StructuredSelection getSelection(){
		List<ISmartAttachment> sel = new ArrayList<ISmartAttachment>();
		for (ThumbInfo t : thumbs){
			if (t.isSelected) sel.add((ISmartAttachment) t.file);
		}
		return new StructuredSelection(sel);
	}
	
	public void refresh() {
		if (isLoading.get()) {
			//still loading; we'll refresh once we've finished loading
			needsRefresh.set(true);
			return;
		}		
		ArrayList<ThumbInfo> old = new ArrayList<ThumbInfo>();
		if (thumbs != null) old.addAll(thumbs);
		thumbs = new ArrayList<ThumbInfo>();
		
		List<Node> orderedNodes = new ArrayList<>();
		int index = 0;
		for (ISmartAttachment a : attachments) {
			ThumbInfo found = null;
			for (ThumbInfo o : old) {
				if (o.file.equals(a)) {
					found = o;
				}
			}
			if (found != null) {
				thumbs.add(found);
				old.remove(found);
			}else {
				found = new ThumbInfo(a, thumbs.size());
				thumbs.add(found);
			}
			found.index = index++;
			found.updateStyle();
			orderedNodes.add(found.imageNode);	
		}
		tile.getChildren().clear();
		tile.getChildren().addAll(orderedNodes);
	}
	
    public synchronized void createThumbnails()  {
    	Platform.setImplicitExit(false);
        
    	if (fxCanvas != null && !fxCanvas.isDisposed()) {
    		
    		fxCanvas.dispose();
    		fxCanvas = null;
    	}
    	fxCanvas = new FXCanvas(this, SWT.NONE);
        toolkit.adapt(fxCanvas);
        
        ScrollPane root = new ScrollPane();
        root.setStyle("-fx-background: white");// + toHex(toolkit.getColors().getBackground())); //$NON-NLS-1$
        root.setOnMouseClicked(e->{
        	clearSelection();
        });
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Horizontal
        root.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Vertical scroll bar
        root.setFitToWidth(true);
        
        tile = new TilePane();
        tile.setPadding(new Insets(marginSize, marginSize, marginSize, marginSize));
        tile.setHgap(marginSize);
        tile.setVgap(marginSize);
        tile.setPrefTileHeight(thumbSize+4);	//4=max border width
        tile.setPrefTileWidth(thumbSize+4);
        tile.setStyle("-fx-background-color: white");// + toHex(toolkit.getColors().getBackground())); //$NON-NLS-1$
        tile.setOnMouseClicked(e->{
        	clearSelection();
        });
        
        root.setContent(tile);
        
        Scene scene = new Scene(root);
        fxCanvas.setScene(scene);
        
       loadImages.schedule();
    }

	protected String getThumbColor(ISmartAttachment file) {
		return ImageGallery.this.backgroundColor;
	}
	
	protected String getMouseOverBackground(ISmartAttachment file) {
		return ImageGallery.this.mouseOverBackgroundColor;
	}
	
	protected String getSelectionBackgroundColor(ISmartAttachment file) {
		return ImageGallery.this.selectionBackgroundColor;
	}
	
	private void clearSelection(){
		if (thumbs == null) return;
		for (ThumbInfo c : thumbs){
			c.isSelected = false;
			c.updateStyle();
		}
	}
    
	private void fireSelectionEvents(org.eclipse.swt.widgets.Event event) {
		for (Listener l : ImageGallery.this.getListeners(SWT.Selection)) {
			l.handleEvent(event);
		}
	}
	
    private class ThumbInfo {
    	
		private static final String LAST_SELECTION_INDEX_KEY = "last_selection_index"; //$NON-NLS-1$
		
		ISmartAttachment file;
		String tooltip;
		
		boolean isSelected;
		boolean mouseOver;
		int index;
			
		BorderPane imageNode;
		
		public ThumbInfo(ISmartAttachment file, int index){
			this.file = file;
			tooltip = file.getFilename();
			this.index = index;
			createNode();
		}
		
		private void updateStyle() {
			String borderColor = ImageGallery.this.borderColor;
			String backgroundColor = getThumbColor(file);
			int width = 1;
			
			if (mouseOver) {
				borderColor = mouseOverBorderColor;
				width = 2;
			}else if (isSelected) {
				borderColor = selectionBorderColor;
				width = 2;
			}
			
			if (isSelected) {
				backgroundColor = getSelectionBackgroundColor(file);
			}else if (mouseOver) {
				backgroundColor = getMouseOverBackground(file);
			}
			imageNode.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: " +width + "px; -fx-background-color: " + backgroundColor + ";"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		
		private void createNode() {
			imageNode = null;
	        try {
	        	//these "attachments" are not encrypted so we can access the files directly
	        	double[] transform = SmartUtils.getExifRotation(file.getAttachmentFile());
	        	try(InputStream is = Files.newInputStream(file.getAttachmentFile())){
		            final Image image = new Image(is, thumbSize, thumbSize, true, true);
		            ImageView imageView = new ImageView(image);
		            
		            imageView.setScaleX(transform[1]);
		            imageView.setScaleY(transform[2]);
		            imageView.setRotate(transform[0]);
		            
		            imageNode = new BorderPane(imageView);
		            imageNode.setPrefHeight(thumbSize);
		            imageNode.setPrefWidth(thumbSize);
		            
		            updateStyle();
		            
		            imageNode.setOnMouseEntered(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent event) {
							mouseOver = true;
							updateStyle();
						}
		            	
		            });
		            
		            imageNode.setOnMouseExited(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent event) {
							mouseOver = false;
							updateStyle();		
						}
		            	
		            });
		            
		            imageNode.setOnMouseClicked(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent event) {
							changeSelection(event);
							updateStyle();
							
							if (event.getButton() == MouseButton.SECONDARY) {
								//show menu
								Point p = ImageGallery.this.toDisplay((int)event.getSceneX(), (int)event.getSceneY());
								if (thumbMenu != null) thumbMenu.show(imageNode, p.x, p.y);
							}
							event.consume();
						}
		            	
		            });
	        	}		            
	        }catch (Exception ex) {
	        	AssetPlugIn.log(ex.getMessage(), ex);
	        	imageNode = new BorderPane();
	        }
	        
	        Tooltip.install(imageNode, new Tooltip(tooltip));
		}
		
		private void changeSelection(MouseEvent event){

			Integer lastSelection = (Integer) getParent().getData(LAST_SELECTION_INDEX_KEY);
			if (lastSelection == null) lastSelection = 0;
			getParent().setData(LAST_SELECTION_INDEX_KEY, index);
			if (event.isControlDown()){
				if (event.getButton() == MouseButton.PRIMARY) isSelected = !isSelected;
			}else if (event.isShiftDown()){
				boolean newSelection = !isSelected;
				//clearSelection();
				
				int from = lastSelection;
				int to = index;
				
				if (index < lastSelection){
					from = index;
					to = lastSelection;
				}
				for (int i = from; i <= to; i ++){
					if (i == index){
						thumbs.get(i).isSelected = true;
					}else{
						thumbs.get(i).isSelected = newSelection;		
					}
					thumbs.get(i).updateStyle();
				}
			}else{
				if (event.getButton() == MouseButton.PRIMARY){
					clearSelection();
				}else if (!isSelected){
					clearSelection();
				}
				isSelected = true;
			}
			updateStyle();
			fireSelectionEvents(new org.eclipse.swt.widgets.Event());
		}
	}
	

	/**
	 * Interface for creating a menu to attach to each thumbnail
	 * @author Emily
	 *
	 */
	public interface IMenuCreator {
		/**
		 * Creates a menu
		 * @param parent
		 * @return
		 */
		public ContextMenu createMenu();
		
	}
}