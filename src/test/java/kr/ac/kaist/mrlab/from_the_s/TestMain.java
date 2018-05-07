package kr.ac.kaist.mrlab.from_the_s;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Created by gyuhyeon on 6/20/17.
 */
public class TestMain {

    @Test
    public void testMain() throws IOException {
        for (Path modelPath : Files.list(Paths.get("data/test/models.previous"))
                .collect(Collectors.toList())) {
            if (!Files.exists(Paths.get("data/models", modelPath.getFileName().toString()))) {
                continue;
            }

            HashMap<String, Integer> elem2type = new HashMap<>();
            BufferedReader br = Files.newBufferedReader(Paths.get(modelPath.toString(), "attribute.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tra = line.split("\t");
                int type = Integer.parseInt(tra[0]);
                tra[2] = tra[2].replace("%SBJ%", "SBJ");
                tra[2] = tra[2].replace("%OBJ%", "OBJ");
                if (type > 0) {
                    elem2type.put(tra[2], type);
                }
            }
            br.close();

            ArrayList<String> lines = new ArrayList<>();
            br = Files.newBufferedReader(Paths.get("data/models", modelPath.getFileName().toString(), "elements.txt"));
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tef = line.split("\t");
                if (elem2type.containsKey(tef[1])) {
                    lines.add(elem2type.get(tef[1]) + "\t" + tef[1] + "\t" + tef[2]);
                } else {
                    lines.add(line);
                }
            }
            br.close();

            BufferedWriter bw = Files.newBufferedWriter(Paths.get("data/models", modelPath.getFileName().toString(), "elements.txt"));
            for (String newLine : lines) {
                bw.write(newLine + "\n");
            }
            bw.close();
        }
    }

}
