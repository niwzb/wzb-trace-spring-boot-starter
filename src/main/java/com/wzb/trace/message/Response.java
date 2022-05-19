package com.wzb.trace.message;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class Response<T> {

    private int code;
    private String message;
    private T data;

    public Response(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return HttpStatus.valueOf(this.code).is2xxSuccessful();
    }

    public String getReasonPhrase() {
        return HttpStatus.valueOf(this.code).getReasonPhrase();
    }

    public static <T> Response<T> success(String message) {
        return new Response<>(200, message);
    }

    public static <T> Response<T> success(String message, T data) {
        return new Response<>(200, message, data);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(500, message);
    }

    public static <T> Response<T> error(String message, T data) {
        return new Response<>(500, message, data);
    }

    public static boolean isSuccess(int code) {
        return HttpStatus.valueOf(code).is2xxSuccessful();
    }

    public static String getReasonPhrase(int code) {
        return HttpStatus.valueOf(code).getReasonPhrase();
    }
}
