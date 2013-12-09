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