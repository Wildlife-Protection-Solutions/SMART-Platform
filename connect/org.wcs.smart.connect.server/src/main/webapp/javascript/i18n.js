var pickaday_i18n = {
		previousMonth	: i18n("pickaday.previousmonth"),
		nextMonth		: i18n("pickaday.nextmonth"),
		months 			: [i18n("pickaday.jan"),i18n("pickaday.feb"),i18n("pickaday.mar"),i18n("pickaday.apr"),i18n("pickaday.may"),i18n("pickaday.jun"),i18n("pickaday.jul"),i18n("pickaday.aug"),i18n("pickaday.sep"),i18n("pickaday.oct"),i18n("pickaday.nov"),i18n("pickaday.dev")],
		weekdays		: [i18n("pickaday.sunday"),i18n("pickaday.monday"),i18n("pickaday.tuesday"),i18n("pickaday.wednesday"),i18n("pickaday.thursday"),i18n("pickaday.friday"),i18n("pickaday.saturday")],
		weekdaysShort	: [i18n("pickaday.sun"),i18n("pickaday.mon"),i18n("pickaday.tue"),i18n("pickaday.wed"),i18n("pickaday.thu"),i18n("pickaday.fri"),i18n("pickaday.sat")]
	};

function i18n(key){
	var lang;
	if(navigator.languages){
		lang = navigator.languages[0] 
	}else{ 
		lang = navigator.language || navigator.userLanguage
	}

	if(lang == "en"){
		return labels_en[key];
	}else if(lang == "es"){
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
