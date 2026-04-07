package com.weibo.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 统一响应结果类
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public static <T> Result<T> success() {
        return new Result<T>().setCode(200).setMessage("操作成功");
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<T>().setCode(200).setMessage("操作成功").setData(data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<T>().setCode(200).setMessage(message).setData(data);
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<T>().setCode(500).setMessage(message);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<T>().setCode(code).setMessage(message);
    }
    
    public static <T> Result<T> unauthenticated() {
        return new Result<T>().setCode(401).setMessage("未登录或登录已过期");
    }
    
    public static <T> Result<T> forbidden() {
        return new Result<T>().setCode(403).setMessage("没有权限");
    }
    
    public static <T> Result<T> notFound() {
        return new Result<T>().setCode(404).setMessage("资源不存在");
    }
}
