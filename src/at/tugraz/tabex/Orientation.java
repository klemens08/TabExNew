package at.tugraz.tabex;

public enum Orientation {
    Portrait(0), Landscape(1);

    private final int id;
    Orientation(int id) { this.id = id; }
    public int getValue() { return id; }
}
