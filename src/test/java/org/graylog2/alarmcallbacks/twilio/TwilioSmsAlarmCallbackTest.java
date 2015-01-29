package org.graylog2.alarmcallbacks.twilio;

import com.google.common.collect.ImmutableMap;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Sms;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TwilioSmsAlarmCallbackTest {
    private static final Configuration VALID_CONFIGURATION = new Configuration(ImmutableMap.<String, Object>of(
            "account_sid", "TEST_account_sid",
            "auth_token", "TEST_auth_token",
            "from_number", "TEST_from_number",
            "to_number", "TEST_to_number"));

    @Mock
    private TwilioRestClient twilioClient;
    @Mock
    private Account twilioAccount;
    @Mock
    private SmsFactory smsFactory;
    @Mock
    private Stream mockStream;
    @Mock
    private AlertCondition.CheckResult mockCheckResult;
    private TwilioSmsAlarmCallback transport;

    @Before
    public void setUp() throws TwilioRestException {
        when(twilioClient.getAccount()).thenReturn(twilioAccount);
        when(twilioAccount.getSmsFactory()).thenReturn(smsFactory);
        when(smsFactory.create(anyMapOf(String.class, String.class))).thenReturn(mock(Sms.class));

        transport = new TwilioSmsAlarmCallback();
    }

    @Test
    public void initializeSucceedsWithCompleteConfiguration() throws AlarmCallbackConfigurationException {
        transport.initialize(VALID_CONFIGURATION);
    }

    @Test(expected = ConfigurationException.class)
    public void initializeFailsWhenAccountSidIsMissing() throws AlarmCallbackConfigurationException, ConfigurationException {
        transport.initialize(new Configuration(ImmutableMap.<String, Object>of(
                "auth_token", "Test",
                "from_number", "Test",
                "to_number", "Test"
        )));
        transport.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void initializeFailsWhenAuthTokenIsMissing() throws AlarmCallbackConfigurationException, ConfigurationException {
        transport.initialize(new Configuration(ImmutableMap.<String, Object>of(
                "account_sid", "Test",
                "from_number", "Test",
                "to_number", "Test"
        )));
        transport.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void initializeFailsWhenFromNumberIsMissing() throws AlarmCallbackConfigurationException, ConfigurationException {
        transport.initialize(new Configuration(ImmutableMap.<String, Object>of(
                "account_sid", "Test",
                "auth_token", "Test",
                "to_number", "Test"
        )));
        transport.checkConfiguration();
    }

    @Test(expected = ConfigurationException.class)
    public void initializeFailsWhenToNumberIsMissing() throws AlarmCallbackConfigurationException, ConfigurationException {
        transport.initialize(new Configuration(ImmutableMap.<String, Object>of(
                "account_sid", "Test",
                "auth_token", "Test",
                "from_number", "Test"
        )));
        transport.checkConfiguration();
    }

    @Test
    public void testGetRequestedConfiguration() throws Exception {
        assertThat(transport.getRequestedConfiguration().asList().keySet(),
                hasItems("account_sid", "auth_token", "from_number", "to_number"));
    }

    @Test
    public void testTransportAlarm() throws Exception {
        when(mockCheckResult.getResultDescription()).thenReturn("Test");

        transport.initialize(VALID_CONFIGURATION);
        transport.call(mockStream, mockCheckResult, twilioClient);

        final Map<String, String> expectedParameters = ImmutableMap.of(
                "To", "TEST_to_number",
                "From", "TEST_from_number",
                "Body", "[Graylog] Test"
        );
        verify(smsFactory).create(expectedParameters);
    }

    @Test
    public void transportAlarmTruncatesBody() throws Exception {
        final StringBuilder descriptionBuilder = new StringBuilder(200);
        for (int i = 0; i < 20; i++) {
            descriptionBuilder.append("0123456789");
        }
        when(mockCheckResult.getResultDescription()).thenReturn(descriptionBuilder.toString());

        transport.initialize(VALID_CONFIGURATION);
        transport.call(mockStream, mockCheckResult, twilioClient);

        final String expectedBody = "[Graylog] "
                + "01234567890123456789012345678901234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789";

        final Map<String, String> expectedParameters = ImmutableMap.of(
                "To", "TEST_to_number",
                "From", "TEST_from_number",
                "Body", expectedBody
        );
        verify(smsFactory).create(expectedParameters);
    }

    @Test
    public void transportAlarmDoesNotThrowException() throws Exception {
        transport.initialize(VALID_CONFIGURATION);
        when(twilioClient.getAccount()).thenThrow(new RuntimeException("BOO!"));

        transport.call(mockStream, mockCheckResult, twilioClient);
    }

    @Test
    public void testGetAttributes() throws AlarmCallbackConfigurationException {
        transport.initialize(VALID_CONFIGURATION);
        final Map<String, Object> attributes = transport.getAttributes();

        assertThat((String) attributes.get("auth_token"), equalTo("****"));
    }

    @Test
    public void testGetName() {
        assertThat(transport.getName(), equalTo("Twilio SMS AlarmCallback"));
    }
}