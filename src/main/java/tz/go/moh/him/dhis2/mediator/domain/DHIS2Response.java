package tz.go.moh.him.dhis2.mediator.domain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DHIS2Response {
    @JsonProperty("status")
    private String status;

    @JsonProperty("importCount")
    private ImportCount importCount;

    /**
     * Obtain the value of the status field
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the value of the status field
     */
    public void setStatus(String status) {
        this.status = status;
    }

    public ImportCount getImportCount() {
        return importCount;
    }

    public void setImportCount(ImportCount importCount) {
        this.importCount = importCount;
    }

    public static class ImportCount {

        private int imported;
        private int updated;
        private int ignored;
        private int deleted;

        public int getIgnored() {
            return ignored;
        }

        public void setIgnored(int ignored) {
            this.ignored = ignored;
        }

        public int getImported() {
            return imported;
        }

        public void setImported(int imported) {
            this.imported = imported;
        }

        public int getUpdated() {
            return updated;
        }

        public void setUpdated(int updated) {
            this.updated = updated;
        }

        public int getDeleted() {
            return deleted;
        }

        public void setDeleted(int deleted) {
            this.deleted = deleted;
        }
    }

}
