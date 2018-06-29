package com.dbl.service.indicator;

import java.time.LocalDateTime;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.dbl.service.LongPoolService;

@Component
public class DropBoxConnectionHealthIndicator implements HealthIndicator {

	private final LongPoolService longPoolService;

	public DropBoxConnectionHealthIndicator(LongPoolService longPoolService) {
		this.longPoolService = longPoolService;
	}

	@Override
	public Health health() {
		LocalDateTime minusMinutes = LocalDateTime.now().minusMinutes(5);
		if (longPoolService.getLastChangeTime().isAfter(minusMinutes)) {
			return Health.up().withDetail("connected", true).build();
		}else {
			longPoolService.connect();
			return Health.down().withDetail("connected", false).build();
		}
	}

}
