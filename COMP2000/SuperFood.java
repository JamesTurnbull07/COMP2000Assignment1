import java.awt.Color;

/**
 * SuperFood is a rare variant of Food. It is worth three times as much 
 * as ordinary food and only spawns 5 percent of the time. this can be seen in
 * World.spawnFood(). This rewards the animal that reaches it first
 * without drastically changing the overall food supply.
 */ 
public class SuperFood extends Food {
    private static final double SUPER_NUTRITION_VALUE = 120;

    public SuperFood(int x, int y) {
        super(x, y);
    }

    @Override
    public double getNutritionValue() {
        return SUPER_NUTRITION_VALUE;
    }

    @Override
    public String getLabel() { return "$"; }

    @Override
    public Color getColor() { return Color.YELLOW; }
}