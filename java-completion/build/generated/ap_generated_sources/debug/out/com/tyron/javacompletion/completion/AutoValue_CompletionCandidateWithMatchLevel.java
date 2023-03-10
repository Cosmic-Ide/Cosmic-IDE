package com.tyron.javacompletion.completion;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_CompletionCandidateWithMatchLevel extends CompletionCandidateWithMatchLevel {

  private final CompletionCandidate completionCandidate;

  private final CompletionPrefixMatcher.MatchLevel matchLevel;

  AutoValue_CompletionCandidateWithMatchLevel(
      CompletionCandidate completionCandidate,
      CompletionPrefixMatcher.MatchLevel matchLevel) {
    if (completionCandidate == null) {
      throw new NullPointerException("Null completionCandidate");
    }
    this.completionCandidate = completionCandidate;
    if (matchLevel == null) {
      throw new NullPointerException("Null matchLevel");
    }
    this.matchLevel = matchLevel;
  }

  @Override
  public CompletionCandidate getCompletionCandidate() {
    return completionCandidate;
  }

  @Override
  public CompletionPrefixMatcher.MatchLevel getMatchLevel() {
    return matchLevel;
  }

  @Override
  public String toString() {
    return "CompletionCandidateWithMatchLevel{"
        + "completionCandidate=" + completionCandidate + ", "
        + "matchLevel=" + matchLevel
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof CompletionCandidateWithMatchLevel) {
      CompletionCandidateWithMatchLevel that = (CompletionCandidateWithMatchLevel) o;
      return this.completionCandidate.equals(that.getCompletionCandidate())
          && this.matchLevel.equals(that.getMatchLevel());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= completionCandidate.hashCode();
    h$ *= 1000003;
    h$ ^= matchLevel.hashCode();
    return h$;
  }

}
