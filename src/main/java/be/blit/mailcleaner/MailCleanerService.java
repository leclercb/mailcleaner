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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

    @Inject
    SpamAssassinService spamAssassinService;

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

            List<String> remoteFolders = Stream.of(store.getDefaultFolder().list("*")).map(Folder::getFullName).toList();

            log.info("Mailbox contains {} folders", remoteFolders.size());
            remoteFolders.forEach(folder -> log.info("Remote folder: {}", folder));

            List<String> folders = config.folders().orElseGet(Collections::emptyList);

            log.info("Start cleaning {} folders", folders.size());
            folders.forEach(folder -> log.info("Folder to clean: {}", folder));

            for (String folderName : folders) {
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

                    String sender = (from != null && from.length > 0) ? from[0].toString() : null;

                    if (sender != null && isBlocked(sender)) {
                        log.info("Sender is in blocked senders list; deleting message \"{}\" from \"{}\"", message.getSubject(), sender);
                        message.setFlag(Flags.Flag.DELETED, true);
                        continue;
                    }

                    if (spamAssassinService.isSpamAssassinEnabled()) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        message.writeTo(out);
                        String eml = out.toString(StandardCharsets.UTF_8);

                        if (spamAssassinService.isSpam(eml)) {
                            log.info("Message was flagged as spam by SpamAssassin; deleting message \"{}\" from \"{}\"", message.getSubject(), sender);
                            //message.setFlag(Flags.Flag.DELETED, true);
                        }
                    }
                }

                folder.close(true);

                log.info("Finished cleaning folder \"{}\"", folderName);
            }

            log.info("Finished cleaning {} folders", folders.size());

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
