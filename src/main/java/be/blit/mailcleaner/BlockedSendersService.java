package be.blit.mailcleaner;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class BlockedSendersService {

    private static final Logger LOG = LoggerFactory.getLogger(BlockedSendersService.class);

    private final Set<String> blockedSenders;
    private final String configFile;

    public BlockedSendersService(MailCleanerConfig config) {
        this.blockedSenders = new HashSet<>(config.blockedSenders());

        this.configFile = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.config.locations", String.class)
                .orElseThrow(() -> new IllegalStateException("Config path not set"))
                .replace("file:", "");

        LOG.info("Config file: {}", this.configFile);
    }

    public Set<String> getBlockedSenders() {
        return Collections.unmodifiableSet(blockedSenders);
    }

    public void addBlockedSender(String sender) {
        LOG.info("Adding blocked sender \"{}\"", sender);
        blockedSenders.add(sender);
        saveToFile();
    }

    public void removeBlockedSender(String sender) {
        LOG.info("Removing blocked sender \"{}\"", sender);
        blockedSenders.remove(sender);
        saveToFile();
    }

    private void saveToFile() {
        try (InputStream in = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(in);

            if (config == null) {
                config = new HashMap<>();
            }

            Map<String, Object> mailCleaner = (Map<String, Object>) config.get("mail-cleaner");

            if (mailCleaner == null) {
                mailCleaner = new HashMap<>();
                config.put("mail-cleaner", mailCleaner);
            }

            mailCleaner.put("blocked-senders", blockedSenders);

            try (Writer writer = new FileWriter(configFile)) {
                yaml.dump(config, writer);
            }
        } catch (Exception e) {
            LOG.error("Error saving config file", e);
        }
    }

}