/*******************************************************************************
 * Copyright (c) 2010 Oracle.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at 
 *     http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *     mkeith - Gemini JPA work 
 ******************************************************************************/
package org.eclipse.gemini.jpa.configadmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

import org.eclipse.gemini.jpa.GeminiPersistenceUnitProperties;
import org.eclipse.gemini.jpa.GeminiUtil;
import org.eclipse.gemini.jpa.PUnitInfo;

/**
 * Stores the configuration data passed to us by the config admin service. 
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PersistenceUnitConfiguration {
        
    String unitName;
    String bsn;
    Collection<String> classes;
    String excludeUnlistedClasses;
    Boolean refreshBundle;
    Map<String,Object> properties;
    
    // Internal state (not really part of the config info)
    String servicePid; // Generated by OSGi config admin service
    String descriptorName; // Name of private JPA descriptor we have generated
    String descriptor; // Private JPA descriptor we have generated

    /*=================*/
    /* Getters/setters */
    /*=================*/
    
    public String getUnitName() { return unitName; }
    protected void setUnitName(String unitName) { this.unitName = unitName; }

    public String getBsn() { return bsn; }
    protected void setBsn(String bsn) { this.bsn = bsn; }

    public Collection<String> getClasses() { return classes; }
    protected void setClasses(Collection<String> classes) { this.classes = classes; }

    public String getExcludeUnlistedClasses() { return excludeUnlistedClasses; }
    protected void setExcludeUnlistedClasses(String flag) { this.excludeUnlistedClasses = flag; }

    public boolean getRefreshBundle() { return (refreshBundle==null) ? false : refreshBundle; }
    protected void setRefreshBundle(boolean flag) { this.refreshBundle = flag; }

    public Map<String, Object> getProperties() { return properties; }
    protected void setProperties(Map<String, Object> props) { this.properties = props;}

    public String getServicePid() { return servicePid; }
    protected void setServicePid(String servicePid) { this.servicePid = servicePid; }

    public String getDescriptorName() { return descriptorName; }
    protected void setDescriptorName(String descName) { this.descriptorName = descName; }

    public String getDescriptor() { return descriptor; }
    protected void setDescriptor(String desc) { this.descriptor = desc; }

    /*==========================*/
    /* Constructors and methods */
    /*==========================*/

    /**
     * Main constructor
     * @param config Properties from Config Admin service
     */
    public PersistenceUnitConfiguration(Dictionary config) {

        unitName = (String)config.get(GeminiPersistenceUnitProperties.PUNIT_NAME);
        bsn = (String)config.get(GeminiPersistenceUnitProperties.PUNIT_BSN);
        servicePid = (String)config.get(Constants.SERVICE_PID);

        // Classes may be in a Collection<String>, or single comma-separated String of class names
        Object clsColl = config.get(GeminiPersistenceUnitProperties.PUNIT_CLASSES);
        if (clsColl != null) {
            if (clsColl instanceof Collection<?>) {
                classes = (Collection<String>)clsColl;
            } else if (clsColl instanceof String) {
                List list = Arrays.asList(((String)clsColl).split(","));
                classes = new ArrayList<String>();
                for (Object s : list) {
                    classes.add(((String)s).trim());
                }
            } else {
                GeminiUtil.warning("Configuration property " +
                               GeminiPersistenceUnitProperties.PUNIT_CLASSES,
                               " must be of type Collection<String> or a comma-separated String of class names");
            }
        }
        // Allow excludeUnlistedClasses to be of type String or Boolean
        Object exclude = config.get(GeminiPersistenceUnitProperties.PUNIT_EXCLUDE_UNLISTED_CLASSES);
        if (exclude != null) {
            if (exclude instanceof Boolean) { 
                excludeUnlistedClasses = exclude.toString();
            } else if (exclude instanceof String) {
                excludeUnlistedClasses = (String) exclude;
            } else {
                GeminiUtil.warning("Configuration property " +
                        GeminiPersistenceUnitProperties.PUNIT_EXCLUDE_UNLISTED_CLASSES,
                        " must be of type String or Boolean");
            }
        }
        // Allow refreshBundle to be of type String or Boolean
        Object refresh = config.get(GeminiPersistenceUnitProperties.PUNIT_REFRESH);
        if (refresh != null) {
            if (refresh instanceof Boolean) { 
                refreshBundle = (Boolean)refresh;
            } else if (refresh instanceof String) {
                refreshBundle = Boolean.parseBoolean((String)refresh);
            } else {
                GeminiUtil.warning("Configuration property " +
                        GeminiPersistenceUnitProperties.PUNIT_REFRESH,
                        " must be of type String or Boolean");
            }
        }

        // All of the rest of the configuration goes in the properties section
        properties = pUnitProperties(config);
    }

    /** 
     * Add any relevant information from this object to the pUnitInfo passed in
     */
    public void updatePUnitInfo(PUnitInfo pUnitInfo) {
        assert(getUnitName() == pUnitInfo.getUnitName());
        // Make a copy of the props and remove the ones we are explicitly handling
        Map<String,Object> props = new HashMap<String,Object>();
        props.putAll(this.getProperties());
        
        // Set any driver-specific props if they are present in the config 
        String driverClassName = (String) props.remove(GeminiUtil.JPA_JDBC_DRIVER_PROPERTY);
        if (driverClassName != null) 
            pUnitInfo.setDriverClassName(driverClassName);
        String driverUrl = (String) props.remove(GeminiUtil.JPA_JDBC_URL_PROPERTY);
        if (driverUrl != null) 
            pUnitInfo.setDriverUrl(driverUrl);
        String driverUser = (String) props.remove(GeminiUtil.JPA_JDBC_USER_PROPERTY);
        if (driverUser != null) 
            pUnitInfo.setDriverUser(driverUser);
        String driverPassword = (String) props.remove(GeminiUtil.JPA_JDBC_PASSWORD_PROPERTY);
        if (driverPassword != null) 
            pUnitInfo.setDriverPassword(driverPassword);
        String driverVersion = (String) props.remove(GeminiUtil.OSGI_JDBC_DRIVER_VERSION_PROPERTY);
        if (driverVersion != null) 
            pUnitInfo.setDriverVersion(driverVersion);

        // Put the remaining properties in if there are any left
        if (!getProperties().isEmpty()) {
            pUnitInfo.setConfigProperties(props);
        }
    }
    
    /**
     * Helper method to remove all of the reserved config properties and  
     * treat the rest as persistence descriptor <property> elements.
     */
    public Map<String,Object> pUnitProperties(Dictionary dict) {
    
        Map<String,Object> props = new HashMap<String,Object>();
        Enumeration keysEnum = dict.keys();
        // Dump in all of the config props
        while (keysEnum.hasMoreElements()) {
            String key = (String)keysEnum.nextElement();
            props.put(key, dict.get(key));
        }
        // Now remove all the ones we know are not actual persistence "<properties>"
        props.remove(GeminiPersistenceUnitProperties.PUNIT_NAME);
        props.remove(GeminiPersistenceUnitProperties.PUNIT_BSN);
        props.remove(GeminiPersistenceUnitProperties.PUNIT_CLASSES);
        props.remove(GeminiPersistenceUnitProperties.PUNIT_EXCLUDE_UNLISTED_CLASSES);
        props.remove(GeminiPersistenceUnitProperties.PUNIT_REFRESH);
        props.remove(GeminiUtil.JPA_PROVIDER_PROPERTY); // Don't expect this, but remove if present 
        props.remove(Constants.SERVICE_PID);
        props.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
        props.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        return props;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PUnitConfig[servicePid=").append(servicePid)
          .append(", unitName=" + unitName)
          .append((bsn!=null) ? ", bsn=" + bsn : "")
          .append((excludeUnlistedClasses!=null) ? ", excludeUnlistedClasses=" + excludeUnlistedClasses : "")
          .append((refreshBundle!=null) ? ", refresh=" + refreshBundle : "");
        if (classes != null) {
            sb.append(", classes={");
            for (String cls : classes)
                sb.append(" ").append(cls);
            sb.append(" }");
        }
        sb.append(", props=").append(properties)
          .append("]");
       return sb.toString();
    }
}