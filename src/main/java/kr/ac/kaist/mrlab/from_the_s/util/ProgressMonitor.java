package kr.ac.kaist.mrlab.from_the_s.util;

/**
 * Created by gyuhyeon on 6/20/17.
 */
public class ProgressMonitor {

    private String task;
    private int done = 0;
    private int size;
    private int prog = -1;

    /**
     * Instantiates {@link kr.ac.kaist.mrlab.from_the_s.util.ProgressMonitor}.
     *
     * @param task Task name.
     * @param size Task size.
     */
    public ProgressMonitor(String task, int size) {
        this.task = task;
        this.size = size + 1;
        this.update();
    }

    /**
     * Updates and shows progress.
     */
    public synchronized void update() {
        int newProgress = (1000 * ++this.done / this.size);
        if (newProgress > this.prog) {
            this.prog = newProgress;
            System.out.printf("\r" + this.task + "... %.1f%%", this.prog / 10.0);
            if (this.prog == 1000) {
                System.out.println(".");
            }
        }
    }

}
