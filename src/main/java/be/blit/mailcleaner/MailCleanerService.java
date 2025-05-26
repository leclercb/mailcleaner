package be.blit.mailcleaner;

import be.blit.mailcleaner.rspamd.RspamdResponse;
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
    RspamdService rspamdService;

    @Scheduled(every = "{mail-cleaner.schedule}")
    public void cleanMailboxes() {
        try {
            log.info("Start cleaning mailbox");

            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");

            log.debug("Connecting to imap server {}:{}", config.imap().host(), config.imap().port());

            Session session = Session.getDefaultInstance(props);
            Store store = session.getStore();
            store.connect(
                    config.imap().host(),
                    config.imap().port(),
                    config.imap().username(),
                    config.imap().password()
            );

            log.debug("Connected to imap server {}:{}", config.imap().host(), config.imap().port());
            log.debug("Folder separator: {}", store.getDefaultFolder().getSeparator());

            List<String> remoteFolders = Stream.of(store.getDefaultFolder().list("*")).map(Folder::getFullName).toList();

            log.debug("Mailbox contains {} folders: {}", remoteFolders.size(), remoteFolders);

            List<String> folders = config.folders().orElseGet(Collections::emptyList);

            log.debug("Start cleaning {} folders: {}", folders.size(), folders);

            for (String folderName : folders) {
                log.info("Start cleaning folder \"{}\"", folderName);

                Folder folder = store.getFolder(folderName);
                if (!folder.exists()) {
                    log.warn("Folder \"{}\" does not exist", folderName);
                    continue;
                }

                log.debug("Opening folder \"{}\"", folderName);

                folder.open(Folder.READ_WRITE);

                Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.DELETED), false));

                log.debug("Found {} messages in folder \"{}\"", messages.length, folderName);

                for (Message message : messages) {
                    Address[] from = message.getFrom();

                    String sender = (from != null && from.length > 0) ? from[0].toString() : null;

                    if (sender != null && isBlocked(sender)) {
                        log.info("Sender is in blocked senders list; deleting message \"{}\" from \"{}\"", message.getSubject(), sender);
                        message.setFlag(Flags.Flag.DELETED, true);
                        continue;
                    }

                    RspamdResponse rspamdResponse = rspamdService.checkMessage(message);

                    if (rspamdResponse != null && rspamdResponse.isSpam()) {
                        log.info("Message was flagged as spam by Rspamd ({}/{}); deleting message {}\"{}\" from \"{}\"",
                                rspamdResponse.getScore(),
                                rspamdResponse.getRequiredScore(),
                                config.rspamd().dryRun() ? "(dry run) " : "",
                                message.getSubject(),
                                sender);

                        if (!config.rspamd().dryRun()) {
                            message.setFlag(Flags.Flag.DELETED, true);
                        }
                    }
                }

                folder.close(true);

                log.info("Finished cleaning folder \"{}\"", folderName);
            }

            log.debug("Finished cleaning {} folders", folders.size());

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
