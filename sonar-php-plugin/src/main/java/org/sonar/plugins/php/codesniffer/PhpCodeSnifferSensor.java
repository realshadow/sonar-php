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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.php.api.PhpConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_ANALYZE_ONLY_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_ARGUMENT_LINE_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_CUSTOM_RULES_PROP_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_REPORT_FILE_NAME_DEFVALUE;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_REPORT_FILE_NAME_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_REPORT_FILE_RELATIVE_PATH_DEFVALUE;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_REPORT_FILE_RELATIVE_PATH_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_REPORT_PATH_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_SEVERITY_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_SEVERITY_OR_LEVEL_MODIFIER_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_SKIP_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferConfiguration.PHPCS_TIMEOUT_KEY;
import static org.sonar.plugins.php.codesniffer.PhpCodeSnifferRuleRepository.PHPCS_REPOSITORY_KEY;
import static org.sonar.plugins.php.core.AbstractPhpConfiguration.DEFAULT_TIMEOUT;

/**
 * The Class PhpCodesnifferPluginSensor.
 */
@Properties({
  @Property(key = PHPCS_SKIP_KEY, defaultValue = "false", name = "Disable PHP CodeSniffer", project = true, global = true,
    description = "If set to true, PHP CodeSniffer will not run.",
    category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER,
    type = PropertyType.BOOLEAN),
  @Property(key = PHPCS_ANALYZE_ONLY_KEY, defaultValue = "false", name = "Only analyze existing PHP CodeSniffer report files",
    project = true, global = true,
    description = "If set to false, PHP CodeSniffer will be executed. If set to true, PHP CodeSniffer will not be executed and the report provided through the \""
      + PHPCS_REPORT_PATH_KEY + "\" property will be used.",
    category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER,
    type = PropertyType.BOOLEAN),
  @Property(key = PHPCS_REPORT_PATH_KEY,
    name = "Report file path", project = true, global = true, description = "Relative path to the report to analyze. Example: path/to/phpcs.xml.",
    category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER),
  @Property(key = PHPCS_REPORT_FILE_RELATIVE_PATH_KEY, defaultValue = PHPCS_REPORT_FILE_RELATIVE_PATH_DEFVALUE,
    name = "Report file path (Deprecated)", project = true, global = true, description = "Replaced by the \"" + PHPCS_REPORT_PATH_KEY + "\" property.",
    category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER),
  @Property(key = PHPCS_REPORT_FILE_NAME_KEY, defaultValue = PHPCS_REPORT_FILE_NAME_DEFVALUE, name = "Report file name (Deprecated)", project = true,
    global = true, description = "Replaced by the \"" + PHPCS_REPORT_PATH_KEY + "\" property.",
    category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER),
  @Property(key = PhpCodeSnifferConfiguration.PHPCS_STANDARD_ARGUMENT_KEY, defaultValue = "",
    name = "Ruleset (or standard) to run PHP CodeSniffer with", project = true, global = true,
    description = "The ruleset file (or the standard name) used to run PHP CodeSniffer against. "
      + "If none is specified all standards will be launched", category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER),
  @Property(key = PHPCS_SEVERITY_OR_LEVEL_MODIFIER_KEY, defaultValue = "", name = "Severity modifier", project = true, global = true,
    description = "Allows to specify a severity modifier, like \"--error-severity=\" or \"--warning-severity=\", "
      + "used in conjunction with property \"" + PHPCS_SEVERITY_KEY + "\".", category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER),
  @Property(key = PHPCS_SEVERITY_KEY, defaultValue = "", name = "Severity level value", project = true, global = true,
    description = "Only issues whose severity is greater or equal to this property will be logged.",
    category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER),
  @Property(key = PHPCS_ARGUMENT_LINE_KEY, defaultValue = "", name = "Additional arguments", project = true, global = true,
    description = "Additionnal parameters that can be passed to PHP CodeSniffer.", category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER),
  @Property(key = PHPCS_TIMEOUT_KEY, defaultValue = "" + DEFAULT_TIMEOUT, name = "Timeout", project = true, global = true,
    description = "Execution of PHP CodeSniffer will be stopped after this amount of time (in minutes).", category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER),
  @Property(key = PHPCS_CUSTOM_RULES_PROP_KEY,
    defaultValue = "", name = "PHP CodeSniffer custom rules",
    description = "XML description of PHP CodeSniffer custom rules", type = PropertyType.TEXT,
    global = true, project = false, category = PhpCodeSnifferSensor.CATEGORY_PHP_CODE_SNIFFER)
})
public class PhpCodeSnifferSensor implements Sensor {

  protected static final String CATEGORY_PHP_CODE_SNIFFER = "PHP CodeSniffer";

  private static final Logger LOG = LoggerFactory.getLogger(PhpCodeSnifferSensor.class);

  private PhpCodeSnifferConfiguration configuration;
  private PhpCodeSnifferExecutor executor;
  private PhpCodeSnifferViolationsXmlParser parser;
  private RulesProfile profile;
  private RuleFinder ruleFinder;
  private final ProjectFileSystem filesystem;

  /**
   * Instantiates a new php codesniffer sensor.
   */
  public PhpCodeSnifferSensor(PhpCodeSnifferConfiguration conf, PhpCodeSnifferExecutor executor, RulesProfile profile,
      PhpCodeSnifferViolationsXmlParser parser, RuleFinder ruleFinder, ProjectFileSystem filesystem) {
    super();
    this.configuration = conf;
    this.executor = executor;
    this.parser = parser;
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.filesystem = filesystem;
  }

  /**
   * {@inheritDoc}
   */
  public void analyse(Project project, SensorContext context) {
    configuration.createWorkingDirectory();

    if (!configuration.isAnalyseOnly()) {
      executor.execute();
    }
    File report = configuration.getReportFile();
    List<PhpCodeSnifferViolation> violations = parser.getViolations(report);

    List<Violation> contextViolations = new ArrayList<Violation>();
    Set<String> unfoundViolations = new HashSet<String>();
    for (PhpCodeSnifferViolation violation : violations) {
      String ruleKey = violation.getRuleKey();
      Rule rule = ruleFinder.findByKey(PHPCS_REPOSITORY_KEY, ruleKey);
      if (rule != null) {
        org.sonar.api.resources.File resource = org.sonar.api.resources.File.fromIOFile(new File(violation.getFileName()), project);
        if (context.getResource(resource) != null) {
          Violation v = Violation.create(rule, resource).setLineId(violation.getLine()).setMessage(violation.getLongMessage());
          contextViolations.add(v);
          LOG.debug("Violation found: " + v);
        }
      } else {
        unfoundViolations.add(ruleKey);
      }
    }
    for (String key : unfoundViolations) {
      LOG.info("No rule found in repository " + PHPCS_REPOSITORY_KEY + " for key " + key);
    }
    context.saveViolations(contextViolations);
  }

  /**
   * {@inheritDoc}
   */
  public boolean shouldExecuteOnProject(Project project) {
    return !configuration.isSkip()
      && !filesystem.mainFiles(PhpConstants.LANGUAGE_KEY).isEmpty()
      && !profile.getActiveRulesByRepository(PHPCS_REPOSITORY_KEY).isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "PHP_CodeSniffer Sensor";
  }
}
