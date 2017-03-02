package com.kennycason.kumo.nlp;

import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.nlp.filter.CompositeFilter;
import com.kennycason.kumo.nlp.filter.Filter;
import com.kennycason.kumo.nlp.filter.StopWordFilter;
import com.kennycason.kumo.nlp.filter.WordSizeFilter;
import com.kennycason.kumo.nlp.normalize.CharacterStrippingNormalizer;
import com.kennycason.kumo.nlp.normalize.LowerCaseNormalizer;
import com.kennycason.kumo.nlp.normalize.Normalizer;
import com.kennycason.kumo.nlp.normalize.TrimToEmptyNormalizer;
import com.kennycason.kumo.nlp.tokenizer.WhiteSpaceWordTokenizer;
import com.kennycason.kumo.nlp.tokenizer.WordTokenizer;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Created by kenny on 7/1/14.
 */
public class FrequencyAnalyzer {

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final int DEFAULT_WORD_MAX_LENGTH = 32;

    public static final int DEFAULT_WORD_MIN_LENGTH = 3;

    public static final int DEFAULT_WORD_FREQUENCIES_TO_RETURN = 50;

    public static final long DEFAULT_URL_LOAD_TIMEOUT = 3000; // 3 sec

    private final Set<String> stopWords = new HashSet<>();

    private WordTokenizer wordTokenizer = new WhiteSpaceWordTokenizer();

    private final List<Filter> filters = new ArrayList<>();

    private final List<Normalizer> normalizers = new ArrayList<>();

    private int wordFrequenciesToReturn = DEFAULT_WORD_FREQUENCIES_TO_RETURN;

    private int maxWordLength = DEFAULT_WORD_MAX_LENGTH;

    private int minWordLength = DEFAULT_WORD_MIN_LENGTH;

    private String characterEncoding = DEFAULT_ENCODING;

    private long urlLoadTimeout = DEFAULT_URL_LOAD_TIMEOUT;

    public FrequencyAnalyzer() {
        this.normalizers.add(new TrimToEmptyNormalizer());
        this.normalizers.add(new CharacterStrippingNormalizer());
        this.normalizers.add(new LowerCaseNormalizer());
    }

    public List<WordFrequency> load(final InputStream fileInputStream) throws IOException {
        return load(IOUtils.readLines(fileInputStream, characterEncoding));
    }

    public List<WordFrequency> load(final File file) throws IOException {
        return this.load(new FileInputStream(file));
    }

    public List<WordFrequency> load(final String filePath) throws IOException {
        return this.load(new File(filePath));
    }

    public List<WordFrequency> load(final URL url) throws IOException {
        final Document doc = Jsoup.parse(url, (int) urlLoadTimeout);
        return load(Collections.singletonList(doc.body().text()));
    }

    public List<WordFrequency> load(final List<String> texts) {
        final List<WordFrequency> wordFrequencies = new ArrayList<>();

        final Map<String, Integer> cloud = buildWordFrequencies(texts, wordTokenizer);
        for (final Entry<String, Integer> wordCount : cloud.entrySet()) {
            wordFrequencies.add(new WordFrequency(wordCount.getKey(), wordCount.getValue()));
        }
        return takeTopFrequencies(wordFrequencies);
    }
    
    public List<WordFrequency> loadWordFrequencies(final List<WordFrequency> wflist) {
        return takeTopFrequencies(wflist);
    }
    
    private Map<String, Integer> buildWordFrequencies(final List<String> texts, final WordTokenizer tokenizer) {
        final Map<String, Integer> wordFrequencies = new HashMap<>();
        for (final String text : texts) {
            final List<String> words = filter(tokenizer.tokenize(text));

            for (final String word : words) {
                final String normalized = normalize(word);
                if (!wordFrequencies.containsKey(normalized)) {
                    wordFrequencies.put(normalized, 1);
                }
                wordFrequencies.put(normalized, wordFrequencies.get(normalized) + 1);
            }
        }
        return wordFrequencies;
    }

    private List<String> filter(final List<String> words) {
        final List<Filter> allFilters = new ArrayList<>();
        allFilters.add(new StopWordFilter(stopWords));
        allFilters.add(new WordSizeFilter(minWordLength, maxWordLength));
        allFilters.addAll(filters);
        final CompositeFilter compositeFilter = new CompositeFilter(allFilters);
        return  words.stream().filter(compositeFilter).collect(Collectors.toList());
    }

    private String normalize(final String word) {
        String normalized = word;
        for (Normalizer normalizer : normalizers) {
            normalized = normalizer.normalize(normalized);
        }
        return normalized;
    }

    private List<WordFrequency> takeTopFrequencies(final Collection<WordFrequency> wordCloudEntities) {
        if (wordCloudEntities.isEmpty()) { return Collections.emptyList(); }

        final List<WordFrequency> sorted =  wordCloudEntities.stream().sorted(WordFrequency::compareTo).collect(Collectors.toList());
        Collections.reverse(sorted);
        return sorted.subList(0, Math.min(sorted.size(), wordFrequenciesToReturn));
    }

    public void setStopWords(final Collection<String> stopWords) {
        this.stopWords.clear();
        this.stopWords.addAll(stopWords);
    }

    public void setWordFrequenciesToReturn(final int wordFrequenciesToReturn) {
        this.wordFrequenciesToReturn = wordFrequenciesToReturn;
    }

    public void setMinWordLength(final int minWordLength) {
        this.minWordLength = minWordLength;
    }

    public void setMaxWordLength(final int maxWordLength) {
        this.maxWordLength = maxWordLength;
    }

    public void setWordTokenizer(final WordTokenizer wordTokenizer) {
        this.wordTokenizer = wordTokenizer;
    }

    public void clearFilters() {
        this.filters.clear();
    }

    public void addFilter(final Filter filter) {
        this.filters.add(filter);
    }

    public void setFilter(final Filter filter) {
        this.filters.clear();
        this.filters.add(filter);
    }

    public void clearNormalizers() {
        this.normalizers.clear();
    }

    public void addNormalizer(final Normalizer normalizer) {
        this.normalizers.add(normalizer);
    }

    public void setNormalizer(final Normalizer normalizer) {
        this.normalizers.clear();
        this.normalizers.add(normalizer);
    }

    public void setCharacterEncoding(final String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setUrlLoadTimeout(final long urlLoadTimeout) {
        this.urlLoadTimeout = urlLoadTimeout;
    }
}
