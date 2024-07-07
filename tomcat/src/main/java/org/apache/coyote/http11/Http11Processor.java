package org.apache.coyote.http11;

import camp.nextstep.exception.UncheckedServletException;
import org.apache.coyote.Processor;
import org.apache.coyote.request.RequestHeaders;
import org.apache.coyote.request.RequestLine;
import org.apache.coyote.response.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);
    private static final String ROOT_PATH = "/";
    private static final String ROOT_CONTENT = "Hello world!";

    private final Socket connection;

    public Http11Processor(final Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.info("connect host: {}, port: {}", connection.getInetAddress(), connection.getPort());
        process(connection);
    }

    @Override
    public void process(final Socket connection) {

        try (final var inputStream = connection.getInputStream();
             final var bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
             final var outputStream = connection.getOutputStream()) {

            String requestLineValue = bufferedReader.readLine();
            List<String> requestHeaderValues = bufferedReader.lines()
                    .takeWhile(line -> !line.isEmpty())
                    .toList();

            RequestLine requestLine = RequestLine.parse(requestLineValue);
            RequestHeaders requestHeaders = RequestHeaders.parse(requestHeaderValues);

            String httpPath = requestLine.getHttpPath();
            String responseBody = findResponseBody(httpPath);
            MimeType mimeType = MimeType.from(httpPath);

            String response = String.join("\r\n",
                    "HTTP/1.1 200 OK ",
                    "Content-Type: " + mimeType.getContentType() + "  ",
                    "Content-Length: " + responseBody.getBytes().length + " ",
                    "",
                    responseBody);

            outputStream.write(response.getBytes());
            outputStream.flush();
        } catch (IOException | UncheckedServletException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String findResponseBody(String httpPath) throws IOException {
        if (httpPath.equals(ROOT_PATH)) {
            return ROOT_CONTENT;
        }
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource("static" + httpPath);
        Path path = Path.of(Objects.requireNonNull(resource).getPath());
        return new String(Files.readAllBytes(path));
    }
}
