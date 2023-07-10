package com.lundong.ascentialsync.util;

import com.lundong.ascentialsync.entity.ExcelRecord;
import com.lundong.ascentialsync.entity.spend.ReimburseData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 数据列表过滤工具
 *
 * @author RawChen
 * @date 2023-06-09 11:20
 */
@Slf4j
public class DataFilterUtil {
	public static List<ReimburseData> filterByVersion(List<ReimburseData> documentList) {
		// 存储每个单据的最新版本号和对应的单据id
		Map<String, String> latestVersionMap = new HashMap<>();

		for (ReimburseData document : documentList) {
			String id = document.getFormCode();
			String version = document.getVersion();

			if (!latestVersionMap.containsKey(id) || Long.parseLong(version) > Long.parseLong(latestVersionMap.get(id))) {
				latestVersionMap.put(id, version);
			}
		}

		// 遍历latestVersionMap
//		for (Map.Entry<String, String> entry : latestVersionMap.entrySet()) {
//			entry.getValue();
//			entry.getKey();
//		}

		List<ReimburseData> documentListNew = new ArrayList<>();
		for (ReimburseData reimburseData : documentList) {
			String s = latestVersionMap.get(reimburseData.getFormCode());
			// 如果根据formCode能在这个map找到对应的version并相等，说明遍历的这个实体是最新的
			if (reimburseData.getVersion().equals(s)) {
				documentListNew.add(reimburseData);
			}
		}

		return documentListNew;
	}

	/**
	 * 对象集合根据某属性去重
	 *
	 * @param keyExtractor
	 * @return
	 * @param <T>
	 */
	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	/**
	 * 根据保账行ID在excelRecords中去重，留下同一报销ID的两条
	 *
	 * @param excelRecords
	 * @param reimburseLineIdList
	 * @return
	 */
	public static List<ExcelRecord> distinctByReimburseLineId(List<ExcelRecord> excelRecords, List<String> reimburseLineIdList) {
		List<ExcelRecord> records = new ArrayList<>();
		for (String s : reimburseLineIdList) {
			List<ExcelRecord> list = new ArrayList<>();
			for (ExcelRecord excelRecord : excelRecords) {
				if (excelRecord.getReimburseLineId() != null && excelRecord.getReimburseLineId().equals(s)) {
					list.add(excelRecord);
				}
			}
			// 如果list数量大于2
			if (list.size() > 2) {
				// 取出最后2个
				List<ExcelRecord> listTemp = new ArrayList<>();
				listTemp.add(list.get(list.size() - 2));
				listTemp.add(list.get(list.size() - 1));
				records.addAll(listTemp);
			} else {
				records.addAll(list);
			}
		}

		// 取出excelRecords中不包含reimburseLineIdList的
		for (ExcelRecord excelRecord : excelRecords) {
			if (!reimburseLineIdList.contains(excelRecord.getReimburseLineId())) {
				records.add(excelRecord);
			}
		}
		return records;
	}
}
