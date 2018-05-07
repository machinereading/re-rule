package kr.ac.kaist.mrlab.from_the_s;

import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by gyuhyeon on 6/20/17.
 */
public class TestDPMatcher {

    @Test
    public void test() throws IOException, InterruptedException, ParseException {
        Configuration.S_IN_SIZE = 2;
        Configuration.O_IN_SIZE = 2;
        Configuration.P_IN_SIZE = 1;
        Configuration.S_OUT_SIZE = 3;
        Configuration.O_OUT_SIZE = 3;
        Configuration.P_OUT_SIZE = 4;
        Configuration.NUMBER_OF_THREADS = 1;
        Configuration.MODEL_ROOT = "data/test/models";
        Configuration.DOCUMENT = "data/test/wiki_ex_PL1.txt";
        Configuration.EXTRACTS = "data/test/extracted-relations.txt";

        new DPMatcher().extractAll();
    }

}
