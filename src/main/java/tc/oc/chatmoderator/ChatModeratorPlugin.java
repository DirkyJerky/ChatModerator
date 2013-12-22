package tc.oc.chatmoderator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import tc.oc.chatmoderator.config.FilterConfiguration;
import tc.oc.chatmoderator.config.ZoneConfiguration;
import tc.oc.chatmoderator.filters.core.*;
import tc.oc.chatmoderator.listeners.ChatModeratorListener;
import tc.oc.chatmoderator.listeners.DebugListener;
import tc.oc.chatmoderator.zones.ZoneType;

import java.util.HashSet;
import java.util.Set;

/**
 * Plugin class.
 */
public class ChatModeratorPlugin extends JavaPlugin {
    private boolean debugEnabled;
    private Set<Listener> listeners;
    private PlayerManager playerManager;
    private Configuration configuration;

    /**
     * Gets whether or not debug mode is enabled.
     *
     * @return Whether or not debug mode is enabled.
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void onDisable() {
        for (Listener listener : this.listeners) {
            if (listener instanceof ChatModeratorListener) {
                ((ChatModeratorListener) listener).unRegisterAll();
            }
            HandlerList.unregisterAll(listener);
        }
        this.playerManager = null;
        this.configuration = null;
    }

    @Override
    public void onEnable() {
       
        // Set up configuration, copy defaults, etc etc
        this.saveDefaultConfig();
        this.reloadConfig();
        this.configuration = this.getConfig();

        // Set up the listeners and player manager
        this.listeners = new HashSet<>();
        this.playerManager = new PlayerManager(this);
        
        // Add debug options
        this.debugEnabled = this.configuration.getBoolean("debug.enabled", false);
        if (this.debugEnabled) {
            this.listeners.add(new DebugListener());
        }
        
        // Initialize the listener, add filters as necessary
        ChatModeratorListener moderatorListener = new ChatModeratorListener(this);
        setUpFilters(moderatorListener);
        setUpZones(moderatorListener);

        this.listeners.add(moderatorListener);

        // And register all the events.
        for (Listener listener : this.listeners) {
            Bukkit.getPluginManager().registerEvents(listener, this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.hasPermission("chatmoderator.reload")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        this.onDisable();
        this.onEnable();

        sender.sendMessage(ChatColor.AQUA + "[ChatModerator] - " + ChatColor.DARK_GREEN + "Successfully reloaded config and registered all filters.");

        return true;
    }

    /**
     * Sets up all the filters in the CMListener.
     *
     * Lower priorities gets run first.
     */
    private void setUpFilters(ChatModeratorListener moderatorListener) {
        moderatorListener.registerFilter(new DuplicateMessageFilter(this.getPlayerManager(), new Permission("chatmoderator.filters.duplicatemessage.exempt"), getConfig().getLong("config.delay-between-messages"), getConfig().getInt("filters.duplicate-messages.priority")));
        moderatorListener.registerFilter(new IPFilter(this.getPlayerManager(), new Permission("chatmoderator.filters.ipfilter.exempt"), getConfig().getInt("filters.server-ip.priority")));
        moderatorListener.registerFilter(new ProfanityFilter(this.getPlayerManager(), new Permission("chatmoderator.filters.profanity.exempt"), (new FilterConfiguration(this, "filters.profanity.expressions")).build().getWeights(), getConfig().getInt("filters.profanity.priority")));
        moderatorListener.registerFilter(new AllCapsFilter(this.getPlayerManager(), new Permission("chatmoderator.filters.all-caps.exempt"), getConfig().getInt("filters.all-caps.priority")));
        moderatorListener.registerFilter(new RepeatedCharactersFilter(this.getPlayerManager(), new Permission("chatmoderator.filters.repeated.exempt"), getConfig().getInt("filters.repeated-characters.count"), getConfig().getInt("filters.repeated-characters.priority")));
    }

    /**
     * Sets up all the zones for the ChatModeratorListener.
     *
     * @param moderatorListener The {@link tc.oc.chatmoderator.listeners.ChatModeratorListener} to work off of.
     */
    private void setUpZones(ChatModeratorListener moderatorListener) {
        moderatorListener.registerZone(ZoneType.CHAT, new ZoneConfiguration(this, "zones.chat").parse().getZone());
        moderatorListener.registerZone(ZoneType.SIGN, new ZoneConfiguration(this, "zones.sign").parse().getZone());
        moderatorListener.registerZone(ZoneType.ANVIL, new ZoneConfiguration(this, "zones.anvil").parse().getZone());
    }

    /**
     * Gets the player manager.
     *
     * @return The player manager.
     */
    public final PlayerManager getPlayerManager() {
        return this.playerManager;
    }
}
