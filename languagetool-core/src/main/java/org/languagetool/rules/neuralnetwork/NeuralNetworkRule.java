package org.languagetool.rules.neuralnetwork;

import org.jetbrains.annotations.NotNull;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.rules.Categories;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.ScoredConfusionSet;
import org.languagetool.tools.Tools;

import java.io.*;
import java.util.*;

public class NeuralNetworkRule extends Rule {
  private final List<String> subjects;
  private final List<Optional<String>> descriptions;
  private double minScore;

  private static final int CONTEXT_LENGTH = 5;
  protected Classifier classifier;

  private final String id;

  public NeuralNetworkRule(ResourceBundle messages, Language language, ScoredConfusionSet confusionSet, Dictionary dictionary, Matrix embedding) {
    super(messages);
    super.setCategory(Categories.TYPOS.getCategory(messages));

    this.subjects = confusionSet.getConfusionTokens();
    this.descriptions = confusionSet.getTokenDescriptions();
    this.minScore = confusionSet.getScore();

    final InputStream W1Stream = streamFor(language, "W_fc1.txt");
    final InputStream b1Stream = streamFor(language, "b_fc1.txt");
    try {
      final InputStream W2Stream = streamFor(language, "W_fc2.txt");
      final InputStream b2Stream = streamFor(language, "b_fc2.txt");
      System.out.println("deep rule for" + confusionSet.toString());
      classifier = new TwoLayerClassifier(dictionary, embedding, W1Stream, b1Stream, W2Stream, b2Stream);
    } catch (RuntimeException e) {
      classifier = new SingleLayerClassifier(dictionary, embedding, W1Stream, b1Stream);
    }

    this.id = createId(language);
  }

  public NeuralNetworkRule(ResourceBundle messages, Language language, ScoredConfusionSet confusionSet, Classifier classifier) {
    super(messages);
    super.setCategory(Categories.TYPOS.getCategory(messages));

    this.subjects = confusionSet.getConfusionTokens();
    this.descriptions = confusionSet.getTokenDescriptions();
    this.minScore = confusionSet.getScore();

    this.classifier = classifier;

    this.id = createId(language);
  }

  @NotNull
  private String createId(Language language) {
    return language.getShortCode().toUpperCase() + "_" + subjects.get(0) + "_VS_" + subjects.get(1) + "_NEURALNETWORK";
  }

  private InputStream streamFor(Language language, String filename) {
    String folderName = String.join("_", subjects);
    return JLanguageTool.getDataBroker().getFromResourceDirAsStream("/" + language.getShortCode() + "/neuralnetwork/" + folderName + "/" + filename);
  }

  public List<String> getSubjects() {
    return subjects;
  }

  protected double getMinScore() {
    return minScore;
  }

  public void setMinScore(double minScore) {
    this.minScore = minScore;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getDescription() {
    // TODO use resource
    return "Possible Typo '" + getSubjects().get(0) + "'/'" + getSubjects().get(1) + "'";
  }

  private Suggestion getSuggestion(float[] y) {
    String suggestion;
    boolean unsure;
    if(y[0] > y[1]) {
      suggestion = getSubjects().get(0);
      unsure = !(y[0] > getMinScore() && y[1] < -getMinScore());
    } else {
      suggestion = getSubjects().get(1);
      unsure = !(y[1] > getMinScore() && y[0] < -getMinScore());
    }
    return new Suggestion(suggestion, unsure);
  }

  @Override
  public RuleMatch[] match(AnalyzedSentence sentence) throws IOException {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();
    for(int i = 1; i < tokens.length; i++) {
//    for(int i = CONTEXT_LENGTH/2; i < tokens.length - CONTEXT_LENGTH/2; i++) {
      String token = tokens[i].getToken();
      if(getSubjects().contains(token)) {
        final String[] context = getContext(tokens, i);
        final float[] y = classifier.getScores(context);
        final Suggestion suggestion = getSuggestion(y);
        if(!suggestion.matches(token)) {
          if (!suggestion.isUnsure()) {
            ruleMatches.add(createRuleMatch(tokens[i], suggestion, y));
          } else {
            System.out.println("unsure: " + getMessage(suggestion, y) + Arrays.toString(context));
          }
        }
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

  @NotNull
  private String[] getContext(AnalyzedTokenReadings[] tokens, int center) {
    String[] context = new String[CONTEXT_LENGTH - 1];
    for(int i = 0; i < CONTEXT_LENGTH/2; i++) {
      context[i] = safeGetToken(tokens, center - CONTEXT_LENGTH/2 + i);
    }
    for(int i = 0; i < CONTEXT_LENGTH/2; i++) {
      context[CONTEXT_LENGTH/2 + i] = safeGetToken(tokens, center + 1 + i);
    }
    return context;
  }

  private static String safeGetToken(AnalyzedTokenReadings[] arr, int i) {
    if (i <= 0 || i >= arr.length) {
      return ".";
    }
    return arr[i].getToken();
  }

  @NotNull
  private RuleMatch createRuleMatch(AnalyzedTokenReadings token, Suggestion suggestion, float[] y) {
    String msg = getMessage(suggestion, y);
    int pos = token.getStartPos();
    RuleMatch ruleMatch = new RuleMatch(this, pos, pos + token.getToken().length(), msg);
    ruleMatch.setSuggestedReplacement(suggestion.toString());
    return ruleMatch;
  }

  @NotNull
  private String getMessage(Suggestion suggestion, float[] y) {
    String msg;
    int suggestionIndex = suggestion.matches(subjects.get(0)) ? 0 : 1;
    int wrongWordIndex = (suggestionIndex + 1) % 2;
    if (descriptions.get(suggestionIndex).isPresent() && descriptions.get(wrongWordIndex).isPresent()) {
      msg = Tools.i18n(messages, "neural_network_suggest_with_description",
              subjects.get(suggestionIndex), descriptions.get(suggestionIndex).get(),
              subjects.get(wrongWordIndex), descriptions.get(wrongWordIndex).get());
    } else {
      msg = Tools.i18n(messages, "neural_network_suggest", subjects.get(suggestionIndex), subjects.get(wrongWordIndex));
    }
    if(suggestion.isUnsure()) {
      msg = "(low certainty) " + msg;
    }
    return msg + " " + certaintiesToString(y);
  }

  private String certaintiesToString(float[] y) {
    return String.format(Locale.US, "[%4.2f, %4.2f]", y[0], y[1]);
  }
}
