package org.codehaus.grails.portlets

import javax.portlet.ActionRequest
import javax.portlet.ActionResponse

import org.gmock.GMockController

scenario "GrailsPortletHandler handleAction", {
	// scenario with a reason
	given "setup", {
		gmc = new GMockController();
		mockPortlet = gmc.mock(GString)
		mockActionRequest = gmc.mock(ActionRequest)
		mockActionResponse = gmc.mock(ActionResponse)
		mockPortletClosureFactory = gmc.mock(PortletClosureFactory)
		mockClosure = gmc.mock(Closure)
		testObject = new GrailsPortletHandlerAdapter()
		testObject.setPortletClosureFactory(mockPortletClosureFactory)
	}


	when "call method", {
		// what all operations has to be carried out in the scenario
		mockActionRequest.getParameter("action").returns("foo")
		mockPortletClosureFactory.getActionClosure(mockActionRequest,mockPortlet,"foo").returns(mockClosure)

	}


	then "closure should be called", { mockClosure.call() }
}
