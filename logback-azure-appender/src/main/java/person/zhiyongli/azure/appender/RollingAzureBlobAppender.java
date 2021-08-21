package person.zhiyongli.azure.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.status.ErrorStatus;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class RollingAzureBlobAppender<E extends ILoggingEvent> extends AppenderBase<E> {

    protected Layout<E> layout;

    public void setLayout(Layout<E> layout) {
        this.layout = layout;
    }


    protected CloudBlobContainer container;
    protected CloudAppendBlob blob;

    protected String connectionString;
    protected String blobName;
    protected String containerName;

    protected Date date;

    public void setBlobName(String blobName) {
        this.blobName = blobName;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void start() {
        if (this.layout == null) {
            this.addStatus(new ErrorStatus("No layout set for the appender named \"" + this.name + "\".", this));
            return;
        }
        System.out.println("Starting RollingAzureBlobAppender...");

        if (connectionString == null) {
            System.err.println("RollingAzureBlobAppender requires a connectionString. Add this to the appender configuration.");
            return;
        } else {
            System.out.println("RollingAzureBlobAppender use connectionString '" + connectionString + "'.");
        }

        if (blobName == null) {
            System.err.println("RollingAzureBlobAppender requires a blobName. Add this to the appender configuration.");
            return;
        } else {
            System.out.println("RollingAzureBlobAppender use blob name '" + blobName);
        }

        if (containerName == null) {
            System.err.println("RollingAzureBlobAppender requires a containerName. Add this to the appender configuration.");
            return;
        } else {
            System.out.println("RollingAzureBlobAppender use container name '" + containerName);
        }

        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(containerName);
            roll();
        } catch (Exception exception) {
            System.err.println("RollingAzureBlobAppender: Exception initializing container. " + exception + " : " + exception.getMessage());
            return;
        }

        super.start();
    }

    @Override
    protected void append(E event) {
        if (differentDay(date, new Date())) {
            try {
                roll();
            } catch (Exception e) {
                this.stop();
            }
        }



        try {
            blob.appendText(event.getFormattedMessage()+"\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void roll() throws Exception {

        date = new Date();
        String stamp = new SimpleDateFormat("yyyy-MM-dd").format(date);
        String blobName = this.blobName + "_" + stamp + ".log";
        blob = container.getAppendBlobReference(blobName);

        if (!blob.exists()) {
            blob.createOrReplace();

            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("date", stamp);
            metadata.put("name", blobName);

            blob.setMetadata(metadata);
            blob.uploadMetadata();
        }
    }

    private boolean differentDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR) ||
                cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR);
    }

}