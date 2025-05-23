package be.blit.mailcleaner;

import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "mail-cleaner")
public interface MailCleanerConfig {

    String schedule();

    ImapConfig imap();

    Optional<SpamAssassinConfig> spamassassin();

    Optional<List<String>> folders();

    Optional<List<String>> blockedSenders();

    interface ImapConfig {

        String host();

        int port();

        String username();

        String password();

    }

    interface SpamAssassinConfig {

        Optional<String> host();

        Optional<Integer> port();

    }

}
