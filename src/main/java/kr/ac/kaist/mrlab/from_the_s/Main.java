package kr.ac.kaist.mrlab.from_the_s;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by gyuhyeon on 6/20/17.
 */
public class Main {

    /**
     * The main function of the project {@link kr.ac.kaist.mrlab.from_the_s}.
     *
     * @param args Arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        Configuration.load(Paths.get("FromTheS.conf"));

        boolean help = false;
        boolean generatePatterns = false;
        boolean annotatePatterns = false;
        boolean extractRelations = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help")) help = true;
            else if (args[i].equals("--generate-patterns")) generatePatterns = true;
            else if (args[i].equals("--annotate-patterns")) annotatePatterns = true;
            else if (args[i].equals("--extract-relations")) extractRelations = true;
        }

        if (help) Main.help();
        if (generatePatterns) Main.generatePatterns();
        if (annotatePatterns) Main.annotatePatterns();
        if (extractRelations) Main.extractRelations();
    }

    private static void help() {
        System.out.printf("    %-25sprints this help message.\n\n", "--help");

        System.out.printf("    %-25sgenerates patterns for models\n\n", "--generate-patterns");

        System.out.printf("    %-25sannotates patterns of models\n\n", "--annotate-patterns");

        System.out.printf("    %-25sextracts relations from a document\n\n", "--extract-relations");
    }

    private static void generatePatterns() throws IOException, InterruptedException {
        DPFactory.generateAll();
    }

    private static void annotatePatterns() throws IOException, InterruptedException {
        DPFactory.annotateAll();
    }

    private static void extractRelations() throws IOException, InterruptedException, ParseException {
        new DPMatcher().extractAll();
    }

}
