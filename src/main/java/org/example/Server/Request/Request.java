package org.example.Server.Request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ToString()
public class Request {
    private final static Logger myLogger = LogManager.getLogger(Request.class);
    private final static byte[] LINE_DELIMITER = new byte[]{'\r', '\n'};
    private final static byte[] HEAD_DELIMITER = new byte[]{'\r', '\n', '\r', '\n'};
    @Getter(AccessLevel.PUBLIC)
    private final RequestMethod method;
    @Getter(AccessLevel.PUBLIC)
    private final String path;
    @Getter(AccessLevel.PUBLIC)
    private final String protocol;
    @Getter(AccessLevel.PUBLIC)
    private final List<String> headers;
    @Getter(AccessLevel.PUBLIC)
    @Setter(AccessLevel.PUBLIC)
    private String body;
    @Setter(AccessLevel.PUBLIC)
    @Getter(AccessLevel.PUBLIC)
    private List<NameValuePair> requestParameters;

    private Request(RequestMethod method, String path, String protocol, List<String> headers) {
        this.method = method;
        this.path = path;
        this.protocol = protocol;
        this.headers = headers;
    }

    public static Request getRequest(byte[] buffer, int read, BufferedInputStream inputStream) {
        final int requestLineEnd = Utils.indexOf(buffer, LINE_DELIMITER, 0, read);
        if (requestLineEnd > -1) {
            String[] requestLineParts = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLineParts.length == 3 && requestLineParts[1].startsWith("/")) {
                final int headersStart = requestLineEnd + LINE_DELIMITER.length;
                final int headersEnd = Utils.indexOf(buffer, HEAD_DELIMITER, headersStart, read);
                if (headersEnd > -1) {
                    try {
                        inputStream.reset();
                        inputStream.skip(headersStart);
                        List<String> headers = Arrays.asList(
                                new String(inputStream.readNBytes(headersEnd - headersStart))
                                        .split("\r\n")
                        );
                        int endPath = requestLineParts[1].indexOf('?');
                        Request request = new Request(
                                RequestMethod.valueOf(requestLineParts[0]),
                                (endPath == -1 ? requestLineParts[1] : requestLineParts[1].substring(0, endPath)),
                                requestLineParts[2],
                                headers
                        );
                        if (request.getMethod() != RequestMethod.GET) {
                            inputStream.skip(HEAD_DELIMITER.length);
                            final Optional<String> contentLength = request.getHeader("Content-Length");
                            if (contentLength.isPresent()) {
                                final byte[] myBody = inputStream.readNBytes(Integer.parseInt(contentLength.get()));
                                request.setBody(new String(myBody));
                            }
                        }
                        try {
                            request.setRequestParameters(URLEncodedUtils.parse(new URI(requestLineParts[1]), StandardCharsets.UTF_8));
                        } catch (URISyntaxException e) {
                            myLogger.error(e.getMessage());
                        }
                        return request;
                    } catch (IOException e) {
                        myLogger.error(e.getMessage());
                    }
                }
            }
        }
        return null;
    }

    public Optional<String> getHeader(String headerName) {
        return headers.stream()
                .filter(x -> x.startsWith(headerName))
                .map(x -> x.substring(x.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private List<String> getParameter(List<NameValuePair> parameters, String parameterName) {
        return parameters.stream()
                .filter(x -> x.getName().equals(parameterName))
                .map(NameValuePair::getValue)
                .collect(Collectors.toList());
    }

    public List<String> getRequestParameter(String parameterName) {
        return getParameter(requestParameters, parameterName);
    }
}
