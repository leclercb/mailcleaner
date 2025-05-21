package be.blit.mailcleaner;

import io.quarkus.runtime.util.StringUtil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@ApplicationScoped
public class MailCleanerService {

    private static final Logger LOG = LoggerFactory.getLogger(MailCleanerService.class);

    @Inject
    MailCleanerConfig config;

    @Scheduled(every = "{mail-cleaner.schedule}")
    public void cleanMailboxes() {
        LOG.info("Start cleaning mailbox");

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");

            if (StringUtil.isNullOrEmpty(config.imap().host())) {
                LOG.warn("Imap host not set, stopping cleaning mailbox");
                return;
            }

            LOG.info("Connecting to imap server {}:{}", config.imap().host(), config.imap().port());

            Session session = Session.getDefaultInstance(props);
            Store store = session.getStore();
            store.connect(
                    config.imap().host(),
                    config.imap().port(),
                    config.imap().username(),
                    config.imap().password()
            );

            LOG.info("Connected to imap server {}:{}", config.imap().host(), config.imap().port());

            LOG.info("Start cleaning folders: {}", config.folders());

            for (String folderName : config.folders()) {
                LOG.info("Start cleaning folder \"{}\"", folderName);

                Folder folder = store.getFolder(folderName);
                if (!folder.exists()) {
                    LOG.warn("Folder \"{}\" does not exist", folderName);
                    continue;
                }

                LOG.info("Opening folder \"{}\"", folderName);

                folder.open(Folder.READ_WRITE);

                Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.DELETED), false));

                LOG.info("Found {} messages in folder \"{}\"", messages.length, folderName);

                for (Message message : messages) {
                    Address[] from = message.getFrom();

                    if (from != null && from.length > 0) {
                        String sender = from[0].toString();

                        if (isBlocked(sender)) {
                            LOG.info("Deleting message \"{}\" from \"{}\"", message.getSubject(), sender);
                            message.setFlag(Flags.Flag.DELETED, true);
                        }
                    }
                }

                folder.close(true);

                LOG.info("Finished cleaning folder \"{}\"", folderName);
            }

            store.close();

            LOG.info("Finished cleaning mailbox");
        } catch (Exception e) {
            LOG.error("Error cleaning mailbox", e);
        }
    }

    private boolean isBlocked(String sender) {
        return config.blockedSenders().stream().anyMatch(sender::contains);
    }

}
