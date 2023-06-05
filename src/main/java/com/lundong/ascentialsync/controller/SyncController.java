package com.lundong.ascentialsync.controller;

import com.lundong.ascentialsync.service.SpendService;
import com.lundong.ascentialsync.service.SyncService;
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
	SyncService syncService;

	@Autowired
	SpendService spendService;

	@GetMapping("/syncStaff")
	public void syncStaff() {
		syncService.syncStaffData();
	}

	@GetMapping("/syncFormData")
	public void syncFormData() {
		spendService.syncFormData();
	}


}
