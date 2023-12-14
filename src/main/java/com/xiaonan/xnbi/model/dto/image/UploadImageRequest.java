package com.xiaonan.xnbi.model.dto.image;

import lombok.Data;

import java.io.Serializable;

@Data
public class UploadImageRequest implements Serializable {

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
