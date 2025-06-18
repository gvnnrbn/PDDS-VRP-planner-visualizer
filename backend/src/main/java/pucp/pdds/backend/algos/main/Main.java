package pucp.pdds.backend.algos.main;

import pucp.pdds.backend.algos.scheduler.Scheduler;
import pucp.pdds.backend.algos.scheduler.SchedulerAgent;
import pucp.pdds.backend.algos.scheduler.SchedulerAgentTextFiles;

public class Main {
    public static void main(String[] args) {
        SchedulerAgent agent = new SchedulerAgentTextFiles();

        Scheduler scheduler = new Scheduler(agent);
        scheduler.run();
    }
}
