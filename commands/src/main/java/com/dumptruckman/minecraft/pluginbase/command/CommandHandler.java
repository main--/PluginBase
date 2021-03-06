package com.dumptruckman.minecraft.pluginbase.command;

import com.dumptruckman.minecraft.pluginbase.command.builtin.BuiltInCommand;
import com.dumptruckman.minecraft.pluginbase.logging.PluginLogger;
import com.dumptruckman.minecraft.pluginbase.messages.BundledMessage;
import com.dumptruckman.minecraft.pluginbase.messages.Message;
import com.dumptruckman.minecraft.pluginbase.messages.Messages;
import com.dumptruckman.minecraft.pluginbase.messages.Theme;
import com.dumptruckman.minecraft.pluginbase.messages.messaging.Messaging;
import com.dumptruckman.minecraft.pluginbase.messages.messaging.SendablePluginBaseException;
import com.dumptruckman.minecraft.pluginbase.minecraft.BasePlayer;
import com.dumptruckman.minecraft.pluginbase.util.time.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for handling commands.
 * <p/>
 * This entails everything from registering them to detecting executed commands and delegating them
 * to the appropriate command class.
 * <p/>
 * This must be implemented fully for a specific Minecraft server implementation.
 *
 * @param <P> Typically represents a plugin implementing this command handler.
 */
public abstract class CommandHandler<P extends CommandProvider & Messaging> {

    @NotNull
    protected final P plugin;
    @NotNull
    protected final Map<String, Class<? extends Command>> commandMap;
    @NotNull
    private final Map<String, CommandKey> commandKeys = new HashMap<String, CommandKey>();
    @NotNull
    private final Map<BasePlayer, QueuedCommand> queuedCommands = new HashMap<BasePlayer, QueuedCommand>();
    @NotNull
    private Map<CommandInfo, String> usageMap = new HashMap<CommandInfo, String>();

    /**
     * Creates a new command handler.
     * <p/>
     * Typically you only want one of these per plugin.
     *
     * @param plugin The plugin utilizing this command handler.
     */
    protected CommandHandler(@NotNull final P plugin) {
        this.plugin = plugin;
        this.commandMap = new HashMap<String, Class<? extends Command>>();
        Messages.registerMessages(plugin, CommandHandler.class);
    }

    /**
     * Retrieves a PluginLogger for this CommandHandler which is inherited from the plugin passed in during construction.
     *
     * @return a PluginLogger for this CommandHandler.
     */
    @NotNull
    protected PluginLogger getLog() {
        return plugin.getLog();
    }

    //public boolean registerCommmands(String packageName) {

    //}

    /**
     * Registers the command represented by the given command class.
     *
     * @param commandClass the command class to register.
     * @return true if command registered successfully.
     * @throws IllegalArgumentException if there was some problem with the command class passed in.
     */
    public boolean registerCommand(@NotNull final Class<? extends Command> commandClass) throws IllegalArgumentException {
        final CommandInfo cmdInfo = commandClass.getAnnotation(CommandInfo.class);
        if (cmdInfo == null) {
            throw new IllegalArgumentException("Command must be annotated with @CommandInfo");
        }
        final Command command = loadCommand(commandClass);
        if (command == null) {
            getLog().severe("Could not register: " + commandClass);
            return false;
        }

        final List<String> aliases;
        if (command instanceof BuiltInCommand) {
            aliases = new ArrayList<String>(cmdInfo.aliases().length + cmdInfo.prefixedAliases().length
                    + cmdInfo.directlyPrefixedAliases().length + ((BuiltInCommand) command).getStaticAliases().size()
                    + 1);
        } else {
            aliases = new ArrayList<String>(cmdInfo.aliases().length + cmdInfo.prefixedAliases().length
                    + cmdInfo.directlyPrefixedAliases().length + 1);
        }
        if (cmdInfo.directlyPrefixPrimary()) {
            aliases.add(plugin.getCommandPrefix() + cmdInfo.primaryAlias());
        } else if (cmdInfo.prefixPrimary())  {
            aliases.add(plugin.getCommandPrefix() + " " + cmdInfo.primaryAlias());
        } else {
            aliases.add(cmdInfo.primaryAlias());
        }
        if (commandMap.containsKey(aliases.get(0))) {
            throw new IllegalArgumentException("Command with the same primary alias has already been registered!");
        }
        for (final String alias : cmdInfo.aliases()) {
            if (!alias.isEmpty()) {
                aliases.add(alias);
            }
        }
        for (final String alias : cmdInfo.prefixedAliases()) {
            if (!alias.isEmpty()) {
                aliases.add(plugin.getCommandPrefix() + " " + alias);
            }
        }
        for (final String alias : cmdInfo.directlyPrefixedAliases()) {
            if (!alias.isEmpty()) {
                aliases.add(plugin.getCommandPrefix() + alias);
            }
        }
        if (command instanceof BuiltInCommand) {
            final BuiltInCommand builtInCommand = (BuiltInCommand) command;
            for (final Object alias : builtInCommand.getStaticAliases()) {
                if (!alias.toString().isEmpty()) {
                    aliases.add(alias.toString());
                }
            }
        }
        final String[] permissions;
        if (command.getPerm() != null) {
            permissions = new String[1];
            permissions[0] = command.getPerm().getName();
        } else {
            permissions = new String[0];
        }
        cacheUsageString(cmdInfo);
        final CommandRegistration<P> bukkitCmdInfo = new CommandRegistration<P>(getArguments(cmdInfo), cmdInfo.desc(), aliases.toArray(new String[aliases.size()]), plugin, permissions);
        if (register(bukkitCmdInfo, command)) {
            getLog().fine("Registered command '%s' to: %s", aliases.get(0), commandClass);
            String split[] = aliases.get(0).split(" ");
            CommandKey key;
            if (split.length == 1) {
                key = newKey(split[0], true);
            } else {
                key = newKey(split[0], false);
                for (int i = 1; i < split.length; i++) {
                    key = key.newKey(split[i], (i == split.length - 1));
                }
            }
            commandMap.put(aliases.get(0), commandClass);
            // Register language in the command class if any.
            Messages.registerMessages(plugin, commandClass);
            return true;
        }

        getLog().severe("Failed to register: " + commandClass);
        usageMap.remove(cmdInfo);
        return false;
    }

