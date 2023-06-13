package com.lundong.ascentialsync.controller;

import com.lundong.ascentialsync.service.PaypoolService;
import com.lundong.ascentialsync.service.SpendService;
import com.lundong.ascentialsync.service.StaffService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author RawChen
 * @date 2023-05-12 14:13
 */
@Slf4j
@RestController
@RequestMapping
public class SyncController {

	@Autowired
	StaffService syncService;

	@Autowired
	SpendService spendService;

	@Autowired
	PaypoolService paypoolService;

	@GetMapping("/syncStaffData")
	public void syncStaffData() {
		syncService.syncStaffData();
	}

	@GetMapping("/syncSpendData")
	public void syncFormData() {
		spendService.syncSpendData();
	}

	@GetMapping("/syncPaypoolData")
	public void syncPaypoolData() {
		paypoolService.syncPaypoolData();
	}

}
