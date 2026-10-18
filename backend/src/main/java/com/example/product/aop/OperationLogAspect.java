package com.example.product.aop;

import com.example.product.entity.SysOperationLog;
import com.example.product.repository.SysOperationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面：拦截标注了 {@link OperationLog} 的方法，记录操作人、模块、参数、耗时与结果。
 * <p>日志写库通过 {@link OperationLogWriter} 异步执行，不影响主业务响应时间。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogWriter logWriter;
    private final ObjectMapper objectMapper;

    @Pointcut("@annotation(com.example.product.aop.OperationLog)")
    public void logPointcut() {
    }

    @Around(value = "logPointcut() && @annotation(operationLog)", argNames = "pjp,operationLog")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                SysOperationLog entity = buildLog(pjp, operationLog, System.currentTimeMillis() - start, error);
                logWriter.saveAsync(entity);
            } catch (Exception e) {
                // 记录日志失败绝不能影响业务
                log.warn("操作日志记录失败: {}", e.getMessage());
            }
        }
    }

    private SysOperationLog buildLog(ProceedingJoinPoint pjp, OperationLog annotation,
                                     long durationMs, Throwable error) {
        SysOperationLog entity = new SysOperationLog();
        entity.setModule(annotation.module());
        entity.setOperation(annotation.operation());
        entity.setDurationMs(durationMs);
        entity.setSuccess(error == null ? 1 : 0);
        if (error != null) {
            String msg = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            entity.setErrorMessage(truncate(msg, 1000));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            entity.setUsername(authentication.getName());
        } else {
            entity.setUsername("anonymous");
        }

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setHttpMethod(request.getMethod());
            entity.setRequestUri(request.getRequestURI());
            entity.setIp(clientIp(request));
        }

        if (annotation.saveParams()) {
            entity.setParams(truncate(serializeArgs(pjp.getArgs()), 2000));
        }
        return entity;
    }

    /** 序列化方法入参，跳过 Servlet 对象与文件上传对象 */
    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            Object[] filtered = Arrays.stream(args)
                    .filter(a -> a != null
                            && !(a instanceof HttpServletRequest)
                            && !(a instanceof jakarta.servlet.http.HttpServletResponse)
                            && !(a instanceof MultipartFile)
                            && !(a instanceof MultipartFile[]))
                    .toArray();
            if (filtered.length == 0) {
                return null;
            }
            return objectMapper.writeValueAsString(filtered.length == 1 ? filtered[0] : filtered);
        } catch (Exception e) {
            return Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", "));
        }
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "...(截断)";
    }

    /**
     * 独立的异步写入组件（单独成 bean 才能让 @Async 生效）。
     */
    @Slf4j
    @Service
    @RequiredArgsConstructor
    public static class OperationLogWriter {

        private final SysOperationLogRepository repository;

        @Async
        public void saveAsync(SysOperationLog entity) {
            try {
                repository.save(entity);
            } catch (Exception e) {
                log.warn("异步写入操作日志失败: {}", e.getMessage());
            }
        }
    }
}
