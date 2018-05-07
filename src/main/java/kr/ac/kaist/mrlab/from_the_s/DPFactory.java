package kr.ac.kaist.mrlab.from_the_s;

import kr.ac.kaist.mrlab.from_the_s.util.ProgressMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Dependency Pattern Factory.
 *
 * Created by gyuhyeon on 6/20/17.
 */
public class DPFactory {

    private static BufferedReader br;

    private static synchronized String nextLine() throws IOException {
        return DPFactory.br.readLine();
    }

    /**
     * Generates patterns from given a sentence, a subject and an object.
     *
     * @param sentence ETRI-parsed sentence.
     * @param subject  Subject entity containing text, byteBegin, and byteEnd.
     * @param object   Object entity containing text, byteBegin, and byteEnd.
     * @return Dependency patterns.
     */
    public static ArrayList<DependencyPattern> generate(JSONObject sentence,
                                                        JSONObject subject,
                                                        JSONObject object) throws ParseException {
        JSONParser jp = new JSONParser();

        subject = (JSONObject) jp.parse(subject.toJSONString());
        object = (JSONObject) jp.parse(object.toJSONString());

        JSONArray dependency = (JSONArray) jp.parse(((JSONArray) sentence.get("dependency")).toJSONString());
        JSONArray word = (JSONArray) jp.parse(((JSONArray) sentence.get("word")).toJSONString());
        JSONArray morp = (JSONArray) jp.parse(((JSONArray) sentence.get("morp")).toJSONString());
        JSONArray NE = (JSONArray) jp.parse(((JSONArray) sentence.get("NE")).toJSONString());

        DPFactory.castIncomingAndOutgoingIndicesToInteger(dependency);

        DPFactory.addMorphemePositionsToDependency(word, dependency);
        DPFactory.addMorphemesToDependency(morp, dependency);

        DPFactory.addMorphemePositionsToEntity(morp, subject);
        DPFactory.addMorphemePositionsToEntity(morp, object);

        JSONArray entities = DPFactory.filterNamedEntities(NE);  // overlapped entities?

        DPFactory.insertEntityToFilteredEntities(subject, entities);
        DPFactory.insertEntityToFilteredEntities(object, entities);

        DPFactory.insertEntitiesToDependency(entities, dependency);

        DPFactory.mergeEntitiesInDependency(dependency);

        ArrayList<Integer> sOutPath = DPFactory.findOutgoingPathFrom(subject, dependency);
        ArrayList<Integer> oOutPath = DPFactory.findOutgoingPathFrom(object, dependency);
        ArrayList<Integer> pOutPath = DPFactory.findOutgoingPathFromPredicate(sOutPath, oOutPath, dependency);

        ArrayList<ArrayList<Integer>> sInPaths = DPFactory
                .findIncomingPathsTo(sOutPath, dependency, oOutPath.get(0));
        ArrayList<ArrayList<Integer>> oInPaths = DPFactory
                .findIncomingPathsTo(oOutPath, dependency, sOutPath.get(0));
        ArrayList<ArrayList<Integer>> pInPaths = DPFactory
                .findIncomingPathsTo(pOutPath, dependency, sOutPath.get(0), oOutPath.get(0));

        ArrayList<DependencyPattern> patterns = new ArrayList<>();
        for (ArrayList<Integer> sInPath : sInPaths) {
            for (ArrayList<Integer> oInPath : oInPaths) {
                for (ArrayList<Integer> pInPath : pInPaths) {
                    patterns.add(new DependencyPattern(DPFactory.combineElements(dependency,
                            sInPath, sOutPath, oInPath, oOutPath, pInPath, pOutPath)));
                }
            }
        }

        return patterns;
    }

    private static void castIncomingAndOutgoingIndicesToInteger(JSONArray dependency) {
        for (int di = 0; di < dependency.size(); di++) {
            JSONObject d = (JSONObject) dependency.get(di);
            d.put("head", ((Long) d.get("head")).intValue());
            JSONArray mod = (JSONArray) d.get("mod");
            for (int mi = 0; mi < mod.size(); mi++) {
                mod.add(mi, ((Long) mod.remove(mi)).intValue());
            }
        }
    }

