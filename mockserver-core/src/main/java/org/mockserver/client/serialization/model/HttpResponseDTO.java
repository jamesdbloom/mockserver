package org.mockserver.client.serialization.model;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.mockserver.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jamesdbloom
 */
public class HttpResponseDTO extends ObjectWithReflectiveEqualsHashCodeToString {
    private Integer statusCode;
    private BodyWithContentTypeDTO body;
    private List<CookieDTO> cookies = new ArrayList<CookieDTO>();
    private List<HeaderDTO> headers = new ArrayList<HeaderDTO>();
    private DelayDTO delay;
    private ConnectionOptionsDTO connectionOptions;

    public HttpResponseDTO(HttpResponse httpResponse) {
        if (httpResponse != null) {
            statusCode = httpResponse.getStatusCode();
            body = BodyWithContentTypeDTO.createDTO(httpResponse.getBody());
            headers = Lists.transform(httpResponse.getHeaders(), new Function<Header, HeaderDTO>() {
                public HeaderDTO apply(Header header) {
                    return new HeaderDTO(header);
                }
            });
            cookies = Lists.transform(httpResponse.getCookies(), new Function<Cookie, CookieDTO>() {
                public CookieDTO apply(Cookie cookie) {
                    return new CookieDTO(cookie);
                }
            });
            delay = (httpResponse.getDelay() != null ? new DelayDTO(httpResponse.getDelay()) : null);
            connectionOptions = (httpResponse.getConnectionOptions() != null ? new ConnectionOptionsDTO(httpResponse.getConnectionOptions()) : null);
        }
    }

    public HttpResponseDTO() {
    }

    public HttpResponse buildObject() {
        return new HttpResponse()
                .withStatusCode(statusCode)
                .withBody(body != null ? body.buildObject() : null)
                .withHeaders(Lists.transform(headers, new Function<HeaderDTO, Header>() {
                    public Header apply(HeaderDTO header) {
                        return header.buildObject();
                    }
                }))
                .withCookies(Lists.transform(cookies, new Function<CookieDTO, Cookie>() {
                    public Cookie apply(CookieDTO cookie) {
                        return cookie.buildObject();
                    }
                }))
                .withDelay((delay != null ? delay.buildObject() : null))
                .withConnectionOptions((connectionOptions != null ? connectionOptions.buildObject() : null));
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public HttpResponseDTO setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public BodyWithContentTypeDTO getBody() {
        return body;
    }

    public HttpResponseDTO setBody(BodyWithContentTypeDTO body) {
        this.body = body;
        return this;
    }

    public List<CookieDTO> getCookies() {
        return cookies;
    }

    public HttpResponseDTO setCookies(List<CookieDTO> cookies) {
        this.cookies = cookies;
        return this;
    }

    public List<HeaderDTO> getHeaders() {
        return headers;
    }

    public HttpResponseDTO setHeaders(List<HeaderDTO> headers) {
        this.headers = headers;
        return this;
    }

    public DelayDTO getDelay() {
        return delay;
    }

    public HttpResponseDTO setDelay(DelayDTO delay) {
        this.delay = delay;
        return this;
    }

    public ConnectionOptionsDTO getConnectionOptions() {
        return connectionOptions;
    }

    public HttpResponseDTO setConnectionOptions(ConnectionOptionsDTO connectionOptions) {
        this.connectionOptions = connectionOptions;
        return this;
    }
}

