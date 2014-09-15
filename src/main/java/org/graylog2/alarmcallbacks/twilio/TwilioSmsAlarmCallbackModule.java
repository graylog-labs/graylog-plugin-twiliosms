package org.graylog2.alarmcallbacks.twilio;

import org.graylog2.plugin.PluginModule;

public class TwilioSmsAlarmCallbackModule extends PluginModule {
    @Override
    protected void configure() {
        registerPlugin(TwilioSmsAlarmCallbackMetadata.class);
        addAlarmCallback(TwilioSmsAlarmCallback.class);
    }
}
