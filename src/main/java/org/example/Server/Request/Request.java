package org.example.Server.Request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
    @Setter(AccessLevel.PUBLIC)
    @Getter(AccessLevel.PUBLIC)
    private List<NameValuePair> postParameters;
    @Setter(AccessLevel.PUBLIC)
    @Getter(AccessLevel.PUBLIC)
    private List<FileItem> parts;

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
                            final Optional<String> contentType = request.getHeader("Content-Type");
                            if (contentType.isPresent()) {
                                final String[] mimeType = contentType.get().split(";");
                                //myLogger.info(String.format("Mime-Type: %s, Path: %s", mimeType[0], request.getPath()));
                                if (mimeType[0].equals("application/x-www-form-urlencoded")) {
                                    request.setPostParameters(URLEncodedUtils.parse(request.getBody(), StandardCharsets.UTF_8));
                                }
                                if (mimeType[0].equals("multipart/form-data")) {
                                    try {
                                        DiskFileItemFactory factory = new DiskFileItemFactory(1048576, Path.of(".", "tmp").toFile());
                                        FileUpload upload = new FileUpload(factory);
                                        try {
                                            request.setParts(upload.parseRequest(request.getRequestContext(request)));
                                        } catch (Exception e) {
                                            myLogger.error(e.getMessage());
                                        }
                                    } catch (Exception e) {
                                        myLogger.error(e.getMessage());
                                    }
                                }
                                myLogger.info("=".repeat(100));
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

    public List<String> getPostParameter(String parameterName) {
        return getParameter(postParameters, parameterName);
    }

    private RequestContext getRequestContext(Request request) {
        return new RequestContext() {
            @Override
            public String getCharacterEncoding() {
                return StandardCharsets.UTF_8.displayName();
            }

            @Override
            public String getContentType() {
                final Optional<String> contentType = request.getHeader("Content-Type");
                return contentType.orElse(null);
            }

            @Override
            public int getContentLength() {
                final Optional<String> contentLength = request.getHeader("Content-Length");
                return contentLength.map(Integer::parseInt).orElse(0);
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(request.getBody().getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    public String getPart(String fieldName) {
        if (parts != null) {
            Optional<FileItem> item = parts.stream().filter(x -> x.getFieldName().equals(fieldName)).findFirst();
            if (item.isPresent()) {
                if (item.get().isFormField()) {
                    return item.get().getString();
                } else {
                    return item.get().getName();
                }
            }
        }
        return "";
    }
}