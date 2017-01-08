package net.zabszk.TFA;

import java.io.File;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Functions {
	public Player[] getOnline()
    {
        try
        {
            Method method = Bukkit.class.getMethod("getOnlinePlayers");
            Object players = method.invoke(null);
            
            if (players instanceof Player[]) {
                Player[] oldPlayers = (Player[]) players;
                return oldPlayers;
            }
            else
            {
                Collection<Player> newPlayers = (Collection<Player>) players;
                
                Player[] online = new Player[newPlayers.size()];
                
                Object[] obj = newPlayers.toArray();
                
                int counter = 0;
                
                for (int i = 0; i < obj.length; i++)
                {
                	if (obj[i] instanceof Player)
                	{
                		String name = obj[i].toString().substring(obj[i].toString().indexOf("{"));
                		name = name.replace("{name=", "");
                		name = name.substring(0, name.length() - 1);
                		
                		online[counter] = Bukkit.getPlayer(name);
                		counter = counter + 1;
                	}
                }
                return online;
            }
         
        } 
        catch (Exception e)
        {
            System.out.println("Player online ERROR");
            System.out.println(e.toString());
            e.printStackTrace();
            
            return null;
        }
	}
	
	public String hash(String base)
	{
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++)
            {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

           return hexString.toString();
        }
        catch(Exception ex)
        {
        	throw new RuntimeException(ex);
        }
	}
	
	public String getIP(Player p)
	{
		return p.getAddress().getAddress().toString();
    }
	
	protected void KickIP(String IP)
	{
		Player[] online = getOnline();
		
		for (int i = 0; i < online.length; i++)
		{
			if (getIP(online[i]).equalsIgnoreCase(IP)) online[i].kickPlayer(Main.getInstance().lang("limit-ip-kick"));
		}
	}
	
	public void SendReminder(Player target)
	{
		if (!Main.getInstance().Authenticated.contains(target.getName()))
		{
			File graczfile = new File("plugins/TwoFactorAuth/players/" + target.getName().toLowerCase() + ".yml");
			FileConfiguration gracz = YamlConfiguration.loadConfiguration(graczfile);
			
			if (graczfile.exists())
			{
				List<String> ips = (List<String>) gracz.get("TrustedIP");
				
				if (ips.contains(getIP(target)))
				{
					Main.getInstance().Authenticated.add(target.getName());
					
					target.sendMessage(Main.getInstance().lang("trusted-ip"));
				}
				else
				{
					target.sendMessage(Main.getInstance().lang("auth-req"));
					target.sendMessage(Main.getInstance().lang("auth-req2"));
				}
			} else {
				Main.getInstance().Authenticated.add(target.getName());
			}
		}
	}
	
	public String random(int lenght)
	{
		char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < lenght; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		String output = sb.toString().toUpperCase();
		
		return output;
	}
	
	public void AddTry(Player target)
	{
		Boolean foundp = false;
		Boolean foundi = false;
		
		for (int i = 0; i < Main.getInstance().Tr.size(); i++)
		{
			if ((Main.getInstance().Tr.get(i).target.equalsIgnoreCase(target.getName()) && Main.getInstance().Tr.get(i).IsPlayer) || (Main.getInstance().Tr.get(i).target.equalsIgnoreCase(Main.getInstance().func.getIP(target)) && !Main.getInstance().Tr.get(i).IsPlayer))
			{
				Tries tr = Main.getInstance().Tr.get(i);
				tr.counter ++;
				tr.time = 0;
				
				Main.getInstance().Tr.set(i, tr);
				
				if (tr.IsPlayer) foundp = true;
				else foundi = true;

				if (tr.counter >= Main.config.getInt("InvalidCodesAccountLimit") && tr.IsPlayer) Bukkit.getPlayer(tr.target).kickPlayer(Main.getInstance().lang("limit-account-kick"));
				else if (tr.counter >= Main.config.getInt("InvalidCodesIpLimit") && !tr.IsPlayer) KickIP(tr.target);
				
				if (foundi && foundp) break;
			}
		}
		
		if (!foundp)
		{
			Tries tr = new Tries();
			
			tr.target = target.getName();
			tr.IsPlayer = true;
			tr.time = 0;
			tr.counter = 1;
			
			Main.getInstance().Tr.add(tr);
		}
		
		if (!foundi)
		{
			Tries tr = new Tries();
			
			tr = new Tries();
			tr.target = getIP(target);
			tr.IsPlayer = false;
			tr.time = 0;
			tr.counter = 1;
			
			Main.getInstance().Tr.add(tr);
		}
	}
	
	protected void doTimer()
	{
		List<Tries> Trr = Main.getInstance().Tr;
		
		for (int i = 0; i < Main.getInstance().Tr.size(); i++)
		{
			Tries tr = Main.getInstance().Tr.get(i);
			tr.time ++;
			
			if (tr.IsPlayer && tr.time >= Main.getInstance().config.getInt("AccountCounterResetAfter")) Trr.remove(i);
			else if (!tr.IsPlayer && tr.time >= Main.getInstance().config.getInt("IpCounterResetAfter")) Trr.remove(i);
			else Trr.set(i, tr);
		}
		
		Main.getInstance().Tr = Trr;
	}
}