    private static void addMorphemePositionsToDependency(JSONArray word, JSONArray dependency) {
        for (int i = 0; i < dependency.size(); i++) {
            ((JSONObject) dependency.get(i)).put("morpBegin",
                    ((Long) ((JSONObject) word.get(i)).get("begin")).intValue());
            ((JSONObject) dependency.get(i)).put("morpEnd",
                    ((Long) (((JSONObject) word.get(i)).get("end"))).intValue());
        }
    }

    private static void addMorphemesToDependency(JSONArray morp, JSONArray dependency) {
        for (int i = 0; i < dependency.size(); i++) {
            JSONObject d = (JSONObject) dependency.get(i);
            int dMorpBegin = (int) d.get("morpBegin");
            int dMorpEnd = (int) d.get("morpEnd");
            JSONArray morps = new JSONArray();
            for (int j = dMorpBegin; j <= dMorpEnd; j++) {
                ((JSONObject) morp.get(j)).put("id", ((Long) ((JSONObject) morp.get(j)).get("id")).intValue());
                morps.add(morp.get(j));
            }
            d.put("morps", morps);
        }
    }

    private static void addMorphemePositionsToEntity(JSONArray morp, JSONObject entity) {
        long eByteBegin = (long) entity.get("byteBegin");
        int mi = 0;
        for (; mi < morp.size(); mi++) {
            long mByteBegin = (long) ((JSONObject) morp.get(mi)).get("position");
            long mByteEnd = mByteBegin + ((String) ((JSONObject) morp.get(mi)).get("lemma")).getBytes().length;
            if (mByteBegin <= eByteBegin && eByteBegin <= mByteEnd) {
                break;
            }
        }
        entity.put("morpBegin", mi);

        long eByteEnd = (long) entity.get("byteEnd");
        for (; mi < morp.size(); mi++) {
            long mByteBegin = (long) ((JSONObject) morp.get(mi)).get("position");
            long mByteEnd = mByteBegin + ((String) ((JSONObject) morp.get(mi)).get("lemma")).getBytes().length;
            if (eByteEnd <= mByteEnd) {
                break;
            }
        }
        entity.put("morpEnd", mi);
    }

    private static JSONArray filterNamedEntities(JSONArray NE) {
        JSONArray filteredNE = new JSONArray();
        for (int i = 0; i < NE.size(); i++) {
            String text = (String) ((JSONObject) NE.get(i)).get("text");
            String type = (String) ((JSONObject) NE.get(i)).get("type");
            if (type.startsWith("CV_")) {
                continue;
            }
            JSONObject ne = new JSONObject();
            ne.put("text", text);
            ne.put("type", type);
            ne.put("morpBegin", ((Long) ((JSONObject) NE.get(i)).get("begin")).intValue());
            ne.put("morpEnd", ((Long) ((JSONObject) NE.get(i)).get("end")).intValue());
            filteredNE.add(ne);
        }

        return filteredNE;
    }

    private static void insertEntityToFilteredEntities(JSONObject entity, JSONArray entities) {
        int eMorpBegin = (int) entity.get("morpBegin");
        int eMorpEnd = (int) entity.get("morpEnd");
        for (int i = entities.size() - 1; i >= 0; i--) {
            int nMorpBegin = (int) ((JSONObject) entities.get(i)).get("morpBegin");
            int nMorpEnd = (int) ((JSONObject) entities.get(i)).get("morpEnd");
            if (eMorpEnd < nMorpBegin) {
                //
            } else if (eMorpBegin == nMorpBegin && eMorpEnd == nMorpEnd) {
                entity.put("type", ((JSONObject) entities.get(i)).get("type"));
                entities.remove(i);
                entities.add(i, entity);
                break;
            } else if (nMorpEnd < eMorpBegin) {
                entity.put("type", "");
                entities.add(i + 1, entity);
                break;
            } else {
                entities.remove(i);
                if (i == 0) {
                    entity.put("type", "");
                    entities.add(0, entity);
                }
            }
        }
    }

