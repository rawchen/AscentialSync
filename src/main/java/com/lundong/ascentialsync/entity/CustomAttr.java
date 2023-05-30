package com.lundong.ascentialsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Builder;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-05-25 11:11
 */
@Data
@Builder
public class CustomAttr {

	@JSONField(name = "id")
	private String id;

	@JSONField(name = "type")
	private String type;

	@JSONField(name = "value")
	private String value;
}
