# COMP2000 Worksheet 1 — Mid-Semester Submission

**Student name:** James Turnbull

**Student ID:** 49162713

**GitHub repo URL:** github.com/JamesTurnbull07/COMP2000Assignment1
---

## 1. Version Control

**1.1.** Paste the first 10 lines of the output of `git log --graph --oneline --all` from your repository:

```
* 74d846e (HEAD -> main, origin/main, origin/HEAD) Added log book                          
* c905984 Added worksheet
* 30cdd0c Cleaned up unused files
* dc09f65 Add validation for superFoodChance range
* 6e6a811 Add old age as a third cause of death for animals
* d037d35 Make SuperFood spawn chance configurable, with validation
* 2ffd228 Added Superfood. 3x for effective than normal food. 5% spawn rate
* e53e745 Sync submitted worksheet and log book
* 0c0f2c3 Refresh commit log and counts in worksheet section 1
```

**1.2.** Describe your workflow. Did you use branches? Pull requests?

I worked directly on main without creating branches, since most of my contribution was made solo and after most of the team's structural work was already done. Each of my commits covers one distinct change, with a commit message describing what that change did.




**1.3.** Estimate the percentage of commits you contributed relative to the total in your repository.

My own commits make up roughly 20% of the total commit history. The rest come from the team before I forked the repository. My commits add many new features such as a SuperFood class and a old age cause of death for the animals.



---

## 2. Program Design

Don't forget to submit a pdf file of your program design along with this file.

**2.1.** List every class in your project and write 1–2 sentences describing its responsibility.

Entity - abstract base class for everything on the grid. holds position, alive/dead state, and death cause.

Animal - abstract class extending Entity. shares behaviour common to every animal, like health, movement, breeding, metabolism, and aging.

Predator - abstract class extending Animal. adds hunting and eating logic for animals that eat prey.

Prey - abstract class extending Animal. adds fleeing behaviour and the ability to eat Food.

Hawk - a concrete predator with its own distinct movement pattern.

Fox - a concrete predator with its own distinct movement pattern.

Rabbit - a concrete prey animal, larger and slower to reproduce.

Mouse - a concrete prey animal, smaller and faster to reproduce.

Food - basic edible entity that restores health when eaten.

SuperFood - a rare, higher value variant of Food.

Edible - interface for anything that provides nutrition. implemented by both Food and Prey.

Grid<T extends Entity> - a generic spatial index used for fast neighbour lookups.

World - owns the whole simulation, runs each tick, and tracks stats and history.

SimulationConfig - holds every tunable parameter and validates them up front.

SimulationConfigException - thrown when a config value is invalid.

SpawnException - thrown when there's no room to place a new animal.

DeathCause - enum for why an animal died. starved, eaten, old age, unknown.

SimulationState - enum for the overall state of a run.

SimPanel - draws the grid and entities using Swing.

GraphPanel - draws the population history graph using Swing.

Main - entry point. sets up the window and runs the simulation loop.

Ablation, ChaseTest, ConfigTest, GridTest, Render, Series - test/utility harnesses, not part of the simulation itself.




**2.2.** Identify any inheritance relationships. For each parent–child pair, list what the child inherits and what it overrides.

Entity - Animal: Animal inherits position, alive/dead state and death cause tracking from Entity. It overrides update() to add health, metabolism and aging on top of the base entity behaviour.

Animal - Predator: Predator inherits health, movement, and breeding from Animal. It doesn't override act() itself bit it does add the new behaviour tryEat() - for hunting prey.

Animal - Prey: Prey inherits the same base behaviour as Predator, but adds fleeing with evade() and eating Food with tryEatFood() and implements Edible since predators can eat prey.

Predator - Hawk / Fox: both inherit hunting/eating logic from Predator. Each overrides stepToward() to give itself a distinct movement pattern. The Hawk moves like a bishop and rests every 4th tick. The fox moves like a rook.

Prey - Rabbit / Mouse: both inherit fleeing and eating logic from Prey. Each overrides breedThreshold() and breedCost() differently, reflecting different reproductive strategies. The rabbit is slower/costlier to breed and the mouse is faster/cheaper.

Food - SuperFood: SuperFood inherits everything from Food including being spawned, drawn, and eaten the same way. It overrides getNutritionValue(), getLabel() and getColor() to be worth more and look different, without needing any other class to know it exists.


**2.3.** Pick the class that you think has the best design. Explain why.

I think SimulationConfig is the best designed class in the project. Every setting the simulation uses grid size, how many animals to start with, how often things spawn, and so on is able to be kept in one place instead of being spread out across different files as random numbers. Before the simulation runs, it checks every single setting is valid, and if something's wrong it stops straight away with a clear error message, instead of the game just breaking somewhere later for no obvious reason. This makes it simple and easier to add to later on. 



**2.4.** Paste one code snippet that demonstrates your use of polymorphism or encapsulation.  Include an explanation of _how_ this demonstrates polymorphim or encapsulation.  Give a reference to a provided reading that talks about this type of polymorphism or encapsulation.

public class SuperFood extends Food {
    private static final double SUPER_NUTRITION_VALUE = 120;

