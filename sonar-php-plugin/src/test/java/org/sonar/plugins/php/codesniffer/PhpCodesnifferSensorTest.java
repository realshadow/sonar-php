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
package org.sonar.plugins.php.codesniffer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.plugins.php.MockUtils;
import org.sonar.plugins.php.api.PhpConstants;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferRuleRepository.PHPCS_REPOSITORY_KEY;

/**
 * The Class PhpCodesnifferSensorTest.
 */
public class PhpCodesnifferSensorTest {

  /*
  @Test
  public void shouldNotLaunchOnNonPhpProject() {
    Project project = mock(Project.class);
    when(project.getLanguage()).thenReturn(Java.INSTANCE);

    PhpCodeSnifferSensor sensor = createSensor(project, null, null, false);
    assertEquals(false, sensor.shouldExecuteOnProject(project));
  }
  */

  @Test
  public void shouldLaunch() {
    RulesProfile profile = createRulesProfile();
    Project project = MockUtils.createMockProject();
    PhpCodeSnifferExecutor executor = mock(PhpCodeSnifferExecutor.class);
    PhpCodeSnifferSensor sensor = createSensor(project, executor, profile, false);

    assertEquals(true, sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void shouldNotLaunchIfSkip() {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getActiveRulesByRepository(PHPCS_REPOSITORY_KEY)).thenReturn(new ArrayList<ActiveRule>());

    Project project = MockUtils.createMockProject();
    PhpCodeSnifferExecutor executor = mock(PhpCodeSnifferExecutor.class);
    PhpCodeSnifferSensor sensor = createSensor(project, executor, profile, true);

    assertEquals(false, sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void shouldNotLaunchIfNoActiveRule() {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getActiveRulesByRepository(PHPCS_REPOSITORY_KEY)).thenReturn(new ArrayList<ActiveRule>());

    Project project = MockUtils.createMockProject();
    PhpCodeSnifferExecutor executor = mock(PhpCodeSnifferExecutor.class);
    PhpCodeSnifferSensor sensor = createSensor(project, executor, profile, false);

    assertEquals(false, sensor.shouldExecuteOnProject(project));
  }

  protected PhpCodeSnifferSensor createSensor(Project project, PhpCodeSnifferExecutor executor, RulesProfile profile, boolean skip) {
    PhpCodeSnifferViolationsXmlParser parser = mock(PhpCodeSnifferViolationsXmlParser.class);
    PhpCodeSnifferConfiguration conf = mock(PhpCodeSnifferConfiguration.class);
    when(conf.isSkip()).thenReturn(skip);
    RuleFinder ruleFinder = mock(RuleFinder.class);
    ProjectFileSystem filesystem = mock(ProjectFileSystem.class);

    when(filesystem.mainFiles(PhpConstants.LANGUAGE_KEY)).thenReturn(ImmutableList.<InputFile>of(mock(InputFile.class)));

    return new PhpCodeSnifferSensor(conf, executor, profile, parser, ruleFinder, filesystem);
  }

  protected RulesProfile createRulesProfile() {
    RulesProfile profile = mock(RulesProfile.class);
    ActiveRule rule = mock(ActiveRule.class);
    when(profile.getActiveRulesByRepository(PHPCS_REPOSITORY_KEY)).thenReturn(Lists.newArrayList(rule));
    return profile;
  }

}
