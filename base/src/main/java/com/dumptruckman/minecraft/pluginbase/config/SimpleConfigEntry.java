package com.dumptruckman.minecraft.pluginbase.config;

import com.dumptruckman.minecraft.pluginbase.locale.Message;
import com.dumptruckman.minecraft.pluginbase.locale.Messages;
import com.dumptruckman.minecraft.pluginbase.plugin.PluginBase;
import com.dumptruckman.minecraft.pluginbase.util.Logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleConfigEntry<T> implements ConfigEntry<T> {

    protected static PluginBase plugin = null;

    public static void init(PluginBase plugin) {
        SimpleConfigEntry.plugin = plugin;
    }

    private String path;
    private T def;
    private final List<String> comments;
    private Class<T> type;

    public SimpleConfigEntry(Class<T> type, String path, T def, String... comments) {
        this.path = path;
        this.def = def;
        this.comments = new ArrayList<String>(Arrays.asList(comments));
        this.type = type;
        //if (this.comments.isEmpty()) {
        //    this.comments.add("");
        //}
        Entries.entries.add(this);
    }

    /**
     * Retrieves the path for a config option.
     *
     * @return The path for a config option.
     */
    public String getName() {
        return this.path;
    }

    public Class<T> getType() {
        return this.type;
    }

    /**
     * Retrieves the default value for a config path.
     *
     * @return The default value for a config path.
     */
    public T getDefault() {
        return this.def;
    }

    /**
     * Retrieves the comment for a config path.
     *
     * @return The comments for a config path.
     */
    public List<String> getComments() {
        return this.comments;
    }

    public boolean isPluginSet() {
        if (plugin == null) {
            if (Logging.getDebugMode() != 3) {
                Logging.setDebugMode(3);
                Logging.fine("Enabled debug mode since Config has not been initialized with a plugin.");
                Logging.fine("All config values will return default.");
            }
            return false;
        }
        return true;
    }

    public boolean isValid(Object obj) {
        return true;
    }

    public Message getInvalidMessage() {
        return Messages.BLANK;
    }

    @Override
    public Object serialize(T value) {
        return value;
    }

    @Override
    public T deserialize(Object o) {
        return getType().cast(o);
    }
}
