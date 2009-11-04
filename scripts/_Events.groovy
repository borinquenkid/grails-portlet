import grails.util.GrailsUtil
import groovy.xml.StreamingMarkupBuilder
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

def portletVersion = '1.0'
def basedir = System.getProperty("base.dir")
def portletXml = new File("${basedir}/web-app/WEB-INF/portlet.xml")
def pluginLibDir = "${portletsPluginDir}/lib"
if (portletsPluginDir.exists()) {
    //we must not be installed...
    pluginDir = basedir
    pluginLibDir = "${basedir}/lib"
    if (!new File(pluginLibDir).exists()) {
        throw new RuntimeException('Unable to find Portlets plugin lib folder')
    }
}

eventPackagingEnd = {
    try {
        def xmlWriter = new StreamingMarkupBuilder();
        def searchPath = "file:${basedir}/grails-app/portlets/**/*Portlet.groovy"
        def customModes = [:]
        def userAttributes = [:]
        event("StatusUpdate", ["Searching for portlets: ${searchPath}"])
        portletFiles = resolveResources(searchPath).toList()
        if (portletFiles.size() > 0) {
            event("StatusUpdate", ["Generating portlet.xml - ${portletFiles.size()} portlets found"])

            if (portletXml.exists()) portletXml.delete()
            def underscoredVersion = portletVersion.replaceAll("\\.", "_")
            def xml = xmlWriter.bind {
                'portlet-app'(version: portletVersion,
                        xmlns: "http://java.sun.com/xml/ns/portlet/portlet-app_${underscoredVersion}.xsd",
                        'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                        'xsi:schemaLocation': "http://java.sun.com/xml/ns/portlet/portlet-app_${underscoredVersion}.xsd") {
                    mkp.comment 'GENERATED BY GRAILS PORTLETS PLUGIN - DO NOT EDIT'
                    portletFiles.each {portletClassFile ->
                        def className = portletClassFile.filename - '.groovy'
                        Class portletClass = classLoader.loadClass(className)
                        def portletName = className - 'Portlet'
                        def instance = portletClass.newInstance()
                        checkRequiredProperties(['supports', 'title', 'displayName'], instance)
                        //TODO security constraints
                        portlet {
                            'portlet-name'(portletName)
                            'display-name'(instance.displayName)
                            if (hasProperty('description', instance))
                                'description'(instance.description)
                            'portlet-class'('org.codehaus.grails.portlets.GrailsDispatcherPortlet')
                            'init-param'
                            {
                                'name'('contextClass')
                                'value'('org.codehaus.grails.portlets.GrailsPortletApplicationContext')
                            }
                            'init-param'
                            {
                                'name'('grailsPortletClass')
                                'value'(className)
                            }
                            'init-param'
                            {
                                'name'('contextConfigLocation')
                                'value'('/WEB-INF/portlet-context.groovy')
                            }
                            instance.supports.each {mime, types ->
                                'supports'
                                {
                                    'mime-type'(mime)
                                    types.each {mode ->
                                        'portlet-mode'(mode)
                                    }
                                }
                            }
                            if (hasProperty('customModes', instance) && instance.customModes instanceof Map) {
                                customModes += instance.supportsCustom
                            }
                            'portlet-info'
                            {
                                //TODO support 1l8n via properties files to supply these
                                'title'(instance.title)
                                if (hasProperty('shortTitle', instance)) 'short-title'(instance.shortTitle)
                                if (hasProperty('keywords', instance)) 'keywords'(instance.keywords)
                            }
                            if (hasProperty('roleRefs', instance) && instance.roleRefs instanceof List) {
                                instance.roleRefs.each {roleName ->
                                    'security-role-ref'
                                    {
                                        'role-name'(roleName)
                                    }
                                }
                            }
                            if (hasProperty('userAttributes', instance) && instance.userAttributes instanceof List) {
                                userAttributes += instance.userAttributes

                            }
                            if (hasProperty('supportedPreferences', instance) && instance.supportedPreferences instanceof Map) {
                                'portlet-preferences'
                                {
                                    instance.supportedPreferences.each {prefName, prefValue ->
                                        'preference'
                                        {
                                            'name'(prefName)
                                            if (prefValue instanceof List) {
                                                prefValue.each {multiValue ->
                                                    'value'(multiValue)
                                                }
                                            } else {
                                                'value'(prefValue)
                                            }
                                            /* TODO
                                            if (preference.readOnly) {
                                                'read-only'('true')
                                            }*/
                                        }
                                    }
                                }
                            }
                        }
                    }
                    userAttributes.each {userAttribute ->
                        'user-attribute'
                        {
                            'name'(userAttribute)
                        }
                    }
                    customModes.each {mode, description ->
                        'custom-portlet-mode'
                        {
                            'description'(description)
                            'name'(mode)
                        }
                    }

                }
            }
            portletXml.write(xml.toString())
        }
        def spring_conf = "${portletsPluginDir}/src/templates/scripts/portlet-context.groovy"
        if(new File(spring_conf).exists()) {
            ant.copy(file:spring_conf,
            todir:"${basedir}/web-app/WEB-INF")
        }
    } catch (Exception e) {
        event("StatusError", ["Unable to generate portlet.xml: " + e.message])
        exit(1)
    }

}

// Those jars are loaded at parents classloaders, 
// and causes silent portlet deployment failure if included.
eventCreateWarStart = { warName, stagingDir ->
    ant.delete {
      fileset(dir:"${stagingDir}/WEB-INF/lib") {
       include(name: "servlet-api*.jar")
       include(name: "portlet-api*.jar")
       include(name: "jcl-over-slf4j*.jar")
      }
   }
}

def hasProperty(propertyName, instance) {
    try {
        def value = instance."${propertyName}"
        return true;
    } catch (MissingPropertyException mpe) {
        return false;
    }
}

def checkRequiredProperties(propertyNames, instance) {
    propertyNames.each {
        if (!hasProperty(it, instance)) {
            throw new MissingPropertyException("${instance.class.name} does not have the required properties ${propertyNames}",
                    instance.class)
        }
    }
}

def resolveResources(String pattern) {
    def resolver = new PathMatchingResourcePatternResolver()
    return resolver.getResources(pattern)
}



