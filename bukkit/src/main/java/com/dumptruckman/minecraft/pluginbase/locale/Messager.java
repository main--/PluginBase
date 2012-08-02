/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.minecraft.pluginbase.locale;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * This interface describes a Messager which sends messages to CommandSenders.
 */
public interface Messager extends MessageProvider {

    /**
     * Sends a message to the specified player with the generic ERROR prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     * @param args    arguments for String.format().
     */
    void bad(Message message, CommandSender sender, Object... args);

    /**
     * Sends a message to the specified player with the generic ERROR prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     */
    void bad(BundledMessage message, CommandSender sender);

    /**
     * Sends a message to the specified player with NO special prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     * @param args    arguments for String.format().
     */
    void normal(Message message, CommandSender sender, Object... args);

    /**
     * Sends a message to the specified player with NO special prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     */
    void normal(BundledMessage message, CommandSender sender);

    /**
     * Sends a message to the specified player with the generic SUCCESS prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     * @param args    arguments for String.format().
     */
    void good(Message message, CommandSender sender, Object... args);

    /**
     * Sends a message to the specified player with the generic SUCCESS prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     */
    void good(BundledMessage message, CommandSender sender);

    /**
     * Sends a message to the specified player with the generic INFO prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     * @param args    arguments for String.format().
     */
    void info(Message message, CommandSender sender, Object... args);

    /**
     * Sends a message to the specified player with the generic INFO prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     */
    void info(BundledMessage message, CommandSender sender);

    /**
     * Sends a message to the specified player with the generic HELP prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     * @param args    arguments for String.format().
     */
    void help(Message message, CommandSender sender, Object... args);

    /**
     * Sends a message to the specified player with the generic HELP prefix.
     *
     * @param message The message to send.
     * @param sender  The entity to send the messages to.
     */
    void help(BundledMessage message, CommandSender sender);

    /**
     * Sends a message to a player that automatically takes words that are too long and puts them on a new line.
     *
     * @param player  Player to send message to.
     * @param message Message to send.
     */
    void sendMessage(CommandSender player, String message);

    /**
     * Sends a message to a player that automatically takes words that are too long and puts them on a new line.
     *
     * @param player   Player to send message to.
     * @param messages Messages to send.
     */
    void sendMessages(CommandSender player, List<String> messages);
}

