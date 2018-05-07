package kr.ac.kaist.mrlab.from_the_s;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by gyuhyeon on 6/20/17.
 */
public class Configuration {

    public static int S_IN_SIZE;
    public static int O_IN_SIZE;
    public static int P_IN_SIZE;
    public static int S_OUT_SIZE;
    public static int O_OUT_SIZE;
    public static int P_OUT_SIZE;

    /**
     * Number of threads.
     */
    public static int NUMBER_OF_THREADS;

    /**
     * The root directory containing models (for corresponding properties).
     * Property names without a prefix.
     */
    public static String MODEL_ROOT;

    /**
     * Document file to extract relations from.
     */
    public static String DOCUMENT;

    /**
     * File to write extracted relations.
     */
    public static String EXTRACTS;

    /**
     * Loads a configuration.
     *
     * @param configFile Configuration file.
     * @throws IOException
     */
    public static void load(Path configFile) throws IOException {
        BufferedReader br = Files.newBufferedReader(configFile);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] kv = line.split("\\s+");
            switch (kv[0]) {
                case "S_IN_SIZE":
                    Configuration.S_IN_SIZE = Integer.parseInt(kv[1]);
                    break;
                case "O_IN_SIZE":
                    Configuration.O_IN_SIZE = Integer.parseInt(kv[1]);
                    break;
                case "P_IN_SIZE":
                    Configuration.P_IN_SIZE = Integer.parseInt(kv[1]);
                    break;
                case "S_OUT_SIZE":
                    Configuration.S_OUT_SIZE = Integer.parseInt(kv[1]);
                    break;
                case "O_OUT_SIZE":
                    Configuration.O_OUT_SIZE = Integer.parseInt(kv[1]);
                    break;
                case "P_OUT_SIZE":
                    Configuration.P_OUT_SIZE = Integer.parseInt(kv[1]);
                    break;
                case "NUMBER_OF_THREADS":
                    Configuration.NUMBER_OF_THREADS = Integer.parseInt(kv[1]);
                    break;
                case "MODEL_ROOT":
                    Configuration.MODEL_ROOT = kv[1].trim();
                    break;
                case "DOCUMENT":
                    Configuration.DOCUMENT = kv[1].trim();
                    break;
                case "EXTRACTS":
                    Configuration.EXTRACTS = kv[1].trim();
                    break;
                default:
                    break;
            }
        }
        br.close();
    }

}
