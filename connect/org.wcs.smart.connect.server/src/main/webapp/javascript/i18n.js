function i18n(key){
	var lang;
	if(navigator.languages){
		lang = navigator.languages[0] 
	}else{ 
		lang = navigator.language || navigator.userLanguage
	}

	if(lang == "es"){
		return labels_es[key];
	}else if(lang == "fr"){
		return labels_fr[key];
	}else if(lang == "hi"){
		return labels_hi[key];
	}else if(lang == "in"){
		return labels_in[key];
	}else if(lang == "km"){
		return labels_km[key];
	}else if(lang == "lo"){
		return labels_lo[key];
	}else if(lang == "ms"){
		return labels_ms[key];
	}else if(lang == "ru"){
		return labels_ru[key];
	}else if(lang == "th"){
		return labels_th[key];
	}else if(lang == "vi"){
		return labels_vi[key];
	}else if(lang == "zh"){
		return labels_zh[key];
	}else {
		return labels_en[key];
	}
}