    private static void insertEntitiesToDependency(JSONArray entities, JSONArray dependency) {
        for (Object d : dependency) {
            ((JSONObject) d).put("entities", new JSONArray());
        }
        int diFrom = dependency.size() - 1;
        for (int ei = entities.size() - 1; ei >= 0; ei--) {
//            int eMorpBegin = (int) ((JSONObject) entities.get(ei)).get("morpBegin");
            int eMorpEnd = (int) ((JSONObject) entities.get(ei)).get("morpEnd");
            for (int di = diFrom; di >= 0; di--) {
                int dMorpBegin = (int) ((JSONObject) dependency.get(di)).get("morpBegin");
                int dMorpEnd = (int) ((JSONObject) dependency.get(di)).get("morpEnd");
                if (dMorpBegin <= eMorpEnd && eMorpEnd <= dMorpEnd) {
                    ((JSONArray) ((JSONObject) dependency.get(di)).get("entities")).add(0, entities.get(ei));
                    diFrom = di;
                    break;
                }
            }
        }
    }

    private static void mergeEntitiesInDependency(JSONArray dependency) {
        int hdi = -1;
        int eMorpBegin = Integer.MAX_VALUE;
        int eMorpEnd = -1;
        for (int di = dependency.size() - 1; di >= 0; di--) {
            JSONObject d = (JSONObject) dependency.get(di);
            int dMorpEnd = (int) d.get("morpEnd");

            if (eMorpBegin <= dMorpEnd && dMorpEnd <= eMorpEnd) {
                JSONObject hd = (JSONObject) dependency.get(hdi);
                JSONArray morps = (JSONArray) d.get("morps");
                for (int mi = morps.size() - 1; mi >= 0; mi--) {
                    int morpId = (int) ((JSONObject) morps.get(mi)).get("id");
                    if (morpId < eMorpBegin) {
                        break;
                    } else {
                        hd.put("morpBegin", morpId);
                        d.put("morpEnd", morpId - 1);

                        ((JSONArray) hd.get("morps")).add(0, morps.remove(mi));
                    }
                }
                JSONArray mod = (JSONArray) hd.get("mod");
                for (int mi = 0; mi < mod.size(); mi++) {
                    if ((int) mod.get(mi) == di) {
                        mod.remove(mi);
                    }
                }
                mod = (JSONArray) d.get("mod");
                for (int mi = mod.size() - 1; mi >= 0; mi--) {
                    ((JSONArray) hd.get("mod")).add(0, mod.get(mi));
                }
            }

            if (!((JSONArray) d.get("entities")).isEmpty()) {
                JSONObject entitiy = (JSONObject) ((JSONArray) d.get("entities")).get(0);
                hdi = di;
                eMorpBegin = (int) entitiy.get("morpBegin");
                eMorpEnd = (int) entitiy.get("morpEnd");
            }
        }
    }

    private static ArrayList<Integer> findOutgoingPathFrom(JSONObject entity, JSONArray dependency) {
//        int eMorpBegin = (int) entity.get("morpBegin");
        int eMorpEnd = (int) entity.get("morpEnd");

        ArrayList<Integer> outPath = new ArrayList<>();
        for (int di = 0; di < dependency.size(); di++) {
            int dMorpBegin = (int) ((JSONObject) dependency.get(di)).get("morpBegin");
            int dMorpEnd = (int) ((JSONObject) dependency.get(di)).get("morpEnd");

            if (dMorpBegin <= eMorpEnd && eMorpEnd <= dMorpEnd) {
                outPath.add(di);
                int hdi = (int) ((JSONObject) dependency.get(di)).get("head");
                while (hdi != -1) {
                    outPath.add(hdi);
                    hdi = (int) ((JSONObject) dependency.get(hdi)).get("head");
                }
                break;
            }
        }
        return outPath;
    }

    private static ArrayList<Integer> findOutgoingPathFromPredicate(ArrayList<Integer> sbjOutPath,
                                                                    ArrayList<Integer> objOutPath,
                                                                    JSONArray dependency) {
        int si = 0;
        int oi = 0;
        while (si < sbjOutPath.size() && oi < objOutPath.size()) {
            if (sbjOutPath.get(si) < objOutPath.get(oi)) {
                si++;
            } else if (sbjOutPath.get(si) > objOutPath.get(oi)) {
                oi++;
            } else {
                break;
            }
        }

        ArrayList<Integer> prdOutPath = new ArrayList<>();
        int pi = sbjOutPath.get(si);
        while (pi != -1) {
            if (((String) ((JSONObject) dependency.get(pi)).get("label")).startsWith("V")) {
                break;
            }
            pi = (int) ((JSONObject) dependency.get(pi)).get("head");
        }
        while (pi != -1) {
            prdOutPath.add(pi);
            pi = (int) ((JSONObject) dependency.get(pi)).get("head");
        }

        return prdOutPath;
    }

