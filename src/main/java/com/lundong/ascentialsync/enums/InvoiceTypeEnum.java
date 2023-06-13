package com.lundong.ascentialsync.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发票类型枚举
 *
 * @author RawChen
 * @date 2023-06-06 15:12
 */
@Getter
@AllArgsConstructor
public enum InvoiceTypeEnum {
	VAT_GENERAL_ELECTRIC("VAT_GENERAL_ELECTRIC", "增值税电子普通发票"),
	VAT("VAT", "增值税专用发票"),
	VAT_ELECTRIC("VAT_ELECTRIC", "增值税电子专用发票"),
	VAT_GENERAL_TOLL_FEE("VAT_GENERAL_TOLL_FEE", "增值税电子普通发票（通行费）"),
	PASSENGER_INVOICE("PASSENGER_INVOICE", "客运发票"),
	HIGHWAY_TOLL_INVOICE("HIGHWAY_TOLL_INVOICE", "高速公路通行费"),
	AIR_TICKET_ITINERARY("AIR_TICKET_ITINERARY", "航空机票行程单"),
	TRAIN_TICKET("TRAIN_TICKET", "火车票"),
	TAXI_INVOICE("TAXI_INVOICE", "出租车发票"),
	DEFAULT_INVOICE("DEFAULT_INVOICE", "0% VAT");

	private String type;
	private String desc;

	public static InvoiceTypeEnum getType(String dataTypeCode) {
		for (InvoiceTypeEnum enums : InvoiceTypeEnum.values()) {
			if (enums.getType().equals(dataTypeCode)) {
				return enums;
			}
		}
		return DEFAULT_INVOICE;
	}
}
