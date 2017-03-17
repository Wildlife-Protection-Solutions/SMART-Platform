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
package org.wcs.smart.report.birt.map.properties;

import org.eclipse.birt.report.designer.internal.ui.util.WidgetUtil;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.IDescriptorProvider;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.section.Section;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * DPI Section that displays a DpiPropertyProvider.
 * 
 * @author Emily
 *
 */
public class DpiComboSection  extends Section {

	protected DpiComboProvider combo;
	protected IDescriptorProvider provider;
	private int width = -1;
	
	public DpiComboSection( String labelText, Composite parent, boolean isFormStyle ) {
		super( labelText, parent, isFormStyle );
	}

	public void createSection( ) {
		getLabelControl( parent );
		getComboControl( parent );
		getGridPlaceholder( parent );
	}

	public void layout( ) {
		GridData gd = (GridData) combo.getControl( ).getLayoutData( );
		if ( getLayoutNum( ) > 0 ){
			gd.horizontalSpan = getLayoutNum( ) - 1 - placeholder;
		}else{
			gd.horizontalSpan = ( (GridLayout) parent.getLayout( ) ).numColumns
					- 1
					- placeholder;
		}
		
		if ( width > -1 ) {
			gd.widthHint = width;
			gd.grabExcessHorizontalSpace = false;
		} else {
			gd.grabExcessHorizontalSpace = false;
		}
	}

	protected DpiComboProvider getComboControl( Composite parent ) {
		if ( combo == null ) {
			combo = new DpiComboProvider(isFormStyle);
			if ( getProvider( ) != null ) combo.setDescriptorProvider( getProvider( ) );
			
			combo.createControl( parent );
			combo.getControl( ).setLayoutData( new GridData( ) );
			combo.getControl( ).addDisposeListener( new DisposeListener( ) {
				public void widgetDisposed( DisposeEvent event ) {
					combo = null;
				}
			});
		} else {
			checkParent( combo.getControl( ), parent );
		}
		return combo;
	}


	public IDescriptorProvider getProvider( ) {
		return provider;
	}

	public void setProvider( IDescriptorProvider provider ) {
		this.provider = provider;
		if ( combo != null )
			combo.setDescriptorProvider( provider );
	}

	public int getWidth( ) {
		return width;
	}

	public void setWidth( int width ) {
		this.width = width;
	}

	public void setInput( Object input ) {
		assert ( input != null );
		combo.setInput( input );
	}
	
	public void load( )	{
		if ( combo != null && !combo.getControl( ).isDisposed( ) )
			combo.load( );
	}

	public void reset( ) {
		if ( combo != null && !combo.getControl( ).isDisposed( ) )
			combo.reset( );
	}

	public void setHidden( boolean isHidden ){
		if ( displayLabel != null )
			WidgetUtil.setExcludeGridData( displayLabel, isHidden );
		if ( combo != null )
			combo.setHidden( isHidden );
		if ( placeholderLabel != null )
			WidgetUtil.setExcludeGridData( placeholderLabel, isHidden );
	}

	public void setVisible( boolean isVisible ){
		if ( displayLabel != null )
			displayLabel.setVisible( isVisible );
		if ( combo != null )
			combo.setVisible( isVisible );
		if ( placeholderLabel != null )
			placeholderLabel.setVisible( isVisible );
	}
}
