package com.lumenml.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(
            pointcut =
                    "execution(* com.lumenml.api..*Controller.*(..)) && @annotation(org.springframework.web.bind.annotation.PostMapping)",
            returning = "result")
    public void auditPost(JoinPoint jp, Object result) {
        String ip = currentIp();
        Map<String, Object> meta = new HashMap<>();
        meta.put("method", jp.getSignature().toShortString());
        auditService.log("HTTP_POST", jp.getTarget().getClass().getSimpleName(), null, ip, meta);
    }

    private String currentIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
