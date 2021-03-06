package com.dumptruckman.minecraft.pluginbase.util;

import com.dumptruckman.minecraft.pluginbase.bukkit.properties.YamlProperties;
import com.dumptruckman.minecraft.pluginbase.logging.Logging;
import com.dumptruckman.minecraft.pluginbase.messages.PluginBaseException;
import com.dumptruckman.minecraft.pluginbase.plugin.BaseConfig;
import com.dumptruckman.minecraft.pluginbase.properties.ListProperty;
import com.dumptruckman.minecraft.pluginbase.properties.MappedProperty;
import com.dumptruckman.minecraft.pluginbase.properties.NestedProperties;
import com.dumptruckman.minecraft.pluginbase.properties.NestedProperty;
import com.dumptruckman.minecraft.pluginbase.properties.NullProperty;
import com.dumptruckman.minecraft.pluginbase.properties.PropertyFactory;
import com.dumptruckman.minecraft.pluginbase.properties.SimpleProperty;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedList;

public class MockConfig extends YamlProperties implements BaseConfig {
    
    public static final SimpleProperty<Boolean> TEST = PropertyFactory.newProperty(Boolean.class, "test", true)
            .comment("# ===[ PluginBase Test ]===").build();
    public static final NullProperty SETTINGS = PropertyFactory.newNullProperty("settings")
            .comment("# ===[ PluginBase Settings ]===").build();
    
    public static final MappedProperty<Integer> SPECIFIC_TEST = PropertyFactory.newMappedProperty(Integer.class, "specific_test")
            .build();

    public static final ListProperty<Integer> LIST_TEST = PropertyFactory.newListProperty(Integer.class, "list_test", new LinkedList<Integer>())
            .build();

    public static final NestedProperty<Nested> NESTED_TEST = PropertyFactory.newNestedProperty(Nested.class, "nested").comment("# ababadfga").build();
    
    public MockConfig(boolean doComments, File configFile) throws PluginBaseException {
        super(Logging.getLogger(), doComments, true, configFile, MockConfig.class, BaseConfig.class);
    }

    @NotNull
    @Override
    protected String getHeader() {
        return "# ===[ PluginBase Config ]===";
    }

    public static interface Nested extends NestedProperties {

        public static final NestedProperty<DoubleNested> NESTED_TEST = PropertyFactory.newNestedProperty(DoubleNested.class, "double_nested").comment("# TEADFA").build();

        public static final SimpleProperty<Boolean> TEST = PropertyFactory.newProperty(Boolean.class, "nested_test", true)
                .comment("# ===[ Nested Test ]===").build();

        public static interface DoubleNested extends NestedProperties {
            public static final SimpleProperty<Boolean> TEST = PropertyFactory.newProperty(Boolean.class, "double_nested_test", true)
                    .comment("# ===[ Double Nested Test ]===").build();
        }
    }
}
