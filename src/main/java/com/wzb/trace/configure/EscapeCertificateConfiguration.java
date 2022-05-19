package com.wzb.trace.configure;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Duration;

@Getter
@Setter
public class EscapeCertificateConfiguration {

    private Rest rest;

    public Rest getNoOrDefaultRest() {
        if (null == rest) {
            rest = new Rest();
            rest.setReadTimeout(Duration.ofMinutes(5));
            rest.setConnectTimeout(Duration.ofMinutes(5));
        }
        return rest;
    }

    @Getter
    @Setter
    public static class Rest implements Serializable {
        private Duration readTimeout;
        private Duration connectTimeout;
    }

}
