package org.wcs.smart.i2.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class SectionTabHeader extends Composite implements IHyperlinkListener{

	private Composite[] parts;
	private Composite stackPanel;
	private Font boldFont;
	private Font normalFont;
	private Hyperlink[] headers ;
	
	public SectionTabHeader(String tabs[], Composite parent, FormToolkit toolkit){
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout(tabs.length, false));
		setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		((GridLayout)getLayout()).marginHeight = 2;
		((GridLayout)getLayout()).marginWidth = 0;
		
		
		headers = new Hyperlink[tabs.length];
		for (int i = 0; i < tabs.length; i ++){
			Hyperlink header = toolkit.createHyperlink(this, tabs[i], SWT.NONE);
			header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
			headers[i] = header;
			normalFont = header.getFont();
			header.setData(i);
			header.addHyperlinkListener(this);
		}
		
		if (boldFont == null){
			FontData fd = headers[0].getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			boldFont = new Font(getDisplay(), fd);
			addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					boldFont.dispose();
				}
			});
		}
	}
	
	public void setContent(Composite[] parts, Composite stackPanel){
		this.parts = parts;
		this.stackPanel = stackPanel;
	}
	
	public void selectTab(int tab){
		linkActivated(new HyperlinkEvent(headers[0], null, null, -1));
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
	}
	
	@Override
	public void linkEntered(HyperlinkEvent e) {
	}
	
	@Override
	public void linkActivated(HyperlinkEvent e) {
		for (Hyperlink link : headers){
			if (link == e.widget){
				((StackLayout)stackPanel.getLayout()).topControl = parts[(int)link.getData()];
				link.setFont(boldFont);
			}else{
				link.setFont(normalFont);
			}
		}
		layout(true);
		stackPanel.layout();
	}
	
}