    private static ArrayList<ArrayList<Integer>> findIncomingPathsTo(ArrayList<Integer> outPath,
                                                                     JSONArray dependency, Integer... stops) {
        ArrayList<ArrayList<Integer>> inPaths = new ArrayList<>();
        if (outPath.isEmpty()) {
            inPaths.add(new ArrayList<>());
            return inPaths;
        }

        ArrayList<Integer> inPath = new ArrayList<>();
        inPath.add(outPath.get(0));
        DPFactory.findIncomingPathsTo(dependency, inPath, inPaths, stops);
        if (inPaths.isEmpty()) {
            inPaths.add(inPath);
            return inPaths;
        } else {
            return inPaths;
        }
    }

    private static void findIncomingPathsTo(JSONArray dependency, ArrayList<Integer> inPath,
                                            ArrayList<ArrayList<Integer>> inPaths, Integer... stops) {
        JSONArray mod = (JSONArray) ((JSONObject) dependency.get(inPath.get(inPath.size() - 1))).get("mod");
        if (mod.isEmpty()) {
            Collections.reverse(inPath);
            inPaths.add(inPath);
        } else {
            for (Object from : mod) {
                if (!Arrays.asList(stops).contains(from)) {
                    ArrayList newInPath = new ArrayList(inPath);
                    newInPath.add(from);
                    DPFactory.findIncomingPathsTo(dependency, newInPath, inPaths, stops);
                }
            }
        }
    }

    private static String[] combineElements(JSONArray dependency,
                                            ArrayList<Integer> sInPath, ArrayList<Integer> sOutPath,
                                            ArrayList<Integer> oInPath, ArrayList<Integer> oOutPath,
                                            ArrayList<Integer> pInPath, ArrayList<Integer> pOutPath) {
        String[] elements = new String[1
                + Configuration.S_IN_SIZE + 2 + Configuration.S_OUT_SIZE
                + Configuration.O_IN_SIZE + 2 + Configuration.O_OUT_SIZE
                + Configuration.P_IN_SIZE + 1 + Configuration.P_OUT_SIZE];
        Arrays.fill(elements, "_");

        if (sOutPath.get(0) < oOutPath.get(0)) {
            elements[0] = "S->O->P";
        } else {    // sOutPath.get(0) > oOutPath.get(0)
            elements[0] = "O->S->P";
        }

        int ei = 1;
        for (int i = 0; i < Configuration.S_IN_SIZE; i++) {
            int si = sInPath.size() - 1 - Configuration.S_IN_SIZE + i;
            if (si >= 0) {
                elements[ei] = DPFactory.getLemmaElementAt(sInPath.get(si), dependency);
            }
            ei++;
        }
        elements[ei++] = DPFactory.getEntityElementAt(sOutPath.get(0), dependency);
        elements[ei++] = "SBJ" + DPFactory.getPostpositionElementAt(sOutPath.get(0), dependency);
        for (int i = 0; i < Configuration.S_OUT_SIZE; i++) {
            if (i + 1 < sOutPath.size()) {
                int si = sOutPath.get(i + 1);
                if (si == oOutPath.get(0)) {
                    elements[ei] = "->OBJ";
                    break;
                } else if (!pOutPath.isEmpty() && si == pOutPath.get(0)) {
                    elements[ei] = "->PRD";
                    break;
                } else {
                    elements[ei] = DPFactory.getLemmaElementAt(si, dependency);
                }
            }
            ei++;
        }

        ei = 1 + Configuration.S_IN_SIZE + 2 + Configuration.S_OUT_SIZE;
        for (int i = 0; i < Configuration.O_IN_SIZE; i++) {
            int oi = oInPath.size() - 1 - Configuration.O_IN_SIZE + i;
            if (oi >= 0) {
                elements[ei] = DPFactory.getLemmaElementAt(oInPath.get(oi), dependency);
            }
            ei++;
        }
        elements[ei++] = DPFactory.getEntityElementAt(oOutPath.get(0), dependency);
        elements[ei++] = "OBJ" + DPFactory.getPostpositionElementAt(oOutPath.get(0), dependency);
        for (int i = 0; i < Configuration.O_OUT_SIZE; i++) {
            if (i + 1 < oOutPath.size()) {
                int oi = oOutPath.get(i + 1);
                if (oi == sOutPath.get(0)) {
                    elements[ei] = "->SBJ";
                    break;
                } else if (!pOutPath.isEmpty() && oi == pOutPath.get(0)) {
                    elements[ei] = "->PRD";
                    break;
                } else {
                    elements[ei] = DPFactory.getLemmaElementAt(oi, dependency);
                }
            }
            ei++;
        }

        ei = 1 + Configuration.S_IN_SIZE + 2 + Configuration.S_OUT_SIZE +
                Configuration.O_IN_SIZE + 2 + Configuration.O_OUT_SIZE;
        for (int i = 0; i < Configuration.P_IN_SIZE; i++) {
            int pi = pInPath.size() - 1 - Configuration.P_IN_SIZE + i;
            if (pi >= 0) {
                elements[ei] = DPFactory.getLemmaElementAt(pInPath.get(pi), dependency);
            }
            ei++;
        }
        for (int i = 0; i < Configuration.P_OUT_SIZE; i++) {
            if (i < pOutPath.size()) {
                elements[ei++] = DPFactory.getLemmaElementAt(pOutPath.get(i), dependency);
            }
        }

        return elements;
    }

