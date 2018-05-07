package kr.ac.kaist.mrlab.from_the_s;

import kr.ac.kaist.mrlab.from_the_s.util.ProgressMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.omg.CORBA.Object;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Dependency Pattern Matcher.
 * <p>
 * Created by gyuhyeon on 6/20/17.
 */
public class DPMatcher {

    /**
     * Instantiates {@link kr.ac.kaist.mrlab.from_the_s.DPMatcher}.
     */
    public DPMatcher() throws IOException, InterruptedException {
        this.models = Files.list(Paths.get(Configuration.MODEL_ROOT))
                .map(p -> p.getFileName().toString()).collect(Collectors.toList());

        ProgressMonitor pm = new ProgressMonitor("Loading patterns", this.models.size());
        this.modelIdx = 0;

        for (String model : this.models) {
            this.rl2pt.put(model, new PatternTrie());
            this.rl2el.put(model, new ArrayList<>());
        }

        ArrayList<DPMatcher.DPMatcherCallable> tasks = new ArrayList<>();
        for (int i = 0; i < Configuration.NUMBER_OF_THREADS; i++) {
            tasks.add(new DPMatcher.DPMatcherCallable(pm));
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(Configuration.NUMBER_OF_THREADS);
        threadPool.invokeAll(tasks);
        threadPool.shutdownNow();
    }

    private HashMap<String, PatternTrie> rl2pt = new HashMap<>();
    private HashMap<String, ArrayList<String>> rl2el = new HashMap<>();

    private List<String> models;
    private int modelIdx;

    private synchronized String nextModel() {
        if (this.modelIdx < this.models.size()) {
            return this.models.get(this.modelIdx++);
        } else {
            return null;
        }
    }

    private class DPMatcherCallable implements Callable<Object> {

        private ProgressMonitor pm;

        public DPMatcherCallable(ProgressMonitor pm) {
            this.pm = pm;
        }

        @Override
        public Object call() throws Exception {
            try {
                String model;
                while ((model = DPMatcher.this.nextModel()) != null) {
                    BufferedReader br = Files.newBufferedReader(Paths.get(Configuration.MODEL_ROOT, model, "patterns-pos.txt"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        DPMatcher.this.rl2pt.get(model).add(new DependencyPattern(line.split("\t")));
                    }
                    br.close();

                    br = Files.newBufferedReader(Paths.get(Configuration.MODEL_ROOT, model, "elements.txt"));
                    while ((line = br.readLine()) != null) {
                        String[] tef = line.split("\t");
                        if (Integer.parseInt(tef[0]) >= 0) {
                            DPMatcher.this.rl2el.get(model).add(tef[1]);
                        }
                    }
                    br.close();

                    this.pm.update();
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();

                throw e;
            }
        }

    }

    private class PatternTrie {

        private Node root = new Node();

        private synchronized void add(DependencyPattern dp) {
            this.root.addNext(dp.getElements(), 0, dp);
        }

        private DependencyPattern match(DependencyPattern dp) {
            return this.match(this.root, dp.getElements(), 0);
        }

        private DependencyPattern match(Node node, String[] elems, int i) {
            if (node.e2n == null) {
                return node.dp;
            }

            if (node.e2n.containsKey(elems[i])) {
                return this.match(node.e2n.get(elems[i]), elems, i + 1);
            } else {
                return null;
            }
        }

        private class Node {

            private DependencyPattern dp = null;
            private HashMap<String, Node> e2n = new HashMap<>();

            public void addNext(String[] elements, int i, DependencyPattern dp) {
                if (i == elements.length) {
                    this.dp = dp;
                    this.e2n = null;
                } else {
                    if (!this.e2n.containsKey(elements[i])) {
                        this.e2n.put(elements[i], new Node());
                    }
                    this.e2n.get(elements[i]).addNext(elements, i + 1, dp);
                }
            }

        }

//        public ArrayList<DependencyPattern> findAllPatterns() {
//            ArrayList<DependencyPattern> dps = new ArrayList<>();
//            this.findAllPatterns(this.root, dps);
//            return dps;
//        }

//        private void findAllPatterns(Node node, ArrayList<DependencyPattern> dps) {
//            if (node.e2n == null) {
//                dps.add(node.dp);
//            } else {
//                for (String element : node.e2n.keySet()) {
//                    this.findAllPatterns(node.e2n.get(element), dps);
//                }
//            }
//        }

    }

    /**
     * Extracts relations by matching the given dependency pattern to learned patterns.
     */
    public void extractAll() throws InterruptedException, IOException, ParseException {
        ProgressMonitor pm = new ProgressMonitor("Extracting relations", this.models.size());
        this.modelIdx = 0;

        if (!Files.exists(Paths.get(Configuration.EXTRACTS))) {
            Files.createFile(Paths.get(Configuration.EXTRACTS));
        }

        JSONArray parsedDoc = new JSONArray();
        JSONParser jp = new JSONParser();
        BufferedReader br = Files.newBufferedReader(Paths.get(Configuration.DOCUMENT));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            parsedDoc.add(jp.parse(line));
        }
        br.close();

        ArrayList<DPMatcher.ExtractAllCallable> tasks = new ArrayList<>();
        for (int i = 0; i < Configuration.NUMBER_OF_THREADS; i++) {
            tasks.add(new DPMatcher.ExtractAllCallable(parsedDoc, pm));
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(Configuration.NUMBER_OF_THREADS);
        threadPool.invokeAll(tasks);
        threadPool.shutdownNow();
    }

    private synchronized void write(ArrayList<Extract> extracts) throws IOException {
        BufferedWriter bw = Files.newBufferedWriter(Paths.get(Configuration.EXTRACTS), StandardOpenOption.APPEND);
        for (Extract extract : extracts) {
            bw.write(extract.sUri + "\t" + extract.rel + "\t" + extract.oUri + "\t.\t0.001\n");
        }
        bw.close();
    }

    private class ExtractAllCallable implements Callable<Object> {

        private JSONArray parsedDoc;
        private ProgressMonitor pm;

        public ExtractAllCallable(JSONArray parsedDoc, ProgressMonitor pm) {
            this.parsedDoc = parsedDoc;
            this.pm = pm;
        }

        @Override
        public Object call() throws Exception {
            try {
                String model;
                while ((model = DPMatcher.this.nextModel()) != null) {
                    ArrayList<Extract> extracts = new ArrayList<>();
                    for (int di = 0; di < this.parsedDoc.size(); di++) {
                        JSONArray parsedPrg = (JSONArray) ((JSONObject) this.parsedDoc.get(di)).get("sentence");
                        for (int pi = 0; pi < parsedPrg.size(); pi++) {
                            JSONObject parsedStc = (JSONObject) parsedPrg.get(pi);

                            JSONArray entities = (JSONArray) parsedStc.get("NE");
                            for (int sei = 0; sei < entities.size(); sei++) {
                                for (int oei = 0; oei < entities.size(); oei++) {
                                    // better not to make exceptions
                                    try {
                                        if (sei == oei) {
                                            continue;
                                        }

                                        JSONObject se = (JSONObject) entities.get(sei);
                                        JSONObject oe = (JSONObject) entities.get(oei);

                                        JSONArray morp = (JSONArray) parsedStc.get("morp");

                                        JSONObject sBeginMorp = (JSONObject) morp.get(((Long) se.get("begin")).intValue());
                                        JSONObject sEndMorp = (JSONObject) morp.get(((Long) se.get("end")).intValue());
                                        JSONObject oBeginMorp = (JSONObject) morp.get(((Long) oe.get("begin")).intValue());
                                        JSONObject oEndMorp = (JSONObject) morp.get(((Long) oe.get("end")).intValue());

                                        int sByteBegin = ((Long) sBeginMorp.get("position")).intValue();
                                        int sByteEnd = ((Long) sEndMorp.get("position")).intValue()
                                                + ((String) sEndMorp.get("lemma")).getBytes().length;
                                        int oByteBegin = ((Long) oBeginMorp.get("position")).intValue();
                                        int oByteEnd = ((Long) oEndMorp.get("position")).intValue()
                                                + ((String) oEndMorp.get("lemma")).getBytes().length;

                                        if (!(sByteEnd <= oByteBegin || oByteEnd <= sByteBegin)) {
                                            continue;
                                        }

                                        JSONObject newSe = new JSONObject();
                                        newSe.put("text", se.get("text"));
                                        newSe.put("uri", "http://mrlab.kaist.ac.kr/resource/" + se.get("text"));
                                        newSe.put("byteBegin", sByteBegin);
                                        newSe.put("byteEnd", sByteEnd);

                                        JSONObject newOe = new JSONObject();
                                        newOe.put("text", oe.get("text"));
                                        newOe.put("uri", "http://mrlab.kaist.ac.kr/resource/" + se.get("text"));
                                        newOe.put("byteBegin", oByteBegin);
                                        newOe.put("byteEnd", oByteEnd);

                                        ArrayList<DependencyPattern> dps = new ArrayList<>();
                                        for (DependencyPattern dp : DPFactory.generate(parsedStc, newSe, newOe)) {
                                            for(int ei = 0; ei < dp.getElements().length; ei++) {
                                                if (!DPMatcher.this.rl2el.get(model).contains(dp.getElements()[ei])) {
                                                    dp.getElements()[ei] = "_";
                                                }
                                            }
                                            dps.add(dp);
                                        }
                                        dps = new ArrayList<>(new HashSet<>(dps));

                                        for (int dpi = dps.size() - 1; dpi >= 0; dpi--) {
                                            DependencyPattern matchedDp = DPMatcher.this.rl2pt.get(model).match(dps.get(dpi));
                                            if (matchedDp == null) {
                                                dps.remove(dpi);
                                            }
                                        }

                                        if (!dps.isEmpty()) {
                                            extracts.add(new Extract(model, parsedStc, se, oe, dps));
                                        }
                                    } catch (Exception e) {
                                        continue;
                                    }
                                }
                            }

                            entities = (JSONArray) parsedStc.get("entities");
                            if (entities == null) {
                                continue;
                            }
                            for (int sei = 0; sei < entities.size(); sei++) {
                                for (int oei = 0; oei < entities.size(); oei++) {
                                    // better not to make exceptions
                                    try {
                                        if (sei == oei) {
                                            continue;
                                        }

                                        JSONObject se = (JSONObject) entities.get(sei);
                                        JSONObject oe = (JSONObject) entities.get(oei);

                                        String stc = (String) parsedStc.get("text");

                                        int sStartOffset = ((Long) se.get("start_offset")).intValue();
                                        int sEndOffset = ((Long) se.get("end_offset")).intValue();
                                        int oStartOffset = ((Long) oe.get("start_offset")).intValue();
                                        int oEndOffset = ((Long) oe.get("end_offset")).intValue();

                                        int sByteBegin = stc.substring(0, sStartOffset).getBytes().length;
                                        int sByteEnd = sByteBegin + stc.substring(sStartOffset, sEndOffset).getBytes().length;
                                        int oByteBegin = stc.substring(0, oStartOffset).getBytes().length;
                                        int oByteEnd = oByteBegin + stc.substring(oStartOffset, oEndOffset).getBytes().length;

                                        if (!(sByteEnd <= oByteBegin || oByteEnd <= sByteBegin)) {
                                            continue;
                                        }

                                        JSONObject newSe = new JSONObject();
                                        newSe.put("text", se.get("text"));
                                        newSe.put("uri", se.get("uri"));
                                        newSe.put("byteBegin", sByteBegin);
                                        newSe.put("byteEnd", sByteEnd);

                                        JSONObject newOe = new JSONObject();
                                        newOe.put("text", oe.get("text"));
                                        newOe.put("uri", oe.get("uri"));
                                        newOe.put("byteBegin", oByteBegin);
                                        newOe.put("byteEnd", oByteEnd);

                                        ArrayList<DependencyPattern> dps = new ArrayList<>();
                                        for (DependencyPattern dp : DPFactory.generate(parsedStc, newSe, newOe)) {
                                            for(int ei = 0; ei < dp.getElements().length; ei++) {
                                                if (!DPMatcher.this.rl2el.get(model).contains(dp.getElements()[ei])) {
                                                    dp.getElements()[ei] = "_";
                                                }
                                            }
                                            dps.add(dp);
                                        }
                                        dps = new ArrayList<>(new HashSet<>(dps));

                                        for (int dpi = dps.size() - 1; dpi >= 0; dpi--) {
                                            DependencyPattern matchedDp = DPMatcher.this.rl2pt.get(model).match(dps.get(dpi));
                                            if (matchedDp == null) {
                                                dps.remove(dpi);
                                            }
                                        }

                                        if (!dps.isEmpty()) {
                                            extracts.add(new Extract(model, parsedStc, newSe, newOe, dps));
                                        }
                                    } catch (Exception e) {
                                        if (e instanceof StringIndexOutOfBoundsException) {
//                                            System.out.println(parsedStc.toJSONString());
                                        }

                                        continue;
                                    }
                                }
                            }
                        }
                    }
                    DPMatcher.this.write(extracts);

                    this.pm.update();
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();

                throw e;
            }
        }

    }

    private class Extract {

        private String rel;

        private String sText;
        private String sUri;

        private String oText;
        private String oUri;

        private String markedStc;

        private ArrayList<DependencyPattern> dps;

        public Extract(String rel, JSONObject parsedStc, JSONObject se, JSONObject oe, ArrayList<DependencyPattern> dps) {
            this.rel = rel;

            this.sText = (String) se.get("text");
            this.sUri = (String) se.get("uri");

            this.oText = (String) oe.get("text");
            this.oUri = (String) oe.get("uri");

            this.markedStc = (String) parsedStc.get("text");
            int sByteBegin = (int) se.get("byteBegin");
            int sByteEnd = (int) se.get("byteEnd");
            int oByteBegin = (int) oe.get("byteBegin");
            int oByteEnd = (int) oe.get("byteEnd");
            if (sByteBegin < oByteBegin) {
                this.markedStc = new String(Arrays.copyOfRange(this.markedStc.getBytes(), 0, oByteBegin))
                        + "<object uri=\"" + this.oUri +  "\">"
                        + new String(Arrays.copyOfRange(this.markedStc.getBytes(), oByteBegin, oByteEnd))
                        + "</object>"
                        + new String(Arrays.copyOfRange(this.markedStc.getBytes(), oByteEnd, this.markedStc.getBytes().length));
                this.markedStc = new String(Arrays.copyOfRange(this.markedStc.getBytes(), 0, sByteBegin))
                        + "<subject uri=\"" + this.oUri +  "\">"
                        + new String(Arrays.copyOfRange(this.markedStc.getBytes(), sByteBegin, sByteEnd))
                        + "</subject>"
                        + new String(Arrays.copyOfRange(this.markedStc.getBytes(), sByteEnd, this.markedStc.getBytes().length));
            } else {
                this.markedStc = new String(Arrays.copyOfRange(this.markedStc.getBytes(), 0, sByteBegin))
                        + "<subject uri=\"" + this.oUri +  "\">"
                        + new String(Arrays.copyOfRange(this.markedStc.getBytes(), sByteBegin, sByteEnd))
                        + "</subject>"
                        + new String(Arrays.copyOfRange(this.markedStc.getBytes(), sByteEnd, this.markedStc.getBytes().length));
                this.markedStc = new String(Arrays.copyOfRange(this.markedStc.getBytes(), 0, oByteBegin))
                        + "<object uri=\"" + this.oUri +  "\">"
                        + new String(Arrays.copyOfRange(this.markedStc.getBytes(), oByteBegin, oByteEnd))
                        + "</object>"
                        + new String(Arrays.copyOfRange(this.markedStc.getBytes(), oByteEnd, this.markedStc.getBytes().length));
            }

            this.dps = dps;
        }

    }

}
