package me.usainsrht.ujobs;

import me.usainsrht.ujobs.api.events.*;
import me.usainsrht.ujobs.models.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ApiAndEventsTest {

    @Test
    void testActionRegistryAndCustomActions() {
        // Built-in actions should be pre-registered
        Action breakAction = ActionRegistry.get("break");
        assertNotNull(breakAction);
        assertEquals("break", breakAction.getName());
        assertEquals(BuiltInActions.Material.BREAK, breakAction);

        Action killAction = ActionRegistry.get("KILL");
        assertNotNull(killAction);
        assertEquals("kill", killAction.getName());
        assertEquals(BuiltInActions.Entity.KILL, killAction);

        // Custom action
        Action spellAction = ActionRegistry.getOrCreate("spell_cast");
        assertNotNull(spellAction);
        assertEquals("spell_cast", spellAction.getName());

        Action sameSpellAction = Action.of("SPELL_CAST");
        assertEquals(spellAction, sameSpellAction);
        assertEquals(spellAction.hashCode(), sameSpellAction.hashCode());

        assertTrue(ActionRegistry.isRegistered("spell_cast"));
    }

    @Test
    void testJobBuilderAndModification() {
        Job job = Job.builder()
                .id("alchemist")
                .name(Component.text("Alchemist"))
                .icon(Material.BREWING_STAND)
                .levelEquation("<next_level>*10")
                .build();

        assertEquals("alchemist", job.getId());
        assertEquals(Material.BREWING_STAND, job.getIcon());
        assertEquals("<next_level>*10", job.getLevelEquation());

        // Add action
        Action brewAction = Action.of("brew");
        Job.ActionReward reward = new Job.ActionReward(10.0, 5.0);
        job.addAction(brewAction, "speed_potion", reward);

        assertTrue(job.hasAction(brewAction));
        assertTrue(job.hasAction(brewAction, "speed_potion"));
        assertEquals(reward, job.getActionReward(brewAction, "speed_potion"));

        // Modify job
        job.setIcon(Material.POTION);
        assertEquals(Material.POTION, job.getIcon());

        // Remove action
        assertTrue(job.removeAction(brewAction, "speed_potion"));
        assertFalse(job.hasAction(brewAction, "speed_potion"));
    }

    @Test
    void testPlayerJobExpGainEventCancellableAndChangedValues() {
        Job job = Job.builder().id("miner").build();
        Action action = BuiltInActions.Material.BREAK;

        // Test sync constructor
        PlayerJobExpGainEvent syncEvent = new PlayerJobExpGainEvent(
                null, job, action, "DIAMOND_ORE", 10.0, 2.0, 1, false
        );
        assertFalse(syncEvent.isAsynchronous());
        assertFalse(syncEvent.isCancelled());
        assertEquals(10.0, syncEvent.getExp());
        assertEquals(2.0, syncEvent.getMoney());
        assertEquals(1, syncEvent.getAmount());

        // Modify values
        syncEvent.setExp(25.0);
        syncEvent.setMoney(5.0);
        syncEvent.setAmount(2);
        assertEquals(25.0, syncEvent.getExp());
        assertEquals(5.0, syncEvent.getMoney());
        assertEquals(2, syncEvent.getAmount());
        assertEquals(50.0, syncEvent.getTotalExp());
        assertEquals(10.0, syncEvent.getTotalMoney());

        // Test cancel
        syncEvent.setCancelled(true);
        assertTrue(syncEvent.isCancelled());

        // Test async constructor
        PlayerJobExpGainEvent asyncEvent = new PlayerJobExpGainEvent(
                null, job, action, "DIAMOND_ORE", 10.0, 2.0, 1, true
        );
        assertTrue(asyncEvent.isAsynchronous());
    }

    @Test
    void testPlayerJobLevelUpEventCancellableAndChangedValues() {
        Job job = Job.builder().id("warrior").build();

        PlayerJobLevelUpEvent levelUpEvent = new PlayerJobLevelUpEvent(
                null, job, 5, 6, List.of("give <player> diamond 1"), null, true
        );

        assertTrue(levelUpEvent.isAsynchronous());
        assertEquals(5, levelUpEvent.getFromLevel());
        assertEquals(6, levelUpEvent.getToLevel());
        assertEquals(1, levelUpEvent.getCommands().size());

        // Modify toLevel and commands
        levelUpEvent.setToLevel(7);
        levelUpEvent.setCommands(List.of("give <player> diamond 2", "broadcast level up"));
        assertEquals(7, levelUpEvent.getToLevel());
        assertEquals(2, levelUpEvent.getCommands().size());

        // Test cancel
        levelUpEvent.setCancelled(true);
        assertTrue(levelUpEvent.isCancelled());
    }

    @Test
    void testJobRegisterAndUnregisterEvents() {
        Job job = Job.builder().id("enchanter").build();

        JobRegisterEvent registerEvent = new JobRegisterEvent(job, false);
        assertFalse(registerEvent.isAsynchronous());
        assertEquals(job, registerEvent.getJob());
        assertFalse(registerEvent.isCancelled());
        registerEvent.setCancelled(true);
        assertTrue(registerEvent.isCancelled());

        JobUnregisterEvent unregisterEvent = new JobUnregisterEvent(job, true);
        assertTrue(unregisterEvent.isAsynchronous());
        assertEquals(job, unregisterEvent.getJob());
        assertFalse(unregisterEvent.isCancelled());
        unregisterEvent.setCancelled(true);
        assertTrue(unregisterEvent.isCancelled());
    }

    @Test
    void testLeaderboardUpdateEvent() {
        Job job = Job.builder().id("hunter").build();
        UUID uuid = UUID.randomUUID();

        PlayerJobLeaderboardUpdateEvent event = new PlayerJobLeaderboardUpdateEvent(
                uuid, job, 2, 0, 15, true
        );

        assertTrue(event.isAsynchronous());
        assertEquals(uuid, event.getPlayerUuid());
        assertEquals(job, event.getJob());
        assertEquals(2, event.getOldPosition());
        assertEquals(0, event.getNewPosition());
        assertEquals(15, event.getLevel());
        assertFalse(event.isCancelled());

        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }
}
