package org.codehaus.grails.portlets;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.util.ConfigObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageView;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.portlet.*;

import java.util.Map;

/**
 * @author Lee Butts
 */
public class GrailsPortletHandlerAdapter implements
		org.springframework.web.portlet.HandlerAdapter, ApplicationContextAware {
	private ApplicationContext applicationContext;
	private Log log = LogFactory.getLog(this.getClass());
	private PortletClosureFactory portletClosureFactory;

	public void setPortletClosureFactory(
			PortletClosureFactory portletClosureFactory) {
		this.portletClosureFactory = portletClosureFactory;
	}

	public boolean supports(Object o) {
		return o instanceof GroovyObject;
	}

	public void handleAction(ActionRequest actionRequest,
			ActionResponse actionResponse, Object o) throws Exception {
		GroovyObject portlet = (GroovyObject) o;
		String action = actionRequest.getParameter("action");
		Closure actionClosure = portletClosureFactory.getActionClosure(
				actionRequest, portlet, action);
		actionClosure.call();
	}

	public ModelAndView handleRender(RenderRequest renderRequest,
			RenderResponse renderResponse, Object o) throws Exception {
		if (getMinimisedConfig() != null
				&& renderRequest.getWindowState().equals(WindowState.MINIMIZED)) {
			log.debug("portlet.handleMinimised is set, rendering empty string");
			renderResponse.setContentType("text/html");
			renderResponse.getPortletOutputStream().write("".getBytes());
			return null;
		} else {
			GroovyObject portlet = (GroovyObject) o;
			String action = renderRequest.getParameter("action");
			Closure render = portletClosureFactory.getRenderClosure(
					renderRequest, portlet, action);
			Object returnValue = render.call();
			if (returnValue instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) returnValue;
				map.put("portletRequest", renderRequest);
				map.put("portletResponse", renderResponse);
				renderRequest.setAttribute(
						GrailsApplicationAttributes.CONTROLLER, portlet);
				String portletName = renderRequest.getAttribute(
						GrailsDispatcherPortlet.PORTLET_NAME).toString();
				String uncapitalizedPortletName = StringUtils
						.uncapitalize(portletName);
				String viewName = "/" + uncapitalizedPortletName + "/"
						+ renderRequest.getParameter("action") + ".gsp";
				if (tryResolveView(viewName)) {
					log.debug("Trying to render action view " + viewName
							+ ".gsp");
				} else {
					log.debug("Couldn't resolve action view " + viewName);
					viewName = "/"
							+ uncapitalizedPortletName
							+ "/"
							+ renderRequest.getPortletMode().toString()
									.toLowerCase();
					if (tryResolveView(viewName)) {
						log.debug("Trying to render mode view " + viewName
								+ ".gsp");
					} else {
						log.debug("Couldn't resolve mode view " + viewName);
						viewName = "/" + uncapitalizedPortletName + "/render";
						log.debug("Trying to render view " + viewName);
					}
					return new ModelAndView(viewName,
							(Map<String, ?>) returnValue);
				}
			}
			return null;
		}
	}

	private Object getMinimisedConfig() {
		try {
			// TODO allow overriding config setting per portlet
			ConfigObject configObject = (ConfigObject) ConfigurationHolder
					.getConfig().get("portlet");
			Object value = null;
			if (configObject != null) {
				value = configObject.get("handleMinimised");
			}
			if (value != null) {
				return value;
			} else {
				log.debug("portlet.handleMinimised not set, proceeding with normal render");
				return null;
			}
		} catch (ClassCastException e) {
			log.warn("Unable to determine portlet.handleMinimised setting");
			return null;
		}
	}

	public boolean tryResolveView(String viewName) {
		ViewResolver vr = (ViewResolver) applicationContext
				.getBean("jspViewResolver");
		try {
			View view = vr.resolveViewName(viewName, LocaleContextHolder
					.getLocaleContext().getLocale());
			return view instanceof GroovyPageView; // GrailsViewResolver will
													// return a GPV if it exists
													// otherwise it's a normal
													// JSP view (which may or
													// may not exist)
		} catch (Exception e) {
			return false;
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void handleEvent(EventRequest arg0, EventResponse arg1, Object arg2)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public ModelAndView handleResource(ResourceRequest arg0,
			ResourceResponse arg1, Object arg2) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
