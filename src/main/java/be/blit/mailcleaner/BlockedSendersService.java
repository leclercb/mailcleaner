package be.blit.mailcleaner;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
@Singleton
public class BlockedSendersService {

    private final Set<String> blockedSenders;
    private final File configFile;

    public BlockedSendersService(MailCleanerConfig config) {
        this.blockedSenders = new TreeSet<>(config.blockedSenders());

        this.configFile = new File(ConfigProvider.getConfig()
                .getOptionalValue("quarkus.config.locations", String.class)
                .orElseThrow(() -> new IllegalStateException("Config path not set"))
                .replace("file:", ""));

        log.info("Config file: {}", this.configFile.getAbsolutePath());
    }

    public Set<String> getBlockedSenders() {
        return Collections.unmodifiableSet(blockedSenders);
    }

    public void addBlockedSender(String sender) {
        log.info("Adding blocked sender \"{}\"", sender);
        blockedSenders.add(sender);
        saveToFile();
    }

    public void removeBlockedSender(String sender) {
        log.info("Removing blocked sender \"{}\"", sender);
        blockedSenders.remove(sender);
        saveToFile();
    }

    private void saveToFile() {
        try (InputStream in = new FileInputStream(configFile)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Yaml yaml = new Yaml(options);

            Map<String, Object> config = yaml.load(in);

            if (config == null) {
                config = new HashMap<>();
            }

            Map<String, Object> mailCleaner = (Map<String, Object>) config.get("mail-cleaner");

            if (mailCleaner == null) {
                mailCleaner = new HashMap<>();
                config.put("mail-cleaner", mailCleaner);
            }

            mailCleaner.put("blocked-senders", blockedSenders.toArray());

            try (Writer writer = new FileWriter(configFile)) {
                yaml.dump(config, writer);
            }
        } catch (Exception e) {
            log.error("Error saving config file", e);
        }
    }

}