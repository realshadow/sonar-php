/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 EchoSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.php.pmd.xml;

import java.util.ArrayList;
import java.util.List;

public final class PmdRule implements Comparable<String> {

  private String ref;

  private String priority;

  private String name;

  private String message;

  private List<PmdProperty> properties = new ArrayList<PmdProperty>();

  private String clazz;// NOSONAR unused private field

  public PmdRule(String ref) {
    this(ref, null);
  }

  public PmdRule(String ref, String priority) {
    this.ref = ref;
    this.priority = priority;
  }

  public String getRef() {
    return ref;
  }

  public void setProperties(List<PmdProperty> properties) {
    this.properties = properties;
  }

  public List<PmdProperty> getProperties() {
    return properties;
  }

  public PmdProperty getProperty(String propertyName) {
    for (PmdProperty property : properties) {
      if (propertyName.equals(property.getName())) {
        return property;
      }
    }
    return null;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(String o) {
    return o.compareTo(this.ref);
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((priority == null) ? 0 : priority.hashCode());
    result = prime * result + ((properties == null) ? 0 : properties.hashCode());
    result = prime * result + ((ref == null) ? 0 : ref.hashCode());
    return result;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PmdRule other = (PmdRule) obj;
    return other.ref.equals(this.ref);
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public void addProperty(PmdProperty property) {
    if (properties == null) {
      properties = new ArrayList<PmdProperty>();
    }
    properties.add(property);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public String getClazz() {
    return clazz;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public void removeProperty(String propertyName) {
    PmdProperty prop = getProperty(propertyName);
    properties.remove(prop);
  }

  public void setClazz(String clazz) {
    this.clazz = clazz;
  }

  public String getName() {
    return name;
  }

  public boolean hasProperties() {
    return properties != null && !properties.isEmpty();
  }
}