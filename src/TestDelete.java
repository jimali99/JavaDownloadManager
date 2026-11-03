import java.util.*;
import com.idm.model.*;
import com.idm.engine.*;
import com.idm.util.*;

public class TestDelete {
    public static void main(String[] args) {
        AppSettings settings = AppSettings.load();
        DownloadQueueManager qm = new DownloadQueueManager(settings);
        List<DownloadTask> tasks = qm.getTasks();
        System.out.println("Tasks before removal: " + tasks.size());
        for (DownloadTask t : new ArrayList<>(tasks)) {
            System.out.println("Removing: " + t.getFileName() + " (File: " + t.getOutputFile().getAbsolutePath() + ")");
            boolean ok = qm.removeTask(t, true);
            System.out.println("Removal result (file deleted): " + ok);
        }
        System.out.println("Tasks after removal in memory: " + qm.getTasks().size());
        List<DownloadTask> reloaded = StorageUtils.loadTasks();
        System.out.println("Tasks reloaded from disk: " + reloaded.size());
    }
}
