import java.util.ArrayList;
import java.util.Collections;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

/**
 * World owns every entity and drives the simulation. It stores each kind
 * of entity in its own typed List (List<Hawk>, List<Mouse>, etc.) so
 * animals can query "give me all the mice" without casting, and it also
 * offers allEntities() as a single List<Entity> for generic operations
 * like drawing or the main update loop. width/height are in cells.
 *
 * World owns the one seeded Random the simulation draws from, so a given
 * seed always replays the same run.
 */
public class World {
    private final int width, height;
    private final long seed;
    private final Random rng;
    private final List<Hawk> hawks = new ArrayList<>();
    private final List<Fox> foxes = new ArrayList<>();
    private final List<Rabbit> rabbits = new ArrayList<>();
    private final List<Mouse> mice = new ArrayList<>();
    private final List<Food> food = new ArrayList<>();
    private final Grid<Entity> grid;
    private final SimulationConfig config;
    private int tickCount = 0;
    private int maxTicks = 0; // 0 = no limit
    private SimulationState state = SimulationState.RUNNING;

    private final Map<String, List<Integer>> history = new LinkedHashMap<>();
    private int starved = 0, eaten = 0, births = 0, hops = 0, oldAge = 0;;

    // A rectangular refuge that Predators are physically barred from entering.
    // Rabbit and Mouse are ordinary Prey, so nothing stops them going in.
    private final int zoneX, zoneY, zoneWidth, zoneHeight;
    private final boolean safeZoneEnabled;

    /** Convenience for tests: default settings on a grid of this size. */
    public World(int width, int height, long seed) {
        this(SimulationConfig.defaults(), width, height, seed);
    }

    public World(SimulationConfig config) {
        this(config, config.getCols(), config.getRows(), config.getSeed());
    }

    private World(SimulationConfig config, int width, int height, long seed) {
        this.config = config;
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.maxTicks = config.getMaxTicks();
        // Mixed, not passed straight in: new Random(n) for small consecutive n
        // produces near-identical first draws, so seeds 1 and 2 would open alike.
        this.rng = new Random(seed * 6364136223846793005L + 1442695040888963407L);
        this.grid = new Grid<>(width, height);
        for (String species : speciesColours().keySet()) history.put(species, new ArrayList<>());
        this.safeZoneEnabled = config.hasSafeZone();
        this.zoneWidth = (int) (width * 0.28);
        this.zoneHeight = (int) (height * 0.38);
        this.zoneX = width - zoneWidth - 1;
        this.zoneY = height - zoneHeight - 1;
    }

    public Grid<Entity> getGrid() { return grid; }
    public SimulationState getState() { return state; }
    public SimulationConfig getConfig() { return config; }
    public int getTickCount() { return tickCount; }
    public void setMaxTicks(int t) { this.maxTicks = t; }

    public Map<String, List<Integer>> getHistory() { return history; }
    public int getStarved() { return starved; }
    public int getEaten() { return eaten; }
    public int getBirths() { return births; }
    public int getHops() { return hops; }

    void recordHop() { hops++; }

    /** Each species reports its own colour, so the graph always matches the dots. */
    public Map<String, Color> speciesColours() {
        Map<String, Color> m = new LinkedHashMap<>();
        m.put("Hawks", new Hawk(0, 0).getColor());
        m.put("Foxes", new Fox(0, 0).getColor());
        m.put("Rabbits", new Rabbit(0, 0).getColor());
        m.put("Mice", new Mouse(0, 0).getColor());
        m.put("Food", new Food(0, 0).getColor());
        return m;
    }

    private void recordHistory() {
        history.get("Hawks").add(hawks.size());
        history.get("Foxes").add(foxes.size());
        history.get("Rabbits").add(rabbits.size());
        history.get("Mice").add(mice.size());
        history.get("Food").add(food.size());
    }
    public Random getRandom() { return rng; }
    public long getSeed() { return seed; }

    public boolean hasSafeZone() { return safeZoneEnabled; }

    public boolean isInSafeZone(int px, int py) {
        return safeZoneEnabled
            && px >= zoneX && px <= zoneX + zoneWidth
            && py >= zoneY && py <= zoneY + zoneHeight;
    }

    public int getZoneCentreX() { return zoneX + zoneWidth / 2; }
    public int getZoneCentreY() { return zoneY + zoneHeight / 2; }

    public int getZoneX() { return zoneX; }
    public int getZoneY() { return zoneY; }
    public int getZoneWidth() { return zoneWidth; }
    public int getZoneHeight() { return zoneHeight; }

    public List<Hawk> getHawks() { return hawks; }
    public List<Fox> getFoxes() { return foxes; }
    public List<Rabbit> getRabbits() { return rabbits; }
    public List<Mouse> getMice() { return mice; }
    public List<Food> getFood() { return food; }

    public void addHawk(Hawk h) { hawks.add(h); }
    public void addFox(Fox f) { foxes.add(f); }
    public void addRabbit(Rabbit r) { rabbits.add(r); }
    public void addMouse(Mouse m) { mice.add(m); }
    public void addFood(Food f) { food.add(f); }

