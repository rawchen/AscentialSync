package com.lundong.ascentialsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-04-24 16:45
 */
@Data
public class FeishuDept {

    @JSONField(name = "department_id")
    private String departmentId;

    @JSONField(name = "parent_department_id")
    private String parentDepartmentId;

    @JSONField(name = "name")
    private String name;
}
