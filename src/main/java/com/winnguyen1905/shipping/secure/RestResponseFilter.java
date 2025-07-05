package com.winnguyen1905.shipping.secure;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class RestResponseFilter
    implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(MethodParameter returnType, Class converterType) {
    return true;
  }

  @Override
  @Nullable
  public Object beforeBodyWrite(
      Object body, MethodParameter returnType, MediaType selectedContentType,
      Class selectedConverterType, ServerHttpRequest request,
      ServerHttpResponse response) {

    if (body instanceof Resource) {
      return body;
    }

    HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
    int statusCode = servletResponse.getStatus();

    if (statusCode > 399) {
      return body;
    }

    RestResponse<Object> restResponse = RestResponse.builder()
        .statusCode(statusCode)
        .data(body)
        .build();

    if (returnType.getMethodAnnotation(ResponseMessage.class) instanceof ResponseMessage message) {
      restResponse = RestResponse.builder()
          .statusCode(statusCode)
          .data(body)
          .message(message.message())
          .build();
    }

    return restResponse;
  }

}