    /** Places one offspring next to its parent. Throws if there is no room inside the world. */
    public Animal spawnNear(Animal parent) throws SpawnException {
        int x = parent.getX() + rng.nextInt(3) - 1;
        int y = parent.getY() + rng.nextInt(3) - 1;
        if (!grid.contains(x, y)) {
            throw new SpawnException("Spawn point (" + x + ", " + y + ") is out of bounds");
        }
        Animal child = parent.newOffspring(x, y);
        register(child);
        births++;
        return child;
    }

    private void register(Animal a) {
        if (a instanceof Hawk) hawks.add((Hawk) a);
        else if (a instanceof Fox) foxes.add((Fox) a);
        else if (a instanceof Rabbit) rabbits.add((Rabbit) a);
        else if (a instanceof Mouse) mice.add((Mouse) a);
    }

    public List<Entity> allEntities() {
        List<Entity> all = new ArrayList<>();
        all.addAll(hawks);
        all.addAll(foxes);
        all.addAll(rabbits);
        all.addAll(mice);
        all.addAll(food);
        return all;
    }

    public void update() {
        if (state != SimulationState.RUNNING) return;
        tickCount++;
        rebuildGrid();

        // Shuffled every tick: allEntities() returns hawks, then foxes, then
        // prey, so a fixed order would hand predators the first move forever.
        List<Entity> actors = allEntities();
        Collections.shuffle(actors, rng);

        for (Entity e : actors) {
            if (e.isAlive()) {
                e.update(this);
                clampToBounds(e);
                keepPredatorsOutOfZone(e);
            }
        }
        removeDead();

        for (int i = 0; i < config.getFoodPerTick() && food.size() < config.getMaxFood(); i++) {
    // superfood spawns 5% of the time and is worth 3 times more than normal food
    boolean isSuper = rng.nextInt(100) < config.getSuperFoodChance();
    Food f = isSuper ? new SuperFood(rng.nextInt(width), rng.nextInt(height))
                      : new Food(rng.nextInt(width), rng.nextInt(height));
    food.add(f);
}

        recordHistory();
        state = evaluateState();
    }

    /** Extinction is an expected outcome, so it is returned, never thrown. */
    private SimulationState evaluateState() {
        if (rabbits.isEmpty() && mice.isEmpty()) return SimulationState.PREY_EXTINCT;
        if (hawks.isEmpty() && foxes.isEmpty()) return SimulationState.PREDATORS_EXTINCT;
        if (maxTicks > 0 && tickCount >= maxTicks) return SimulationState.TIME_LIMIT;
        return SimulationState.RUNNING;
    }

    /**
     * Rebuilt once per tick, before anything moves, so every animal perceives
     * the same world state regardless of where it falls in the update order.
     */
    private void rebuildGrid() {
        grid.clear();
        for (Entity e : allEntities()) {
            if (!e.isAlive()) continue;
            if (e instanceof Animal) ((Animal) e).resetBreedFlag();
            if (grid.contains(e.getX(), e.getY())) grid.add(e);
        }
    }

    /** setPosition() is protected on Entity, but World is a same-package collaborator, so it can call it. */
    private void clampToBounds(Entity e) {
        int clampedX = Math.max(0, Math.min(e.getX(), width - 1));
        int clampedY = Math.max(0, Math.min(e.getY(), height - 1));
        e.setPosition(clampedX, clampedY);
    }

    /** If a Predator ended its move inside the safe zone, push it back out to the nearest edge. */
    private void keepPredatorsOutOfZone(Entity e) {
        if (!(e instanceof Predator)) return;
        if (!isInSafeZone(e.getX(), e.getY())) return;

        int distLeft = e.getX() - zoneX;
        int distRight = (zoneX + zoneWidth) - e.getX();
        int distTop = e.getY() - zoneY;
        int distBottom = (zoneY + zoneHeight) - e.getY();
        int nearest = Math.min(Math.min(distLeft, distRight), Math.min(distTop, distBottom));

        if (nearest == distLeft) e.setPosition(zoneX - 1, e.getY());
        else if (nearest == distRight) e.setPosition(zoneX + zoneWidth + 1, e.getY());
        else if (nearest == distTop) e.setPosition(e.getX(), zoneY - 1);
        else e.setPosition(e.getX(), zoneY + zoneHeight + 1);
    }

    private void removeDead() {
        for (Entity e : allEntities()) {
            if (e.isAlive() || !(e instanceof Animal)) continue;
            if (e.getDeathCause() == DeathCause.STARVED) starved++;
            else if (e.getDeathCause() == DeathCause.EATEN) eaten++;
            else if (e.getDeathCause() == DeathCause.OLD_AGE) oldAge++;
        }
        hawks.removeIf(h -> !h.isAlive());
        foxes.removeIf(f -> !f.isAlive());
        rabbits.removeIf(r -> !r.isAlive());
        mice.removeIf(m -> !m.isAlive());
        food.removeIf(f -> !f.isAlive());
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
