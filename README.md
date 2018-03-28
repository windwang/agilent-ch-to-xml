# agilent-ch-to-xml

This a program that runs in the Windows command prompt. Its purpose is to scan a user-configured input folder for .pdf and .ch files created by Agilent's ChemStation, rename the .pdf files to whatever the sample's name was, convert the .ch files to a usable .xml file and if it doesn't detect any more new files, it rests for five minutes. After the rest period, it scans the input folder for new files again and the processing begins again. 
