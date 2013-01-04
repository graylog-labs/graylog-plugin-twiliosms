/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.twiliosmstransport.transport;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Account;
import java.util.HashMap;
import java.util.Map;
import org.graylog2.plugin.alarms.Alarm;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class TwilioSmsTransport implements Transport {

    public static final String NAME = "Twilio SMS";
    public static final String USER_FIELD_NAME = "Phone number for SMS";
    
    public static final int MAX_MSG_LENGTH = 140;

    private Map<String, String> config;
    
    public void initialize(Map<String, String> config) throws TransportConfigurationException {
        this.config = config;
        
        // All requested configuration is actually required.
        for (String key : getRequestedConfiguration().keySet()) {
            if (!configSet(key)) {
                throw new TransportConfigurationException("Missing configuration option: " + key);
            }
        }
    }

    public Map<String, String> getRequestedConfiguration() {
        Map<String, String> requestedConfig = new HashMap<String, String>();
        
        requestedConfig.put("account_sid", "Twilio account SID");
        requestedConfig.put("auth_token", "Twilio authorization token");
        requestedConfig.put("from_number", "The from number of sent SMS. Must be a valid phone number in your account.");
        
        return requestedConfig;
    }

    public void transportAlarm(Alarm alarm) {
        for (AlarmReceiver receiver : alarm.getReceivers(this)) {
            try {
                send(alarm, receiver);
            } catch(Exception e) {
                System.out.println("Could not send alarm via " + NAME);
                e.printStackTrace();
            }
        }
    }

    public String getName() {
        return NAME;
    }

    public String getUserFieldName() {
        return USER_FIELD_NAME;
    }
    
    private void send(Alarm alarm, AlarmReceiver receiver) throws TwilioRestException {
        final TwilioRestClient client = new TwilioRestClient(config.get("account_sid"), config.get("auth_token"));
        final Account mainAccount = client.getAccount();
        final SmsFactory smsFactory = mainAccount.getSmsFactory();
        
        final Map<String, String> smsParams = new HashMap<String, String>();
        smsParams.put("To", receiver.getAddress(this));
        smsParams.put("From", config.get("from_number"));
        smsParams.put("Body", buildMessage(alarm));
        
        smsFactory.create(smsParams);
    }
    
    private String buildMessage(Alarm alarm) {
        String msg = "[graylog2] " + alarm.getDescription();
        
        int maxLength = (msg.length() < MAX_MSG_LENGTH) ? msg.length() : MAX_MSG_LENGTH;
        return msg.substring(0, maxLength);
    }
    
    private boolean configSet(String key) {
        return config != null && config.containsKey(key) && config.get(key) != null && !config.get(key).isEmpty();
    }
    
}
