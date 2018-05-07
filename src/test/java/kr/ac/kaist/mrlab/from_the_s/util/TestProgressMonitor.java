package kr.ac.kaist.mrlab.from_the_s.util;

import org.junit.Test;

import java.util.ArrayList;

/**
 * Created by gyuhyeon on 6/20/17.
 */
public class TestProgressMonitor {

    @Test
    public void testUpdate() throws InterruptedException {
        ArrayList<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            numbers.add(i);
        }

        ProgressMonitor pm = new ProgressMonitor("Simply Counting", numbers.size());
        for (int i = 0; i < 10000; i++) {
            pm.update();
            Thread.sleep(1);
        }
    }

}
