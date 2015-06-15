package org.g1.tools;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Logger;

public class RequestResponseDumpFilter implements Filter {

  private static Logger log = Logger.getLogger(RequestResponseDumpFilter.class.getName());

  private static final String NON_HTTP_REQ_MSG =
      "Not available. Non-http request.";
  private static final String NON_HTTP_RES_MSG =
      "Not available. Non-http response.";

  private static final ThreadLocal<Timestamp> timestamp =
      new ThreadLocal<Timestamp>() {
        @Override
        protected Timestamp initialValue() {
          return new Timestamp();
        }
      };

  private static final ThreadLocal<String> jessionId = new ThreadLocal<String>();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest hRequest = null;
    HttpServletResponse hResponse = null;
    final ByteArrayPrintWriter pw = new ByteArrayPrintWriter();
    if (request instanceof HttpServletRequest) {
      hRequest = (HttpServletRequest) request;
    }
    if (response instanceof HttpServletResponse) {
      hResponse = (HttpServletResponse) response;
      hResponse = new HttpServletResponseWrapper(hResponse) {
        public PrintWriter getWriter() {
          return pw.getWriter();
        }

        public ServletOutputStream getOutputStream() {
          return pw.getStream();
        }
      };
    }

    // Log pre-service information
    doLog("START TIME        ", getTimestamp());

    if (hRequest == null) {
      doLog("        requestURI", NON_HTTP_REQ_MSG);
      doLog("          authType", NON_HTTP_REQ_MSG);
    } else {
      doLog("        requestURI", hRequest.getRequestURI());
      doLog("          authType", hRequest.getAuthType());
    }

    doLog(" characterEncoding", request.getCharacterEncoding());
    doLog("     contentLength",
        Integer.valueOf(request.getContentLength()).toString());
    doLog("       contentType", request.getContentType());

    if (hRequest == null) {
      doLog("       contextPath", NON_HTTP_REQ_MSG);
      doLog("            cookie", NON_HTTP_REQ_MSG);
      doLog("            header", NON_HTTP_REQ_MSG);
    } else {
      doLog("       contextPath", hRequest.getContextPath());
      Cookie cookies[] = hRequest.getCookies();
      if (cookies != null) {
        for (int i = 0; i < cookies.length; i++) {
          doLog("            cookie", cookies[i].getName() +
              "=" + cookies[i].getValue());
        }
      }
      Enumeration<String> hnames = hRequest.getHeaderNames();
      while (hnames.hasMoreElements()) {
        String hname = hnames.nextElement();
        Enumeration<String> hvalues = hRequest.getHeaders(hname);
        while (hvalues.hasMoreElements()) {
          String hvalue = hvalues.nextElement();
          doLog("            header", hname + "=" + hvalue);
        }
      }
    }

    doLog("            locale", request.getLocale().toString());

    if (hRequest == null) {
      doLog("            method", NON_HTTP_REQ_MSG);
    } else {
      doLog("            method", hRequest.getMethod());
    }

    Enumeration<String> pnames = request.getParameterNames();
    while (pnames.hasMoreElements()) {
      String pname = pnames.nextElement();
      String pvalues[] = request.getParameterValues(pname);
      StringBuilder result = new StringBuilder(pname);
      result.append('=');
      for (int i = 0; i < pvalues.length; i++) {
        if (i > 0) {
          result.append(", ");
        }
        result.append(pvalues[i]);
      }
      doLog("         parameter", result.toString());
    }

    if (hRequest == null) {
      doLog("          pathInfo", NON_HTTP_REQ_MSG);
    } else {
      doLog("          pathInfo", hRequest.getPathInfo());
    }

    doLog("          protocol", request.getProtocol());

    if (hRequest == null) {
      doLog("       queryString", NON_HTTP_REQ_MSG);
    } else {
      doLog("       queryString", hRequest.getQueryString());
    }

    doLog("        remoteAddr", request.getRemoteAddr());
    doLog("        remoteHost", request.getRemoteHost());

