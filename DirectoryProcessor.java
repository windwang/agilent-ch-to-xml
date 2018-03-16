package chreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;

public class DirectoryProcessor {

	private static HashMap<String,String> config;
	private static ArrayList<String> processed = new ArrayList<String>();
	
	public static HashMap<String,String> parseConfig( String configPath ) 
	{
		HashMap<String,String> config = new HashMap<String, String>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(configPath))) {
		    String line;
		    while ((line = br.readLine()) != null) {
				if(line.contains("#") || line.length() == 0 || !line.contains("=")) continue;
		    	String[] parts = line.split("=");
		    	String key = parts[0].trim();
		    	String value = parts[1].trim();
		    	config.put(key, value);
		    }
		}
		catch(Exception e) {}
		
		return config;
	}
	
	public static int getMagic(String file)
	{
		byte[] buffer = new byte[4];
		InputStream is;
		try {
			is = new FileInputStream(file);
			if( is.read(buffer) != buffer.length ) 
			{
				is.close();
				return 0;
			}
			is.close();
				
			return ByteBuffer.wrap(buffer).getInt();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}
	
	private static ArrayList<String> directoryRecurse(File path)
	{
		ArrayList<String> paths = new ArrayList<String>();
		//System.out.println("Scanning " + path.getPath());
		for(File f: path.listFiles())
		{
			String p = f.getPath();
			if( f.isDirectory() && !(p.contains("DEFAULT") || p.contains("SNAPSHOT") || p.contains("DEMO"))  ) paths.addAll(directoryRecurse(f));
			else if(p.toLowerCase().contains(".ch") && !processed.contains(p)) paths.add(p);
		}
		
		return paths;
	}
	
	public static ArrayList<String> getUnprocessedSampleFiles(String path)
	{
		return directoryRecurse(new File(path));
	}
	
	public static void main(String[] args) {
		config = parseConfig("config.ini");
		
		while(true)
		{
			ArrayList<String> paths = getUnprocessedSampleFiles(config.get("sourcePath"));
			System.out.printf("Found %d new reports.\n", paths.size());

			for(String s: paths)
				processSampleFile(s);
			
			System.out.println("Sleeping for 5 minutes.");
			try { Thread.sleep(300000); } catch (InterruptedException e) { }			
		}
	}

	private static String cleanName(String n)
	{
		return n.replace("<", ".").replace(">", ".").replace(":", ".").replace("\"", ".").replace("/", ".").replace("\\", ".").replace("|", ".").replace("?", ".").replace("*", "*");
	}
	
	private static void makedir(String path)
	{
		new File(config.get("destPath") + "\\" + path).mkdir();
	}
	
	private static void processSampleFile(String s) {
		int magic = getMagic(s);
		String currentDir = s.substring(0, s.lastIndexOf('\\'));
		
		boolean canReadHeader = (magic == 53555256) || (magic == 53557297) || (magic == 53557049);
		boolean canReadSamples = (magic == 53557049);
		
		String pdfPath = null;
		
		for(File f: new File(currentDir).listFiles())
			if(f.getName().contains(".pdf")) pdfPath = f.getPath();
			
		if(!canReadSamples && pdfPath == null) 
		{
			processed.add(s);
			System.out.println("Skipping " + s);
			return;
		}
		
		String analysisMethod = "", sampleName ="", sampleDate ="";
		
		if( canReadSamples || (canReadHeader && pdfPath != null) )
		{
			SampleGC7890 sample = new SampleGC7890(s);
			
			analysisMethod = cleanName(sample.analysisMethod);
			sampleName = cleanName(sample.sampleName);
			sampleDate = cleanName(sample.sampleDate);
			
			if( canReadSamples )
			{
				//makedir(analysisMethod);
				makedir("\\xml");
				String newPath = String.format("%s\\xml\\%s %s.xml", config.get("destPath"), sampleName, sampleDate);
				System.out.printf("%s -> %s\n", s, newPath);
				sample.writeXml(newPath);
			}
		}
		else if( pdfPath != null && !canReadHeader )
		{
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(currentDir + "\\report00.csv"), "UTF-16"))) {
				String line;
				while ((line = br.readLine()) != null) {
					if(line.contains("Sample Name"))
						sampleName = cleanName(line.split("\"")[3]);
					else if(line.contains("Analysis Method"))
						analysisMethod = cleanName(line.split("\"")[5]);
					else if(line.contains("Injection Date"))
						analysisMethod = cleanName(line.split("\"")[3]);
				}
			}
			catch(Exception e) {}
		}
		
		if( analysisMethod.length() != 0 && sampleName.length() != 0 && sampleDate.length() != 0 && pdfPath != null )
		{
			//makedir(analysisMethod);
			//makedir("\\pdf");
			String newPath = String.format("%s\\%s %s.pdf", config.get("destPath"), sampleName, sampleDate);
			System.out.printf("%s -> %s\n", s, newPath);
			try{Files.copy(Paths.get(pdfPath), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);} catch(Exception e){}

		}
		processed.add(s);
	}
}
