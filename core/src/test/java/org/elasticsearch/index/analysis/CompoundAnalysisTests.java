/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.lucene.all.AllEntries;
import org.elasticsearch.common.lucene.all.AllTokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.compound.DictionaryCompoundWordTokenFilterFactory;
import org.elasticsearch.index.analysis.filter1.MyFilterTokenFilterFactory;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.IndexSettingsModule;
import org.hamcrest.MatcherAssert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;

/**
 */
public class CompoundAnalysisTests extends ESTestCase {
    public void testDefaultsCompoundAnalysis() throws Exception {
        Settings settings = getJsonSettings();
        IndexSettings idxSettings = IndexSettingsModule.newIndexSettings("test", settings);
        AnalysisService analysisService = new AnalysisRegistry(null, new Environment(settings),
                Collections.emptyMap(),Collections.singletonMap("myfilter", MyFilterTokenFilterFactory::new),Collections.emptyMap(),Collections.emptyMap()).build(idxSettings);

        TokenFilterFactory filterFactory = analysisService.tokenFilter("dict_dec");
        MatcherAssert.assertThat(filterFactory, instanceOf(DictionaryCompoundWordTokenFilterFactory.class));
    }

    public void testDictionaryDecompounder() throws Exception {
        Settings[] settingsArr = new Settings[]{getJsonSettings(), getYamlSettings()};
        for (Settings settings : settingsArr) {
            List<String> terms = analyze(settings, "decompoundingAnalyzer", "donaudampfschiff spargelcremesuppe");
            MatcherAssert.assertThat(terms.size(), equalTo(8));
            MatcherAssert.assertThat(terms, hasItems("donau", "dampf", "schiff", "donaudampfschiff", "spargel", "creme", "suppe", "spargelcremesuppe"));
        }
    }

    private List<String> analyze(Settings settings, String analyzerName, String text) throws IOException {
        IndexSettings idxSettings = IndexSettingsModule.newIndexSettings("test", settings);
        AnalysisService analysisService = new AnalysisRegistry(null, new Environment(settings),
                Collections.emptyMap(), Collections.singletonMap("myfilter", MyFilterTokenFilterFactory::new),Collections.emptyMap(),Collections.emptyMap()).build(idxSettings);

        Analyzer analyzer = analysisService.analyzer(analyzerName).analyzer();

        AllEntries allEntries = new AllEntries();
        allEntries.addText("field1", text, 1.0f);

        TokenStream stream = AllTokenStream.allTokenStream("_all", text, 1.0f, analyzer);
        stream.reset();
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

        List<String> terms = new ArrayList<>();
        while (stream.incrementToken()) {
            String tokText = termAtt.toString();
            terms.add(tokText);
        }
        return terms;
    }

    private Settings getJsonSettings() throws IOException {
        String json = "/org/elasticsearch/index/analysis/test1.json";
        return Settings.builder()
                .loadFromStream(json, getClass().getResourceAsStream(json))
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString())
                .build();
    }

    private Settings getYamlSettings() throws IOException {
        String yaml = "/org/elasticsearch/index/analysis/test1.yml";
        return Settings.builder()
                .loadFromStream(yaml, getClass().getResourceAsStream(yaml))
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString())
                .build();
    }
}
