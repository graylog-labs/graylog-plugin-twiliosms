/**
 * Copyright 2013-2014 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.alarmcallbacks.twilio;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Sms;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.lang.Math.min;

public class TwilioSmsAlarmCallback implements AlarmCallback {
    private static final Logger LOG = LoggerFactory.getLogger(TwilioSmsAlarmCallback.class);

    private static final String NAME = "Twilio SMS AlarmCallback";
    private static final int MAX_MSG_LENGTH = 140;

    private static final String CK_ACCOUNT_SID = "account_sid";
    private static final String CK_AUTH_TOKEN = "auth_token";
    private static final String CK_FROM_NUMBER = "from_number";
    private static final String CK_TO_NUMBER = "to_number";
    private static final String[] MANDATORY_CONFIGURATION_KEYS = new String[]{
            CK_ACCOUNT_SID, CK_AUTH_TOKEN, CK_FROM_NUMBER, CK_TO_NUMBER
    };
    private static final List<String> SENSITIVE_CONFIGURATION_KEYS = ImmutableList.of(CK_AUTH_TOKEN);

    private Configuration configuration;

    @Override
    public void initialize(final Configuration config) throws AlarmCallbackConfigurationException {
        this.configuration = config;
    }

    @Override
    public void call(Stream stream, AlertCondition.CheckResult result) throws AlarmCallbackException {
        final TwilioRestClient twilioClient = new TwilioRestClient(
                configuration.getString(CK_ACCOUNT_SID), configuration.getString(CK_AUTH_TOKEN));

        call(stream, result, twilioClient);
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest cr = new ConfigurationRequest();

        cr.addField(new TextField(CK_ACCOUNT_SID, "Account SID", "", "Twilio account SID",
                ConfigurationField.Optional.NOT_OPTIONAL));
        cr.addField(new TextField(CK_AUTH_TOKEN, "Authorization Token", "", "Twilio authorization token",
                ConfigurationField.Optional.NOT_OPTIONAL));
        cr.addField(new TextField(CK_FROM_NUMBER, "Sender Phone Number", "",
                "The phone number of sent SMS. Must be a valid phone number in your account.",
                ConfigurationField.Optional.NOT_OPTIONAL));
        cr.addField(new TextField(CK_TO_NUMBER, "Recipient Phone Number", "",
                "The phone number of the recipient of the SMS.",
                ConfigurationField.Optional.NOT_OPTIONAL));

        return cr;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Maps.transformEntries(configuration.getSource(), new Maps.EntryTransformer<String, Object, Object>() {
            @Override
            public Object transformEntry(String key, Object value) {
                if (SENSITIVE_CONFIGURATION_KEYS.contains(key)) {
                    return "****";
                }
                return value;
            }
        });
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        for (String key : MANDATORY_CONFIGURATION_KEYS) {
            if (!configuration.stringIsSet(key)) {
                throw new ConfigurationException(key + " is mandatory and must not be empty.");
            }
        }
    }


    @VisibleForTesting
    void call(final Stream stream, final AlertCondition.CheckResult result, final TwilioRestClient twilioClient) {
        try {
            send(twilioClient, result);
        } catch (Exception e) {
            LOG.error("Could not send alarm via Twilio SMS", e);
        }
    }

    public String getName() {
        return NAME;
    }

    private void send(final TwilioRestClient client, final AlertCondition.CheckResult result)
            throws TwilioRestException {
        final Account mainAccount = client.getAccount();
        final SmsFactory smsFactory = mainAccount.getSmsFactory();

        final Map<String, String> smsParams = ImmutableMap.of(
                "To", configuration.getString(CK_TO_NUMBER),
                "From", configuration.getString(CK_FROM_NUMBER),
                "Body", buildMessage(result));

        final Sms sms = smsFactory.create(smsParams);

        LOG.debug("Sent SMS with status {}: {}", sms.getStatus(), sms.getBody());
    }

    private String buildMessage(final AlertCondition.CheckResult result) {
        final String msg = "[Graylog] " + result.getResultDescription();

        return msg.substring(0, min(msg.length(), MAX_MSG_LENGTH));
    }
}