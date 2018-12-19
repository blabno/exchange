package bisq.httpapi.model;

import java.util.List;

public class BackupList {

    @SuppressWarnings("WeakerAccess")
    public List<String> backups;

    public BackupList(List<String> backups) {
        this.backups = backups;
    }
}
