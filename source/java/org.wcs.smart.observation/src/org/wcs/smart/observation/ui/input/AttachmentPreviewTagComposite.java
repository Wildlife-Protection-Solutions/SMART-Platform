package org.wcs.smart.observation.ui.input;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.nebula.widgets.chips.Chips;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.AttachmentTag;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.AttachmentTagLink;
import org.wcs.smart.observation.model.ITaggedAttachment;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * For editting: Only works for observation and waypoint attachments.
 */
public class AttachmentPreviewTagComposite extends Composite {

	private TableViewer attachmentViewer;
	
	private Composite attachmentThumbnail;
	private Composite tagList;
	
	
	private List<AttachmentTag> tags;
	private Color lgreen, lblue;
	
	private boolean readonly = false;
	private Function<ISmartAttachment, Boolean> canEdit;
	private Consumer<Event> onAdd;
	private Consumer<Event> onDelete;
	
	/**
	 * create read only viewer for attachment with 
	 * table viewer on the left and attachment details on the 
	 * right
	 * 
	 * @param parent
	 */
	public AttachmentPreviewTagComposite(Composite parent) {
		super(parent, SWT.NONE);
		this.tags = null;
		this.readonly = true;
		create(null, null, null, i->false);
	}
	
	/**
	 * create attachment list viewer which allows you
	 * to add, delete, and edit tags associated with the attachment
	 * @param parent
	 * @param tags
	 * @param onAdd
	 * @param onDelete
	 */
	public AttachmentPreviewTagComposite(Composite parent,  List<AttachmentTag> tags, 
			Consumer<Event> onAdd, Consumer<Event> onDelete) {
		this(parent, tags, onAdd, onDelete, i->true);
	}

	public AttachmentPreviewTagComposite(Composite parent,  List<AttachmentTag> tags, 
			Consumer<Event> onAdd, Consumer<Event> onDelete, Function<ISmartAttachment, Boolean> canEdit) {
		super(parent, SWT.NONE);
		setTags(tags);
		this.readonly = false;
		create(tags, onAdd, onDelete, canEdit);
		
	}

	public void setTags(List<AttachmentTag> tags) {
		this.tags = new ArrayList<>(tags);
		Collections.sort(tags);
	}
	
	private void fireModified() {
		for (Listener l : this.getListeners(SWT.Modify)) {
			l.handleEvent(new Event());
		}
	}
	
	public void deleteAttachments(List<? extends ISmartAttachment> attachments) {
		for (Iterator<ISmartAttachment> iterator = getSelection().iterator(); iterator.hasNext();) {
			ISmartAttachment remove = (ISmartAttachment) iterator.next();
			if (remove instanceof WaypointAttachment) attachments.remove(remove);
		}
		refresh();
		setSelection(null);
	}
	
	public void addAttachment(Supplier<ISmartAttachment> create, List<ISmartAttachment> attachments) {
		FileDialog fd = new FileDialog(getShell(), SWT.MULTI);
		
		String file = fd.open();
		if (file == null) {
			return;
		}
		Path root = Paths.get(fd.getFilterPath());
		ISmartAttachment added = null;
		for (int i = 0; i < fd.getFileNames().length; i ++){
			Path f = root.resolve(fd.getFileNames()[i]);
			if (!Files.exists(f)){
				SmartPlugIn.displayLog(MessageFormat.format("File {0} not found.", f.toAbsolutePath().toString()), null);
				return;
			}
			ISmartAttachment wpa = create.get();
			wpa.setCopyFromLocation(f);
			wpa.setFilename(f.getFileName().toString());
			attachments.add(wpa);
			added = wpa;
			
		}
		refresh();
		setSelection(added);

	}
	
	private void doAddAttachment() {
		onAdd.accept(new Event());
		fireModified();
		attachmentViewer.refresh();
	}
	
	private void doRemoveAttachment() {
		onDelete.accept(new Event());
		fireModified();
		attachmentViewer.refresh();
	}
	