    private static String getLemmaElementAt(int di, JSONArray dependency) {
        JSONObject entity = (JSONObject) ((JSONObject) dependency.get(di)).get("entity");
        int eMorpBegin = entity != null ? (int) entity.get("morpBegin") : Integer.MAX_VALUE;
        int eMorpEnd = entity != null ? (int) entity.get("morpEnd") : -1;

        String headElement = "";
        String tailElement = "";
        JSONArray morps = (JSONArray) ((JSONObject) dependency.get(di)).get("morps");
        for (int mi = 0; mi < morps.size(); mi++) {
            int morpIdx = (int) ((JSONObject) morps.get(mi)).get("id");
            if (eMorpBegin <= morpIdx && morpIdx <= eMorpEnd) {
                continue;
            } else {
                String lemma = (String) ((JSONObject) morps.get(mi)).get("lemma");
                String type = (String) ((JSONObject) morps.get(mi)).get("type");
                if (type.equals("VCP")) {
                    break;
                } else if (type.startsWith("E") || type.startsWith("J")) {
                    break;
                } else {
                    if (morpIdx < eMorpBegin) {
                        headElement += (String) ((JSONObject) morps.get(mi)).get("lemma");
                    } else {
                        tailElement += (String) ((JSONObject) morps.get(mi)).get("lemma");
                    }
                }
            }
        }

        return headElement + (entity != null ? (String) entity.get("type") : "") + tailElement;
    }

    private static String getPostpositionElementAt(int di, JSONArray dependency) {
        String element = "";
        JSONArray morps = (JSONArray) ((JSONObject) dependency.get(di)).get("morps");
        for (int mi = 0; mi < morps.size(); mi++) {
            String lemma = (String) ((JSONObject) morps.get(mi)).get("lemma");
            String type = (String) ((JSONObject) morps.get(mi)).get("type");
            if (type.startsWith("N") || type.startsWith("V")) {
                continue;
            } else if (type.startsWith("E") || type.startsWith("J") || type.startsWith("S")) {
                if (type.equals("JKS") && (lemma.equals("이") || lemma.equals("가"))) {
                    element += "가";
                } else if (type.equals("JKB") && (lemma.equals("께") || lemma.equals("에게"))) {
                    element += "에게";
                } else if (type.equals("JKG") && lemma.equals("의")) {
                    element += "의";
                } else if (type.equals("JKO") && (lemma.equals("을") || lemma.equals("를"))) {
                    element += "를";
                } else if (type.equals("JX") && (lemma.equals("은") || lemma.equals("는"))) {
                    element += "는";
                } else if (type.equals("SP")) {
                    element += lemma;
                }
            }
        }

        return element;
    }

    private static String getEntityElementAt(int di, JSONArray dependency) {
        JSONArray entities = (JSONArray) ((JSONObject) dependency.get(di)).get("entities");
        for (int ei = 0; ei < entities.size(); ei++) {
            if (((JSONObject) entities.get(ei)).containsKey("byteBegin")) {
                return (String) ((JSONObject) entities.get(ei)).get("type");
            }
        }

        return "_";
    }

    private static ArrayList<String> allPatterns;

    private static HashMap<String, Integer> allElem2freq;

