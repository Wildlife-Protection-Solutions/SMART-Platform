#Run me in a Powershell window (that is in the local directory) with the command:  & .\updateNonEnglishFiles.ps1
#I will update the other 11 language files with any new keys in the labels_en.js file, keeping any existing translations in the other language files.


#I need to be in the same directory as all the labels_en.js, labels_fr.js, etc files.
#The files need to be named exactly like that, the engligh file need a "":"" array entry as the last entry (with no comma after it on the last entry)
#All new keys need to go in the _en.js file, if you put new ones straight into fr or other they will be lost through this process.

#to add new languages, just add a line like process_lang("zh"); with your new language code, the script will create a new file with all english in it.
 

function process_lang($lang){

	$lines=Get-Content labels_en.js

	$target=Get-Content ("labels_" + $lang + ".js")
	$keys = @()
	$values = @()

	ForEach ($line in $target){
		If( $line -like "labels_*"){
			#this is the starting line, do nothing.
		}elseif($line -match '\"(.+)\"[ ]*:[ ]*\"(.+)\"'){
			$keys += $matches[1]
			$values += $matches[2]
		}
	}
		



	$outfile =""
	$x=0;
	ForEach ($line in $lines){
		If( $line -like "labels_*"){
			#this is the starting line
			#write out the first line in the target file.
			$outfile += ("labels_" + $lang + "= {`r`n")
		}elseif($line -match '\"(.+)\"[ ]*:[ ]*\"(.+)\"'){
			$sourceKey = $matches[1];
			$sourceValue = $matches[2];
			
			$i=0;
			$found=0;
			foreach($key in $keys){
				if($key -eq $sourceKey){
					#matched the key in the existing file, it already exists, write it out and stop looking.
					$outfile += ("`"" + $sourceKey + "`"" + ":" + "`"" + $values[$i] + "`"" + ",`r`n")
					$found = 1;
					break
				}
				$i++
			}
			if($found -eq 0){
				#no existing value found, use the sourcevalue
				$outfile += ("`"" + $sourceKey + "`"" + ":" + "`"" + $sourceValue  + "`"" + ",`r`n")
			}
		}
		$x++;
	}

	$outfile += "`"`":`"`"`r`n}"

	$outfile | Out-File -Encoding "UTF8" ("labels_" + $lang + ".js")
}



process_lang("es");
process_lang("fr");
process_lang("hi");
process_lang("in");
process_lang("km");
process_lang("lo");
process_lang("ms");
process_lang("ru");
process_lang("th");
process_lang("vi");
process_lang("zh");






