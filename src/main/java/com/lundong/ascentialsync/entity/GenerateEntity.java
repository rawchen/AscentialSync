package com.lundong.ascentialsync.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.ByteArrayOutputStream;

/**
 * @author shuangquan.chen
 * @date 2023-09-18 12:01
 */
@Data
@Accessors(chain = true)
public class GenerateEntity {

    private ByteArrayOutputStream byteArrayOutputStream;

    private String fileName;

    private String formCode;

}
