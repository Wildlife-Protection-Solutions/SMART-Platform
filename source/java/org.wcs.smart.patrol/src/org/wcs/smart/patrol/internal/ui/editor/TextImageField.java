package org.wcs.smart.patrol.internal.ui.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.ca.IconItem;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.internal.Messages;

public class TextImageField extends Composite {

	private Label image;
	private Text text;
	
	private String noValueLabel = Messages.PatrolSummaryEditor_NoTeamLabel;
	
	private boolean disposeImage = true;
	
	public TextImageField(Composite parent) {
		super(parent, SWT.BORDER );
		createControl();
	}

	public TextImageField(Composite parent, int style) {
		super(parent, style );
		createControl();
	}
	
	public void setNoValueLabel(String noValue) {
		this.noValueLabel = noValue;
	}
	
	private void createControl() {
		setLayout(new GridLayout(2, false));
		((GridLayout)getLayout()).marginWidth = 2;
		((GridLayout)getLayout()).marginHeight = 2;
		
		image = new Label(this, SWT.NONE);
		image.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		image.addListener(SWT.Dispose, e->{
			if (image.getImage() != null) image.dispose();
		});
		text = new Text(this, SWT.NONE);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setEditable(false);
	}
	
	public void adapt(FormToolkit toolkit) {
		toolkit.adapt(this);
		toolkit.adapt(image, true, true);
		toolkit.adapt(text, true, true);
	}
	
	
	public void setValue(NamedItem item) {
		//dispose of existing image
		if (disposeImage && image.getImage() != null) {
			Image img = image.getImage();
			image.setImage(null);
			img.dispose();
		}
		disposeImage = true;
		if (item == null) {
			text.setText(noValueLabel);
			return;
		}
		
		text.setText(item.getName());
		
		if (item instanceof IconItem && ((IconItem)item).getIcon() != null) {
			Image img = IconManager.INSTANCE.getThumbnail( HibernateManager.loadIcon((IconItem)item), IconManager.Size.ICON);
			image.setImage(img);
			((GridData)image.getLayoutData()).widthHint = SWT.DEFAULT;
		}else {
			
			((GridData)image.getLayoutData()).widthHint = 0;
		}
		layout(true);
	}
	
	
	/**
	 * Sets the name and image. Note the pluginImage will not be disposed of
	 * @param name
	 * @param pluginImage
	 */
	public void setValue(String name, Image pluginImage) {
		if (disposeImage && image.getImage() != null) {
			Image img = image.getImage();
			image.setImage(null);
			img.dispose();
		}
		disposeImage = false;
		if (name == null) {
			text.setText(noValueLabel);
		}
		
		text.setText(name);
		if (pluginImage == null) {
			((GridData)image.getLayoutData()).widthHint = 0;
		}else {
			image.setImage(pluginImage);
			((GridData)image.getLayoutData()).widthHint = SWT.DEFAULT;
		}
		layout(true);
	}
}
