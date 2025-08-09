package me.usainsrht.ujobs.models;

public class BuiltInActions {

    public enum Entity implements EntityAction {

        KILL("kill"),
        BREED("breed"),
        TAME("tame");

        final String name;
        Entity(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Material implements MaterialAction {

        BREAK("break"),
        PLACE("place"),
        FISH("fish"),
        ANVIL_MERGE("anvil_merge"),
        CRAFT("craft"),
        TRADE("trade"),
        HARVEST("harvest"),
        SMELT("smelt");

        final String name;
        Material(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Special implements SpecialAction {

        ENCHANT("enchant"),
        GENERATE_LOOT("generate_loot"),
        RAID("raid");

        final String name;
        Special(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static Action get(String name) {
        for (Entity entity : Entity.values()) {
            if (entity.getName().equalsIgnoreCase(name)) {
                return entity;
            }
        }
        for (Material material : Material.values()) {
            if (material.getName().equalsIgnoreCase(name)) {
                return material;
            }
        }
        for (Special special : Special.values()) {
            if (special.getName().equalsIgnoreCase(name)) {
                return special;
            }
        }
        return null;
    }

}
