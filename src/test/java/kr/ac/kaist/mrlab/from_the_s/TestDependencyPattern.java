package kr.ac.kaist.mrlab.from_the_s;

import org.junit.Test;

/**
 * Created by gyuhyeon on 6/20/17.
 */
public class TestDependencyPattern {

    @Test
    public void testToString() {
        DependencyPattern dp1 = new DependencyPattern(new String[] {"A", "B", "C", "D"});
        DependencyPattern dp2 = new DependencyPattern(new String[] {"A", "B", "_", "_"});
        System.out.println(dp1);
        System.out.println(dp2);
    }

    @Test
    public void testEquals() {
        DependencyPattern dp1 = new DependencyPattern(new String[] {"A", "B", "C", "D"});
        DependencyPattern dp2 = new DependencyPattern(new String[] {"A", "B", "C", "D"});
        DependencyPattern dp3 = new DependencyPattern(new String[] {"A", "B", "C", "E"});
        System.out.println(dp1.equals(dp2));
        System.out.println(dp1.equals(dp3));
        System.out.println(dp2.equals(dp1));
        System.out.println(dp2.equals(dp3));
        System.out.println(dp3.equals(dp1));
        System.out.println(dp3.equals(dp2));
    }

}
