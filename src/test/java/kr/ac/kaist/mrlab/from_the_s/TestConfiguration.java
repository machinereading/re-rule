package kr.ac.kaist.mrlab.from_the_s;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;

/**
 * Created by gyuhyeon on 6/21/17.
 */
public class TestConfiguration {

    @Test
    public void testLoad() throws IOException, IllegalAccessException {
        Configuration.load(Paths.get("./FromTheS.conf"));

        for (Field filed : Configuration.class.getDeclaredFields()) {
            System.out.println(filed.getName() + ": " + filed.get(new Configuration()));
        }
    }

}
