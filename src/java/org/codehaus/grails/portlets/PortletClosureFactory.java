package org.codehaus.grails.portlets;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import javax.portlet.PortletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PortletClosureFactory {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	public Closure getActionClosure(PortletRequest request,
			GroovyObject portlet, String actionParameter) {
		System.out.println("REAL CALL");
		return getPortletClosure(request, portlet, actionParameter, "action");
	}

	public Closure getRenderClosure(PortletRequest request,
			GroovyObject portlet, String actionParameter) {
		return getPortletClosure(request, portlet, actionParameter, "render");
	}

	private Closure getPortletClosure(PortletRequest request,
			GroovyObject portlet, String actionParameter, String closurePrefix) {
		Closure portletClosure = null;
		if (actionParameter != null) {
			try {
				portletClosure = (Closure) portlet.getProperty(actionParameter);
			} catch (Exception e) {
				log.warn("Unable to find Closure property " + actionParameter
						+ " from action request parameter");
			}
		}
		if (portletClosure == null) {
			String portletMode = request.getPortletMode().toString()
					.toLowerCase();
			String modeActionName = closurePrefix
					+ StringUtils.capitalize(portletMode);
			try {
				portletClosure = (Closure) portlet.getProperty(modeActionName);
			} catch (Exception e) {
				log.trace("Didn't find portlet mode " + closurePrefix
						+ " closure: " + modeActionName);
			}
		}
		if (portletClosure == null) {
			String defaultParam = "do" + StringUtils.capitalize(closurePrefix);
			log.debug("Falling back to " + defaultParam + " closure");
			portletClosure = (Closure) portlet.getProperty(defaultParam);
		}
		return portletClosure;
	}

}
