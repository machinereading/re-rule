package kr.ac.kaist.mrlab.from_the_s;

/**
 * Created by gyuhyeon on 6/20/17.
 */
public class DependencyPattern {

    private String[] elems;

    /**
     * Instantiates {@link kr.ac.kaist.mrlab.from_the_s.DependencyPattern} and
     * sets elements.
     *
     * @param elements Elements.
     */
    public DependencyPattern(String[] elements) {
        this.elems = elements;
    }

    /**
     * Gets elements.
     *
     * @return Elements to get.
     */
    public String[] getElements() {
        return this.elems;
    }

    /**
     * Sets elements.
     *
     * @param elems Elements to set.
     */
    public void setElements(String[] elems) {
        this.elems = elems;
    }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return String.join("\t", this.elems).hashCode();
    }

    @Override
    public String toString() {
        String str = "[ ";
        for (int i = 0; i < this.elems.length - 1; i++) {
            str += this.elems[i] + " | ";
        }
        str += this.elems[this.elems.length - 1] + " ]";

        return str;
    }
    
}
