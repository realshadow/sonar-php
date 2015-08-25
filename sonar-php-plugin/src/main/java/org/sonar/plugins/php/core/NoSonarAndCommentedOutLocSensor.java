/*
 * Sonar PHP Plugin
 * Copyright (C) 2010 Codehaus Sonar Plugins
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.php.core;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.plugins.php.api.PhpConstants;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.recognizer.CamelCaseDetector;
import org.sonar.squid.recognizer.CodeRecognizer;
import org.sonar.squid.recognizer.ContainsDetector;
import org.sonar.squid.recognizer.Detector;
import org.sonar.squid.recognizer.EndWithDetector;
import org.sonar.squid.recognizer.KeywordsDetector;
import org.sonar.squid.recognizer.LanguageFootprint;
import org.sonar.squid.text.Source;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Phase(name = Phase.Name.PRE)
// The NoSonarFilter must be fed before launching the violation engines
public class NoSonarAndCommentedOutLocSensor implements Sensor {

  private static final double CODE_RECOGNIZER_SENSITIVITY = 0.9;

  private static final Logger LOG = LoggerFactory.getLogger(NoSonarAndCommentedOutLocSensor.class);

  private final NoSonarFilter filter;
  private final ModuleFileSystem filesystem;

  public NoSonarAndCommentedOutLocSensor(ModuleFileSystem filesystem, NoSonarFilter noSonarFilter) {
    this.filter = noSonarFilter;
    this.filesystem = filesystem;
  }

  /**
   * @see org.sonar.api.batch.Sensor#analyse(org.sonar.api.resources.Project, org.sonar.api.batch.SensorContext)
   */
  public void analyse(Project project, SensorContext context) {
    List<File> sourceFiles = filesystem.files(FileQuery.onSource().onLanguage(PhpConstants.LANGUAGE_KEY));
    for (File file : sourceFiles) {
      org.sonar.api.resources.File phpFile = getSonarResource(project, file);
      if (phpFile != null) {
        Source source = analyseSourceCode(file);
        if (source != null) {
          filter.addResource(phpFile, source.getNoSonarTagLines());
          double measure = source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES);
          context.saveMeasure(phpFile, CoreMetrics.COMMENTED_OUT_CODE_LINES, measure);
        }
      }
    }
  }

  @VisibleForTesting
  org.sonar.api.resources.File getSonarResource(Project project, File file) {
    return org.sonar.api.resources.File.fromIOFile(file, project);
  }

  protected static Source analyseSourceCode(File file) {
    Source result = null;
    FileReader reader = null;
    try {
      reader = new FileReader(file);
      result = new Source(reader, new CodeRecognizer(CODE_RECOGNIZER_SENSITIVITY, new PhpLanguageFootprint()));
    } catch (FileNotFoundException e) {
      throw new SonarException("Unable to open file '" + file.getAbsolutePath() + "'", e);
    } catch (RuntimeException rEx) {
      LOG.error("Error while parsing file '" + file.getAbsolutePath() + "'", rEx);
    } finally {
      IOUtils.closeQuietly(reader);
    }

    return result;
  }

  /**
   * @see org.sonar.api.batch.CheckProject#shouldExecuteOnProject(org.sonar.api.resources.Project)
   */
  public boolean shouldExecuteOnProject(Project project) {
    return !filesystem.files(FileQuery.onSource().onLanguage(PhpConstants.LANGUAGE_KEY)).isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "NoSonar and Commented out LOC Sensor";
  }

  private static class PhpLanguageFootprint implements LanguageFootprint {

    private static final double CAMEL_CASE_PROBABILITY = 0.5;
    private static final double CONDITIONAL_PROBABILITY = 0.95;
    private static final double PHP_KEYWORDS_PROBABILITY = 0.3;
    private static final double BOOLEAN_OPERATOR_PROBABILITY = 0.7;
    private static final double END_WITH_DETECTOR_PROBABILITY = 0.95;
    private final Set<Detector> detectors = new HashSet<Detector>();

    public PhpLanguageFootprint() {
      detectors.add(new EndWithDetector(END_WITH_DETECTOR_PROBABILITY, '}', ';', '{'));
      detectors.add(new KeywordsDetector(BOOLEAN_OPERATOR_PROBABILITY, "||", "&&"));
      detectors.add(new KeywordsDetector(PHP_KEYWORDS_PROBABILITY, PhpConstants.PHP_KEYWORDS_ARRAY));
      detectors.add(new ContainsDetector(CONDITIONAL_PROBABILITY, "++", "for(", "if(", "while(", "catch(", "switch(", "try{", "else{"));
      detectors.add(new CamelCaseDetector(CAMEL_CASE_PROBABILITY));
    }

    /**
     * @see org.sonar.squid.recognizer.LanguageFootprint#getDetectors()
     */
    public Set<Detector> getDetectors() {
      return detectors;
    }
  }
}
