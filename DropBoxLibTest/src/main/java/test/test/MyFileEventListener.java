package test.test;

import java.util.List;

import org.springframework.stereotype.Component;

import com.dbl.config.DropBoxLibProperties;
import com.dbl.domain.message.FileMessage;
import com.dbl.service.FileEventListener;
import com.dropbox.core.v2.files.Metadata;

@Component
public class MyFileEventListener implements FileEventListener {

	private final DropBoxLibProperties dropBoxLibProperties;
	
	public MyFileEventListener(DropBoxLibProperties dropBoxLibProperties) {
		this.dropBoxLibProperties = dropBoxLibProperties;
	}
	
	@Override
	public void fileChanged(FileMessage fileMessage) {
		Metadata messageDetails = fileMessage.getMessageDetails();
		System.out.println("found new file change:" + messageDetails);

	}

	@Override
	public List<String> getInterestingFileFormat() {
		return dropBoxLibProperties.getInterestingFileFormat();
	}

}
