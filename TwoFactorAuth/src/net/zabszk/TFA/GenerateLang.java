package net.zabszk.TFA;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class GenerateLang {
	protected static void GenerateLang(String name)
	{
		File langfile = new File("plugins/TwoFactorAuth/lang/" + name + ".yml");
		if (!langfile.exists())
		{
			try 
			{
				//File file = new File(Main.class.getResource("/resources/" + name + ".yml").getFile());
				JarFile file = new JarFile(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
				ZipEntry entry = file.getEntry("resources/" + name + ".yml");
				InputStream inputStream = file.getInputStream(entry);
				
				
				Files.copy(inputStream, Paths.get("plugins/TwoFactorAuth/lang/" + name + ".yml"));
			    /*BufferedReader br = new BufferedReader(new FileReader(file));
			    BufferedWriter bw = new BufferedWriter(new FileWriter(langfile));
			    String l;
			    while((l=br.readLine())!=null){
			        bw.write(l);
			        bw.newLine();
			    }
			    
			    bw.close();*/
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