    /**
     * Tells the server implementation to register the given command information as a command so that
     * someone using the command will delegate the execution to this plugin/command handler.
     *
     * @param commandInfo the info for the command to register.
     * @return true if successfully registered.
     */
    protected abstract boolean register(@NotNull final CommandRegistration<P> commandInfo, @NotNull final Command<P> command);

    /**
     * Constructs a command object from the given Command class.
     * <p/>
     * The command class must accept a single parameter which is an object extending both
     * {@link Messaging} and {@link CommandProvider}.
     *
     * @param clazz the command class to instantiate.
     * @return a new instance of the command or null if unable to instantiate.
     */
    @Nullable
    protected Command loadCommand(@NotNull final Class<? extends Command> clazz) {
        try {
            for (final Constructor constructor : clazz.getDeclaredConstructors()) {
                if (constructor.getParameterTypes().length == 1
                        && Messaging.class.isAssignableFrom(constructor.getParameterTypes()[0])
                        && CommandProvider.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
                    constructor.setAccessible(true);
                    try {
                        return (Command) constructor.newInstance(plugin);
                    } finally {
                        constructor.setAccessible(false);
                    }
                }
            }
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        } catch (final InstantiationException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    void removedQueuedCommand(@NotNull final BasePlayer player, @NotNull final QueuedCommand command) {
        if (queuedCommands.containsKey(player) && queuedCommands.get(player).equals(command)) {
            queuedCommands.remove(player);
        }
    }

    /** Message used when a users tries to confirm a command but has not queued one or the queued one has expired. */
    public static final Message NO_QUEUED_COMMANDS = Message.createMessage("commands.queued.none_queued",
            Theme.SORRY + "Sorry, but you have not used any commands that require confirmation.");
    /** Default message used when the user must confirm a queued command. */
    public static final Message MUST_CONFIRM = Message.createMessage("commands.queued.must_confirm",
            Theme.DO_THIS + "You must confirm the previous command by typing " + Theme.CMD_HIGHLIGHT + "%s"
                    + "\n" + Theme.INFO + "You have %s to comply.");

    /**
     * Confirms any queued command for the given player.
     *
     * @param player the player to confirm queued commands for.
     * @return true if there was a queued command.
     */
    public boolean confirmCommand(@NotNull final BasePlayer player) {
        final QueuedCommand queuedCommand = queuedCommands.get(player);
        if (queuedCommand != null) {
            queuedCommand.confirm();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Locates and runs a command executed by a user.
     *
     * @param player the user executing the command.
     * @param args the space separated arguments of the command including the base command itself.
     * @return true if the command executed successfully.
     * @throws SendablePluginBaseException if there were any exceptions brought about by the usage of the command.
     * <p/>
     * The causes are many fold and include things such as using an improper amount of parameters or attempting to
     * use a flag not recognized by the command.
     * TODO This needs to throw an extended PluginBaseException
     */
    public boolean locateAndRunCommand(@NotNull final BasePlayer player, @NotNull String[] args) throws CommandException {
        args = commandDetection(args);
        getLog().finest("'%s' is attempting to use command '%s'", player, Arrays.toString(args));
        if (this.plugin.useQueuedCommands()
                && !this.commandMap.containsKey(this.plugin.getCommandPrefix() + "confirm")
                && args.length == 2
                && args[0].equalsIgnoreCase(this.plugin.getCommandPrefix())
                && args[1].equalsIgnoreCase("confirm")) {
            getLog().finer("No confirm command registered, using built in confirm...");
            if (!confirmCommand(player)) {
                this.plugin.getMessager().message(player, NO_QUEUED_COMMANDS);
            }
            return true;
        }
        final Class<? extends Command> commandClass = commandMap.get(args[0]);
        if (commandClass == null) {
            getLog().severe("Could not locate registered command '" + args[0] + "'");
            return false;
        }
        final Command command = loadCommand(commandClass);
        if (command == null) {
            getLog().severe("Could not load registered command class '" + commandClass + "'");
            return false;
        }
        final CommandInfo cmdInfo = command.getClass().getAnnotation(CommandInfo.class);
        if (cmdInfo == null) {
            getLog().severe("Missing CommandInfo for command: " + args[0]);
            return false;
        }
        final Set<Character> valueFlags = new HashSet<Character>();

        char[] flags = cmdInfo.flags().toCharArray();
        final Set<Character> newFlags = new HashSet<Character>();
        for (int i = 0; i < flags.length; ++i) {
            if (flags.length > i + 1 && flags[i + 1] == ':') {
                valueFlags.add(flags[i]);
                ++i;
            }
            newFlags.add(flags[i]);
        }
        final CommandContext context = new CommandContext(args, valueFlags);
        if (context.argsLength() < cmdInfo.min()) {
            throw new CommandUsageException(Message.bundleMessage(TOO_FEW_ARGUMENTS), getUsage(args, 0, command, cmdInfo));
        }
        if (cmdInfo.max() != -1 && context.argsLength() > cmdInfo.max()) {
            throw new CommandUsageException(Message.bundleMessage(TOO_MANY_ARGUMENTS), getUsage(args, 0, command, cmdInfo));
        }
        if (!cmdInfo.anyFlags()) {
            for (char flag : context.getFlags()) {
                if (!newFlags.contains(flag)) {
                    throw new CommandUsageException(Message.bundleMessage(UNKNOWN_FLAG, flag), getUsage(args, 0, command, cmdInfo));
                }
            }
        }
        if (!command.runCommand(player, context)) {
            throw new CommandUsageException(Message.bundleMessage(USAGE_ERROR), getUsage(args, 0, command, cmdInfo));
        }
        if (command instanceof QueuedCommand) {
            final QueuedCommand queuedCommand = (QueuedCommand) command;
            getLog().finer("Queueing command '%s' for '%s'", queuedCommand, player);
            queuedCommands.put(player, queuedCommand);
            final BundledMessage confirmMessage = queuedCommand.getConfirmMessage();
            if (confirmMessage != null) {
                this.plugin.getMessager().message(player, confirmMessage.getMessage(), confirmMessage.getArgs());
            } else {
                this.plugin.getMessager().message(player, MUST_CONFIRM,
                        "/" + this.plugin.getCommandPrefix() + "confirm",
                        Duration.valueOf(queuedCommand.getExpirationDuration()).asVerboseString());
            }
        }
        return true;
    }

    public static final Message TOO_FEW_ARGUMENTS = Message.createMessage("commands.usage.too_few_arguments",
            Theme.ERROR + "Too few arguments.");
    public static final Message TOO_MANY_ARGUMENTS = Message.createMessage("commands.usage.too_many_arguments",
            Theme.ERROR + "Too many arguments.");
    public static final Message UNKNOWN_FLAG = Message.createMessage("commands.usage.unknown_flag", Theme.ERROR + "Unknown flag: " + Theme.VALUE + "%s");
    public static final Message USAGE_ERROR = Message.createMessage("commands.usage.usage_error", Theme.ERROR + "Usage error...");

    public static final Message VALUE_FLAG_ALREADY_GIVEN = Message.createMessage("commands.usage.value_flag_already_given",
            Theme.ERROR + "Value flag '" + Theme.VALUE + "%s" + Theme.ERROR + "' already given");
    public static final Message NO_VALUE_FOR_VALUE_FLAG = Message.createMessage("commands.usage.must_specify_value_for_value_flag",
            Theme.ERROR + "No value specified for the '" + Theme.VALUE + "-%s" + Theme.ERROR + "' flag.");

    /**
     * Returns a list of strings detailing the usage of the given command.
     *
     * @param args
     * @param level
     * @param cmd
     * @param cmdInfo
     * @return
     */
    protected List<String> getUsage(@NotNull final String[] args, final int level, final Command cmd, @NotNull final CommandInfo cmdInfo) {
        final List<String> commandUsage = new ArrayList<String>();
        final StringBuilder command = new StringBuilder();
        command.append(Theme.CMD_USAGE);
        command.append('/');
        for (int i = 0; i <= level; ++i) {
            command.append(args[i]);
            command.append(' ');
        }
        command.append(getArguments(cmdInfo));
        commandUsage.add(command.toString());

        final String help;
        final Message helpMessage = cmd.getHelp();
        if (helpMessage != null) {
            help = plugin.getMessager().getLocalizedMessage(helpMessage);
        } else {
            help = "";
        }
        if (!help.isEmpty()) {
            commandUsage.add(help);
        }

        return commandUsage;
    }

    private void cacheUsageString(@NotNull final CommandInfo cmdInfo) {
        final String flags = cmdInfo.flags();

        final StringBuilder command2 = new StringBuilder();
        command2.append(parseUsage(cmdInfo.usage()));

        for (int i = 0; i < flags.length(); ++i) {
            command2.append(" ");
            command2.append(Theme.OPT_ARG).append("[").append(Theme.CMD_FLAG).append("-");
            command2.append(flags.charAt(i));
            if (flags.length() > (i + 1) && flags.charAt(i + 1) == ':') {
                command2.append(Theme.REQ_ARG).append(" {VALUE}");
                i++;
            }
            command2.append(Theme.OPT_ARG).append("]");
        }

        usageMap.put(cmdInfo, command2.toString());
    }

    protected String getArguments(@NotNull final CommandInfo cmdInfo) {
        return usageMap.containsKey(cmdInfo) ? usageMap.get(cmdInfo) : "";
    }

    private static final Pattern OPTIONAL_ARGS_PATTERN = Pattern.compile("\\[.+?\\]");
    private static final Pattern REQUIRED_ARGS_PATTERN = Pattern.compile("\\{.+?\\}");

    private CharSequence parseUsage(@NotNull String usageString) {
        if (usageString.isEmpty()) {
            return usageString;
        }
        // Add required arg theme before required args
        StringBuilder usage = new StringBuilder(usageString.length() + 10);
        Matcher matcher = REQUIRED_ARGS_PATTERN.matcher(usageString);
        int lastIndex = 0;
        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                // Add the initial part of the string if the required arg isn't first position
                usage.append(usageString.subSequence(lastIndex, matcher.start()));
            }
            usage.append(Theme.REQ_ARG);
            usage.append(matcher.group());
            lastIndex = matcher.end();
        }
        // Add what is left over in the string
        usage.append(usageString.subSequence(lastIndex, usageString.length()));

        // Replace initial string with builder that contains colored required args
        usageString = usage.toString();

        // Add optional arg theme before optional args
        usage = new StringBuilder(usageString.length() + 10);
        matcher = OPTIONAL_ARGS_PATTERN.matcher(usageString);
        lastIndex = 0;
        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                // Add the initial part of the string if the optional arg isn't first position
                usage.append(usageString.subSequence(lastIndex, matcher.start()));
            }
            usage.append(Theme.OPT_ARG);
            usage.append(matcher.group());
            lastIndex = matcher.end();
        }
        // Add what is left over in the string
        usage.append(usageString.subSequence(lastIndex, usageString.length()));
        return usage;
    }

