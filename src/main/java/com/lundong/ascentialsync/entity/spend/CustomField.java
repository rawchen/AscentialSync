package com.lundong.ascentialsync.entity.spend;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-07-04 15:23
 */
@Data
public class CustomField {

	@JSONField(name = "dynamic_field_code")
	private String dynamicFieldCode;

	@JSONField(name = "field_code")
	private String fieldCode;

	@JSONField(name = "field_value_code")
	private String fieldValueCode;

	@JSONField(name = "field_value_name")
	private String fieldValueName;
}