    @Override
    public double getNutritionValue() {
        return SUPER_NUTRITION_VALUE;
    }
} 

This demonstrates polymorphism through method overriding. Prey.java calls food.getNutritionValue() without knowing or caring whether food is a Food or a SuperFood. At runtime, Java automatically calls whichever version actually belongs to the object so SuperFoods overridden method runs and returns 120 instead of foods default 40. No other class needed to be changed to support this. The existing code just works differently for the new subclass automatically. This is runtime polymorphism. Reference: Learning Java, 3rd edition, Chapter 6, "Subclassing and Inheritance," section "Overriding Methods" — the Week 4 prescribed reading.



---

## 3. Generics and Exceptions

**3.1.** List every place your code uses generics (e.g. `ArrayList<Actor>`, `Optional<Cell>`, `HashMap<String, Team>`). If you deliberately used none, explain why.

Grid<T extends Entity> - a generic spatial index that works for any kind of entity. The bound means it can only ever hold entities, so it stays type safe while still being reusable for anything else.

Grids generic method <U extends T> List<U> occupantsWithin() and lets you search for a more specific subtype within a more general grid.

Animal.findNearest(), protected <T extends Entity> T findNearest(List<T> candidates) - works on a List<Hawk>, List<Prey>, or any other list of entities, without needing a separate near-duplicate method for each type.

List<Hawk>, List<Fox>, List<Rabbit>, List<Mouse>, List<Food> - each collection is restricted to the type its meant to hold, so the compiler catches mistakes before the program even runs.

SimulationConfig's Map<String, Long> - holds every setting by name, type checked at compile time.



**3.2.** List every place your code handles exceptions (try/catch, throws, custom exception classes). What error is each protecting against?

SimulationConfigException - thrown throughout SimulationConfig whenever a setting is invalid. Negative values, malformed command-line arguments, too many animals to fit on the grid, no prey animals at all, or if the  superFoodChance outside 0-100. This protects against the simulation starting in a broken state.

SpawnException - thrown by World.spawnNear() when theres genuinely no valid space left to place a new animal. This protects against failing to spawn an animal or placing it somewhere invalid.

Main.java catches SimulationConfigException at startup. If the config is invalid, the program prints a clear error and exits cleanly instead of crashing with a raw stack trace or continuing to run in a bad state.

Grid.cellAt() deliberately does not catch its IndexOutOfBoundsException. Its documented as "a caller bug, not an expected condition," so its left as an unchecked exception on purpose, to fail loudly during development rather than being silently swallowed.




**3.3.** Paste a code snippet showing either a generic class/method or a try/catch block.

if (values.get("superFoodChance") < 0 || values.get("superFoodChance") > 100) {
    throw new SimulationConfigException(
        "superFoodChance must be between 0 and 100, got " + values.get("superFoodChance"));
}

This is inside SimulationConfig.validate(). It checks that my superFoodChance setting is a sensible percentage before the simulation ever starts, throwing SimulationConfigException with a clear message if it is not. 



---

## 4. Log Book

Don't forget to submit an electronic version of your logbook.

**4.1.** Which week's activity taught you the most? What did you learn?

Week 5, generics and type erasure, taught me the most. At the time, my group worked on the Container exercise in class and could not get it to throw. I had to leave the class early before seeing the solution. Coming back to it later and finally understanding why storing the container using its raw type let me bypass the type checking, and why the error only shows up later when the item is read back out. It also directly explains why classes like Grid<T extends Entity> are useful in the actual project. 

## 5. Uniqueness and Creativity

**5.1.** List everything you added to the project that was not part of the in-class activities.
SuperFood - a rare variant of Food, overriding getNutritionValue(), getLabel(), and getColor()

superFoodChance - a new configurable setting in SimulationConfig controlling how often SuperFood spawns, with validation ensuring it stays between 0 and 100

OLD_AGE - a new value added to the DeathCause enum, plus the aging logic in Animal that tracks each animals age and kills it once it passes maxAge() 


**5.2.** Which feature required the most independent research or problem-solving? What did you learn from it?

Making superFoodChance configurable required the most independent problem-solving. The challenge was  making sure the new setting behaved consistently with the rest of the system as well as using the same Map<String, Long> storage, adding validation in the same place other checks live, and exposing it through a getter the same way hasSafeZone() does. I also had to trace how SimulationConfigException actually gets handled to be confident my new validation would actually stop a bad value at startup rather than just existing as dead code that never ran. 

**5.3.** Paste one code snippet that you are especially proud of. Explain why it goes beyond what was done in class.

public void update(World world) {
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

Im most proud of the aging mechanic and making maxAge() an overridable method rather than a fixed constant. I believe that this is a genuininly interesting change that makes the project more realistic. I noticed the existing code already had a pattern for per species differences such as breedThreshold() and breedCost(), are overridden differently by Rabbit, Mouse, Hawk, and Fox. Rather than hardcoding a single lifespan for every animal, I followed that same pattern so any species could eventually have its own lifespan just by overriding one method. It goes beyond the in class activities in a few ways. Its not confined to one class> It required coordinated changes across Animal , DeathCause , and World , so I had to keep three files consistent with each other rather than making an isolated change.