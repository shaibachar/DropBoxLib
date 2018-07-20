package test.test.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dbl.service.DropBoxService;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;

@RestController
@RequestMapping("/dropBox")
public class DropBoxResource {
	private static final Logger logger = LoggerFactory.getLogger(DropBoxResource.class);

	@Autowired
	private ServletContext servletContext;

	@Autowired
	private DropBoxService dropBoxService;

	@GetMapping("/allFiles")
	public List<FileMetadata> allFiles() throws IOException, DbxException {

		List<FileMetadata> allFiles = dropBoxService.allFiles("", true);

		return allFiles;
	}

	@GetMapping("/download")
	public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("fileName") String fileName) throws IOException, DbxException {

		MediaType mediaType = getMediaTypeForFileName(this.servletContext, fileName);
		logger.debug("fileName: " + fileName);
		logger.debug("mediaType: " + mediaType);

		byte[] file = dropBoxService.download(fileName);
		InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(file));

		return ResponseEntity.ok()
				// Content-Disposition
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
				// Content-Type
				.contentType(mediaType)
				// Contet-Length
				.contentLength(file.length) //
				.body(resource);
	}

	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("filePath") String filePath) throws IOException, DbxException {
		MediaType mediaType = getMediaTypeForFileName(this.servletContext, filePath);
		FileMetadata upload = dropBoxService.upload(file.getInputStream(), filePath);

		return ResponseEntity.ok()
				// Content-Disposition
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filePath)
				// Content-Type
				.contentType(mediaType)
				// Contet-Length
				.contentLength(file.getSize()) //
				.body(upload.getId());
	}

	@PostMapping("/rename")
	public ResponseEntity<String> rename(@RequestParam("currentName") String currentName, @RequestParam("newName") String newName) throws DbxException {

		dropBoxService.rename(currentName, newName);

		return ResponseEntity.ok().build();
	}

	private MediaType getMediaTypeForFileName(ServletContext servletContext, String fileName) {
		String mineType = servletContext.getMimeType(fileName);
		try {
			MediaType mediaType = MediaType.parseMediaType(mineType);
			return mediaType;
		} catch (Exception e) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}
}
