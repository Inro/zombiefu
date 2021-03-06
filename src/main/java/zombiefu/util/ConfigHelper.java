package zombiefu.util;

import jade.core.Actor;
import jade.util.Dice;
import jade.util.Guard;
import jade.util.datatype.ColoredChar;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zombiefu.actor.Door;
import zombiefu.human.Shop;
import zombiefu.builder.FoodBuilder;
import zombiefu.builder.ItemBuilder;
import zombiefu.builder.MonsterBuilder;
import zombiefu.builder.WeaponBuilder;
import zombiefu.items.Item;
import zombiefu.items.KeyCard;
import zombiefu.builder.HumanBuilder;
import zombiefu.builder.RandomEnemyClass;
import zombiefu.builder.RandomItemClass;
import zombiefu.builder.ShopBuilder;
import zombiefu.creature.AttributeSet;
import zombiefu.exception.ActorConfigNotFoundException;
import zombiefu.human.Human;
import zombiefu.items.Food;
import zombiefu.items.Weapon;
import zombiefu.items.WeaponType;
import zombiefu.level.Level;
import zombiefu.mapgen.RoomBuilder;
import zombiefu.creature.Monster;
import zombiefu.fight.DamageAnimation;
import zombiefu.human.ShopInventar;
import zombiefu.player.Attribute;
import zombiefu.player.Discipline;

public class ConfigHelper {

    private static final ColoredChar DEFAULT_FLOOR_CHAR = ColoredChar.create(' ');
    private static HashMap<String, WeaponBuilder> weapons;
    private static HashMap<String, FoodBuilder> food;
    private static HashMap<String, Door> doors;
    private static HashMap<String, ShopBuilder> shops;
    private static HashMap<String, Level> levels;
    private static HashMap<String, MonsterBuilder> monsters;
    private static HashMap<String, HumanBuilder> humans;
    private static HashMap<Character, Color> charSet;
    private static HashMap<Character, Boolean> passSet;
    private static HashMap<Character, Boolean> visibleSet;
    private static HashMap<String, String> randomItems;
    private static HashMap<String, String> randomMonster;

    private static void initCharSet() {
        charSet = new HashMap<>();
        passSet = new HashMap<>();
        visibleSet = new HashMap<>();
        String[] settings = getStrings(ZombieGame.getResource("base", "charset.txt"));
        for (int i = 0; i < settings.length; i++) {
            String[] setting = settings[i].split(" ");
            charSet.put(setting[0].charAt(0), Color.decode("0x" + setting[3]));
            passSet.put(setting[0].charAt(0), setting[1].equals("passable"));
            visibleSet.put(setting[0].charAt(0), setting[2].equals("visible"));
        }
    }

    public static ItemBuilder getItemBuilderByName(String s) {
        try {
            return getWeaponBuilderByName(s);
        } catch (ActorConfigNotFoundException ex) {
            try {
                return getFoodBuilderByName(s);
            } catch (ActorConfigNotFoundException ex1) {
                return null;
            }
        }
    }

    private static FoodBuilder getFoodBuilderByName(String s) throws ActorConfigNotFoundException {
        if (food == null) {
            food = new HashMap<>();
        }
        if (!food.containsKey(s)) {
            ZombieTools.log("getFoodBuilderByName(" + s + "): Erzeuge FoodBuilder");
            ActorConfig config = new ActorConfig("food", s);
            String name = config.getName();
            ColoredChar c = config.getChar();
            Integer hp = Integer.decode(config.get("hp", "0"));
            food.put(s, new FoodBuilder(c, name, hp));
        }
        return food.get(s);
    }

