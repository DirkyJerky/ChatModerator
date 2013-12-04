package tc.oc.chatmoderator.filters.core;

import com.google.common.base.Preconditions;
import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.Permission;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.chatmoderator.PlayerManager;
import tc.oc.chatmoderator.PlayerViolationManager;
import tc.oc.chatmoderator.filters.Filter;
import tc.oc.chatmoderator.violations.core.DuplicateMessageViolation;

import javax.annotation.Nullable;

public class DuplicateMessageFilter extends Filter {

    /**
     * The delay in milliseconds between two messages before they are allowed to be sent.
     */
    private long delay;

    public DuplicateMessageFilter(PlayerManager playerManager, Permission exemptPermission, long delay, int priority) {
        super(playerManager, exemptPermission, priority);

        Preconditions.checkArgument(delay > 0, "Delay must be greater than 0...");
        this.delay = delay;
    }

    /**
     * Filters a message to make sure that it was not sent too soon before the last message.
     *
     * @param message The message that should be instead sent. This may be a modified message, the unchanged message, or
     *                <code>null</code>, if the message is to be cancelled.
     * @param player  The player that sent the message.
     * @return
     */
    @Override
    public @Nullable String filter(String message, OfflinePlayer player) {
        PlayerViolationManager violationSet = this.getPlayerManager().getViolationSet(player);

        Instant now = Instant.now();

        Instant lastMessage = violationSet.getLastMessageTime();
        Duration difference = new Duration(lastMessage, now);

        if(lastMessage == null) {
            violationSet.setLastMessageTime(now);
            return message;
        }

        if (lastMessage.withDurationAdded(delay, 1).isAfter(now)) {
            violationSet.addViolation(new DuplicateMessageViolation(Instant.now(), player, message, difference));
            message = null;
        } else {
            violationSet.setLastMessageTime(now);
        }

        return message;
    }

}
