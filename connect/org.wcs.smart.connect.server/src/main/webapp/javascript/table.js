function tableCreate(){
	var table = document.createElement("div");
	table.className="smart-table";
	return table;
}

function tableAddHeader(table, headers){
	
	var row = document.createElement("div");
	row.className="table-row smart-table-header";
	table.appendChild(row);
	
	for (var i = 0; i < headers.length; i++){
		var cell = document.createElement("div");
		cell.className="table-cell smart-table-cell";
		if (headers[i] != null && headers[i].length > 0){
			cell.innerHTML=headers[i];
		}
		row.appendChild(cell);
	}
	return row;
}

function tableCreateRow(table, values, rowstyle){
	var row = document.createElement("div");
	row.className="table-row " + rowstyle;
	
	table.appendChild(row);
	
	for (var i = 0; i < values.length; i ++){
		cell = document.createElement("div");
		cell.className="table-cell smart-table-cell";
		if (values[i] != null && values[i].length > 0){
			cell.innerHTML=values[i];
		}
		row.appendChild(cell);
	}
	return row;
}