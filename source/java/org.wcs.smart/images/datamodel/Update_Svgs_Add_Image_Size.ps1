#To run this script you have to enable powershell scripting
#Run shell as Administrator - search for "powershell ISE" app, right-click and run as adminstrator
# run these two commands in the command line
# Get-ExecutionPolicy
# Set-ExecutionPolicy remoteSigned

# Cd to the dir this file is in,
# then hit the run green arrow 

# OR call the script in a normal powershell window and not the ISE

# Run this to reset it back to restricted policy:
# Set-ExecutionPolicy restricted


#Run the function on any directory with svgs you want to update, it will update all svgs files in the dir

UpdateAllSvgInDir -dir "./line/"
UpdateAllSvgInDir -dir "./color/"
UpdateAllSvgInDir -dir "./black/"
#UpdateAllSvgInDir -dir "./test/"




#updates all the svg files in a given folder
function UpdateAllSvgInDir {
    Param ([string]$dir)

    $dirlist = Get-ChildItem -File -Path $dir

    foreach ($file in $dirlist){
        [string]$filename = $file
        $i = $filename.IndexOf(".svg")
        $stripped_file = $filename.substring(0,$filename.IndexOf(".svg")) #this will probably fail on capitalized .Svg or >SVG files?
        Write-Output "Updating File:"
        Write-Output $dir $stripped_file
        UpdateSvg -filename $stripped_file -filedir $dir
    }
}

#pass in filename (no extension) and directory, this function will update the SVG file to have a height and width atribute
function UpdateSvg {
 Param ([string]$filename, [string]$filedir)
 
 $n_file = ($filedir + $filename + "-new.svg")
 $filepath = ($filedir + $filename + ".svg")
 
 $o_file = Get-Content -Path $filepath

 foreach ($line in $o_file) {
    $viewbox_index = $line.IndexOf("viewBox=")
    if($viewbox_index -ne -1){

        $end_index = $viewbox_index + 8 + 14 #this won't work when the "0 0 128 128" part is longer or shorter than 14 chars.


        if( $end_index -gt $line.length){
            $end_index = $line.length
        }
        $before_text = $line.substring(0,$end_index)  
        $after_text = $line.substring($end_index)

        $start_number = (13 + $viewbox_index)
        $numbers = $line.substring( $start_number, 8)
     
        $firstspace = $numbers.IndexOf(" ", 0)
        $closequote= $numbers.IndexOf('"', $firstspace + 1)

        $width = $numbers.substring(0, $firstspace)
        $height = $numbers.substring(($firstspace + 1), ($closequote - $firstspace -1))
        $size_text = 'width="' + $width + '" height="' + $height + '"'
    
        Add-Content $n_file $before_text
        Add-Content $n_file $size_text
        Add-Content $n_file $after_text
    }else{
        Add-Content $n_file $line
    }
 }

 $nameonly = $filename + ".svg"
 Remove-Item $filepath
 Rename-Item $n_file $nameonly

 
}