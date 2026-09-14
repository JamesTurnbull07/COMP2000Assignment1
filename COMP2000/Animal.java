import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animal is-a Entity that moves and spends energy. It factors out the
 * behaviour every animal shares (movement, vision, metabolism, breeding)
 * so that Predator and Prey only need to add what makes them different.
 *
 * findNearest() is generic: it works on a List<Hawk>, List<Prey>, or any
 * mixed list, so hunting, fleeing and mate-finding reuse one search.
 *
 * moveToward()/moveAwayFrom() are each overloaded: one takes an Entity,
 * the other a raw (x, y) cell.
 */
public abstract class Animal extends Entity {
    protected static final double METABOLISM = 1.0;

    protected double health;
    protected final double maxHealth;
    protected int speed;
    protected int visionRadius;
    private boolean bredThisTick = false;
    protected int age = 0;

    public Animal(int x, int y, double startHealth, int speed, int visionRadius) {
        super(x, y);
        this.health = startHealth;
        this.maxHealth = startHealth * 2;
        this.speed = speed;
        this.visionRadius = visionRadius;
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    /**
     * Well fed enough to be worth calling fed - drives speed and foraging urgency.
     */
    protected boolean isWellFed() {
        return health > maxHealth * 0.5;
    }

    protected void feed(double amount) {
        health = Math.min(health + amount, maxHealth);
    }

    // --- breeding -----------------------------------------------------

    /** Fraction of maxHealth needed before this animal will breed. */
    protected double breedThreshold() {
        return maxHealth * 0.30;
    }

    /** Fraction of maxHealth each parent pays per offspring. */
    protected double breedCost() {
        return maxHealth * 0.20;
    }

    protected boolean canBreed() {
        return !bredThisTick && health >= breedThreshold();
    }

    /** ticks an animal can live before dying of old age, species can override. */
    protected int maxAge() {
        return 300;
    }

    void resetBreedFlag() {
        bredThisTick = false;
    }

    private void payBreedCost() {
        health -= breedCost();
        bredThisTick = true;
    }

    /** Each concrete species knows how to make one of itself. */
    protected abstract Animal newOffspring(int x, int y);

    /**
     * Breed with one eligible neighbour of the same species. At most one
     * offspring per animal per tick, otherwise a crowded cell would produce
     * a birth for every pair in it.
     */
    protected void tryBreed(World world) {
        if (!canBreed())
            return;
        for (Animal other : world.getGrid().occupantsWithin(getX(), getY(), 1, Animal.class)) {
            if (other == this || other.getClass() != getClass() || !other.canBreed())
                continue;
            try {
                Animal child = world.spawnNear(this);
                payBreedCost();
                other.payBreedCost();
                // The child is made out of what its parents just paid, minus a
                // loss. Without this, breeding would create energy from nothing
                // and every population would grow without limit.
                child.health = (breedCost() + other.breedCost()) * 0.6;
            } catch (SpawnException e) {
                // No room next to the parent this tick; neither parent pays.
            }
            return;
        }
    }

    /**
     * Closest breedable animal of this species, or null. Searched over a wider
     * radius than ordinary vision: at low population density two adults would
     * otherwise almost never meet, and the species dies out with food to spare.
     */
    protected Animal nearestMate(World world) {
        int range = visionRadius * 3;
        List<Animal> mates = new ArrayList<>();
        for (Animal a : world.getGrid().occupantsWithin(getX(), getY(), range, Animal.class)) {
            if (a != this && a.getClass() == getClass() && a.canBreed())
                mates.add(a);
        }
        Animal nearest = null;
        int best = Integer.MAX_VALUE;
        for (Animal m : mates) {
            int d = distanceTo(m);
            if (d < best) {
                best = d;
                nearest = m;
            }
        }
        return nearest;
    }

    // --- movement -----------------------------------------------------

    protected void moveToward(Entity target) {
        moveToward(target.getX(), target.getY());
    }

    /** Overload: move toward a raw cell instead of a whole Entity. */
    protected void moveToward(int targetX, int targetY) {
        int[] d = stepToward(targetX - getX(), targetY - getY());
        setPosition(getX() + d[0], getY() + d[1]);
    }

    protected void moveAwayFrom(Entity threat) {
        moveAwayFrom(threat.getX(), threat.getY());
    }

    /** Overload: flee a raw cell instead of a whole Entity. */
    protected void moveAwayFrom(int threatX, int threatY) {
        int[] d = stepToward(getX() - threatX, getY() - threatY);
        setPosition(getX() + d[0], getY() + d[1]);
    }

    /**
     * How this animal covers a (dx, dy) gap in one tick, as {stepX, stepY}.
     * The default is free movement in any of 8 directions; Fox and Hawk
     * override it with their own restricted movement.
     */
    protected int[] stepToward(int dx, int dy) {
        return new int[] { clampStep(dx), clampStep(dy) };
    }

    /** At most `speed` cells along an axis, never overshooting. */
    protected int clampStep(int delta) {
        return Math.max(-speed, Math.min(speed, delta));
    }

    /** Takes the World to reach its seeded Random. */
    protected void wander(World world) {
        Random rng = world.getRandom();
        setPosition(getX() + rng.nextInt(3) - 1, getY() + rng.nextInt(3) - 1);
    }

    /** Generic search: finds the closest living candidate within vision range. */
    protected <T extends Entity> T findNearest(List<T> candidates) {
        T nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (T candidate : candidates) {
            if (!candidate.isAlive())
                continue;
            int d = this.distanceTo(candidate);
            if (d < bestDist) {
                bestDist = d;
                nearest = candidate;
            }
        }
        return (bestDist <= visionRadius) ? nearest : null;
    }

    @Override
    public void update(World world) {
        // World filters the dead out before calling this. If that ever stops
        // being true, fail here rather than let a corpse move and eat.
        if (!isAlive()) {
            throw new IllegalStateException("update() called on a dead " + getClass().getSimpleName());
        }
        health -= METABOLISM;
        if (health <= 0) {
            kill(DeathCause.STARVED);
            return;
        }
        age++;
        if (age >= maxAge()) {
            kill(DeathCause.OLD_AGE);
            return;
        }
        act(world);
    }

    /** Each concrete animal decides what "acting" means for it. */
    protected abstract void act(World world);

    @Override
    public String toString() {
        return super.toString() + String.format(", health %.0f", health);
    }
}
