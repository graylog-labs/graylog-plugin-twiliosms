package org.graylog2.alarmcallbacks.twilio;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.Version;

import java.net.URI;

public class TwilioSmsAlarmCallbackMetadata implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return TwilioSmsAlarmCallback.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return "Twilio SMS Alarmcallback Plugin";
    }

    @Override
    public String getAuthor() {
        return "TORCH GmbH";
    }

    @Override
    public URI getURL() {
        return URI.create("http://www.torch.sh");
    }

    @Override
    public Version getVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public String getDescription() {
        return "Alarm callback plugin that sends all stream alerts as SMS to a defined phone number.";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(0, 21, 0);
    }
}
