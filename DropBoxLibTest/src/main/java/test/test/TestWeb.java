package test.test;

import org.springframework.web.bind.annotation.RestController;

import com.dbl.service.DropBoxService;

@RestController
public class TestWeb {

	private final DropBoxService dropBoxService2;
	
	public TestWeb(DropBoxService dropBoxService) {
		dropBoxService2 = dropBoxService;
		// TODO Auto-generated constructor stub
	}
}
