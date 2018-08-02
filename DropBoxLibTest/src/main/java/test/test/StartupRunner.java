package test.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.dbl.service.DropBoxService;
import com.dbl.service.FileEventListener;
import com.dbl.service.LongPoolService;

@Component
@ConditionalOnProperty(prefix = "job.autorun", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StartupRunner implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

	@Autowired
	private DropBoxService dropBoxService;
	
	@Autowired
	private LongPoolService longPoolService;
	@Autowired
	private FileEventListener fileEventListener;
	
	@Override
	public void run(String... args) throws Exception {
		log.info("Going to connect to dropBox server");
		dropBoxService.connect();

		log.info("Going to connect to dropBox and listen to changes");
		longPoolService.register(fileEventListener);
		longPoolService.keepConnection();
	}

}
