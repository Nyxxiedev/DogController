package com.DogController.project.commands;

import java.util.ArrayList;
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
import com.DogController.project.utils.Utils;

public class DogCommand implements CommandExecutor, TabCompleter {
    private static final String PREFIX = "&6[Dogs] &r";
    private final DogControlManager dogControlManager;

    public DogCommand(DogControlManager dogControlManager) {
        this.dogControlManager = dogControlManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Utils.colorize(PREFIX + "&cOnly players can use this command."));
            return true;
        }
        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "attack", "agro", "aggro" -> setDisposition(player, args, Disposition.AGGRESSIVE);
            case "passive" -> setDisposition(player, args, Disposition.PASSIVE);
            case "neutral" -> setDisposition(player, args, Disposition.NEUTRAL);
            case "tp", "teleport" -> teleportDogs(player, args);
            case "sit" -> setDogsSitting(player, args, true);
            case "stand" -> setDogsSitting(player, args, false);
            case "defense", "defence" -> setDefense(player, args);
            default -> sendUsage(player, label);
        }
        return true;
    }

    private void setDisposition(Player owner, String[] args, Disposition disposition) {
        if (args.length != 2) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cUsage: /dogs " + disposition.name().toLowerCase(Locale.ROOT) + " <player>"));
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
        String action = switch (disposition) {
            case AGGRESSIVE -> "&cattack";
            case PASSIVE -> "&aignore";
            case NEUTRAL -> "&eneutral toward";
        };
        owner.sendMessage(Utils.colorize(PREFIX + "Your dogs will " + action + " &f" + target.getName() + "&r."));
    }

    private void teleportDogs(Player owner, String[] args) {
        if (args.length != 1) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cUsage: /dogs tp"));
            return;
        }
        owner.sendMessage(Utils.colorize(PREFIX + "&eFinding your dogs and teleporting them safely..."));
        dogControlManager.teleportDogs(owner, teleported -> owner.sendMessage(Utils.colorize(
                PREFIX + "&aTeleported " + teleported + " dog(s) to you.")));
    }

    private void setDogsSitting(Player owner, String[] args, boolean sitting) {
        if (args.length != 1) {
            owner.sendMessage(Utils.colorize(PREFIX + "&cUsage: /dogs " + (sitting ? "sit" : "stand")));
            return;
        }
        owner.sendMessage(Utils.colorize(PREFIX + "&eFinding your dogs..."));
        dogControlManager.setDogsSitting(owner, sitting, affected -> owner.sendMessage(Utils.colorize(
                PREFIX + (sitting ? "&aYour dogs are sitting." : "&aYour dogs are standing.")
                        + " &7(" + affected + " dog(s))")));
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
        player.sendMessage(Utils.colorize("&6/" + label + " neutral <player> &7- clear your dogs' player-specific order"));
        player.sendMessage(Utils.colorize("&6/" + label + " tp &7- teleport your dogs to you"));
        player.sendMessage(Utils.colorize("&6/" + label + " sit &7- make your dogs sit"));
        player.sendMessage(Utils.colorize("&6/" + label + " stand &7- make your dogs stand"));
        player.sendMessage(Utils.colorize("&6/" + label + " defense [on|off] &7- protect yourself from nearby players"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(List.of("attack", "passive", "neutral", "tp", "sit", "stand", "defense"), args[0]);
        }
        if (args.length == 2 && List.of("attack", "agro", "aggro", "passive", "neutral").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return matching(names, args[1]);
        }
        if (args.length == 2 && List.of("defense", "defence").contains(args[0].toLowerCase(Locale.ROOT))) {
            return matching(List.of("on", "off"), args[1]);
        }
        return List.of();
    }

    private List<String> matching(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
