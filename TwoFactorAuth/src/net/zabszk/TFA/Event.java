package net.zabszk.TFA;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Event implements Listener
{
	@EventHandler (priority = EventPriority.HIGH)
	public void onJoin (PlayerJoinEvent e)
	{
		for (int i = 0; (i < Main.getInstance().Tr.size()); i++)
		{
			if ((Main.getInstance().Tr.get(i).target.equalsIgnoreCase(Main.getInstance().func.getIP(e.getPlayer())) && !Main.getInstance().Tr.get(i).IsPlayer))
			{
				if (Main.getInstance().Tr.get(i).counter >= Main.config.getInt("InvalidCodesIpLimit") && !Main.getInstance().Tr.get(i).IsPlayer)
				{
					e.getPlayer().kickPlayer(Main.getInstance().lang("limit-ip").replace("%time", Integer.toString((Main.getInstance().config.getInt("IpCounterResetAfter") - Main.getInstance().Tr.get(i).time))));
					break;
				}
			}
		}
		
		File graczfile = new File("plugins/TwoFactorAuth/players/" + e.getPlayer().getName().toLowerCase() + ".yml");
		FileConfiguration gracz = YamlConfiguration.loadConfiguration(graczfile);
		
		if (graczfile.exists())
		{
			List<String> ips = (List<String>) gracz.get("TrustedIP");
			
			if (ips.contains(getIP(e.getPlayer())))
			{
				Main.getInstance().Authenticated.add(e.getPlayer().getName());
				
				e.getPlayer().sendMessage(Main.getInstance().lang("trusted-ip"));
			}
		}
		else Main.getInstance().Authenticated.add(e.getPlayer().getName());
		
		Main.getInstance().func.SendReminder(e.getPlayer());
		
		List<String> NewUsers = Main.NewUsers;
		
		for (int i = 0; i < NewUsers.size(); i++)
		{
			if (NewUsers.get(i).split(",")[0].equalsIgnoreCase(e.getPlayer().getName())) Main.NewUsers.remove(NewUsers.get(i));
		}
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onLogin (PlayerLoginEvent e)
	{
		for (int i = 0; (i < Main.getInstance().Tr.size()); i++)
		{
			if ((Main.getInstance().Tr.get(i).target.equalsIgnoreCase(e.getPlayer().getName()) && Main.getInstance().Tr.get(i).IsPlayer))
			{
				if (Main.getInstance().Tr.get(i).counter >= Main.config.getInt("InvalidCodesAccountLimit")) e.disallow(Result.KICK_BANNED, Main.getInstance().lang("limit-account").replace("%time", Integer.toString((Main.getInstance().config.getInt("AccountCounterResetAfter") - Main.getInstance().Tr.get(i).time))));
				
				break;
			}
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onQuit (PlayerQuitEvent e)
	{
		if (Main.getInstance().Authenticated.contains(e.getPlayer().getName())) Main.getInstance().Authenticated.remove(e.getPlayer().getName());
		
		List<String> NewUsers = Main.NewUsers;
		
		for (int i = 0; i < NewUsers.size(); i++)
		{
			if (NewUsers.get(i).split(",")[0].equalsIgnoreCase(e.getPlayer().getName())) Main.NewUsers.remove(NewUsers.get(i));
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onCommandPreprocess (PlayerCommandPreprocessEvent e)
	{		
		String command = e.getMessage();
		
		if (command.length() > 1)
		{
			command = command.substring(1);
			
			if (command.contains(" ")) command = command.substring(0, command.indexOf(" "));
		}
		
		if (!(command.equalsIgnoreCase("tfa") || command.equalsIgnoreCase("2fa") || command.equalsIgnoreCase("twofactor")))
		{
			if (IsUnlogged(e.getPlayer(), false))
			{
				FileConfiguration config = Main.getInstance().config;
				
				List<String> cmds = (List<String>) config.getList("AllowedCommands");
				
				Boolean dozwolone = false;
								
				for (int i = 0; i < cmds.size(); i++)
				{
					if (cmds.get(i).equalsIgnoreCase(command)) dozwolone = true;
				}
				
				if (!dozwolone)
				{
					Main.getInstance().func.SendReminder(e.getPlayer());
					e.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onChat (AsyncPlayerChatEvent e)
	{
		if (IsUnlogged(e.getPlayer(), true)) e.setCancelled(true);
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onMove (PlayerMoveEvent e)
	{
		if (IsUnlogged(e.getPlayer(), true)) e.getPlayer().teleport(e.getFrom());
	}
	
	@EventHandler (priority = EventPriority.LOW)
	public void onItemDrop (PlayerDropItemEvent e)
	{
		if (IsUnlogged(e.getPlayer(), true)) e.setCancelled(true);
	}
	
	@EventHandler (priority = EventPriority.LOW)
	public void onItemPickup (PlayerPickupItemEvent e)
	{
		if (IsUnlogged(e.getPlayer(), true)) e.setCancelled(true);
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onBlockBreak (BlockBreakEvent e)
	{
		if (IsUnlogged(e.getPlayer(), true)) e.setCancelled(true);
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onBlockPleace (BlockPlaceEvent e)
	{
		if (IsUnlogged(e.getPlayer(), true)) e.setCancelled(true);
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onInteract (PlayerInteractEvent e)
	{
		if (IsUnlogged(e.getPlayer(), true)) e.setCancelled(true);
	}
	
	public boolean IsUnlogged(Player target, boolean SendReminder)
	{
		return Main.getInstance().IsUnAuthenticated(target, SendReminder);
	}
	
	public String getIP(Player p)
	{
		return p.getAddress().getAddress().toString();
    }
}