	private void create(List<AttachmentTag> tags, Consumer<Event> onAdd, 
			Consumer<Event> onDelete,
			Function<ISmartAttachment, Boolean> canEdit) {
		
		this.canEdit = canEdit;
		this.onAdd = onAdd;
		this.onDelete = onDelete;
		
		lgreen = new Color(getDisplay(), 245, 255, 245);
		lblue = new Color(getDisplay(), 245, 245, 255);
		addDisposeListener(e->{
			lgreen.dispose();
			lblue.dispose();
		});

		setLayout(new GridLayout(2, true));
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Composite leftAttach = new Composite(this, SWT.BORDER);
		leftAttach.setLayout(new GridLayout(readonly ? 1 : 2, false));
		((GridLayout)leftAttach.getLayout()).marginWidth = 0;
		((GridLayout)leftAttach.getLayout()).marginHeight = 0;
		leftAttach.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)leftAttach.getLayoutData()).heightHint = 80;
		
		Composite wrapper = new Composite(leftAttach, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		attachmentViewer = new TableViewer(wrapper, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		attachmentViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attachmentViewer.setContentProvider(ArrayContentProvider.getInstance());
		attachmentViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				ISmartAttachment a = (ISmartAttachment)element;
				if (canEdit.apply(a)) {
					return ((ISmartAttachment)element).getFilename();
				}else {
					return "**" + ((ISmartAttachment)element).getFilename(); //$NON-NLS-1$
				}
			}
		});
		TableColumn tc = new TableColumn(attachmentViewer.getTable(), SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(100));
		wrapper.setLayout(layout);
		
		attachmentViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISmartAttachment att = (ISmartAttachment) ((StructuredSelection)attachmentViewer.getSelection()).getFirstElement();
				if (att != null){
					AttachmentUtil.openAttachment(att);
				}
			}
		});
		
		if (!readonly) {
			ToolBar tb = new ToolBar(leftAttach, SWT.FLAT | SWT.VERTICAL);
			tb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			ToolItem tiEdit = new ToolItem(tb, SWT.PUSH);
			tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			tiEdit.setToolTipText(Messages.AttributeWizardPage_addAttachmentTooltip);
			tiEdit.addListener(SWT.Selection, onAdd::accept);
			
			ToolItem tiDelete = new ToolItem(tb, SWT.PUSH);
			tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			tiDelete.setToolTipText(Messages.AttributeWizardPage_removeAttachmenttooltip);
			tiDelete.addListener(SWT.Selection, e->doRemoveAttachment());
			
			Menu menu = new Menu(attachmentViewer.getControl());
			
			MenuItem miEdit = new MenuItem(menu, SWT.PUSH);
			miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			miEdit.setText(DialogConstants.ADD_BUTTON_TEXT);
			miEdit.addListener(SWT.Selection, e->doAddAttachment());
			
			MenuItem miDelete = new MenuItem(menu, SWT.PUSH);
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			miDelete.addListener(SWT.Selection, onDelete::accept);
			
			attachmentViewer.getControl().setMenu(menu);
			
			attachmentViewer.addSelectionChangedListener(e->{
				miDelete.setEnabled(canEdit.apply(getFirstSelection()));
				tiDelete.setEnabled(canEdit.apply(getFirstSelection()));
			});
		}
		
		Composite rightAttach = new Composite(this, SWT.NONE);
		rightAttach.setLayout(new GridLayout(2, false));
		((GridLayout)rightAttach.getLayout()).marginWidth = 0;
		((GridLayout)rightAttach.getLayout()).marginHeight = 0;
		rightAttach.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)rightAttach.getLayoutData()).widthHint = 300;
		
		attachmentThumbnail = new Composite(rightAttach, SWT.NONE);
		attachmentThumbnail.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		((GridData)attachmentThumbnail.getLayoutData()).widthHint = 100;
		((GridData)attachmentThumbnail.getLayoutData()).heightHint = 100;
		
		tagList = new Composite(rightAttach, SWT.NONE);
		tagList.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		tagList.setLayout(new GridLayout());
		((GridLayout)tagList.getLayout()).marginWidth = 0;
		((GridLayout)tagList.getLayout()).marginHeight = 0;
		
		attachmentViewer.addSelectionChangedListener(e->{
			ISmartAttachment a = (ISmartAttachment) attachmentViewer.getStructuredSelection().getFirstElement();
			updateSelection(a);
		});
	}
	
	public void setSelection(ISmartAttachment selection) {
		if (selection == null) {
			attachmentViewer.setSelection(null);
			return;
		}
		attachmentViewer.setSelection(new StructuredSelection(selection));
	}
	
	private void updateSelection(ISmartAttachment selection) {

		while(tagList.getChildren().length > 0) {
			tagList.getChildren()[0].dispose();
		}
		while(attachmentThumbnail.getChildren().length > 0) {
			attachmentThumbnail.getChildren()[0].dispose();
		}
		
		if (selection == null) {
			layout();
			return ;
		}
		
		Thumbnail thumb = new Thumbnail(selection,100, true);
		thumb.createThumbnail(attachmentThumbnail);
		
		if ( !(selection instanceof ITaggedAttachment)){
			layout();
			return;
		}
		
		ITaggedAttachment tagged = (ITaggedAttachment)selection;
		
		if (tagged.getAttachmentTags() == null) {
			tagged.setAttachmentTags(new ArrayList<>());
		}
		
		Composite ctags = new Composite(tagList, SWT.NONE);
		ctags.setLayout(new RowLayout());
		((RowLayout)ctags.getLayout()).wrap = true;
		ctags.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ctags.setBackground(ctags.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		List<AttachmentTagLink> atags = tagged.getAttachmentTags();
		Set<AttachmentTag> hastags = new HashSet<>();
		for (AttachmentTagLink link : atags) {
			Chips x = new Chips(ctags, readonly || !this.canEdit.apply(selection) ? SWT.NONE : SWT.CLOSE);
			x.setText(link.getTag().getName());
			x.setChipsBackground(x.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			x.setHoverBackground(lblue);
			x.setBorderColor(x.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
			x.setHoverBorderColor(x.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
			x.setBackground(x.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			hastags.add(link.getTag());
			
			x.addCloseListener(e->{
				tagged.getAttachmentTags().remove(link);
				updateSelection(selection);
				fireModified();
			});
		}
		
		if (!readonly && hastags.size() != this.tags.size() && this.canEdit.apply(selection)) {
			Chips add = new Chips(tagList, SWT.PUSH);
			add.setText("Add Tag");
			add.setImage(ObservationPlugIn.getDefault().getImageRegistry().get(ObservationPlugIn.ADD14_ICON));
			add.setChipsBackground(add.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			add.setHoverBackground(lgreen);
			add.setBorderColor(add.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
			add.setHoverBorderColor(add.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
			add.setBackground(add.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			add.moveAbove(ctags);
			
			Menu mnu = new Menu(add);	
			
			for(AttachmentTag t : this.tags) {
				if (hastags.contains(t)) continue;
				MenuItem kid = new MenuItem(mnu, SWT.PUSH);
				kid.setText(t.getName());
				kid.addListener(SWT.Selection, evt->{
					
					AttachmentTagLink link = new AttachmentTagLink();
					link.setTag(t);
					if (selection instanceof ObservationAttachment) {
						link.setObservationAttachment((ObservationAttachment) selection);
					}else if (selection instanceof WaypointAttachment) {
						link.setWaypointAttachment((WaypointAttachment) selection);
					}else {
						return;
					}
					tagged.getAttachmentTags().add(link);
					updateSelection(selection);
					fireModified();
				});
				
				
				add.setMenu(mnu);
				add.addListener(SWT.Selection, evt->mnu.setVisible(true));
			}
		}
		this.tagList.getParent().layout(true);
		this.tagList.layout(true);
		layout(true);
	}
	
	public ISmartAttachment getFirstSelection() {
		if (this.attachmentViewer.getStructuredSelection().isEmpty()) return null;
		return (ISmartAttachment)this.attachmentViewer.getStructuredSelection().getFirstElement();
	}
	
	/**
	 * 
	 * @return the current selection
	 */
	public IStructuredSelection getSelection() {
		return this.attachmentViewer.getStructuredSelection();
	}
	
	public void refresh() {
		
		List<? extends ISmartAttachment> items = (List<? extends ISmartAttachment>) attachmentViewer.getInput();
		sortInput(items);
		this.attachmentViewer.refresh();
	}
	
	private void sortInput(List<? extends ISmartAttachment> items) {
		items.sort((a,b)->{
			boolean ea = canEdit.apply(a);
			boolean eb = canEdit.apply(b);
			if (ea && !eb) return -1;
			if (!ea && eb) return 1;
			return Collator.getInstance().compare(a.getFilename(), b.getFilename());
		});
	}
	
	public void setInput(List<? extends ISmartAttachment> attachments) {
		sortInput(attachments);
		
		this.attachmentViewer.setInput(attachments);
		if (!attachments.isEmpty()) {
			this.attachmentViewer.setSelection(new StructuredSelection(attachments.get(0)));
		}
		updateSelection(getFirstSelection());
	}

}
