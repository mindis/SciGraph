/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.sdsc.scigraph.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import edu.sdsc.scigraph.owlapi.curies.CurieUtil;

public class HyperGeometricAnalyzer {

  private final GraphDatabaseService graphDb;
  private final CurieUtil curieUtil;

  @Inject
  public HyperGeometricAnalyzer(GraphDatabaseService graphDb, CurieUtil curieUtil) {
    this.graphDb = graphDb;
    this.curieUtil = curieUtil;
  }

  private double getCountFrom(Set<AnalyzerResult> set, Long id) throws Exception {
    for (AnalyzerResult el : set) {
      if (el.getNodeId() == id) {
        return el.getCount();
      }
    }
    throw new Exception("Coud not retrieve count for id " + id);
  }

  private Set<AnalyzerResult> getSampleSetNodes(List<String> sampleSet, String ontologyClass,
      String path) {
    Set<AnalyzerResult> sampleSetNodes = new HashSet<AnalyzerResult>();
    String query =
        "match (n:" + ontologyClass + ")-[rel:" + path
            + "]->(t) where HAS (n.label) and n.label in " + sampleSet + " return t, count(*)";
    Result result = graphDb.execute(query);
    while (result.hasNext()) {
      Map<String, Object> map = result.next();
      sampleSetNodes.add(new AnalyzerResult(((Node) map.get("t")).getId(), (Long) map
          .get("count(*)")));
    }
    return sampleSetNodes;
  }

  private Set<AnalyzerResult> getCompleteSetNodes(String ontologyClass, String path) {
    String query = "match (n:" + ontologyClass + ")-[rel:" + path + "]->(t) return t, count(*)";
    Result result2 = graphDb.execute(query);
    Set<AnalyzerResult> allSubjects = new HashSet<AnalyzerResult>();
    while (result2.hasNext()) {
      Map<String, Object> map = result2.next();
      allSubjects
          .add(new AnalyzerResult(((Node) map.get("t")).getId(), (Long) map.get("count(*)")));
    }
    return allSubjects;
  }

  private int getTotalCount(String ontologyClass) {
    String query = "match (n:" + ontologyClass + ") return count(*)";
    Result result3 = graphDb.execute(query);
    int totalCount = 0;
    while (result3.hasNext()) {
      Map<String, Object> map = result3.next();
      totalCount = ((Long) map.get("count(*)")).intValue();
    }
    return totalCount;
  }

  public List<AnalyzerResult> analyze(List<String> sampleSet, String ontologyClass, String path) {
    ArrayList<AnalyzerResult> pValues = new ArrayList<AnalyzerResult>();
    try (Transaction tx = graphDb.beginTx()) {
      Set<AnalyzerResult> sampleSetNodes = getSampleSetNodes(sampleSet, ontologyClass, path);

      Set<AnalyzerResult> completeSetNodes = getCompleteSetNodes(ontologyClass, path);

      int totalCount = getTotalCount(ontologyClass);

      // apply the HyperGeometricDistribution for each topping
      for (AnalyzerResult n : sampleSetNodes) {
        HypergeometricDistribution hypergeometricDistribution =
            new HypergeometricDistribution(totalCount, (int) getCountFrom(completeSetNodes,
                n.getNodeId()), sampleSet.size());
        double p = hypergeometricDistribution.upperCumulativeProbability((int) n.getCount());
        pValues.add(new AnalyzerResult(n.getNodeId(), p));
      }

      // sort by p-value
      Collections.sort(pValues, new AnalyzerResultComparator());

      tx.success();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return pValues;
  }

}
