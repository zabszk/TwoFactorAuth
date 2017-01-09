package net.zabszk.TFA;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
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
				JarFile file = new JarFile(URLDecoder.decode(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()));
				ZipEntry entry = file.getEntry("resources/" + name + ".yml");
				InputStream inputStream = file.getInputStream(entry);
				
				Files.copy(inputStream, Paths.get("plugins/TwoFactorAuth/lang/" + name + ".yml"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
