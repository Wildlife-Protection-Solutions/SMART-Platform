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

	var value;
	if(lang == "en"){
		value = labels_en[key];
	}else if(lang == "es"){
		value = labels_es[key];
	}else if(lang == "fr"){
		value = labels_fr[key];
	}else if(lang == "hi"){
		value = labels_hi[key];
	}else if(lang == "in"){
		value = labels_in[key];
	}else if(lang == "km"){
		value = labels_km[key];
	}else if(lang == "lo"){
		value = labels_lo[key];
	}else if(lang == "ms"){
		value = labels_ms[key];
	}else if(lang == "ru"){
		value = labels_ru[key];
	}else if(lang == "th"){
		value = labels_th[key];
	}else if(lang == "vi"){
		value = labels_vi[key];
	}else if(lang == "zh"){
		value = labels_zh[key];
	}
	if (!value){
		value = labels_en[key];
	}
	return value;
}
