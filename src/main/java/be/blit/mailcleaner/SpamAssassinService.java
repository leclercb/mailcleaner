package be.blit.mailcleaner;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

@Slf4j
@Singleton
public class SpamAssassinService {

    @Inject
    MailCleanerConfig config;

    public boolean isSpamAssassinEnabled() {
        return config.spamassassin().isPresent() &&
                config.spamassassin().get().host().isPresent() &&
                config.spamassassin().get().port().isPresent();
    }

    public boolean isSpam(String message) throws Exception {
        if (!isSpamAssassinEnabled()) {
            return false;
        }

        try {
            try (Socket socket = new Socket(config.spamassassin().get().host().get(), config.spamassassin().get().port().get());
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                writer.write("CHECK SPAMC/1.2\r\n");
                writer.write("Content-length: " + message.length() + "\r\n");
                writer.write("\r\n");
                writer.write(message);
                writer.flush();

                String line;
                boolean isSpam = false;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Spam:")) {
                        isSpam = line.contains("True");
                        break;
                    }
                }

                return isSpam;
            }
        } catch (Exception e) {
            log.error("Error while communicating with SpamAssassin", e);
            throw e;
        }
    }

}