    public String[] commandDetection(@NotNull final String[] split) {
        CommandKey commandKey = getKey(split[0]);
        CommandKey lastActualCommand = null;
        if (commandKey == null) {
            return split;
        } else if (commandKey.isCommand()) {
            lastActualCommand = commandKey;
        }

        int i;
        int lastActualCommandIndex = 0;
        for (i = 1; i < split.length; i++) {
            commandKey = commandKey.getKey(split[i]);
            if (commandKey != null) {
                if (commandKey.isCommand()) {
                    lastActualCommand = commandKey;
                    lastActualCommandIndex = i;
                }
            } else {
                break;
            }
        }
        if (lastActualCommand != null) {
            String[] newSplit = new String[split.length - lastActualCommandIndex];
            newSplit[0] = lastActualCommand.getName();
            if (newSplit.length > 1 && lastActualCommandIndex + 1 < split.length) {
                System.arraycopy(split, lastActualCommandIndex + 1, newSplit, 1, split.length - lastActualCommandIndex - 1);
            }
            return newSplit;
        }
        return split;
    }

    protected CommandKey getKey(@NotNull final String key) {
        return commandKeys.get(key);
    }

    protected CommandKey newKey(@NotNull final String key, final boolean command) {
        if (commandKeys.containsKey(key)) {
            if (command) {
                commandKeys.put(key, new CommandKey(commandKeys.get(key)));
            }
            return commandKeys.get(key);
        } else {
            final CommandKey commandKey = new CommandKey(key, command);
            commandKeys.put(key, commandKey);
            return commandKey;
        }
    }
}