    private static WeaponBuilder getWeaponBuilderByName(String s) throws ActorConfigNotFoundException {
        if (weapons == null) {
            weapons = new HashMap<>();
        }
        if (!weapons.containsKey(s)) {
            ZombieTools.log("getWeaponBuilderByName(" + s + "): Erzeuge WeaponBuilder");
            ActorConfig config = new ActorConfig("weapons", s);
            String name = config.getName();
            ColoredChar c = config.getChar();
            WeaponType type = WeaponType.getTypeFromString(config.get("type", "Nahkampf"));
            String munitionStr = config.get("munition", "-1");
            int munition = munitionStr.equals("unbegrenzt") ? -1 : Integer.decode(munitionStr);
            int damage = Integer.decode(config.get("damage", "1"));
            int range = Integer.decode(config.get("range", "1"));
            double radius = Double.valueOf(config.get("radius", "1.0"));
            int dazeTurns = Integer.decode(config.get("daze.turns", "0"));
            double dazeProbability = Double.valueOf(config.get("daze.probability", "0"));
            String expertsStr = config.get("experts");
            boolean staticMunition = !config.get("staticMunition", "true").equals("false");
            Set<Discipline> experts = new HashSet<>();
            if (expertsStr != null) {
                for (String exp : expertsStr.split(" ")) {
                    experts.add(Discipline.getTypeFromString(exp));
                }
            }
            weapons.put(s, new WeaponBuilder(c, name, damage, type, experts, munition, staticMunition, radius, range, dazeTurns, dazeProbability));
        }
        return weapons.get(s);
    }

    public static Level getLevelByName(String s) {
        if (levels == null) {
            levels = new HashMap<>();
        }
        if (!levels.containsKey(s)) {
            levels.put(s, createLevelFromFile(s));
        }
        return levels.get(s);
    }

    public static Door getDoorByName(String s) {
        if (doors == null) {
            doors = new HashMap<>();
        }
        if (!doors.containsKey(s)) {
            doors.put(s, new Door(s));
        }
        return doors.get(s);
    }

    public static Shop newShopByName(String s) {
        if (shops == null) {
            shops = new HashMap<>();
        }
        if (!shops.containsKey(s)) {
            ZombieTools.log("newShopByName(" + s + "): Erzeuge ShopBuilder");
            List<String> list = new ArrayList<>(Arrays.asList(getStrings(ZombieGame.getResource("shops", s + ".shop"))));
            String[] charInfo = list.remove(0).split(" ");
            ShopInventar inv = new ShopInventar(list);
            shops.put(s, new ShopBuilder(ColoredChar.create(charInfo[0].charAt(0), Color.decode("0x" + charInfo[1])), s, inv));

        }
        return shops.get(s).buildShop();
    }

    public static Monster newMonsterByName(String s) {
        if (monsters == null) {
            monsters = new HashMap<>();
        }
        if (!monsters.containsKey(s)) {
            ZombieTools.log("newMonsterByName(" + s + "): Erzeuge MonsterBuilder");
            ActorConfig config = ActorConfig.newConfig("monsters", s);
            String name = config.getName();
            ColoredChar c = config.getChar();
            AttributeSet attSet = new AttributeSet(
                    config.contains("baseAttr.hp") ? Integer.decode(config.get("baseAttr.hp")) : null,
                    config.contains("baseAttr.att") ? Integer.decode(config.get("baseAttr.att")) : null,
                    config.contains("baseAttr.def") ? Integer.decode(config.get("baseAttr.def")) : null,
                    config.contains("baseAttr.dex") ? Integer.decode(config.get("baseAttr.dex")) : null);
            boolean staticAttributes = config.get("staticAttributes", "false").equals("true");
            Weapon w = newWeaponByName(config.get("weapon"));
            int ects = Integer.decode(config.get("ects"));
            int chaseDistance = Integer.decode(config.get("chaseDistance", "10"));
            ITMString itemDrop = new ITMString(config.get("drop"));
            double habitatRadius = config.contains("habitatRadius") ? Double.valueOf(config.get("habitatRadius")) : 10;
            monsters.put(s, new MonsterBuilder(c, name, attSet, w, ects, itemDrop, staticAttributes, chaseDistance, habitatRadius));
        }
        return monsters.get(s).buildMonster();
    }

