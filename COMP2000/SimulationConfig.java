import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every number the simulation starts with, in one place, validated once.
 *
 * Values are read from key=value arguments, so a run can be varied without
 * recompiling: java Main seed=42 rabbits=40 foodPerTick=3
 */
public class SimulationConfig {
    private final Map<String, Long> values = new LinkedHashMap<>();

    private static final Map<String, Long> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("seed", 0L); // 0 means "pick one from the clock"
        DEFAULTS.put("cols", 40L);
        DEFAULTS.put("rows", 30L);
        DEFAULTS.put("hawks", 2L);
        DEFAULTS.put("foxes", 3L);
        DEFAULTS.put("rabbits", 25L);
        DEFAULTS.put("mice", 30L);
        DEFAULTS.put("food", 40L);
        DEFAULTS.put("foodPerTick", 2L);
        DEFAULTS.put("maxFood", 120L);
        DEFAULTS.put("maxTicks", 0L); // 0 means no limit
        DEFAULTS.put("tickMs", 150L); // milliseconds between ticks
        DEFAULTS.put("safeZone", 1L); // 1 = refuge on, 0 = no refuge
        DEFAULTS.put("superFoodChance", 5L); // percent chance each spawned food is a superfood
    }

    private SimulationConfig() {
        values.putAll(DEFAULTS);
    }

    public static SimulationConfig defaults() {
        SimulationConfig c = new SimulationConfig();
        c.values.put("seed", System.currentTimeMillis());
        return c;
    }

    /** Parses key=value arguments over the defaults, then validates the result. */
    public static SimulationConfig fromArgs(String[] args) throws SimulationConfigException {
        SimulationConfig c = new SimulationConfig();
        for (String arg : args) {
            int eq = arg.indexOf('=');
            if (eq < 1) {
                throw new SimulationConfigException(
                    "Expected key=value but got '" + arg + "'. Known keys: " + DEFAULTS.keySet());
            }
            String key = arg.substring(0, eq);
            String raw = arg.substring(eq + 1);
            if (!DEFAULTS.containsKey(key)) {
                throw new SimulationConfigException(
                    "Unknown setting '" + key + "'. Known keys: " + DEFAULTS.keySet());
            }
            try {
                c.values.put(key, Long.parseLong(raw));
            } catch (NumberFormatException e) {
                // Rethrown as our own type: the caller cares that the config was
                // bad, not that a particular parse failed deep inside here.
                throw new SimulationConfigException(
                    "Value for '" + key + "' must be a whole number, got '" + raw + "'", e);
            }
        }
        if (c.values.get("seed") == 0L) c.values.put("seed", System.currentTimeMillis());
        c.validate();
        return c;
    }

    private void validate() throws SimulationConfigException {
        requirePositive("cols");
        requirePositive("rows");
        requirePositive("tickMs");
        for (String key : new String[]{"hawks", "foxes", "rabbits", "mice", "food",
                                       "foodPerTick", "maxFood", "maxTicks"}) {
            if (values.get(key) < 0) {
                throw new SimulationConfigException(key + " cannot be negative, got " + values.get(key));
            }
        }
        long cells = getCols() * (long) getRows();
        long animals = getHawks() + getFoxes() + getRabbits() + getMice();
        if (animals + getFood() > cells) {
            throw new SimulationConfigException("Cannot place " + (animals + getFood())
                + " entities on a " + getCols() + "x" + getRows() + " grid (" + cells + " cells)");
        }
        if (getRabbits() + getMice() == 0) {
            throw new SimulationConfigException("Need at least one prey animal or the run ends immediately");
        }
        if (getMaxFood() < getFoodPerTick()) {
            throw new SimulationConfigException("maxFood (" + getMaxFood()
                + ") is below foodPerTick (" + getFoodPerTick() + "), so food could never accumulate");
        }
    }

    private void requirePositive(String key) throws SimulationConfigException {
        if (values.get(key) <= 0) {
            throw new SimulationConfigException(key + " must be greater than zero, got " + values.get(key));
        }
    }

    private int intOf(String key) { return values.get(key).intValue(); }

    public long getSeed()        { return values.get("seed"); }
    public int getCols()         { return intOf("cols"); }
    public int getRows()         { return intOf("rows"); }
    public int getHawks()        { return intOf("hawks"); }
    public int getFoxes()        { return intOf("foxes"); }
    public int getRabbits()      { return intOf("rabbits"); }
    public int getMice()         { return intOf("mice"); }
    public int getFood()         { return intOf("food"); }
    public int getFoodPerTick()  { return intOf("foodPerTick"); }
    public int getMaxFood()      { return intOf("maxFood"); }
    public int getMaxTicks()     { return intOf("maxTicks"); }
    public int getTickMs()       { return intOf("tickMs"); }
    public boolean hasSafeZone() { return values.get("safeZone") != 0L; }
    public int getSuperFoodChance() { return intOf("superFoodChance"); }

    @Override
    public String toString() { return values.toString(); }
}
