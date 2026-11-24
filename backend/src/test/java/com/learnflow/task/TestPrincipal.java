package com.learnflow.task;
import com.learnflow.user.LoginUser;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.*;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;

class TestPrincipal {
    static HandlerMethodArgumentResolver resolver() {
        return new HandlerMethodArgumentResolver() {
            public boolean supportsParameter(MethodParameter parameter) { return parameter.getParameterType() == LoginUser.class; }
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                    NativeWebRequest request, WebDataBinderFactory binder) {
                return new LoginUser(1L, "testuser", "not-a-real-hash");
            }
        };
    }
}
