/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.magmaguy.elitemobs.events;

import com.magmaguy.elitemobs.EliteMobs;
import com.magmaguy.elitemobs.EntityTracker;
import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.config.ConfigValues;
import com.magmaguy.elitemobs.config.EventsConfig;
import com.magmaguy.elitemobs.config.MobCombatSettingsConfig;
import com.magmaguy.elitemobs.config.ValidMobsConfig;
import com.magmaguy.elitemobs.events.mobs.TheReturned;
import com.magmaguy.elitemobs.events.mobs.ZombieKing;
import com.magmaguy.elitemobs.mobspawning.NaturalEliteMobSpawnEventHandler;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM;
import static org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL;

public class DeadMoon implements Listener {

    public static boolean entityQueued = false;
    public static boolean eventOngoing = false;

    public static void initializeEvent() {
        entityQueued = true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(CreatureSpawnEvent event) {

        if (event.isCancelled()) return;

        if (event.getEntity().getWorld().getTime() < 13184 || event.getEntity().getWorld().getTime() > 22800) return;
        if (!event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)) return;
        if (!(event.getEntity().getType().equals(EntityType.ZOMBIE)) && !(event.getEntity().getType().equals(EntityType.HUSK)))
            return;
        if (!EventLauncher.verifyPlayerCount()) return;

        if (!EliteMobs.validWorldList.contains(event.getEntity().getWorld())) return;

        if (event.getEntity().getWorld().getEnvironment().equals(World.Environment.NETHER) ||
                event.getEntity().getWorld().getEnvironment().equals(World.Environment.THE_END)) return;

        if (MoonPhaseDetector.detectMoonPhase(event.getEntity().getWorld()) != MoonPhaseDetector.moonPhase.NEW_MOON)
            return;

        if (!ConfigValues.mobCombatSettingsConfig.getBoolean(MobCombatSettingsConfig.NATURAL_MOB_SPAWNING) ||
                !ConfigValues.validMobsConfig.getBoolean(ValidMobsConfig.ALLOW_AGGRESSIVE_ELITEMOBS))
            return;


        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER) && !ConfigValues.mobCombatSettingsConfig.getBoolean(MobCombatSettingsConfig.SPAWNERS_SPAWN_ELITE_MOBS))
            return;

        if (!(event.getSpawnReason() == NATURAL || event.getSpawnReason() == CUSTOM)) return;

        if (!EntityTracker.isEliteMob(event.getEntity()))
            NaturalEliteMobSpawnEventHandler.naturalMobProcessor(event.getEntity(), CreatureSpawnEvent.SpawnReason.NATURAL);

        //add entityQueued
        if (entityQueued && !TheReturned.isTheReturned(event.getEntity())
                && (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL) ||
                event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.CUSTOM))) {

            eventOngoing = true;
            entityQueued = false;

            EventMessage.sendEventMessage(event.getEntity(), ConfigValues.eventsConfig.getString(EventsConfig.DEADMOON_ANNOUNCEMENT_MESSAGE));

            ZombieKing.spawnZombieKing(event.getLocation());
            terminateEvent(event.getEntity());

        }

    }

    private void terminateEvent(LivingEntity livingEntity) {

        World world = livingEntity.getWorld();

        new BukkitRunnable() {

            @Override
            public void run() {

                if (world.getTime() > 22800 || world.getTime() < 13184 || MoonPhaseDetector.detectMoonPhase(world) != MoonPhaseDetector.moonPhase.NEW_MOON) {

                    eventOngoing = false;

                    EventMessage.sendEventMessage(livingEntity, ConfigValues.eventsConfig.getString(EventsConfig.DEADMOON_END_MESSAGE));

                    cancel();

                }

            }

        }.runTaskTimer(MetadataHandler.PLUGIN, 1, 1);

    }


}
