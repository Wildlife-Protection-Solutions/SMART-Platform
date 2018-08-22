/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.data;

import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.importer.ActionableWarning;
import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.SmartUtils;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.xmp.XmpDirectory;

/**
 * Panel for displaying details of selected file or multiple
 * images depending on selection
 * 
 * @author Emily
 *
 */
public class FileDetailsPanel {

	private static final String LAST_SELECTION = "LAST_SELECTION"; //$NON-NLS-1$
	private static final String LAST_INDEX_KEY = "LAST_INDEX"; //$NON-NLS-1$
	private static final String IMAGE_DATAKEY = "IMAGE"; //$NON-NLS-1$
	private static final String IMAGE_PROXY_DATAKEY = "IMAGE_PROXY"; //$NON-NLS-1$
	
	private Composite fileDetailsComposite;
	private Composite singleSelectDetails;
	private Composite multiSelectDetails;
	
	private TableViewer tblExif;
	
	private Label lblDetailsFileName; 
	private Label lblDetailsStatus ;
	private Canvas imageCanvas;
	private Composite proxyDetailsComp; 
	 
	private DataImportPage view;
	private FormToolkit toolkit;
	
	public FileDetailsPanel(Composite parent, DataImportPage view, FormToolkit toolkit) {
		this.view = view;
		this.toolkit = toolkit;
		createDetailsComposite(parent, toolkit);
	}
	
