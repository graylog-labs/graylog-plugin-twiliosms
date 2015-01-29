package org.graylog2.alarmcallbacks.twilio;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Collection;
import java.util.Collections;

public class TwilioSmsAlarmCallbackPlugin implements Plugin {
    @Override
    public Collection<PluginModule> modules() {
        return Collections.<PluginModule>singleton(new TwilioSmsAlarmCallbackModule());
    }

    @Override
    public PluginMetaData metadata() {
        return new TwilioSmsAlarmCallbackMetadata();
    }
}
