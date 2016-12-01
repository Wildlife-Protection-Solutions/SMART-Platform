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
package org.wcs.smart.i2.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Twistie;
import org.wcs.smart.i2.Intelligence2PlugIn;

/**
 * Creates a header to a sash form with a title and standard set of controls   
 * @author Emily
 *
 */
public class SmartSection extends Composite{

	public static final String MAX_KEY = "currentmaximized";
	public static final String KIDS_KEY = "kids";
	
	private boolean isMinimized;
	private Composite header;
	private Twistie img;
	
	private boolean processingEvent = false;
	private SashForm sashForm;
	private FormToolkit toolkit;
	
	@SuppressWarnings("unchecked")
	public SmartSection(SashForm parent, FormToolkit toolkit, String text) {
		super(parent, SWT.NONE);
		this.sashForm = parent;
		this.toolkit = toolkit;
		
		//add to list of parts
		List<SmartSection> parts = (List<SmartSection>) parent.getData(KIDS_KEY);
		if (parts == null){
			parts = new ArrayList<>();
			parent.setData(KIDS_KEY, parts);
		}
		parts.add(this);
		parent.setData(MAX_KEY, -1);
				
		createContents(parent, text);
		
		addListener(SWT.Resize, (e)->{
			parent.setData(MAX_KEY, -1);
			if (getClientArea().height > getMinSize()) setMinimized(false);
		});
		
		
	}
	
	private void setMinimized(boolean min){
		if (processingEvent) return;
		isMinimized = min;
		if (isMinimized){
			img.setExpanded(false);
		}else{
			img.setExpanded(true);
		}
	}
	
	private int getMinSize(){
		int size = header.computeSize(SWT.DEFAULT, SWT.DEFAULT).y+2;
		return size;
	}
	
	private void createContents(final SashForm sash, String text){
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		header = toolkit.createComposite(this, SWT.NONE);
		header.setLayout(new GridLayout(3, false));
		header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)header.getLayout()).marginWidth = 2;
		((GridLayout)header.getLayout()).marginHeight = 2;
		
		img = new Twistie(header, SWT.NONE){
			@Override
			protected void handleActivate(Event e) {
				
			}
		};
		img.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		img.setExpanded(true);
		Label l =toolkit.createLabel(header, text);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		final Font boldFont = new Font(getDisplay(), fd);
		l.addDisposeListener((e) -> {boldFont.dispose();});
		l.setFont(boldFont);

		Label exp = toolkit.createLabel(header, text);
		exp.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_SECTION_EXPAND));
		exp.addListener(SWT.MouseUp, (e)-> maximize());
		exp.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		
		MouseAdapter listener = new MouseAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				sashForm.setData(MAX_KEY, -1);
				List<SmartSection> items = (List<SmartSection>) sash.getData(KIDS_KEY);
				int index = -1;
				int sumWeights = 0;
				int weights[] = sash.getWeights();
				for (int i : weights) sumWeights += i;
				//re-portion to match current sash size then resize components
				int sumWeights2 = 0;
				
				for (int i = 0; i < weights.length; i ++){
					weights[i] = (int)( ( 1.0 * weights[i] / sumWeights) * sash.getClientArea().height);
					if (weights[i] < items.get(i).getMinSize()) weights[i] = items.get(i).getMinSize();
					sumWeights2 += weights[i];
				}
				
				for (int i = 0; i < items.size(); i ++){
					if (items.get(i) == SmartSection.this){
						index = i;
						break;
					}
				}
				
				int delta = 0;
				if (!isMinimized){
					setMinimized(true);
					delta = getClientArea().height - getMinSize();
				}else{
					//want to set the default height to some logical portion 
					//of entire page and take away from another expanded element
					setMinimized(false);
					
					int height = sumWeights2;
					for (SmartSection s : items){
						height -= s.getMinSize();
					}
					delta = -(height / items.size());
				}
				
				
				if (index < 0){
					resizeMinSize();
					return;
				}
				//find non minimized section to subtract from
				if (isMinimized){
					weights[index] = getMinSize();
				}else{
					weights[index] = -delta + getMinSize();
				}
				int start = index + 1;
				if (start >= items.size()) start = 0;
				while(start != index ){
					if (!items.get(start).isMinimized){
						int newweight = weights[start] + delta;
						if (newweight < items.get(start).getMinSize()){
							delta = delta + (newweight - items.get(start).getMinSize());
							newweight = items.get(start).getMinSize();
							weights[start] = newweight;
						}else{
							weights[start] = newweight;
							break;
						}
						
					}
					
					start ++;
					if (start >= items.size()) start = 0;
				}
				processingEvent = true;
				try{
					sash.setWeights(weights);	
				}finally{
					processingEvent = false;
				}
				
			}
		};
		l.addMouseListener(listener);
		img.addMouseListener(listener);
		
		l.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
		img.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
		exp.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
		
	}
	
	@SuppressWarnings("unchecked")
	public void resizeMinSize(){
		List<SmartSection> sections = (List<SmartSection>) sashForm.getData(KIDS_KEY);
		int[] newweights = new int[sections.size()];
		Arrays.fill(newweights, -1);
		int cnt = 0;
		int off = 0;
		for (int i = 0; i < sections.size(); i ++){
			
			if (sections.get(i).isMinimized){
				newweights[i] = sections.get(i).getMinSize();
				off += newweights[i];
			}else{
				cnt++;
			}
		}
		int totalHeight = sashForm.getClientArea().height - off;
		for (int i = 0; i < sections.size(); i ++){
			if (newweights[i] == -1){
				newweights[i] = totalHeight / cnt;
			}
		}		
		for (SmartSection s : sections) s.processingEvent = true;
		try{
			sashForm.setWeights(newweights);
		}finally{
			for (SmartSection s : sections) s.processingEvent = false;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void maximize(){
		List<SmartSection> sections = (List<SmartSection>) sashForm.getData(KIDS_KEY);
		for (SmartSection s : sections){
			s.setMinimized(s != this);
		}
		resizeMinSize();
	}

}