    /**
     * Generates patterns for all models in {@link kr.ac.kaist.mrlab.from_the_s.Configuration#MODEL_ROOT}.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void generateAll() throws IOException, InterruptedException {
        List<Path> models = Files.list(Paths.get(Configuration.MODEL_ROOT)).collect(Collectors.toList());
        for (int mi = 0; mi < models.size(); mi++) {
            ProgressMonitor pm = new ProgressMonitor("Generating patterns for "
                    + models.get(mi).getFileName().toString() + " (" + (mi + 1) + "/" + models.size() + ")", 1);

            DPFactory.allPatterns = new ArrayList<>();
            DPFactory.allElem2freq = new HashMap<>();

            DPFactory.br = Files.newBufferedReader(Paths.get(models.get(mi).toString(), "ds-data-parsed.txt"));

            ArrayList<DPFactory.GenerateAllCallable> tasks = new ArrayList<>();
            for (int i = 0; i < Configuration.NUMBER_OF_THREADS; i++) {
                tasks.add(new DPFactory.GenerateAllCallable());
            }

            ExecutorService threadPool = Executors.newFixedThreadPool(Configuration.NUMBER_OF_THREADS);
            threadPool.invokeAll(tasks);
            threadPool.shutdownNow();

            DPFactory.br.close();

            BufferedWriter bw = Files.newBufferedWriter(Paths.get(models.get(mi).toString(), "patterns-raw.txt"));
            for (String pattern : new ArrayList<>(new HashSet<>(DPFactory.allPatterns))) {
                bw.write(pattern + "\n");
            }
            bw.close();

            ArrayList<Element> elements = new ArrayList<>();
            for (String elem : DPFactory.allElem2freq.keySet()) {
                elements.add(new Element(elem, DPFactory.allElem2freq.get(elem)));
            }

            bw = Files.newBufferedWriter(Paths.get(models.get(mi).toString(), "elements.txt"));
            elements.sort(null);
            for (Element element : elements) {
                int type = element.elem.contains("SBJ") ||  element.elem.contains("OBJ")
                        || element.elem.contains("->") || element.elem.equals("_")
                        ? 0 : -1;
                bw.write(type + "\t" + element.elem + "\t" + element.freq + "\n");
            }
            bw.close();

            pm.update();
        }
    }

    private static synchronized void addPatterns(ArrayList<String> patterns) {
        DPFactory.allPatterns.addAll(patterns);
    }

    private static synchronized void addElements(HashMap<String, Integer> elem2freq) {
        for (String elem : elem2freq.keySet()) {
            if (!DPFactory.allElem2freq.containsKey(elem)) {
                DPFactory.allElem2freq.put(elem, 0);
            }
            DPFactory.allElem2freq.put(elem, DPFactory.allElem2freq.get(elem) + elem2freq.get(elem));
        }
    }

    private static class Element implements Comparable<Element> {
        private String elem;
        private int freq;

        public Element(String elem, int freq) {
            this.elem = elem;
            this.freq = freq;
        }

        @Override
        public int compareTo(Element e) {
            return e.freq - this.freq;
        }
    }

    private static class GenerateAllCallable implements Callable<Object> {

        private JSONParser jp = new JSONParser();

        @Override
        public Object call() throws Exception {
            try {
                ArrayList<String> patterns = new ArrayList<>();
                HashMap<String, Integer> elem2freq = new HashMap<>();
                String parsedInstJstr;
                while ((parsedInstJstr = DPFactory.nextLine()) != null) {
                    // better not to get exceptions
                    try {
                        JSONObject parsedInst = (JSONObject) this.jp.parse(parsedInstJstr);
                        for (DependencyPattern dp : DPFactory.generate(
                                (JSONObject) parsedInst.get("sentence"),
                                (JSONObject) parsedInst.get("subject"),
                                (JSONObject) parsedInst.get("object"))) {
                            String patternStr = "";
                            for (String element : dp.getElements()) {
                                patternStr += element + "\t";

                                if (!elem2freq.containsKey(element)) {
                                    elem2freq.put(element, 0);
                                }
                                elem2freq.put(element, elem2freq.get(element) + 1);
                            }
                            patterns.add(patternStr.trim());
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }

                DPFactory.addPatterns(patterns);
                DPFactory.addElements(elem2freq);

                return null;
            } catch (Exception e) {
                e.printStackTrace();

                throw e;
            }
        }

    }

    private static ArrayList<String> allPosPatterns;

    private static ArrayList<String> allNegPatterns;

    /**
     * Annotates patterns for all models in {@link kr.ac.kaist.mrlab.from_the_s.Configuration#MODEL_ROOT}
     * based on an 'elements.txt' file in the model directory.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void annotateAll() throws IOException, InterruptedException {
        List<Path> models = Files.list(Paths.get(Configuration.MODEL_ROOT)).collect(Collectors.toList());
        for (int mi = 0; mi < models.size(); mi++) {
            ProgressMonitor pm = new ProgressMonitor("Annotating patterns of "
                    + models.get(mi).getFileName().toString() + " (" + (mi + 1) + "/" + models.size() + ")", 1);

            HashMap<String, Integer> elem2type = new HashMap<>();
            BufferedReader br = Files.newBufferedReader(Paths.get(models.get(mi).toString(), "elements.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tef = line.split("\t");
                int type = Integer.parseInt(tef[0]);
                if (type >= 0) {
                    elem2type.put(tef[1], type);
                }
            }
            br.close();

            DPFactory.allPosPatterns = new ArrayList<>();
            DPFactory.allNegPatterns = new ArrayList<>();

            DPFactory.br = Files.newBufferedReader(Paths.get(models.get(mi).toString(), "patterns-raw.txt"));

            ArrayList<DPFactory.AnnotateAllCallable> tasks = new ArrayList<>();
            for (int i = 0; i < Configuration.NUMBER_OF_THREADS; i++) {
                tasks.add(new DPFactory.AnnotateAllCallable(elem2type));
            }

            ExecutorService threadPool = Executors.newFixedThreadPool(Configuration.NUMBER_OF_THREADS);
            threadPool.invokeAll(tasks);
            threadPool.shutdownNow();

            DPFactory.br.close();

            BufferedWriter bw = Files.newBufferedWriter(Paths.get(models.get(mi).toString(), "patterns-pos.txt"));
            DPFactory.allPosPatterns = new ArrayList<>(new HashSet<>(DPFactory.allPosPatterns));
            DPFactory.allPosPatterns.sort(null);
            for (String pattern : DPFactory.allPosPatterns) {
                bw.write(pattern + "\n");
            }
            bw.close();

            bw = Files.newBufferedWriter(Paths.get(models.get(mi).toString(), "patterns-neg.txt"));
            DPFactory.allNegPatterns = new ArrayList<>(new HashSet<>(DPFactory.allNegPatterns));
            DPFactory.allNegPatterns.sort(null);
            for (String pattern : DPFactory.allNegPatterns) {
                bw.write(pattern + "\n");
            }
            bw.close();

            pm.update();
        }
    }

    private static synchronized void addPositivePatterns(ArrayList<String> positivePatterns) {
        DPFactory.allPosPatterns.addAll(positivePatterns);
    }

    private static synchronized void addNegativePatterns(ArrayList<String> negativePatterns) {
        DPFactory.allNegPatterns.addAll(negativePatterns);
    }

    private static class AnnotateAllCallable implements Callable<Object> {

        private HashMap<String, Integer> elem2type;

        public AnnotateAllCallable(HashMap<String, Integer> elem2type) {
            this.elem2type = elem2type;
        }

        @Override
        public Object call() throws Exception {
            try {
                ArrayList<String> posPatterns = new ArrayList<>();
                ArrayList<String> negPatterns = new ArrayList<>();
                String patternStr;
                while ((patternStr = DPFactory.nextLine()) != null) {
                    String[] elems = patternStr.split("\t");

                    int type = 0;
                    for(int i = 0; i < elems.length; i++) {
                        if (this.elem2type.containsKey(elems[i])) {
                            if (type < this.elem2type.get(elems[i])) {
                                type = this.elem2type.get(elems[i]);
                            }
                        } else {
                            elems[i] = "_";
                        }
                    }
                    type %= 2;

                    if (type == 1) {
                        posPatterns.add(String.join("\t", elems));
                    } else {
                        negPatterns.add(String.join("\t", elems));
                    }
                }

                DPFactory.addPositivePatterns(posPatterns);
                DPFactory.addNegativePatterns(negPatterns);

                return null;
            } catch (Exception e) {
                e.printStackTrace();

                throw e;
            }
        }

    }

}
