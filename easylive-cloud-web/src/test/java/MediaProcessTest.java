import com.easylive.EasyliveCloudWebRunApplication;
import com.easylive.service.MediaProcessService;
import com.easylive.service.UserFocusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = EasyliveCloudWebRunApplication.class)
public class MediaProcessTest {

    @Autowired
    private MediaProcessService mediaProcessService;

    @Test
    public void testExtract() {
        String path = "E:\\AALEARN\\Code\\EasyLive-Cloud\\file\\video\\20260408\\3olKB0xfpFi\\index.m3u8";
        String result = mediaProcessService.extractAudioFromM3u8(path);
        System.out.println(result);
    }
}
