package ro.simavi.sphinx.dtm.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/helper/log")
public class LogController {

    @Autowired
    private Environment environment;

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @RequestMapping(value = "show", method = RequestMethod.GET)
    public void getFile(HttpServletResponse response) {
        try {

            String loggingFileName = environment.getProperty("logging.file.name");
            logger.info("display log file from: "+loggingFileName);

            InputStream is = new BufferedInputStream(new FileInputStream(loggingFileName));
            org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();

        } catch (Exception ex) {
            logger.info("download log file - error");
            throw new RuntimeException("IOError writing file to output stream");
        }

    }

    @RequestMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile1() throws IOException {

        String loggingFileName = environment.getProperty("logging.file.name");
        InputStreamResource resource = new InputStreamResource(new FileInputStream(loggingFileName));

        return ResponseEntity.ok()
                // Content-Disposition
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + "dtm.txt")
                // Content-Type
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

}
