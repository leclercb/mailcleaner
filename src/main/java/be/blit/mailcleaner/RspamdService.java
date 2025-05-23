package be.blit.mailcleaner;

import be.blit.mailcleaner.rspamd.RspamdClient;
import be.blit.mailcleaner.rspamd.RspamdResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.mail.Message;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Singleton
public class RspamdService {

    @Inject
    @RestClient
    RspamdClient rspamdClient;

    public RspamdResponse checkMessage(Message message) throws Exception {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            message.writeTo(out);
            String eml = out.toString(StandardCharsets.UTF_8);

            return rspamdClient.checkMessage(eml);
        } catch (Exception e) {
            log.error("Error while communicating with Rspamd", e);
            return null;
        }
    }

}