    if (hRequest == null) {
      doLog("        remoteUser", NON_HTTP_REQ_MSG);
      doLog("requestedSessionId", NON_HTTP_REQ_MSG);
    } else {
      doLog("        remoteUser", hRequest.getRemoteUser());
      doLog("requestedSessionId", hRequest.getRequestedSessionId());
    }

    doLog("            scheme", request.getScheme());
    doLog("        serverName", request.getServerName());
    doLog("        serverPort",
        Integer.valueOf(request.getServerPort()).toString());

    if (hRequest == null) {
      doLog("       servletPath", NON_HTTP_REQ_MSG);
    } else {
      doLog("       servletPath", hRequest.getServletPath());
    }

    doLog("          isSecure",
        Boolean.valueOf(request.isSecure()).toString());
    if (hRequest != null) {
      BufferedReader reader = hRequest.getReader();
      String line = null;
      StringBuilder inputBuffer = new StringBuilder();
      do {
        line = reader.readLine();
        if (null != line) {
          inputBuffer.append(line);
        }
      } while (line != null);
      reader.close();
      doLog("     request body", inputBuffer.toString());
    }
    doLog("------------------",
        "--------------------------------------------");
    // Perform the request
    chain.doFilter(request, hResponse);
    pw.getStream().flush();
    pw.getWriter().flush();
    // Log post-service information
    doLog("------------------",
        "--------------------------------------------");
    if (hRequest == null) {
      doLog("          authType", NON_HTTP_REQ_MSG);
    } else {
      doLog("          authType", hRequest.getAuthType());
    }

    doLog("       contentType", response.getContentType());

    if (hResponse == null) {
      doLog("            header", NON_HTTP_RES_MSG);
    } else {
      Iterable<String> rhnames = hResponse.getHeaderNames();
      for (String rhname : rhnames) {
        Iterable<String> rhvalues = hResponse.getHeaders(rhname);
        for (String rhvalue : rhvalues) {
          doLog("            header", rhname + "=" + rhvalue);
        }
      }
    }

    if (hRequest == null) {
      doLog("        remoteUser", NON_HTTP_REQ_MSG);
    } else {
      doLog("        remoteUser", hRequest.getRemoteUser());
    }

    if (hResponse == null) {
      doLog("        remoteUser", NON_HTTP_RES_MSG);
    } else {
      doLog("            status",
          Integer.valueOf(hResponse.getStatus()).toString());
    }
    if (hResponse != null) {
      byte[] bytes = pw.toByteArray();
      response.getOutputStream().write(bytes);
      doLog("response body", new String(bytes));
    }
    doLog("END TIME          ", getTimestamp());
    doLog("==================",
        "============================================");
  }

  private void doLog(String attribute, String value) {
    StringBuilder sb = new StringBuilder(80);
    sb.append(Thread.currentThread().getName());
    sb.append(' ');
    sb.append(attribute);
    sb.append('=');
    sb.append(value);
    log.info(sb.toString());
  }

  private String getTimestamp() {
    Timestamp ts = timestamp.get();
    long currentTime = System.currentTimeMillis();

    if ((ts.date.getTime() + 999) < currentTime) {
      ts.date.setTime(currentTime - (currentTime % 1000));
      ts.update();
    }
    return ts.dateString;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // NOOP
  }

  @Override
  public void destroy() {
    // NOOP
  }

  private static final class Timestamp {
    private final Date date = new Date(0);
    private final SimpleDateFormat format =
        new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private String dateString = format.format(date);

    private void update() {
      dateString = format.format(date);
    }
  }

  private static class ByteArrayServletStream extends ServletOutputStream {

    ByteArrayOutputStream baos;

    ByteArrayServletStream(ByteArrayOutputStream baos) {
      this.baos = baos;
    }

    public void write(int param) throws IOException {
      baos.write(param);
    }
  }

  private static class ByteArrayPrintWriter {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    private PrintWriter pw = new PrintWriter(baos);

    private ServletOutputStream sos = new ByteArrayServletStream(baos);

    public PrintWriter getWriter() {
      return pw;
    }

    public ServletOutputStream getStream() {
      return sos;
    }

    byte[] toByteArray() {
      return baos.toByteArray();
    }
  }
}
