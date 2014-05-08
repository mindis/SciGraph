package edu.sdsc.scigraph.opennlp;

import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import com.google.inject.AbstractModule;
import com.google.inject.throwingproviders.CheckedProvider;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

public class OpenNlpModule extends AbstractModule {

  @Override
  protected void configure() {
    install(ThrowingProviderBinder.forModule(this));
  }

  public static interface TokenizerProvider extends CheckedProvider<Tokenizer> {
    Tokenizer get() throws IOException;
  }

  @CheckedProvides(TokenizerProvider.class)
  Tokenizer getTokenizer() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/opennlp/en-token.bin")) {
      TokenizerModel model = new TokenizerModel(is);
      return new TokenizerME(model);
    }
  }

  public static interface SentenceDetectorProvider extends CheckedProvider<SentenceDetectorME> {
    SentenceDetectorME get() throws IOException;
  }

  @CheckedProvides(SentenceDetectorProvider.class)
  SentenceDetectorME getSentenceDetector() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/opennlp/en-sent.bin")) {
      SentenceModel model = new SentenceModel(is);
      return new SentenceDetectorME(model);
    }
  }

  public static interface PosTaggerProvider extends CheckedProvider<POSTaggerME> {
    POSTaggerME get() throws IOException;
  }

  @CheckedProvides(PosTaggerProvider.class)
  POSTaggerME getPosTagger() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/opennlp/en-pos-maxent.bin")) {
      POSModel model = new POSModel(is);
      return new POSTaggerME(model);
    }
  }

  public static interface ChunkerProvider extends CheckedProvider<ChunkerME> {
    ChunkerME get() throws IOException;
  }

  @CheckedProvides(ChunkerProvider.class)
  ChunkerME getChunker() throws IOException {
    try (InputStream is = getClass().getResourceAsStream("/opennlp/en-chunker.bin")) {
      ChunkerModel model = new ChunkerModel(is);
      return new ChunkerME(model);
    }
  }

}
