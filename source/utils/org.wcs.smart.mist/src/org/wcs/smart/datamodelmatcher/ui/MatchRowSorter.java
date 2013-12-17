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

package org.wcs.smart.datamodelmatcher.ui;

import java.util.Comparator;

public class MatchRowSorter implements Comparator<MatchRow> {
	
	int direction;
	int col;
	
    @Override
    public int compare(MatchRow o1, MatchRow o2) {
    	if(col == 2){
    		if(direction >0){
    			return o1.getSmartItem().getText().compareTo(o2.getSmartItem().getText());
    		}else{
    			return o2.getSmartItem().getText().compareTo(o1.getSmartItem().getText());
    		}
    	}else if(col == 1){
    		if(direction >0){
    			return o1.getMistItem().getText().compareTo(o2.getMistItem().getText());
    		}else{
    			return o2.getMistItem().getText().compareTo(o1.getMistItem().getText());
    		}
    	}else{
    		if(direction >0){
    			if(o1.getMatched() == o2.getMatched()){
    				return 0; 
    			}else if (o1.getMatched() ){
    				return 1;
    			}else{
    				return -1;
    			}
    		}else{
    			if(o1.getMatched() == o2.getMatched()){
    				return 0; 
    			}else if (o1.getMatched() ){
    				return -1;
    			}else{
    				return 1;
    			}
    		}
    	}
    }

    public void setDirection(int d){
    	this.direction = d;
    }
    
    public void setCol(int c){
    	this.col = c;
    }
}