    public static Human newHumanByName(String s) {
        if (humans == null) {
            humans = new HashMap<>();
        }
        if (!humans.containsKey(s)) {
            ZombieTools.log("newHumanByName(" + s + "): Erzeuge HumanBuilder");
            ActorConfig config = ActorConfig.newConfig("humans", s);
            String name = config.getName();
            ColoredChar c = config.getChar();
            AttributeSet attSet = new AttributeSet(
                    config.contains("baseAttr.hp") ? Integer.decode(config.get("baseAttr.hp")) : 1,
                    config.contains("baseAttr.att") ? Integer.decode(config.get("baseAttr.att")) : 1,
                    config.contains("baseAttr.def") ? Integer.decode(config.get("baseAttr.def")) : 1,
                    config.contains("baseAttr.dex") ? Integer.decode(config.get("baseAttr.dex")) : 1);
            Item offerItem = config.contains("deal.offerItem") ? new ITMString(config.get("deal.offerItem")).getSingleItem() : null;
            Integer offerMoney = config.contains("deal.offerMoney") ? ZombieTools.parseMoneyString(config.get("deal.offerMoney")) : null;
            Integer requestMoney = config.contains("deal.requestMoney") ? ZombieTools.parseMoneyString(config.get("deal.requestMoney")) : null;
            String requestItem = config.get("deal.requestItem");
            double habitatRadius  =  config.contains("habitatRadius") ? Double.valueOf(config.get("habitatRadius")) : 10;
            Map<String, String> phraseSet = config.getSubConfig("phrase");
            humans.put(s, new HumanBuilder(c, name, attSet, phraseSet, offerItem, offerMoney, requestItem, requestMoney, habitatRadius));
        }
        return humans.get(s).buildHuman();
    }

    public static Weapon newWeaponByName(String s) {
        try {
            return (Weapon) getWeaponBuilderByName(s).buildItem();
        } catch (ActorConfigNotFoundException ex) {
            Guard.verifyState(false);
            return null;
        }
    }

    public static Food newFoodByName(String s) {
        try {
            return (Food) getFoodBuilderByName(s).buildItem();
        } catch (ActorConfigNotFoundException ex) {
            System.out.println(s);
            Guard.verifyState(false);
            return null;
        }
    }

    public static KeyCard getKeyCardByName(String s) {
        return new KeyCard(getDoorByName(s));
    }

    public static HashMap<Character, Color> getCharSet() {
        if (charSet == null) {
            initCharSet();
        }
        return charSet;
    }

    public static HashMap<Character, Boolean> getPassSet() {
        if (passSet == null) {
            initCharSet();
        }
        return passSet;
    }

    public static HashMap<Character, Boolean> getVisibleSet() {
        if (visibleSet == null) {
            initCharSet();
        }
        return visibleSet;
    }

    public static Item newRandomItem(RandomItemClass a) {
        if (randomItems == null) {
            randomItems = new HashMap<>();
            randomItems = ActorConfig.newConfig("builders", "items").getConfig();
        }
        String[] items = randomItems.get(a.toString().toLowerCase()).split(" ");
        return new ITMString(Dice.global.choose(Arrays.asList(items))).getSingleItem();
    }

    public static Monster newRandomMonster(RandomEnemyClass a) {
        if (randomMonster == null) {
            randomMonster = new HashMap<>();
            randomMonster = ActorConfig.newConfig("builders", "monsters").getConfig();
        }
        String[] items = randomMonster.get(a.toString().toLowerCase()).split(" ");
        return new ITMString(Dice.global.choose(Arrays.asList(items))).getSingleMonster();
    }

    public static boolean isValidChar(char c) {
        return getCharSet().containsKey(c);
    }

