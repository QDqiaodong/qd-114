package com.flower.cultivation.exception;

import com.flower.cultivation.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VarietyOccupiedException.class)
    public Result<Object> handleVarietyOccupiedException(VarietyOccupiedException e) {
        log.warn("品种占用异常: {}", e.getMessage());
        Result<Object> result = new Result<>();
        result.setCode(409);
        result.setMessage(e.getMessage());
        result.setData(e.getOccupancies());
        return result;
    }

    @ExceptionHandler(StageTransitionException.class)
    public Result<Object> handleStageTransitionException(StageTransitionException e) {
        log.warn("阶段转换异常: {}", e.getMessage());
        Result<Object> result = new Result<>();
        result.setCode(422);
        result.setMessage(e.getMessage());
        return result;
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.fail("系统异常，请联系管理员");
    }
}
