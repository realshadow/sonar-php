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

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.*;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.plugins.php.api.PhpConstants;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.text.Source;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NoSonarAndCommentedOutLocSensorTest {

  /*
  @Test
  public void testAnalyse() {
    NoSonarFilter noSonarFilter = new NoSonarFilter();
    ModuleFileSystem filesystem = mock(ModuleFileSystem.class);

    NoSonarAndCommentedOutLocSensor sensor = new NoSonarAndCommentedOutLocSensor(filesystem, noSonarFilter);
    SensorContext context = mock(SensorContext.class);
    Project project = getMockProject();
    sensor.analyse(project, context);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    // Mail.php contains 9 commented oud code lines.
    verify(context).saveMeasure(new org.sonar.api.resources.File("Mail.php"), CoreMetrics.COMMENTED_OUT_CODE_LINES, 9d);
  }
  */

  /*
  @Test
  public void testShouldNotRunOnJavaProject() {
    NoSonarFilter noSonarFilter = new NoSonarFilter();
    ModuleFileSystem filesystem = mock(ModuleFileSystem.class);

    NoSonarAndCommentedOutLocSensor sensor = new NoSonarAndCommentedOutLocSensor(filesystem, noSonarFilter);
    SensorContext context = mock(SensorContext.class);
    Project project = getMockProject();
    when(project.getLanguageKey()).thenReturn("java");
    sensor.analyse(project, context);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }
  */

  @Test
  public void testAnalyseEmptySourceFiles() {
    NoSonarFilter noSonarFilter = new NoSonarFilter();
    ModuleFileSystem filesystem = mock(ModuleFileSystem.class);

    NoSonarAndCommentedOutLocSensor sensor = new NoSonarAndCommentedOutLocSensor(filesystem, noSonarFilter);
    SensorContext context = mock(SensorContext.class);
    Project project = getMockProject();

    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(fs);
    File fakeFile = new File("fake.php");
    when(fs.getSourceDirs()).thenReturn(Arrays.asList(fakeFile.getParentFile()));
    when(fs.mainFiles(PhpConstants.LANGUAGE_KEY)).thenReturn(InputFileUtils.create(fakeFile.getParentFile(), Arrays.asList(fakeFile, new File("fake"))));

    sensor.analyse(project, context);
    verify(context, never()).saveMeasure(any(Resource.class), any(org.sonar.api.measures.Metric.class), any(Double.class));

  }

  @Test
  public void testAnalyseSourceCode() {
    File file = new File(this.getClass().getResource("/Mail.php").getPath());
    Source source = NoSonarAndCommentedOutLocSensor.analyseSourceCode(file);
    assertEquals(1, source.getNoSonarTagLines().size());
    assertEquals(17, (int) source.getNoSonarTagLines().iterator().next());

    assertEquals(9, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  @Test
  public void testAnalyseSourceCodeWithRegions() {
    File file = new File(this.getClass().getResource("/Math2.php").getPath());
    Source source = NoSonarAndCommentedOutLocSensor.analyseSourceCode(file);
    assertEquals(1, source.getNoSonarTagLines().size());
    assertEquals(126, (int) source.getNoSonarTagLines().iterator().next());

    assertEquals(3, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  @Test
  public void testAnalyseSourceCodeWithNoNoSonar() {
    File file = new File(this.getClass().getResource("/Math3.php").getPath());
    Source source = NoSonarAndCommentedOutLocSensor.analyseSourceCode(file);
    assertEquals(0, source.getNoSonarTagLines().size());
    assertEquals(5, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  // TEST for SONARPLUGINS-662
  @Test
  public void testAnalyseSourceCodeWithMultiLineString() {
    File file = new File(this.getClass().getResource("/Math4.php").getPath());
    Source source = NoSonarAndCommentedOutLocSensor.analyseSourceCode(file);
    assertEquals(1, source.getNoSonarTagLines().size());
    assertEquals(91, (int) source.getNoSonarTagLines().iterator().next());

    assertEquals(5, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  /**
   * @return a mock project used by all tests cases in this class.
   */
  private Project getMockProject() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(PhpConstants.LANGUAGE_KEY);

    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(fs);
    File f1 = TestUtils.getResource("/Mail.php");
    when(fs.getSourceDirs()).thenReturn(Arrays.asList(f1.getParentFile()));
    when(fs.mainFiles(PhpConstants.LANGUAGE_KEY)).thenReturn(InputFileUtils.create(f1.getParentFile(), Arrays.asList(f1, new File("fake"))));

    return project;
  }
}
