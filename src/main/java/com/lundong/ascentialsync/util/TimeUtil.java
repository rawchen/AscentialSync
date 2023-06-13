package com.lundong.ascentialsync.util;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * @author RawChen
 * @date 2023-03-02 15:14
 */
public class TimeUtil {

	/**
	 * 生成时间戳
	 *
	 * @return
	 */
	public static String getTimestamp() {
		return (Calendar.getInstance().getTimeInMillis() / 1000) + "";
	}

	/**
	 * 时间戳转UTC格式
	 *
	 * @param joinTime
	 * @return
	 */
	public static String timestampToUTC(String joinTime) {
		try {
			LocalDateTime localDateTime = LocalDateTimeUtil
					.ofUTC(Long.parseLong(joinTime + "000") + 28800000);
			return localDateTime
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+08:00"));
		} catch (NumberFormatException e) {
			return LocalDateTime.now().
					format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+08:00"));
		}
	}

	/**
	 * 时间戳转yyyyMMddhms格式
	 *
	 * @param joinTime
	 * @return
	 */
	public static String timestampToDateFormat(String joinTime) {
		try {
			LocalDateTime localDateTime = LocalDateTimeUtil.of(Long.parseLong(joinTime));
			return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		} catch (NumberFormatException e) {
			return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		}
	}

	/**
	 * 时间戳转Date
	 *
	 * @param joinTime
	 * @return
	 */
	public static Date timestampToDate(String joinTime) {
		try {
			if (joinTime != null) {
				Date date;
				ZoneId zoneId = ZoneId.systemDefault();
				LocalDateTime localDateTime = LocalDateTimeUtil.of(Long.parseLong(joinTime));
				ZonedDateTime zdt = localDateTime.atZone(zoneId);
				date = Date.from(zdt.toInstant());
				return date;
			} else {
				return new Date();
			}
		} catch (NumberFormatException e) {
			return new Date();
		}
	}

	/**
	 * 获取指定某一天的开始时间戳
	 *
	 * @param date
	 * @return
	 */
	public static Long getDailyStartTime(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}

	/**
	 * 获取指定某一天的结束时间戳
	 *
	 * @param date
	 * @return
	 */
	public static Long getDailyEndTime(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTimeInMillis();
	}
}
