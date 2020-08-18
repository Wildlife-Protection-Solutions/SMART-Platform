/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.report.birt.map.properties;

import org.eclipse.birt.report.designer.internal.ui.util.WidgetUtil;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.IDescriptorProvider;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.section.Section;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.ReportItemHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.birt.report.model.elements.ExtendedItem;
import org.eclipse.birt.report.model.elements.ReportDesign;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.report.birt.map.SmartMapItemPlugIn;
import org.wcs.smart.report.birt.map.internal.Messages;
import org.wcs.smart.report.birt.map.item.SmartMapItem;

/**
 * DPI Section that displays a DpiPropertyProvider.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class ApplyAllSection  extends Section {

	protected IDescriptorProvider provider;
	private int width = -1;
	private FormToolkit toolkit;
	private ExtendedItemHandle itemHandle;
	
	public ApplyAllSection( Composite parent, boolean isFormStyle, FormToolkit toolkit, ExtendedItemHandle itemHandle) {
		super( "", parent, isFormStyle ); //$NON-NLS-1$
		this.toolkit = toolkit;
		this.itemHandle = itemHandle;
	}

	public void createSection( ) {
		Hyperlink btnApplyAllMaps = toolkit.createHyperlink(parent, Messages.ApplyAllSection_ApplyToAllMaps, SWT.NONE);
		btnApplyAllMaps.setToolTipText(Messages.ApplyAllSection_ApplyToAllTooltip);
		GridData gd = new GridData(SWT.FILL, SWT.BOTTOM, false, false);
		btnApplyAllMaps.setLayoutData(gd);
		btnApplyAllMaps.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				SlotHandle h  = itemHandle.getRoot().getComponents();
				
				for (Object o : ((ReportDesign)h.getElement()).getAllElements()) {
					if (o instanceof ExtendedItem && ((ExtendedItem)o).getExtendedElement() instanceof SmartMapItem) {
						
						
						
						ExtendedItemHandle handle = (ExtendedItemHandle) ((ExtendedItem)o).getHandle(h.getModule());
						if (handle == ApplyAllSection.this.itemHandle) continue;
						
						SmartMapItem smartItem = (SmartMapItem) ((ExtendedItem)o).getExtendedElement();
						try {
							handle.setProperty(ReportItemHandle.HEIGHT_PROP, ApplyAllSection.this.itemHandle.getProperty(ReportItemHandle.HEIGHT_PROP));
							handle.setProperty(ReportItemHandle.WIDTH_PROP, ApplyAllSection.this.itemHandle.getProperty(ReportItemHandle.WIDTH_PROP));
							smartItem.setDPI(((SmartMapItem)ApplyAllSection.this.itemHandle.getReportItem()).getDPI());
						}catch (Exception ex) {
							SmartMapItemPlugIn.displayLog(ex.getMessage(), ex);
						}
						
					}
				}
				

			}
		});
	}

	public void layout( ) {

	}

	
	public IDescriptorProvider getProvider( ) {
		return provider;
	}

	public void setProvider( IDescriptorProvider provider ) {
		this.provider = provider;
	}

	public int getWidth( ) {
		return width;
	}

	public void setWidth( int width ) {
		this.width = width;
	}

	public void setInput( Object input ) {
		assert ( input != null );
	}
	
	public void load( )	{
		
	}

	public void reset( ) {
		
	}

	public void setHidden( boolean isHidden ){
		if ( displayLabel != null )
			WidgetUtil.setExcludeGridData( displayLabel, isHidden );
		if ( placeholderLabel != null )
			WidgetUtil.setExcludeGridData( placeholderLabel, isHidden );
	}

	public void setVisible( boolean isVisible ){
		if ( displayLabel != null )
			displayLabel.setVisible( isVisible );
		
		if ( placeholderLabel != null )
			placeholderLabel.setVisible( isVisible );
	}
}
