package com.xiaonan.xnbi.model.dto.text;

import lombok.Data;

import java.io.Serializable;

@Data
public class UploadTextRequest implements Serializable {

    /**
     * 业务
     */
    private String biz;

    /**
     * 问题
     */
    private String goal;

    private static final long serialVersionUID = 1L;
}
