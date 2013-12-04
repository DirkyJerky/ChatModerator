package tc.oc.chatmoderator.filters.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import com.google.common.net.InetAddresses;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.Permission;
import org.joda.time.Instant;

import tc.oc.chatmoderator.PlayerManager;
import tc.oc.chatmoderator.PlayerViolationManager;
import tc.oc.chatmoderator.filters.Filter;
import tc.oc.chatmoderator.violations.Violation;
import tc.oc.chatmoderator.violations.core.ServerIPViolation;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter to check for IP addresses in messages.
 */
public class IPFilter extends Filter {

    private static final Pattern regexPattern = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");

    public IPFilter(PlayerManager playerManager, Permission exemptPermission, int priority) {
        super(playerManager, exemptPermission, priority);
    }

    /**
     * Filters a message. When this happens, violations are dispatched if necessary, and listeners of the violations can
     * modify the message. If the player is exempt (via permissions) from this filter, the message that was passed in is
     * returned instead of the filter processing it.
     *
     * @param message The message that should be instead sent. This may be a modified message, the unchanged message, or
     *                <code>null</code>, if the message is to be cancelled.
     * @param player  The player that sent the message.
     */
    @Nullable
    @Override
    public String filter(String message, final OfflinePlayer player) {
//      if(((Player) player).hasPermission(this.getExemptPermission()))
//          return message;
     
        Matcher matcher = regexPattern.matcher(Preconditions.checkNotNull(message));
        Set<InetAddress> ipAddresses = new HashSet<>();

        PlayerViolationManager violations = this.getPlayerManager().getViolationSet(Preconditions.checkNotNull(player, "Player"));
        Violation violation = new ServerIPViolation(Instant.now(), player, message, violations.getViolationLevel(ServerIPViolation.class), ImmutableSet.copyOf(ipAddresses));               
        
        while (matcher.find()) {
            try {
                ipAddresses.add(InetAddresses.forString(matcher.group()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(violation.isFixed()) {
                message = message.replaceFirst(matcher.group(), ChatColor.MAGIC + matcher.group().substring(0, 7) + ChatColor.RESET);
            }

        }

        if (ipAddresses.size() > 0) {
            violations.addViolation(violation);
        }

        return message;
    }

    public static Pattern getRegexPattern() {
        return regexPattern;
    }
}
