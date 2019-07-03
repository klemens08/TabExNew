package at.tugraz.tabex;

import java.util.ArrayList;

public class CustomList<E> extends ArrayList<E> {

    public E first() {
        return this.get(0);
    }

    public E last() {
        return this.get(this.size() - 1);
    }
}
