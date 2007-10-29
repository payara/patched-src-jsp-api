

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * glassfish/bootstrap/legal/CDDLv1.0.txt or
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Portions Copyright Apache Software Foundation.
 */


package org.apache.catalina.core;

import java.io.IOException;

// START SJSAS 6324911
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedInputStream;
import javax.servlet.ServletOutputStream;
// END SJSAS 6324911

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// START GlassFish Issue 1057
import javax.servlet.http.HttpSession;
// END GlassFish Issue 1057
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
// START GlassFish Issue 1057
import org.apache.catalina.Session;
// END GlassFish Issue 1057
// START SJSAS 6374691
import org.apache.catalina.Valve;
// END SJSAS 6374691
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.ResponseUtil;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;
import com.sun.org.apache.commons.logging.Log;
import com.sun.org.apache.commons.logging.LogFactory;

/**
 * Valve that implements the default basic behavior for the
 * <code>StandardHost</code> container implementation.
 * <p>
 * <b>USAGE CONSTRAINT</b>:  This implementation is likely to be useful only
 * when processing HTTP requests.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.20 $ $Date: 2007/04/10 17:09:56 $
 */

final class StandardHostValve
    extends ValveBase {


    private static Log log = LogFactory.getLog(StandardHostValve.class);

    private static final ClassLoader standardHostValveClassLoader =
        StandardHostValve.class.getClassLoader();


    // ----------------------------------------------------- Instance Variables


    /**
     * The descriptive information related to this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardHostValve/1.0";

    /**
     * The string manager for this package.
     */
    private static final StringManager sm =
        StringManager.getManager(Constants.Package);


    // START SJSAS 6374691
    private Valve errorReportValve;
    // END SJSAS 6374691


    // ------------------------------------------------------------- Properties


    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {

        return (info);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Select the appropriate child Context to process this request,
     * based on the specified request URI.  If no matching Context can
     * be found, return an appropriate HTTP error.
     *
     * @param request Request to be processed
     * @param response Response to be produced
     * @param valveContext Valve context used to forward to the next Valve
     *
     * @exception IOException if an input/output error occurred
     * @exception ServletException if a servlet error occurred
     */
     /** IASRI 4665318
     public void invoke(Request request, Response response,
                        ValveContext valveContext)
         throws IOException, ServletException {
     */
     // START OF IASRI 4665318
     public int invoke(Request request, Response response)
         throws IOException, ServletException {
     // END OF IASRI 4665318

        // Select the Context to be used for this Request
        Context context = request.getContext();
        if (context == null) {
            /* S1AS 4878272
            ((HttpServletResponse) response.getResponse()).sendError
                (HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                 sm.getString("standardHost.noContext"));
            */
            // BEGIN S1AS 4878272
            ((HttpServletResponse) response.getResponse()).sendError
                (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setDetailMessage(sm.getString("standardHost.noContext"));
            // END S1AS 4878272
            // START OF IASRI 4665318
            // return;     // NOTE - Not much else we can do generically
            return END_PIPELINE;
            // END OF IASRI 4665318
        }

        // Bind the context CL to the current thread
        if( context.getLoader() != null ) {
            // Not started - it should check for availability first
            // This should eventually move to Engine, it's generic.
            Thread.currentThread().setContextClassLoader
                    (context.getLoader().getClassLoader());
        }
                 
        // START GlassFish Issue 1057
        // Update the session last access time for our session (if any)
        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        hreq.getSession(false);
        // END GlassFish Issue 1057

        // Ask this Context to process this request
        // START OF IASRI 4665318
        context.getPipeline().invoke(request, response);
        return END_PIPELINE;
    }


    public void postInvoke(Request request, Response response)
        // START SJSAS 6374691
        throws IOException, ServletException
        // END SJSAS 6374691
    {
        // START SJSAS 6374990
        if (((ServletResponse) response).isCommitted()) {
            return;
        }
        // END SJSAS 6374990

        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        // END OF IASRI 4665318
        // Error page processing
        response.setSuspended(false);

        Throwable t = (Throwable) hreq.getAttribute(Globals.EXCEPTION_ATTR);

        if (t != null) {
            throwable(request, response, t);
        } else {
            status(request, response);
        }

        Thread.currentThread().setContextClassLoader
            (standardHostValveClassLoader);

        // START SJSAS 6374691
        if (errorReportValve != null) {
            errorReportValve.postInvoke(request, response);
        }
        // END SJSAS 6374691
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Handle the specified Throwable encountered while processing
     * the specified Request to produce the specified Response.  Any
     * exceptions that occur during generation of the exception report are
     * logged and swallowed.
     *
     * @param request The request being processed
     * @param response The response being generated
     * @param exception The exception that occurred (which possibly wraps
     *  a root cause exception
     */
    protected void throwable(Request request, Response response,
                             Throwable throwable) {
        Context context = request.getContext();
        if (context == null)
            return;
        
        Throwable realError = throwable;
        
        if (realError instanceof ServletException) {
            realError = ((ServletException) realError).getRootCause();
            if (realError == null) {
                realError = throwable;
            }
        } 

        // If this is an aborted request from a client just log it and return
        if (realError instanceof ClientAbortException ) {
            if (log.isDebugEnabled()) {
                log.debug
                    (sm.getString("standardHost.clientAbort",
                                  realError.getCause().getMessage()));
            }
            return;
        }

        ErrorPage errorPage = findErrorPage(context, throwable);
        if ((errorPage == null) && (realError != throwable)) {
            errorPage = findErrorPage(context, realError);
        }

        if (errorPage != null) {
            response.setAppCommitted(false);
            ServletRequest sreq = request.getRequest();
            ServletResponse sresp = response.getResponse();
            sreq.setAttribute
                (ApplicationFilterFactory.DISPATCHER_REQUEST_PATH_ATTR,
                 errorPage.getLocation());
            sreq.setAttribute(ApplicationFilterFactory.DISPATCHER_TYPE_ATTR,
                              Integer.valueOf(ApplicationFilterFactory.ERROR));
            sreq.setAttribute
                (Globals.STATUS_CODE_ATTR,
                 Integer.valueOf(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            sreq.setAttribute(Globals.ERROR_MESSAGE_ATTR,
                              throwable.getMessage());
            sreq.setAttribute(Globals.EXCEPTION_ATTR,
                              realError);
            Wrapper wrapper = request.getWrapper();
            if (wrapper != null)
                sreq.setAttribute(Globals.SERVLET_NAME_ATTR,
                                  wrapper.getName());
            /* GlassFish 6386229
            if (sreq instanceof HttpServletRequest)
                sreq.setAttribute(Globals.EXCEPTION_PAGE_ATTR,
                                  ((HttpServletRequest) sreq).getRequestURI());
            */
            // START GlassFish 6386229
            sreq.setAttribute(Globals.EXCEPTION_PAGE_ATTR,
                              ((HttpServletRequest) sreq).getRequestURI());
            // END GlassFish 6386229
            sreq.setAttribute(Globals.EXCEPTION_TYPE_ATTR,
                              realError.getClass());
            if (custom(request, response, errorPage)) {
                try {
                    sresp.flushBuffer();
                } catch (IOException e) {
                    log("Exception Processing " + errorPage, e);
                }
            }
        } else {
            // A custom error-page has not been defined for the exception
            // that was thrown during request processing. Check if an
            // error-page for error code 500 was specified and if so, 
            // send that page back as the response.
            ServletResponse sresp = (ServletResponse) response;

            /* GlassFish 6386229
            if (sresp instanceof HttpServletResponse) {
                ((HttpServletResponse) sresp).setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                // The response is an error
                response.setError();

                status(request, response);
            }
            */
            // START GlassFish 6386229
            ((HttpServletResponse) sresp).setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // The response is an error
            response.setError();

            status(request, response);
            // END GlassFish 6386229
        }
            

    }


    /**
     * Handle the HTTP status code (and corresponding message) generated
     * while processing the specified Request to produce the specified
     * Response.  Any exceptions that occur during generation of the error
     * report are logged and swallowed.
     *
     * @param request The request being processed
     * @param response The response being generated
     */
    protected void status(Request request, Response response) {

        /* 6386229
        // Do nothing on non-HTTP responses
        if (!(response instanceof HttpResponse))
            return;
        */
        HttpResponse hresponse = (HttpResponse) response;
        /* 6386229
        if (!(response.getResponse() instanceof HttpServletResponse))
            return;
        */
        int statusCode = hresponse.getStatus();

        // Handle a custom error page for this status code
        Context context = request.getContext();
        if (context == null)
            return;

        /*
         * Only look for error pages when isError() is set.
         * isError() is set when response.sendError() is invoked.
         */
        if (!response.isError()) {
            return;
        }

        ErrorPage errorPage = context.findErrorPage(statusCode);
        if (errorPage != null) {
            response.setAppCommitted(false);
            ServletRequest sreq = request.getRequest();
            ServletResponse sresp = response.getResponse();
            sreq.setAttribute(Globals.STATUS_CODE_ATTR,
                              Integer.valueOf(statusCode));
            String message = RequestUtil.filter(hresponse.getMessage());
            if (message == null)
                message = "";
            sreq.setAttribute(Globals.ERROR_MESSAGE_ATTR, message);
            sreq.setAttribute
                (ApplicationFilterFactory.DISPATCHER_REQUEST_PATH_ATTR,
                 errorPage.getLocation());
            sreq.setAttribute(ApplicationFilterFactory.DISPATCHER_TYPE_ATTR,
                              Integer.valueOf(ApplicationFilterFactory.ERROR));
            
             
            Wrapper wrapper = request.getWrapper();
            if (wrapper != null)
                sreq.setAttribute(Globals.SERVLET_NAME_ATTR,
                                  wrapper.getName());
            if (sreq instanceof HttpServletRequest)
                sreq.setAttribute(Globals.EXCEPTION_PAGE_ATTR,
                                  ((HttpServletRequest) sreq).getRequestURI());
            if (custom(request, response, errorPage)) {
                try {
                    sresp.flushBuffer();
                } catch (IOException e) {
                    log("Exception Processing " + errorPage, e);
                }
            }
        }
        // START SJSAS 6324911
        else {
            errorPage = ((StandardHost) getContainer()).findErrorPage(
                                                        statusCode);
            if (errorPage != null) {
                try {
                    handleHostErrorPage(response, errorPage, statusCode);
                } catch (Exception e) {
                    log("Exception Processing " + errorPage, e);
                }
            }
        }
        // END SJSAS 6324911
    }


    /**
     * Find and return the ErrorPage instance for the specified exception's
     * class, or an ErrorPage instance for the closest superclass for which
     * there is such a definition.  If no associated ErrorPage instance is
     * found, return <code>null</code>.
     *
     * @param context The Context in which to search
     * @param exception The exception for which to find an ErrorPage
     */
    protected static ErrorPage findErrorPage
        (Context context, Throwable exception) {

        if (exception == null)
            return (null);
        Class clazz = exception.getClass();
        String name = clazz.getName();
        while (!Object.class.equals(clazz)) {
            ErrorPage errorPage = context.findErrorPage(name);
            if (errorPage != null)
                return (errorPage);
            clazz = clazz.getSuperclass();
            if (clazz == null)
                break;
            name = clazz.getName();
        }
        return (null);

    }


    /**
     * Handle an HTTP status code or Java exception by forwarding control
     * to the location included in the specified errorPage object.  It is
     * assumed that the caller has already recorded any request attributes
     * that are to be forwarded to this page.  Return <code>true</code> if
     * we successfully utilized the specified error page location, or
     * <code>false</code> if the default error report should be rendered.
     *
     * @param request The request being processed
     * @param response The response being generated
     * @param errorPage The errorPage directive we are obeying
     */
    protected boolean custom(Request request, Response response,
                             ErrorPage errorPage) {

        if (debug >= 1)
            log("Processing " + errorPage);

        // Validate our current environment
        /* GlassFish 6386229
        if (!(request instanceof HttpRequest)) {
            if (debug >= 1)
                log(" Not processing an HTTP request --> default handling");
            return (false);     // NOTE - Nothing we can do generically
        }
        */
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        /* GlassFish 6386229
        if (!(response instanceof HttpResponse)) {
            if (debug >= 1)
                log("Not processing an HTTP response --> default handling");
            return (false);     // NOTE - Nothing we can do generically
        }
        */
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();

        ((HttpRequest) request).setPathInfo(errorPage.getLocation());

        try {

            // Reset the response if possible (else IllegalStateException)
            //hres.reset();
            // Reset the response (keeping the real error code and message)
            Integer statusCodeObj =
                (Integer) hreq.getAttribute(Globals.STATUS_CODE_ATTR);
            int statusCode = statusCodeObj.intValue();
            String message = 
                (String) hreq.getAttribute(Globals.ERROR_MESSAGE_ATTR);
            ((HttpResponse) response).reset(statusCode, message);

            // Forward control to the specified location
            ServletContext servletContext =
                request.getContext().getServletContext();
            RequestDispatcher rd =
                servletContext.getRequestDispatcher(errorPage.getLocation());
            rd.forward(hreq, hres);

            // If we forward, the response is suspended again
            response.setSuspended(false);

            // Indicate that we have successfully processed this custom page
            return (true);

        } catch (Throwable t) {

            // Report our failure to process this custom page
            log("Exception Processing " + errorPage, t);
            return (false);

        }

    }


    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        Logger logger = container.getLogger();
        if (logger != null)
            logger.log(this.toString() + ": " + message);
        else
            System.out.println(this.toString() + ": " + message);

    }


    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = container.getLogger();
        if (logger != null)
            logger.log(this.toString() + ": " + message, throwable);
        else {
            System.out.println(this.toString() + ": " + message);
            throwable.printStackTrace(System.out);
        }

    }


    // START SJSAS 6324911
    /**
     * Copies the contents of the given error page to the response, and
     * updates the status message with the reason string of the error page.
     *
     * @param response The response object
     * @param errorPage The error page whose contents are to be copied
     * @param statusCode The status code
     */
    private void handleHostErrorPage(Response response,
                                     ErrorPage errorPage,
                                     int statusCode)
            throws Exception {

        ServletOutputStream ostream = null;
        PrintWriter writer = null;
        FileReader reader = null;
        BufferedInputStream istream = null;
        IOException ioe = null;

        String message = errorPage.getReason();
        if (message != null) {
            ((HttpResponse) response).reset(statusCode, message);
        }
         
        try {
            ostream = response.getResponse().getOutputStream();
        } catch (IllegalStateException e) {
            // If it fails, we try to get a Writer instead if we're
            // trying to serve a text file
            writer = response.getResponse().getWriter();
        }

        if (writer != null) {
            reader = new FileReader(errorPage.getLocation());
            ioe = ResponseUtil.copy(reader, writer);
            try {
                reader.close();
            } catch (Throwable t) {
                ;
            }
        } else {
            istream = new BufferedInputStream(
                new FileInputStream(errorPage.getLocation()));
            ioe = ResponseUtil.copy(istream, ostream);
            try {
                istream.close();
            } catch (Throwable t) {
                ;
            }
        }

        // Rethrow any exception that may have occurred
        if (ioe != null) {
            throw ioe;
        }
    }
    // END SJSAS 6324911


    // START SJSAS 6374691
    void setErrorReportValve(Valve errorReportValve) {
        this.errorReportValve = errorReportValve;
    }
    // END SJSAS 6374691

}
