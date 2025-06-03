package com.lei.enums;

import lombok.Getter;

/**
 * @author lizhilei
 * @version 1.0
 * 状态码定义约束，共6位，前三位代表服务，后三位代表接口
 */
public enum BizCodeEnum {



    /**
     * 账号
     */
    ACCOUNT_REPEAT(250001,"账号已经存在"),
    ACCOUNT_UNREGISTER(250002,"账号不存在"),
    ACCOUNT_PWD_ERROR(250003,"账号或者密码错误"),
    ACCOUNT_UNLOGIN(250004,"账号未登录"),




    /**
     * 通用操作码
     */

    OPS_REPEAT(110001,"重复操作"),
    OPS_NETWORK_ADDRESS_ERROR(110002,"网络地址错误"),


    /**
     * 文件相关
     */
    FILE_UPLOAD_USER_IMG_FAIL(700101,"用户头像文件上传失败"),


    /**
     * 数据库路由信息
     */
    DB_ROUTE_NOT_FOUND(800101,"数据库找不到"),




    /**
     * 数据查询条数超过限制
     */
    DATA_OUT_OF_LIMIT_SIZE(400001,"查询条数超过限制") ,

    /**
     * 数据查询超过最大跨度
     */
    DATA_OUT_OF_LIMIT_DATE(400002,"日期查询超过最大跨度") ;


    @Getter
    private final String message;

    @Getter
    private final int code;

    BizCodeEnum(int code, String message){
        this.code = code;
        this.message = message;
    }
}