    public static Level createLevelFromFile(String mapName) {

        ZombieTools.log("createLevelFromFile(" + mapName + ")");

        // Lese Metadaten ein
        ZombieTools.log("createLevelFromFile(" + mapName + "): Lese Metadaten ein");
        ActorConfig config;
        try {
            config = new ActorConfig("maps", mapName);
        } catch (ActorConfigNotFoundException ex) {
            Guard.verifyState(false);
            return null;
        }

        // Levename
        String name = config.contains("name") ? config.get("name") : mapName;
        boolean fullView = config.contains("fullView") ? !config.get("fullView").equals("false") : false;
        boolean hasEnemies = config.contains("hasEnemies") ? !config.get("hasEnemies").equals("false") : true;

        // Suche floorChar und bgChar
        ColoredChar floorChar, bgChar;
        if (config.contains("defaulttile.char") && config.contains("defaulttile.color")) {
            floorChar = ColoredChar.create(ZombieTools.getCharFromString(config.get("defaulttile.char")), ZombieTools.getColorFromString(config.get("defaulttile.color")));
        } else {
            floorChar = DEFAULT_FLOOR_CHAR;
        }

        // Lese ItemMap ein
        ZombieTools.log("createLevelFromFile(" + mapName + "): Lese Itemmap ein");
        HashMap<Character, ITMString> itemMap = new HashMap<>();
        String[] items = getStrings(ZombieGame.getResource("maps", mapName + ".itm"));
        for (String st : items) {
            String[] it = st.split(" ", 2);
            itemMap.put(it[0].charAt(0), new ITMString(it[1]));
        }

        // Lese Map ein
        ZombieTools.log("createLevelFromFile(" + mapName + "): Lese Maps aus Mapfile");
        String[] level = getStrings(ZombieGame.getResource("maps", mapName + ".map"));
        ColoredChar[][] chars = new ColoredChar[level.length][level[0].length()];
        for (int i = 0; i < level.length; i++) {
            for (int j = 0; j < level[i].length(); j++) {
                if (level[i].charAt(j) == '.') {
                    chars[i][j] = floorChar;
                } else if (isValidChar(level[i].charAt(j))) {
                    char c = level[i].charAt(j);
                    chars[i][j] = ColoredChar.create(c, getCharSet().get(c));
                } else if (itemMap.containsKey(level[i].charAt(j))) {
                    chars[i][j] = floorChar;
                } else {
                    chars[i][j] = ColoredChar.create(level[i].charAt(j), Color.WHITE);
                }
            }
        }

        // Baue Level
        ZombieTools.log("createLevelFromFile(" + mapName + "): Erzeuge Level");
        RoomBuilder builder = new RoomBuilder(chars, floorChar);
        Level lev = new Level(builder.width(), builder.height(), builder, name, fullView, hasEnemies);

        // Lade statische Items auf Map
        ZombieTools.log("createLevelFromFile(" + mapName + "): Lade statische Items");
        for (int x = 0; x < lev.width(); x++) {
            for (int y = 0; y < lev.height(); y++) {
                char c;
                try {
                    c = level[y].charAt(x);
                } catch (StringIndexOutOfBoundsException ex) {
                    c = ' ';
                }
                if (itemMap.containsKey(c)) {
                    Set<Actor> actors = itemMap.get(c).getActorSet();
                    for (Actor a : actors) {
                        lev.addActor(a, x, y);
                    }
                }
            }
        }

        return lev;
    }

    public static String[] getStrings(InputStream input) {
        LinkedList<String> lines = new LinkedList<>();
        try {
            InputStreamReader reader = new InputStreamReader(input, "UTF-8");
            BufferedReader text = new BufferedReader(reader);
            String temp;
            while ((temp = text.readLine()) != null) {
                if (!temp.startsWith("//")) {
                    // End of Line comments done right? Jap.
                    lines.add(temp.replaceFirst("\\s*\\/\\/.*$", ""));
                }
            }
            text.close();
            reader.close();
        } catch (IOException e) {
        }
        String[] erg = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            erg[i] = lines.get(i);
        }
        return erg;
    }

    // Liest eine Datei im UTF-16 Format ein und gibt das 2-dim Feld in
    // ColoredChars zurück
    private static ColoredChar[][] readFile(InputStream input) throws IOException {
        String[] level = getStrings(input);
        ColoredChar[][] chars = new ColoredChar[level.length][level[0].length()];
        for (int i = 0; i < level.length; i++) {
            for (int j = 0; j < level[i].length(); j++) {
                chars[i][j] = ColoredChar.create(level[i].charAt(j),
                        Color.white);
            }
        }
        return chars;
    }

    private static String getFirstWordOfFile(InputStream input) {
        String firstLine = getStrings(input)[0];
        return firstLine.split(" ")[0];
    }

    public static ColoredChar[][] getImage(String imageName) throws IOException {
        return readFile(ZombieGame.getResource("screens", imageName + ".scr"));
    }
}