	private void createDetailsComposite(Composite parent, FormToolkit toolkit) {
		fileDetailsComposite = toolkit.createComposite(parent, SWT.NONE);
		fileDetailsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fileDetailsComposite.setLayout(new StackLayout());
		
		singleSelectDetails = toolkit.createComposite(fileDetailsComposite);
		singleSelectDetails.setLayout(new GridLayout());
		
		multiSelectDetails = toolkit.createComposite(fileDetailsComposite);
		multiSelectDetails.setLayout(new GridLayout());
		
		((StackLayout)fileDetailsComposite.getLayout()).topControl = singleSelectDetails;
		
		Composite top = toolkit.createComposite(singleSelectDetails);
		top.setLayout(new GridLayout(2, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblDetailsStatus = toolkit.createLabel(top, ""); //$NON-NLS-1$
		lblDetailsFileName = toolkit.createLabel(top, ""); //$NON-NLS-1$
		lblDetailsFileName .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SashForm detailsSash = new SashForm(singleSelectDetails, SWT.VERTICAL);
		detailsSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite infoComposite = toolkit.createComposite(detailsSash, SWT.NONE);
		infoComposite.setLayout(new GridLayout());
		infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)infoComposite.getLayout()).marginWidth = 0;
		((GridLayout)infoComposite.getLayout()).marginHeight = 0;
		
		Composite header = toolkit.createComposite(infoComposite);
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		
		
		Hyperlink lnkDetails = toolkit.createHyperlink(header, Messages.FileDetailsPanel_DetailsSection, SWT.NONE);
		Hyperlink lnkExif = toolkit.createHyperlink(header, Messages.FileDetailsPanel_MetadataSection, SWT.NONE);
		
		Composite stackComposite = toolkit.createComposite(infoComposite, SWT.BORDER);
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackComposite.setLayout(new StackLayout());
		
		proxyDetailsComp = toolkit.createComposite(stackComposite);
		proxyDetailsComp.setLayout(new GridLayout());
		((GridLayout)proxyDetailsComp.getLayout()).marginWidth = 0;
		((GridLayout)proxyDetailsComp.getLayout()).marginHeight = 0;
		
		Composite exifMetadataComp = toolkit.createComposite(stackComposite);
		exifMetadataComp.setLayout(new GridLayout());
		((GridLayout)exifMetadataComp.getLayout()).marginWidth = 0;
		((GridLayout)exifMetadataComp.getLayout()).marginHeight = 0;
		
		tblExif = new TableViewer(exifMetadataComp, SWT.FULL_SELECTION);
		tblExif.getTable().setLinesVisible(false);
		tblExif.getTable().setHeaderVisible(true);
		tblExif.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblExif.setContentProvider(ArrayContentProvider.getInstance());
		
		Color bgColor = new Color(tblExif.getControl().getDisplay(), 160,185,224);
		tblExif.getControl().addListener(SWT.Dispose, e->bgColor.dispose());
		
		TableViewerColumn colTag = new TableViewerColumn(tblExif, SWT.NONE);
		colTag.getColumn().setText(Messages.FileDetailsPanel_TagColumn);
		colTag.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof String[]) return ((String[])element)[0];
				if (element instanceof String) return (String)element;
				return ""; //$NON-NLS-1$
			}
			@Override
			public Color getBackground(Object element) {
				if (element instanceof String) return bgColor;
				return null;
			}
		});
		
		
		TableViewerColumn colTagValue = new TableViewerColumn(tblExif, SWT.NONE);
		colTagValue.getColumn().setText(Messages.FileDetailsPanel_ValueColumn);
		colTagValue.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof String[]) return ((String[])element)[1];
				return ""; //$NON-NLS-1$
			}
			@Override
			public Color getBackground(Object element) {
				if (element instanceof String) return bgColor;
				return null;
			}
		});
		
		FontData fd = lnkDetails.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(lnkDetails.getDisplay(), fd);
		Font normalFont = lnkDetails.getFont(); 
		lnkDetails.addListener(SWT.Dispose, e->boldFont.dispose());
		
		lblDetailsFileName.setFont(boldFont);
		lnkDetails.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackComposite.getLayout()).topControl = proxyDetailsComp;
				stackComposite.layout();
				lnkDetails.setFont(boldFont);
				lnkExif.setFont(normalFont);
				header.layout();
			}
		});
		lnkExif.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				((StackLayout)stackComposite.getLayout()).topControl = exifMetadataComp;
				stackComposite.layout();
				lnkDetails.setFont(normalFont);
				lnkExif.setFont(boldFont);
				header.layout();
			}
		});

		((StackLayout)stackComposite.getLayout()).topControl = proxyDetailsComp;
		lnkDetails.setFont(boldFont);
		
		imageCanvas = new Canvas(detailsSash,SWT.BORDER);
		imageCanvas.addListener(SWT.Paint, e->{
			Image img = (Image)imageCanvas.getData(IMAGE_DATAKEY);
			if (img == null || img.isDisposed()) return;
			
			Rectangle bounds = img.getBounds();
			Rectangle cbounds = imageCanvas.getBounds();	
			// scale image
			int x = 0, y = 0, width = 0, height = 0;
			if (cbounds.width > cbounds.height) {
				height = cbounds.height;
				width = bounds.width * height / bounds.height;
				x = (cbounds.width - width) / 2;
			} else {
				width = cbounds.width;
				height = bounds.height * width / bounds.width;
				y = (cbounds.height - height) / 2;
			}
			e.gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, x, y, width, height);
		});
		imageCanvas.addListener(SWT.Dispose, e->{
			Image img = (Image)imageCanvas.getData(IMAGE_DATAKEY);
			if (img != null && img.isDisposed()) img.dispose();
		});
		imageCanvas.addListener(SWT.MouseDoubleClick, e->{
			final FileProxy p = (FileProxy)imageCanvas.getData(IMAGE_PROXY_DATAKEY);
			if (p == null) return;
			AttachmentUtil.launch(p.getFile().toFile());
		});
		detailsSash.setWeights(new int[] {3,2});
		
		fileDetailsComposite.layout(true);	
	}
	
	/**
	 * Update the panel based on the provided selection
	 * 
	 * @param selection
	 */
	void updateFileDetails(IStructuredSelection selection) {	
		//clear existing
		
		if (selection != null && selection.size() == 1) {
			Object selected = selection.getFirstElement();
			Object last = proxyDetailsComp.getData(LAST_SELECTION);
			if (selected.equals(last)) return;
			
			proxyDetailsComp.setData(LAST_SELECTION, selected);
		}else {
			proxyDetailsComp.setData(LAST_SELECTION, null);
		}
		
		if (proxyDetailsComp.isDisposed()) return;
		for (Control c : proxyDetailsComp.getChildren()) c.dispose();
		tblExif.setData(LAST_INDEX_KEY, tblExif.getTable().getVerticalBar().getSelection());
		tblExif.setInput(null);
		Image lastImage = (Image) imageCanvas.getData(IMAGE_DATAKEY);
		if (lastImage != null && !lastImage.isDisposed()) lastImage.dispose();
		imageCanvas.redraw();
				
		for (Control c : multiSelectDetails.getChildren()) c.dispose();
		
		if (selection == null || selection.isEmpty()) {
			lblDetailsFileName.setText( "" ); //$NON-NLS-1$
			lblDetailsStatus.setImage( null );
			return;
		}
		
		if (selection.size() > 1) {
			//multi select pain
			
			ScrolledComposite sc = new ScrolledComposite(multiSelectDetails, SWT.V_SCROLL);
			Composite details = toolkit.createComposite(sc);
			sc.setContent(details);
			sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			int cnt = 0;
			int size = sc.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - sc.getVerticalBar().getSize().x;
			
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				Object item = iterator.next();
				if (!(item instanceof FileProxy)) continue;
				FileProxy proxy = (FileProxy)item;
				
				Canvas canvas = new Canvas(details, SWT.BORDER);
				toolkit.adapt(canvas);
				
				if (proxy.getIncidentGroup() != null) {
					int colorIndex = proxy.getIncidentGroup() % view.getRowColors().length;
					canvas.setBackground(view.getRowColors()[colorIndex]);
				}
				canvas.setBounds(0, (cnt+2)*size, size, size);
				cnt++;
				
				canvas.setData(IMAGE_PROXY_DATAKEY, proxy);
				canvas.addListener(SWT.Paint, e->{
					if (canvas.isDisposed()) return;
					Image img = (Image)canvas.getData(IMAGE_DATAKEY);
					if (img == null || img.isDisposed()) return;
					// scale image
					Rectangle cbounds = canvas.getBounds();	
					Rectangle bounds = img.getBounds();
					int x = 0, y = 0, width = 0, height = 0;
					if (cbounds.width > cbounds.height) {
						height = cbounds.height;
						width = bounds.width * height / bounds.height;
						x = (cbounds.width - width) / 2;
					} else {
						width = cbounds.width;
						height = bounds.height * width / bounds.width;
						y = (cbounds.height - height) / 2;
					}
					e.gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, x, y, width, height);
				});
				String tooltip = proxy.getFile().getFileName().toString();
				if (proxy.getIncidentGroup() != null) {
					tooltip += "\n" + Messages.FileDetailsPanel_GroupTooltip + proxy.getIncidentGroup(); //$NON-NLS-1$
				}
				canvas.setToolTipText(tooltip);
				canvas.addListener(SWT.Dispose, e->{
					Image img = (Image)canvas.getData(IMAGE_DATAKEY);
					if (img != null && img.isDisposed()) img.dispose();
				});
				canvas.addListener(SWT.MouseDoubleClick, e->{
					final FileProxy p = (FileProxy)canvas.getData(IMAGE_PROXY_DATAKEY);
					if (p == null) return;
					AttachmentUtil.launch(p.getFile().toFile());
				});
				
				
			}
			details.setSize(size, cnt*size);
			
			
			//only load and display images
			//in the viewing range so we don't run
			//out of memory 
			Job refreshimage = new Job(Messages.FileDetailsPanel_refreshJobName) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Display.getDefault().syncExec(()->{
						if (sc.isDisposed()) return;
						int min = Math.abs(details.getLocation().y);
						int max = min + sc.getBounds().height;
						for (Control c : details.getChildren()) {
							Image img = (Image) c.getData(IMAGE_DATAKEY);
							int y = c.getLocation().y;
							int y2 = c.getLocation().y + c.getBounds().height;
							
							if ((y > min && y < max) || (y2>min && y2 < max)) {
								if (img != null) continue;	
								LoadImageJob imgjob = new LoadImageJob((Canvas)c);
								imgjob.setSystem(true);
								imgjob.schedule();
								
							}else {
								if (img != null) {
									img.dispose();
									c.setData(IMAGE_DATAKEY, null);
								}
							}
						}
					});
					
					return Status.OK_STATUS;
				}
				
			};
			sc.addListener(SWT.Resize, e->{
				int size2 = sc.getBounds().width - sc.getVerticalBar().getSize().x;
				int cnt2 = 0;
				for (Control c : details.getChildren()) {
					c.setBounds(0, cnt2 * (size2 + 2), size2, size2);
					cnt2++;
				}				
				details.setSize(size2, cnt2*(size2+2));
				sc.setMinSize(size2, cnt2 * (size2+2));
				sc.layout(true);
				details.layout(true);
				refreshimage.schedule(200);
			});
			sc.getVerticalBar().addListener(SWT.Selection, e->refreshimage.schedule(200));
			
			((StackLayout)fileDetailsComposite.getLayout()).topControl = multiSelectDetails;
			
			fileDetailsComposite.layout(true);
			multiSelectDetails.layout(true);
			refreshimage.schedule();
			return;
		}
		
		((StackLayout)fileDetailsComposite.getLayout()).topControl = singleSelectDetails;
		fileDetailsComposite.layout();
		Object first = selection.getFirstElement();
		if (!(first instanceof FileProxy)) {
			lblDetailsFileName.setText( "" ); //$NON-NLS-1$
			lblDetailsStatus.setImage( null );
			return;
		}
		
		FileProxy proxy = (FileProxy)first;
		
		lblDetailsFileName.setText(proxy.getFile().getFileName().toString());
		lblDetailsStatus.setImage( AssetPlugIn.getDefault().getImageRegistry().get(  proxy.isValid() ? AssetPlugIn.ICON_IMPORT_COMPLETE : AssetPlugIn.ICON_IMPORT_INCOMPLETE));
		if (!proxy.isValid()) lblDetailsStatus.setToolTipText(proxy.validMessage(Locale.getDefault()));
		lblDetailsStatus.getParent().layout();
		
		ScrolledComposite scroll = new ScrolledComposite(proxyDetailsComp, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		toolkit.adapt(scroll);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite bits = toolkit.createComposite(scroll);
		scroll.setContent(bits);
		bits.setLayout(new GridLayout());
		bits.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FontData fd = bits.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont = new Font(bits.getDisplay(), fd);
		bits.addListener(SWT.Dispose, e->boldFont.dispose());
		
		Composite fileSection = toolkit.createComposite(bits);
		fileSection.setLayout(new GridLayout(2, false));
		fileSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_SummarySection);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setFont(boldFont);
		
		if (!proxy.isValid()) {
			l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_StatusLabel);
			l = toolkit.createLabel(fileSection, proxy.validMessage(Locale.getDefault()));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_DateTimeLabel);
		l = toolkit.createLabel(fileSection, proxy.getImageDate() == null ? "" : DateFormat.getDateTimeInstance().format(proxy.getImageDate()) ); //$NON-NLS-1$
		
		l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_AssetLabel);
		l = toolkit.createLabel(fileSection, proxy.getAsset() == null ? "" : proxy.getAsset().getId() ); //$NON-NLS-1$
		
		l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_StationLabel);
		l = toolkit.createLabel(fileSection, proxy.getStation() == null ? "" : proxy.getStation().getId() ); //$NON-NLS-1$
		
		l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_LocationLabel);
		l = toolkit.createLabel(fileSection, proxy.getStationLocation() == null ? "" : proxy.getStationLocation().getId() ); //$NON-NLS-1$
		
		l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_LongLabel);
		l = toolkit.createLabel(fileSection, proxy.getX() == null ? "" : String.valueOf(proxy.getX()) ); //$NON-NLS-1$
		
		l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_LatLabel);
		l = toolkit.createLabel(fileSection, proxy.getY() == null ? "" : String.valueOf(proxy.getY()) ); //$NON-NLS-1$
		
		l = toolkit.createLabel(fileSection, Messages.FileDetailsPanel_CommentLabel);
		l = toolkit.createLabel(fileSection, proxy.getWaypointComment() == null ? "" : String.valueOf(proxy.getWaypointComment()) ); //$NON-NLS-1$
		
		Composite obsSection = toolkit.createComposite(bits);
		obsSection.setLayout(new GridLayout(2, false));
		obsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(obsSection, Messages.FileDetailsPanel_ObsLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setFont(boldFont);
		
		for (WaypointObservation wo : proxy.getObservations()) {
			l = toolkit.createLabel(obsSection, wo.getCategory().getFullCategoryName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			for (WaypointObservationAttribute a : wo.getAttributes()) {
				l = toolkit.createLabel(obsSection, a.getAttribute().getName() + ":"); //$NON-NLS-1$
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).horizontalIndent = 10;
				
				l = toolkit.createLabel(obsSection, a.getAttributeValueAsString(Locale.getDefault()));
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
		}
		
		Composite warnSection = toolkit.createComposite(bits);
		warnSection.setLayout(new GridLayout());
		warnSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(warnSection, Messages.FileDetailsPanel_WarningsLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setFont(boldFont);
		
		for (ActionableWarning aw : proxy.getWarnings()) {
			l = toolkit.createLabel(warnSection, aw.getMessage());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).horizontalIndent = 10;
			
		}
		
		scroll.setMinSize(bits.computeSize(SWT.DEFAULT,  SWT.DEFAULT));
		proxyDetailsComp.layout(true);
		
		

		Job j2 = new Job(Messages.FileDetailsPanel_readmetadatajobname) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<Directory, List<Tag>> exif = FileMetadataReader.readExifMetadata(proxy.getFile());
				List<Object> values = new ArrayList<>();
				if (exif != null) {
					List<Directory> dirs = new ArrayList<>();
					dirs.addAll(exif.keySet());
					dirs.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
					for (Directory d : dirs) {
						values.add(d.getName());
						if (d.getClass().equals(XmpDirectory.class)) {
							//get xmp metadata
							values.addAll(FileMetadataReader.readXmpMetadata((XmpDirectory)d));
						}else {
							List<Tag> items = exif.get(d);
							for (Tag t : items) {
								values.add(new String[] {t.getTagName(), t.getDescription()});
							}
						}
					}
				}
				
				Display.getDefault().syncExec(()->{
					
					if (exif == null) {
						tblExif.setInput(new String[] {Messages.FileDetailsPanel_ReadError});
					}else {
						int scroll = (int)tblExif.getData(LAST_INDEX_KEY);
						tblExif.setInput(values);
						
						for (TableColumn c : tblExif.getTable().getColumns()) {
							c.pack();
							c.setWidth(c.getWidth() + 20);
						}
						tblExif.getTable().setTopIndex(scroll);
					}
				});
				return Status.OK_STATUS;
			}
			
		};
		j2.schedule();
		
		imageCanvas.setData(IMAGE_PROXY_DATAKEY, proxy);
		LoadImageJob imgLoader = new LoadImageJob(imageCanvas);
		imgLoader.setSystem(true);
		imgLoader.schedule();
		
		fileDetailsComposite.layout(true);
	}
	
	private class LoadImageJob extends Job {
		
		Canvas toUpdate = null;
		FileProxy proxy = null;
		
		public LoadImageJob(Canvas toUpdate) {
			super(Messages.FileDetailsPanel_loadJobName);
			this.toUpdate = toUpdate;
		}

		protected IStatus run(IProgressMonitor monitor) {
			
			try {
				Display.getDefault().syncExec(()->{
					if (toUpdate.isDisposed()) return;
					proxy = (FileProxy) toUpdate.getData(IMAGE_PROXY_DATAKEY);
					Image lastImage = (Image) toUpdate.getData(IMAGE_DATAKEY);
					if (lastImage != null && !lastImage.isDisposed()) lastImage.dispose();
					toUpdate.setData(IMAGE_DATAKEY, null);
				});
				if (proxy == null) return Status.OK_STATUS;
				
			
				Image img = null;
				
				try{
					img = new Image(toUpdate.getDisplay(), proxy.getFile().toString());
					// transform based on exif orientation data
					Transform imageTransform = SmartUtils.getExifImageTransform(proxy.getFile().toFile(), img.getBounds().width, img.getBounds().height);
					if (imageTransform != null) {
						//TODO: performance
						Image image3 = new Image(Display.getDefault(), img.getBounds().height, img.getBounds().width);
						GC gc3 = new GC(image3);
						gc3.setTransform(imageTransform);
						gc3.drawImage(img, 0, 0);
						img.dispose();
						img = image3;
					}
				}catch (Exception ex) {
					ex.printStackTrace();
				}
				
				final Image ii = img;
				Display.getDefault().syncExec(()->{
					if (toUpdate.isDisposed()) {
						ii.dispose();
						return;
					}
					toUpdate.setData(IMAGE_DATAKEY, ii);
					toUpdate.redraw();
				});
			}catch (Exception ex) {
				//invalid format
				ex.printStackTrace();
			}
			return Status.OK_STATUS;
		}
		
	}
}
