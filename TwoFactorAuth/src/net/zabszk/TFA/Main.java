package net.zabszk.TFA;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mcstats.Metrics;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;


public class Main extends JavaPlugin
{
	public static Plugin plugin;
	protected static FileConfiguration config;
	private static File langfile;
	protected static FileConfiguration langconf;
	
	private static Main instance;
	
	protected static Event event;
	public static Functions func;
	
	protected static List<String> Authenticated;
	protected static List<String> NewUsers;
	protected static List<Tries> Tr;
	
	@Override
	public void onEnable()
	{
		plugin = this;
		instance = this;
		
		Authenticated = new ArrayList();
		NewUsers = new ArrayList();
		Tr = new ArrayList();
		
		config = getConfig();
		
		config.addDefault("AllowedCommands", new String[] {"login", "l", "register", "captcha"});
		config.addDefault("ServerName", "Minecraft Server");
		config.addDefault("Lang", "en");
		
		config.addDefault("InvalidCodesIpLimit", 5);
		config.addDefault("InvalidCodesAccountLimit", 3);
		
		config.addDefault("IpCounterResetAfter", 5);
		config.addDefault("AccountCounterResetAfter", 2);
		config.addDefault("AllowMetrics", true);
		
		config.options().copyDefaults(true);
		saveConfig();
		
		if (config.getBoolean("AllowMetrics"))
		{
			try {
	            Metrics metrics = new Metrics(this);
	            metrics.start();
	        } catch (Exception e) {
	            System.out.println("Metrics error!");
	            e.printStackTrace();
	        }
		}
		
		File path = new File("plugins/TwoFactorAuth/lang/");
		
		try
		{
			if (!path.exists()) path.mkdir();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		langfile = new File("plugins/TwoFactorAuth/lang/" + config.getString("Lang") + ".yml");
		langconf = YamlConfiguration.loadConfiguration(langfile);
		
		event = new Event();
		func = new Functions();
		
		getServer().getPluginManager().registerEvents(event, this);
		
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
	     
        scheduler.scheduleSyncRepeatingTask(this, new Runnable()
        {
            @Override
            public void run()
            {
            	func.doTimer();
            }
        }, 0L, 1200L);
		
		System.out.println("[TwoFactorAuth] Plugin enabled!");
	}
	
	@Override
	public void onDisable()
	{
		Authenticated.clear();
		Authenticated = new ArrayList();
		
		NewUsers.clear();
		NewUsers = new ArrayList();
		
		Tr.clear();
		Tr = new ArrayList();
		
		System.out.println("[TwoFactorAuth] Plugin disabled!");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("tfa"))
		{
			if (sender instanceof Player)
			{
				if (args.length == 0)
				{
					if (IsAuthenticated((Player) sender, true))
					{
						File graczfile = new File("plugins/TwoFactorAuth/players/" + sender.getName().toLowerCase() + ".yml");
						FileConfiguration gracz = YamlConfiguration.loadConfiguration(graczfile);
						
						sender.sendMessage(ChatColor.GREEN + "/tfa info");
						
						if (graczfile.exists())
						{
							sender.sendMessage(ChatColor.GREEN + "/tfa disable");
							sender.sendMessage(ChatColor.GREEN + "/tfa trust");
							sender.sendMessage(ChatColor.GREEN + "/tfa distrust");
						}
						else sender.sendMessage(ChatColor.GREEN + "/tfa enable");
						
						if (sender.isOp() || sender.hasPermission("tfa.reload")) sender.sendMessage(ChatColor.GOLD + "/tfa reload");
						
						sender.sendMessage("");
						sender.sendMessage(ChatColor.DARK_AQUA + "TwoFactorAuth, ver. 1.0");
					}
				}
				else if (args.length == 1 && IsUnAuthenticated((Player) sender, false))
				{
					File graczfile = new File("plugins/TwoFactorAuth/players/" + sender.getName().toLowerCase() + ".yml");
					FileConfiguration gracz = YamlConfiguration.loadConfiguration(graczfile);
					
					if (graczfile.exists())
					{
						Boolean passed = false;
						
						GoogleAuthenticator gAuth = new GoogleAuthenticator();
						
						try
						{
							passed = gAuth.authorize(gracz.getString("Secret"), Integer.valueOf(args[0]));
						}
						catch (Exception ex) {}
						
						if (passed)
						{
							Authenticated.add(sender.getName());
							
							sender.sendMessage(lang("auth-complete"));
						}
						else if (args[0].equalsIgnoreCase(gracz.getString("BackupCode")) || func.hash(args[0]).equalsIgnoreCase(gracz.getString("BackupCode")))
						{
							graczfile.delete();
							
							sender.sendMessage(lang("auth-backup"));
						}
						else
						{
							func.AddTry((Player) sender);
							
							sender.sendMessage(lang("auth-error"));
						}
					}
					else Authenticated.add(sender.getName());
				}
				else
				{
					File graczfile = new File("plugins/TwoFactorAuth/players/" + sender.getName().toLowerCase() + ".yml");
					FileConfiguration gracz = YamlConfiguration.loadConfiguration(graczfile);
					
					if (args[0].equalsIgnoreCase("enable"))
					{
						if (graczfile.exists()) sender.sendMessage(lang("enable-err-enabled"));
						else
						{
							if (args.length == 1)
							{
								String[] user = null;
								
								for (int i = 0; i < NewUsers.size(); i++)
								{
									if (NewUsers.get(i).split(",")[0].equalsIgnoreCase(sender.getName())) user = NewUsers.get(i).split(",");
								}
								
								GoogleAuthenticator gAuth = new GoogleAuthenticator();
								final GoogleAuthenticatorKey key = gAuth.createCredentials();
								
								String secret = "";
								String kod = "";
								
								if (user == null) secret = key.getKey();
								else secret = user[1];
								
								if (user == null) kod = func.random(12);
								else kod = user[2];
								
								sender.sendMessage("");
								sender.sendMessage(lang("enable-activation"));
								sender.sendMessage(lang("enable-secretdata"));
								sender.sendMessage("");
								sender.sendMessage(lang("enable-app"));
								sender.sendMessage(lang("enable-app2"));
								sender.sendMessage(lang("enable-app3"));
								sender.sendMessage(lang("enable-app4"));
								sender.sendMessage(lang("enable-app5"));
								sender.sendMessage("");
								sender.sendMessage(lang("enable-secretdata"));
								sender.sendMessage(lang("enable-data"));
								sender.sendMessage(lang("enable-data-secret").replace("%secret", secret));
								sender.sendMessage(lang("enable-data-qr").replace("%qr", key.getQRBarcodeURL(sender.getName(), config.getString("ServerName"), secret).replace(" ", "_")));
								sender.sendMessage("");
								sender.sendMessage(lang("enable-backup"));
								sender.sendMessage(lang("enable-backup-code").replace("%code", kod));
								sender.sendMessage("");
								sender.sendMessage(lang("enable-secretdata"));
								sender.sendMessage("");
								sender.sendMessage(lang("enable-tocomplete"));
								sender.sendMessage(lang("enable-tocomplete2"));
								sender.sendMessage("");
								sender.sendMessage(lang("enable-activation"));
								sender.sendMessage("");
								
								if (user == null) NewUsers.add(sender.getName() + "," + key.getKey() + "," + kod);
							}
							else if (args.length == 2)
							{
								String[] user = null;
								
								for (int i = 0; i < NewUsers.size(); i++)
								{
									if (NewUsers.get(i).split(",")[0].equalsIgnoreCase(sender.getName())) user = NewUsers.get(i).split(",");
								}
									
								if (user == null) sender.sendMessage(lang("enable-err-startfirst"));
								else
								{
									sender.sendMessage("");
									
									Boolean passed = false;
									
									GoogleAuthenticator gAuth = new GoogleAuthenticator();
									
									try
									{
										passed = gAuth.authorize(user[1], Integer.valueOf(args[1]));
									}
									catch (Exception ex)
									{
										sender.sendMessage(lang("enable-err-only-digits"));
									}
									
									if (passed)
									{
										sender.sendMessage(lang("enable-enabling"));
										
										try
										{
											graczfile.createNewFile();
										}
										catch (Exception ex) {}
										
										gracz.set("Secret", user[1]);
										gracz.set("BackupCode", func.hash(user[2]));
										gracz.set("TrustedIP", new ArrayList());
										
										try
										{
											gracz.save(graczfile);
										}
										catch (Exception ex) {}
										
										sender.sendMessage(lang("enable-enabled"));
									}
									else sender.sendMessage(lang("enable-err-token"));
									
									sender.sendMessage("");
								}
							}
						}
					}
					else if (args[0].equalsIgnoreCase("disable"))
					{
						if (args.length == 2)
						{
							if (graczfile.exists())
							{
								Boolean passed = false;
								
								GoogleAuthenticator gAuth = new GoogleAuthenticator();
								
								try
								{
									passed = gAuth.authorize(gracz.getString("Secret"), Integer.valueOf(args[1]));
								}
								catch (Exception ex) {}
								
								if (passed || args[1].equalsIgnoreCase(gracz.getString("BackupCode")) || func.hash(args[1]).equalsIgnoreCase(gracz.getString("BackupCode")))
								{
									graczfile.delete();
									
									sender.sendMessage(lang("disable-disabled"));
								}
								else
								{
									func.AddTry((Player) sender);
									
									sender.sendMessage(lang("disable-err-token"));
								}
							}
							else sender.sendMessage(lang("not-enabled"));
						}
						else sender.sendMessage(lang("disable-syntax"));
					}
					else if (args[0].equalsIgnoreCase("trust") && IsAuthenticated((Player) sender, true))
					{
						if (args.length != 2) sender.sendMessage(lang("trust-syntax"));
						else
						{
							if (graczfile.exists())
							{
								String ip = args[1];
								if (!ip.startsWith("/")) ip = "/" + ip;
								
								List<String> ips = gracz.getStringList("TrustedIP");
								
								ips.add(ip);
								
								gracz.set("TrustedIP", ips);
								
								try
								{
									gracz.save(graczfile);
								}
								catch (Exception ex) {}
								
								sender.sendMessage(lang("trust-done"));
							}
							else sender.sendMessage(lang("not-enabled"));
						}
					}
					else if (args[0].equalsIgnoreCase("distrust") && IsAuthenticated((Player) sender, true))
					{
						if (args.length != 2) sender.sendMessage(lang("distrust-syntax"));
						else
						{
							if (graczfile.exists())
							{
								String ip = args[1];
								if (!ip.startsWith("/")) ip = "/" + ip;
								
								List<String> ips = gracz.getStringList("TrustedIP");
								
								ips.remove(ip);
								
								gracz.set("TrustedIP", ips);
								
								try
								{
									gracz.save(graczfile);
								}
								catch (Exception ex) {}
								
								sender.sendMessage(lang("distrust-done"));
							}
							else sender.sendMessage(lang("not-enabled"));
						}
					}
					else if (args[0].equalsIgnoreCase("info") && IsAuthenticated((Player) sender, true))
					{
						if (graczfile.exists())
						{
							List<String> ips = gracz.getStringList("TrustedIP");
							
							sender.sendMessage(lang("info-status").replace("%status", lang("info-enabled")));
							
							if (ips.size() == 0) sender.sendMessage(lang("info-trusted-IPs") + " " + lang("none"));
							else
							{
								sender.sendMessage(lang("info-trusted-IPs"));
								
								for (int i = 0; i < ips.size(); i++)
								{
									sender.sendMessage(ChatColor.DARK_AQUA + "> " + ChatColor.GRAY + "- " + ips.get(i));
								}
							}
						}
						else sender.sendMessage(lang("info-status").replace("%status", lang("info-disabled")));
					}
					else if (args[0].equalsIgnoreCase("reload") && (sender.isOp() || sender.hasPermission("tfa.reload")))
					{
						reloadConfig();
						config = getConfig();
						
						langfile = new File("plugins/TwoFactorAuth/lang/" + config.getString("Lang") + ".yml");
						langconf = YamlConfiguration.loadConfiguration(langfile);
						
						sender.sendMessage(ChatColor.GREEN + "[TwoFactorAuth] Config reloaded.");
					}
					else sender.sendMessage(lang("unknown-subcommand"));
				}
			}
			else if (sender == Bukkit.getConsoleSender())
			{
				if (args.length == 0)
				{
					sender.sendMessage(ChatColor.RED + "/tfa reset <nick> [disables 2FA]");
					sender.sendMessage(ChatColor.RED + "/tfa unlock <nick/IP> [resets invalid tokens counter]");
					sender.sendMessage(ChatColor.RED + "/tfa unlockall [resets all invalid tokens counters]");
					sender.sendMessage(ChatColor.RED + "/tfa show [show current counters state]");
					sender.sendMessage(ChatColor.RED + "/tfa reload");
				}
				else
				{
					if (args[0].equalsIgnoreCase("reset"))
					{
						if (args.length == 1) sender.sendMessage(ChatColor.DARK_RED + "> " + ChatColor.GRAY + "Syntax: /tfa reset <nick>");
						else
						{
							File graczfile = new File("plugins/TwoFactorAuth/players/" + args[1] + ".yml");
							
							if (graczfile.exists())
							{
								graczfile.delete();
								sender.sendMessage(lang("admin-reset-done"));
							}
							else sender.sendMessage(lang("admin-reset-fail"));
						}
					}
					else if (args[0].equalsIgnoreCase("unlock"))
					{
						if (args.length == 1) sender.sendMessage(ChatColor.DARK_RED + "> " + ChatColor.GRAY + "Syntax: /tfa unlock <nick/IP>");
						else
						{
							Boolean found = false;
							
							for (int i = 0; (i < Main.getInstance().Tr.size()); i++)
							{
								if (Tr.get(i).target.equalsIgnoreCase(args[1]))
								{
									Tr.remove(i);
									found = true;
									break;
								}
							}
							
							if (found) sender.sendMessage(lang("admin-unlock-done"));
							else sender.sendMessage(lang("admin-unlock-fail"));
						}
					}
					else if (args[0].equalsIgnoreCase("unlockall"))
					{
						Tr.clear();
						Tr = new ArrayList();
						
						sender.sendMessage(lang("admin-unlock-all"));
					}
					else if (args[0].equalsIgnoreCase("show"))
					{
						sender.sendMessage(lang("admin-show-current"));
						
						for (int i = 0; (i < Tr.size()); i++)
						{
							String in = String.valueOf(i);
							if (in.length() < 2) in = "0" + i;
							
							if (Tr.get(i).IsPlayer) sender.sendMessage(ChatColor.GRAY + in + ". " + Tr.get(i).target + " -  " + Tr.get(i).counter + " tries, expires in " + (config.getInt("AccountCounterResetAfter") - Tr.get(i).time) + " minute(s)");
							else sender.sendMessage(ChatColor.GRAY + in + ". " + Tr.get(i).target + " -  " + Tr.get(i).counter + " tries, expires in " + (config.getInt("IpCounterResetAfter") - Tr.get(i).time) + " minute(s)");
						}
					}
					else if (args[0].equalsIgnoreCase("reload"))
					{
						reloadConfig();
						config = getConfig();
						
						langfile = new File("plugins/TwoFactorAuth/lang/" + config.getString("Lang") + ".yml");
						langconf = YamlConfiguration.loadConfiguration(langfile);
						
						sender.sendMessage(ChatColor.GREEN + "[TwoFactorAuth] Config reloaded.");
					}
					else sender.sendMessage(lang("unknown-subcommand"));
				}
			}
			else sender.sendMessage(lang("wrong-level"));
		}
		
		return true;
	}
	
	public boolean IsUnAuthenticated(Player target, boolean SendReminder)
	{
		return !IsAuthenticated(target, SendReminder);
	}
	
	public boolean IsAuthenticated(Player target, boolean SendReminder)
	{
		if (Authenticated.contains(target.getName())) return true;
		else
		{
			if (SendReminder) func.SendReminder(target);
			return false;
		}
	}
	
	public String lang(String text)
	{
		try
		{
			return ChatColor.translateAlternateColorCodes('&', langconf.getString(text));
		}
		catch (Exception e)
		{
			return ChatColor.RED + "Translation " + text + " not found in /plugins/TwoFactorAuth/lang/" + config.getString("Lang") + ".yml";
		}
	}
	
	public static Main getInstance()
    {	 
        return instance;
    }
}
