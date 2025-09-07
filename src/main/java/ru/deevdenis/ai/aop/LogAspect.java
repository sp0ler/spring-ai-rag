package ru.deevdenis.ai.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private ObjectMapper objectMapper;

    @Around("execution(* ru.deevdenis.ai.service.*.*(..)) || execution(* ru.deevdenis.ai.rag.*.*(..))")
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // Логируем входные параметры
        logMethodArguments(className, methodName, joinPoint.getArgs());

        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // Логируем результат
            logMethodResult(className, methodName, result, executionTime);

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logMethodError(className, methodName, throwable, executionTime);
            throw throwable;
        }
    }

    private void logMethodArguments(String className, String methodName, Object[] args) {
        try {
            if (args != null && args.length > 0) {
                String argumentsJson = objectMapper.writeValueAsString(args);
                log.info("[{}::{}] Входные аргументы: {}", className, methodName, argumentsJson);
            } else {
                log.info("[{}::{}] Вызван без аргументов", className, methodName);
            }
        } catch (Exception e) {
            log.warn("[{}::{}] Не удалось сериализовать аргументы: {}", className, methodName, e.getMessage());
            log.debug("[{}::{}] Аргументы (raw): {}", className, methodName, arrayToString(args));
        }
    }

    private void logMethodResult(String className, String methodName, Object result, long executionTime) {
        try {
            if (result != null) {
                String resultJson = objectMapper.writeValueAsString(result);
                log.info("[{}::{}] Результат ({} мс): {}",
                        className, methodName, executionTime, resultJson);
            } else {
                log.info("[{}::{}] Возвращен null ({} мс)", className, methodName, executionTime);
            }
        } catch (Exception e) {
            log.warn("[{}::{}] Не удалось сериализовать результат: {}", className, methodName, e.getMessage());
            log.info("[{}::{}] Результат ({} мс): {}",
                    className, methodName, executionTime, result != null ? result.toString() : "null");
        }
    }

    private void logMethodError(String className, String methodName, Throwable throwable, long executionTime) {
        log.error("[{}::{}] Ошибка при выполнении ({} мс): {}",
                className, methodName, executionTime, throwable.getMessage(), throwable);
    }

    private String arrayToString(Object[] array) {
        if (array == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(array[i] != null ? array[i].toString() : "null");
        }
        sb.append("]");
        return sb.toString();
    }
}
