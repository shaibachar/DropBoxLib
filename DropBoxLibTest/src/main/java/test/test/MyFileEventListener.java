package test.test;

import org.springframework.stereotype.Component;

import com.dbl.domain.message.FileMessage;
import com.dbl.service.FileEventListener;
import com.dropbox.core.v2.files.Metadata;

@Component
public class MyFileEventListener implements FileEventListener {

	@Override
	public void fileChanged(FileMessage fileMessage) {
		Metadata messageDetails = fileMessage.getMessageDetails();
		System.out.println("found new file change:" + messageDetails);

	}

}
