package ru.deevdenis.ai.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String url;
    private String model;
    private Splitter splitter;
    private Similarity similarity;
    private Embedding embedding;

    @Setter
    @Getter
    public static class Splitter {
        private int chunkSize;
        private int minChars;
        private int minTokens;
        private int maxChunks;
        private boolean keepSeparator;
    }

    @Setter
    @Getter
    public static class Similarity {
        private float threshold;
        private int topK;
    }

    @Setter
    @Getter
    public static class Embedding {
        private String prefix;
        private String indexName;
    }
}
