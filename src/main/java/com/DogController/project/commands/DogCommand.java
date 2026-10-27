package com.DogController.project.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import com.DogController.project.managers.DogControlManager;
import com.DogController.project.managers.DogControlManager.Disposition;
import com.DogController.project.managers.DogControlManager.TeleportRequestResult;
import com.DogController.project.managers.DogControlManager.TeleportRequestStatus;
import com.DogController.project.utils.Utils;

public class DogCommand implements CommandExecutor, TabCompleter {
    private static final String PREFIX = "&6[Dogs] &r";
    private static final List<String> DISPOSITION_COMMANDS =
            Arrays.asList("attack", "agro", "aggro", "passive", "neutral");
    private static final List<String> DEFENSE_COMMANDS = Arrays.asList("defense", "defence");
    private static final List<String> ROOT_COMMANDS =
            Arrays.asList("attack", "passive", "neutral", "tp", "sit", "stand", "defense");

    private final DogControlManager dogControlManager;

    public DogCommand(DogControlManager dogControlManager) {
        this.dogControlManager = dogControlManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.colorize(PREFIX + "&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "attack":
            case "agro":
            case "aggro":
                setDisposition(player, args, Disposition.AGGRESSIVE);
                break;
            case "passive":
                setDisposition(player, args, Disposition.PASSIVE);
                break;
            case "neutral":
                setDisposition(player, args, Disposition.NEUTRAL);
                break;
            case "tp":
            case "teleport":
                teleportDogs(player, args);
                break;
            case "sit":
                setDogsSitting(player, args, true);
                break;
            case "stand":
                setDogsSitting(player, args, false);
                break;
            case "defense":
            case "defence":
                setDefense(player, args);
                break;
            default:
                sendUsage(player, label);
                break;
        }
        return true;
    }

    private void setDisposition(Player owner, String[] args, Disposition disposition) {
        if (args.length != 2) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cUsage: /dogs "
                    + disposition.name().toLowerCase(Locale.ROOT) + " <player>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cThat player must be online."));
            return;
        }
        if (disposition == Disposition.AGGRESSIVE && target.equals(owner)) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cYour dogs cannot be ordered to attack you."));
            return;
        }
        dogControlManager.setDisposition(owner, target, disposition);
        String action;
        switch (disposition) {
            case AGGRESSIVE:
                action = "&cattack";
                break;
            case PASSIVE:
                action = "&aignore";
                break;
            case NEUTRAL:
            default:
                action = "&eneutral toward";
                break;
        }
        owner.sendMessage(Utils.colorize(PREFIX + "Your dogs will " + action + " &f" + target.getName() + "&r."));
    }

    private void teleportDogs(Player owner, String[] args) {
        if (args.length != 1) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cUsage: /dogs tp"));
            return;
        }
        TeleportRequestResult result = dogControlManager.teleportDogs(owner,
                teleported -> owner.sendMessage(Utils.colorize(
                        PREFIX + "&aTeleported " + teleported + " dog(s) to you.")));
        switch (result.status()) {
            case QUEUED:
                owner.sendMessage(Utils.colorize(
                        PREFIX + "&eSummon queued. Dogs are processed gradually to reduce server load."));
                break;
            case IN_PROGRESS:
                owner.sendMessage(Utils.colorize(
                        PREFIX + "&eYour dogs are already being summoned."));
                break;
            case COOLDOWN:
                owner.sendMessage(Utils.colorize(PREFIX + "&cPlease wait "
                        + result.cooldownSecondsRemaining() + " second(s) before summoning again."));
                break;
            case NO_DOGS:
                owner.sendMessage(Utils.colorize(PREFIX + "&cNo tracked dogs were found."));
                break;
            default:
                break;
        }
    }

    private void setDogsSitting(Player owner, String[] args, boolean sitting) {
        if (args.length != 1) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cUsage: /dogs " + (sitting ? "sit" : "stand")));
            return;
        }
        owner.sendMessage(Utils.colorize(PREFIX + "&eFinding your dogs..."));
        dogControlManager.setDogsSitting(owner, sitting, affected -> owner.sendMessage(Utils.colorize(
                PREFIX + (sitting ? "&aSitting order applied to " : "&aStanding order applied to ")
                        + affected + " dog(s).")));
    }

    private void setDefense(Player owner, String[] args) {
        if (args.length > 2 || (args.length == 2
                && !args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cUsage: /dogs defense [on|off]"));
            return;
        }

        boolean enabled;
        if (args.length == 1) {
            enabled = dogControlManager.toggleDefense(owner);
        } else {
            enabled = args[1].equalsIgnoreCase("on");
            dogControlManager.setDefenseEnabled(owner, enabled);
        }
        owner.sendMessage(Utils.colorize(PREFIX + (enabled
                ? "&aDefense mode enabled. Your dogs will protect you from nearby players."
                : "&eDefense mode disabled.")));
    }

    private void sendUsage(Player player, String label) {
        player.sendMessage(Utils.colorize(PREFIX + "&eCommands:"));
        player.sendMessage(Utils.colorize("&6/" + label + " attack <player> &7- make your dogs attack a player"));
        player.sendMessage(Utils.colorize("&6/" + label + " passive <player> &7- make your dogs ignore a player"));
        player.sendMessage(Utils.colorize("&6/" + label
                + " neutral <player> &7- clear your dogs' player-specific order"));
        player.sendMessage(Utils.colorize("&6/" + label + " tp &7- teleport your dogs to you"));
        player.sendMessage(Utils.colorize("&6/" + label + " sit &7- make your dogs sit"));
        player.sendMessage(Utils.colorize("&6/" + label + " stand &7- make your dogs stand"));
        player.sendMessage(Utils.colorize("&6/" + label
                + " defense [on|off] &7- protect yourself from nearby players"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(ROOT_COMMANDS, args[0]);
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && DISPOSITION_COMMANDS.contains(subcommand)) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return matching(names, args[1]);
        }
        if (args.length == 2 && DEFENSE_COMMANDS.contains(subcommand)) {
            return matching(Arrays.asList("on", "off"), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> matching(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(value);
            }
        }
        return matches;
    }
}
