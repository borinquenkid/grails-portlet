package org.codehaus.grails.portlets;

import grails.util.GrailsUtil;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.portlet.handler.HandlerInterceptorAdapter;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.portlet.*;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This class is needed to modify the request/set context holders
 * after DispatcherPortlet has done it's thing.
 * Otherwise this code could go in the GrailsDispatcherServlet
 *
 * @author Lee Butts
 */
public class GrailsPortletHandlerInterceptor extends HandlerInterceptorAdapter implements
        ServletContextAware {
    private ServletContext servletContext;
    private PortletReloadFilter portletReloadFilter = new PortletReloadFilter();

    public boolean preHandleAction(ActionRequest actionRequest, ActionResponse actionResponse, Object o)
            throws Exception {
        beforeHandle(actionRequest, actionResponse);
        return true;
    }

    private void beforeHandle(PortletRequest portletRequest, PortletResponse portletResponse) {
        LocaleContextHolder.setLocale(portletRequest.getLocale());
        convertRequestToGrailsWebRequest(portletRequest, portletResponse);
        Log log = LogFactory.getLog(this.getClass());
        PortletSession session = portletRequest.getPortletSession();
        log.info("PSession = " + session.getId());
        log.info("HSession = " + ((GrailsWebRequest) RequestContextHolder.currentRequestAttributes()).getCurrentRequest().getSession().getId());
        //TODO I think this could move to GrailsDispatcherServlet
        if (GrailsUtil.isDevelopmentEnv()) {
            runReloadFilter(portletRequest, portletResponse);
        }
    }

    private void convertRequestToGrailsWebRequest(PortletRequest portletRequest, PortletResponse portletResponse) {
        //TODO this may break on PortletContainers other than Pluto...
        HttpServletRequest request = (HttpServletRequest) portletRequest;
        //Make sure we have an underlying http session and that it won't time out - PLUTO BUG
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(0);
        GrailsWebRequest webRequest = new GrailsWebRequest(request,
                (HttpServletResponse) portletResponse, servletContext);
        RequestContextHolder.setRequestAttributes(webRequest);
        portletRequest.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest);
    }

    private void runReloadFilter(PortletRequest actionRequest, PortletResponse actionResponse) {
        portletReloadFilter.setServletContext(servletContext);
        portletReloadFilter.doFilterInternal(actionRequest, actionResponse);
    }

    private void afterHandle(PortletRequest portletRequest) {
        GrailsWebRequest webRequest = (GrailsWebRequest) portletRequest.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);
        webRequest.requestCompleted();
        portletRequest.removeAttribute(GrailsApplicationAttributes.WEB_REQUEST);
        RequestContextHolder.setRequestAttributes(null);
        LocaleContextHolder.setLocale(null);
    }

    public void afterActionCompletion(ActionRequest actionRequest, ActionResponse actionResponse, Object o, Exception e) throws Exception {
        afterHandle(actionRequest);
    }

    public boolean preHandleRender(RenderRequest renderRequest, RenderResponse renderResponse, Object o) throws Exception {
        beforeHandle(renderRequest, renderResponse);
        return true;
    }

    public void afterRenderCompletion(RenderRequest renderRequest, RenderResponse renderResponse, Object o, Exception e) throws Exception {
        afterHandle(renderRequest);
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
