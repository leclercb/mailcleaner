package be.blit.mailcleaner.rspamd;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class RspamdResponse {

    @JsonProperty("is_skipped")
    private boolean isSkipped;

    private double score;

    @JsonProperty("required_score")
    private double requiredScore;

    private String action;

    private Map<String, Symbol> symbols;

    private List<String> urls;
    private List<String> emails;

    @JsonProperty("message-id")
    private String messageId;

    @Data
    @NoArgsConstructor
    public static class Symbol {

        private String name;
        private double score;
        private List<String> options;

    }

    public boolean isSpam() {
        return score >= requiredScore;
    }

}