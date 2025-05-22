package be.blit.mailcleaner;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

@Slf4j
@ApplicationScoped
public class MailCleanerService {

    @Inject
    MailCleanerConfig config;

    @Inject
    BlockedSendersService blockedSendersService;

    @Scheduled(every = "{mail-cleaner.schedule}")
    public void cleanMailboxes() {
        try {
            log.info("Start cleaning mailbox");

            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");

            log.info("Connecting to imap server {}:{}", config.imap().host(), config.imap().port());

            Session session = Session.getDefaultInstance(props);
            Store store = session.getStore();
            store.connect(
                    config.imap().host(),
                    config.imap().port(),
                    config.imap().username(),
                    config.imap().password()
            );

            log.info("Connected to imap server {}:{}", config.imap().host(), config.imap().port());
            log.info("Folder separator: {}", store.getDefaultFolder().getSeparator());

            List<String> folders = Stream.of(store.getDefaultFolder().list("*")).map(Folder::getFullName).toList();

            log.info("Mailbox contains {} folders", folders.size());
            folders.forEach(folder -> log.info("Remote folder: {}", folder));
            log.info("Start cleaning {} folders", config.folders().size());
            config.folders().forEach(folder -> log.info("Folder to clean: {}", folder));

            for (String folderName : config.folders()) {
                log.info("Start cleaning folder \"{}\"", folderName);

                Folder folder = store.getFolder(folderName);
                if (!folder.exists()) {
                    log.warn("Folder \"{}\" does not exist", folderName);
                    continue;
                }

                log.info("Opening folder \"{}\"", folderName);

                folder.open(Folder.READ_WRITE);

                Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.DELETED), false));

                log.info("Found {} messages in folder \"{}\"", messages.length, folderName);

                for (Message message : messages) {
                    Address[] from = message.getFrom();

                    if (from != null && from.length > 0) {
                        String sender = from[0].toString();

                        if (isBlocked(sender)) {
                            log.info("Deleting message \"{}\" from \"{}\"", message.getSubject(), sender);
                            message.setFlag(Flags.Flag.DELETED, true);
                        }
                    }
                }

                folder.close(true);

                log.info("Finished cleaning folder \"{}\"", folderName);
            }

            log.info("Finished cleaning {} folders", config.folders().size());

            store.close();

            log.info("Finished cleaning mailbox");
        } catch (Exception e) {
            log.error("Error cleaning mailbox", e);
        }
    }

    private boolean isBlocked(String sender) {
        return blockedSendersService.getBlockedSenders().stream().anyMatch(sender::contains);
    }

}
