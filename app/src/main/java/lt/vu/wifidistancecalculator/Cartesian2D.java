package lt.vu.wifidistancecalculator;

public class Cartesian2D {

    private final float x;
    private final float y;

    public Cartesian2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static double distance(Cartesian2D o, Cartesian2D center) {

        double xDiffSquared = Math.pow(o.getX() - center.getX(), 2);
        double yDiffSquared = Math.pow(o.getY() - center.getY(), 2);

        return Math.sqrt(xDiffSquared + yDiffSquared);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
