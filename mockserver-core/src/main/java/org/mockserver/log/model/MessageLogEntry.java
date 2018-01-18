package org.mockserver.log.model;

import org.mockserver.model.HttpRequest;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @author jamesdbloom
 */
public class MessageLogEntry extends LogEntry {
    private final static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final String message;
    protected Date timeStamp = new Date();

    public MessageLogEntry(@Nullable HttpRequest httpRequest, String message) {
        super(httpRequest);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeStamp() {
        return dateFormat.format(timeStamp);
    }
}
