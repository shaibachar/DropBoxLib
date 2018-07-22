package com.dbl.service.indicator;

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
		if (longPoolService.isHealth()) {
			return Health.up().withDetail("connected", true).build();
		}else {
			return Health.down().withDetail("connected", false).build();
		}
	}

}
