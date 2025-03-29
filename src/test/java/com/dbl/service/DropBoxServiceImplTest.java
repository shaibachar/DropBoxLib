package com.dbl.service;

import com.dbl.config.DropBoxLibProperties;
import com.dbl.exception.DropBoxLibException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListRevisionsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DropBoxServiceImplTest {

    @Mock
    private DropBoxLibProperties properties;

    @Mock
    private DropBoxUtils dropBoxUtils;

    @Mock
    private DbxClientV2 client;

    @Mock
    private DbxUserFilesRequests userFilesRequests;

    @InjectMocks
    private DropBoxServiceImpl dropBoxService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dropBoxService = new DropBoxServiceImpl(properties, dropBoxUtils);
        // inject mock client manually since DropBoxUtils creates it
        Whitebox.setInternalState(dropBoxService, "client", client);
    }

    @Test
    void testGetRevisions_success() throws Exception {
        String path = "/test.txt";
        ListRevisionsResult revisionsResult = mock(ListRevisionsResult.class);

        when(client.files()).thenReturn(userFilesRequests);
        when(userFilesRequests.listRevisions(path)).thenReturn(revisionsResult);

        ListRevisionsResult result = dropBoxService.getRevisions(path);

        assertNotNull(result);
        verify(client.files()).listRevisions(path);
    }

    @Test
    void testGetRevisions_failure() throws Exception {
        String path = "/fail.txt";

        when(client.files()).thenReturn(userFilesRequests);
        when(userFilesRequests.listRevisions(path)).thenThrow(new RuntimeException("Simulated failure"));

        DropBoxLibException ex = assertThrows(DropBoxLibException.class,
                () -> dropBoxService.getRevisions(path));
        assertTrue(ex.getMessage().contains("error while getting revisions"));
    }

    @Test
    void testDownload_failure_throwsWrappedException() throws Exception {
        String filePath = "/some/file.txt";

        when(dropBoxUtils.download(filePath, client)).thenThrow(new RuntimeException("fail"));

        DropBoxLibException ex = assertThrows(DropBoxLibException.class,
                () -> dropBoxService.download(filePath));

        assertTrue(ex.getMessage().contains("error while download file"));
    }

    @Test
    void testUpload_success() throws Exception {
        FileMetadata mockMetadata = mock(FileMetadata.class);

        when(dropBoxUtils.upload(any(), eq("/path/file.txt"), eq(client), eq(true)))
                .thenReturn(mockMetadata);

        FileMetadata result = dropBoxService.upload(mock(InputStream.class), "/path/file.txt", true);
        assertNotNull(result);
    }

    @Test
    void testAllFiles_withFilter() {
        FileMetadata file1 = mock(FileMetadata.class);
        FileMetadata file2 = mock(FileMetadata.class);

        when(file1.getPathLower()).thenReturn("/folder/a.txt");
        when(file2.getPathLower()).thenReturn("/folder/b.jpg");

        DropBoxServiceImpl spyService = Mockito.spy(dropBoxService);
        doReturn(List.of(file1, file2)).when(spyService).allFiles("/folder", true);

        List<FileMetadata> filtered = spyService.allFiles("/folder", true, List.of(".txt"));
        assertEquals(1, filtered.size());
        assertEquals("/folder/a.txt", filtered.get(0).getPathLower());
    }
}
