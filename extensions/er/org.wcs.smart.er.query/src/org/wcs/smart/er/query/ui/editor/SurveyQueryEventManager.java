package org.wcs.smart.er.query.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.query.model.Query;

public class SurveyQueryEventManager {

	private static SurveyQueryEventManager instance;
	
	private List<SurveyDesignChangeListener> listeners;
	
	private SurveyQueryEventManager(){
		listeners = new ArrayList<SurveyQueryEventManager.SurveyDesignChangeListener>();
	}
	
	public static SurveyQueryEventManager getInstance(){
		if (instance == null){
			instance = new SurveyQueryEventManager();
		}
		return instance;
	}
	
	public void addSurveyDesignChangeListener(SurveyDesignChangeListener listener){
		listeners.add(listener);
		
	}
	
	public void removeSurveyDesignChangeListener(SurveyDesignChangeListener listener){
		listeners.remove(listener);
	}
	
	public void fireSurveyDesignChange(SurveyDesign newDesign, Query query){
		for (SurveyDesignChangeListener ll : listeners){
			ll.surveyDesignChange(newDesign, query);
		}
	}
	
	public interface SurveyDesignChangeListener{
		public void surveyDesignChange(SurveyDesign newDesign, Query query);
	}
}
