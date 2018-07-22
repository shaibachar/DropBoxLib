package com.dbl.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dbl.service.DropBoxService;
import com.dbl.service.DropBoxServiceImpl;
import com.dbl.service.DropBoxUtils;
import com.dbl.service.DropBoxUtilsImpl;
import com.dbl.service.LongPoolService;
import com.dbl.service.LongPoolServiceImpl;
import com.dbl.service.indicator.DropBoxConnectionHealthIndicator;

@Configuration
@EnableConfigurationProperties(DropBoxLibProperties.class)
@ConditionalOnClass(value = { DropBoxService.class, LongPoolService.class })
public class DropBoxConfig {

	@Autowired
	private DropBoxLibProperties boxLibProperties;

	@Bean
	@ConditionalOnMissingBean
	public DropBoxUtils getDropBoxUtils() {
		return new DropBoxUtilsImpl();
	}

	@Bean
	@ConditionalOnMissingBean
	public DropBoxService getDropBoxService() {
		return new DropBoxServiceImpl(boxLibProperties, getDropBoxUtils());
	}

	@Bean
	@ConditionalOnMissingBean
	public LongPoolService getLongPoolService() {
		return new LongPoolServiceImpl(boxLibProperties, getDropBoxUtils());
	}

}
