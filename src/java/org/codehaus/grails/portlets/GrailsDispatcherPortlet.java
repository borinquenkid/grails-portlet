package org.codehaus.grails.portlets;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.web.portlet.DispatcherPortlet;

/**
 * @author Lee Butts
 */
public class GrailsDispatcherPortlet extends DispatcherPortlet {
    public static final String PORTLET_NAME = "grails.portlet.name";
    public static final String PORTLET_CONFIG = "grails.portlet.config";

    private GrailsApplication application;

    protected void doActionService(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
        addPortletInfoToRequest(actionRequest);
        super.doActionService(actionRequest, actionResponse);
    }

    protected void doRenderService(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {
        addPortletInfoToRequest(renderRequest);
        super.doRenderService(renderRequest, renderResponse);
    }

    private void addPortletInfoToRequest(PortletRequest portletRequest) {
        addPortletNameToRequest(portletRequest);
        addPortletConfigToRequest(portletRequest);
    }

    private void addPortletConfigToRequest(PortletRequest portletRequest) {
        portletRequest.setAttribute(PORTLET_CONFIG, getPortletConfig());
    }

    private void addPortletNameToRequest(PortletRequest request) {
        String portletName = getPortletConfig().getPortletName();
        request.setAttribute(PORTLET_NAME, portletName);
    }
}
