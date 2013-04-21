/*
 * WorldEdit
 * Copyright (C) 2012 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copied and modified on March 9, 2013 by dumptruckman.
*/
package com.dumptruckman.minecraft.pluginbase.command;

import com.dumptruckman.minecraft.pluginbase.messages.messaging.Messaging;

public class CommandRegistration<P extends CommandProvider & Messaging> {

    private final String[] aliases;
    private final P registeredWith;
    private final String usage, desc;
    private final String[] permissions;

    CommandRegistration(String usage, String desc, String[] aliases, P registeredWith) {
        this(usage, desc, aliases, registeredWith, null);
    }

    CommandRegistration(String usage, String desc, String[] aliases, P registeredWith, String[] permissions) {
        this.usage = usage;
        this.desc = desc;
        this.aliases = aliases;
        this.permissions = permissions;
        this.registeredWith = registeredWith;
    }

    public String[] getAliases() {
        return aliases;
    }

    public String getName() {
        return aliases[0];
    }

    public String getUsage() {
        return usage;
    }

    public String getDesc() {
        return desc;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public P getRegisteredWith() {
        return registeredWith;
    }
}