package com.lundong.ascentialsync.entity.spend;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-06-02 16:02
 */
@Data
public class SpendCustomField {

	@JSONField(name = "code")
	private String code;

	@JSONField(name = "nameI18n")
	private String nameI18n;
}
