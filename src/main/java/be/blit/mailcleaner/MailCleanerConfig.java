package be.blit.mailcleaner;

import io.smallrye.config.ConfigMapping;

import java.util.List;

@ConfigMapping(prefix = "mail-cleaner")
public interface MailCleanerConfig {

    ImapConfig imap();

    List<String> folders();

    List<String> blockedSenders();

    String schedule();

    interface ImapConfig {

        String host();

        int port();

        String username();

        String password();

    }

}
